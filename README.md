# AutoSprint

自动疾跑 —— 地面移动时自动触发疾跑，无需双击 W。

## 功能

- 地面行走自动疾跑
- 饱食度 ≤ 6 时不触发
- 仅生存/冒险模式生效
- 支持游泳/滑翔/乘坐/激流时跳过
- 玩家可通过指令开关

## 指令

| 指令 | 别名 | 说明 |
|------|------|------|
| `/autosprint` | `/as` | 切换自动疾跑开关 |

## 配置

`config.yml`:

```yaml
default-enabled: true
min-food: 6
```

## 构建

```bash
mvn clean package
```

产物位于 `target/AutoSprint.jar`
