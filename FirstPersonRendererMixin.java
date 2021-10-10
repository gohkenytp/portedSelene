package net.mehvahdjukaar.selene.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.selene.api.IFirstPersonAnimationProvider;
import net.mehvahdjukaar.selene.map.CustomDecorationHolder;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.saveddata.SavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class FirstPersonRendererMixin {

    public void renderItem(AbstractClientPlayer f3, float f4, float f9, InteractionHand f13, float f, ItemStack f1, float f2, PoseStack flag1, MultiBufferSource flag2, int i, CallbackInfo ci) {
        Item item = f1.getItem();
        if (item instanceof IFirstPersonAnimationProvider) {
            ((IFirstPersonAnimationProvider) item).animateItemFirstPerson(entity, f1, hand, PoseStack, partialTicks, pitch, attackAnim, handHeight);
        }
    }

}
