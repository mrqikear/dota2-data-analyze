-- Dota2 Data Analyze 数据库初始化脚本
-- PostgreSQL 15

-- 系统用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(64) NOT NULL UNIQUE,
    pass_word VARCHAR(128) NOT NULL,
    nick_name VARCHAR(64),
    email VARCHAR(128),
    phone VARCHAR(32),
    status SMALLINT DEFAULT 0 NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT DEFAULT 0,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT 0,
    deleted SMALLINT DEFAULT 0
);
COMMENT ON TABLE sys_user IS '系统用户';
COMMENT ON COLUMN sys_user.user_name IS '用户名';
COMMENT ON COLUMN sys_user.pass_word IS '密码(MD5)';
COMMENT ON COLUMN sys_user.nick_name IS '昵称';
COMMENT ON COLUMN sys_user.email IS '邮箱';
COMMENT ON COLUMN sys_user.phone IS '手机号';
COMMENT ON COLUMN sys_user.status IS '状态 0=启用 1=禁用';
COMMENT ON COLUMN sys_user.deleted IS '逻辑删除 0=未删 1=已删';

CREATE INDEX idx_sys_user_user_name ON sys_user(user_name);
CREATE INDEX idx_sys_user_status ON sys_user(status);

-- Steam账号表
CREATE TABLE IF NOT EXISTS steam_account (
    id BIGSERIAL PRIMARY KEY,
    steam_id VARCHAR(64) NOT NULL UNIQUE,
    nick_name VARCHAR(128) NOT NULL,
    avatar VARCHAR(512),
    profile_url VARCHAR(512),
    last_fetch_time TIMESTAMP,
    status SMALLINT DEFAULT 0 NOT NULL,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT DEFAULT 0,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT DEFAULT 0,
    deleted SMALLINT DEFAULT 0
);
COMMENT ON TABLE steam_account IS 'Steam账号';
COMMENT ON COLUMN steam_account.steam_id IS 'Steam 64位数字ID';
COMMENT ON COLUMN steam_account.nick_name IS '昵称';
COMMENT ON COLUMN steam_account.avatar IS '头像URL';
COMMENT ON COLUMN steam_account.profile_url IS '个人资料页URL';
COMMENT ON COLUMN steam_account.last_fetch_time IS '最后拉取比赛时间';
COMMENT ON COLUMN steam_account.status IS '状态 0=正常 1=禁用';
COMMENT ON COLUMN steam_account.deleted IS '逻辑删除 0=未删 1=已删';

CREATE INDEX idx_steam_account_steam_id ON steam_account(steam_id);
CREATE INDEX idx_steam_account_status ON steam_account(status);

-- 插入默认管理员 (密码: admin123)
INSERT INTO sys_user (user_name, pass_word, nick_name, status) VALUES
('admin', '0192023a7bbd73250516f069df18b500', '管理员', 0)
ON CONFLICT (user_name) DO NOTHING;

-- match_main + match_player
CREATE TABLE IF NOT EXISTS match_main (
    match_id BIGINT PRIMARY KEY,
    start_time BIGINT DEFAULT 0,
    duration INTEGER DEFAULT 0,
    game_mode INTEGER DEFAULT 0,
    lobby_type INTEGER DEFAULT 0,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
);
COMMENT ON TABLE match_main IS '比赛主表';
COMMENT ON COLUMN match_main.match_id IS '比赛ID(OpenDota)';
COMMENT ON COLUMN match_main.start_time IS '比赛开始时间(unix)';
COMMENT ON COLUMN match_main.duration IS '比赛时长(秒)';
COMMENT ON COLUMN match_main.game_mode IS '游戏模式';
COMMENT ON COLUMN match_main.lobby_type IS '房间类型';

CREATE TABLE IF NOT EXISTS match_player (
    id BIGSERIAL PRIMARY KEY,
    match_id BIGINT NOT NULL REFERENCES match_main(match_id),
    steam_id VARCHAR(64) NOT NULL,
    hero_id INTEGER DEFAULT 0,
    kills INTEGER DEFAULT 0,
    deaths INTEGER DEFAULT 0,
    assists INTEGER DEFAULT 0,
    win BOOLEAN DEFAULT FALSE,
    gold_per_min INTEGER DEFAULT 0,
    xp_per_min INTEGER DEFAULT 0,
    last_hits INTEGER DEFAULT 0,
    denies INTEGER DEFAULT 0,
    hero_damage INTEGER DEFAULT 0,
    tower_damage INTEGER DEFAULT 0,
    hero_healing INTEGER DEFAULT 0,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (match_id, steam_id)
);
COMMENT ON TABLE match_player IS '比赛选手关联表';
COMMENT ON COLUMN match_player.steam_id IS 'Steam 64位数字ID';
COMMENT ON COLUMN match_player.hero_id IS '英雄ID';
COMMENT ON COLUMN match_player.kills IS '击杀';
COMMENT ON COLUMN match_player.deaths IS '死亡';
COMMENT ON COLUMN match_player.assists IS '助攻';
COMMENT ON COLUMN match_player.win IS '是否胜利';
COMMENT ON COLUMN match_player.gold_per_min IS '每分钟金钱';
COMMENT ON COLUMN match_player.xp_per_min IS '每分钟经验';
COMMENT ON COLUMN match_player.last_hits IS '补刀';
COMMENT ON COLUMN match_player.denies IS '反补';
COMMENT ON COLUMN match_player.hero_damage IS '英雄伤害';
COMMENT ON COLUMN match_player.tower_damage IS '建筑伤害';
COMMENT ON COLUMN match_player.hero_healing IS '治疗量';
CREATE INDEX IF NOT EXISTS idx_match_player_steam_id ON match_player(steam_id);
CREATE INDEX IF NOT EXISTS idx_match_player_hero_id ON match_player(hero_id);
CREATE INDEX IF NOT EXISTS idx_match_main_start_time ON match_main(start_time);

-- ================ 额外表（后面加的） ================

-- match_detail
CREATE TABLE IF NOT EXISTS match_detail (
    match_id BIGINT PRIMARY KEY,
    radiant_win BOOLEAN DEFAULT FALSE,
    duration INTEGER DEFAULT 0,
    game_mode INTEGER DEFAULT 0,
    lobby_type INTEGER DEFAULT 0,
    radiant_score INTEGER DEFAULT 0,
    dire_score INTEGER DEFAULT 0,
    radiant_name VARCHAR(255) DEFAULT '',
    dire_name VARCHAR(255) DEFAULT '',
    first_blood_time INTEGER DEFAULT 0,
    raw_json TEXT,
    sync_status SMALLINT DEFAULT 0,
    sync_error TEXT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE match_detail IS '比赛详细数据（OpenDota 原始 JSON）';
CREATE INDEX IF NOT EXISTS idx_match_detail_sync_status ON match_detail(sync_status);

-- asset_cache
CREATE TABLE IF NOT EXISTS asset_cache (
    id BIGSERIAL PRIMARY KEY,
    asset_type VARCHAR(32) NOT NULL,
    asset_key VARCHAR(128) NOT NULL,
    mime_type VARCHAR(64) DEFAULT 'image/png',
    base64_data TEXT,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (asset_type, asset_key)
);
COMMENT ON TABLE asset_cache IS '资源缓存（英雄/装备/技能图标 base64）';

-- game_constants
CREATE TABLE IF NOT EXISTS game_constants (
    id BIGSERIAL PRIMARY KEY,
    const_type VARCHAR(32) NOT NULL UNIQUE,
    data_json TEXT NOT NULL,
    version INTEGER DEFAULT 1,
    fetched_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE game_constants IS '游戏常量缓存（OpenDota /api/constants/*）';

-- hero_daily_stats
CREATE TABLE IF NOT EXISTS hero_daily_stats (
    id BIGSERIAL PRIMARY KEY,
    hero_id INTEGER NOT NULL,
    snap_date DATE NOT NULL DEFAULT CURRENT_DATE,
    pub_picks INTEGER DEFAULT 0,
    pub_wins INTEGER DEFAULT 0,
    pub_win_rate DECIMAL(5,1) DEFAULT 0,
    turbo_picks INTEGER DEFAULT 0,
    turbo_wins INTEGER DEFAULT 0,
    turbo_win_rate DECIMAL(5,1) DEFAULT 0,
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (hero_id, snap_date)
);
COMMENT ON TABLE hero_daily_stats IS '英雄每日胜率';
CREATE INDEX IF NOT EXISTS idx_hero_daily_stats_date ON hero_daily_stats(snap_date);
CREATE INDEX IF NOT EXISTS idx_hero_daily_stats_hero ON hero_daily_stats(hero_id);

-- MVP/FMVP 字段 (match_main 后加的)
ALTER TABLE match_main ADD COLUMN IF NOT EXISTS mvp_steam_id VARCHAR(64);
ALTER TABLE match_main ADD COLUMN IF NOT EXISTS fmvp_steam_id VARCHAR(64);
COMMENT ON COLUMN match_main.mvp_steam_id IS 'MVP Steam ID';
COMMENT ON COLUMN match_main.fmvp_steam_id IS 'FMVP Steam ID';

-- 种子数据: Steam 账号
INSERT INTO steam_account (steam_id, nick_name, avatar, profile_url, status) VALUES
('76561198089680577', 'Without M', 'https://avatars.steamstatic.com/840c3970f5b4c73c0f2038d910c4e75263de622b_full.jpg', 'https://steamcommunity.com/profiles/76561198089680577/', 0),
('76561198091614644', 'oldMouse', 'https://avatars.steamstatic.com/a6d2eaac14ffd70ff1cc89f4b94b541d1b816e89_full.jpg', 'https://steamcommunity.com/profiles/76561198091614644/', 0),
('76561198104827480', 'ye', 'https://avatars.steamstatic.com/536b5260eb677b3a0978496c43ff79b77557c115_full.jpg', 'https://steamcommunity.com/profiles/76561198104827480/', 0),
('76561198098352293', 'done!', 'https://avatars.steamstatic.com/d2568fde8e8e42e6587f25149fc3f21c3bc975bd_full.jpg', 'https://steamcommunity.com/profiles/76561198098352293/', 0),
('76561198132012912', 'talker', 'https://avatars.steamstatic.com/ab9cdd84b7e68e684cb082d7f1d64f68f6ea9328_full.jpg', 'https://steamcommunity.com/profiles/76561198132012912/', 0),
('76561198161333880', 'I win', 'https://avatars.steamstatic.com/bf146ea9853a5a56562ffd7f27afe9d347fce95e_full.jpg', 'https://steamcommunity.com/id/530608152/', 0)
ON CONFLICT (steam_id) DO NOTHING;
