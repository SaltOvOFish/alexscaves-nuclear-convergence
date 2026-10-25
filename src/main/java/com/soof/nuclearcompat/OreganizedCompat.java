package com.soof.nuclearcompat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.LootTableLoadEvent;

/**
 * 与「井然有矿（Oreganized）」的可选联动。
 *
 * <p>仅当 Oreganized 已加载时生效：把本模组秽岩铅矿石的战利品表替换为
 * 「精准采集掉本体、否则掉 {@code oreganized:raw_lead}（时运加成）」。</p>
 *
 * <p>铅矿石的「铅尘」效果由 Oreganized 自己的 BlockEvent.BreakEvent 处理，
 * 只需通过 {@code data/oreganized/tags/block/creates_lead_cloud.json}
 * 把方块加入对应标签即可，无需额外代码。</p>
 */
@EventBusSubscriber(modid = NuclearCompat.MODID)
public final class OreganizedCompat {

    private static final String OREGANIZED = "oreganized";
    private static final ResourceLocation ORE_TABLE =
            ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "blocks/coprolith_lead_ore");
    private static final ResourceLocation ORG_RAW_LEAD =
            ResourceLocation.fromNamespaceAndPath(OREGANIZED, "raw_lead");

    /** 与原本的战利品表结构一致，只把掉落物换成 Oreganized 的粗铅。 */
    private static final String TABLE_JSON = """
            {
              "type": "minecraft:block",
              "pools": [
                {
                  "bonus_rolls": 0.0,
                  "rolls": 1.0,
                  "entries": [
                    {
                      "type": "minecraft:alternatives",
                      "children": [
                        {
                          "type": "minecraft:item",
                          "name": "alexscaves_nuclear_convergence:coprolith_lead_ore",
                          "conditions": [
                            {
                              "condition": "minecraft:match_tool",
                              "predicate": {
                                "predicates": {
                                  "minecraft:enchantments": [
                                    {
                                      "enchantments": "minecraft:silk_touch",
                                      "levels": { "min": 1 }
                                    }
                                  ]
                                }
                              }
                            }
                          ]
                        },
                        {
                          "type": "minecraft:item",
                          "name": "oreganized:raw_lead",
                          "functions": [
                            {
                              "function": "minecraft:apply_bonus",
                              "enchantment": "minecraft:fortune",
                              "formula": "minecraft:ore_drops"
                            },
                            { "function": "minecraft:explosion_decay" }
                          ]
                        }
                      ]
                    }
                  ]
                }
              ]
            }
            """;

    private OreganizedCompat() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!ModList.get().isLoaded(OREGANIZED)) {
            return;
        }
        if (!ORE_TABLE.equals(event.getName())) {
            return;
        }
        Item rawLead = BuiltInRegistries.ITEM.get(ORG_RAW_LEAD);
        if (rawLead == Items.AIR) {
            return;
        }

        JsonObject json = JsonParser.parseString(TABLE_JSON).getAsJsonObject();
        LootTable table = LootTable.DIRECT_CODEC
                .parse(event.getRegistries().createSerializationContext(JsonOps.INSTANCE), json)
                .getOrThrow();
        event.setTable(table);
    }
}
