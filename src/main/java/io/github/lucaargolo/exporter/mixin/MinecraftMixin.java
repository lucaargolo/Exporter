package io.github.lucaargolo.exporter.mixin;

import io.github.lucaargolo.exporter.ExporterCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Shadow @Nullable public LocalPlayer player;

    @Shadow @Final public Options options;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;", ordinal = 0), method = "startAttack", locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    public void atAttack(CallbackInfoReturnable<Boolean> cir, ItemStack itemStack, boolean bl, BlockHitResult blockHitResult, BlockPos blockPos) {
        if(player != null && itemStack.is(Items.WOODEN_HOE)) {
            CompoundTag tag = itemStack.getTag();
            if(tag != null && tag.contains("exporter_wand")) {
                player.swing(InteractionHand.MAIN_HAND);
                ExporterCommand.FIRST_POS = blockPos.immutable();
                player.displayClientMessage(Component.literal("Set first position to "+ExporterCommand.FIRST_POS.toShortString()), false);
                this.options.keyAttack.setDown(false);
                cir.setReturnValue(true);
            }
        }
    }

}
