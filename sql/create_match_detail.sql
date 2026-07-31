-- 比赛详情表：存储 OpenDota /api/matches/{id} 全量数据
-- 需在 dota2_analyze 数据库中执行
CREATE TABLE IF NOT EXISTS match_detail (
    match_id BIGINT PRIMARY KEY,
    radiant_win BOOLEAN NOT NULL DEFAULT FALSE,
    duration INT NOT NULL DEFAULT 0,
    game_mode INT NOT NULL DEFAULT 0,
    lobby_type INT NOT NULL DEFAULT 0,
    radiant_score INT NOT NULL DEFAULT 0,
    dire_score INT NOT NULL DEFAULT 0,
    radiant_name VARCHAR(128) DEFAULT '',
    dire_name VARCHAR(128) DEFAULT '',
    first_blood_time INT DEFAULT 0,
    raw_json JSONB,
    sync_status SMALLINT NOT NULL DEFAULT 0,
    sync_error VARCHAR(500) DEFAULT '',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE match_detail IS '比赛详情（OpenDota /api/matches/{id} 全量数据）';
COMMENT ON COLUMN match_detail.match_id IS '比赛ID';
COMMENT ON COLUMN match_detail.radiant_win IS '天辉是否获胜';
COMMENT ON COLUMN match_detail.duration IS '时长（秒）';
COMMENT ON COLUMN match_detail.game_mode IS '游戏模式';
COMMENT ON COLUMN match_detail.lobby_type IS '房间类型';
COMMENT ON COLUMN match_detail.radiant_score IS '天辉击杀数';
COMMENT ON COLUMN match_detail.dire_score IS '夜魇击杀数';
COMMENT ON COLUMN match_detail.radiant_name IS '天辉队伍名称';
COMMENT ON COLUMN match_detail.dire_name IS '夜魇队伍名称';
COMMENT ON COLUMN match_detail.first_blood_time IS '一血时间（秒）';
COMMENT ON COLUMN match_detail.raw_json IS 'OpenDota 全量JSON';
COMMENT ON COLUMN match_detail.sync_status IS '同步状态: 0=待同步, 1=同步中, 2=已同步, -1=失败';
COMMENT ON COLUMN match_detail.sync_error IS '同步错误信息';
COMMENT ON COLUMN match_detail.created_time IS '创建时间';
COMMENT ON COLUMN match_detail.updated_time IS '更新时间';

CREATE INDEX IF NOT EXISTS idx_match_detail_sync_status ON match_detail(sync_status);
