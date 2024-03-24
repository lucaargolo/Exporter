package io.github.lucaargolo.exporter;

import com.mojang.blaze3d.vertex.VertexConsumer;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.creation.AccessorModels;
import de.javagl.jgltf.model.creation.GltfModelBuilder;
import de.javagl.jgltf.model.impl.*;
import de.javagl.jgltf.model.io.GltfModelWriter;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExporterClient implements ClientModInitializer {

    private static int MODEL_COUNT = 0;

    public static int MARKED_ENTITY = -1;

    public static MultiBufferSource MARKED_BUFFER;
    public static final Object2IntMap<VertexConsumer> MARKED_CONSUMERS = new Object2IntArrayMap<>();


    public static final IntArraySet CAPTURED_IMAGES = new IntArraySet();
    public static final Int2IntArrayMap VERTICE_COUNT = new Int2IntArrayMap();
    public static final Int2ObjectArrayMap<IntArrayList> VERTICE_HOLDER = new Int2ObjectArrayMap<>();
    public static final Int2ObjectArrayMap<List<Vector3f>> CAPTURED_VERTICES = new Int2ObjectArrayMap<>();
    public static final Int2ObjectArrayMap<List<Vector3i>> CAPTURED_TRIANGLES = new Int2ObjectArrayMap<>();
    public static final Int2ObjectArrayMap<List<Vector2f>> CAPTURED_UVS = new Int2ObjectArrayMap<>();

    @Override
    public void onInitializeClient() {
    }

    public static void captureVertex(int glID, float x, float y, float z, float u, float v) {
        CAPTURED_IMAGES.add(glID);

        List<Vector3f> vertices = ExporterClient.CAPTURED_VERTICES.computeIfAbsent(glID, i -> new ArrayList<>());
        List<Vector3i> triangles = ExporterClient.CAPTURED_TRIANGLES.computeIfAbsent(glID, i -> new ArrayList<>());
        List<Vector2f> uvs = ExporterClient.CAPTURED_UVS.computeIfAbsent(glID, i -> new ArrayList<>());

        Vector3f capturedVertex = new Vector3f(x, y, z);
        int index = vertices.size();
        vertices.add(capturedVertex);
        uvs.add(new Vector2f(u, v));

        int verticeCount = ExporterClient.VERTICE_COUNT.computeIfAbsent(glID, i -> 0);
        IntArrayList verticeHolder = ExporterClient.VERTICE_HOLDER.computeIfAbsent(glID, i -> new IntArrayList(new int[4]));
        verticeHolder.set(verticeCount % 4, index);
        if((verticeCount+1) % 4 == 0) {
            int a = verticeHolder.getInt(0);
            int b = verticeHolder.getInt(1);
            int c = verticeHolder.getInt(2);
            int d = verticeHolder.getInt(3);
            triangles.add(new Vector3i(a, b, c));
            triangles.add(new Vector3i(d, a, c));
        }
        ExporterClient.VERTICE_COUNT.put(glID, verticeCount+1);
    }

    public static ByteBuffer extractTexture(int glId)  throws IOException {
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

    public static ByteBuffer convertToPNG(ByteBuffer rgbaData, int width, int height) throws IOException {
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
        ImageIO.write(img, "png", baos);
        baos.flush();
        byte[] pngData = baos.toByteArray();
        baos.close();

        return ByteBuffer.wrap(pngData);
    }

    public static void writeCapturedModel() {
        var builder = GltfModelBuilder.create();

        var scene = new DefaultSceneModel();

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
            int[] triangles = new int[CAPTURED_TRIANGLES.get(glId).size() * 3];
            for (int i = 0; i < CAPTURED_TRIANGLES.get(glId).size(); i++) {
                Vector3i triangle = CAPTURED_TRIANGLES.get(glId).get(i);
                triangles[i * 3] = triangle.x;
                triangles[i * 3 + 1] = triangle.y;
                triangles[i * 3 + 2] = triangle.z;
            }
            try {
                var node = new DefaultNodeModel();
                ByteBuffer imageBuffer = extractTexture(glId);
                MeshModel mesh = writeMesh(imageBuffer, vertices, uvs, triangles);
                node.addMeshModel(mesh);
                scene.addNode(node);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        builder.addSceneModel(scene);


        var model = builder.build();
        var file = new File(FabricLoader.getInstance().getGameDir() + File.separator + "model" + ++MODEL_COUNT + ".gltf");
        var writer = new GltfModelWriter();
        try {
            writer.writeEmbedded(model, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static MeshModel writeMesh(ByteBuffer imageBuffer, float[] vertices, float[] texCoords, int[] indices) {

        var mesh = new DefaultMeshModel();

        var primitive = new DefaultMeshPrimitiveModel(GL11.GL_TRIANGLES);

        var position = AccessorModels.createFloat3D(FloatBuffer.wrap(vertices));
        primitive.putAttribute("POSITION", position);

        var texCoord = AccessorModels.createFloat2D(FloatBuffer.wrap(texCoords));
        primitive.putAttribute("TEXCOORD_0", texCoord);

        var triangles = AccessorModels.createUnsignedIntScalar(IntBuffer.wrap(indices));
        primitive.setIndices(triangles);

        try {
            var image = new DefaultImageModel();
            image.setImageData(imageBuffer);

            var texture = new DefaultTextureModel();
            texture.setImageModel(image);
            texture.setMinFilter(GL11.GL_NEAREST);
            texture.setMagFilter(GL11.GL_NEAREST);

            var material = new MaterialModelV2();
            material.setBaseColorTexture(texture);
            material.setAlphaMode(MaterialModelV2.AlphaMode.MASK);
            material.setDoubleSided(true);

            primitive.setMaterialModel(material);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mesh.addMeshPrimitiveModel(primitive);
        return mesh;

    }



}
