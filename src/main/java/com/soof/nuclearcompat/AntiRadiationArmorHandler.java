package com.soof.nuclearcompat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.nuclearteam.createnuclear.CNAttributes;

/**
 * 重设原子核动防辐射套装的护甲数值。
 *
 * <p>Create Nuclear 的护甲值来自物品自带的 {@code ItemAttributeModifiers}，
 * 无法用数据包覆盖；这里在 {@link ItemAttributeModifierEvent} 里把护甲值修饰符整段替换，
 * 并额外给每个部件加 0.1 击退抗性。</p>
 *
 * <p><b>注意</b>：每个部件必须使用<b>不同的修饰符 ID</b>。实体属性表以 ID 为键，
 * 四件共用同一个 ID 时会互相覆盖——装备整套只算一件、脱下时又会把别的部件一起移除，
 * 表现为「整套护甲值显示不对」。</p>
 */
@EventBusSubscriber(modid = NuclearCompat.MODID)
public final class AntiRadiationArmorHandler {

    /** 每个部件额外的击退抗性。 */
    private static final double KNOCKBACK_RESISTANCE_PER_PIECE = 0.1D;

    /**
     * CN 辐射模式下，AC 防化服每个部件提供的辐照抗性。
     *
     * <p>AC 自己的辐照机制按 25%/件减伤，换成原子核动的数值体系后，只有
     * {@code createnuclear:irradiated_resistance} 属性有意义；CN 的防辐射套装原本就是
     * 0.25/件，这里给防化服补上同样的数值，两套护甲才保持对等。</p>
     */
    private static final double RADIATION_RESISTANCE_PER_HAZMAT_PIECE = 0.25D;

    private static final String NAMESPACE = "createnuclear";
    private static final String AC_NAMESPACE = "alexscaves";

    private AntiRadiationArmorHandler() {
    }

    @SubscribeEvent
    public static void onItemAttributes(ItemAttributeModifierEvent event) {
        // CN 辐射模式下，AC 防化服也需要提供辐照抗性（不受护甲增强开关影响）。
        if (NuclearCompatConfig.useCreateNuclearRadiation()) {
            applyHazmatRadiationResistance(event);
        }

        if (!NuclearCompatConfig.COMMON.enhancedAntiRadiationArmor.get()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!NAMESPACE.equals(id.getNamespace())) {
            return;
        }

        String path = id.getPath();
        double armor;
        EquipmentSlotGroup group;
        String piece;
        if (path.endsWith("anti_radiation_helmet")) {
            armor = 2.0D;
            group = EquipmentSlotGroup.HEAD;
            piece = "helmet";
        } else if (path.endsWith("anti_radiation_chestplate")) {
            armor = 6.0D;
            group = EquipmentSlotGroup.CHEST;
            piece = "chestplate";
        } else if (path.endsWith("anti_radiation_leggings")) {
            armor = 5.0D;
            group = EquipmentSlotGroup.LEGS;
            piece = "leggings";
        } else if (path.endsWith("anti_radiation_boots")) {
            armor = 2.0D;
            group = EquipmentSlotGroup.FEET;
            piece = "boots";
        } else {
            return;
        }

        // 先把护甲材料原本的护甲值修饰符整段移除，再写入目标值（韧性、辐照抗性等其它属性保持原样）。
        event.removeAllModifiersFor(Attributes.ARMOR);

        // ID 必须按部件区分，否则四件共用一个 ID 会在实体属性表里互相覆盖。
        ResourceLocation armorId = ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "anti_radiation_armor/" + piece);
        ResourceLocation knockbackId = ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "anti_radiation_knockback/" + piece);

        event.addModifier(
                Attributes.ARMOR,
                new AttributeModifier(armorId, armor, AttributeModifier.Operation.ADD_VALUE),
                group);
        event.addModifier(
                Attributes.KNOCKBACK_RESISTANCE,
                new AttributeModifier(knockbackId, KNOCKBACK_RESISTANCE_PER_PIECE,
                        AttributeModifier.Operation.ADD_VALUE),
                group);
    }

    /** 给 AC 的防化服四个部件补上 0.25 辐照抗性（仅 CN 辐射模式）。 */
    private static void applyHazmatRadiationResistance(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!AC_NAMESPACE.equals(id.getNamespace())) {
            return;
        }

        EquipmentSlotGroup group;
        String piece;
        switch (id.getPath()) {
            case "hazmat_mask" -> {
                group = EquipmentSlotGroup.HEAD;
                piece = "helmet";
            }
            case "hazmat_chestplate" -> {
                group = EquipmentSlotGroup.CHEST;
                piece = "chestplate";
            }
            case "hazmat_leggings" -> {
                group = EquipmentSlotGroup.LEGS;
                piece = "leggings";
            }
            case "hazmat_boots" -> {
                group = EquipmentSlotGroup.FEET;
                piece = "boots";
            }
            default -> {
                return;
            }
        }

        event.addModifier(
                CNAttributes.IRRADIATED_RESISTANCE,
                new AttributeModifier(
                        ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "hazmat_radiation/" + piece),
                        RADIATION_RESISTANCE_PER_HAZMAT_PIECE,
                        AttributeModifier.Operation.ADD_VALUE),
                group);
    }
}
