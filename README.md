# ServerNews

**Read this in other languages: [English](README.md)，[中文](README_zh.md)。**

## Plugin Introduction
A news announcement system plugin designed for Minecraft servers, which allows administrators to publish and manage server news, displaying it to players in beautifully formatted book form. The plugin supports multiple languages (automatically switching based on client language settings), interactive content, and automatic notification features.  

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
| Command | Description | Permission |
|------|------|------|
| `/news` | Opens the news book | `servernews.use` |

### Admin Commands
| Command | Description | Example | Permission |
|------|------|------|------|
| `/newsadmin reload` | 	Reload configuration files	 | `/newsadmin reload` | `servernews.admin` |
| `/newsadmin add <title> <content>` | Add news | `/newsadmin add "Update Notice" "Server updated to 1.20"` | `servernews.admin` |
| `/newsadmin remove <id>` | Remove news | `/newsadmin remove 0` | `servernews.admin` |
| `/newsadmin list` | 	List all news | `/newsadmin list` | `servernews.admin` |
| `/newsadmin stats` | 	Show news statistics | `/newsadmin stats` | `servernews.admin` |

> **Tip**：You can directly edit `news.yml` and reload afterward

### Permission List

| Permission Node            | 	Description                     | Default |
|---------------------|--------------------------|--------|
| `servernews.use`    | Allows using `/news` command      | true   |
| `servernews.admin`  | Grants access to admin commands     | op     |
| `servernews.*`      | Grants all news-related permissions    | op     |

## Configuration Files

### config.yml
```yaml
# Maximum number of stored news  
max-news: 10

auto-notification:
  # Delay after player joins (in ticks) 
  delay-ticks: 60
```

### `news.yml` stores all news entries in the following format:

```yaml
news:
  - title: "News Title"
    content: "News content (supports color codes)"
    date: "YYYY-MM-DD HH:mm"
    url: "https://example.com" # Optional  
    command: "/command" # Optional  
    hover: "Hover text" # Optional  
```

## Frequently Asked Questions

### Q: How to add colored news?  
**A:**  
Use `&` followed by color codes, for example:  
- `&a` for green text    
- `&#FF0000` for red text (HEX color code)  

### Q: Why can't the news book be opened?  
**A:**  
Check：  
1. If the player has the `servernews.use` permission  
2. For any error logs in the server console    

### Q: How to increase the news limit?  
**A:**  
Modify the `max-news` value in `config.yml`  

---

### bStats
![bStats](https://bstats.org/signatures/bukkit/ServerNews.svg)
