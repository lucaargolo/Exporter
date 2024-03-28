package io.github.lucaargolo.exporter.compat.custom;

import io.github.lucaargolo.exporter.compat.Compat;
import net.coderbot.iris.texture.pbr.PBRTextureHolder;
import net.coderbot.iris.texture.pbr.PBRTextureManager;

public class IrisCompat extends Compat.Impl {

    @Override
    public int normalTexture(int glId) {
        PBRTextureHolder holder = PBRTextureManager.INSTANCE.getHolder(glId);
        if(holder != null) {
            return holder.getNormalTexture().getId();
        }
        return -1;
    }

    @Override
    public int specularTexture(int glId) {
        PBRTextureHolder holder = PBRTextureManager.INSTANCE.getHolder(glId);
        if(holder != null) {
            return holder.getSpecularTexture().getId();
        }
        return -1;
    }
}
