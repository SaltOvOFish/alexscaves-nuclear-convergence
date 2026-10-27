package com.soof.nuclearcompat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 两套护甲的公共判断工具：
 * - Create Nuclear 防辐射套装（default_anti_radiation_*）
 * - Alex's Caves Up 防化套装（hazmat_*）
 */
public final class ArmorCompat {

    private static final ResourceLocation CN_HELMET = ResourceLocation.fromNamespaceAndPath("createnuclear", "default_anti_radiation_helmet");
    private static final ResourceLocation CN_CHESTPLATE = ResourceLocation.fromNamespaceAndPath("createnuclear", "default_anti_radiation_chestplate");
    private static final ResourceLocation CN_LEGGINGS = ResourceLocation.fromNamespaceAndPath("createnuclear", "default_anti_radiation_leggings");
    private static final ResourceLocation CN_BOOTS = ResourceLocation.fromNamespaceAndPath("createnuclear", "default_anti_radiation_boots");

    private static final ResourceLocation AC_MASK = ResourceLocation.fromNamespaceAndPath("alexscaves", "hazmat_mask");
    private static final ResourceLocation AC_CHESTPLATE = ResourceLocation.fromNamespaceAndPath("alexscaves", "hazmat_chestplate");
    private static final ResourceLocation AC_LEGGINGS = ResourceLocation.fromNamespaceAndPath("alexscaves", "hazmat_leggings");
    private static final ResourceLocation AC_BOOTS = ResourceLocation.fromNamespaceAndPath("alexscaves", "hazmat_boots");

    private ArmorCompat() {
    }

    /**
     * 注册名 → 物品的解析缓存。
     *
     * <p>原先每次判断都走 {@code BuiltInRegistries.ITEM.get(ResourceLocation)}（一次哈希 + 查表），
     * 而酸液路径（{@code AcidBlock#entityInside}）是<b>每个实体每 tick</b> 调用，
     * 一次要查 4 次；辐照效果结算时也要查 8 次。缓存后全部变成一次 Map 查表。</p>
     */
    private static final Map<ResourceLocation, Item> ITEM_CACHE = new ConcurrentHashMap<>();

    private static Item item(ResourceLocation id) {
        return ITEM_CACHE.computeIfAbsent(id, key -> {
            Item resolved = BuiltInRegistries.ITEM.get(key);
            return resolved == null ? Items.AIR : resolved;
        });
    }

    private static boolean is(ItemStack stack, ResourceLocation id) {
        Item item = item(id);
        return item != Items.AIR && stack.is(item);
    }

    /** 该物品是否为原子核动的防辐射护甲（任一部件）。 */
    public static boolean isAntiRadiationArmor(ItemStack stack) {
        return is(stack, CN_HELMET) || is(stack, CN_CHESTPLATE) || is(stack, CN_LEGGINGS) || is(stack, CN_BOOTS);
    }

    /** 穿戴防辐射套装的数量（0~4） */
    public static int countAntiRadiationArmor(LivingEntity entity) {
        int n = 0;
        if (is(entity.getItemBySlot(EquipmentSlot.HEAD), CN_HELMET)) n++;
        if (is(entity.getItemBySlot(EquipmentSlot.CHEST), CN_CHESTPLATE)) n++;
        if (is(entity.getItemBySlot(EquipmentSlot.LEGS), CN_LEGGINGS)) n++;
        if (is(entity.getItemBySlot(EquipmentSlot.FEET), CN_BOOTS)) n++;
        return n;
    }

    /** 是否穿戴整套防辐射套装（4 件） */
    public static boolean hasFullAntiRadiationArmor(LivingEntity entity) {
        return countAntiRadiationArmor(entity) == 4;
    }

    /** 是否穿戴整套防化套装（4 件） */
    public static boolean hasFullHazmatArmor(LivingEntity entity) {
        return is(entity.getItemBySlot(EquipmentSlot.HEAD), AC_MASK)
                && is(entity.getItemBySlot(EquipmentSlot.CHEST), AC_CHESTPLATE)
                && is(entity.getItemBySlot(EquipmentSlot.LEGS), AC_LEGGINGS)
                && is(entity.getItemBySlot(EquipmentSlot.FEET), AC_BOOTS);
    }

    /**
     * 计算「防化等价」护甲总数（防化服 + 防辐射服），上限 4。
     *
     * <p>{@code wornHazmat} 是调用方通过 {@code HazmatArmorItem.getWornAmount} 得到的
     * 防化服数量（该方法属于 Alex's Caves 具体类，Up/Neo 包名不同，只能留在 mixin 里），
     * 这里只负责把它与防辐射服数量相加并封顶。</p>
     */
    public static int countHazmatLikeArmor(int wornHazmat, LivingEntity entity) {
        return Math.min(wornHazmat + countAntiRadiationArmor(entity), 4);
    }
}
