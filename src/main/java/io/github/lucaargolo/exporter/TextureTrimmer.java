package io.github.lucaargolo.exporter;

import com.mojang.datafixers.util.Pair;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2f;
import org.joml.Vector3i;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class TextureTrimmer {

    private record TextureReference(int t0, int t1, int t2) {}

    private record TexturePart(int startX, int startY, int width, int height) {}

    public static BufferedImage trimImage(BufferedImage original, List<Vector3i> triangles, List<Vector2f> uvs) {
        return trimTextureAndUvs(original, triangles, uvs, true).getFirst();
    }

    public static float[] trimUvs(BufferedImage original, List<Vector3i> triangles, List<Vector2f> uvs) {
        return trimTextureAndUvs(original, triangles, uvs, false).getSecond();
    }

    private static Pair<BufferedImage, float[]> trimTextureAndUvs(BufferedImage original, List<Vector3i> triangles, List<Vector2f> uvs, boolean image) {
        List<Pair<TexturePart, TextureReference>> pairs = getTextureParts(original, triangles, uvs);
        HashMap<TexturePart, TexturePart> updated = new HashMap<>();

        int atlasWidth = 0;
        int atlasHeight = 0;
        int rowHeight = 0;

        for (Pair<TexturePart, TextureReference> pair : pairs) {
            TexturePart part = pair.getFirst();
            if (!updated.containsKey(part)) {
                atlasWidth += pair.getFirst().width();
                atlasHeight = Math.max(atlasHeight, pair.getFirst().height());
                updated.put(part, part);
            }
        }
        updated.clear();

        BufferedImage atlas = null;
        Graphics2D g2d = null;
        if(image) {
            atlas = new BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_INT_ARGB);
            g2d = atlas.createGraphics();
        }

        int currentX = 0;
        int currentY = 0;

        for (Pair<TexturePart, TextureReference> pair : pairs) {
            TexturePart part = pair.getFirst();
            if (!updated.containsKey(part)) {

                BufferedImage partImage = null;
                if(image) {
                    partImage = new BufferedImage(part.width(), part.height(), BufferedImage.TYPE_INT_ARGB);
                    for (int x = 0; x < part.width(); x++) {
                        for (int y = 0; y < part.height(); y++) {
                            partImage.setRGB(x, y, original.getRGB(part.startX() + x, part.startY() + y));
                        }
                    }
                }

                if (currentX + part.width() <= atlasWidth) {
                    updated.put(part, new TexturePart(currentX, currentY, part.width(), part.height()));
                    if(image)
                        g2d.drawImage(partImage, currentX, currentY, null);
                    currentX += part.width();
                    rowHeight = Math.max(rowHeight, part.height());
                } else {
                    currentY += rowHeight;
                    currentX = 0;
                    rowHeight = part.height();
                    updated.put(part, new TexturePart(currentX, currentY, part.width(), part.height()));
                    if(image)
                        g2d.drawImage(partImage, currentX, currentY, null);
                    currentX += part.width();
                }
            }
        }
        if(image)
            g2d.dispose();

        float[] texCoords = new float[uvs.size() * 2];
        if(!image) {
            for (Pair<TexturePart, TextureReference> pair : pairs) {
                TexturePart oldPart = pair.getFirst();
                TexturePart newPart = updated.get(oldPart);
                TextureReference reference = pair.getSecond();

                float minU = (float) oldPart.startX() / original.getWidth();
                float minV = (float) oldPart.startY() / original.getHeight();
                float maxU = (float) (oldPart.startX() + oldPart.width()) / original.getWidth();
                float maxV = (float) (oldPart.startY() + oldPart.height()) / original.getHeight();

                float newMinU = (float) newPart.startX() / atlasWidth;
                float newMinV = (float) newPart.startY() / atlasHeight;
                float newMaxU = (float) (newPart.startX() + newPart.width()) / atlasWidth;
                float newMaxV = (float) (newPart.startY() + newPart.height()) / atlasHeight;

                float scaleX = (newMaxU - newMinU) / (maxU - minU);
                float scaleY = (newMaxV - newMinV) / (maxV - minV);

                texCoords[reference.t0 * 2] = newMinU + (uvs.get(reference.t0).x - minU) * scaleX;
                texCoords[reference.t0 * 2 + 1] = newMinV + (uvs.get(reference.t0).y - minV) * scaleY;
                texCoords[reference.t1 * 2] = newMinU + (uvs.get(reference.t1).x - minU) * scaleX;
                texCoords[reference.t1 * 2 + 1] = newMinV + (uvs.get(reference.t1).y - minV) * scaleY;
                texCoords[reference.t2 * 2] = newMinU + (uvs.get(reference.t2).x - minU) * scaleX;
                texCoords[reference.t2 * 2 + 1] = newMinV + (uvs.get(reference.t2).y - minV) * scaleY;
            }
        }

        return new Pair<>(atlas, texCoords);
    }

    @NotNull
    private static List<Pair<TexturePart, TextureReference>> getTextureParts(BufferedImage original, List<Vector3i> triangles, List<Vector2f> uvs) {
        int width = original.getWidth();
        int height = original.getHeight();

        List<Pair<TexturePart, TextureReference>> parts = new ArrayList<>();

        for (Vector3i triangle : triangles) {
            int t0 = triangle.x;
            int t1 = triangle.y;
            int t2 = triangle.z;

            float u0 = uvs.get(t0).x;
            float v0 = uvs.get(t0).y;

            float u1 = uvs.get(t1).x;
            float v1 = uvs.get(t1).y;

            float u2 = uvs.get(t2).x;
            float v2 = uvs.get(t2).y;

            float minU = Math.min(u0, Math.min(u1, u2));
            float minV = Math.min(v0, Math.min(v1, v2));
            float maxU = Math.max(u0, Math.max(u1, u2));
            float maxV = Math.max(v0, Math.max(v1, v2));

            int innerWidth = Mth.ceil((maxU - minU) * width);
            int innerHeight = Mth.ceil((maxV - minV) * height);
            int startX = Mth.floor(minU * width);
            int startY = Mth.floor(minV * height);

            TexturePart part = new TexturePart(startX, startY, innerWidth, innerHeight);
            TextureReference reference = new TextureReference(t0, t1, t2);
            parts.add(new Pair<>(part, reference));
        }

        parts.sort(Comparator.comparing(Pair::getFirst, Comparator.comparingInt(TexturePart::startX).thenComparingInt(TexturePart::startY)));

        return parts;
    }


}
