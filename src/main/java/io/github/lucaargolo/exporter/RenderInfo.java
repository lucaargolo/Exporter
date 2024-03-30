package io.github.lucaargolo.exporter;

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

}
