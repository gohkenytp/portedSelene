package net.mehvahdjukaar.selene.network;

import net.mehvahdjukaar.selene.Selene;
import net.mehvahdjukaar.selene.map.CustomDecoration;
import net.mehvahdjukaar.selene.map.CustomDecorationHolder;
import net.mehvahdjukaar.selene.map.CustomDecorationType;
import net.mehvahdjukaar.selene.map.MapDecorationHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.world.item.MapItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fmllegacy.network.NetworkDirection;
import net.minecraftforge.fmllegacy.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;


public class SyncCustomMapDecorationPacket {
    private final int mapId;
    private final CustomDecoration[] customDecoration;


    public SyncCustomMapDecorationPacket(int mapId, CustomDecoration[] customDecoration) {
        this.mapId = mapId;
        this.customDecoration = customDecoration;
    }

    public SyncCustomMapDecorationPacket(FriendlyByteBuf buffer) {
        this.mapId = buffer.readVarInt();
        this.customDecoration = new CustomDecoration[buffer.readVarInt()];

        for(int i = 0; i < this.customDecoration.length; ++i) {
            CustomDecorationType<?,?> type = MapDecorationHandler.get(buffer.readResourceLocation());
            if(type!=null){
                this.customDecoration[i] = type.loadDecorationFromBuffer(buffer);
            }
        }
    }

    public static void buffer(SyncCustomMapDecorationPacket message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.mapId);
        buffer.writeVarInt(message.customDecoration.length);

        for(CustomDecoration decoration : message.customDecoration) {
            buffer.writeResourceLocation(decoration.getType().getId());
            decoration.saveToBuffer(buffer);
        }
    }


    public static void handler(SyncCustomMapDecorationPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {

                Minecraft mc = Minecraft.getInstance();
                MapItemRenderer mapitemrenderer = mc.gameRenderer.getMapRenderer();
                String s = MapItem.makeKey(message.getMapId());
                MapItemSavedData mapitemsaveddata = mc.level.getMapItemSavedData(s);
                if (mapitemsaveddata == null) {
                    mapitemsaveddata = new MapItemSavedData(s);
                    if (mapitemrenderer.getMapInstanceIfExists(s) != null) {
                        MapItemSavedData mapitemsaveddata1 = mapitemrenderer.getData(mapitemrenderer.getMapInstanceIfExists(s));
                        if (mapitemsaveddata1 != null) {
                            mapitemsaveddata = mapitemsaveddata1;
                        }
                    }
                    mc.level.setMapItemSavedData(mapitemsaveddata);
                }

                message.applyToMap(mapitemsaveddata);
                mapitemrenderer.update(mapitemsaveddata);
            }
        });
        context.setPacketHandled(true);
    }



    @OnlyIn(Dist.CLIENT)
    public int getMapId() {
        return this.mapId;
    }

    @OnlyIn(Dist.CLIENT)
    public void applyToMap(MapItemSavedData data) {
        if(data instanceof CustomDecorationHolder){
            Map<String, CustomDecoration> decorations = ((CustomDecorationHolder) data).getCustomDecorations();
            decorations.clear();
            for(int i = 0; i < this.customDecoration.length; ++i) {
                CustomDecoration mapdecoration = this.customDecoration[i];
                if(mapdecoration!=null) decorations.put("icon-" + i, mapdecoration);
                else{
                    Selene.LOGGER.warn("Failed to load custom map decoration, skipping");
                }
            }
        }
    }
}