package net.mehvahdjukaar.selene.mixins;

import com.google.common.collect.Maps;
import net.mehvahdjukaar.selene.Selene;
import net.mehvahdjukaar.selene.map.CustomDecoration;
import net.mehvahdjukaar.selene.map.CustomDecorationHolder;
import net.mehvahdjukaar.selene.map.CustomDecorationType;
import net.mehvahdjukaar.selene.map.MapDecorationHandler;
import net.mehvahdjukaar.selene.map.markers.DummyMapWorldMarker;
import net.mehvahdjukaar.selene.map.markers.MapWorldMarker;
import net.mehvahdjukaar.selene.network.NetworkHandler;
import net.mehvahdjukaar.selene.network.SyncCustomMapDecorationPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapBanner;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;


@Mixin(MapDataMixin.class)
public abstract class MapDataMixin extends MapItemSavedData implements CustomDecorationHolder {
    public MapDataMixin(String name) {
        super(name);
    }

    @Shadow
    public int x;

    @Shadow
    public int z;

    @Shadow
    public byte scale;

    @Final
    @Shadow
    public Map<String, MapDecoration> decorations;

    @Shadow
    public ResourceKey<Level> dimension;

    @Shadow
    public byte[] colors;
    @Shadow
    public boolean locked;
    @Shadow
    @Final
    private Map<String, MapBanner> bannerMarkers;
    //new decorations (stuff that gets rendered)
    @Final
    public Map<String, CustomDecoration> customDecorations = Maps.newLinkedHashMap();

    //world markers
    @Final
    private Map<String, MapWorldMarker<?>> customMapMarkers = Maps.newHashMap();

    @Override
    public Map<String, CustomDecoration> getCustomDecorations() {
        return customDecorations;
    }

    @Override
    public Map<String, MapWorldMarker<?>> getCustomMarkers() {
        return customMapMarkers;
    }

    private <D extends CustomDecoration> void addCustomDecoration(MapWorldMarker<D> marker) {
        D decoration = marker.createDecorationFromMarker(scale, x, z, dimension, locked);
        if (decoration != null) {
            this.customDecorations.put(marker.getMarkerId(), decoration);
        }
    }


    protected void lockData( MapItemSavedData data, CallbackInfo ci ) {
        if (data instanceof CustomDecorationHolder) {
            this.customMapMarkers.putAll(((CustomDecorationHolder) data).getCustomMarkers());
            this.customDecorations.putAll(((CustomDecorationHolder) data).getCustomDecorations());
        }
    }


    public void tickCarriedBy(Player player, ItemStack stack, CallbackInfo ci) {
        CompoundTag CompoundTag = stack.getTag();
        if (CompoundTag != null && CompoundTag.contains("CustomDecorations", 9)) {
            ListTag listtag = CompoundTag.getList("CustomDecorations", 10);
            //for exploration maps
            for (int j = 0; j < listtag.size(); ++j) {
                CompoundTag com = listtag.getCompound(j);
                if (!this.decorations.containsKey(com.getString("id"))) {
                    String name = com.getString("type");
                    //TODO: add more checks
                    CustomDecorationType<CustomDecoration, ?> type = (CustomDecorationType<CustomDecoration, ?>) MapDecorationHandler.get(name);
                    if (type != null) {
                        MapWorldMarker<CustomDecoration> dummy = new DummyMapWorldMarker(type, com.getInt("x"), com.getInt("z"));
                        this.addCustomDecoration(dummy);
                    } else {
                        Selene.LOGGER.warn("Failed to load map decoration " + name + ". Skipping it");

                    }
                }
            }
        }
    }


    public void load(CompoundTag compound, CallbackInfo ci) {
        if (compound.contains("customMarkers")) {
            ListTag listTag = compound.getList("customMarkers", 10);

            for (int j = 0; j < listTag.size(); ++j) {
                MapWorldMarker<?> marker = MapDecorationHandler.readWorldMarker(listTag.getCompound(j));
                if (marker != null) {
                    this.customMapMarkers.put(marker.getMarkerId(), marker);
                    this.addCustomDecoration(marker);
                }
            }
        }
    }

    public void save(CompoundTag p_189551_1_, CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag com = cir.getReturnValue();

        ListTag listTag = new ListTag();

        for (MapWorldMarker<?> marker : this.customMapMarkers.values()) {
            CompoundTag com2 = new CompoundTag();
            com2.put(marker.getTypeId(), marker.saveToNBT(new CompoundTag()));
            listTag.add(com2);
        }
        com.put("customMarkers", listTag);
    }

    @Override
    public void resetCustomDecoration() {
        for (String key : this.customMapMarkers.keySet()) {
            this.customDecorations.remove(key);
            this.customMapMarkers.remove(key);
        }
        for (String key : this.bannerMarkers.keySet()) {
            this.bannerMarkers.remove(key);
            this.decorations.remove(key);
        }
    }

    @Override
    public void toggleCustomDecoration(LevelAccessor world, BlockPos pos) {
        double d0 = (double) pos.getX() + 0.5D;
        double d1 = (double) pos.getZ() + 0.5D;
        int i = 1 << this.scale;
        double d2 = (d0 - (double) this.x) / (double) i;
        double d3 = (d1 - (double) this.z) / (double) i;
        if (d2 >= -63.0D && d3 >= -63.0D && d2 <= 63.0D && d3 <= 63.0D) {
            List<MapWorldMarker<?>> markers = MapDecorationHandler.getMarkersFromWorld(world, pos);
            boolean changed = false;
            for (MapWorldMarker<?> marker : markers) {
                if (marker != null) {
                    //toggle
                    String id = marker.getMarkerId();
                    if (this.customMapMarkers.containsKey(id) && this.customMapMarkers.get(id).equals(marker)) {
                        this.customMapMarkers.remove(id);
                        this.customDecorations.remove(id);
                    } else {
                        this.customMapMarkers.put(id, marker);
                        this.addCustomDecoration(marker);
                    }
                    changed = true;
                }
            }
            if (changed) this.setDirty();
        }
    }

    public void checkBanners(BlockGetter world, int x, int z, CallbackInfo ci) {
        Iterator<MapWorldMarker<?>> iterator = this.customMapMarkers.values().iterator();

        while (iterator.hasNext()) {
            MapWorldMarker<?> marker = iterator.next();
            if (marker.getPos().getX() == x && marker.getPos().getZ() == z) {
                MapWorldMarker<?> newMarker = marker.getType().getWorldMarkerFromWorld(world, marker.getPos());
                String id = marker.getMarkerId();
                if (newMarker == null) {
                    iterator.remove();
                    this.customDecorations.remove(id);
                } else if (Objects.equals(id, newMarker.getMarkerId()) && marker.shouldUpdate(newMarker)) {
                    newMarker.updateDecoration(this.customDecorations.get(id));
                }
            }


        }
    }

        public void getUpdatePacket(ItemStack stack, BlockGetter reader, Player Player, CallbackInfoReturnable<Packet<?>> cir) {
        Packet<?> packet = cir.getReturnValue();
        if (Player instanceof ServerPlayer && packet instanceof ClientboundMapItemDataPacket) {
            NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) Player),
                    new SyncCustomMapDecorationPacket(MapItem.getMapId(stack), this.customDecorations.values().toArray(new CustomDecoration[0])));
        }
    }

}
