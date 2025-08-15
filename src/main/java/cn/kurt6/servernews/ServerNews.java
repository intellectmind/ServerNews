package cn.kurt6.servernews;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.TabCompleter;
import me.clip.placeholderapi.PlaceholderAPI;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ServerNews extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private FileConfiguration messagesConfig;
    private FileConfiguration newsConfig;
    private File messagesFile;
    private File newsFile;
    private boolean placeholderAPIEnabled = false;
    private NewsManager newsManager;

    @Override
    public void onEnable() {
        // bStats
        int pluginId = 26872;
        cn.kurt6.back.bStats.Metrics metrics = new cn.kurt6.back.bStats.Metrics(this, pluginId);

        // 检查PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            placeholderAPIEnabled = true;
            getLogger().info("PlaceholderAPI found and enabled!");
        }

        // 创建配置文件
        createConfigs();

        // 初始化NewsManager
        newsManager = new NewsManager(this);

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(this, this);

        // 注册命令
        Objects.requireNonNull(getCommand("news")).setExecutor(this);
        Objects.requireNonNull(getCommand("newsadmin")).setExecutor(this);
        Objects.requireNonNull(getCommand("newsadmin")).setTabCompleter(this);

        newsManager.loadReadHistory();

        // 定期清理阅读历史 - Folia兼容方式
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            // Folia服务器
            getLogger().info("Detected Folia server, using Folia scheduler");
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(this,
                    task -> {
                        try {
                            newsManager.cleanupReadHistory();
                        } catch (Exception e) {
                            getLogger().warning("Error during read history cleanup: " + e.getMessage());
                        }
                    },
                    20L * 60 * 60, // 初始延迟1小时
                    20L * 60 * 60 * 6 // 每6小时执行一次
            );
        } catch (ClassNotFoundException e) {
            // 传统Bukkit服务器
            getLogger().info("Using traditional Bukkit scheduler");
            Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                    () -> {
                        try {
                            newsManager.cleanupReadHistory();
                        } catch (Exception ex) {
                            getLogger().warning("Error during read history cleanup: " + ex.getMessage());
                        }
                    },
                    20L * 60 * 60, // 1小时后开始
                    20L * 60 * 60 * 6 // 每6小时执行一次
            );
        }

        getLogger().info("ServerNews Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        newsManager.saveReadHistory();
        getLogger().info("ServerNews Plugin has been disabled!");
    }

    private void createConfigs() {
        // 创建messages.yml
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // 创建news.yml
        newsFile = new File(getDataFolder(), "news.yml");
        if (!newsFile.exists()) {
            saveResource("news.yml", false);
        }
        newsConfig = YamlConfiguration.loadConfiguration(newsFile);

        // 创建默认配置
        saveDefaultConfig();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int delayTicks = getConfig().getInt("auto-notification.delay-ticks", 60);

        // 使用 Folia 兼容的方式调度任务
        if (isFolia()) {
            // Folia 使用 EntityScheduler
            player.getScheduler().runDelayed(this, task -> {
                if (player.isOnline() && newsManager.hasUnreadNews(player)) {
                    String playerLang = getPlayerLanguage(player);
                    sendNewsNotification(player, playerLang);
                }
            }, null, delayTicks);
        } else {
            // 传统 Bukkit 使用全局调度器
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline() && newsManager.hasUnreadNews(player)) {
                    String playerLang = getPlayerLanguage(player);
                    sendNewsNotification(player, playerLang);
                }
            }, delayTicks);
        }
    }

    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("newsadmin")) {
            if (args.length == 1) {
                // 主命令补全
                List<String> completions = new ArrayList<>();
                String partial = args[0].toLowerCase();

                if ("reload".startsWith(partial)) completions.add("reload");
                if ("add".startsWith(partial)) completions.add("add");
                if ("remove".startsWith(partial)) completions.add("remove");
                if ("list".startsWith(partial)) completions.add("list");
                if ("stats".startsWith(partial)) completions.add("stats");
                if ("help".startsWith(partial)) completions.add("help");

                return completions;
            } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
                // remove命令的索引补全
                List<Map<String, Object>> newsList = newsManager.getNewsList();
                List<String> indices = new ArrayList<>();
                for (int i = 0; i < newsList.size(); i++) {
                    indices.add(String.valueOf(i));
                }
                return indices;
            } else if (args[0].equalsIgnoreCase("add") && args.length > 3) {
                // add命令的选项补全
                List<String> completions = new ArrayList<>();
                String partial = args[args.length - 1].toLowerCase();

                if ("-url".startsWith(partial)) completions.add("-url");
                if ("-cmd".startsWith(partial)) completions.add("-cmd");
                if ("-hover".startsWith(partial)) completions.add("-hover");

                return completions;
            }
        }
        return null;
    }

    private void sendNewsNotification(Player player, String lang) {
        String message = messagesConfig.getString("messages." + lang + ".news-notification",
                messagesConfig.getString("messages.zh.news-notification"));
        String clickText = messagesConfig.getString("messages." + lang + ".click-to-view",
                messagesConfig.getString("messages.zh.click-to-view"));

        // 处理占位符
        if (placeholderAPIEnabled) {
            message = PlaceholderAPI.setPlaceholders(player, message);
            clickText = PlaceholderAPI.setPlaceholders(player, clickText);
        }

        Component notification = parseColoredText(message)
                .clickEvent(ClickEvent.runCommand("/news"))
                .hoverEvent(HoverEvent.showText(parseColoredText(clickText)));

        player.sendMessage(notification);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("news")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("This command can only be used by players!");
                return true;
            }

            Player player = (Player) sender;
            openNewsBook(player);
            return true;
        }

        if (command.getName().equalsIgnoreCase("newsadmin")) {
            if (!sender.hasPermission("servernews.admin")) {
                String lang = sender instanceof Player ? getPlayerLanguage((Player) sender) : "zh";
                String message = getMessage(lang, "no-permission");
                sender.sendMessage(parseColoredText(message));
                return true;
            }

            if (args.length == 0) {
                sendAdminHelp(sender);
                return true;
            }

            String lang = sender instanceof Player ? getPlayerLanguage((Player) sender) : "zh";

            switch (args[0].toLowerCase()) {
                case "reload":
                    reloadConfigs();
                    String message = getMessage(lang, "config-reloaded");
                    sender.sendMessage(parseColoredText(message));
                    break;

                case "add":
                    handleAddCommand(sender, args, lang);
                    break;

                case "remove":
                    handleRemoveCommand(sender, args, lang);
                    break;

                case "list":
                    handleListCommand(sender, lang);
                    break;

                case "stats":
                    handleStatsCommand(sender, lang);
                    break;

                case "help":
                default:
                    sendAdminHelp(sender);
                    break;
            }
            return true;
        }

        return false;
    }

    /**
     * 处理添加新闻命令
     */
    private void handleAddCommand(CommandSender sender, String[] args, String lang) {
        if (args.length < 3) {
            sender.sendMessage(parseColoredText(getMessage(lang, "news-add-usage")));
            return;
        }

        try {
            // 解析参数
            ParsedAddCommand parsed = parseAddCommand(args);

            // 验证新闻内容
            if (!newsManager.validateNews(parsed.title, parsed.content)) {
                sender.sendMessage(parseColoredText(getMessage(lang, "news-add-invalid")));
                return;
            }

            // 验证URL格式
            if (parsed.url != null && !isValidUrl(parsed.url)) {
                sender.sendMessage(parseColoredText(getMessage(lang, "news-add-invalid-url")));
                return;
            }

            // 验证命令格式
            if (parsed.command != null && !isValidCommand(parsed.command)) {
                sender.sendMessage(parseColoredText(getMessage(lang, "news-add-invalid-command")));
                return;
            }

            // 添加新闻
            if (newsManager.addNews(parsed.title, parsed.content, parsed.url, parsed.command, parsed.hover)) {
                sender.sendMessage(parseColoredText(getMessage(lang, "news-add-success")));
            } else {
                sender.sendMessage(parseColoredText(getMessage(lang, "news-add-failed")));
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage(parseColoredText("§c" + e.getMessage()));
        }
    }

    /**
     * 解析添加命令的参数
     */
    private ParsedAddCommand parseAddCommand(String[] args) {
        String title = args[1];
        StringBuilder contentBuilder = new StringBuilder();
        String url = null;
        String command = null;
        String hover = null;

        // 查找选项的开始位置
        int optionsStart = -1;
        for (int i = 2; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                optionsStart = i;
                break;
            } else {
                if (contentBuilder.length() > 0) {
                    contentBuilder.append(" ");
                }
                contentBuilder.append(args[i]);
            }
        }

        String content = contentBuilder.toString();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Content cannot be empty!");
        }

        // 解析选项
        if (optionsStart != -1) {
            for (int i = optionsStart; i < args.length; i++) {
                if (args[i].equals("-url") && i + 1 < args.length) {
                    url = args[i + 1];
                    i++; // 跳过下一个参数
                } else if (args[i].equals("-cmd") && i + 1 < args.length) {
                    command = args[i + 1];
                    i++; // 跳过下一个参数
                } else if (args[i].equals("-hover") && i + 1 < args.length) {
                    // 悬停文本可能包含空格，收集直到下一个选项或结束
                    StringBuilder hoverBuilder = new StringBuilder();
                    i++; // 移动到悬停文本的第一个单词
                    while (i < args.length && !args[i].startsWith("-")) {
                        if (hoverBuilder.length() > 0) {
                            hoverBuilder.append(" ");
                        }
                        hoverBuilder.append(args[i]);
                        i++;
                    }
                    i--; // 回退一步，因为外层循环会自增
                    hover = hoverBuilder.toString();
                }
            }
        }

        return new ParsedAddCommand(title, content, url, command, hover);
    }

    /**
     * 验证URL格式
     */
    private boolean isValidUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
    }

    /**
     * 验证命令格式
     */
    private boolean isValidCommand(String command) {
        return command.startsWith("/");
    }

    /**
     * 处理删除新闻命令
     */
    private void handleRemoveCommand(CommandSender sender, String[] args, String lang) {
        if (args.length < 2) {
            sender.sendMessage(parseColoredText(getMessage(lang, "news-remove-usage")));
            return;
        }

        try {
            int index = Integer.parseInt(args[1]);
            if (newsManager.removeNews(index)) {
                sender.sendMessage(parseColoredText(getMessage(lang, "news-remove-success")));
            } else {
                sender.sendMessage(parseColoredText(getMessage(lang, "news-remove-invalid")));
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(parseColoredText(getMessage(lang, "news-remove-number-error")));
        }
    }

    /**
     * 处理列表命令
     */
    private void handleListCommand(CommandSender sender, String lang) {
        List<Map<String, Object>> newsList = newsManager.getNewsList();

        sender.sendMessage(parseColoredText(getMessage(lang, "news-list-header")));

        if (newsList.isEmpty()) {
            sender.sendMessage(parseColoredText(getMessage(lang, "news-list-empty")));
        } else {
            for (int i = 0; i < newsList.size(); i++) {
                Map<String, Object> news = newsList.get(i);
                String title = (String) news.get("title");
                String date = (String) news.get("date");

                StringBuilder itemBuilder = new StringBuilder();
                String itemTemplate = getMessage(lang, "news-list-item");
                itemBuilder.append(itemTemplate
                        .replace("{index}", String.valueOf(i))
                        .replace("{title}", title)
                        .replace("{date}", date));

                // 添加功能标识
                if (news.containsKey("url") && !((String)news.get("url")).isEmpty()) {
                    itemBuilder.append(getMessage(lang, "news-list-has-url"));
                }
                if (news.containsKey("command") && !((String)news.get("command")).isEmpty()) {
                    itemBuilder.append(getMessage(lang, "news-list-has-cmd"));
                }
                if (news.containsKey("hover") && !((String)news.get("hover")).isEmpty()) {
                    itemBuilder.append(getMessage(lang, "news-list-has-hover"));
                }

                sender.sendMessage(parseColoredText(itemBuilder.toString()));
            }
        }
    }

    /**
     * 处理统计命令
     */
    private void handleStatsCommand(CommandSender sender, String lang) {
        Map<String, Object> stats = newsManager.getNewsStats();

        sender.sendMessage(parseColoredText(getMessage(lang, "stats-header")));

        String totalMsg = getMessage(lang, "stats-total")
                .replace("{total}", String.valueOf(stats.get("total")))
                .replace("{max}", String.valueOf(stats.get("maxAllowed")));
        sender.sendMessage(parseColoredText(totalMsg));

        if (stats.containsKey("latestTitle")) {
            String latestMsg = getMessage(lang, "stats-latest")
                    .replace("{title}", (String) stats.get("latestTitle"));
            sender.sendMessage(parseColoredText(latestMsg));

            String dateMsg = getMessage(lang, "stats-date")
                    .replace("{date}", (String) stats.get("latestDate"));
            sender.sendMessage(parseColoredText(dateMsg));
        }

        String urlMsg = getMessage(lang, "stats-with-url")
                .replace("{count}", String.valueOf(stats.get("withUrl")));
        sender.sendMessage(parseColoredText(urlMsg));

        String cmdMsg = getMessage(lang, "stats-with-command")
                .replace("{count}", String.valueOf(stats.get("withCommand")));
        sender.sendMessage(parseColoredText(cmdMsg));

        String hoverMsg = getMessage(lang, "stats-with-hover")
                .replace("{count}", String.valueOf(stats.get("withHover")));
        sender.sendMessage(parseColoredText(hoverMsg));
    }

    /**
     * 获取本地化消息
     */
    private String getMessage(String lang, String key) {
        return messagesConfig.getString("messages." + lang + "." + key,
                messagesConfig.getString("messages.zh." + key, "§cMessage not found: " + key));
    }

    private void openNewsBook(Player player) {
        String playerLang = getPlayerLanguage(player);

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta bookMeta = (BookMeta) book.getItemMeta();

        if (bookMeta == null) return;

        // 设置书籍标题和作者
        String title = messagesConfig.getString("messages." + playerLang + ".book-title",
                messagesConfig.getString("messages.zh.book-title"));
        String author = messagesConfig.getString("messages." + playerLang + ".book-author",
                messagesConfig.getString("messages.zh.book-author"));

        bookMeta.setTitle(parseStringToLegacy(title));
        bookMeta.setAuthor(parseStringToLegacy(author));

        // 生成书籍页面
        List<Component> pages = generateNewsPages(player, playerLang);
        bookMeta.pages(pages);

        book.setItemMeta(bookMeta);
        player.openBook(book);

        // 标记为已阅读
        newsManager.markAsRead(player);
    }

    private List<Component> generateNewsPages(Player player, String lang) {
        List<Component> pages = new ArrayList<>();

        // 第一页 - 欢迎页
        Component welcomePage = createWelcomePage(player, lang);
        pages.add(welcomePage);

        // 新闻页面
        List<Map<String, Object>> newsList = newsManager.getNewsList();

        if (newsList.isEmpty()) {
            String noNewsMsg = messagesConfig.getString("messages." + lang + ".no-news",
                    messagesConfig.getString("messages.zh.no-news"));
            pages.add(parseColoredText(noNewsMsg));
        } else {
            for (Map<String, Object> newsItem : newsList) {
                List<Component> newsPages = newsManager.createFormattedNews(player, newsItem, lang);
                for (Component page : newsPages) {
                    TextComponent.Builder builder = Component.text();
                    builder.append(page);
                    pages.add(builder.build());
                }
            }
        }

        return pages;
    }

    private Component createWelcomePage(Player player, String lang) {
        String welcome = messagesConfig.getString("messages." + lang + ".welcome-page",
                messagesConfig.getString("messages.zh.welcome-page"));

        // 处理占位符
        if (placeholderAPIEnabled) {
            welcome = PlaceholderAPI.setPlaceholders(player, welcome);
        }

        // 替换玩家名称和时间
        welcome = welcome.replace("{player}", player.getName())
                .replace("{time}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));

        return newsManager.parseColoredText(welcome);
    }

    private Component parseColoredText(String text) {
        return newsManager.parseColoredText(text);
    }

    private String parseStringToLegacy(String text) {
        if (text == null) return "";
        return text.replace('&', '§');
    }

    private String getPlayerLanguage(Player player) {
        // 获取玩家完整的locale (如zh_CN, en_US, ja_JP等)
        String locale = player.locale().toString().toLowerCase();

        // 支持的语言映射表
        Map<String, String> languageMap = new HashMap<>();
        languageMap.put("zh_cn", "zh");  // 简体中文
        languageMap.put("zh_tw", "zh_tw"); // 繁体中文
        languageMap.put("zh_hk", "zh_tw"); // 香港繁体
        languageMap.put("ja", "ja");    // 日语
        languageMap.put("ko", "ko");    // 韩语
        languageMap.put("es", "es");    // 西班牙语
        languageMap.put("fr", "fr");    // 法语
        languageMap.put("de", "de");    // 德语
        languageMap.put("ru", "ru");    // 俄语
        // 可以继续添加更多语言支持

        // 检查完整locale匹配
        if (languageMap.containsKey(locale)) {
            return languageMap.get(locale);
        }

        // 检查主要语言部分 (如zh_CN -> zh)
        String primaryLanguage = locale.split("_")[0];
        if (languageMap.containsKey(primaryLanguage)) {
            return languageMap.get(primaryLanguage);
        }

        // 默认英语
        return "en";
    }

    private void reloadConfigs() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        newsConfig = YamlConfiguration.loadConfiguration(newsFile);
        reloadConfig();
    }

    private void sendAdminHelp(CommandSender sender) {
        String lang = sender instanceof Player ? getPlayerLanguage((Player) sender) : "zh";

        sender.sendMessage(parseColoredText(getMessage(lang, "admin-help-header")));
        sender.sendMessage(parseColoredText(getMessage(lang, "admin-help-reload")));
        sender.sendMessage(parseColoredText(getMessage(lang, "admin-help-add")));
        sender.sendMessage(parseColoredText(getMessage(lang, "admin-help-remove")));
        sender.sendMessage(parseColoredText(getMessage(lang, "admin-help-list")));
        sender.sendMessage(parseColoredText(getMessage(lang, "admin-help-stats")));
        sender.sendMessage(parseColoredText(getMessage(lang, "admin-help-addoptions")));
        sender.sendMessage(parseColoredText(getMessage(lang, "admin-help-example")));
    }

    // Getter methods for NewsManager
    public FileConfiguration getNewsConfig() {
        return newsConfig;
    }

    public File getNewsFile() {
        return newsFile;
    }

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }

    public NewsManager getNewsManager() {
        return newsManager;
    }

    /**
     * 解析添加命令的结果类
     */
    private static class ParsedAddCommand {
        public final String title;
        public final String content;
        public final String url;
        public final String command;
        public final String hover;

        public ParsedAddCommand(String title, String content, String url, String command, String hover) {
            this.title = title;
            this.content = content;
            this.url = url;
            this.command = command;
            this.hover = hover;
        }
    }
}