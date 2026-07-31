package com.dota2.entity.dao;

import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * DAO for item analysis queries against match_detail.raw_json.
 */
public interface ItemAnalysisDao {

    /**
     * 4.1 Starting item combos
     */
    List<Map<String, Object>> queryStartingItems(
            @Param("steamId") String steamId,
            @Param("excludeItems") String excludeItems
    );

    /**
     * 4.3 Single item stats: final item usage + win rate.
     */
    List<Map<String, Object>> queryItemStats(
            @Param("steamId") String steamId,
            @Param("heroId") Integer heroId
    );

    /**
     * Total games/wins for a filter (used by win contribution).
     */
    Map<String, Object> queryTotalStats(
            @Param("steamId") String steamId,
            @Param("heroId") Integer heroId
    );

    /**
     * 4.2 Item build routes: extract first N non-consumable purchased items.
     */
    List<Map<String, Object>> queryBuildRoutes(
            @Param("steamId") String steamId,
            @Param("heroId") Integer heroId,
            @Param("topN") Integer topN,
            @Param("excludeItems") String excludeItems
    );

    /**
     * 4.6 Archetype data: per-player final items + win.
     */
    List<Map<String, Object>> queryArchetypeData(
            @Param("steamId") String steamId,
            @Param("heroId") Integer heroId
    );

    /**
     * 4.5 Personal item usage for global comparison.
     */
    List<Map<String, Object>> queryPersonalItemUsage(
            @Param("steamId") String steamId,
            @Param("heroId") Integer heroId
    );
}
