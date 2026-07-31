package com.dota2.api.controller;

import com.dota2.common.utils.Result;
import com.dota2.entity.entity.AssetCacheEntity;
import com.dota2.api.service.AssetCacheService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

@RestController
@RequestMapping(value = "/asset")
@Validated
@Slf4j
@Api(tags = "asset cache")
public class AssetController {

    @Autowired
    private AssetCacheService assetCacheService;

    private static final String CDN_BASE = "https://cdn.cloudflare.steamstatic.com";

    @ApiOperation(value = "get hero icon (cached locally)")
    @GetMapping(value = "/hero/{heroName:.+}", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] heroIcon(@PathVariable String heroName) {
        String name = heroName.replaceAll("\\.png$", "");
        String key = "hero_" + name;
        String cached = assetCacheService.getCached("hero", key);
        if (cached != null) {
            return Base64.getDecoder().decode(cached);
        }
        String url = CDN_BASE + "/apps/dota2/images/dota_react/heroes/" + name + ".png";
        byte[] data = assetCacheService.downloadAndCache("hero", key, url, "image/png");
        return data;
    }

    @ApiOperation(value = "get item icon (cached locally)")
    @GetMapping(value = "/item/{itemName:.+}", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] itemIcon(@PathVariable String itemName) {
        String name = itemName.replaceAll("\\.png$", "");
        String key = "item_" + name;
        String cached = assetCacheService.getCached("item", key);
        if (cached != null) {
            return Base64.getDecoder().decode(cached);
        }
        String url = CDN_BASE + "/apps/dota2/images/dota_react/items/" + name + ".png";
        byte[] data = assetCacheService.downloadAndCache("item", key, url, "image/png");
        return data;
    }

    @ApiOperation(value = "get ability icon (cached locally)")
    @GetMapping(value = "/ability/{abilityName:.+}", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] abilityIcon(@PathVariable String abilityName) {
        String name = abilityName.replaceAll("\\.png$", "");
        String key = "ability_" + name;
        String cached = assetCacheService.getCached("ability", key);
        if (cached != null) {
            return Base64.getDecoder().decode(cached);
        }
        String url = CDN_BASE + "/apps/dota2/images/dota_react/abilities/" + name + ".png";
        byte[] data = assetCacheService.downloadAndCache("ability", key, url, "image/png");
        return data;
    }

    @ApiOperation(value = "prefetch all item icons")
    @PostMapping(value = "/prefetchItems")
    public Result<String> prefetchItems() {
        String[] items = {"blink","blades_of_attack","broadsword","chainmail","claymore","helm_of_iron_will",
                "javelin","mithril_hammer","platemail","quarterstaff","quelling_blade","ring_of_protection",
                "gauntlets","slippers","mantle","branches","belt_of_strength","boots_of_elves","robe","circlet",
                "ogre_axe","blade_of_alacrity","staff_of_wizardry","ultimate_orb","gloves","lifesteal",
                "ring_of_regen","sobi_mask","boots","gem","cloak","talisman_of_evasion","cheese","magic_stick",
                "recipe_magic_wand","magic_wand","ghost","clarity","flask","dust","bottle","ward_observer",
                "ward_sentry","tango","courier","tpscroll","recipe_travel_boots","travel_boots","phase_boots",
                "power_treads","mana_boots","tranquil_boots","guardian_greaves","soul_ring","recipe_soul_ring",
                "arcane_ring","octarine_core","sange_and_yasha","refresher","null_talisman","sange","yasha",
                "kaya","hand_of_midas","recipe_hand_of_midas","oblivion_staff","pers","power_treads_int",
                "power_treads_str","power_treads_agi","nullifier","desolator","recipe_desolator","black_king_bar",
                "recipe_black_king_bar","aegis","recipe_aether_lens","aether_lens","dragon_lance","hurricane_pike",
                "recipe_hurricane_pike","force_staff","recipe_force_staff","aghanims_shard","crimson_guard",
                "recipe_crimson_guard","guardian_greaves_3","pipe","recipe_pipe","urn_of_shadows","spirit_vessel",
                "recipe_spirit_vessel","headdress","recipe_headdress","mekansm","recipe_mekansm","solar_crest",
                "recipe_solar_crest","buckler","recipe_buckler","vladmir","bracer","recipe_bracer","wraith_band",
                "recipe_wraith_band","infused_raindrop","boots_of_bearing_2","lotus_orb","recipe_lotus_orb",
                "soul_booster","recipe_soul_booster","aeon_disk","recipe_aeon_disk","travel_boots_2",
                "travel_boots_3","wind_lace","veil_of_discord","recipe_veil_of_discord","echo_sabre",
                "recipe_echo_sabre","yasha_and_kaya","kaya_and_sange","holy_locket","recipe_holy_locket",
                "rapier","divine_rapier","monkey_king_bar","radiance","butterfly","recipe_butterfly",
                "daedalus","recipe_daedalus","skull_basher","recipe_skull_basher","battle_fury",
                "recipe_battle_fury","manta_style","recipe_manta_style","sange_and_yasha_2",
                "satanic","recipe_satanic","mjollnir","recipe_mjollnir","silver_edge","recipe_silver_edge",
                "glimmer_cape","recipe_glimmer_cape","aghanims_scepter","refresher_recipe","refresher_shard",
                "heart","recipe_heart","assault_cuirass","recipe_assault","bloodstone","recipe_bloodstone",
                "shivas_guard","recipe_shivas_guard","sheepstick","recipe_sheepstick","orchid","recipe_orchid",
                "cyclone","recipe_cyclone","abyssal_blade","recipe_abyssal_blade","ethereal_blade",
                "recipe_ethereal_blade","smoke_of_deceit","helm_of_the_dominator","recipe_helm_of_the_dominator",
                "maelstrom","recipe_maelstrom","greater_crit","recipe_greater_crit","armlet","recipe_armlet",
                "invis_sword","recipe_invis_sword","lesser_crit","recipe_lesser_crit","dagon",
                "recipe_dagon","dagon_2","dagon_3","dagon_4","dagon_5","necronomicon","recipe_necronomicon",
                "necronomicon_2","necronomicon_3","ultimate_scepter_2","ultimate_scepter_3",
                "moon_shard","mask_of_madness","sange_kaya_yasha","boots_of_bearing","overwhelming_blink",
                "swift_blink","arcane_blink","wind_waker","tranquil_boots_2","tranquil_boots_3",
                "ring_of_tarrasque","vambrace","vambrace_upgrade","elixir","pogo_stick","fae_grenade",
                "double_axe","specialist_array","trusty_shovel","book_of_shadows","ogre_seal_totem",
                "seer_stone","minotaur_horn","trickster_cloak","gris_gris","magical_lamp","paladin_sword",
                "titan_sliver","pirate_hat","clumsy_net","nimble_robber","mind_breaker","elven_tunic",
                "ceremonial_robe","vengeances_shadow","spark_of_courage","giants_ring","stormcrafter",
                "rattlecage","oakheart","psychic_headband","repair_kit","ancient_purse","bulwark",
                "force_field","mercy_gift","warhammer","unicorn_horn","lance_of_perseus","magnifying_glass",
                "essence_ring","orb_of_revelations","dandelion_pendant","hunter's_knife","dragon_scale",
                "book_of_the_dead","fallen_sky","arcane_ring","royal_jelly","omniscient_monocle",
                "philosophers_stone","broom_handle","imp_claw","flicker","spider_legs","spider_legs_2",
                "ironwood_tree","mirror_shield","orb_of_destruction","reprisal","ballista","completion_pack",
                "completion_pack_2","container","techies_minefield_sign","container_2","lotus_pool_coupon",
                "aghanims_shard_2","blind_deaf_zombie_junk"};
        int count = 0;
        for (String name : items) {
            String key = "item_" + name;
            if (assetCacheService.getCached("item", key) == null) {
                String url = CDN_BASE + "/apps/dota2/images/dota_react/items/" + name + ".png";
                assetCacheService.downloadAndCache("item", key, url, "image/png");
                count++;
            }
        }
        return Result.ok("装备图标预缓存完成，新增 " + count + " 个");
    }

    @ApiOperation(value = "prefetch all hero icons")
    @PostMapping(value = "/prefetchHeroes")
    public Result<String> prefetchHeroes() {
        String[] heroes = {"antimage","axe","bane","bloodseeker","crystal_maiden","drow_ranger","earthshaker",
                "juggernaut","mirana","morphling","nevermore","phantom_lancer","puck","pudge","razor","sand_king",
                "storm_spirit","sven","tiny","vengefulspirit","windrunner","zuus","kunkka","lina","lion",
                "shadow_shaman","slardar","tidehunter","witch_doctor","lich","riki","enigma","tinker","sniper",
                "necrolyte","warlock","beastmaster","queenofpain","venomancer","faceless_void","skeleton_king",
                "death_prophet","phantom_assassin","pugna","templar_assassin","viper","luna","dragon_knight",
                "dazzle","rattletrap","leshrac","furion","life_stealer","dark_seer","clinkz","omniknight",
                "enchantress","huskar","night_stalker","broodmother","bounty_hunter","weaver","jakiro","batrider",
                "chen","spectre","ancient_apparition","doom_bringer","ursa","spirit_breaker","gyrocopter",
                "alchemist","invoker","silencer","obsidian_destroyer","lycan","brewmaster","shadow_demon",
                "lone_druid","chaos_knight","meepo","treant","ogre_magi","undying","rubick","disruptor",
                "nyx_assassin","naga_siren","keeper_of_the_light","wisp","visage","slark","medusa","troll_warlord",
                "centaur","magnataur","shredder","bristleback","tusk","skywrath_mage","abaddon","elder_titan",
                "legion_commander","techies","ember_spirit","earth_spirit","abyssal_underlord","terrorblade",
                "phoenix","oracle","winter_wyvern","arc_warden","monkey_king","dark_willow","pangolier",
                "grimstroke","hoodwink","void_spirit","snapfire","mars","ringmaster","dawnbreaker","marci",
                "primal_beast","muerta","kez","largo"};
        int count = 0;
        for (String h : heroes) {
            String key = "hero_" + h;
            if (assetCacheService.getCached("hero", key) == null) {
                String url = CDN_BASE + "/apps/dota2/images/dota_react/heroes/" + h + ".png";
                assetCacheService.downloadAndCache("hero", key, url, "image/png");
                count++;
            }
        }
        return Result.ok("预缓存完成，新增 " + count + " 个英雄图标");
    }
}
