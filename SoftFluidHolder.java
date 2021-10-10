package net.mehvahdjukaar.selene.fluids;

import net.mehvahdjukaar.selene.fluids.client.FluidParticleColors;
import net.mehvahdjukaar.selene.util.PotionNBTHelper;
import net.mehvahdjukaar.selene.util.Utils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.alchemy.*;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * instance this fluid tank in your tile entity
 */
@SuppressWarnings("unused")
public class SoftFluidHolder {
    public static final int BOTTLE_COUNT = 1;
    public static final int BOWL_COUNT = 2;
    public static final int BUCKET_COUNT = 4;

    //count in bottles
    private int count = 0;
    private final int capacity;
    @Nullable
    private CompoundTag nbt = null;
    private SoftFluid fluid = SoftFluidRegistry.EMPTY;
    //special tint color. Used for dynamic tint fluids like water and potions
    private int specialColor = 0;
    private boolean needColorRefresh = true;
    private net.minecraft.world.InteractionHand InteractionHand;

    public SoftFluidHolder(int capacity) {
        this.capacity = capacity;
    }

    /**
     * call this method from your block when player interacts. tries to fill or empty current held item in tank
     *
     * @param player player
     * @param hand   hand
     * @return interaction successful
     */
    public boolean interactWithPlayer(Player player, InteractionHand handIn, @Nullable Level level, @Nullable BlockPos pos) {
        ItemStack handStack = player.getItemInHand(InteractionHand);

        ItemStack returnStack = this.interactWithItem(InteractionHand, level, pos, false);
        //for items that have no bottle
        if (returnStack != null) {
            Utils.swapItem(player, InteractionHand, returnStack);

            if (!handStack.isEmpty()) player.awardStat(Stats.ITEM_USED.get(handStack.getItem()));
            return true;
        }
        return false;
    }

    /**
     * makes current item interact with fluid tank. returns empty stack if
     *
     * @param stack ItemStack to be interacted with
     * @param Level level. null if no sound is to be played
     * @param pos   position. null if no sound is to be played
     * @return resulting ItemStack: empty for empty hand return, null if it failed
     */
    @Nullable
    public ItemStack interactWithItem( InteractionHand stack, @Nullable Level level, @Nullable BlockPos pos, boolean simulate) {

        ItemStack returnStack;
        //try filling
        returnStack = this.tryFillingItem(stack.getItem(), level, pos, simulate);
        if (returnStack != null) return returnStack;
        //try emptying
        returnStack = this.tryDrainItem(stack, level, pos, simulate);

        return returnStack;
    }

    //handles special nbt items such as potions or soups
    private void applyNBTtoItemStack(ItemStack stack) {
        String[] nbtKey = this.fluid.getNbtKeyFromItem();
        if (this.nbt != null && !this.nbt.isEmpty() && nbtKey != null) {
            CompoundTag newCom = new CompoundTag();
            for (String s : nbtKey) {
                //ignores bottle tag, handled separately since it's a diff item
                if (this.nbt.contains(s) && !s.equals("Bottle")) {
                    newCom.put(s, this.nbt.get(s));
                }
            }
            if(!newCom.isEmpty())stack.setTag(newCom);
        }
    }

    //same syntax as create
    private void addPotionTag(Item i, CompoundTag com) {
        String type = "REGULAR";
        if (i instanceof SplashPotionItem) type = "SPLASH";
        else if (i instanceof LingeringPotionItem) type = "LINGERING";
        com.putString("Bottle", type);
    }

    /**
     * tries pouring the content of provided item in the tank
     * also plays sound
     *
     * @return empty container item, null if it failed
     */
    @Nullable
    public ItemStack tryDrainItem(ItemStack filledContainerStack, @Nullable Level level, @Nullable BlockPos pos, boolean simulate) {

        //TODO: generalize this adding a function list that converts items in compounts and fluid pair or a compound whitelist
        //TODO: all of this  is horrible

        Item filledContainer = filledContainerStack.getItem();

        /*
        if(filledContainer instanceof ISoftFluidContainerItem){
            ISoftFluidContainerItem p = ((ISoftFluidContainerItem) filledContainer);
            ResourceLocation r = p.getSoftFluid();
            SoftFluid s = SoftFluidRegistry.get(r.toString());
            if(!s.isEmpty()) {
                CompoundTag nbt = p.getFluidNBT();
                int am = p.getAmount();
                if (this.isEmpty()) {
                    this.setFluid(s, nbt);
                }
                if (this.canAddSoftFluid(s, am, nbt)) {
                    this.grow(am);
                    SoundEvent sound = p.getEmptySound();
                    if (sound != null && world != null && pos != null)
                        world.playSound(null, pos, sound, SoundSource.BLOCKS, 1, 1);
                    return p.getEmptyContainer();
                }
            }
            return null
        }*/


        SoftFluid s = SoftFluidRegistry.fromItem(filledContainer);

        if (s.isEmpty()) return null;

        CompoundTag com = filledContainerStack.getTag();
        CompoundTag newCom = new CompoundTag();

        //convert potions to water bottles
        Potion potion = PotionUtils.getPotion(filledContainerStack);
        boolean hasCustomPot = (com != null && com.contains("CustomPotionEffects"));
        if (potion == Potions.WATER && !hasCustomPot){
            s = SoftFluidRegistry.WATER;
        }
        //add tags to splash and lingering potions
        else if (potion != Potions.EMPTY || hasCustomPot) {
            addPotionTag(filledContainer, newCom);
        }

        //copy nbt from item
        String[] nbtKey = s.getNbtKeyFromItem();
        if (com != null && nbtKey != null) {
            for (String k : nbtKey) {
                if (com.contains(k)) {
                    newCom.put(k, com.get(k));
                }
            }
        }

        //set new fluid if empty
        if (this.isEmpty()) {
            this.setFluid(s, newCom.isEmpty() ? null : newCom);
        }

        Item empty = s.tryGettingEmptyItem(filledContainer);
        SoftFluid.FilledContainerCategory category = s.tryGettingFilledItems(empty);

        if (category != null && empty != null) {

            int amount = category.getAmount();
            if (this.canAddSoftFluid(s, amount, newCom)) {
                if (simulate) return ItemStack.EMPTY;
                this.grow(amount);

                SoundEvent sound = category.getEmptySound();
                if (sound != null && world != null && pos != null)
                    world.playSound(null, pos, sound, SoundSource.BLOCKS, 1, 1);

                return new ItemStack(empty);
            }
        }
        return null;
    }

    /**
     * tries removing said amount of fluid and returns filled item
     * also plays sound
     *
     * @return filled bottle item. null if it failed or if simulated is true and failed
     */
    @Nullable
    public ItemStack tryFillingItem(Item emptyContainer, @Nullable Level level, @Nullable BlockPos pos, boolean simulate) {
        SoftFluid.FilledContainerCategory category = fluid.tryGettingFilledItems(emptyContainer);
        if (category != null) {
            int amount = category.getAmount();
            if (this.canRemove(amount)) {
                if (simulate) return ItemStack.EMPTY;
                ItemStack stack = new ItemStack(category.getFirstFilled());
                //case for lingering potions
                if (this.fluid == SoftFluidRegistry.POTION) {
                    if (this.nbt != null && this.nbt.contains("Bottle") && !emptyContainer.getRegistryName().getNamespace().equals("inspirations")) {
                        String bottle = this.nbt.getString("Bottle");
                        if (bottle.equals("SPLASH")) stack = new ItemStack(Items.SPLASH_POTION);
                        else if (bottle.equals("LINGERING")) stack = new ItemStack(Items.LINGERING_POTION);
                    }
                }

                //converts water bottles into potions
                if (emptyContainer == Items.GLASS_BOTTLE && fluid == SoftFluidRegistry.WATER)
                    stack = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);

                this.applyNBTtoItemStack(stack);
                this.shrink(amount);

                SoundEvent sound = category.getEmptySound();
                if (sound != null && world != null && pos != null)
                    world.playSound(null, pos, sound, SoundSource.BLOCKS, 1, 1);

                return stack;
            }
        }
        return null;
    }

    /**
     * tries removing bottle amount and returns filled bottle
     *
     * @return filled bottle item. null if it failed
     */
    @Nullable
    public ItemStack tryFillingBottle(Level level, BlockPos pos) {
        return tryFillingItem(Items.GLASS_BOTTLE, world, pos, false);
    }

    /**
     * tries removing bucket amount and returns filled bucket
     *
     * @return filled bucket item. null if it failed
     */
    @Nullable
    public ItemStack tryFillingBucket(Level level, BlockPos pos) {
        return tryFillingItem(Items.BUCKET, world, pos, false);
    }

    /**
     * tries removing bowl amount and returns filled bowl
     *
     * @return filled bowl item. null if it failed
     */
    @Nullable
    public ItemStack tryFillingBowl(Level level, BlockPos pos) {
        return tryFillingItem(Items.BOWL, world, pos, false);
    }

    /**
     * checks if current tank holds equivalent fluid as provided forge fluids stack & nbt
     *
     * @param fluidStack forge fluid stack
     * @param com        fluid nbt
     * @return is same
     */
    public boolean isSameFluidAs(FluidStack fluidStack, CompoundTag com) {
        return this.fluid.isEquivalent(fluidStack.getFluid()) && areNbtEquals(com,this.nbt);
    }

    /**
     * checks if current tank holds equivalent fluid as provided forge fluids stack
     *
     * @param fluidStack forge fluid stack
     * @return is same
     */
    //might be wrong
    public boolean isSameFluidAs(FluidStack fluidStack) {
        return isSameFluidAs(fluidStack,fluidStack.getTag());
    }

    private boolean areNbtEquals(CompoundTag nbt, CompoundTag nbt1) {
        return (((nbt == null || nbt.isEmpty()) && (nbt1 == null || nbt1.isEmpty())) || nbt1.equals(nbt));
    }

    /**
     * checks if current tank holds equivalent fluid as provided soft fluid
     *
     * @param other soft fluid
     * @return is same
     */
    public boolean isSameFluidAs(SoftFluid other) {
        return isSameFluidAs(other, null);
    }

    /**
     * checks if current tank holds equivalent fluid as provided soft fluid
     *
     * @param other soft fluid
     * @param com   fluid nbt
     * @return is same
     */
    public boolean isSameFluidAs(SoftFluid other, @Nullable CompoundTag com) {
        return this.fluid.equals(other) && areNbtEquals(this.nbt, com);
    }

    /**
     * try adding provided forge fluid to the tank
     *
     * @param fluidStack forge fluid stack
     * @return success
     */
    public boolean tryAddingFluid(FluidStack fluidStack) {
        int count = fluidStack.getAmount();
        SoftFluid s = SoftFluidRegistry.fromForgeFluid(fluid.getForgeFluid());
        return tryAddingFluid(s, count, fluidStack.getTag());
    }

    /**
     * try adding provided soft fluid to the tank
     *
     * @param s     soft fluid to add
     * @param count count to add
     * @param com   fluid nbt
     * @return success
     */
    public boolean tryAddingFluid(SoftFluid s, int count, @Nullable CompoundTag com) {
        if (this.canAdd(count)) {
            if (this.isEmpty()) {
                this.setFluid(s, com);
                this.setCount(count);
                return true;
            } else if (this.isSameFluidAs(s, com)) {
                this.grow(count);
                return true;
            }
        }
        return false;
    }

    /**
     * try adding provided soft fluid to the tank
     *
     * @param s     soft fluid to add
     * @param count count to add
     * @return success
     */
    public boolean tryAddingFluid(SoftFluid s, int count) {
        return tryAddingFluid(s, count, null);
    }

    /**
     * try adding 1 bottle of provided soft fluid to the tank
     *
     * @param s soft fluid to add
     * @return success
     */
    public boolean tryAddingFluid(SoftFluid s) {
        return this.tryAddingFluid(s, 1);
    }

    public boolean tryTransferFluid(SoftFluidHolder destination) {
        return this.tryTransferFluid(destination, BOTTLE_COUNT);
    }

    //transfers between two fluid holders
    public boolean tryTransferFluid(SoftFluidHolder destination, int amount) {
        if (destination.canAdd(amount) && this.canRemove(amount)) {
            if (destination.isEmpty()) {
                destination.setFluid(this.getFluid(), this.getNbt());
                this.shrink(amount);
                destination.grow(amount);
                return true;
            } else if (this.isSameFluidAs(destination.getFluid(), destination.nbt)) {
                this.shrink(amount);
                destination.grow(amount);
                return true;
            }
        }
        return false;
    }

    /**
     * empties n bottle of content into said forge fluid tank
     *
     * @param fluidDestination forge fluid tank handler
     * @param bottles          number of bottles to empty (1blt = 250mb)
     * @return success
     */
    public boolean tryTransferToFluidTank(IFluidHandler fluidDestination, int bottles) {
        if (!this.canRemove(bottles)) return false;
        int milliBuckets = bottles * 250;
        FluidStack stack = this.toEquivalentForgeFluid(milliBuckets);
        if (!stack.isEmpty()) {
            int fillableAmount = fluidDestination.fill(stack, IFluidHandler.FluidAction.SIMULATE);
            if (fillableAmount == milliBuckets) {
                fluidDestination.fill(stack, IFluidHandler.FluidAction.EXECUTE);
                this.shrink(bottles);
                return true;
            }
        }
        return false;
    }

    public boolean tryTransferToFluidTank(IFluidHandler fluidDestination) {
        return this.tryTransferToFluidTank(fluidDestination, BOTTLE_COUNT);
    }

    //drains said fluid tank of 250mb (1 bottle) of fluid
    public boolean drainFluidTank(IFluidHandler fluidSource, int bottles) {
        if (!this.canAdd(bottles)) return false;
        int milliBuckets = bottles * 250;
        FluidStack drainable = fluidSource.drain(milliBuckets, IFluidHandler.FluidAction.SIMULATE);
        if (!drainable.isEmpty() && drainable.getAmount() == milliBuckets) {
            boolean transfer = false;
            CompoundTag fsTag = drainable.getTag();
            if (this.fluid.isEmpty()) {
                this.setFluid(SoftFluidRegistry.fromForgeFluid(drainable.getFluid()), fsTag);
                transfer = true;
            } else if (this.isSameFluidAs(drainable, fsTag)) {
                transfer = true;
            }
            if (transfer) {
                fluidSource.drain(milliBuckets, IFluidHandler.FluidAction.EXECUTE);
                this.grow(bottles);
                return true;
            }
        }
        return false;
    }

    public boolean drainFluidTank(IFluidHandler fluidSource) {
        return this.drainFluidTank(fluidSource, BOTTLE_COUNT);
    }

    /**
     * gets the equivalent forge fluid without draining the tank. returned stack might be empty
     *
     * @param mb forge minecraft buckets
     * @return forge fluid stacks
     */
    public FluidStack toEquivalentForgeFluid(int mb) {
        FluidStack stack = new FluidStack(this.fluid.getForgeFluid(), mb);
        this.applyNBTtoFluidStack(stack);
        return stack;
    }

    private void applyNBTtoFluidStack(FluidStack fluidStack) {
        String[] nbtKey = this.fluid.getNbtKeyFromItem();
        if (this.nbt != null && !this.nbt.isEmpty() && !fluidStack.isEmpty() && nbtKey != null) {
            CompoundTag newCom = new CompoundTag();
            for (String k : nbtKey) {
                if (this.nbt.contains(k)) {
                    newCom.put(k, this.nbt.get(k));
                }
            }
            if(!newCom.isEmpty()) fluidStack.setTag(newCom);
        }
    }

    /**
     * can I remove n bottles of fluid
     *
     * @param n bottles amount
     * @return can remove
     */
    public boolean canRemove(int n) {
        return this.count >= n && !this.isEmpty();
    }

    /**
     * can I add n bottles of fluid
     *
     * @param n bottles amount
     * @return can add
     */
    public boolean canAdd(int n) {
        return this.count + n <= this.capacity;
    }

    /**
     * can provided soft fluid be added to tank
     *
     * @param s     soft fluid to add
     * @param count bottles amount
     * @return can add
     */
    public boolean canAddSoftFluid(SoftFluid s, int count) {
        return canAddSoftFluid(s, count, null);
    }

    /**
     * can provided soft fluid be added to tank
     *
     * @param s     soft fluid to add
     * @param count bottles amount
     * @param nbt   soft fluid nbt
     * @return can add
     */
    public boolean canAddSoftFluid(SoftFluid s, int count, @Nullable CompoundTag nbt) {
        return this.canAdd(count) && this.isSameFluidAs(s, nbt);
    }

    public boolean isFull() {
        return this.count == this.capacity;
    }

    public boolean isEmpty() {
        //count 0 should always = to fluid.empty
        return this.fluid.isEmpty() || this.count <= 0;
    }

    /**
     * grows contained fluid by at most inc bottles. doesn't need checking
     *
     * @param inc maximum increment
     */
    public void lossyAdd(int inc) {
        this.count = Math.min(this.capacity, this.count + inc);
    }

    /**
     * unchecked sets the tank fluid count
     *
     * @param count bottles count
     */
    public void setCount(int count) {
        this.count = count;
    }

    /**
     * fills out tank to the maximum capacity
     */
    public void fillCount() {
        this.setCount(capacity);
    }

    /**
     * unchecked grows the tank by inc bottles. Check with canAdd()
     *
     * @param inc bottles increment
     */
    public void grow(int inc) {
        this.setCount(this.count + inc);
    }

    /**
     * unchecked shrinks the tank by inc bottles
     *
     * @param inc bottles increment
     */
    public void shrink(int inc) {
        this.grow(-inc);
        if (this.count == 0) this.fluid = SoftFluidRegistry.EMPTY;
    }

    /**
     * gets liquid height for renderer
     *
     * @param maxHeight maximum height in blocks
     * @return fluid height
     */
    public float getHeight(float maxHeight) {
        return maxHeight * (float) this.count / (float) this.capacity;
    }

    /**
     * @return comparator block redstone power
     */
    public int getComparatorOutput() {
        float f = this.count / (float) this.capacity;
        return Mth.floor(f * 14.0F) + 1;
    }

    public int getCount() {
        return count;
    }

    @Nonnull
    public SoftFluid getFluid() {
        return fluid;
    }

    @Nullable
    public CompoundTag getNbt() {
        return nbt;
    }

    public void setNbt(@Nullable CompoundTag nbt) {
        this.nbt = nbt;
    }

    /**
     * resets & clears the tank
     */
    public void clear() {
        this.fluid = SoftFluidRegistry.EMPTY;
        this.setCount(0);
        this.nbt = null;
        this.specialColor = 0;
    }

    /**
     * copies the content of a fluid tank into this
     *
     * @param other other tank
     */
    public void copy(SoftFluidHolder other) {
        this.setFluid(other.getFluid(), other.getNbt());
        this.setCount(Math.min(this.capacity, other.getCount()));
    }

    /**
     * copies the content of a fluid tank into this
     *
     * @param other forge fluid tank
     */
    public void copy(IFluidHandler other) {
        FluidStack drainable = other.getFluidInTank(0).copy();// 250, IFluidHandler.FluidAction.SIMULATE);
        CompoundTag nbt = drainable.isEmpty() ? null : drainable.getTag();
        this.setFluid(SoftFluidRegistry.fromForgeFluid(drainable.getFluid()), nbt);
        this.setCount(Math.min(this.capacity, other.getTankCapacity(0)));
    }

    /**
     * fills to max capacity with provided forge fluid
     *
     * @param fluidStack forge fluid
     */
    public void fill(FluidStack fluidStack) {
        this.setFluid(fluidStack);
        this.fillCount();
    }

    /**
     * fills to max capacity with provided soft fluid
     *
     * @param fluid forge fluid
     */
    public void fill(SoftFluid fluid) {
        this.fill(fluid, null);
    }

    /**
     * fills to max capacity with provided soft fluid
     *
     * @param fluid soft fluid
     * @param nbt   soft fluid nbt
     */
    public void fill(SoftFluid fluid, @Nullable CompoundTag nbt) {
        this.setFluid(fluid, nbt);
        this.fillCount();
    }

    /**
     * sets current fluid to provided forge fluid equivalent
     *
     * @param fluidStack forge fluid
     */
    public void setFluid(FluidStack fluidStack) {
        SoftFluid s = SoftFluidRegistry.fromForgeFluid(fluidStack.getFluid());
        this.setFluid(s, fluidStack.getTag());
    }

    /**
     * sets current fluid to provided soft fluid equivalent
     *
     * @param fluid soft fluid
     */
    public void setFluid(SoftFluid fluid) {
        this.setFluid(fluid, null);
    }

    //called when it goes from empty to full
    public void setFluid(SoftFluid fluid, @Nullable CompoundTag nbt) {
        this.fluid = fluid;
        this.nbt = nbt;
        this.specialColor = 0;
        if (this.fluid.isEmpty()) this.setCount(0);
        this.needColorRefresh = true;
    }

    /**
     * @return tint color to be applied on the fuid texture
     */
    public int getTintColor(@Nullable LevelAccessor world, @Nullable BlockPos pos) {
        SoftFluid.TintMethod method = this.fluid.getTintMethod();
        if (method == SoftFluid.TintMethod.NO_TINT) return -1;
        if (this.needColorRefresh) {
            this.refreshSpecialColor(world, pos);
            this.needColorRefresh = false;
        }
        if (this.specialColor != 0) return this.specialColor;
        return this.fluid.getTintColor();
    }

    /**
     * @return tint color to be applied on the fluid texture
     */
    public int getFlowingTint(@Nullable LevelAccessor world, @Nullable BlockPos pos) {
        SoftFluid.TintMethod method = this.fluid.getTintMethod();
        if (method == SoftFluid.TintMethod.FLOWING) return this.getParticleColor(world, pos);
        else return this.getTintColor(world, pos);
    }

    /**
     * @return tint color to be used on particle. Differs from getTintColor since it returns an average color extrapolated from their fluid textures
     */
    public int getParticleColor(@Nullable LevelAccessor world, @Nullable BlockPos pos) {
        if (this.isEmpty()) return -1;
        int tintColor = this.getTintColor(world, pos);
        //if tint color is white gets averaged color
        if (tintColor == -1)
            return FluidParticleColors.get(this.fluid.getID());
        return tintColor;
    }

    //grabs world/ fluidstack dependednt tint color if fluid has associated forge fluid. overrides normal tint color
    private void refreshSpecialColor(@Nullable LevelAccessor world, @Nullable BlockPos pos) {

        if (fluid == SoftFluidRegistry.POTION) {
            this.specialColor = PotionNBTHelper.getColorFromNBT(this.nbt);
        } else {
            Fluid f = this.getFluid().getForgeFluid();
            if (f != Fluids.EMPTY) {
                FluidAttributes att = f.getAttributes();
                //world accessor
                int w = -1;
                if (world != null && pos != null) w = att.getColor(world, pos);
                //stack accessor
                if (w == -1) w = att.getColor(this.toEquivalentForgeFluid(1));
                if (w != -1) this.specialColor = w;
            }
        }

    }

    /**
     * @return true if contained fluid has associated food
     */
    public boolean containsFood() {
        return this.fluid.isFood();
    }

    /**
     * returns associated food items
     *
     * @return food
     */
    public ItemStack getFood() {
        ItemStack stack = new ItemStack(this.fluid.getFoodItem());
        this.applyNBTtoItemStack(stack);
        return stack;
    }

    /**
     * call from tile entity. loads tank from nbt
     *
     * @param compound nbt
     */
    public void load(CompoundTag compound) {
        if (compound.contains("FluidHolder")) {
            CompoundTag cmp = compound.getCompound("FluidHolder");
            this.count = cmp.getInt("Count");
            this.nbt = cmp.getCompound("NBT");
            String id = cmp.getString("Fluid");
            SoftFluid sf = SoftFluidRegistry.get(id);
            //TODO: remove
            //supplementaries backwards compat
            if (sf.isEmpty()) {
                sf = SoftFluidRegistry.get(id.replace("supplementaries", "minecraft"));
                if (sf.isEmpty()) {
                    sf = SoftFluidRegistry.get(id.replace("minecraft", "supplementaries"));
                }
            }

            this.setFluid(sf, this.nbt);
        }
    }

    /**
     * call from tile entity. saves to nbt
     *
     * @param compound nbt
     * @return nbt
     */
    public CompoundTag save(CompoundTag compound) {
        CompoundTag cmp = new CompoundTag();
        cmp.putInt("Count", this.count);
        cmp.putString("Fluid", this.fluid.getID());
        //for item render. needed for potion colors
        cmp.putInt("CachedColor", this.getTintColor(null, null));
        if (this.nbt != null && !this.nbt.isEmpty()) cmp.put("NBT", this.nbt);
        compound.put("FluidHolder", cmp);

        return compound;
    }

    /**
     * makes player drink 1 bottle and removes it from the tank
     *
     * @param player player
     * @param world  world
     * @return success
     */
    public boolean tryDrinkUpFluid(Player player, Level level) {
        if (this.isEmpty()) return false;
        ItemStack stack = this.getFood();
        Item item = stack.getItem();

        //case for xp
        if (this.fluid == SoftFluidRegistry.XP) {
            player.giveExperiencePoints(Utils.getXPinaBottle(1, world.random));
            if (world.isClientSide) return true;
            this.shrink(1);

            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1, 1);
            return true;
        }
        //food
        else if (this.containsFood()) {

            Food food = item.getFoodProperties();
            int div = this.fluid.getFoodDivider();
            //single items are handled by items themselves
            if (div == 1) {
                stack.getItem().finishUsingItem(stack.copy(), world, player);
                if (world.isClientSide) return true;
                this.shrink(1);
                if (food == null || stack.getItem().isEdible()) {
                    player.playNotifySound(item.getDrinkingSound(), SoundSource.PLAYERS, 1, 1);
                }
                //player already plays sound
                return true;
            }

            boolean success = false;
            //stew & bucket case
            if (food != null && player.canEat(false)) {

                if (item instanceof SuspiciousStewItem) susStewBehavior(player, stack, div);

                if (world.isClientSide) return true;
                player.getFoodData().eat(food.getNutrition() / div, food.getSaturationModifier() / (float) div);

                success = true;
            } else if (item instanceof MilkBucketItem) {
                if (world.isClientSide) return true;
                milkBottleBehavior(player, stack);
                success = true;
            }
            if (success) {
                this.shrink(1);
                player.playNotifySound(item.getDrinkingSound(), SoundSource.PLAYERS, 1, 1);
                return true;
            }
        }
        return false;
    }

    //vanilla fluids special behaviors

    //stew code
    private static void susStewBehavior(Player player, ItemStack stack, int div) {
        CompoundTag CompoundTag = stack.getTag();
        if (CompoundTag != null && CompoundTag.contains("Effects", 9)) {
            ListTag listtag = CompoundTag.getList("Effects", 10);
            for (int i = 0; i < listtag.size(); ++i) {
                int j = 160;
                CompoundTag CompoundTag1 = listtag.getCompound(i);
                if (CompoundTag1.contains("EffectDuration", 3))
                    j = CompoundTag1.getInt("EffectDuration") / div;
                Effect effect = Effect.byId(CompoundTag1.getByte("EffectId"));
                if (effect != null) {
                    player.addEffect(new EffectInstance(effect, j));
                }
            }
        }
    }

    //removes just 1 effect
    private static boolean milkBottleBehavior(Player player, ItemStack stack) {
        for (EffectInstance effect : player.getActiveEffectsMap().values()) {
            if (effect.isCurativeItem(stack)) {
                player.removeEffect(effect.getEffect());
                return true;
            }
        }
        return false;
    }

    //util functions
    public static int getLiquidCountFromItem(Item i) {
        if (i == Items.GLASS_BOTTLE) {
            return BOTTLE_COUNT;
        } else if (i == Items.BOWL) {
            return BOWL_COUNT;
        } else if (i == Items.BUCKET) {
            return BUCKET_COUNT;
        }
        return 0;
    }

    public static ItemStack getEmptyBottle() {
        return new ItemStack(Items.GLASS_BOTTLE);
    }

    public static ItemStack getEmptyBucket() {
        return new ItemStack(Items.BUCKET);
    }

    public static ItemStack getEmptyBowl() {
        return new ItemStack(Items.BOWL);
    }

}
