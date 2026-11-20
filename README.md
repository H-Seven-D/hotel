# yeu 民宿酒店预订管理系统

一个面向民宿与酒店日常运营的前后端分离管理系统，适合作为 Java Web 方向的毕业设计项目。系统采用蓝白色视觉主题，支持普通员工与管理员两类身份，覆盖门店、房型、房态、预订、入住退房、客户、排班、员工与审计管理。

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 开发工具 | IntelliJ IDEA |
| 后端语言 | Java 17 |
| 后端框架 | Spring Boot 3.3.5、Spring Security、Spring AOP |
| 数据访问 | MyBatis-Plus 3.5.7 |
| 数据库 | MySQL 8 |
| 身份认证 | JWT + BCrypt |
| 接口文档 | Springdoc OpenAPI / Swagger UI |
| 前端框架 | Vue 3.5、Vite 5 |
| UI 组件 | Element Plus |
| 图表 | ECharts |
| 包管理 | Maven、npm |

## 功能范围

普通员工可以：

- 管理民宿或酒店基础资料。
- 管理房型、房间和实时房态。
- 创建和修改预订，办理入住、退房与取消。
- 对清洁中、维护中的房间进行状态调度。
- 维护客户档案。
- 查看和安排员工排班。
- 查看运营总览。

管理员在拥有普通员工权限的基础上，还可以：

- 新增、修改、停用和删除员工账号。
- 重置员工登录密码。
- 查看所有员工的登录时间、下线时间、在线状态与 IP。
- 查看所有业务操作日志，包括操作模块、操作内容、请求地址和执行结果。

## 项目结构

```text
D:\hotel
├─ backend/                         Spring Boot 后端
│  ├─ src/main/java/com/homestay/   业务代码
│  ├─ src/main/resources/
│  │  ├─ application.yml            应用配置
│  │  └─ db/
│  │     ├─ schema.sql              建库建表脚本
│  │     └─ data.sql                演示数据
│  └─ pom.xml
├─ frontend/                        Vue 3 前端
│  ├─ src/
│  └─ package.json
├─ docs/
│  ├─ 毕业设计说明.md
│  └─ 开题报告素材.md
└─ README.md
```

## 演示账号

| 角色 | 账号 | 初始密码 |
| --- | --- | --- |
| 管理员 | `admin` | `Admin@123` |
| 普通员工 | `employee` | `Staff@123` |
| 普通员工 | `zhangmin` | `Staff@123` |

首次启动时，后端会把演示数据里的初始密码自动转换为 BCrypt 哈希。正式使用前应修改演示密码。

## 数据库配置

默认配置位于 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/homestay_booking?createDatabaseIfNotExist=true&...
    username: root
    password: 123456
```

当前默认连接为：

- 地址：`127.0.0.1`
- 端口：`3306`
- 数据库：`homestay_booking`
- 用户名：`root`
- 密码：`123456`

后端启动时会自动创建数据库和数据表，并执行演示数据初始化。也可以通过环境变量覆盖：

```powershell
$env:DB_URL="jdbc:mysql://127.0.0.1:3306/homestay_booking?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的密码"
```

手动初始化数据库时，按顺序执行：

```powershell
& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p --default-character-set=utf8mb4 --execute="SOURCE D:/hotel/backend/src/main/resources/db/schema.sql; SOURCE D:/hotel/backend/src/main/resources/db/data.sql;"
```

## 启动后端

1. 在 IDEA 中打开 `D:\hotel\backend`。
2. 将 Project SDK 设置为 JDK 17。
3. 等待 Maven 下载依赖。
4. 运行 `com.homestay.HomestayApplication`。

命令行启动：

```powershell
$env:JAVA_HOME="C:\Users\MR\.jdks\ms-17.0.19"
cd D:\hotel\backend
mvn spring-boot:run
```

如果本机 Maven 默认仓库目录不可写，可额外使用：

```powershell
mvn "-Dmaven.repo.local=D:\hotel\.m2\repository" spring-boot:run
```

后端地址：`http://127.0.0.1:8080`

接口文档：`http://127.0.0.1:8080/swagger-ui.html`

健康检查：`http://127.0.0.1:8080/api/health`

## IDEA 一键启动

项目根目录的 `.run/一键启动.run.xml` 已配置共享运行方案。IDEA 完成文件同步后，在右上角运行配置下拉框中选择：

```text
一键启动
```

点击绿色运行按钮后，启动器会依次执行：

1. IDEA 使用 JDK 17 编译并启动 Spring Boot。
2. 后端完成数据库连接并监听 `8080`。
3. Spring Boot 自动选择可用的本地端口启动 Vite。
4. Vite 自动打开默认浏览器并显示登录页。

直接运行现有的 `HomestayApplication` 配置也具有相同效果，不一定必须使用“一键启动”名称。点击 IDEA 的红色停止按钮会同时结束后端和前端进程。如果 `5173` 已被其他程序占用，系统会自动选择下一个可用端口。

如果在运行配置下拉框中暂时没有看到“一键启动”，刷新项目文件或重启一次 IDEA。

## 启动前端

```powershell
cd D:\hotel\frontend
npm install
npm run dev:frontend
```

前端地址：`http://127.0.0.1:5173`

Vite 已将 `/api` 代理到 `http://127.0.0.1:8080`，本地开发无需额外配置跨域地址。

## 构建项目

后端：

```powershell
cd D:\hotel\backend
mvn "-DskipTests" package
```

可执行文件为：

```text
backend/build/homestay-booking-1.0.0-exec.jar
```

前端：

```powershell
cd D:\hotel\frontend
npm run build
```

构建产物位于 `frontend/dist`。

## 权限与审计设计

- 登录成功后生成 JWT，同时在 `login_session` 中记录本次会话。
- JWT 只保存在当前浏览器标签页的 `sessionStorage`，新打开项目时必须重新登录。
- 启动时会清理旧版本保存在 `localStorage` 中的持久化登录信息。
- JWT 中携带会话编号，服务端会校验会话是否仍为 `ONLINE`。
- 主动退出时更新下线时间并将会话改为 `OFFLINE`。
- 关键业务接口使用 `@AuditAction` 注解，由 AOP 统一记录操作模块、操作内容、请求方式、IP、时间和执行结果。
- 管理员接口在 Spring Security 中统一限制为 `ROLE_ADMIN`。
- 用户密码使用 BCrypt 加密，不在接口响应中返回密码字段。
- 后端会阻止删除当前登录账号、删除最后一个管理员，以及删除仍有关联预订的客户或房间。

## 文档

- [毕业设计说明](D:/hotel/docs/毕业设计说明.md)
- [开题报告素材](D:/hotel/docs/开题报告素材.md)

## 已验证内容

- MySQL 8 数据库 `homestay_booking` 已创建。
- 10 张业务表及演示数据已初始化。
- 后端 56 个 Java 源文件编译成功。
- 可执行 JAR 打包成功。
- 前端 2237 个模块完成生产构建。
- 管理员登录、仪表盘、员工、门店与审计接口已通过真实 MySQL 联调。
- 普通员工访问管理员接口返回 HTTP `403`，权限隔离有效。
- 重复日期预订返回业务码 `400`，取消预订后房间恢复为空闲。
- 管理员 8 个业务页面均可打开，普通员工访问管理员路由会自动回到仪表盘。
- 桌面和移动端无横向溢出、按钮裁切、控制台错误，ECharts 图表画布有效。

页面自动化检查命令：

```powershell
cd D:\hotel\frontend
npm run visual:check
```

检查截图输出在 `frontend/artifacts`，包括登录页、桌面仪表盘、房型房间、预订入住、移动端仪表盘和移动端导航。
