package net.mehvahdjukaar.selene.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.selene.api.IThirdPersonAnimationProvider;
import net.mehvahdjukaar.selene.util.TwoHandedAnimation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class ThirdPersonRendererMixin<T extends LivingEntity> extends AgeableListModel<T> {

    public TwoHandedAnimation animationType = new TwoHandedAnimation();

    @Inject(method = "poseRightArm", at = @At(value = "HEAD"), cancellable = true)
    public void poseRightArm(T entity, CallbackInfo ci) {
        //cancel off hand animation if two handed so two handed animation always happens last
        if (this.animationType.isTwoHanded()) ci.cancel();
        HumanoidArm HumanoidArm = entity.getMainArm();
        ItemStack stack = entity.getItemInHand(HumanoidArm == HumanoidArm.RIGHT ? Hand.MAIN_HAND : Hand.OFF_HAND);
        Item item = stack.getItem();
        if (item instanceof IThirdPersonAnimationProvider) {
            if (((IThirdPersonAnimationProvider) item).poseRightArmGeneric(stack, this, entity, HumanoidArm, this.animationType)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "poseLeftArm", at = @At(value = "HEAD"), cancellable = true)
    public void poseLeftArm(T entity, CallbackInfo ci) {
        //cancel off hand animation if two handed so two handed animation always happens last
        if (this.animationType.isTwoHanded()) ci.cancel();
        HumanoidArm HumanoidArm = entity.getMainArm();
        ItemStack stack = entity.getItemInHand(HumanoidArm == HumanoidArm.RIGHT ? Hand.OFF_HAND : Hand.MAIN_HAND);
        Item item = stack.getItem();
        if (item instanceof IThirdPersonAnimationProvider) {
            if (((IThirdPersonAnimationProvider) item).poseLeftArmGeneric(stack, this, entity, HumanoidArm, this.animationType)) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "setupAnim", at = @At(value = "RETURN"), cancellable = true)
    public void setupAnim(T p_225597_1_, float p_225597_2_, float p_225597_3_, float p_225597_4_, float p_225597_5_, float p_225597_6_, CallbackInfo ci) {
        this.animationType.setTwoHanded(false);
    }

}
