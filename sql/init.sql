-- ============================================================
-- Dota2 Analyze - Database Init Script
-- Run automatically by docker-compose on first start
-- ============================================================

-- 1. Create tables (match_detail, match_main, match_player, steam_account, sys_user)

-- match_main - match main table
CREATE TABLE IF NOT EXISTS public.match_main (
    match_id bigint NOT NULL PRIMARY KEY,
    start_time bigint DEFAULT 0,
    duration integer DEFAULT 0,
    game_mode integer DEFAULT 0,
    lobby_type integer DEFAULT 0,
    mvp_steam_id character varying(64),
    fmvp_steam_id character varying(64),
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    deleted smallint DEFAULT 0
);
COMMENT ON TABLE public.match_main IS '比赛主表';

-- match_detail - match JSONB detail (lazy sync)
CREATE TABLE IF NOT EXISTS public.match_detail (
    match_id bigint NOT NULL PRIMARY KEY,
    radiant_win boolean,
    duration integer,
    game_mode integer,
    lobby_type integer,
    radiant_score integer,
    dire_score integer,
    radiant_name character varying(255),
    dire_name character varying(255),
    first_blood_time integer,
    raw_json text,
    sync_status integer DEFAULT 0,
    sync_error text,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);

-- match_player - player stats per match
CREATE TABLE IF NOT EXISTS public.match_player (
    id bigint NOT NULL PRIMARY KEY,
    match_id bigint NOT NULL,
    steam_id character varying(64) NOT NULL,
    hero_id integer DEFAULT 0,
    kills integer DEFAULT 0,
    deaths integer DEFAULT 0,
    assists integer DEFAULT 0,
    win boolean DEFAULT false,
    gold_per_min integer DEFAULT 0,
    xp_per_min integer DEFAULT 0,
    last_hits integer DEFAULT 0,
    denies integer DEFAULT 0,
    hero_damage integer DEFAULT 0,
    tower_damage integer DEFAULT 0,
    hero_healing integer DEFAULT 0,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT match_player_match_id_steam_id_key UNIQUE (match_id, steam_id),
    CONSTRAINT match_player_match_id_fkey FOREIGN KEY (match_id) REFERENCES public.match_main(match_id)
);
CREATE SEQUENCE IF NOT EXISTS public.match_player_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER TABLE public.match_player ALTER COLUMN id SET DEFAULT nextval('public.match_player_id_seq'::regclass);

-- steam_account - tracked Steam accounts
CREATE TABLE IF NOT EXISTS public.steam_account (
    id bigint NOT NULL PRIMARY KEY,
    steam_id character varying(64) NOT NULL UNIQUE,
    nick_name character varying(128) NOT NULL,
    avatar character varying(512),
    profile_url character varying(512),
    last_fetch_time timestamp without time zone,
    status smallint DEFAULT 0 NOT NULL,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint DEFAULT 0,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint DEFAULT 0,
    deleted smallint DEFAULT 0
);
CREATE SEQUENCE IF NOT EXISTS public.steam_account_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER TABLE public.steam_account ALTER COLUMN id SET DEFAULT nextval('public.steam_account_id_seq'::regclass);

-- sys_user - system users
CREATE TABLE IF NOT EXISTS public.sys_user (
    id bigint NOT NULL PRIMARY KEY,
    user_name character varying(64) NOT NULL UNIQUE,
    pass_word character varying(128) NOT NULL,
    nick_name character varying(64),
    email character varying(128),
    phone character varying(32),
    status smallint DEFAULT 0 NOT NULL,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    created_by bigint DEFAULT 0,
    updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint DEFAULT 0,
    deleted smallint DEFAULT 0
);
CREATE SEQUENCE IF NOT EXISTS public.sys_user_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER TABLE public.sys_user ALTER COLUMN id SET DEFAULT nextval('public.sys_user_id_seq'::regclass);

-- 2. Indexes
CREATE INDEX IF NOT EXISTS idx_match_main_start_time ON public.match_main (start_time);
CREATE INDEX IF NOT EXISTS idx_match_detail_sync_status ON public.match_detail (sync_status);
CREATE INDEX IF NOT EXISTS idx_match_detail_game_mode ON public.match_detail (game_mode);
CREATE INDEX IF NOT EXISTS idx_match_player_steam_id ON public.match_player (steam_id);
CREATE INDEX IF NOT EXISTS idx_match_player_hero_id ON public.match_player (hero_id);
CREATE INDEX IF NOT EXISTS idx_steam_account_steam_id ON public.steam_account (steam_id);
CREATE INDEX IF NOT EXISTS idx_steam_account_status ON public.steam_account (status);
CREATE INDEX IF NOT EXISTS idx_sys_user_user_name ON public.sys_user (user_name);
CREATE INDEX IF NOT EXISTS idx_sys_user_status ON public.sys_user (status);

-- 3. Seed data

-- Admin user (password: admin123, MD5)
INSERT INTO public.sys_user (id, user_name, pass_word, nick_name, status, created_time)
VALUES (1, 'admin', '0192023a7bbd73250516f069df18b500', '管理员', 0, NOW())
ON CONFLICT DO NOTHING;

-- Tracked Steam accounts
INSERT INTO public.steam_account (id, steam_id, nick_name, avatar, profile_url, status) VALUES
(2064964523049852930, '76561198089680577', 'Without M', 'https://avatars.steamstatic.com/840c3970f5b4c73c0f2038d910c4e75263de622b_full.jpg', 'https://steamcommunity.com/profiles/76561198089680577/', 0),
(2063815768657293313, '76561198091614644', 'oldMouse', 'https://avatars.steamstatic.com/a6d2eaac14ffd70ff1cc89f4b94b541d1b816e89_full.jpg', 'https://steamcommunity.com/profiles/76561198091614644/', 0),
(2061351237372813314, '76561198104827480', 'ye', 'https://avatars.steamstatic.com/536b5260eb677b3a0978496c43ff79b77557c115_full.jpg', 'https://steamcommunity.com/profiles/76561198104827480/', 0),
(2061352994417074177, '76561198098352293', 'done！', 'https://avatars.steamstatic.com/d2568fde8e8e42e6587f25149fc3f21c3bc975bd_full.jpg', 'https://steamcommunity.com/profiles/76561198098352293/', 0),
(2061351665011466241, '76561198132012912', 'talker', 'https://avatars.steamstatic.com/ab9cdd84b7e68e684cb082d7f1d64f68f6ea9328_full.jpg', 'https://steamcommunity.com/profiles/76561198132012912/', 0),
(2061352674450399233, '76561198161333880', 'I win', 'https://avatars.steamstatic.com/bf146ea9853a5a56562ffd7f27afe9d347fce95e_full.jpg', 'https://steamcommunity.com/id/530608152/', 0)
ON CONFLICT DO NOTHING;

-- Reset sequences
SELECT setval('public.sys_user_id_seq', (SELECT COALESCE(MAX(id), 1) FROM public.sys_user));
SELECT setval('public.steam_account_id_seq', (SELECT COALESCE(MAX(id), 1) FROM public.steam_account));
SELECT setval('public.match_player_id_seq', (SELECT COALESCE(MAX(id), 1) FROM public.match_player));
