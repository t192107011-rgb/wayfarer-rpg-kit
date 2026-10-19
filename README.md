# Wayfarer RPG Kit · 旅人 RPG

原生 Android、Java 编写的单人回合制 RPG 入门模板。无需 Unity 许可证、服务器、账号或付费素材；游戏完全离线运行。程序绘制地图和角色，没有 WebView。

**版本：0.1.0 · Android 8.0（API 26）及以上 · MIT**

## 已有内容

- 三张地图：风栖村、回声林地、灯塔遗迹；格子移动、碰撞、传送点和宝箱。
- 可完成的两阶段主线：接受长老委托 → 清理三只敌人 → 回村领奖 → 击败古塔守卫 → 回村完成结局。
- 回合制战斗：攻击、星火斩、防御、生命/法力药水和逃跑；敌人每第三回合重击。
- 经验升级、金币、背包、商店及自动穿戴装备；免费旅店与战败回村。
- 独立自动存档 + 3 个手动存档；战斗中可保存；版本和范围校验；原子写入；覆盖确认。
- 触屏方向按钮、键盘方向键/WASD、E/空格交互；纵向滚动界面兼容较小屏幕。
- Java 核心回归测试、GitHub Actions 自动检查并生成测试 APK。

这是可玩且可扩展的**项目起点**。当前为单角色、固定地图、线性示例任务；尚未包含队伍、职业树、复杂装备槽、地图编辑器、音效音乐、联网及应用商店正式签名。

## 手机上获取 APK

进入本仓库 **Actions → Android checks and APK → 成功的运行 → Artifacts → wayfarer-rpg-kit-debug-apk**，下载 ZIP 并解压安装其中的 `app-debug.apk`。需要登录 GitHub；构建产物保留 14 天，可用 Run workflow 再生成。

Debug APK 用于测试。不同构建环境可能使用不同 debug 签名；签名不同时无法直接覆盖安装。正式持续发布前请配置自己的固定签名，保管好密钥，不能提交至仓库。

## 本地运行

1. 安装 JDK 17、Android SDK Platform 35、Build Tools 35.0.0。
2. 用 Android Studio 打开项目，等待 Gradle 同步。
3. 连接 Android 8.0+ 手机，点击 Run。

命令行（Linux/macOS）：

```bash
bash scripts/test-core.sh
bash gradlew lintDebug assembleDebug
```

Windows 可在 Git Bash 中运行上面的核心测试；构建用 `gradlew.bat lintDebug assembleDebug`。设置 `ANDROID_HOME`，或使用本机 `local.properties` 的 `sdk.dir`。本机路径不要提交。

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`。首次构建需要联网下载工具，APK 安装后游戏不依赖网络。

## 玩法

起点位于村庄中央。走到「长」旁点交互接任务。走到右侧金色箭头进入森林。红色 `!` 为战斗，金色方块为一次性宝箱。

星火斩消耗 5 MP；防御恢复 3 MP，并将本次所受伤害减半。战斗中使用药水也消耗回合。生命/法力已满或物品不足时不扣物品、不消耗回合。普通战斗逃跑成功率 70%；首领不能逃跑。

旅店免费恢复，刷新普通敌人；首领和宝箱不会刷新。战败扣除最多 20 金币，回村恢复。完成任务后必须回到长老身边交互领取奖励。新游戏只覆盖自动存档，保留手动槽。

## 源码导航

| 文件 | 用途 |
| --- | --- |
| `core/GameData.java` | 地图格子、敌人数值、NPC/宝箱/传送点定义 |
| `core/GameEngine.java` | 探索、任务、战斗、经济、成长与存档格式 |
| `WorldView.java` | 原生 Canvas 程序绘图，可替换为素材渲染 |
| `MainActivity.java` | 操作界面、对话框、生命周期 |
| `SaveStore.java` | App 私有目录、原子保存与有界读取 |
| `tests/CoreTests.java` | 完整主线及边界回归测试 |

Java 文件根目录为 `app/src/main/java/io/wayfarer/rpg/`。

- [进一步开发指南](docs/DEVELOPMENT.md)
- [验收与测试说明](docs/TESTING.md)
- [第三方说明](THIRD_PARTY_NOTICES.md)

## 复制成自己的游戏

将仓库作为模板使用，或克隆后建立新仓库。修改 `applicationId`、`namespace`、Java 包名/路径、应用名、图标与剧情数据。新游戏应使用新 `applicationId`，避免覆盖原模板及其存档。

## 存档与权限

无互联网权限、无广告/统计 SDK、无外部存储权限。存档放在 App 私有目录，关闭系统备份。每次操作后和切入后台时保存，读档先完整校验再替换内存状态。自动存档损坏时会提示，允许从手动槽恢复，不会静默覆盖。卸载会清除存档；当前没有导入导出功能。存档为本地明文数据，校验用于稳定性，不是反作弊或加密。

## 许可证

原创源码和程序绘制图形采用 MIT，可修改、商用，分发时保留许可证。Gradle Wrapper 等工具的条款见第三方说明。
