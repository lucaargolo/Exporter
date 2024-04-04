package io.github.lucaargolo.exporter;

import java.util.Objects;

public record ImageInfo(RenderInfo render, Type type, boolean trim) {

    public enum Type {
        BASE,
        NORMAL,
        SPECULAR;
    }

    @Override
    public int hashCode() {
        return Objects.hash(render.glId(), render.normalGlId(), render.specularGlId(), type, trim);
    }

    @Override
    public boolean equals(Object obj) {
        //This is not accurate but works for what we need.
        //Trimmed images are never the same because they change depending on the captured data.
        if(trim)
            return false;
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if(obj instanceof ImageInfo other) {
            return render.equalsTexture(other.render) && type == other.type && trim == other.trim;
        }else{
            return false;
        }
    }

    public ImageInfo toNormal() {
        return new ImageInfo(this.render, Type.NORMAL, this.trim);
    }

    public ImageInfo toSpecular() {
        return new ImageInfo(this.render, Type.SPECULAR, this.trim);
    }

}
