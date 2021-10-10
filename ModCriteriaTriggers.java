package net.mehvahdjukaar.selene.common;

import com.google.gson.JsonObject;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.*;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

public class ModCriteriaTriggers {

    public static void init(){}

    public static final GrindedItem GRIND = CriteriaTriggers.register(new GrindedItem());
    
    public static class GrindedItem extends SimpleCriterionTrigger<GrindedItem.Instance> {
        private static final ResourceLocation ID = new ResourceLocation("grind_item");

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public GrindedItem.Instance createInstance(JsonObject json, EntityPredicate.Composite predicate, DeserializationContext p_230241_3_) {
            ItemPredicate itempredicate = ItemPredicate.fromJson(json.get("item"));
            return new GrindedItem.Instance(predicate, itempredicate);
        }

        public void trigger(ServerPlayer Player, ItemStack stack) {
            this.trigger(Player, (instance) -> instance.matches(stack));
        }

        public static class Instance extends AbstractCriterionTriggerInstance implements CriterionTriggerInstance {
            private final ItemPredicate item;

            public Instance(EntityPredicate.Composite p_i231585_1_, ItemPredicate item) {
                super();
                serializeToJson(GrindedItem.ID, p_i231585_1_);
                this.item = item;
            }

            private void serializeToJson( ResourceLocation id, EntityPredicate.Composite p_i231585_1_ ) {
            }

            public boolean matches(ItemStack stack) {
                return this.item.matches(stack);
            }

            @Override
            public ResourceLocation getCriterion() {
                return null;
            }

            @Override
            public JsonObject serializeToJson(SerializationContext serializer) {
                JsonObject jsonobject = super.serializeToJson(serializer);
                jsonobject.add("item", this.item.serializeToJson());
                return jsonobject;
            }
        }
    }


}
