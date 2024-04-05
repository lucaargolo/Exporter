package io.github.lucaargolo.exporter.mixin;

import io.github.lucaargolo.exporter.ExporterCommand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {


    @Shadow @Final private Minecraft minecraft;

    @Inject(at = @At("HEAD"), method = "useItemOn", locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    public void atUse(LocalPlayer player, InteractionHand hand, BlockHitResult result, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack itemStack = player.getItemInHand(hand);
        if(itemStack.is(Items.WOODEN_HOE)) {
            CompoundTag tag = itemStack.getTag();
            if(tag != null && tag.contains("exporter_wand")) {
                player.swing(InteractionHand.MAIN_HAND);
                ExporterCommand.SECOND_POS = result.getBlockPos().immutable();
                player.displayClientMessage(Component.literal("Set second position to "+ExporterCommand.SECOND_POS.toShortString()), false);
                this.minecraft.options.keyUse.setDown(false);
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
        }
    }

}
