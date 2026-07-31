package com.dota2.entity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("hero_daily_stats")
public class HeroDailyStatsEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer heroId;
    private String heroName;
    private Long pubPicks;
    private Long pubWins;
    private Long turboPicks;
    private Long turboWins;
    private String pubPicksTrend;
    private String pubWinsTrend;
    private String turboPicksTrend;
    private String turboWinsTrend;
    private LocalDate fetchDate;
    private LocalDateTime createdTime;
}
