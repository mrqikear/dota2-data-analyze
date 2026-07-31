# Dota 2 Data Analyze System

Dota 2 比赛数据分析平台。追踪多个 Steam 账号的比赛记录，提供伤害分析、MVP 统计、英雄表现等数据可视化功能。

## 技术栈

| 层 | 技术 | 版本 |
|:--|:-----|:-----|
| 前端 | Vue 3 + Vite 8 | Node 22 |
| 后端 | Spring Boot 2.7.8 | Java 11 |
| 数据库 | PostgreSQL 16 | Docker |
| 数据源 | OpenDota API + Steam Web API | - |

## 项目结构

```
dota2-data-analyze/
├── docker/
│   ├── docker-compose.yml   # PostgreSQL + 自动建表
│   └── init.sql              # 数据库初始化 SQL
├── dota2-api/                # Spring Boot 后端 (API 服务)
│   ├── pom.xml
│   └── src/main/java/com/dota2/
│       ├── api/
│       │   ├── controller/   # REST 控制器
│       │   ├── service/      # 业务逻辑层
│       │   ├── config/       # Spring 配置 + 拦截器
│       │   └── filter/       # 认证过滤器
│       └── entity/
│           ├── dao/          # MyBatis Mapper
│           ├── entity/       # 数据实体
│           ├── form/         # 请求表单
│           ├── service/      # MyBatis-Plus Service
│           └── vo/           # 视图对象
├── dota2-common/             # 公共工具模块
├── dota2-entity/             # 数据实体模块
├── dota2-frontend/           # Vue 3 前端
│   ├── src/
│   │   ├── api/              # Axios API 封装
│   │   ├── views/            # 页面组件
│   │   ├── router/           # 路由
│   │   ├── utils/            # 工具函数
│   │   └── components/       # 公共组件
│   ├── vite.config.js        # Vite + 代理配置
│   └── package.json
├── sql/                      # 数据库迁移脚本
└── docker-compose.yml        # 根目录编排文件
```

## 数据库

### PostgreSQL (Docker)
- 端口: 5432
- 数据库: dota2_analyze
- 用户: dota2
- 密码: dota2_analyze_pwd

### 核心表

| 表名 | 用途 | 关键字段 |
|:-----|:-----|:---------|
| `steam_account` | 追踪的 Steam 账号 | steam_id, nick_name |
| `match_main` | 比赛主表 | match_id, start_time, game_mode, mvp_steam_id, fmvp_steam_id |
| `match_player` | 玩家比赛数据 | match_id, steam_id, hero_id, kills, deaths, hero_damage |
| `match_detail` | 比赛详细原始 JSON | match_id, raw_json(OpenDota 全量数据), sync_status |
| `sys_user` | 系统用户 | user_name, pass_word(RSA 加密) |
| `asset_cache` | 图片缓存 | asset_type(hero/item/ability), base64_data |
| `game_constants` | OpenDota 常量 | const_type(abilities/heroes/items), data_json |
| `hero_daily_stats` | 英雄每日胜率 | hero_id, win, pick, date |

### 初始化
```bash
docker compose -f docker/docker-compose.yml up -d
```
自动执行 `init.sql` 创建所有表。如果已有数据，可用 `sql/` 目录下的迁移脚本。

## 后端 API

### 启动方式
```bash
cd dota2-api
mvn package -DskipTests
java -jar target/dota2-api-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

### 核心 API

| 端点 | 方法 | 用途 |
|:-----|:-----|:------|
| `/match/page` | POST | 分页查询比赛记录 (支持 steamId/gameMode/heroId/parsed 筛选) |
| `/match/detail/{matchId}` | GET | 比赛详情 (包含玩家数据+MVP 计算) |
| `/match/detail/fetch/{matchId}` | POST | 从 OpenDota 拉取比赛数据 |
| `/match/detail/request/{matchId}` | POST | 请求 OpenDota 解析 replay |
| `/match/detail/damage/{matchId}` | GET | 伤害来源分析 (damage_inflictor 明细) |
| `/match/sync/{steamId}` | GET | 同步某个账号的所有比赛 |
| `/match/syncTurbo/{steamId}` | GET | 同步加速模式比赛 (通过 Steam API) |
| `/match/relatedMatches` | POST | 查询关联比赛 (队友/对手/ solo) |
| `/match/playerStats/{steamId}` | GET | 用户统计汇总 (胜率/MVP/英雄统计) |
| `/match/backfillMvp` | POST | 回填所有比赛的 MVP/FMVP |
| `/constants/sync` | POST | 同步 OpenDota 常量 (abilities/heroes/items) |
| `/constants/{type}` | GET | 获取缓存的常量 |
| `/user/login` | POST | 用户登录 |

### 数据流程
```
Steam API / OpenDota API
       ↓
 MatchSyncService (定时 6h 同步)
 SteamTurboSyncService (定时 1h 同步)
       ↓
    match_main + match_player 表
       ↓
 MatchDetailSyncService (定时 10s 轮询缺失数据)
       ↓
    match_detail 表 (rawJson)
       ↓
    前端展示
```

### 关键配置
- 代理: `app.proxy.host=127.0.0.1`, `app.proxy.port=7897` (访问 Steam/OpenDota 时需要)
- 认证: `/user/login` 返回 token, 其他 API 需 `ssoToken` header
- 免认证路径: `/user/login`, `/match/**`, `/constants/**`, `/asset/**`

## 前端

### 启动方式
```bash
cd dota2-frontend
yarn install   # 或 npm install
yarn dev --port 5200   # Vite 开发服务器 + 代理
```

Vite 配置了代理: `/api` → `http://localhost:9601` (自动去掉 `/api` 前缀)

### 页面路由

| 路由 | 功能 |
|:-----|:------|
| `/login` | 登录页 |
| `/dashboard` | 仪表盘 |
| `/match` | 比赛列表 (筛选/分页/伤害弹窗) |
| `/match/detail/:id` | 比赛详情 (玩家表/伤害源/装备) |
| `/analysis` | 英雄数据分析 |
| `/steamAccount` | Steam 账号管理 |
| `/user` | 用户管理 |

### 比赛列表功能
- 模式筛选 (天梯/加速/普通)
- 解析状态筛选 (已解析/未解析)
- Steam 账号筛选
- 英雄头像悬停 → 伤害弹窗 (含物理/魔法/纯粹分类)
- MVP/FMVP 标签
- 分页 + 排序

### 比赛详情功能
- 比赛信息 (模式/时长/击杀)
- 玩家表 (KDA/伤害/承伤/装备/技能加点)
- 伤害源弹窗 (按来源展示, 含分类统计)
- 选人/Ban 列表
- 原始 JSON 查看

## 部署步骤 (完整)

### 1. 数据库
```bash
docker compose -f docker/docker-compose.yml up -d
```

### 2. 后端
```bash
cd dota2-api
mvn package -DskipTests
java -jar target/dota2-api-1.0-SNAPSHOT.jar --spring.profiles.active=dev
# 后端运行在 http://localhost:9601
```

### 3. 前端
```bash
cd dota2-frontend
yarn install
yarn dev --port 5200
# 前端运行在 http://localhost:5200
```

### 4. 初始化数据
```bash
# 1. 同步游戏常量 (技能/英雄/装备数据)
curl -X POST http://localhost:9601/constants/sync

# 2. 登录获取 token
curl -X POST http://localhost:9601/user/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 3. 添加 Steam 账号 (通过前端页面)
# 4. 同步比赛
curl http://localhost:9601/match/sync/{steamId}

# 5. 同步加速模式
curl http://localhost:9601/match/syncTurbo/{steamId}

# 6. 回填 MVP (已有数据时)
curl -X POST http://localhost:9601/match/backfillMvp
```

## 关于 hero_damage 的说明

游戏中 `hero_damage` 不包含辉耀灼烧、竭心光环等持续伤害。伤害源弹窗展示的是 `damage_inflictor` (combat log 全量数据)，两者口径不同，都是正确的。

## 数据流架构

```
[Steam API] ──→ MatchSyncService ←── [OpenDota API]
                    │                        │
                    ▼                        ▼
            match_main 表              match_detail 表
            match_player 表            (rawJson 原始数据)
                    │                        │
                    ▼                        ▼
              前端页面                   前端解析展示
                                      (damage_inflictor 等)
```
