package com.kitnef;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.inventory.InventoryType;
import cn.nukkit.inventory.Item;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import cn.nukkit.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class OfflineRewardPlugin extends PluginBase implements Listener {

    private Config config;
    private final HashMap<String, Inventory> playerInventories = new HashMap<>();
    private final Random random = new Random();

    @Override
    public void onEnable() {
        // Load config and set default values if not already set
        this.saveDefaultConfig();
        config = this.getConfig();

        // Register events
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        int totalSecondsOffline = (int) (System.currentTimeMillis() - player.getLastLogin()) / 1000;

        // Kiểm tra quyền cấp độ của người chơi và nhận phần thưởng
        for (int level = 1; level <= 5; level++) {
            if (player.hasPermission("reward.level" + level)) {
                int timePerItem = config.getInt("time_per_level.level" + level);
                int rewardsReceived = totalSecondsOffline / timePerItem;

                // Cấp phần thưởng dựa trên số lần nhận vật phẩm
                for (int i = 0; i < rewardsReceived; i++) {
                    grantReward(player);
                }

                // Lấy thông báo từ config và thay thế các tham số
                String message = config.getString("message")
                        .replace("{days}", String.valueOf(totalSecondsOffline / 86400))
                        .replace("{hours}", String.valueOf((totalSecondsOffline % 86400) / 3600))
                        .replace("{minutes}", String.valueOf((totalSecondsOffline % 3600) / 60))
                        .replace("{rewards}", String.valueOf(rewardsReceived));

                // Gửi thông báo với màu sắc và ký tự đặc biệt
                player.sendMessage(TextFormat.colorize(message));
                break;
            }
        }
    }

    private void grantReward(Player player) {
        // Tỷ lệ nhận vật phẩm hoặc lệnh
        boolean isItemReward = random.nextInt(100) < getItemChance();

        // Chạy grantItem hoặc grantCommand dựa trên xác suất
        if (isItemReward) {
            grantRandomItem(player);
        } else {
            grantRandomCommand(player);
        }
    }

    private void grantRandomItem(Player player) {
        // Lấy danh sách các vật phẩm từ cấu hình
        List<?> itemList = config.getList("items");
        int totalChance = 0;
        for (Object obj : itemList) {
            Config itemConfig = (Config) obj;
            totalChance += itemConfig.getInt("chance");
        }

        // Tính xác suất ngẫu nhiên để chọn vật phẩm
        int randomChance = random.nextInt(totalChance);
        int accumulatedChance = 0;
        for (Object obj : itemList) {
            Config itemConfig = (Config) obj;
            accumulatedChance += itemConfig.getInt("chance");
            if (randomChance < accumulatedChance) {
                int itemId = itemConfig.getInt("id");
                int amount = itemConfig.getInt("amount");

                // Kho đồ ảo của người chơi
                Inventory virtualInventory = getPlayerInventory(player);

                // Tạo vật phẩm và cho vào kho đồ ảo
                Item item = Item.get(itemId);
                item.setCount(amount);
                virtualInventory.addItem(item);
                break;
            }
        }
    }

    private void grantRandomCommand(Player player) {
        // Lấy danh sách các lệnh từ cấu hình
        List<?> commandList = config.getList("commands");
        int totalChance = 0;
        for (Object obj : commandList) {
            Config commandConfig = (Config) obj;
            totalChance += commandConfig.getInt("chance");
        }

        // Tính xác suất ngẫu nhiên để chọn lệnh
        int randomChance = random.nextInt(totalChance);
        int accumulatedChance = 0;
        for (Object obj : commandList) {
            Config commandConfig = (Config) obj;
            accumulatedChance += commandConfig.getInt("chance");
            if (randomChance < accumulatedChance) {
                String command = commandConfig.getString("command");
                int repeatCount = commandConfig.getInt("repeat");

                // Lặp lại việc thực thi lệnh theo số lần quy định
                for (int i = 0; i < repeatCount; i++) {
                    getServer().dispatchCommand(getServer().getConsoleSender(), command.replace("{player}", player.getName()));
                }
                break;
            }
        }
    }

    private Inventory getPlayerInventory(Player player) {
        // Kiểm tra nếu người chơi chưa có kho đồ ảo thì tạo mới
        return playerInventories.computeIfAbsent(player.getName(), k -> getServer().getInventoryManager().getInventory(InventoryType.HOPPER));
    }

    private int getItemChance() {
        return 50;  // Cứ cho tỉ lệ vật phẩm là 50% (có thể thay đổi sau này)
    }
}
