package cn.kurt6.servernews;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import me.clip.placeholderapi.PlaceholderAPI;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewsManager {

    private final ServerNews plugin;
    private final Map<UUID, Long> lastViewTime;
    private final Pattern HEX_PATTERN = Pattern.compile("#[a-fA-F0-9]{6}");

    public NewsManager(ServerNews plugin) {
        this.plugin = plugin;
        this.lastViewTime = new ConcurrentHashMap<>();
    }

    /**
     * 检查玩家是否有未读新闻
     */
    public boolean hasUnreadNews(Player player) {
        UUID playerId = player.getUniqueId();
        long lastView = lastViewTime.getOrDefault(playerId, 0L);

        List<Map<String, Object>> newsList = getNewsList();
        if (newsList.isEmpty()) {
            return false;
        }

        // 获取最新新闻的时间戳
        Map<String, Object> latestNews = newsList.get(0);
        String dateStr = (String) latestNews.get("date");

        try {
            LocalDateTime newsDate = LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            long newsTimestamp = newsDate.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

            return newsTimestamp > lastView;
        } catch (Exception e) {
            return true; // 如果解析失败，假设有未读新闻
        }
    }

    /**
     * 标记玩家已阅读新闻
     */
    public void markAsRead(Player player) {
        lastViewTime.put(player.getUniqueId(), System.currentTimeMillis());
    }

    /**
     * 获取新闻列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getNewsList() {
        FileConfiguration newsConfig = plugin.getNewsConfig();
        return (List<Map<String, Object>>) newsConfig.getList("news", new ArrayList<>());
    }

    /**
     * 添加新闻
     */
    public boolean addNews(String title, String content, String url, String command, String hover) {
        try {
            List<Map<String, Object>> newsList = getNewsList();

            Map<String, Object> newsItem = new HashMap<>();
            newsItem.put("title", title);
            newsItem.put("content", content);
            newsItem.put("date", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

            if (url != null && !url.isEmpty()) {
                newsItem.put("url", url);
            }
            if (command != null && !command.isEmpty()) {
                newsItem.put("command", command);
            }
            if (hover != null && !hover.isEmpty()) {
                newsItem.put("hover", hover);
            }

            newsList.add(0, newsItem); // 添加到开头

            // 限制新闻数量
            int maxNews = plugin.getConfig().getInt("max-news", 10);
            if (newsList.size() > maxNews) {
                newsList = newsList.subList(0, maxNews);
            }

            FileConfiguration newsConfig = plugin.getNewsConfig();
            newsConfig.set("news", newsList);
            newsConfig.save(plugin.getNewsFile());

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to add news: " + e.getMessage());
            return false;
        }
    }

    /**
     * 删除新闻
     */
    public boolean removeNews(int index) {
        try {
            List<Map<String, Object>> newsList = getNewsList();

            if (index < 0 || index >= newsList.size()) {
                return false;
            }

            newsList.remove(index);

            FileConfiguration newsConfig = plugin.getNewsConfig();
            newsConfig.set("news", newsList);
            newsConfig.save(plugin.getNewsFile());

            return true;
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to remove news: " + e.getMessage());
            return false;
        }
    }

    /**
     * 创建格式化的新闻组件
     */
    public Component createFormattedNews(Player player, Map<String, Object> newsItem, String language) {
        String title = (String) newsItem.get("title");
        String content = (String) newsItem.get("content");
        String date = (String) newsItem.get("date");
        String url = (String) newsItem.get("url");
        String command = (String) newsItem.get("command");
        String hover = (String) newsItem.get("hover");

        TextComponent.Builder builder = Component.text();

        // 添加标题和日期（无点击事件）
        Component titleComponent = parseColoredText("&6&l" + title + "\n&r&7" + date + "\n\n");
        builder.append(titleComponent);

        // 添加内容
        Component contentComponent = parseColoredText(content);

        // 处理占位符
        if (plugin.isPlaceholderAPIEnabled()) {
            String contentStr = LegacyComponentSerializer.legacySection().serialize(contentComponent);
            contentStr = PlaceholderAPI.setPlaceholders(player, contentStr);
            contentComponent = parseColoredText(contentStr);
        }

        // 创建可点击的内容组件
        Component interactiveContent = contentComponent;

        // 添加交互功能
        if (url != null && !url.isEmpty()) {
            interactiveContent = interactiveContent.clickEvent(ClickEvent.openUrl(url));
        } else if (command != null && !command.isEmpty()) {
            interactiveContent = interactiveContent.clickEvent(ClickEvent.runCommand(command));
        }

        if (hover != null && !hover.isEmpty()) {
            Component hoverComponent = parseColoredText(hover);
            if (plugin.isPlaceholderAPIEnabled()) {
                String hoverStr = LegacyComponentSerializer.legacySection().serialize(hoverComponent);
                hoverStr = PlaceholderAPI.setPlaceholders(player, hoverStr);
                hoverComponent = parseColoredText(hoverStr);
            }
            interactiveContent = interactiveContent.hoverEvent(HoverEvent.showText(hoverComponent));
        }

        builder.append(interactiveContent);
        return builder.build();
    }

    /**
     * 解析带颜色的文本
     */
    public Component parseColoredText(String text) {
        if (text == null) return Component.empty();

        // 处理十六进制颜色
        text = parseHexColors(text);

        // 处理传统颜色代码
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

    /**
     * 解析十六进制颜色
     */
    private String parseHexColors(String text) {
        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String hexColor = matcher.group();
            String replacement = "§x";
            for (char c : hexColor.substring(1).toCharArray()) {
                replacement += "§" + c;
            }
            matcher.appendReplacement(sb, replacement);
        }

        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 获取新闻统计信息
     */
    public Map<String, Object> getNewsStats() {
        Map<String, Object> stats = new HashMap<>();
        List<Map<String, Object>> newsList = getNewsList();

        stats.put("total", newsList.size());
        stats.put("maxAllowed", plugin.getConfig().getInt("max-news", 10));

        if (!newsList.isEmpty()) {
            Map<String, Object> latest = newsList.get(0);
            stats.put("latestTitle", latest.get("title"));
            stats.put("latestDate", latest.get("date"));
        }

        // 统计有链接和命令的新闻数量
        long withUrl = newsList.stream().mapToLong(news ->
                news.containsKey("url") && !((String)news.get("url")).isEmpty() ? 1 : 0
        ).sum();

        long withCommand = newsList.stream().mapToLong(news ->
                news.containsKey("command") && !((String)news.get("command")).isEmpty() ? 1 : 0
        ).sum();

        stats.put("withUrl", withUrl);
        stats.put("withCommand", withCommand);

        return stats;
    }

    /**
     * 清理过期的阅读记录
     */
    public void cleanupReadHistory() {
        long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L); // 7天前
        lastViewTime.entrySet().removeIf(entry -> entry.getValue() < cutoffTime);
    }

    /**
     * 验证新闻内容
     */
    public boolean validateNews(String title, String content) {
        if (title == null || title.trim().isEmpty()) {
            return false;
        }
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        if (title.length() > 100) { // 标题长度限制
            return false;
        }
        if (content.length() > 2000) { // 内容长度限制
            return false;
        }
        return true;
    }
}