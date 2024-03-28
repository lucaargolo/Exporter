package io.github.lucaargolo.exporter;

public record RenderInfo(int glId, int normalGlId, int specularGlId, Type type, boolean backface, boolean alpha) {

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
