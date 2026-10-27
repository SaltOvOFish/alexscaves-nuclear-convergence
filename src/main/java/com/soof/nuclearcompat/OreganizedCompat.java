package com.soof.nuclearcompat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import java.util.Map;

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
 * <p>仅当 Oreganized 已加载时生效：把本模组两种铅矿石（秽岩铅矿石 / 辐射铅矿石）的战利品表
 * 替换为「精准采集掉本体、否则掉 {@code oreganized:raw_lead}」。</p>
 *
 * <p>铅矿石的「铅尘」效果由 Oreganized 自己的 {@code BlockEvent.BreakEvent} 处理
 * （{@code LeadOreBlock#shouldSpawnCloud} 只判断方块是否在 {@code oreganized:creates_lead_cloud}
 * 方块标签里），因此只需通过 {@code data/oreganized/tags/block/creates_lead_cloud.json}
 * 把两种铅矿石都加进标签即可，无需额外代码。</p>
 */
@EventBusSubscriber(modid = NuclearCompat.MODID)
public final class OreganizedCompat {

    private static final String OREGANIZED = "oreganized";

    private static final ResourceLocation ORG_RAW_LEAD =
            ResourceLocation.fromNamespaceAndPath(OREGANIZED, "raw_lead");

    /** 秽岩铅矿石：单件掉落 + ore_drops 时运。 */
    private static final String COPROLITH_JSON = """
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

    /** 辐射铅矿石：2~4 件掉落 + uniform_bonus_count(×4) 时运（与原表一致，只换掉落物）。 */
    private static final String RADROCK_JSON = """
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
                          "name": "alexscaves_nuclear_convergence:radrock_lead_ore",
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
                              "add": false,
                              "count": {
                                "type": "minecraft:uniform",
                                "max": 4.0,
                                "min": 2.0
                              },
                              "function": "minecraft:set_count"
                            },
                            {
                              "enchantment": "minecraft:fortune",
                              "formula": "minecraft:uniform_bonus_count",
                              "function": "minecraft:apply_bonus",
                              "parameters": {
                                "bonusMultiplier": 4
                              }
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

    /** 战利品表名 -> 替换用的 JSON。 */
    private static final Map<ResourceLocation, String> TABLE_OVERRIDES = Map.of(
            ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "blocks/coprolith_lead_ore"), COPROLITH_JSON,
            ResourceLocation.fromNamespaceAndPath(NuclearCompat.MODID, "blocks/radrock_lead_ore"), RADROCK_JSON
    );

    private OreganizedCompat() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!ModList.get().isLoaded(OREGANIZED)) {
            return;
        }
        String override = TABLE_OVERRIDES.get(event.getName());
        if (override == null) {
            return;
        }
        Item rawLead = BuiltInRegistries.ITEM.get(ORG_RAW_LEAD);
        if (rawLead == Items.AIR) {
            return;
        }

        JsonObject json = JsonParser.parseString(override).getAsJsonObject();
        LootTable table = LootTable.DIRECT_CODEC
                .parse(event.getRegistries().createSerializationContext(JsonOps.INSTANCE), json)
                .getOrThrow();
        event.setTable(table);
    }
}
