-- 全英雄每日胜率快照表
-- 由定时任务每日从 OpenDota /api/heroStats 爬取
CREATE TABLE IF NOT EXISTS hero_daily_stats (
    id BIGSERIAL PRIMARY KEY,
    hero_id INT NOT NULL,
    snap_date DATE NOT NULL DEFAULT CURRENT_DATE,
    
    -- 普通匹配（含天梯）统计
    pub_picks INT NOT NULL DEFAULT 0,
    pub_wins INT NOT NULL DEFAULT 0,
    pub_win_rate DECIMAL(5,1) DEFAULT 0,
    
    -- 加速模式统计
    turbo_picks INT NOT NULL DEFAULT 0,
    turbo_wins INT NOT NULL DEFAULT 0,
    turbo_win_rate DECIMAL(5,1) DEFAULT 0,
    
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE (hero_id, snap_date)
);

COMMENT ON TABLE hero_daily_stats IS '全英雄每日胜率快照（来自 OpenDota）';
COMMENT ON COLUMN hero_daily_stats.hero_id IS '英雄ID';
COMMENT ON COLUMN hero_daily_stats.snap_date IS '快照日期';
COMMENT ON COLUMN hero_daily_stats.pub_picks IS '普通匹配出场数';
COMMENT ON COLUMN hero_daily_stats.pub_wins IS '普通匹配胜场';
COMMENT ON COLUMN hero_daily_stats.pub_win_rate IS '普通匹配胜率';
COMMENT ON COLUMN hero_daily_stats.turbo_picks IS '加速模式出场数';
COMMENT ON COLUMN hero_daily_stats.turbo_wins IS '加速模式胜场';
COMMENT ON COLUMN hero_daily_stats.turbo_win_rate IS '加速模式胜率';

CREATE INDEX IF NOT EXISTS idx_hero_daily_stats_date ON hero_daily_stats(snap_date);
CREATE INDEX IF NOT EXISTS idx_hero_daily_stats_hero ON hero_daily_stats(hero_id);
