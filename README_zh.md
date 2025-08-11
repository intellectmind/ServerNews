# ServerNews 插件文档

## 插件介绍
一个为 Minecraft 服务器设计的新闻公告系统插件，它允许管理员发布和管理服务器新闻，并以精美的书籍形式展示给玩家。插件支持多语言（根据客户端文字自动切换）、交互式内容和自动通知功能

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
| 命令 | 描述 | 权限 |
|------|------|------|
| `/news` | 打开新闻书籍 | `servernews.use` |

### 管理员命令
| 命令 | 描述 | 示例 | 权限 |
|------|------|------|------|
| `/newsadmin reload` | 重载配置文件 | `/newsadmin reload` | `servernews.admin` |
| `/newsadmin add <标题> <内容>` | 添加新闻 | `/newsadmin add "更新公告" "服务器已更新至1.20"` | `servernews.admin` |
| `/newsadmin remove <序号>` | 删除新闻 | `/newsadmin remove 0` | `servernews.admin` |
| `/newsadmin list` | 列出所有新闻 | `/newsadmin list` | `servernews.admin` |
| `/newsadmin stats` | 显示新闻统计 | `/newsadmin stats` | `servernews.admin` |

> **建议**：直接在`news.yml`编辑新闻，完成后reload即可

### 权限列表

| 权限节点            | 描述                     | 默认值 |
|---------------------|--------------------------|--------|
| `servernews.use`    | 允许使用`/news`命令      | true   |
| `servernews.admin`  | 允许使用新闻管理命令     | op     |
| `servernews.*`      | 包含所有新闻相关权限    | op     |

## 配置文件

### config.yml
```yaml
# 最大保存的新闻数量
max-news: 10

# 自动通知设置
auto-notification:
  # 玩家加入后延迟时间 (tick)
  delay-ticks: 60
```

### `news.yml`存储所有新闻内容，格式为：

```yaml
news:
  - title: "新闻标题"
    content: "新闻内容（支持颜色代码）"
    date: "YYYY-MM-DD HH:mm"
    url: "https://example.com" # 可选
    command: "/command" # 可选
    hover: "悬停提示文本" # 可选
```

## 常见问题解答

### Q: 如何添加带颜色的新闻？
**A:**  
使用 `&` 符号加上颜色代码，例如：
- `&a` 绿色文字
- `&#FF0000` 红色文字（十六进制颜色代码）

### Q: 为什么新闻书籍打不开？
**A:**  
请检查：
1. 玩家是否拥有 `servernews.use` 权限
2. 服务器控制台是否有相关错误日志

### Q: 如何增加新闻数量限制？
**A:**  
修改配置文件 `config.yml` 中的 `max-news` 参数值
