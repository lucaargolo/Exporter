package io.github.lucaargolo.exporter;

public record ImageInfo(RenderInfo render, Type type, boolean trim) {

    public enum Type {
        BASE,
        NORMAL,
        SPECULAR;
    }

    public ImageInfo toNormal() {
        return new ImageInfo(this.render, Type.NORMAL, this.trim);
    }

    public ImageInfo toSpecular() {
        return new ImageInfo(this.render, Type.SPECULAR, this.trim);
    }

}
