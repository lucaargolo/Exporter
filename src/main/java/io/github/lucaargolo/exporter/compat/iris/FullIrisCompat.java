package io.github.lucaargolo.exporter.compat.iris;

import io.github.lucaargolo.exporter.utils.ModelBuilder;
import io.github.lucaargolo.exporter.utils.info.RenderInfo;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.coderbot.batchedentityrendering.impl.WrappableRenderType;
import net.coderbot.iris.compat.sodium.impl.vertex_format.IrisCommonVertexAttributes;
import net.coderbot.iris.texture.pbr.PBRTextureHolder;
import net.coderbot.iris.texture.pbr.PBRTextureManager;
import net.coderbot.iris.vertices.NormI8;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.system.MemoryUtil;

public class FullIrisCompat extends IrisCompat.Impl {

    @Override
    public int normalTexture(int glId) {
        PBRTextureHolder holder = PBRTextureManager.INSTANCE.getHolder(glId);
        if(holder != null) {
            return holder.getNormalTexture().getId();
        }
        return super.normalTexture(glId);
    }

    @Override
    public int specularTexture(int glId) {
        PBRTextureHolder holder = PBRTextureManager.INSTANCE.getHolder(glId);
        if(holder != null) {
            return holder.getSpecularTexture().getId();
        }
        return super.specularTexture(glId);
    }

    @Override
    public void captureTangent(RenderInfo info, Object object, long pointer) {
        if(object instanceof VertexFormatDescription format && format.containsElement(IrisCommonVertexAttributes.TANGENT)) {
            long tangentOffset = format.getElementOffset(IrisCommonVertexAttributes.TANGENT);
            int tangent = MemoryUtil.memGetInt(pointer + tangentOffset);
            ModelBuilder.captureTangent(info, NormI8.unpackX(tangent), NormI8.unpackY(tangent), NormI8.unpackZ(tangent), NormI8.unpackW(tangent));
        }
    }

    @Override
    public boolean isPresent() {
        return true;
    }

    @Override
    public RenderType unwrapRenderType(RenderType renderType) {
        if(renderType instanceof WrappableRenderType wrappable) {
            return wrappable.unwrap();
        }else{
            return renderType;
        }
    }
}
