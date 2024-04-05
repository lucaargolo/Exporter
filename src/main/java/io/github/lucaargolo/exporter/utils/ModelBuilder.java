package io.github.lucaargolo.exporter.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.creation.AccessorModels;
import de.javagl.jgltf.model.creation.GltfModelBuilder;
import de.javagl.jgltf.model.impl.DefaultMeshModel;
import de.javagl.jgltf.model.impl.DefaultMeshPrimitiveModel;
import de.javagl.jgltf.model.impl.DefaultNodeModel;
import de.javagl.jgltf.model.impl.DefaultSceneModel;
import de.javagl.jgltf.model.io.GltfModelWriter;
import io.github.lucaargolo.exporter.ExporterClient;
import io.github.lucaargolo.exporter.utils.helper.MaterialHelper;
import io.github.lucaargolo.exporter.utils.info.RenderInfo;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

public class ModelBuilder {

    private static int MODEL_COUNT = 0;

    private static Matrix4f INVERTED_POSE;
    private static Matrix3f INVERTED_NORMAL;
    private static Vector3f VERTEX_POSITION = new Vector3f(0, 0, 0);

    private static Matrix4f BACKUP_POSE;
    private static Matrix3f BACKUP_NORMAL;
    private static Vector3f BACKUP_POSITION = new Vector3f(0, 0, 0);

    private static Matrix4f EXTRA_POSE;
    private static Matrix3f EXTRA_NORMAL;
    private static Vector3f EXTRA_POSITION = new Vector3f(0, 0, 0);

    private static final Set<RenderInfo> CAPTURED_INFO = new HashSet<>();

    private static final Map<RenderInfo, Integer> VERTICE_COUNT = new HashMap<>();
    private static final Map<RenderInfo, IntArrayList> VERTICE_HOLDER = new HashMap<>();
    private static final Map<RenderInfo, List<Vector3f>> CAPTURED_VERTICES = new HashMap<>();
    private static final Map<RenderInfo, List<Vector3f>> CAPTURED_NORMALS = new HashMap<>();
    private static final Map<RenderInfo, List<Vector4f>> CAPTURED_TANGENTS = new HashMap<>();
    private static final Map<RenderInfo, List<Vector2f>> CAPTURED_UVS = new HashMap<>();
    private static final Map<RenderInfo, List<Vector4f>> CAPTURED_COLORS = new HashMap<>();
    private static final Map<RenderInfo, List<Vector3i>> CAPTURED_TRIANGLES = new HashMap<>();

    private static final List<NodeModel> NODES = new ArrayList<>();

    private static boolean UV = false;
    private static boolean COLOR = false;
    private static boolean NORMAL = false;

    public static void clearMesh() {
        CAPTURED_INFO.clear();
        VERTICE_COUNT.clear();
        CAPTURED_VERTICES.clear();
        CAPTURED_NORMALS.clear();
        CAPTURED_TANGENTS.clear();
        CAPTURED_UVS.clear();
        CAPTURED_COLORS.clear();
        CAPTURED_TRIANGLES.clear();
    }

    public static void captureVertex(RenderInfo render, float x, float y, float z) {
        if(render.mode().primitiveLength < 3)
            return;

        CAPTURED_INFO.add(render);
        List<Vector3f> vertices = CAPTURED_VERTICES.computeIfAbsent(render, i -> new ArrayList<>());
        List<Vector3i> triangles = CAPTURED_TRIANGLES.computeIfAbsent(render, i -> new ArrayList<>());

        Vector4f reversedVertex = INVERTED_POSE != null ? INVERTED_POSE.transform(new Vector4f(x, y, z, 1f)) : new Vector4f(x, y, z, 1f);
        reversedVertex = EXTRA_POSE != null ? EXTRA_POSE.transform(reversedVertex) : reversedVertex;
        Vector3f capturedVertex = new Vector3f(reversedVertex.x, reversedVertex.y, reversedVertex.z).add(VERTEX_POSITION).add(EXTRA_POSITION);
        int index = vertices.size();
        vertices.add(capturedVertex);

        int verticeCount = VERTICE_COUNT.computeIfAbsent(render, i -> 0);
        IntArrayList verticeHolder = VERTICE_HOLDER.computeIfAbsent(render, i -> new IntArrayList(new int[4]));
        if(render.mode().primitiveLength == 4) {
            verticeHolder.set(verticeCount % 4, index);
            if ((verticeCount + 1) % 4 == 0) {
                int v0 = verticeHolder.getInt(0);
                int v1 = verticeHolder.getInt(1);
                int v2 = verticeHolder.getInt(2);
                int v3 = verticeHolder.getInt(3);
                triangles.add(new Vector3i(v0, v1, v2));
                triangles.add(new Vector3i(v3, v0, v2));
            }
        }else{
            verticeHolder.set(verticeCount % 3, index);
            if ((verticeCount + 1) % 3 == 0) {
                int v0 = verticeHolder.getInt(0);
                int v1 = verticeHolder.getInt(1);
                int v2 = verticeHolder.getInt(2);
                triangles.add(new Vector3i(v0, v1, v2));
            }
        }
        VERTICE_COUNT.put(render, verticeCount+1);

        COLOR = false;
        UV = false;
        NORMAL = false;
    }

    public static void captureNormal(RenderInfo render, float x, float y, float z) {
        if(render.mode().primitiveLength < 3)
            return;
        CAPTURED_INFO.add(render);
        List<Vector3f> normals = CAPTURED_NORMALS.computeIfAbsent(render, i -> new ArrayList<>());
        Vector3f reversedNormal = INVERTED_NORMAL != null ? INVERTED_NORMAL.transform(new Vector3f(x, y, z)) : new Vector3f(x, y, z);
        reversedNormal = EXTRA_NORMAL != null ? EXTRA_NORMAL.transform(reversedNormal) : reversedNormal;
        normals.add(reversedNormal.normalize());
        NORMAL = true;
    }

    public static void captureTangent(RenderInfo render, float x, float y, float z, float w) {
        if(render.mode().primitiveLength < 3)
            return;
        CAPTURED_INFO.add(render);
        List<Vector4f> tangents = CAPTURED_TANGENTS.computeIfAbsent(render, i -> new ArrayList<>());
        tangents.add(new Vector4f(x, y, z, w));
    }

    public static void captureUv(RenderInfo render, float u, float v) {
        if(render.mode().primitiveLength < 3)
            return;
        CAPTURED_INFO.add(render);
        List<Vector2f> uvs = CAPTURED_UVS.computeIfAbsent(render, i -> new ArrayList<>());
        uvs.add(new Vector2f(Mth.clamp(u, 0f, 1f), Mth.clamp(v, 0f, 1f)));
        UV = true;
    }

    public static void captureRgb(RenderInfo render, float r, float g, float b, float a) {
        if(render.mode().primitiveLength < 3)
            return;
        CAPTURED_INFO.add(render);
        List<Vector4f> colors = CAPTURED_COLORS.computeIfAbsent(render, i -> new ArrayList<>());
        colors.add(new Vector4f(Mth.clamp(r, 0f, 1f), Mth.clamp(g, 0f, 1f), Mth.clamp(b, 0f, 1f), render.alpha() ? Mth.clamp(a, 0f, 1f) : 1f));
        COLOR = true;
    }

    public static void endCapture(RenderInfo render) {
        if(render.mode().primitiveLength < 3)
            return;
        if(!NORMAL)
            captureNormal(render, 0f, 1f, 0f);
        if(!UV)
            captureUv(render, 0f, 0f);
        if(!COLOR)
            captureRgb(render, 1f, 1f, 1f, 1f);

    }

    public static MeshModel writeMesh(MaterialModel material, float[] vertices, float[] normals, float[] tangents, float[] texCoords, float[] colors, int[] indices) {

        var mesh = new DefaultMeshModel();

        var primitive = new DefaultMeshPrimitiveModel(GL11.GL_TRIANGLES);

        var position = AccessorModels.createFloat3D(FloatBuffer.wrap(vertices));
        primitive.putAttribute("POSITION", position);

        var normal = AccessorModels.createFloat3D(FloatBuffer.wrap(normals));
        primitive.putAttribute("NORMAL", normal);

        //Tangents can be empty if Iris is not present
        if(tangents.length > 0) {
            //var tangent = AccessorModels.createFloat4D(FloatBuffer.wrap(tangents));
            //primitive.putAttribute("TANGENT", tangent);
        }

        var texCoord = AccessorModels.createFloat2D(FloatBuffer.wrap(texCoords));
        primitive.putAttribute("TEXCOORD_0", texCoord);

        var color = AccessorModels.createFloat4D(FloatBuffer.wrap(colors));
        primitive.putAttribute("COLOR_0", color);


        var triangles = AccessorModels.createUnsignedIntScalar(IntBuffer.wrap(indices));
        primitive.setIndices(triangles);

        try {
            primitive.setMaterialModel(material);
        } catch (Exception e) {
            ExporterClient.LOGGER.error("Error while saving mesh: ", e);
        }

        mesh.addMeshPrimitiveModel(primitive);
        return mesh;

    }

    public static void writeCapturedNode(Vector3f translation) {
        var node = new DefaultNodeModel();
        node.setTranslation(new float[] {translation.x, translation.y, translation.z});
        for (RenderInfo render: CAPTURED_INFO) {
            float[] vertices = new float[CAPTURED_VERTICES.get(render).size() * 3];
            for (int i = 0; i < CAPTURED_VERTICES.get(render).size(); i++) {
                Vector3f vertex = CAPTURED_VERTICES.get(render).get(i);
                vertices[i * 3] = vertex.x;
                vertices[i * 3 + 1] = vertex.y;
                vertices[i * 3 + 2] = vertex.z;
            }
            float[] normals = new float[CAPTURED_NORMALS.get(render).size() * 3];
            for (int i = 0; i < CAPTURED_NORMALS.get(render).size(); i++) {
                Vector3f normal = CAPTURED_NORMALS.get(render).get(i);
                normals[i * 3] = normal.x;
                normals[i * 3 + 1] = normal.y;
                normals[i * 3 + 2] = normal.z;
            }
            //Tangents can be empty if Iris is not present
            int tangentSize = CAPTURED_TANGENTS.get(render) != null ? CAPTURED_TANGENTS.get(render).size() : 0;
            float[] tangents = new float[tangentSize * 4];
            for (int i = 0; i < tangentSize; i++) {
                Vector4f tangent = CAPTURED_TANGENTS.get(render).get(i);
                tangents[i * 4] = tangent.x;
                tangents[i * 4 + 1] = tangent.y;
                tangents[i * 4 + 2] = tangent.z;
                tangents[i * 4 + 3] = tangent.w;
            }
            float[] uvs = new float[CAPTURED_UVS.get(render).size() * 2];
            for (int i = 0; i < CAPTURED_UVS.get(render).size(); i++) {
                Vector2f uv = CAPTURED_UVS.get(render).get(i);
                uvs[i * 2] = uv.x;
                uvs[i * 2 + 1] = uv.y;
            }
            float[] colors = new float[CAPTURED_COLORS.get(render).size() * 4];
            for (int i = 0; i < CAPTURED_COLORS.get(render).size(); i++) {
                Vector4f color = CAPTURED_COLORS.get(render).get(i);
                colors[i * 4] = color.x;
                colors[i * 4 + 1] = color.y;
                colors[i * 4 + 2] = color.z;
                colors[i * 4 + 3] = color.w;
            }
            int[] triangles = new int[CAPTURED_TRIANGLES.get(render).size() * 3];
            for (int i = 0; i < CAPTURED_TRIANGLES.get(render).size(); i++) {
                Vector3i triangle = CAPTURED_TRIANGLES.get(render).get(i);
                triangles[i * 3] = triangle.x;
                triangles[i * 3 + 1] = triangle.y;
                triangles[i * 3 + 2] = triangle.z;
            }
            try {
                var child = new DefaultNodeModel();
                var image = render.image(false);
                var material = MaterialHelper.getMaterial(image);
                var coords = MaterialHelper.getUvs(image, uvs);
                MeshModel mesh = writeMesh(material, vertices, normals, tangents, coords, colors, triangles);
                child.addMeshModel(mesh);
                node.addChild(child);
            } catch (Exception e) {
                ExporterClient.LOGGER.error("Error while saving node: ", e);
            }
        }
        NODES.add(node);
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
        while (file.exists()) {
            file = new File(FabricLoader.getInstance().getGameDir() + File.separator + "models" + File.separator + "model" + ++MODEL_COUNT + ".glb");
        }
        var writer = new GltfModelWriter();
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        try {
            writer.writeBinary(model, file);
            if(player != null)
                player.displayClientMessage(Component.literal("Successfully saved model at "+file+" with "+model.getMeshModels().size()+" meshes.").withStyle(ChatFormatting.GREEN), false);
        } catch (Exception e) {
            ExporterClient.LOGGER.error("Error while saving model: ", e);
            if(player != null)
                player.displayClientMessage(Component.literal("Error while saving model. Please check logs.").withStyle(ChatFormatting.RED), false);
        }

        NODES.clear();
        MaterialHelper.clearCache();
    }

    public static void setPosition(Vector3f position) {
        VERTEX_POSITION = position;
    }

    public static void setupInvertedPose(PoseStack poseStack) {
        INVERTED_POSE = poseStack.last().pose().invert(new Matrix4f());
        INVERTED_NORMAL = poseStack.last().normal().invert(new Matrix3f());
    }

    public static void clearInvertedPose() {
        INVERTED_POSE = null;
        INVERTED_NORMAL = null;
    }

    public static void backupPose() {
        BACKUP_POSE = INVERTED_POSE;
        BACKUP_NORMAL = INVERTED_NORMAL;
        BACKUP_POSITION = VERTEX_POSITION;
    }

    public static void restorePose() {
        INVERTED_POSE = BACKUP_POSE;
        INVERTED_NORMAL = BACKUP_NORMAL;
        VERTEX_POSITION = BACKUP_POSITION;
    }

    public static void setExtraPosition(Vector3f position) {
        EXTRA_POSITION = position;
    }

    public static void setupExtraPose(PoseStack poseStack) {
        EXTRA_POSE = poseStack.last().pose();
        EXTRA_NORMAL = poseStack.last().normal();
    }

    public static void clearExtraPose() {
        EXTRA_POSE = null;
        EXTRA_NORMAL = null;
    }

    public static @Nullable List<Vector3i> getCapturedTriangles(RenderInfo render) {
        return CAPTURED_TRIANGLES.get(render);
    }

    public static @Nullable List<Vector2f> getCapturedUvs(RenderInfo render) {
        return CAPTURED_UVS.get(render);
    }

}
