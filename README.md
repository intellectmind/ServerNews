# ServerNews

**Read this in other languages: [English](README.md)，[中文](README_zh.md)。**

## Plugin Introduction
A news announcement system plugin designed for Minecraft servers, which allows administrators to publish and manage server news, displaying it to players in beautifully formatted book form.

The plugin supports multiple languages (automatically switching based on client language settings), interactive content, and automatic notification features.

Compatible with Folia, Paper, Bukkit, Purpur, Spigot, and other server cores.

## Features
- 📖 Displays news content in book form
- 🔔 Automatically notifies players of unread news upon login
- 🌍 Supports Chinese and English by default; additional languages can be added in `messages.yml`
- 🔗 Supports URL links and command interactions
- 📊 News statistics tracking
- 📅 Automatically clears expired read records
- 🎨 Supports color codes and HEX colors
- 📱 Compatible with PlaceholderAPI placeholders

> **Note**：PlaceholderAPI must be installed to use PlaceholderAPI functionality

## Usage

### Player Commands
| Command | Description | Permission | Default |
|---------|-------------|------------|---------|
| `/news` | Opens the news book | `servernews.use` | ✅ Available to all players |

### Admin Commands
| Command | Description | Example | Permission |
|---------|-------------|---------|------------|
| `/newsadmin` | Show admin help menu | `/newsadmin` | `servernews.admin` |
| `/newsadmin help` | Show detailed help | `/newsadmin help` | `servernews.admin` |
| `/newsadmin reload` | Reload configuration files | `/newsadmin reload` | `servernews.admin` |
| `/newsadmin add <title> <content> [options]` | Add news with optional features | See examples below | `servernews.admin` |
| `/newsadmin remove <id>` | Remove news by index | `/newsadmin remove 0` | `servernews.admin` |
| `/newsadmin list` | List all news with details | `/newsadmin list` | `servernews.admin` |
| `/newsadmin stats` | Show comprehensive news statistics | `/newsadmin stats` | `servernews.admin` |

### Advanced Add Command Options
The `add` command supports several optional parameters to enhance news functionality:

#### Basic Usage:
```
/newsadmin add "News Title" "News content here"
```

#### With URL Link:
```
/newsadmin add "Update Notice" "Check our website for details" -url https://example.com
```

#### With Command Execution:
```
/newsadmin add "Server Event" "Click to join the event!" -cmd /warp event
```

#### With Hover Text:
```
/newsadmin add "Important" "Server maintenance scheduled" -hover "Hover for more info"
```

#### Combined Features:
```
/newsadmin add "New Features" "Multiple interactive elements" -url https://example.com -cmd /help -hover "Click for website or hover for commands"
```

#### Option Details:
- **`-url <link>`**: Adds a clickable URL link (must start with http:// or https://)
- **`-cmd <command>`**: Adds a clickable command (must start with /)
- **`-hover <text>`**: Adds hover tooltip text (can contain spaces and color codes)

> **Tips**:
> - Options can be used in any order
> - URL and command cannot be used together (command takes priority)
> - Use quotes around text containing spaces
> - You can directly edit `news.yml` and use `/newsadmin reload` afterward

### Permission List

| Permission Node | Description | Default | Notes |
|-----------------|-------------|---------|--------|
| `servernews.use` | Allows using `/news` command | `true` | All players can view news |
| `servernews.admin` | Grants access to admin commands | `op` | Full administrative control |
| `servernews.*` | Grants all news-related permissions | `op` | Includes all permissions |

## Configuration Files

### config.yml
```yaml
# Maximum number of stored news items
max-news: 10

# Auto-notification settings
auto-notification:
  # Delay after player joins (in ticks, 20 ticks = 1 second)
  delay-ticks: 60
```

### `news.yml`
Stores all news entries in the following format:

```yaml
news:
  - title: "News Title"
    content: "News content (supports color codes and placeholders)"
    date: "2024-01-15 14:30"
    url: "https://example.com" # Optional: clickable URL
    command: "/spawn" # Optional: clickable command  
    hover: "Hover text with &acolors" # Optional: hover tooltip
  - title: "Another News"
    content: "More news content here"
    date: "2024-01-14 12:00"
    # Optional fields can be omitted
```

### `messages.yml`
Contains all translatable messages for different languages. The plugin automatically detects player language and uses appropriate messages.

## Command Examples

### Adding News with Colors
```bash
# Basic colored news
/newsadmin add "&6&lServer Update" "&aNew features added! &bCheck them out."

# With HEX colors
/newsadmin add "&#FF0000Important Notice" "&#00FF00Everything is working fine!"

# With PlaceholderAPI
/newsadmin add "Welcome %player_name%" "You have played for %player_time% hours"
```

### Management Examples
```bash
# List all current news
/newsadmin list

# Remove the first news item (index 0)
/newsadmin remove 0

# View detailed statistics
/newsadmin stats

# Reload after manual edits
/newsadmin reload
```

## Frequently Asked Questions

### Q: How to add colored news?
**A:**  
Use `&` followed by color codes or HEX colors:
- `&a` for green text
- `&#FF0000` for red text (HEX color code)
- `&l` for bold, `&o` for italic, `&n` for underline

### Q: Why can't the news book be opened?
**A:**  
Check the following:
1. Player has the `servernews.use` permission
2. No error logs in the server console
3. News data is properly formatted in `news.yml`
4. Plugin is properly loaded and enabled

### Q: How to increase the news limit?
**A:**  
Modify the `max-news` value in `config.yml` and reload the plugin.

### Q: Can I use PlaceholderAPI placeholders?
**A:**  
Yes! Install PlaceholderAPI and use any placeholder in news titles, content, and hover text. Examples:
- `%player_name%` - Player's name
- `%server_name%` - Server name
- `%player_time%` - Play time

### Q: How does the auto-notification work?
**A:**  
The plugin tracks when each player last viewed news. When they join, if there's newer news available, they'll receive a clickable notification after the configured delay.

---

### bStats
![bStats](https://bstats.org/signatures/bukkit/ServerNews.svg)