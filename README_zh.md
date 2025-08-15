# ServerNews 插件文档

**其他语言版本: [English](README.md)，[中文](README_zh.md)。**

## 插件介绍
一个为 Minecraft 服务器设计的新闻公告系统插件，它允许管理员发布和管理服务器新闻，并以精美的书籍形式展示给玩家。

插件支持多语言（根据客户端语言自动切换）、交互式内容和自动通知功能

支持Folia, Paper, Bukkit, Purpur, Spigot等多个核心

## 功能特性
- 📖 以书籍形式展示新闻内容
- 🔔 玩家登录时自动通知未读新闻
- 🌍 插件默认支持中文和英文，可以在 `messages.yml` 中添加更多语言支持
- 🔗 支持URL链接和命令交互
- 📊 新闻统计功能
- 📅 自动清理过期阅读记录
- 🎨 支持颜色代码和十六进制颜色
- 📱 兼容 PlaceholderAPI 占位符

> **注意**：如需使用 PlaceholderAPI 功能，请确保已安装 PlaceholderAPI 插件

## 使用方法

### 玩家命令
| 命令 | 描述 | 权限 | 默认 |
|------|------|------|------|
| `/news` | 打开新闻书籍 | `servernews.use` | ✅ 所有玩家可用 |

### 管理员命令
| 命令 | 描述 | 示例 | 权限 |
|------|------|------|------|
| `/newsadmin` | 显示管理员帮助菜单 | `/newsadmin` | `servernews.admin` |
| `/newsadmin help` | 显示详细帮助信息 | `/newsadmin help` | `servernews.admin` |
| `/newsadmin reload` | 重载配置文件 | `/newsadmin reload` | `servernews.admin` |
| `/newsadmin add <标题> <内容> [选项]` | 添加新闻（支持多种功能） | 见下方示例 | `servernews.admin` |
| `/newsadmin remove <序号>` | 删除指定序号的新闻 | `/newsadmin remove 0` | `servernews.admin` |
| `/newsadmin list` | 列出所有新闻详情 | `/newsadmin list` | `servernews.admin` |
| `/newsadmin stats` | 显示详细新闻统计 | `/newsadmin stats` | `servernews.admin` |

### 高级添加命令选项
`add` 命令支持多个可选参数来增强新闻功能：

#### 基础用法：
```
/newsadmin add "新闻标题" "新闻内容"
```

#### 添加URL链接：
```
/newsadmin add "更新通知" "点击查看官网详情" -url https://example.com
```

#### 添加命令执行：
```
/newsadmin add "服务器活动" "点击传送到活动地点！" -cmd /warp event
```

#### 添加悬停提示：
```
/newsadmin add "重要通知" "服务器维护计划" -hover "悬停查看更多信息"
```

#### 组合功能：
```
/newsadmin add "新功能发布" "多种交互元素展示" -url https://example.com -cmd /help -hover "点击访问网站或悬停查看命令"
```

#### 选项详解：
- **`-url <链接>`**: 添加可点击的URL链接（必须以 http:// 或 https:// 开头）
- **`-cmd <命令>`**: 添加可点击的命令（必须以 / 开头）
- **`-hover <文本>`**: 添加悬停提示文本（可包含空格和颜色代码）

> **使用技巧**:
> - 选项可以按任意顺序使用
> - URL和命令不能同时使用（命令优先）
> - 包含空格的文本需要用引号包围
> - 也可以直接在 `news.yml` 中编辑，完成后使用 `/newsadmin reload` 重载

### 权限列表

| 权限节点 | 描述 | 默认值 | 说明 |
|----------|------|--------|------|
| `servernews.use` | 允许使用`/news`命令 | `true` | 所有玩家都可查看新闻 |
| `servernews.admin` | 允许使用新闻管理命令 | `op` | 完整的管理控制权限 |
| `servernews.*` | 包含所有新闻相关权限 | `op` | 包含所有权限 |

## 配置文件

### config.yml
```yaml
# 最大保存的新闻数量
max-news: 10

# 自动通知设置
auto-notification:
  # 玩家加入后延迟时间 (tick，20 tick = 1秒)
  delay-ticks: 60
```

### `news.yml`
存储所有新闻内容，格式为：

```yaml
news:
  - title: "新闻标题"
    content: "新闻内容（支持颜色代码和占位符）"
    date: "2024-01-15 14:30"
    url: "https://example.com" # 可选：可点击的URL
    command: "/spawn" # 可选：可点击的命令
    hover: "悬停文本 &a支持颜色" # 可选：悬停提示
  - title: "另一条新闻"
    content: "更多新闻内容"
    date: "2024-01-14 12:00"
    # 可选字段可以省略
```

### `messages.yml`
包含不同语言的所有可翻译消息。插件会自动检测玩家语言并使用相应消息。

## 命令示例

### 添加彩色新闻
```bash
# 基础彩色新闻
/newsadmin add "&6&l服务器更新" "&a新功能已上线！&b快来体验吧。"

# 使用HEX颜色
/newsadmin add "&#FF0000重要通知" "&#00FF00一切运行正常！"

# 使用PlaceholderAPI
/newsadmin add "欢迎 %player_name%" "您已游戏 %player_time% 小时"
```

### 管理示例
```bash
# 列出所有当前新闻
/newsadmin list

# 删除第一条新闻（索引0）
/newsadmin remove 0

# 查看详细统计
/newsadmin stats

# 手动编辑后重载
/newsadmin reload
```

## 常见问题解答

### Q: 如何添加带颜色的新闻？
**A:**  
使用 `&` 符号加上颜色代码或十六进制颜色：
- `&a` 绿色文字
- `&#FF0000` 红色文字（十六进制颜色代码）
- `&l` 粗体，`&o` 斜体，`&n` 下划线

### Q: 为什么新闻书籍打不开？
**A:**  
请检查以下几点：
1. 玩家是否拥有 `servernews.use` 权限
2. 服务器控制台是否有相关错误日志
3. `news.yml` 中的新闻数据格式是否正确
4. 插件是否正确加载和启用

### Q: 如何增加新闻数量限制？
**A:**  
修改配置文件 `config.yml` 中的 `max-news` 参数值并重载插件。

### Q: 可以使用PlaceholderAPI占位符吗？
**A:**  
可以！安装PlaceholderAPI后，可在新闻标题、内容和悬停文本中使用任何占位符。例如：
- `%player_name%` - 玩家姓名
- `%server_name%` - 服务器名称
- `%player_time%` - 游戏时长

### Q: 自动通知功能是如何工作的？
**A:**  
插件会跟踪每个玩家上次查看新闻的时间。当他们加入服务器时，如果有更新的新闻，将在配置的延迟时间后收到可点击的通知。

---

### bStats
![bStats](https://bstats.org/signatures/bukkit/ServerNews.svg)