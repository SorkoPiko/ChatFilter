package com.sorkopiko.chatFilter;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.entity.Player;
import java.util.List;
import java.util.ArrayList;

public class ChatFilterListener implements Listener {

    private final ChatFilter plugin;

    public ChatFilterListener(ChatFilter plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        if (plugin.isFilterChatEnabled() && plugin.censorable(event.getPlayer())) {
            String originalMessage = event.getMessage();
            String filteredMessage = plugin.replaceForbiddenWords(originalMessage);

            if (!originalMessage.equals(filteredMessage)) {
                event.setMessage(filteredMessage);
                plugin.punishPlayer(event.getPlayer(), originalMessage, "CHAT");
            }
        }
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        if (plugin.isFilterSignsEnabled() && plugin.censorable(event.getPlayer())) {
            boolean foundForbiddenText = false;
            StringBuilder originalLines = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                String line = event.getLine(i);
                if (line != null) {
                    originalLines.append("Line ").append(i + 1).append(": ").append(line).append(" | ");
                    String filteredLine = plugin.replaceForbiddenWords(line);
                    if (!line.equals(filteredLine)) {
                        event.setLine(i, filteredLine);
                        foundForbiddenText = true;
                    }
                }
            }
            if (foundForbiddenText) {
                plugin.punishPlayer(event.getPlayer(), originalLines.toString(), "SIGN");
            }
        }
    }

    @EventHandler
    public void onPlayerEditBook(PlayerEditBookEvent event) {
        if (plugin.isFilterBooksEnabled() && event.isSigning() && plugin.censorable(event.getPlayer())) {
            BookMeta bookMeta = event.getNewBookMeta();
            boolean foundForbiddenText = false;
            StringBuilder originalContent = new StringBuilder();

            // Filter pages
            List<String> pages = bookMeta.getPages();
            List<String> filteredPages = new ArrayList<>();

            for (int i = 0; i < pages.size(); i++) {
                String page = pages.get(i);
                originalContent.append("Page ").append(i + 1).append(": ").append(page).append(" | ");
                String filteredPage = plugin.replaceForbiddenWords(page);
                filteredPages.add(filteredPage);
                if (!page.equals(filteredPage)) {
                    foundForbiddenText = true;
                }
            }

            bookMeta.setPages(filteredPages);

            // Filter title
            if (bookMeta.hasTitle()) {
                String originalTitle = bookMeta.getTitle();
                originalContent.append("Title: ").append(originalTitle);
                String filteredTitle = plugin.replaceForbiddenWords(originalTitle);
                if (originalTitle != null && !originalTitle.equals(filteredTitle)) {
                    bookMeta.setTitle(filteredTitle);
                    foundForbiddenText = true;
                }
            }

            event.setNewBookMeta(bookMeta);

            if (foundForbiddenText) {
                plugin.punishPlayer(event.getPlayer(), originalContent.toString(), "BOOK");
            }
        }
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        Player player = event.getView().getPlayer() instanceof Player ? (Player) event.getView().getPlayer() : null;
        if (plugin.isFilterAnvilsEnabled() && plugin.censorable(player)) {
             ItemStack result = event.getResult();

            if (result != null && result.hasItemMeta()) {
                ItemMeta meta = result.getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    String originalName = meta.getDisplayName();
                    String filteredName = plugin.replaceForbiddenWords(originalName);
                    if (!originalName.equals(filteredName)) {
                        meta.setDisplayName(filteredName);
                        result.setItemMeta(meta);
                        event.setResult(result);
                        plugin.punishPlayer(player, originalName, "ANVIL");
                    }
                }
            }
        }
    }
}
