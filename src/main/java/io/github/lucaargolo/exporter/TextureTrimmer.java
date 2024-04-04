package io.github.lucaargolo.exporter;

import com.mojang.datafixers.util.Pair;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class TextureTrimmer {

    private record TextureReference(int t0, int t1, int t2) {}

    private record TexturePart(int startX, int startY, int width, int height) {}

    public static Pair<BufferedImage, float[]> trimTexture(BufferedImage original, int[] indices, float[] texCoords) {

        List<Pair<TexturePart, TextureReference>> pairs = getTextureParts(original, indices, texCoords);
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

        BufferedImage atlas = new BufferedImage(atlasWidth, atlasHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = atlas.createGraphics();

        int currentX = 0;
        int currentY = 0;

        for (Pair<TexturePart, TextureReference> pair : pairs) {
            TexturePart part = pair.getFirst();
            if (!updated.containsKey(part)) {
                BufferedImage partImage = new BufferedImage(part.width(), part.height(), BufferedImage.TYPE_INT_ARGB);
                for (int x = 0; x < part.width(); x++) {
                    for (int y = 0; y < part.height(); y++) {
                        partImage.setRGB(x, y, original.getRGB(part.startX() + x, part.startY() + y));
                    }
                }

                if (currentX + part.width() <= atlasWidth) {
                    updated.put(part, new TexturePart(currentX, currentY, part.width(), part.height()));
                    g2d.drawImage(partImage, currentX, currentY, null);
                    currentX += part.width();
                    rowHeight = Math.max(rowHeight, part.height());
                } else {
                    currentY += rowHeight;
                    currentX = 0;
                    rowHeight = part.height();
                    updated.put(part, new TexturePart(currentX, currentY, part.width(), part.height()));
                    g2d.drawImage(partImage, currentX, currentY, null);
                    currentX += part.width();
                }
            }
        }
        g2d.dispose();

        float[] newTexCoords = new float[texCoords.length];
        for (Pair<TexturePart, TextureReference> pair : pairs) {
            TexturePart oldPart = pair.getFirst();
            TexturePart newPart = updated.get(oldPart);
            TextureReference reference = pair.getSecond();

            float minU = (float) oldPart.startX() / original.getWidth();
            float minV = (float) oldPart.startY() / original.getHeight();
            float maxU = (float) (oldPart.startX() + oldPart.width()) / original.getWidth();
            float maxV = (float) (oldPart.startY() + oldPart.height()) / original.getHeight();

            float newMinU = (float) newPart.startX() / atlas.getWidth();
            float newMinV = (float) newPart.startY() / atlas.getHeight();
            float newMaxU = (float) (newPart.startX() + newPart.width()) / atlas.getWidth();
            float newMaxV = (float) (newPart.startY() + newPart.height()) / atlas.getHeight();

            float scaleX = (newMaxU - newMinU) / (maxU - minU);
            float scaleY = (newMaxV - newMinV) / (maxV - minV);

            newTexCoords[reference.t0 * 2] = newMinU + (texCoords[reference.t0 * 2] - minU) * scaleX;
            newTexCoords[reference.t0 * 2 + 1] = newMinV + (texCoords[reference.t0 * 2 + 1] - minV) * scaleY;
            newTexCoords[reference.t1 * 2] = newMinU + (texCoords[reference.t1 * 2] - minU) * scaleX;
            newTexCoords[reference.t1 * 2 + 1] = newMinV + (texCoords[reference.t1 * 2 + 1] - minV) * scaleY;
            newTexCoords[reference.t2 * 2] = newMinU + (texCoords[reference.t2 * 2] - minU) * scaleX;
            newTexCoords[reference.t2 * 2 + 1] = newMinV + (texCoords[reference.t2 * 2 + 1] - minV) * scaleY;
        }

        return new Pair<>(atlas, newTexCoords);
    }

    @NotNull
    private static List<Pair<TexturePart, TextureReference>> getTextureParts(BufferedImage original, int[] indices, float[] texCoords) {
        int width = original.getWidth();
        int height = original.getHeight();

        List<Pair<TexturePart, TextureReference>> parts = new ArrayList<>();

        for (int i = 0; i < indices.length; i += 3) {
            int t0 = indices[i];
            int t1 = indices[i + 1];
            int t2 = indices[i + 2];

            float u0 = texCoords[t0 * 2];
            float v0 = texCoords[t0 * 2 + 1];

            float u1 = texCoords[t1 * 2];
            float v1 = texCoords[t1 * 2 + 1];

            float u2 = texCoords[t2 * 2];
            float v2 = texCoords[t2 * 2 + 1];

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
