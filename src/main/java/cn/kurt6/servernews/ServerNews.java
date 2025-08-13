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
                    task -> newsManager.cleanupReadHistory(),
                    20L * 60 * 60, // 初始延迟1小时
                    20L * 60 * 60 * 6 // 每6小时执行一次
            );
        } catch (ClassNotFoundException e) {
            // 传统Bukkit服务器
            getLogger().info("Using traditional Bukkit scheduler");
            Bukkit.getScheduler().runTaskTimerAsynchronously(this,
                    () -> newsManager.cleanupReadHistory(),
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

        // 使用 Folia 兼容的方式调度任务
        if (isFolia()) {
            // Folia 使用 EntityScheduler
            player.getScheduler().runDelayed(this, task -> {
                if (player.isOnline() && newsManager.hasUnreadNews(player)) {
                    String playerLang = getPlayerLanguage(player);
                    sendNewsNotification(player, playerLang);
                }
            }, null, getConfig().getInt("auto-notification.delay-ticks", 60));
        } else {
            // 传统 Bukkit 使用全局调度器
            Bukkit.getScheduler().runTaskLater(this, () -> {
                if (player.isOnline() && newsManager.hasUnreadNews(player)) {
                    String playerLang = getPlayerLanguage(player);
                    sendNewsNotification(player, playerLang);
                }
            }, getConfig().getInt("auto-notification.delay-ticks", 60));
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

                return completions;
            } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
                // remove命令的索引补全
                List<Map<String, Object>> newsList = newsManager.getNewsList();
                List<String> indices = new ArrayList<>();
                for (int i = 0; i < newsList.size(); i++) {
                    indices.add(String.valueOf(i));
                }
                return indices;
            }
        }
        return null; // 返回null让Bukkit处理默认补全
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
                String message = messagesConfig.getString("messages." + lang + ".no-permission",
                        messagesConfig.getString("messages.zh.no-permission"));
                sender.sendMessage(parseColoredText(message));
                return true;
            }

            if (args.length == 0) {
                sendAdminHelp(sender);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "reload":
                    reloadConfigs();
                    String lang = sender instanceof Player ? getPlayerLanguage((Player) sender) : "zh";
                    String message = messagesConfig.getString("messages." + lang + ".config-reloaded",
                            messagesConfig.getString("messages.zh.config-reloaded"));
                    sender.sendMessage(parseColoredText(message));
                    break;

                case "add":
                    if (args.length < 3) {
                        sender.sendMessage("§cUsage: /newsadmin add <title> <content>");
                        return true;
                    }
                    String title = args[1];
                    String content = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

                    if (newsManager.validateNews(title, content)) {
                        if (newsManager.addNews(title, content, null, null, null)) {
                            sender.sendMessage("§aNews added successfully!");
                        } else {
                            sender.sendMessage("§cFailed to add news!");
                        }
                    } else {
                        sender.sendMessage("§cInvalid news format! Check title and content length.");
                    }
                    break;

                case "remove":
                    if (args.length < 2) {
                        sender.sendMessage("§cUsage: /newsadmin remove <index>");
                        return true;
                    }
                    try {
                        int index = Integer.parseInt(args[1]);
                        if (newsManager.removeNews(index)) {
                            sender.sendMessage("§aNews removed successfully!");
                        } else {
                            sender.sendMessage("§cInvalid news index!");
                        }
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cInvalid number format!");
                    }
                    break;

                case "list":
                    List<Map<String, Object>> newsList = newsManager.getNewsList();
                    if (newsList.isEmpty()) {
                        sender.sendMessage("§7No news available.");
                    } else {
                        sender.sendMessage("§6=== News List ===");
                        for (int i = 0; i < newsList.size(); i++) {
                            Map<String, Object> news = newsList.get(i);
                            sender.sendMessage("§a" + i + "§7: §e" + news.get("title") + " §7(" + news.get("date") + ")");
                        }
                    }
                    break;

                case "stats":
                    Map<String, Object> stats = newsManager.getNewsStats();
                    sender.sendMessage("§6=== News Statistics ===");
                    sender.sendMessage("§aTotal news: §e" + stats.get("total") + "§7/§e" + stats.get("maxAllowed"));
                    if (stats.containsKey("latestTitle")) {
                        sender.sendMessage("§aLatest: §e" + stats.get("latestTitle"));
                        sender.sendMessage("§aDate: §e" + stats.get("latestDate"));
                    }
                    sender.sendMessage("§aWith URL: §e" + stats.get("withUrl"));
                    sender.sendMessage("§aWith Command: §e" + stats.get("withCommand"));
                    break;

                default:
                    sendAdminHelp(sender);
                    break;
            }
            return true;
        }

        return false;
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
        sender.sendMessage("§6=== ServerNews Admin Commands ===");
        sender.sendMessage("§a/newsadmin reload §7- Reload configurations");
        sender.sendMessage("§a/newsadmin add <title> <content> §7- Add new news");
        sender.sendMessage("§a/newsadmin remove <index> §7- Remove news by index");
        sender.sendMessage("§a/newsadmin list §7- List all news with indices");
        sender.sendMessage("§a/newsadmin stats §7- Show news statistics");
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
}