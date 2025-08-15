package cn.kurt6.servernews;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import me.clip.placeholderapi.PlaceholderAPI;

import java.io.File;
import java.io.IOException;
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

    private long lastSaveTime = 0;
    private static final long SAVE_INTERVAL = 30 * 60 * 1000; // 30分钟保存一次

    /**
     * 标记玩家已阅读新闻
     */
    public void markAsRead(Player player) {
        lastViewTime.put(player.getUniqueId(), System.currentTimeMillis());

        // 每隔 30 分钟自动保存一次
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSaveTime > SAVE_INTERVAL) {
            // 尝试异步保存，如果失败则同步保存
            try {
                saveReadHistoryAsync();
                lastSaveTime = currentTime;
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to save read history asynchronously, saving synchronously: " + e.getMessage());
                saveReadHistory();
                lastSaveTime = currentTime;
            }
        }
    }

    /**
     * 异步保存阅读历史 - Folia 兼容
     */
    public void saveReadHistoryAsync() {
        try {
            // 检查是否为 Folia 环境
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            // Folia 环境下使用全局调度器的异步方法
            Bukkit.getAsyncScheduler().runNow(plugin, task -> {
                saveReadHistory();
            });
        } catch (ClassNotFoundException e) {
            // 传统 Bukkit 环境
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                saveReadHistory();
            });
        } catch (Exception e) {
            // 如果异步调用失败，直接同步保存
            plugin.getLogger().warning("Failed to schedule async save, saving synchronously: " + e.getMessage());
            saveReadHistory();
        }
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

            // 只在非空且非null时添加可选字段
            if (url != null && !url.trim().isEmpty()) {
                newsItem.put("url", url.trim());
            }
            if (command != null && !command.trim().isEmpty()) {
                newsItem.put("command", command.trim());
            }
            if (hover != null && !hover.trim().isEmpty()) {
                newsItem.put("hover", hover.trim());
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
            e.printStackTrace();
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
     * 创建格式化的新闻组件（支持分页）
     */
    public List<Component> createFormattedNews(Player player, Map<String, Object> newsItem, String language) {
        List<Component> pages = new ArrayList<>();
        String title = (String) newsItem.get("title");
        String content = (String) newsItem.get("content");
        String date = (String) newsItem.get("date");
        String url = (String) newsItem.get("url");
        String command = (String) newsItem.get("command");
        String hover = (String) newsItem.get("hover");

        // 处理占位符
        if (plugin.isPlaceholderAPIEnabled()) {
            title = PlaceholderAPI.setPlaceholders(player, title);
            content = PlaceholderAPI.setPlaceholders(player, content);
            if (hover != null) {
                hover = PlaceholderAPI.setPlaceholders(player, hover);
            }
        }

        // 创建标题和日期组件（无点击事件）
        Component titleComponent = parseColoredText("&6&l" + title + "\n&r&7" + date + "\n\n");

        // 处理内容分页
        String[] lines = content.split("\n");
        StringBuilder currentPageContent = new StringBuilder();
        int currentPage = 1;

        // 计算总页数
        int estimatedLines = 0;
        for (String line : lines) {
            estimatedLines += Math.max(1, (line.length() / 20) + 1); // 估算行数，每行约20字符
        }
        int totalPages = Math.max(1, (int) Math.ceil(estimatedLines / 9.0)); // 每页约9行

        // 重新处理分页
        int linesOnCurrentPage = 0;
        for (String line : lines) {
            // 检查是否需要换页
            int lineLength = Math.max(1, (line.length() / 20) + 1);
            if (linesOnCurrentPage + lineLength > 9 && currentPageContent.length() > 0) {
                // 完成当前页
                addPage(pages, titleComponent, currentPageContent.toString(),
                        currentPage, totalPages, url, command, hover);
                currentPage++;
                currentPageContent = new StringBuilder();
                linesOnCurrentPage = 0;
            }
            currentPageContent.append(line).append("\n");
            linesOnCurrentPage += lineLength;
        }

        // 添加最后一页
        if (currentPageContent.length() > 0) {
            // 重新计算总页数（防止估算错误）
            totalPages = Math.max(currentPage, totalPages);
            addPage(pages, titleComponent, currentPageContent.toString(),
                    currentPage, totalPages, url, command, hover);
        }

        return pages;
    }

    /**
     * 添加一个新闻页面
     */
    private void addPage(List<Component> pages, Component titleComponent, String content,
                         int currentPage, int totalPages, String url, String command, String hover) {
        TextComponent.Builder builder = Component.text();

        // 添加标题
        builder.append(titleComponent);

        // 添加内容
        Component contentComponent = parseColoredText(content);

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
            interactiveContent = interactiveContent.hoverEvent(HoverEvent.showText(hoverComponent));
        }

        builder.append(interactiveContent);

        // 如果有多页，添加页码信息
        if (totalPages > 1) {
            Component pageInfo = parseColoredText("\n\n&7[第 " + currentPage + "/" + totalPages + " 页]");
            builder.append(pageInfo);
        }

        pages.add(builder.build());
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

        // 统计有链接、命令和悬停的新闻数量
        long withUrl = newsList.stream().mapToLong(news ->
                news.containsKey("url") && !((String)news.get("url")).isEmpty() ? 1 : 0
        ).sum();

        long withCommand = newsList.stream().mapToLong(news ->
                news.containsKey("command") && !((String)news.get("command")).isEmpty() ? 1 : 0
        ).sum();

        long withHover = newsList.stream().mapToLong(news ->
                news.containsKey("hover") && !((String)news.get("hover")).isEmpty() ? 1 : 0
        ).sum();

        stats.put("withUrl", withUrl);
        stats.put("withCommand", withCommand);
        stats.put("withHover", withHover);

        return stats;
    }

    /**
     * 清理过期的阅读记录
     */
    public void cleanupReadHistory() {
        try {
            long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L); // 7天前
            int removedCount = 0;

            Iterator<Map.Entry<UUID, Long>> iterator = lastViewTime.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, Long> entry = iterator.next();
                if (entry.getValue() < cutoffTime) {
                    iterator.remove();
                    removedCount++;
                }
            }

            if (removedCount > 0) {
                plugin.getLogger().info("Cleaned up " + removedCount + " expired read history entries");
                // 清理后保存一次
                saveReadHistory(); // 同步保存，因为我们已经在异步任务中了
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error during read history cleanup: " + e.getMessage());
        }
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

    /**
     * 保存阅读历史到文件
     */
    public void saveReadHistory() {
        File readHistoryFile = new File(plugin.getDataFolder(), "read_history.yml");
        YamlConfiguration config = new YamlConfiguration();

        // 将UUID-long对转换为UUID-string对以便存储
        Map<String, String> saveMap = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : lastViewTime.entrySet()) {
            saveMap.put(entry.getKey().toString(), entry.getValue().toString());
        }

        config.set("read_history", saveMap);

        try {
            config.save(readHistoryFile);
            plugin.getLogger().info("Saved reading history for " + saveMap.size() + " players");
        } catch (IOException e) {
            plugin.getLogger().warning("Unable to save reading history: " + e.getMessage());
        }
    }

    /**
     * 从文件加载阅读历史
     */
    @SuppressWarnings("unchecked")
    public void loadReadHistory() {
        File readHistoryFile = new File(plugin.getDataFolder(), "read_history.yml");
        if (!readHistoryFile.exists()) {
            plugin.getLogger().info("No reading history file found, starting fresh");
            return;
        }

        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(readHistoryFile);
            Map<String, Object> rawMap = config.getConfigurationSection("read_history") != null
                    ? config.getConfigurationSection("read_history").getValues(false)
                    : new HashMap<>();

            int loadedCount = 0;
            for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                try {
                    UUID uuid = UUID.fromString(entry.getKey());
                    // 处理可能为String或Long的值
                    long time = entry.getValue() instanceof String
                            ? Long.parseLong((String) entry.getValue())
                            : (Long) entry.getValue();
                    lastViewTime.put(uuid, time);
                    loadedCount++;
                } catch (Exception e) {
                    plugin.getLogger().warning("Error loading reading history for entry: " + entry.getKey() + " - " + e.getMessage());
                }
            }

            plugin.getLogger().info("Loaded reading history for " + loadedCount + " players");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load reading history: " + e.getMessage());
        }
    }
}