package io.github.lucaargolo.exporter.utils.info;

import com.mojang.blaze3d.vertex.VertexFormat;

public record RenderInfo(int glId, int normalGlId, int specularGlId, VertexFormat.Mode mode, Type type, boolean backface, boolean alpha) {

    public enum Type {
        SOLID,
        CUTOUT,
        TRANSLUCENT;

        public static Type fromName(String name) {
            if(name.toLowerCase().contains("translucent")) {
                return TRANSLUCENT;
            }else if(name.toLowerCase().contains("cutout")) {
                return CUTOUT;
            }else{
                return SOLID;
            }
        }
    }

    public ImageInfo image(boolean trim) {
        return new ImageInfo(this, ImageInfo.Type.BASE, trim);
    }

    public boolean equalsTexture(RenderInfo other) {
        return glId == other.glId && normalGlId == other.normalGlId && specularGlId == other.specularGlId;
    }

}
