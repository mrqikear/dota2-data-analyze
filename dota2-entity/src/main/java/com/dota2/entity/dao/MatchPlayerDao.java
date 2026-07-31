package com.dota2.entity.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dota2.entity.entity.MatchPlayerEntity;
import com.dota2.entity.vo.MatchPlayerVo;
import org.apache.ibatis.annotations.Param;

import com.dota2.entity.vo.HeroStatsVo;
import java.util.List;

public interface MatchPlayerDao extends BaseMapper<MatchPlayerEntity> {

    List<MatchPlayerVo> pageWithMain(@Param("steamId") String steamId,
                                     @Param("gameMode") Integer gameMode,
                                     @Param("heroId") Integer heroId,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit,
                                     @Param("sortField") String sortField,
                                     @Param("sortOrder") String sortOrder,
                                     @Param("parsed") Boolean parsed);

    long countWithMain(@Param("steamId") String steamId,
                       @Param("gameMode") Integer gameMode,
                       @Param("heroId") Integer heroId,
                       @Param("parsed") Boolean parsed);

    /**
     * Get player steam_id and nick_name for a match (used in match detail).
     */
    List<MatchPlayerVo> listPlayersByMatch(@Param("matchId") Long matchId);

    /**
     * Hero stats grouped by hero, with recent 20/50 using window functions.
     */
    List<HeroStatsVo> queryHeroStats(@Param("steamId") String steamId,
                                     @Param("matchType") Integer matchType,
                                     @Param("startTime") Long startTime,
                                     @Param("endTime") Long endTime,
                                     @Param("minMatches") int minMatches,
                                     @Param("sortField") String sortField,
                                     @Param("sortOrder") String sortOrder);

    /**
     * Find match IDs where ALL given steam_ids appeared in the same match.
     */
    List<Long> findRelatedMatchIds(@Param("steamIds") List<String> steamIds,
                                   @Param("count") int count);

    List<Long> findOpponentMatchIds(@Param("steamIds") List<String> steamIds,
                                    @Param("count") int count,
                                    @Param("opponentId") String opponentId);

    List<Long> findSoloMatchIds(@Param("steamId") String steamId,
                                @Param("excludeIds") List<String> excludeIds,
                                @Param("offset") int offset,
                                @Param("limit") int limit,
                                @Param("gameMode") Integer gameMode);

    long countSoloMatchIds(@Param("steamId") String steamId,
                           @Param("excludeIds") List<String> excludeIds,
                           @Param("gameMode") Integer gameMode);

    List<java.util.Map<String, Object>> querySoloPlayerStats(@Param("steamId") String steamId,
                                                              @Param("excludeIds") List<String> excludeIds,
                                                              @Param("gameMode") Integer gameMode);

    List<java.util.Map<String, Object>> querySoloMvpStats(@Param("steamId") String steamId,
                                                           @Param("excludeIds") List<String> excludeIds,
                                                           @Param("gameMode") Integer gameMode);

    List<Long> findMatchIdsBySteamId(@Param("steamId") String steamId,
                                      @Param("limit") int limit);

    List<java.util.Map<String, Object>> queryPlayerStats(@Param("steamId") String steamId,
                                                          @Param("byMode") boolean byMode,
                                                          @Param("gameMode") Integer gameMode);

    List<java.util.Map<String, Object>> queryPlayerHeroStats(@Param("steamId") String steamId,
                                                              @Param("gameMode") Integer gameMode);
}
