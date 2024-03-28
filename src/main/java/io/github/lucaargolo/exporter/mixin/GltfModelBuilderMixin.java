package io.github.lucaargolo.exporter.mixin;

import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.creation.GltfModelBuilder;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = GltfModelBuilder.class, remap = false)
public abstract class GltfModelBuilderMixin {

    @Shadow public abstract void addTextureModel(TextureModel textureModel);

    @Inject(at = @At(value = "INVOKE", target = "Lde/javagl/jgltf/model/creation/GltfModelBuilder;addTextureModel(Lde/javagl/jgltf/model/TextureModel;)V", ordinal = 1, shift = At.Shift.AFTER), method = "addMaterialModel", locals = LocalCapture.CAPTURE_FAILSOFT)
    public void theyForgotToIncludeTheNormals(MaterialModel materialModel, CallbackInfo ci, boolean added, MaterialModelV2 materialModelV2) {
        addTextureModel(materialModelV2.getNormalTexture());
    }

}
