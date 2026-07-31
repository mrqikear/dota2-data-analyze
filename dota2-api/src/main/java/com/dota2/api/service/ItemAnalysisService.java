package com.dota2.api.service;

import com.dota2.entity.dao.ItemAnalysisDao;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ItemAnalysisService {

    private static final List<String> EXCLUDED_STARTING_ITEMS = List.of(
            "tango", "flask", "clarity", "ward_observer", "ward_sentry",
            "smoke_of_deceit", "dust", "faerie_fire", "blood_grenade",
            "tpscroll", "bottle", "enchanted_mango", "cheese",
            "courier", "flying_courier"
    );

    /** Items excluded from build routes (consumables + trivial cheap items). */
    private static final List<String> EXCLUDED_BUILD_ITEMS = List.of(
            "tango", "flask", "clarity", "ward_observer", "ward_sentry",
            "smoke_of_deceit", "dust", "faerie_fire", "blood_grenade",
            "tpscroll", "bottle", "enchanted_mango", "cheese",
            "courier", "flying_courier", "tango_single", "ward_dispenser",
            "recipe_magic_wand", "recipe_bracer", "recipe_wraith_band",
            "recipe_null_talisman", "recipe_ring_of_basilius",
            "recipe_buckler", "recipe_headdress"
    );

    // ============ Archetype definitions (item IDs → group) ============

    private static final Set<Integer> PHYSICAL_CORE = Set.of(
            141, 135, 156, 139, 168, 208, 250, 225, 249,
            145, 134, 143, 158, 166, 149, 152, 154,
            252, 942, 277
    );

    private static final Set<Integer> MAGICAL_CORE = Set.of(
            104, 176, 235, 119, 121, 100, 96, 108, 174, 534, 911,
            232, 97, 98, 110, 259, 273, 228,
            1107, 1097
    );

    private static final Set<Integer> TANK_ITEMS = Set.of(
            114, 112, 242, 226, 256, 692, 90, 125, 131,
            116, 127, 151, 243
    );

    private static final Set<Integer> SUPPORT_ITEMS = Set.of(
            231, 79, 229, 267, 254, 102, 269, 232,
            187, 190, 206, 1128, 230, 226
    );

    private static final Set<Integer> MOBILITY_ITEMS = Set.of(
            1, 600, 603, 604, 263, 931, 220, 48, 50, 63, 214
    );

    /** Map of archetype name → item ID set */
    private static final Map<String, Set<Integer>> ARCHETYPE_GROUPS = new LinkedHashMap<>();
    static {
        ARCHETYPE_GROUPS.put("物理核心", PHYSICAL_CORE);
        ARCHETYPE_GROUPS.put("法系核心", MAGICAL_CORE);
        ARCHETYPE_GROUPS.put("防御/开团", TANK_ITEMS);
        ARCHETYPE_GROUPS.put("辅助装备", SUPPORT_ITEMS);
        ARCHETYPE_GROUPS.put("切入/机动", MOBILITY_ITEMS);
    }

    @Autowired
    private ItemAnalysisDao itemAnalysisDao;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ========================================================================
    // 4.1 出门装组合
    // ========================================================================
    public List<Map<String, Object>> getStartingItems(String steamId) {
        String excludeCsv = EXCLUDED_STARTING_ITEMS.stream()
                .map(s -> "'" + s + "'")
                .collect(Collectors.joining(","));
        List<Map<String, Object>> rows = itemAnalysisDao.queryStartingItems(steamId, excludeCsv);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            try {
                String itemsJson = (String) row.get("items_json");
                long games = ((Number) row.get("games")).longValue();
                long wins = ((Number) row.get("wins")).longValue();
                List<String> items = objectMapper.readValue(itemsJson, new TypeReference<List<String>>() {});
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("items", items);
                item.put("games", games);
                item.put("wins", wins);
                item.put("winRate", Math.round(100.0 * wins / games * 10.0) / 10.0);
                result.add(item);
            } catch (Exception e) {
                log.warn("parse starting item row failed: {}", row, e);
            }
        }
        return result;
    }

    // ========================================================================
    // 4.3 单件装备分析
    // ========================================================================
    public List<Map<String, Object>> getItemStats(String steamId, Integer heroId) {
        List<Map<String, Object>> rows = itemAnalysisDao.queryItemStats(steamId, heroId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("itemId", ((Number) row.get("item_id")).intValue());
            item.put("games", ((Number) row.get("games")).longValue());
            item.put("wins", ((Number) row.get("wins")).longValue());
            item.put("winRate", row.get("win_rate"));
            result.add(item);
        }
        return result;
    }

    // ========================================================================
    // 4.4 装备胜率贡献
    // ========================================================================
    public List<Map<String, Object>> getWinContribution(String steamId, Integer heroId) {
        // 1. Get total stats for baseline
        Map<String, Object> totalStats = itemAnalysisDao.queryTotalStats(steamId, heroId);
        long totalGames = ((Number) totalStats.getOrDefault("total_games", 0)).longValue();
        long totalWins = ((Number) totalStats.getOrDefault("total_wins", 0)).longValue();
        double baselineWinRate = totalGames > 0 ? 100.0 * totalWins / totalGames : 50.0;

        // 2. Get per-item stats
        List<Map<String, Object>> itemRows = itemAnalysisDao.queryItemStats(steamId, heroId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : itemRows) {
            int itemId = ((Number) row.get("item_id")).intValue();
            long itemGames = ((Number) row.get("games")).longValue();
            long itemWins = ((Number) row.get("wins")).longValue();
            double withItemWR = ((Number) row.get("win_rate")).doubleValue();

            // Win rate WITHOUT this item
            long withoutGames = totalGames - itemGames;
            long withoutWins = totalWins - itemWins;
            double withoutItemWR = withoutGames > 0 ? 100.0 * withoutWins / withoutGames : 0;

            double delta = Math.round((withItemWR - withoutItemWR) * 10.0) / 10.0;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("itemId", itemId);
            item.put("games", itemGames);
            item.put("wins", itemWins);
            item.put("withItemWinRate", withItemWR);
            item.put("withoutItemWinRate", Math.round(withoutItemWR * 10.0) / 10.0);
            item.put("baselineWinRate", Math.round(baselineWinRate * 10.0) / 10.0);
            item.put("delta", delta);
            result.add(item);
        }

        // Sort by delta descending
        result.sort((a, b) -> Double.compare(
                ((Number) b.get("delta")).doubleValue(),
                ((Number) a.get("delta")).doubleValue()
        ));
        return result;
    }

    // ========================================================================
    // 4.2 大件出装路线
    // ========================================================================
    public List<Map<String, Object>> getBuildRoutes(String steamId, Integer heroId, Integer topN) {
        if (topN == null || topN < 2) topN = 5;
        if (topN > 10) topN = 10;

        String excludeCsv = EXCLUDED_BUILD_ITEMS.stream()
                .map(s -> "'" + s + "'")
                .collect(Collectors.joining(","));

        List<Map<String, Object>> rows = itemAnalysisDao.queryBuildRoutes(steamId, heroId, topN, excludeCsv);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            try {
                String itemsJson = (String) row.get("items_json");
                String timesJson = (String) row.get("times_json");
                long games = ((Number) row.get("games")).longValue();
                long wins = ((Number) row.get("wins")).longValue();

                List<String> items = objectMapper.readValue(itemsJson, new TypeReference<List<String>>() {});
                List<Integer> times = objectMapper.readValue(timesJson, new TypeReference<List<Integer>>() {});

                Map<String, Object> route = new LinkedHashMap<>();
                route.put("items", items);
                route.put("purchaseTimes", times);
                route.put("games", games);
                route.put("wins", wins);
                route.put("winRate", Math.round(100.0 * wins / games * 10.0) / 10.0);
                route.put("avgFirstTime", row.get("avg_first_time"));
                result.add(route);
            } catch (Exception e) {
                log.warn("parse build route row failed: {}", row, e);
            }
        }
        return result;
    }

    // ========================================================================
    // 4.6 流派自动识别
    // ========================================================================
    public List<Map<String, Object>> getArchetypeStats(String steamId, Integer heroId) {
        List<Map<String, Object>> rows = itemAnalysisDao.queryArchetypeData(steamId, heroId);

        // Count per archetype
        Map<String, ArchetypeAccumulator> acc = new LinkedHashMap<>();
        for (String name : ARCHETYPE_GROUPS.keySet()) {
            acc.put(name, new ArchetypeAccumulator());
        }
        acc.put("未分类", new ArchetypeAccumulator());

        for (Map<String, Object> row : rows) {
            try {
                boolean win = Boolean.TRUE.equals(row.get("win"));
                String itemsJson = ((String) row.get("items_json"))
                        .replace("[", "").replace("]", "").replace(" ", "");
                List<Integer> itemIds = new ArrayList<>();
                if (!itemsJson.isEmpty()) {
                    for (String s : itemsJson.split(",")) {
                        try { itemIds.add(Integer.parseInt(s.trim())); } catch (Exception ignored) {}
                    }
                }

                if (itemIds.isEmpty()) continue;

                // Count how many items match each archetype
                String bestArchetype = "未分类";
                int bestCount = 0;
                for (Map.Entry<String, Set<Integer>> entry : ARCHETYPE_GROUPS.entrySet()) {
                    int count = 0;
                    for (int id : itemIds) {
                        if (entry.getValue().contains(id)) count++;
                    }
                    if (count > bestCount) {
                        bestCount = count;
                        bestArchetype = entry.getKey();
                    }
                }

                ArchetypeAccumulator a = acc.get(bestArchetype);
                if (a != null) {
                    a.games++;
                    if (win) a.wins++;
                    a.goldPerMin += ((Number) row.getOrDefault("gold_per_min", 0)).intValue();
                    a.xpPerMin += ((Number) row.getOrDefault("xp_per_min", 0)).intValue();
                    a.kills += ((Number) row.getOrDefault("kills", 0)).intValue();
                    a.deaths += ((Number) row.getOrDefault("deaths", 0)).intValue();
                    a.assists += ((Number) row.getOrDefault("assists", 0)).intValue();
                    a.heroDamage += ((Number) row.getOrDefault("hero_damage", 0)).longValue();
                    a.towerDamage += ((Number) row.getOrDefault("tower_damage", 0)).longValue();
                }
            } catch (Exception e) {
                log.warn("parse archetype row failed: {}", row, e);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, ArchetypeAccumulator> entry : acc.entrySet()) {
            ArchetypeAccumulator a = entry.getValue();
            if (a.games == 0) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("archetype", entry.getKey());
            item.put("games", a.games);
            item.put("wins", a.wins);
            item.put("winRate", Math.round(100.0 * a.wins / a.games * 10.0) / 10.0);
            item.put("avgKills", Math.round(1.0 * a.kills / a.games * 10.0) / 10.0);
            item.put("avgDeaths", Math.round(1.0 * a.deaths / a.games * 10.0) / 10.0);
            item.put("avgAssists", Math.round(1.0 * a.assists / a.games * 10.0) / 10.0);
            item.put("avgGpm", a.goldPerMin / a.games);
            item.put("avgXpm", a.xpPerMin / a.games);
            item.put("avgHeroDamage", a.heroDamage / a.games);
            item.put("avgTowerDamage", a.towerDamage / a.games);
            result.add(item);
        }
        result.sort((a, b) -> Long.compare(
                ((Number) b.get("games")).longValue(),
                ((Number) a.get("games")).longValue()
        ));
        return result;
    }

    // ========================================================================
    // 4.5 个人 vs 全球对比
    // ========================================================================
    public Map<String, Object> getCompareGlobal(String steamId, Integer heroId) {
        if (steamId == null || heroId == null) {
            return Collections.emptyMap();
        }

        // 1. Personal stats from DB → per-item usage count
        List<Map<String, Object>> personalRows = itemAnalysisDao.queryPersonalItemUsage(steamId, heroId);
        long totalPersonalGames = personalRows.stream()
                .mapToLong(r -> ((Number) r.get("games")).longValue())
                .sum();

        // 2. Global stats from OpenDota (returns raw counts, normalize to %)
        Map<Integer, Map<String, Object>> globalMap = new HashMap<>();
        Map<String, Long> phaseTotals = new HashMap<>();

        try {
            String url = "https://api.opendota.com/api/heroes/" + heroId + "/itemPopularity";
            org.springframework.web.client.RestTemplate rt = new org.springframework.web.client.RestTemplate();
            String globalJson = rt.getForObject(url, String.class);
            if (globalJson != null) {
                com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(globalJson);
                String[] phases = {"start_game_items", "early_game_items", "mid_game_items", "late_game_items"};

                // First pass: compute total per phase
                for (String phase : phases) {
                    long total = 0;
                    com.fasterxml.jackson.databind.JsonNode phaseData = root.path(phase);
                    if (phaseData.isObject()) {
                        Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = phaseData.fields();
                        while (fields.hasNext()) {
                            total += fields.next().getValue().asLong(0);
                        }
                    }
                    phaseTotals.put(phase, total);
                }

                // Second pass: normalize to percentage
                for (String phase : phases) {
                    com.fasterxml.jackson.databind.JsonNode phaseData = root.path(phase);
                    long phaseTotal = phaseTotals.getOrDefault(phase, 1L);
                    if (phaseData.isObject() && phaseTotal > 0) {
                        Iterator<Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = phaseData.fields();
                        while (fields.hasNext()) {
                            Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> field = fields.next();
                            try {
                                int itemId = Integer.parseInt(field.getKey());
                                double pct = Math.round(10000.0 * field.getValue().asLong(0) / phaseTotal) / 100.0;
                                Map<String, Object> globalItem = globalMap.computeIfAbsent(itemId, k -> new LinkedHashMap<>());
                                globalItem.put("itemId", itemId);
                                globalItem.put(phase + "_pct", pct);
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch OpenDota global data for hero {}: {}", heroId, e.getMessage());
        }

        // 3. Merge personal + global, compute differences
        List<Map<String, Object>> merged = new ArrayList<>();
        String[] phases = {"start", "early", "mid", "late"};
        String[] phaseKeys = {"start_game_items", "early_game_items", "mid_game_items", "late_game_items"};

        for (Map<String, Object> personalRow : personalRows) {
            int itemId = ((Number) personalRow.get("item_id")).intValue();
            long personalGames = ((Number) personalRow.get("games")).longValue();
            double personalPct = totalPersonalGames > 0
                    ? Math.round(10000.0 * personalGames / totalPersonalGames) / 100.0
                    : 0;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("itemId", itemId);
            item.put("personalGames", personalGames);
            item.put("personalPct", personalPct);
            item.put("personalWinRate", personalRow.get("win_rate"));

            Map<String, Object> globalItem = globalMap.get(itemId);
            double maxGlobalPct = 0;
            String bestPhase = "mid";
            for (int i = 0; i < phases.length; i++) {
                double gp = 0;
                if (globalItem != null) {
                    Object v = globalItem.get(phaseKeys[i] + "_pct");
                    gp = v instanceof Number ? ((Number) v).doubleValue() : 0;
                }
                item.put("global" + capitalize(phases[i]) + "Pct", gp);
                if (gp > maxGlobalPct) {
                    maxGlobalPct = gp;
                    bestPhase = phases[i];
                }
            }
            item.put("bestPhase", bestPhase);
            item.put("globalPct", maxGlobalPct); // best phase % for comparison

            // Difference: personal usage vs global best-phase usage
            double diff = Math.round((personalPct - maxGlobalPct) * 100.0) / 100.0;
            item.put("diff", diff);

            merged.add(item);
        }

        // Add global-only items not in personal data
        for (Map.Entry<Integer, Map<String, Object>> entry : globalMap.entrySet()) {
            int globalItemId = entry.getKey();
            if (merged.stream().anyMatch(m -> ((Number) m.get("itemId")).intValue() == globalItemId)) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("itemId", globalItemId);
            item.put("personalGames", 0);
            item.put("personalPct", 0);
            item.put("personalWinRate", 0);

            double maxGlobalPct = 0;
            String bestPhase = "mid";
            for (int i = 0; i < phases.length; i++) {
                Object v = entry.getValue().get(phaseKeys[i] + "_pct");
                double gp = v instanceof Number ? ((Number) v).doubleValue() : 0;
                item.put("global" + capitalize(phases[i]) + "Pct", gp);
                if (gp > maxGlobalPct) {
                    maxGlobalPct = gp;
                    bestPhase = phases[i];
                }
            }
            item.put("bestPhase", bestPhase);
            item.put("globalPct", maxGlobalPct);
            item.put("diff", -maxGlobalPct); // personal=0, so diff = -global
            merged.add(item);
        }

        // 4. Generate highlight messages
        List<Map<String, Object>> highlights = new ArrayList<>();
        for (Map<String, Object> item : merged) {
            double diff = ((Number) item.get("diff")).doubleValue();
            if (Math.abs(diff) >= 5) { // threshold: 5% difference
                Map<String, Object> h = new LinkedHashMap<>();
                h.put("itemId", item.get("itemId"));
                h.put("personalPct", item.get("personalPct"));
                h.put("globalPct", item.get("globalPct"));
                h.put("diff", diff);
                h.put("direction", diff > 0 ? "more" : "less");
                highlights.add(h);
            }
        }
        highlights.sort((a, b) -> Double.compare(
                Math.abs(((Number) b.get("diff")).doubleValue()),
                Math.abs(((Number) a.get("diff")).doubleValue())
        ));

        // Sort merged by |diff| desc for display
        merged.sort((a, b) -> Double.compare(
                Math.abs(((Number) b.get("diff")).doubleValue()),
                Math.abs(((Number) a.get("diff")).doubleValue())
        ));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("heroId", heroId);
        result.put("totalGames", totalPersonalGames);
        result.put("highlights", highlights);
        result.put("items", merged);
        return result;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ============ Helper ============

    private static class ArchetypeAccumulator {
        long games = 0;
        long wins = 0;
        long kills = 0, deaths = 0, assists = 0;
        long goldPerMin = 0, xpPerMin = 0;
        long heroDamage = 0, towerDamage = 0;
    }
}
