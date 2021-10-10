package net.mehvahdjukaar.selene.mixins;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.MapExtendingRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MapExtendingRecipe.class)
public abstract class MapExtendingRecipeMixin {



    private boolean matches(ItemStack original, CraftingContainer inventory, Level level) {
        CompoundTag CompoundTag = original.getTag();
        if (CompoundTag != null && CompoundTag.contains("CustomDecorations", 9)) {
            return true;
        }
        return original.isEmpty();
    }

}