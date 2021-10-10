package net.mehvahdjukaar.selene.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.context.UseOnContext;

import java.util.Random;


public class Utils {

    public static void swapItem( Player player, InteractionHand hand, ItemStack oldItem, ItemStack newItem, boolean bothsides){
        if(!player.level.isClientSide || bothsides)
            player.setItemInHand(InteractionHand, ItemUtils.createFilledResult(oldItem.copy(), player, newItem, player.isCreative()));
    }
    public static void swapItem(Player player, InteractionHand hand, ItemStack oldItem, ItemStack newItem){
        if(!player.level.isClientSide)
        player.setItemInHand(InteractionHand, ItemUtils.createFilledResult(oldItem.copy(), player, newItem, player.isCreative()));
    }
    public static void swapItemNBT(Player player, InteractionHand hand, ItemStack oldItem, ItemStack newItem){
        if(!player.level.isClientSide)
            player.setItemInHand(InteractionHand, ItemUtils.createFilledResult(oldItem.copy(), player, newItem,false));
    }
    public static void swapItem(Player player, InteractionHand hand, ItemStack newItem){
        if(!player.level.isClientSide)
        player.setItemInHand(InteractionHand, ItemUtils.createFilledResult(player.getItemInHand(hand).copy(), player, newItem, player.isCreative()));
    }
    //xp bottle logic
    public static int getXPinaBottle(int bottleCount, Random rand){
        int xp = 0;
        for(int i = 0; i<bottleCount; i++) xp += (3 + rand.nextInt(5) + rand.nextInt(5));
        return xp;
    }

}