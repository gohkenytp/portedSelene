package net.mehvahdjukaar.selene.util;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potions;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static net.minecraft.world.item.alchemy.PotionUtils.*;

public class PotionNBTHelper {
    private static final MutableComponent EMPTY = (new TranslatableComponent("effect.none")).withStyle(TextFormatting.GRAY);

    //I need this cause I'm using block entity tag so I can't give PotionUtil methods an itemStack directly
    public static void addPotionTooltip( @Nullable CompoundTag com, List<Component> tooltip, float durationFactor) {
        List<EffectInstance> list = getAllEffects(com);
        List<Pair<Attribute, AttributeModifier>> list1 = Lists.newArrayList();
        if (list.isEmpty()) {
            tooltip.add(EMPTY);
        } else {
            for(EffectInstance effectinstance : list) {
                MutableComponent MutableComponent = new TranslatableComponent(effectinstance.getDescriptionId());
                Effect effect = effectinstance.getEffect();
                Map<Attribute, AttributeModifier> map = effect.getAttributeModifiers();
                if (!map.isEmpty()) {
                    for(Map.Entry<Attribute, AttributeModifier> entry : map.entrySet()) {
                        AttributeModifier attributemodifier = entry.getValue();
                        AttributeModifier attributemodifier1 = new AttributeModifier(attributemodifier.getName(), effect.getAttributeModifierValue(effectinstance.getAmplifier(), attributemodifier), attributemodifier.getOperation());
                        list1.add(new Pair<>(entry.getKey(), attributemodifier1));
                    }
                }

                if (effectinstance.getAmplifier() > 0) {
                    MutableComponent = new TranslatableComponent("potion.withAmplifier", MutableComponent, new TranslatableComponent("potion.potency." + effectinstance.getAmplifier()));
                }

                if (effectinstance.getDuration() > 20) {
                    MutableComponent = new TranslatableComponent("potion.withDuration", MutableComponent, EffectUtils.formatDuration(effectinstance, durationFactor));
                }

                tooltip.add(MutableComponent.withStyle(effect.getCategory().getTooltipFormatting()));
            }
        }

        if (!list1.isEmpty()) {
            tooltip.add(StringTextComponent.EMPTY);
            tooltip.add((new TranslatableComponent("potion.whenDrank")).withStyle(TextFormatting.DARK_PURPLE));

            for(Pair<Attribute, AttributeModifier> pair : list1) {
                AttributeModifier attributemodifier2 = pair.getSecond();
                double d0 = attributemodifier2.getAmount();
                double d1;
                if (attributemodifier2.getOperation() != AttributeModifier.Operation.MULTIPLY_BASE && attributemodifier2.getOperation() != AttributeModifier.Operation.MULTIPLY_TOTAL) {
                    d1 = attributemodifier2.getAmount();
                } else {
                    d1 = attributemodifier2.getAmount() * 100.0D;
                }

                if (d0 > 0.0D) {
                    tooltip.add((new TranslatableComponent("attribute.modifier.plus." + attributemodifier2.getOperation().toValue(), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(d1), new TranslatableComponent(pair.getFirst().getDescriptionId()))).withStyle(TextFormatting.BLUE));
                } else if (d0 < 0.0D) {
                    d1 = d1 * -1.0D;
                    tooltip.add((new TranslatableComponent("attribute.modifier.take." + attributemodifier2.getOperation().toValue(), ItemStack.ATTRIBUTE_MODIFIER_FORMAT.format(d1), new TranslatableComponent(pair.getFirst().getDescriptionId()))).withStyle(TextFormatting.RED));
                }
            }
        }

    }

    public static int getColorFromNBT(@Nullable CompoundTag com) {
        if (com != null && com.contains("CustomPotionColor", 99)) {
            return com.getInt("CustomPotionColor");
        } else {
            return getPotion(com) == Potions.EMPTY ? 16253176 : getColor(getAllEffects(com));
        }
    }
}
