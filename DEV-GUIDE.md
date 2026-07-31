# Dota2 Data Analyze - 开发手册

> Dota2 个人数据分析系统 —— 基于 Spring Boot + MyBatis-Plus + PostgreSQL

---

## 目录

1. [项目概览](#1-项目概览)
2. [快速开始](#2-快速开始)
3. [项目架构](#3-项目架构)
4. [模块说明](#4-模块说明)
5. [数据库设计](#5-数据库设计)
6. [API 接口文档](#6-api-接口文档)
7. [开发指南](#7-开发指南)
8. [部署指南](#8-部署指南)
9. [常见问题](#9-常见问题)

---

## 1. 项目概览

### 1.1 项目信息

| 项目 | 内容 |
|------|------|
| 项目名 | dota2-data-analyze |
| 基础路径 | `D:\dota2-data-analyze` |
| 端口 | 9601 |
| 框架 | Spring Boot 2.7.8 + MyBatis-Plus 3.4.3.4 |
| 数据库 | PostgreSQL 15 |
| JDK | 11 |
| IDE 推荐 | IntelliJ IDEA |

### 1.2 技术栈

```
后端框架    Spring Boot 2.7.8
ORM        MyBatis-Plus 3.4.3.4
分页        PageHelper 1.4.6
数据库      PostgreSQL 15
认证        JWT (auth0/java-jwt 3.19.2)
API文档     Swagger 2.9.2
工具集      Hutool 5.8.9 + Lombok + Commons
容器化      Docker Compose
```

### 1.3 目标功能（规划）

- [x] 系统用户管理（登录、CRUD）
- [x] Steam 账号管理
- [ ] OpenDota 数据拉取（OpenDota API）
- [ ] 英雄胜率统计
- [ ] 出装分析
- [ ] 技能加点分析
- [ ] 时间节奏分析
- [ ] 多账号对比
- [ ] 组队分析
- [ ] 报告导出

---

## 2. 快速开始

### 2.1 前置条件

- JDK 11+
- Maven 3.6+
- Docker + Docker Compose（或直接连已有 PostgreSQL）
- IntelliJ IDEA（推荐）

### 2.2 启动数据库

```bash
cd D:\dota2-data-analyze\docker
docker-compose up -d
```

首次启动会自动执行 `init.sql` 建表并插入默认管理员。

### 2.3 编译启动

```bash
# 编译全部模块
cd D:\dota2-data-analyze
mvn clean install -DskipTests

# 启动 API
mvn spring-boot:run -pl dota2-api
```

### 2.4 访问验证

| 地址 | 说明 |
|------|------|
| `http://localhost:9601` | API 根路径 |
| `http://localhost:9601/swagger-ui.html` | Swagger API 文档 |
| `http://localhost:9602` | PostgreSQL (Docker) |

### 2.5 默认账号

- 用户名: `admin`
- 密码: `admin123`
- 登录接口: `POST /user/login`

---

## 3. 项目架构

### 3.1 模块依赖

```
dota2-data-analyze (父POM)
 ├── dota2-common       (公共: BaseEntity, Result, JWT, SYSLOG)
 ├── dota2-entity       (实体: Entity, DAO, Service, Form, VO)
 └── dota2-api          (API: Controller, Config, Filter, 启动类)
```

### 3.2 分层架构

```
Controller 层  ← 接收请求、参数校验、返回 Result
  ↓
Service 层    ← 业务逻辑
  ↓
DAO 层        ← 数据库操作 (MyBatis-Plus BaseMapper)
  ↓
Entity 层     ← 数据库表映射
```

### 3.3 请求流程

```
客户端 → AuthInterceptor(JWT校验) → Controller → Service → DAO → PostgreSQL
         ↑ 白名单: /user/login, /swagger-ui.html...
```

所有 API（除登录和 Swagger 外）都需要在 Header 中携带 `ssoToken: <JWT_TOKEN>`。

---

## 4. 模块说明

### 4.1 dota2-common（公共模块）

#### 4.1.1 基础类

| 类 | 路径 | 说明 |
|----|------|------|
| `BaseEntity` | `com.dota2.common.base` | 实体基类：id, createdTime, createdBy, updatedTime, updatedBy, deleted |
| `CoreService<T>` | `com.dota2.common.base` | 通用 Service 接口（继承 IService） |
| `CoreServiceImpl<M,T>` | `com.dota2.common.base` | 通用实现：findByKv, selectOne, convertToQueryWrapper |
| `Result<T>` | `com.dota2.common.utils` | 统一响应：code(000000=成功), message, data |

#### 4.1.2 工具类

| 类 | 说明 |
|----|------|
| `JwtUtils` | JWT 签发验证（HMAC256, 24h过期, secret=dota2-analyze-secret-key-2024） |
| `OperationLogHelper` | ThreadLocal 存储操作日志详情 |
| `MyMetaObjectHandler` | MyBatis-Plus 自动填充 createdTime/updatedTime |

#### 4.1.3 注解

| 注解 | 说明 |
|----|------|
| `@SYSLOG` | 标记需要记录操作日志的方法，配合 OperationLogHelper.setDetail() 使用 |

### 4.2 dota2-entity（领域实体模块）

#### 4.2.1 数据层

| 类 | 说明 |
|----|------|
| `SysUserDao` | 系统用户 Mapper |
| `SteamAccountDao` | Steam 账号 Mapper |

#### 4.2.2 服务层

| 类 | 说明 |
|----|------|
| `SysUserService` / `SysUserServiceImpl` | 用户服务（login / getCurrentUser / findByUserName） |
| `SteamAccountService` / `SteamAccountServiceImpl` | Steam 账号服务（CRUD） |

**扩展约定：** 新建业务模块时在 `entity/` 下 4 个子包各自加文件：
- `entity/` → Entity 表映射
- `dao/` → DAO 接口
- `service/ + service/impl/` → Service + 实现
- `form/` → 请求入参
- `vo/` → 响应出参

### 4.3 dota2-api（API 模块）

#### 4.3.1 启动类

`Dota2Application` 配置了：

```java
@MapperScan("com.dota2.**.dao")
@ComponentScan("com.dota2")
@EnableTransactionManagement
@EnableAsync + @EnableScheduling  // 预置异步+定时任务
```

#### 4.3.2 配置类

| 类 | 说明 |
|----|------|
| `WebMvcConfig` | CORS 跨域 + 拦截器注册（排除 /user/login 和 Swagger 路径） |
| `SwaggerConfig` | Swagger 2 文档，扫描 `com.dota2.api.controller` |
| `GlobalExceptionHandler` | Dota2Exception / 参数校验 / 通用异常统一返回 |

#### 4.3.3 过滤器

| 类 | 说明 |
|----|------|
| `AuthInterceptor` | 从 Header `ssoToken` 读取 JWT 并校验，失败返回 401 |

---

## 5. 数据库设计

### 5.1 ER 图

```
┌─────────────────────┐     ┌────────────────────────────┐
│      sys_user       │     │      steam_account         │
├─────────────────────┤     ├────────────────────────────┤
│ id          BIGSERIAL│     │ id               BIGSERIAL │
│ user_name   VARCHAR  │     │ steam_id         VARCHAR  │ ← Steam 64位数字ID
│ pass_word   VARCHAR  │     │ nick_name        VARCHAR  │
│ nick_name   VARCHAR  │     │ avatar           VARCHAR  │
│ email       VARCHAR  │     │ profile_url      VARCHAR  │
│ phone       VARCHAR  │     │ last_fetch_time  TIMESTAMP│
│ status      SMALLINT │     │ status           SMALLINT │
│ created_*   ...      │     │ created_* / updated_* ...│
│ deleted     SMALLINT │     │ deleted          SMALLINT │
└─────────────────────┘     └────────────────────────────┘
```

### 5.2 sys_user 表

| 字段 | 类型 | 默认 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | 自增 | 主键 |
| user_name | VARCHAR(64) | | 用户名（唯一） |
| pass_word | VARCHAR(128) | | MD5 加密密码 |
| nick_name | VARCHAR(64) | NULL | 昵称 |
| email | VARCHAR(128) | NULL | 邮箱 |
| phone | VARCHAR(32) | NULL | 手机号 |
| status | SMALLINT | 0 | 0=启用 1=禁用 |
| created_time | TIMESTAMP | now() | 创建时间 |
| created_by | BIGINT | 0 | 创建人 |
| updated_time | TIMESTAMP | now() | 更新时间 |
| updated_by | BIGINT | 0 | 更新人 |
| deleted | SMALLINT | 0 | 逻辑删除 0=未删 1=已删 |

索引：
- `idx_sys_user_user_name` (user_name)
- `idx_sys_user_status` (status)

### 5.3 steam_account 表

| 字段 | 类型 | 默认 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | 自增 | 主键 |
| steam_id | VARCHAR(64) | | Steam 64位数字ID（唯一） |
| nick_name | VARCHAR(128) | | 昵称 |
| avatar | VARCHAR(512) | NULL | 头像 URL |
| profile_url | VARCHAR(512) | NULL | 个人资料页链接 |
| last_fetch_time | TIMESTAMP | NULL | 最后拉取比赛时间 |
| status | SMALLINT | 0 | 0=正常 1=禁用 |
| created_* / updated_* / deleted | ... | ... | 通用字段 |

索引：
- `idx_steam_account_steam_id` (steam_id)
- `idx_steam_account_status` (status)

### 5.4 数据库连接参数

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/dota2_analyze
    username: dota2
    password: dota2_123456
    driver-class-name: org.postgresql.Driver
```

### 5.5 建表 SQL

见 `docker/init.sql`。Docker 首次启动自动执行，也可手动执行：

```bash
docker exec -i dota2-analyze-db psql -U dota2 -d dota2_analyze < docker/init.sql
```

---

## 6. API 接口文档

### 6.1 系统用户 - UserController

#### POST /user/login

登录获取 JWT Token。

**Request Body:**

```json
{
  "userName": "admin",
  "password": "admin123"
}
```

**Response:**

```json
{
  "code": "000000",
  "message": "Success",
  "data": {
    "id": 1,
    "userName": "admin",
    "nickName": "管理员",
    "email": null,
    "phone": null,
    "status": 0,
    "createdTime": "2026-06-01T00:00:00",
    "token": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

#### GET /user/current

获取当前登录用户信息。**需 `ssoToken` Header。**

#### POST /user/page

分页查询用户列表。

**Request Body:**

```json
{
  "page": 1,
  "size": 10,
  "userName": "admin",
  "status": null
}
```

#### POST /user/addUser

新增用户。**需要认证。**

**Request Body:**

```json
{
  "userName": "test",
  "password": "test123",
  "nickName": "测试用户",
  "email": "",
  "phone": ""
}
```

#### POST /user/editUser

编辑用户信息。

```json
{
  "id": 1,
  "nickName": "新昵称",
  "email": "new@email.com",
  "phone": "",
  "status": 0
}
```

#### POST /user/deleteUser

批量删除用户（逻辑删除）。

```json
{
  "ids": [1, 2]
}
```

### 6.2 Steam账号 - SteamAccountController

#### POST /steamAccount/page

分页查询 Steam 账号列表。

#### GET /steamAccount/listAll

获取全部 Steam 账号（下拉列表用）。

#### POST /steamAccount/addAccount

新增 Steam 账号。

```json
{
  "steamId": "76561198888888888",
  "nickName": "我的账号",
  "avatar": "https://avatars.steamstatic.com/...",
  "profileUrl": "https://steamcommunity.com/id/..."
}
```

#### POST /steamAccount/editAccount

编辑 Steam 账号。

```json
{
  "id": 1,
  "nickName": "新昵称",
  "avatar": "https://...",
  "profileUrl": "https://...",
  "status": 0
}
```

#### POST /steamAccount/deleteAccount

批量删除 Steam 账号。

```json
{
  "ids": [1, 2, 3]
}
```

### 6.3 通用响应格式

```json
{
  "code": "000000",     // 000000=成功, 999999=失败
  "message": "Success",
  "data": {}            // 具体数据
}
```

### 6.4 错误码说明

| code | 说明 |
|------|------|
| 000000 | 成功 |
| 999999 | 通用错误（具体见 message） |

### 6.5 认证方式

所有接口（除 `/user/login` 和 Swagger 路径外）需要在请求头中携带：

```
ssoToken: eyJhbGciOiJIUzI1NiIs...
```

---

## 7. 开发指南

### 7.1 项目导入 IDEA

1. File → Open → 选择 `D:\dota2-data-analyze`
2. 等待 Maven 索引加载完成
3. 确保 JDK 11+ 已配置 (File → Project Structure → Project SDK)
4. 找到 `Dota2Application.java` 右键 Run

### 7.2 环境配置

**IDEA 终端 PATH 刷新（如 codewhale 命令找不到）：**

```powershell
$env:Path = [Environment]::GetEnvironmentVariable("Path", "Machine") + ";" + [Environment]::GetEnvironmentVariable("Path", "User")
```

### 7.3 新增业务模块

1. 新建 Entity（继承 BaseEntity）
2. 新建 DAO 接口（继承 BaseMapper）
3. 新建 Service 接口（继承 CoreService）
4. 新建 ServiceImpl（继承 CoreServiceImpl）
5. 新建 Form（请求参数校验）
6. 新建 VO（响应出参）
7. 新建 Controller
8. 运行 `mvn clean install` 编译

### 7.4 开发规范

**命名规范：**

- Controller: `XxxController` | 路径: `@RequestMapping("/xxx")`
- Service: `XxxService` (接口) / `XxxServiceImpl` (实现)
- Entity: `XxxEntity` | 表名: `xxx`
- Form: `XxxForm`
- VO: `XxxVo`

**操作日志：**

```java
@SYSLOG
@PostMapping("/someOperation")
public Result<Void> someOperation(@RequestBody XxxForm form) {
    OperationLogHelper.setDetail(form.getName()); // 记录操作详情
    // 业务逻辑...
}
```

**异常处理：**

```java
throw new Dota2Exception("错误描述");          // 业务异常 → 前端显示 message
throw new RuntimeException("系统错误");         // 系统异常 → GlobalExceptionHandler 兜底
```

**分页查询：**

```java
@PostMapping("/page")
public Result<PageInfo<Entity>> page(@RequestBody @Valid XxxPageForm form) {
    PageHelper.startPage(form.getPage(), form.getSize());
    List<Entity> list = service.list();  // 或带条件
    return Result.ok(new PageInfo<>(list));
}
```

### 7.5 MyBatis-Plus 常用操作

```java
// 基础 CRUD（继承 CoreService 后自带）
service.getById(id);                     // 按 ID 查询
service.list();                          // 查询全部 (已过滤 deleted=0)
service.listByIds(ids);                  // 批量查询
service.save(entity);                    // 新增
service.updateById(entity);              // 按 ID 更新
service.removeById(id);                  // 逻辑删除
service.removeByIds(ids);                // 批量逻辑删除

// 条件查询
service.getOne(Wrappers.<XxxEntity>lambdaQuery()
    .eq(XxxEntity::getField, value));

service.list(Wrappers.<XxxEntity>lambdaQuery()
    .like(XxxEntity::getName, keyword)
    .eq(XxxEntity::getStatus, 0)
    .orderByDesc(XxxEntity::getCreatedTime));

// 分页
PageHelper.startPage(pageNum, pageSize);
List<XxxEntity> list = service.list(condition);
PageInfo<XxxEntity> pageInfo = new PageInfo<>(list);
```

### 7.6 Entity 字段策略

| MyBatis-Plus 特性 | 配置值 |
|------------------|--------|
| ID 生成策略 | 自增 (id-type: 0) |
| 字段策略 | 非空判断 (field-strategy: 1) |
| 下划线转驼峰 | 开启 |
| 逻辑删除 | 0=未删, 1=已删 |
| 自动填充 | createdTime / updatedTime (MyMetaObjectHandler) |

### 7.7 Maven 编译注意

```bash
# 全量编译
mvn clean install -DskipTests

# 仅编译 API 模块（依赖会自动编译）
mvn clean install -pl dota2-api -am -DskipTests

# 启动 API
mvn spring-boot:run -pl dota2-api
```

---

## 8. 部署指南

### 8.1 Docker 部署

```bash
# 构建镜像
cd D:\dota2-data-analyze
mvn clean package -DskipTests

# 推送镜像或直接运行
java -jar dota2-api/target/dota2-api-1.0-SNAPSHOT.jar --spring.profiles.active=dev
```

### 8.2 生产环境配置

创建 `application-prod.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://prod-db:5432/dota2_analyze
    username: dota2
    password: ${DB_PASSWORD}
```

启动指定 profile：

```bash
java -jar dota2-api.jar --spring.profiles.active=prod
```

### 8.3 Nginx 反向代理（可选）

```nginx
server {
    listen 80;
    server_name api.dota2-analyze.com;

    location / {
        proxy_pass http://127.0.0.1:9601;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

---

## 9. 常见问题

### Q1: 启动报数据库连接失败？

确保 Docker 中 PostgreSQL 已启动：

```bash
docker ps | findstr dota2-analyze-db
```

如未启动：

```bash
cd D:\dota2-data-analyze\docker
docker-compose up -d
```

### Q2: 登录返回 999999 用户名或密码错误？

- 默认密码 admin123，MD5 加密后为 `0192023a7bbd73250516f069df18b500`
- 确认 `init.sql` 已正确执行，admin 用户已插入
- 确认数据库密码与 `application-dev.yml` 一致

### Q3: Swagger 页面打不开？

直接访问 `http://localhost:9601/swagger-ui.html`，确认 API 已启动。

### Q4: 接口返回 401？

请求头缺少 `ssoToken`，或 Token 已过期（默认 24 小时）。

### Q5: Maven 编译报 lombok 相关错误？

检查 IDEA 中是否已安装 Lombok 插件，且 Annotation Processing 已启用。

---

## 附录

### A. 文件清单

```
D:\dota2-data-analyze\
├── pom.xml                                      # 父 POM
├── docker\
│   ├── docker-compose.yml                      # Docker Compose
│   └── init.sql                                # 数据库初始化
├── dota2-common\                               # 公共模块
│   ├── pom.xml
│   └── src\main\java\com\dota2\common\
│       ├── annotation\SYSLOG.java
│       ├── base\BaseEntity.java
│       ├── base\CoreService.java
│       ├── base\CoreServiceImpl.java
│       ├── config\MyMetaObjectHandler.java
│       ├── exception\Dota2Exception.java
│       └── utils\Result.java, JwtUtils.java, OperationLogHelper.java
├── dota2-entity\                               # 实体模块
│   ├── pom.xml
│   └── src\main\java\com\dota2\entity\
│       ├── dao\SysUserDao.java, SteamAccountDao.java
│       ├── entity\SysUserEntity.java, SteamAccountEntity.java
│       ├── form\7个 Form 文件
│       ├── service\ + service\impl\ 接口与实现
│       └── vo\UserVo.java, SteamAccountVo.java
└── dota2-api\                                  # API 模块
    ├── pom.xml
    └── src\main\
        ├── java\com\dota2\api\
        │   ├── Dota2Application.java
        │   ├── config\WebMvcConfig.java, SwaggerConfig.java, GlobalExceptionHandler.java
        │   ├── controller\UserController.java, SteamAccountController.java
        │   └── filter\AuthInterceptor.java
        └── resources\application.yml, application-dev.yml
```

### B. 扩展计划

- [ ] 英雄数据表 (`hero`)
- [ ] 比赛数据表 (`match`, `match_player`)
- [ ] OpenDota 数据拉取定时任务
- [ ] 出装分析模块
- [ ] 技能加点分析
- [ ] 时间节奏分析
- [ ] 多账号对比仪表盘
- [ ] 报告导出（CSV/PDF）
