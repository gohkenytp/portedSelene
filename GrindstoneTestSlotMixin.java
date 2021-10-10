package net.mehvahdjukaar.selene.mixins;


import net.mehvahdjukaar.selene.common.ModCriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"net.minecraft.world.inventory.GrindstoneMenu$4"})
public abstract class GrindstoneTestSlotMixin {

    private void onTake(Player player, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if(player instanceof ServerPlayer)
            ModCriteriaTriggers.GRIND.trigger((ServerPlayer)player, stack.copy());
    }

}
