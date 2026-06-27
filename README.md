# AutoSprint

自动疾跑 —— 地面行走/水中游泳时自动触发疾跑，无需双击 W。

## 功能

- 地面行走自动疾跑
- 水中游泳自动疾跑
- 饱食度 ≤ 6 时不触发
- 仅生存/冒险模式生效
- 支持滑翔/乘坐/激流时跳过
- 玩家可通过指令开关

## 指令

| 指令 | 别名 | 权限 | 说明 |
|------|------|------|------|
| `/autosprint` | `/as` | `autosprint.use` | 切换自动疾跑开关 |
| `/autosprint reload` | - | `autosprint.admin` | 重载配置 |
| `/autosprint debug` | - | `autosprint.admin` | 开关调试模式 |

## 权限

| 权限节点 | 默认 | 说明 |
|----------|------|------|
| `autosprint.use` | 所有人 | 允许使用 `/as` 开关 |
| `autosprint.admin` | OP | 允许重载配置和调试 |
| `autosprint.debug` | OP | 接收调试广播 |

## 配置

`config.yml`:

```yaml
enabled: true
default-enabled: true
min-food: 6
forward-threshold: 0.05
interval-ticks: 1
air-walk-speed: 0.3
default-walk-speed: 0.2
debug: false
```

## 构建

```bash
mvn clean package
```

产物位于 `target/AutoSprint.jar`
