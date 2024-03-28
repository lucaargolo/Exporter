package io.github.lucaargolo.exporter;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.creation.AccessorModels;
import de.javagl.jgltf.model.creation.GltfModelBuilder;
import de.javagl.jgltf.model.impl.*;
import de.javagl.jgltf.model.io.GltfModelWriter;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import io.github.lucaargolo.exporter.compat.Compat;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
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
import java.util.*;

public class ExporterClient implements ClientModInitializer {

    public static final Logger LOGGER = LogUtils.getLogger();
    private static int MODEL_COUNT = 0;

    public static boolean SETUP = false;
    public static boolean COMPLETE = false;
    public static float TICK_DELTA = 1f;

    public static BoundingBox MARKED_BOX;
    public static int MARKED_ENTITY = -1;

    public static Matrix4f INVERTED_POSE;
    public static Matrix3f INVERTED_NORMAL;
    public static Vector3f VERTEX_POSITION = new Vector3f(0, 0, 0);

    public static Matrix4f EXTRA_POSE;
    public static Matrix3f EXTRA_NORMAL;
    public static Vector3f EXTRA_POSITION = new Vector3f(0, 0, 0);

    public static MultiBufferSource MARKED_BUFFER;
    public static final Map<VertexConsumer, RenderInfo> MARKED_CONSUMERS = new HashMap<>();
    public static final Set<RenderInfo> CAPTURED_INFO = new HashSet<>();

    public static final Map<RenderInfo, Integer> VERTICE_COUNT = new HashMap<>();
    public static final Map<RenderInfo, IntArrayList> VERTICE_HOLDER = new HashMap<>();
    public static final Map<RenderInfo, List<Vector3f>> CAPTURED_VERTICES = new HashMap<>();
    public static final Map<RenderInfo, List<Vector3i>> CAPTURED_TRIANGLES = new HashMap<>();
    public static final Map<RenderInfo, List<Vector2f>> CAPTURED_UVS = new HashMap<>();
    public static final Map<RenderInfo, List<Vector4f>> CAPTURED_COLORS = new HashMap<>();
    public static final Map<RenderInfo, List<Vector3f>> CAPTURED_NORMALS = new HashMap<>();

    public static final List<NodeModel> NODES = new ArrayList<>();
    public static final Map<RenderInfo, MaterialModel> MATERIALS = new HashMap<>();
    public static final Map<Integer, TextureModel> TEXTURES = new HashMap<>();

    private static boolean UV = false;
    private static boolean COLOR = false;
    private static boolean NORMAL = false;

    /**TODO:
     *  - Lava exporting is glitchy for some reason?.
     *  - Fix fluid exporting when using complete. (I think its a Godot Issue)
     *  - Add user interface with options (Ambient Occlusion on/off, Entities on/off, File binary/embed/normal)
     *  - Find a way to export blocks outside player view? (Not priority)
     *  - Why didn't it export entities in all of fabric?
     *  - Create compatibility?
     */

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register(ExporterCommand::register);
        WorldRenderEvents.BEFORE_ENTITIES.register(ExporterClient::renderMarkedBox);
        WorldRenderEvents.END.register(ExporterClient::setupRender);
    }

    public static void captureBuffer(RenderType renderType, VertexConsumer buffer) {
        if(renderType instanceof RenderType.CompositeRenderType composite) {
            var cullShard = composite.state().cullState;
            var emptyShard = composite.state().textureState;
            var transparencyShard = composite.state().transparencyState;

            int glId = -1;
            int normalGlId = -1;
            int specularGlId = -1;
            if(emptyShard instanceof RenderStateShard.TextureStateShard textureShard && textureShard.texture.isPresent()) {
                Minecraft minecraft = Minecraft.getInstance();
                TextureManager manager = minecraft.getTextureManager();
                AbstractTexture texture = manager.getTexture(textureShard.texture.get());
                glId = texture.getId();
                normalGlId = Compat.collectNormalTexture(glId);
                specularGlId = Compat.collectSpecularTexture(glId);
            }

            boolean oldCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            cullShard.setupRenderState();
            boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
            if(oldCull) {
                RenderSystem.enableCull();
            }else{
                RenderSystem.disableCull();
            }

            boolean oldBlend = GL11.glIsEnabled(GL11.GL_BLEND);
            transparencyShard.setupRenderState();
            boolean blend = GL11.glIsEnabled(GL11.GL_BLEND);
            if(oldBlend) {
                RenderSystem.enableBlend();
            }else{
                RenderSystem.disableBlend();
            }

            String name = composite.toString();
            RenderInfo info = new RenderInfo(glId, normalGlId, specularGlId, RenderInfo.Type.fromName(name), cull, blend);
            buffer = Compat.collectAllBuffers(buffer);
            ExporterClient.MARKED_CONSUMERS.put(buffer, info);

            //TODO: Add support to MultiTextureShard
        }
    }

    public static void setupRender(WorldRenderContext context) {
        if(SETUP) {
            if(MARKED_BOX == null && MARKED_ENTITY == -1) {
                Compat.clearAll();
                SETUP = false;
            }
        }else if(MARKED_BOX != null || MARKED_ENTITY != -1) {
            Compat.setupAll();
            SETUP = true;
        }
    }

    public static void renderMarkedBox(WorldRenderContext context) {
        renderMarkedBox(context.world(), context.matrixStack(), context.consumers(), context.tickDelta());
    }

    public static void renderMarkedBox(ClientLevel level, PoseStack poseStack, MultiBufferSource bufferSource, float tickDelta) {
        if (SETUP && MARKED_BOX != null) {
            var minecraft = Minecraft.getInstance();
            var blockEntityDispatcher = minecraft.getBlockEntityRenderDispatcher();
            var blockDispatcher = minecraft.getBlockRenderer();
            var entityDispatcher = minecraft.getEntityRenderDispatcher();
            TICK_DELTA = tickDelta;
            markEntity(Integer.MAX_VALUE);
            for (BlockPos pos : BlockPos.betweenClosed(MARKED_BOX.minX(), MARKED_BOX.minY(), MARKED_BOX.minZ(), MARKED_BOX.maxX(), MARKED_BOX.maxY(), MARKED_BOX.maxZ())) {
                renderBlock(blockEntityDispatcher, blockDispatcher, level, pos, poseStack, bufferSource, tickDelta);
            }
            if (!COMPLETE)
                ExporterClient.writeCapturedNode(new Vector3f(0, 0, 0));
            AABB aabb = AABB.of(MARKED_BOX);
            for (Entity entity : level.getEntities(null, aabb)) {
                markEntity(entity.getId());
                float rot = Mth.lerp(tickDelta, entity.yRotO, entity.getYRot());
                entityDispatcher.render(entity, entity.getX(), entity.getY(), entity.getZ(), rot, tickDelta, poseStack, Objects.requireNonNull(bufferSource), LightTexture.FULL_BRIGHT);
            }
            MARKED_ENTITY = -1;
            MARKED_BOX = null;
            writeCapturedModel();
        }
    }

    public static void renderBlock(@Nullable BlockEntityRenderDispatcher blockEntityDispatcher, BlockRenderDispatcher blockDispatcher, BlockAndTintGetter level, BlockPos pos, PoseStack poseStack, MultiBufferSource bufferSource, float tickDelta) {
        var state = level.getBlockState(pos);
        if (!state.isAir()) {
            int packedLight = LevelRenderer.getLightColor(level, pos);
            var blockEntity = level.getBlockEntity(pos);
            if (blockEntity != null && blockEntityDispatcher != null) {
                renderBlockEntity(blockEntityDispatcher, blockEntity, pos, poseStack, bufferSource, tickDelta, packedLight);
            }
            if (state.getRenderShape() != RenderShape.ENTITYBLOCK_ANIMATED) {
                tesselateBlock(blockDispatcher, level, pos, state, poseStack, bufferSource);
            }
            FluidState fluidState = state.getFluidState();
            if (!fluidState.isEmpty()) {
                tesselateFluid(blockDispatcher, level, pos, state, fluidState, bufferSource);
            }
        }
    }

    public static void renderBlockEntity(BlockEntityRenderDispatcher blockEntityDispatcher, BlockEntity blockEntity, BlockPos pos, PoseStack poseStack, MultiBufferSource bufferSource, float tickDelta, int packedLight) {
        var renderer = blockEntityDispatcher.getRenderer(blockEntity);
        if (renderer != null) {
            if (COMPLETE) {
                markEntity(Integer.MAX_VALUE);
            }
            INVERTED_POSE = poseStack.last().pose().invert(new Matrix4f());
            INVERTED_NORMAL = poseStack.last().normal().invert(new Matrix3f());
            MARKED_BUFFER = bufferSource;
            if (!COMPLETE && MARKED_BOX != null) {
                VERTEX_POSITION = getCenter(pos).sub(getCenter(MARKED_BOX));
            } else {
                VERTEX_POSITION = new Vector3f(0, 0, 0);
            }
            renderer.render(blockEntity, tickDelta, poseStack, Objects.requireNonNull(bufferSource), packedLight, OverlayTexture.NO_OVERLAY);
            INVERTED_POSE = null;
            INVERTED_NORMAL = null;
            MARKED_BUFFER = null;
            MARKED_CONSUMERS.clear();
            if (COMPLETE) {
                writeCapturedNode(getCenter(pos).sub(getCenter(MARKED_BOX)));
                MARKED_ENTITY = -1;
            }
        }
    }

    private static void tesselateBlock(BlockRenderDispatcher blockDispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource) {
        if (ExporterClient.COMPLETE) {
            markEntity(Integer.MAX_VALUE);
        }
        MARKED_BUFFER = bufferSource;
        INVERTED_POSE = poseStack.last().pose().invert(new Matrix4f());
        INVERTED_NORMAL = poseStack.last().normal().invert(new Matrix3f());
        VERTEX_POSITION = new Vector3f(getCenter(pos).sub(getCenter(MARKED_BOX)));
        if (MARKED_BUFFER != null) {
            RenderType renderType = ItemBlockRenderTypes.getChunkRenderType(state);
            VertexConsumer consumer = MARKED_BUFFER.getBuffer(renderType);
            blockDispatcher.renderBatched(state, pos, level, poseStack, consumer, !COMPLETE, RandomSource.create());
        }
        INVERTED_POSE = null;
        INVERTED_NORMAL = null;
        MARKED_BUFFER = null;
        MARKED_CONSUMERS.clear();
        if (ExporterClient.COMPLETE) {
            writeCapturedNode(new Vector3f(0, 0, 0));
            MARKED_ENTITY = -1;
        }
    }

    private static void tesselateFluid(BlockRenderDispatcher blockDispatcher, BlockAndTintGetter level, BlockPos pos, BlockState state, FluidState fluidState, MultiBufferSource bufferSource) {
        if (ExporterClient.COMPLETE) {
            markEntity(Integer.MAX_VALUE);
        }
        MARKED_BUFFER = bufferSource;
        var offset = new Vector3f(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
        VERTEX_POSITION = getCenter(pos).sub(offset).sub(getCenter(MARKED_BOX));
        if (MARKED_BUFFER != null) {
            RenderType renderType = ItemBlockRenderTypes.getRenderLayer(fluidState);
            VertexConsumer consumer = MARKED_BUFFER.getBuffer(renderType);
            blockDispatcher.renderLiquid(pos, level, consumer, state, fluidState);
        }
        MARKED_BUFFER = null;
        MARKED_CONSUMERS.clear();
        if (ExporterClient.COMPLETE) {
            writeCapturedNode(new Vector3f(0, 0, 0));
            MARKED_ENTITY = -1;
        }
    }

    public static void markEntity(int entityId) {
        MARKED_ENTITY = entityId;
        CAPTURED_INFO.clear();
        VERTICE_COUNT.clear();
        CAPTURED_VERTICES.clear();
        CAPTURED_TRIANGLES.clear();
        CAPTURED_UVS.clear();
        CAPTURED_COLORS.clear();
        CAPTURED_NORMALS.clear();
    }

    public static void captureVertex(RenderInfo info, float x, float y, float z) {
        CAPTURED_INFO.add(info);
        List<Vector3f> vertices = CAPTURED_VERTICES.computeIfAbsent(info, i -> new ArrayList<>());
        List<Vector3i> triangles = CAPTURED_TRIANGLES.computeIfAbsent(info, i -> new ArrayList<>());

        Vector4f reversedVertex = INVERTED_POSE != null ? INVERTED_POSE.transform(new Vector4f(x, y, z, 1f)) : new Vector4f(x, y, z, 1f);
        reversedVertex = EXTRA_POSE != null ? EXTRA_POSE.transform(reversedVertex) : reversedVertex;
        Vector3f capturedVertex = new Vector3f(reversedVertex.x, reversedVertex.y, reversedVertex.z).add(VERTEX_POSITION).add(EXTRA_POSITION);
        int index = vertices.size();
        vertices.add(capturedVertex);

        int verticeCount = VERTICE_COUNT.computeIfAbsent(info, i -> 0);
        IntArrayList verticeHolder = VERTICE_HOLDER.computeIfAbsent(info, i -> new IntArrayList(new int[4]));
        verticeHolder.set(verticeCount % 4, index);
        if((verticeCount+1) % 4 == 0) {
            int v0 = verticeHolder.getInt(0);
            int v1 = verticeHolder.getInt(1);
            int v2 = verticeHolder.getInt(2);
            int v3 = verticeHolder.getInt(3);
            triangles.add(new Vector3i(v0, v1, v2));
            triangles.add(new Vector3i(v3, v0, v2));
        }
        ExporterClient.VERTICE_COUNT.put(info, verticeCount+1);

        COLOR = false;
        UV = false;
        NORMAL = false;
    }

    public static void captureUv(RenderInfo info, float u, float v) {
        CAPTURED_INFO.add(info);
        List<Vector2f> uvs = CAPTURED_UVS.computeIfAbsent(info, i -> new ArrayList<>());
        uvs.add(new Vector2f(Mth.clamp(u, 0f, 1f), Mth.clamp(v, 0f, 1f)));
        UV = true;
    }

    public static void captureRgb(RenderInfo info, float r, float g, float b, float a) {
        CAPTURED_INFO.add(info);
        List<Vector4f> colors = CAPTURED_COLORS.computeIfAbsent(info, i -> new ArrayList<>());
        colors.add(new Vector4f(Mth.clamp(r, 0f, 1f), Mth.clamp(g, 0f, 1f), Mth.clamp(b, 0f, 1f), info.alpha() ? Mth.clamp(a, 0f, 1f) : 1f));
        COLOR = true;
    }

    public static void captureNormal(RenderInfo info, float x, float y, float z) {
        CAPTURED_INFO.add(info);
        List<Vector3f> normals = CAPTURED_NORMALS.computeIfAbsent(info, i -> new ArrayList<>());
        Vector3f reversedNormal = INVERTED_NORMAL != null ? INVERTED_NORMAL.transform(new Vector3f(x, y, z)) : new Vector3f(x, y, z);
        reversedNormal = EXTRA_NORMAL != null ? EXTRA_NORMAL.transform(reversedNormal) : reversedNormal;
        normals.add(reversedNormal.normalize());
        NORMAL = true;
    }

    public static void endCapture(RenderInfo info) {
        if(!UV)
            captureUv(info, 0f, 0f);
        if(!COLOR)
            captureRgb(info, 1f, 1f, 1f, 1f);
        if(!NORMAL)
            captureNormal(info, 0f, 1f, 0f);
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
        for (RenderInfo info: CAPTURED_INFO) {
            float[] vertices = new float[CAPTURED_VERTICES.get(info).size() * 3];
            for (int i = 0; i < CAPTURED_VERTICES.get(info).size(); i++) {
                Vector3f vertex = CAPTURED_VERTICES.get(info).get(i);
                vertices[i * 3] = vertex.x;
                vertices[i * 3 + 1] = vertex.y;
                vertices[i * 3 + 2] = vertex.z;
            }
            float[] uvs = new float[CAPTURED_UVS.get(info).size() * 2];
            for (int i = 0; i < CAPTURED_UVS.get(info).size(); i++) {
                Vector2f uv = CAPTURED_UVS.get(info).get(i);
                uvs[i * 2] = uv.x;
                uvs[i * 2 + 1] = uv.y;
            }
            float[] colors = new float[CAPTURED_COLORS.get(info).size() * 4];
            for (int i = 0; i < CAPTURED_COLORS.get(info).size(); i++) {
                Vector4f color = CAPTURED_COLORS.get(info).get(i);
                colors[i * 4] = color.x;
                colors[i * 4 + 1] = color.y;
                colors[i * 4 + 2] = color.z;
                colors[i * 4 + 3] = color.w;
            }
            float[] normals = new float[CAPTURED_NORMALS.get(info).size() * 3];
            for (int i = 0; i < CAPTURED_NORMALS.get(info).size(); i++) {
                Vector3f normal = CAPTURED_NORMALS.get(info).get(i);
                normals[i * 3] = normal.x;
                normals[i * 3 + 1] = normal.y;
                normals[i * 3 + 2] = normal.z;
            }
            int[] triangles = new int[CAPTURED_TRIANGLES.get(info).size() * 3];
            for (int i = 0; i < CAPTURED_TRIANGLES.get(info).size(); i++) {
                Vector3i triangle = CAPTURED_TRIANGLES.get(info).get(i);
                triangles[i * 3] = triangle.x;
                triangles[i * 3 + 1] = triangle.y;
                triangles[i * 3 + 2] = triangle.z;
            }
            try {
                var child = new DefaultNodeModel();
                var material = MATERIALS.computeIfAbsent(info, ExporterClient::getMaterial);
                MeshModel mesh = writeMesh(material, vertices, uvs, colors, normals, triangles);
                child.addMeshModel(mesh);
                node.addChild(child);
            } catch (Exception e) {
                LOGGER.error("Error while saving node: ", e);
            }
        }
        NODES.add(node);
    }

    public static TextureModel getTexture(int glId) {
        //TODO: Handle LabPBR 1.3
        ByteBuffer imageBuffer = extractTexture(glId);

        var image = new DefaultImageModel();
        image.setImageData(imageBuffer);

        var texture = new DefaultTextureModel();
        texture.setImageModel(image);
        texture.setMinFilter(GL11.GL_NEAREST);
        texture.setMagFilter(GL11.GL_NEAREST);

        return texture;
    }

    public static MaterialModel getMaterial(RenderInfo info) {
        //TODO: Handle LabPBR 1.3

        var material = new MaterialModelV2();
        if(info.glId() >= 0)
            material.setBaseColorTexture(TEXTURES.computeIfAbsent(info.glId(), ExporterClient::getTexture));
        if(info.normalGlId() >= 0)
            material.setNormalTexture(TEXTURES.computeIfAbsent(info.normalGlId(), ExporterClient::getTexture));
        if(info.specularGlId() >= 0)
            material.setMetallicRoughnessTexture(TEXTURES.computeIfAbsent(info.specularGlId(), ExporterClient::getTexture));

        var alphaMode = switch (info.type()) {
            case SOLID -> MaterialModelV2.AlphaMode.OPAQUE;
            case CUTOUT -> MaterialModelV2.AlphaMode.MASK;
            case TRANSLUCENT -> MaterialModelV2.AlphaMode.BLEND;
        };
        material.setAlphaMode(alphaMode);
        material.setDoubleSided(info.backface());

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
        var file = new File(FabricLoader.getInstance().getGameDir() + File.separator + "models" + File.separator + "model" + ++MODEL_COUNT + ".glb");
        var writer = new GltfModelWriter();
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        try {
            writer.writeBinary(model, file);
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

        var color = AccessorModels.createFloat4D(FloatBuffer.wrap(colors));
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

    public static Vector3f getCenter(BoundingBox box) {
        return new Vector3f(box.minX() + (box.maxX() - box.minX() + 1f) / 2f, box.minY() + (box.maxY() - box.minY() + 1f) / 2f, box.minZ() + (box.maxZ() - box.minZ() + 1f) / 2f);
    }

    public static Vector3f getCenter(BlockPos pos) {
        return new Vector3f(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
    }

}
