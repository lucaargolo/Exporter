package io.github.lucaargolo.exporter.utils.helper;

import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.impl.DefaultImageModel;
import de.javagl.jgltf.model.impl.DefaultTextureModel;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import io.github.lucaargolo.exporter.utils.ModelBuilder;
import io.github.lucaargolo.exporter.utils.info.ImageInfo;
import net.minecraft.util.Mth;
import org.joml.Vector2f;
import org.joml.Vector3i;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MaterialHelper {

    public static final Map<ImageInfo, MaterialModel> MATERIALS = new HashMap<>();
    public static final Map<ImageInfo, TextureModel> TEXTURES = new HashMap<>();
    public static final Map<ImageInfo, BufferedImage> IMAGES = new HashMap<>();

    private static final double[][] METALS_TABLE = {
            {2.9114, 2.9497, 2.5845, 3.0893, 2.9318, 2.7670}, // Iron
            {0.18299, 0.42108, 1.3734, 3.4242, 2.3459, 1.7704}, // Gold
            {1.3456, 0.96521, 0.61722, 7.4746, 6.3995, 5.3031}, // Aluminum
            {3.1071, 3.1812, 2.3230, 3.3314, 3.3291, 3.1350}, // Chrome
            {0.27105, 0.67693, 1.3164, 3.6092, 2.6248, 2.2921}, // Copper
            {1.9100, 1.8300, 1.4400, 3.5100, 3.4000, 3.1800}, // Lead
            {2.3757, 2.0847, 1.8453, 4.2655, 3.7153, 3.1365}, // Platinum
            {0.15943, 0.14512, 0.13547, 3.9291, 3.1900, 2.3808} // Silver
    };

    public static MaterialModel getMaterial(ImageInfo image) {
        return MATERIALS.computeIfAbsent(image, MaterialHelper::getMaterialInner);
    }


    public static float[] getUvs(ImageInfo image, float[] uvs) {
        List<Vector3i> capturedTriangles = ModelBuilder.getCapturedTriangles(image.render());
        List<Vector2f> capturedUvs = ModelBuilder.getCapturedUvs(image.render());
        return image.trim() ? TrimHelper.trimUvs(IMAGES.get(image), capturedTriangles, capturedUvs) : uvs;
    }


    private static MaterialModel getMaterialInner(ImageInfo image) {
        var material = new MaterialModelV2();
        if(image.render().glId() >= 0)
            material.setBaseColorTexture(TEXTURES.computeIfAbsent(image, MaterialHelper::getTexture));
        if(image.render().normalGlId() >= 0)
            material.setNormalTexture(TEXTURES.computeIfAbsent(image.toNormal(), MaterialHelper::getTexture));
        if(image.render().specularGlId() >= 0)
            material.setMetallicRoughnessTexture(TEXTURES.computeIfAbsent(image.toSpecular(), MaterialHelper::getTexture));
        var alphaMode = switch (image.render().type()) {
            case SOLID -> MaterialModelV2.AlphaMode.OPAQUE;
            case CUTOUT -> MaterialModelV2.AlphaMode.MASK;
            case TRANSLUCENT -> MaterialModelV2.AlphaMode.BLEND;
        };
        material.setAlphaMode(alphaMode);
        material.setDoubleSided(image.render().backface());

        return material;
    }

    private static TextureModel getTexture(ImageInfo image) {
        BufferedImage bufferedImage = IMAGES.computeIfAbsent(image, MaterialHelper::extractImage);
        ByteBuffer imageBuffer = trimBuffered(bufferedImage, image);

        var model = new DefaultImageModel();
        model.setImageData(imageBuffer);

        var texture = new DefaultTextureModel();
        texture.setImageModel(model);
        texture.setMinFilter(GL11.GL_NEAREST);
        texture.setMagFilter(GL11.GL_NEAREST);

        return texture;
    }


    private static ByteBuffer trimBuffered(BufferedImage bufferedImage, ImageInfo image) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            if(image.trim()) {
                List<Vector3i> capturedTriangles = ModelBuilder.getCapturedTriangles(image.render());
                List<Vector2f> capturedUvs = ModelBuilder.getCapturedUvs(image.render());
                BufferedImage trimmed = TrimHelper.trimImage(bufferedImage, capturedTriangles, capturedUvs);
                ImageIO.write(trimmed, "png", outputStream);
            }else{
                ImageIO.write(bufferedImage, "png", outputStream);
            }
            outputStream.flush();
            byte[] pngData = outputStream.toByteArray();
            outputStream.close();
            return ByteBuffer.wrap(pngData);
        }catch (Exception e) {
            return ByteBuffer.wrap(new byte[0]);
        }
    }

    private static BufferedImage extractImage(ImageInfo image) {
        int[] textureId = new int[1];
        GL11.glGetIntegerv(GL11.GL_TEXTURE_BINDING_2D, textureId);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, image.render().glId());
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        ByteBuffer buffer = BufferUtils.createByteBuffer(width * height * 4);
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, buffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId[0]);

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] pixels = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            int r = buffer.get() & 0xFF;
            int g = buffer.get() & 0xFF;
            int b = buffer.get() & 0xFF;
            int a = buffer.get() & 0xFF;

            if(image.type() == ImageInfo.Type.NORMAL) {
                float x = r / 255.0f;
                float y = 1f - (g / 255.0f);
                float z = (float) Math.sqrt(1.0f - (x * x + y * y));

                a = 255;
                r = (int) (x * 255);
                g = (int) (y * 255);
                b = (int) (z * 255);
            }else if(image.type() == ImageInfo.Type.SPECULAR) {
                double roughness = Math.pow(1.0 - (r / 255.0), 2.0);

                double metallic;
                if (g >= 230 && g <= 254) {
                    int metalIndex = Mth.clamp(g - 230, 0, METALS_TABLE.length-1);
                    double[] metalValues = METALS_TABLE[metalIndex];
                    metallic = (metalValues[0] + metalValues[1] + metalValues[2]) / 3.0;
                } else {
                    metallic = g / 255.0;
                }

                a = 255;
                r = 255;
                g = (int) (roughness * 255);
                b = (int) (metallic * 255);
            }

            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        img.setRGB(0, 0, width, height, pixels, 0, width);

        return img;
    }

    public static void clearCache() {
        MATERIALS.clear();
        TEXTURES.clear();
        IMAGES.clear();
    }

}
