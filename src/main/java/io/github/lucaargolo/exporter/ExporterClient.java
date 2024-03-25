package io.github.lucaargolo.exporter;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.creation.AccessorModels;
import de.javagl.jgltf.model.creation.GltfModelBuilder;
import de.javagl.jgltf.model.impl.*;
import de.javagl.jgltf.model.io.GltfModelWriter;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import io.github.lucaargolo.exporter.entities.ReferenceBlockDisplay;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

public class ExporterClient implements ClientModInitializer {

    public static final Logger LOGGER = LogUtils.getLogger();
    private static int MODEL_COUNT = 0;
    public static boolean COMPLETE = false;

    public static int MARKED_ENTITY = -1;
    public static Matrix4f INVERTED_POSE;
    public static Vector3f VERTEX_POSITION = new Vector3f(0, 0, 0);
    public static BoundingBox MARKED_BOX;
    public static MultiBufferSource MARKED_BUFFER;
    public static final Object2IntMap<VertexConsumer> MARKED_CONSUMERS = new Object2IntArrayMap<>();

    public static final IntArraySet CAPTURED_IMAGES = new IntArraySet();
    public static final Int2IntArrayMap VERTICE_COUNT = new Int2IntArrayMap();
    public static final Int2ObjectArrayMap<IntArrayList> VERTICE_HOLDER = new Int2ObjectArrayMap<>();
    public static final Int2ObjectArrayMap<List<Vector3f>> CAPTURED_VERTICES = new Int2ObjectArrayMap<>();
    public static final Int2ObjectArrayMap<List<Vector3i>> CAPTURED_TRIANGLES = new Int2ObjectArrayMap<>();
    public static final Int2ObjectArrayMap<List<Vector2f>> CAPTURED_UVS = new Int2ObjectArrayMap<>();
    public static final Int2ObjectArrayMap<List<Vector3f>> CAPTURED_COLORS = new Int2ObjectArrayMap<>();
    public static final Int2ObjectArrayMap<List<Vector3f>> CAPTURED_NORMALS = new Int2ObjectArrayMap<>();

    public static final List<NodeModel> NODES = new ArrayList<>();
    public static final Int2ObjectArrayMap<MaterialModel> MATERIALS = new Int2ObjectArrayMap<>();

    private static boolean UV = false;
    private static boolean COLOR = false;
    private static boolean NORMAL = false;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(ExporterCommand::register);
        WorldRenderEvents.BEFORE_ENTITIES.register(context -> {
            if(MARKED_BOX != null) {
                var minecraft = Minecraft.getInstance();
                var blockDispatcher = minecraft.getBlockRenderer();
                var entityDispatcher = minecraft.getEntityRenderDispatcher();
                var blockEntityDispatcher = minecraft.getBlockEntityRenderDispatcher();

                var level = context.world();
                markEntity(Integer.MAX_VALUE);
                BlockPos.betweenClosed(MARKED_BOX.minX(), MARKED_BOX.minY(), MARKED_BOX.minZ(), MARKED_BOX.maxX(), MARKED_BOX.maxY(), MARKED_BOX.maxZ()).forEach(pos -> {
                    var state = level.getBlockState(pos);
                    if(!state.isAir()) {
                        int i = LevelRenderer.getLightColor(level, pos);
                        var blockEntity = level.getBlockEntity(pos);
                        if(blockEntity != null) {
                            var renderer = blockEntityDispatcher.getRenderer(blockEntity);
                            if(renderer != null) {
                                if(ExporterClient.COMPLETE) {
                                    markEntity(Integer.MAX_VALUE);
                                }
                                ExporterClient.INVERTED_POSE = context.matrixStack().last().pose().invert(new Matrix4f());
                                ExporterClient.MARKED_BUFFER = context.consumers();
                                if(!ExporterClient.COMPLETE && ExporterClient.MARKED_BOX != null) {
                                    BoundingBox box = ExporterClient.MARKED_BOX;
                                    ExporterClient.VERTEX_POSITION = new Vector3f((float) (pos.getX() - box.getCenter().getX() - 0.5), (float) (pos.getY() - box.getCenter().getY() - 0.5), (float) (pos.getZ() - box.getCenter().getZ() - 0.5));
                                }else{
                                    ExporterClient.VERTEX_POSITION = new Vector3f(0, 0, 0);
                                }
                                renderer.render(blockEntity, context.tickDelta(), context.matrixStack(), context.consumers(), i, OverlayTexture.NO_OVERLAY);
                                ExporterClient.INVERTED_POSE = null;
                                ExporterClient.MARKED_BUFFER = null;
                                ExporterClient.MARKED_CONSUMERS.clear();
                                if(ExporterClient.COMPLETE) {
                                    BoundingBox box = ExporterClient.MARKED_BOX;
                                    ExporterClient.writeCapturedNode(new Vector3f((float) (pos.getX() - box.getCenter().getX() - 0.5), (float) (pos.getY() - box.getCenter().getY() - 0.5), (float) (pos.getZ() - box.getCenter().getZ() - 0.5)));
                                }
                            }
                        }
                        if(state.getRenderShape() != RenderShape.ENTITYBLOCK_ANIMATED) {
                            if(ExporterClient.COMPLETE) {
                                var display = new ReferenceBlockDisplay(EntityType.BLOCK_DISPLAY, level);
                                display.setBlockPos(pos.immutable());
                                display.setBlockState(state);
                                display.setId(Integer.MAX_VALUE);
                                display.updateRenderSubState(true, context.tickDelta());
                                display.renderState = display.createInterpolatedRenderState(display.createFreshRenderState(), context.tickDelta());
                                if (COMPLETE) {
                                    markEntity(Integer.MAX_VALUE);
                                } else {
                                    MARKED_ENTITY = Integer.MAX_VALUE;
                                }
                                entityDispatcher.render(display, pos.getX(), pos.getY(), pos.getZ(), 0f, context.tickDelta(), context.matrixStack(), context.consumers(), LightTexture.FULL_BRIGHT);
                            }else{
                                ExporterClient.MARKED_BUFFER = context.consumers();
                                BoundingBox box = ExporterClient.MARKED_BOX;
                                ExporterClient.INVERTED_POSE = context.matrixStack().last().pose().invert(new Matrix4f());
                                ExporterClient.VERTEX_POSITION = new Vector3f((float) (pos.getX() - box.getCenter().getX() - 0.5), (float) (pos.getY() - box.getCenter().getY() - 0.5), (float) (pos.getZ() - box.getCenter().getZ() - 0.5));
                                if(MARKED_BUFFER != null) {
                                    RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(state);
                                    VertexConsumer consumer = ExporterClient.MARKED_BUFFER.getBuffer(renderType);
                                    blockDispatcher.renderBatched(state, pos, level, context.matrixStack(), consumer, true, RandomSource.create());
                                }
                                ExporterClient.INVERTED_POSE = null;
                                ExporterClient.MARKED_BUFFER = null;
                                ExporterClient.MARKED_CONSUMERS.clear();
                            }
                        }
                        FluidState fluidState = state.getFluidState();
                        if(!fluidState.isEmpty()) {
                            if(ExporterClient.COMPLETE) {
                                markEntity(Integer.MAX_VALUE);
                            }
                            ExporterClient.MARKED_BUFFER = context.consumers();
                            BoundingBox box = ExporterClient.MARKED_BOX;
                            ExporterClient.VERTEX_POSITION = new Vector3f(-pos.getX()%16 + pos.getX() - box.getCenter().getX() - 0.5f - 16f, -pos.getY()%16 + pos.getY() - box.getCenter().getY() - 0.5f, -pos.getZ()%16 + pos.getZ() - box.getCenter().getZ() - 0.5f);
                            if(MARKED_BUFFER != null) {
                                RenderType renderType = ItemBlockRenderTypes.getRenderLayer(fluidState);
                                VertexConsumer consumer = ExporterClient.MARKED_BUFFER.getBuffer(renderType);
                                blockDispatcher.renderLiquid(pos, level, consumer, state, fluidState);
                            }
                            ExporterClient.MARKED_BUFFER = null;
                            ExporterClient.MARKED_CONSUMERS.clear();
                            if(ExporterClient.COMPLETE) {
                                ExporterClient.writeCapturedNode(new Vector3f(0, 0, 0));
                            }
                        }
                    }
                });
                if(!COMPLETE)
                    ExporterClient.writeCapturedNode(new Vector3f(0, 0, 0));
                AABB aabb = AABB.of(MARKED_BOX);
                level.getEntities(null, aabb).forEach(entity -> {
                    markEntity(entity.getId());
                    float rot = Mth.lerp(context.tickDelta(), entity.yRotO, entity.getYRot());
                    entityDispatcher.render(entity, entity.getX(), entity.getY(), entity.getZ(), rot, context.tickDelta(), context.matrixStack(), context.consumers(), LightTexture.FULL_BRIGHT);
                });
                MARKED_BOX = null;
                writeCapturedModel();
            }
        });
    }

    public static void markEntity(int entityId) {
        MARKED_ENTITY = entityId;
        CAPTURED_IMAGES.clear();
        VERTICE_COUNT.clear();
        CAPTURED_VERTICES.clear();
        CAPTURED_TRIANGLES.clear();
        CAPTURED_UVS.clear();
        CAPTURED_COLORS.clear();
        CAPTURED_NORMALS.clear();
    }

    public static void captureVertex(int glID, float x, float y, float z) {
        CAPTURED_IMAGES.add(glID);
        List<Vector3f> vertices = CAPTURED_VERTICES.computeIfAbsent(glID, i -> new ArrayList<>());
        List<Vector3i> triangles = CAPTURED_TRIANGLES.computeIfAbsent(glID, i -> new ArrayList<>());

        Vector4f reversedVertex = INVERTED_POSE != null ? INVERTED_POSE.transform(new Vector4f(x, y, z, 1f)) : new Vector4f(x, y, z, 1f);
        Vector3f capturedVertex = new Vector3f(reversedVertex.x, reversedVertex.y, reversedVertex.z).add(VERTEX_POSITION);
        int index = vertices.size();
        vertices.add(capturedVertex);

        int verticeCount = VERTICE_COUNT.computeIfAbsent(glID, i -> 0);
        IntArrayList verticeHolder = VERTICE_HOLDER.computeIfAbsent(glID, i -> new IntArrayList(new int[4]));
        verticeHolder.set(verticeCount % 4, index);
        if((verticeCount+1) % 4 == 0) {
            int v0 = verticeHolder.getInt(0);
            int v1 = verticeHolder.getInt(1);
            int v2 = verticeHolder.getInt(2);
            int v3 = verticeHolder.getInt(3);
            triangles.add(new Vector3i(v0, v1, v2));
            triangles.add(new Vector3i(v3, v0, v2));
        }
        ExporterClient.VERTICE_COUNT.put(glID, verticeCount+1);

        COLOR = false;
        UV = false;
        NORMAL = false;
    }

    public static void captureUv(int glID, float u, float v) {
        CAPTURED_IMAGES.add(glID);
        List<Vector2f> uvs = CAPTURED_UVS.computeIfAbsent(glID, i -> new ArrayList<>());
        uvs.add(new Vector2f(u, v));
        UV = true;
    }

    public static void captureRgb(int glID, float r, float g, float b) {
        CAPTURED_IMAGES.add(glID);
        List<Vector3f> colors = CAPTURED_COLORS.computeIfAbsent(glID, i -> new ArrayList<>());
        colors.add(new Vector3f(r, g, b));
        COLOR = true;
    }

    public static void captureNormal(int glID, float x, float y, float z) {
        CAPTURED_IMAGES.add(glID);
        List<Vector3f> normals = CAPTURED_NORMALS.computeIfAbsent(glID, i -> new ArrayList<>());
        normals.add(new Vector3f(x, y, z));
        NORMAL = true;
    }

    public static void endCapture(int glID) {
        if(!UV)
            captureUv(glID, 0f, 0f);
        if(!COLOR)
            captureRgb(glID, 1f, 1f, 1f);
        if(!NORMAL)
            captureNormal(glID, 1f, 1f, 1f);
    }

    public static ByteBuffer extractTexture(int glId) {
        int[] textureId = new int[1];
        GL11.glGetIntegerv(GL11.GL_TEXTURE_BINDING_2D, textureId);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, glId);
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId[0]);

        return convertToPNG(buffer, width, height);
    }

    public static ByteBuffer convertToPNG(ByteBuffer rgbaData, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            int r = rgbaData.get() & 0xFF;
            int g = rgbaData.get() & 0xFF;
            int b = rgbaData.get() & 0xFF;
            int a = rgbaData.get() & 0xFF;
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        img.setRGB(0, 0, width, height, pixels, 0, width);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(img, "png", baos);
            baos.flush();
            byte[] pngData = baos.toByteArray();
            baos.close();
            return ByteBuffer.wrap(pngData);
        }catch (Exception e) {
            return ByteBuffer.wrap(new byte[0]);
        }
    }

    public static void writeCapturedNode(Vector3f translation) {
        var node = new DefaultNodeModel();
        node.setTranslation(new float[] {translation.x, translation.y, translation.z});
        for (int glId : CAPTURED_IMAGES) {
            float[] vertices = new float[CAPTURED_VERTICES.get(glId).size() * 3];
            for (int i = 0; i < CAPTURED_VERTICES.get(glId).size(); i++) {
                Vector3f vertex = CAPTURED_VERTICES.get(glId).get(i);
                vertices[i * 3] = vertex.x;
                vertices[i * 3 + 1] = vertex.y;
                vertices[i * 3 + 2] = vertex.z;
            }
            float[] uvs = new float[CAPTURED_UVS.get(glId).size() * 2];
            for (int i = 0; i < CAPTURED_UVS.get(glId).size(); i++) {
                Vector2f uv = CAPTURED_UVS.get(glId).get(i);
                uvs[i * 2] = uv.x;
                uvs[i * 2 + 1] = uv.y;
            }
            float[] colors = new float[CAPTURED_COLORS.get(glId).size() * 3];
            for (int i = 0; i < CAPTURED_COLORS.get(glId).size(); i++) {
                Vector3f color = CAPTURED_COLORS.get(glId).get(i);
                colors[i * 3] = color.x;
                colors[i * 3 + 1] = color.y;
                colors[i * 3 + 2] = color.z;
            }
            float[] normals = new float[CAPTURED_NORMALS.get(glId).size() * 3];
            for (int i = 0; i < CAPTURED_NORMALS.get(glId).size(); i++) {
                Vector3f normal = CAPTURED_NORMALS.get(glId).get(i);
                normals[i * 3] = normal.x;
                normals[i * 3 + 1] = normal.y;
                normals[i * 3 + 2] = normal.z;
            }
            int[] triangles = new int[CAPTURED_TRIANGLES.get(glId).size() * 3];
            for (int i = 0; i < CAPTURED_TRIANGLES.get(glId).size(); i++) {
                Vector3i triangle = CAPTURED_TRIANGLES.get(glId).get(i);
                triangles[i * 3] = triangle.x;
                triangles[i * 3 + 1] = triangle.y;
                triangles[i * 3 + 2] = triangle.z;
            }
            try {
                var child = new DefaultNodeModel();
                var material = MATERIALS.computeIfAbsent(glId, ExporterClient::getMaterial);
                MeshModel mesh = writeMesh(material, vertices, uvs, colors, normals, triangles);
                child.addMeshModel(mesh);
                node.addChild(child);
            } catch (Exception e) {
                LOGGER.error("Error while saving node: ", e);
            }
        }
        NODES.add(node);
    }

    public static MaterialModel getMaterial(int glId) {
        ByteBuffer imageBuffer = extractTexture(glId);

        var image = new DefaultImageModel();
        image.setImageData(imageBuffer);

        var texture = new DefaultTextureModel();
        texture.setImageModel(image);
        texture.setMinFilter(GL11.GL_NEAREST);
        texture.setMagFilter(GL11.GL_NEAREST);

        var material = new MaterialModelV2();
        material.setBaseColorTexture(texture);
        material.setAlphaMode(MaterialModelV2.AlphaMode.MASK);
        material.setDoubleSided(false);

        return material;
    }

    public static void writeCapturedModel() {
        var builder = GltfModelBuilder.create();

        var scene = new DefaultSceneModel();

        for(NodeModel node : NODES) {
            scene.addNode(node);
        }
        builder.addSceneModel(scene);

        var model = builder.build();
        var file = new File(FabricLoader.getInstance().getGameDir() + File.separator + "models" + File.separator + "model" + ++MODEL_COUNT + ".gltf");
        var writer = new GltfModelWriter();
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        try {
            writer.writeEmbedded(model, file);
            if(player != null)
                player.displayClientMessage(Component.literal("Successfully saved model at "+file+" with "+model.getMeshModels().size()+" meshes.").withStyle(ChatFormatting.GREEN), false);
        } catch (Exception e) {
            LOGGER.error("Error while saving model: ", e);
            if(player != null)
                player.displayClientMessage(Component.literal("Error while saving model. Please check logs.").withStyle(ChatFormatting.RED), false);
        }
    }

    public static MeshModel writeMesh(MaterialModel material, float[] vertices, float[] texCoords, float[] colors, float[] normals, int[] indices) {

        var mesh = new DefaultMeshModel();

        var primitive = new DefaultMeshPrimitiveModel(GL11.GL_TRIANGLES);

        var position = AccessorModels.createFloat3D(FloatBuffer.wrap(vertices));
        primitive.putAttribute("POSITION", position);

        var texCoord = AccessorModels.createFloat2D(FloatBuffer.wrap(texCoords));
        primitive.putAttribute("TEXCOORD_0", texCoord);

        var color = AccessorModels.createFloat3D(FloatBuffer.wrap(colors));
        primitive.putAttribute("COLOR_0", color);

        var normal = AccessorModels.createFloat3D(FloatBuffer.wrap(normals));
        primitive.putAttribute("NORMAL", normal);

        var triangles = AccessorModels.createUnsignedIntScalar(IntBuffer.wrap(indices));
        primitive.setIndices(triangles);

        try {
            primitive.setMaterialModel(material);
        } catch (Exception e) {
            LOGGER.error("Error while saving mesh: ", e);
        }

        mesh.addMeshPrimitiveModel(primitive);
        return mesh;

    }



}
