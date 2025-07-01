package com.sorkopiko.chatFilter;

import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ChatFilter extends JavaPlugin {

    private List<String> forbiddenWords;
    private List<Pattern> forbiddenRegex;
    private String warningMessage;
    private char censorChar;
    private boolean filterChat;
    private boolean filterSigns;
    private boolean filterBooks;
    private boolean filterAnvils;
    private boolean enableLogging;
    private File logFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();
        setupLogging();
        getServer().getPluginManager().registerEvents(new ChatFilterListener(this), this);

        ChatFilterCommand commandExecutor = new ChatFilterCommand(this);
        PluginCommand command = Objects.requireNonNull(getCommand("chatfilter"));
        command.setExecutor(commandExecutor);

        getLogger().info("ChatFilter has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("ChatFilter has been disabled!");
    }

    public void loadConfig() {
        reloadConfig();
        FileConfiguration config = getConfig();

        forbiddenWords = config.getStringList("forbidden-words");
        forbiddenRegex = new ArrayList<>();
        for (String regex : config.getStringList("forbidden-regex")) {
            try {
                forbiddenRegex.add(Pattern.compile(regex, Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException e) {
                getLogger().warning("Invalid regex pattern in config: " + regex + " - " + e.getMessage());
            }
        }
        warningMessage = config.getString("warning-message", "&cYou are not allowed to use that language!");
        censorChar = config.getString("censor-character", "*").charAt(0);
        filterChat = config.getBoolean("filter-chat", true);
        filterSigns = config.getBoolean("filter-signs", true);
        filterBooks = config.getBoolean("filter-books", true);
        filterAnvils = config.getBoolean("filter-anvils", true);
        enableLogging = config.getBoolean("enable-logging", true);
    }

    private void setupLogging() {
        if (enableLogging) {
            File dataFolder = getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            logFile = new File(dataFolder, "offenses.log");
        }
    }

    public void logOffense(Player player, String originalMessage, String context) {
        if (!enableLogging || logFile == null) {
            return;
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            String playerName = (player != null) ? player.getName() : "Unknown";
            String playerUuid = (player != null) ? player.getUniqueId().toString() : "Unknown";

            String logEntry = String.format("[%s] Player: %s (%s) | Context: %s | Message: %s%n",
                    now.format(formatter),
                    playerName,
                    playerUuid,
                    context,
                    originalMessage
            );

            try (FileWriter writer = new FileWriter(logFile, true)) {
                writer.write(logEntry);
            }
        } catch (IOException e) {
            getLogger().warning("Failed to write to offense log: " + e.getMessage());
        }
    }

    public boolean isTextForbidden(String text) {
        String lowerCaseText = text.toLowerCase();

        // Check forbidden words
        for (String word : forbiddenWords) {
            if (lowerCaseText.contains(word.toLowerCase())) {
                return true;
            }
        }

        // Check forbidden regex
        for (Pattern pattern : forbiddenRegex) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    public String replaceForbiddenWords(String text) {
        String result = text;

        // Replace forbidden words
        for (String word : forbiddenWords) {
            String lowerCaseWord = word.toLowerCase();
            String lowerCaseResult = result.toLowerCase();

            int startIndex = 0;
            while ((startIndex = lowerCaseResult.indexOf(lowerCaseWord, startIndex)) >= 0) {
                StringBuilder censored = new StringBuilder();
                censored.append(String.valueOf(censorChar).repeat(word.length()));

                result = result.substring(0, startIndex) + censored + result.substring(startIndex + word.length());
                lowerCaseResult = result.toLowerCase();
                startIndex += censored.length();
            }
        }

        // Replace forbidden regex
        for (Pattern pattern : forbiddenRegex) {
            java.util.regex.Matcher matcher = pattern.matcher(result);
            StringBuilder sb = new StringBuilder();

            while (matcher.find()) {
                String match = matcher.group();
                matcher.appendReplacement(sb, String.valueOf(censorChar).repeat(match.length()).replace("$", "\\$"));
            }
            matcher.appendTail(sb);
            result = sb.toString();
        }

        return result;
    }

    public void punishPlayer(Player player, String originalMessage) {
        if (player != null && player.isOnline()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', warningMessage));
        }
    }

    public void punishPlayer(Player player, String originalMessage, String context) {
        punishPlayer(player, originalMessage);
        logOffense(player, originalMessage, context);
    }

    public boolean isFilterChatEnabled() {
        return filterChat;
    }

    public boolean isFilterSignsEnabled() {
        return filterSigns;
    }

    public boolean isFilterBooksEnabled() {
        return filterBooks;
    }

    public boolean isFilterAnvilsEnabled() {
        return filterAnvils;
    }

    public boolean censorable(Player player) {
        if (player == null || !player.isOnline()) {
            return false;
        }

        return !player.hasPermission("chatfilter.bypass");
    }
}
