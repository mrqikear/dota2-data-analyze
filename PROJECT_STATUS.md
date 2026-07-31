# Dota2 数据分析系统 - 项目状态文档

> 本文档面向 AI 助手，用于快速理解项目结构、当前进度和技术债务

---

## 1. 项目基本信息

| 属性 | 值 |
|------|-----|
| **项目名称** | dota2-data-analyze |
| **项目路径** | `D:\dota2-data-analyze` |
| **运行端口** | 9601 |
| **应用状态** | ✅ 运行中 (PID 需检查) |
| **代理配置** | 127.0.0.1:7897 (Clash) |

### 技术栈
```
后端: Spring Boot 2.7.8 + MyBatis-Plus 3.5.3.1 + PostgreSQL 15
前端: Vue3 + Element Plus + Vite
JDK: 11
Node: 22 (通过 QClaw 的 yarn22 脚本)
```

---

## 2. 模块结构

```
dota2-data-analyze/
├── dota2-common/          # 公共模块: BaseEntity, Result, JWT工具, 异常处理
├── dota2-entity/          # 实体模块: Entity, DAO, Service, Form, VO
├── dota2-api/             # API模块: Controller, Config, 启动类, 定时任务Service
├── dota2-frontend/        # 前端: Vue3 + Element Plus
├── docker/                # Docker Compose + 初始化SQL
└── sql/                   # 数据库迁移脚本
```

---

## 3. 数据库表结构

### 核心表

| 表名 | 用途 | 记录数(参考) |
|------|------|-------------|
| `sys_user` | 系统用户 | 1+ |
| `steam_account` | 追踪的Steam账号 | 5个 |
| `match_main` | 比赛主表 | ~5848场 |
| `match_player` | 比赛玩家详情 | ~5630条 |
| `match_detail` | 比赛详细数据(OpenDota JSON) | ~5250条 |

### 追踪的Steam账号
1. I win - 76561198161333880
2. Invoker - 76561198104827480
3. done！ - 76561198098352293
4. talker - 76561198132012912
5. oldMouse - 76561198091614640

---

## 4. 已完成功能

### 后端
- [x] JWT 登录认证
- [x] 系统用户 CRUD
- [x] Steam 账号管理
- [x] **Turbo模式同步** (`SteamTurboSyncService`)
  - Steam GetMatchHistory (steamchina) 发现比赛
  - OpenDota `/api/matches/{id}` 获取详情
  - 404时自动调用 `/api/request` 请求解析
  - **每小时自动全量同步** (`@Scheduled(fixedRate=3600000)`)
  - **OpenDota 429限流退避** (指数退避: 1h→2h→4h→8h→16h→24h)
- [x] **比赛详情同步** (`MatchDetailSyncService`)
  - 批量获取 OpenDota 详情
  - 解析 players 数组写入 match_player
  - 429限流处理
- [x] **HeroStats 模块** - 英雄使用概览统计
- [x] **比赛详情页** - 技能加点、物品、玩家信息
- [x] **Long转String序列化** - 解决前端JS精度丢失问题

### 前端
- [x] 登录页面
- [x] 用户管理
- [x] Steam账号管理
- [x] **比赛列表** - 分页、筛选、中文模式标签
- [x] **比赛详情** - 技能面板、物品、玩家昵称显示
- [x] **英雄概览** - 胜率统计、跳转比赛列表
- [x] 自定义每页条数
- [x] SPA路由配置

---

## 5. 当前数据状态 (2026-06-02)

```
总比赛:        5848 场
已同步详情:    5250 场
待处理:        4 场
失败(429):     594 场

Turbo比赛:     977 场 (来自Steam同步)
match_player:  5630 条
```

### 已知数据问题
- I win 和 talker 的最新比赛停在 2026-05-30 (OpenDota限流导致)
- 526场骨架比赛无match_player记录 (等待429退避后重试)

---

## 6. 关键配置

### application-dev.yml
```yaml
spring.datasource.url: jdbc:postgresql://localhost:5432/dota2_analyze
steam.api-key: B5BCA6DCA3EF402141658B5261315F93
app.proxy.host: 127.0.0.1
app.proxy.port: 7897
```

### 前端构建
```bash
# 使用 QClaw 的 Node 22
cd dota2-frontend
yarn22 install
yarn22 build

# 产物复制到后端 static 目录
dist/ → dota2-api/src/main/resources/static/
```

---

## 7. 技术债务与注意事项

### 高优先级
1. **OpenDota 限流问题**
   - 匿名 API 限制约 1500次/天
   - 已实施指数退避，但可能导致同步滞后
   - 考虑：购买 OpenDota API Key 或优化同步频率

2. **Steam API 可靠性**
   - `api.steamchina.com` 偶尔 500 错误
   - `api.steampowered.com` 被墙
   - 当前方案：china域名 + OpenDota兜底

### 中优先级
3. **SPA路由方案**
   - 当前 `/match/**` 排除所有路径，可能暴露API端点
   - 未来计划：API迁移至 `/api/` 前缀

4. **数据库连接配置**
   - YAML配置 `localhost:5432` 实际未运行
   - 应用通过 Docker 内部网络连接正常
   - 配置冗余需清理

### 低优先级
5. **日志机制**
   - Start-Process参数冲突导致 `app.log` 未生成
   - 需改用 `cmd /c start /B java -jar ... > app.log 2>&1`

6. **临时脚本清理**
   - 13个Python调试脚本可删除

---

## 8. 核心服务类说明

### SteamTurboSyncService
- **职责**: Turbo模式比赛发现和详情获取
- **调度**: 每小时执行 `scheduledSyncAll()`
- **并发控制**: `AtomicBoolean` 防止重叠执行
- **限流退避**: `dailyLimitBackoffUntil` + `backoffRetryCount`
- **关键方法**:
  - `fetchFromOpenDota(matchId)` - 带429退避的详情获取
  - `requestParse(matchId)` - 调用 `/api/request` 请求解析
  - `parseRetryAfter(headers)` - 解析Retry-After头

### MatchDetailSyncService
- **职责**: 批量同步比赛详情和玩家数据
- **调度**: 定时扫描 `sync_status=0` 的记录
- **关键逻辑**:
  - 获取 OpenDota JSON
  - 解析 `players` 数组写入 `match_player`
  - 处理 404 (未解析) → 调用 `/api/request`
  - 处理 429 → 指数退避

---

## 9. 常用命令

```bash
# 编译启动
cd D:\dota2-data-analyze
mvn clean install -DskipTests
mvn spring-boot:run -pl dota2-api

# 前端构建
cd dota2-frontend
yarn22 build
# 手动复制 dist 到 dota2-api/src/main/resources/static/

# 数据库
docker-compose -f docker/docker-compose.yml up -d

# 检查应用
Get-NetTCPConnection -LocalPort 9601
```

---

## 10. 扩展计划

- [x] **装备分析模块（全部完成）**
  - 4.1 出门装组合 — `purchase_log time≤0`，排除消耗品，按组合聚合
  - 4.2 大件出装路线 — `purchase_log` 按时间排序取前N件，可展开时间轴
  - 4.3 单件装备分析 — `item_0~5` 使用率 + 胜率，支持英雄筛选
  - 4.4 装备胜率贡献 — 购买 vs 未购买的胜率差值，正负色标
  - 4.5 个人 vs 全球对比 — 本地数据 + OpenDota `/api/heroes/{id}/itemPopularity` 并排
  - 4.6 流派自动识别 — 物理/法系/防御/辅助/切入 5 类流派分类统计
- [ ] 技能加点趋势分析
- [ ] 时间节奏分析（分时段胜率）
- [ ] 多账号对比仪表盘
- [ ] 组队配合分析
- [ ] 报告导出（PDF/Excel）
- [ ] 数据可视化图表

---

## 11. 相关文件路径

| 文件 | 路径 |
|------|------|
| 项目根目录 | `D:\dota2-data-analyze` |
| 后端启动类 | `dota2-api/src/main/java/com/dota2/api/Dota2Application.java` |
| Turbo同步服务 | `dota2-api/.../service/SteamTurboSyncService.java` |
| 详情同步服务 | `dota2-api/.../service/MatchDetailSyncService.java` |
| 前端入口 | `dota2-frontend/src/main.js` |
| 比赛列表页 | `dota2-frontend/src/views/match/index.vue` |
| 比赛详情页 | `dota2-frontend/src/views/matchDetail/index.vue` |
| 出装分析页 | `dota2-frontend/src/views/analysis/itemAnalysis/index.vue` |
| 出装分析控制器 | `dota2-api/.../controller/ItemAnalysisController.java` |
| 出装分析服务 | `dota2-api/.../service/ItemAnalysisService.java` |
| 出装分析DAO | `dota2-entity/.../dao/ItemAnalysisDao.java` |
| 出装分析Mapper | `dota2-api/.../mapper/ItemAnalysisMapper.xml` |
| 物品展示工具 | `dota2-frontend/src/utils/itemDisplay.js` |
| 英雄概览页 | `dota2-frontend/src/views/analysis/heroOverview/index.vue` |
| 开发手册 | `DEV-GUIDE.md` |

---

*文档生成时间: 2026-06-03*
*最后更新: 2026-06-02 (OpenDota限流优化完成)*
