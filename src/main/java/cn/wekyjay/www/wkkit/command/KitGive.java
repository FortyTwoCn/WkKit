package cn.wekyjay.www.wkkit.command;

import cn.wekyjay.www.wkkit.WkKit;
import cn.wekyjay.www.wkkit.api.PlayersReceiveKitEvent;
import cn.wekyjay.www.wkkit.api.ReceiveType;
import cn.wekyjay.www.wkkit.config.LangConfigLoader;
import cn.wekyjay.www.wkkit.kit.Kit;
import cn.wekyjay.www.wkkit.kit.KitGetter;
import cn.wekyjay.www.wkkit.tool.WKTool;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class KitGive {
    static WkKit wk = WkKit.getWkKit();

    public Boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(args.length < 3) {
            sender.sendMessage(LangConfigLoader.getStringWithPrefix("Commands.give", ChatColor.GREEN));
            return true;
        }

        String kitname = args[1];
        Kit kit = Kit.getKit(kitname);
        if(kit == null) {
            sender.sendMessage(LangConfigLoader.getStringWithPrefix("KIT_NOT_FOUND", ChatColor.RED));
            return true;
        }

        String target = args[2];
        if(target.equalsIgnoreCase("@Me")) {
            if(!(sender instanceof Player)) {
                sender.sendMessage(LangConfigLoader.getStringWithPrefix("COMMAND_PLAYER_ONLY", ChatColor.RED));
                return true;
            }
            Player p = (Player)sender;
            this.ExcutionMode(sender, p, kit, args.length >= 4 ? args[3] : "1");
        } else {
            // 尝试获取在线玩家
            Player p = Bukkit.getPlayer(target);
            if(p != null && p.isOnline()) {
                // 玩家在线，尝试直接给予
                this.ExcutionMode(sender, p, kit, args.length >= 4 ? args[3] : "1");
            } else {
                // 玩家不在线，转为发送礼包
                sendKitToOfflinePlayer(sender, target, kit, args.length >= 4 ? args[3] : "1");
            }
        }
        return true;
    }

    public void ExcutionMode(CommandSender sender, Player player, Kit kit, String mode) {
        try {
            PlayerInventory pinv = player.getInventory();
            ItemStack[] getItemList = kit.getItemStacks();
            
            switch(mode) {
                case "2":
                    if(!WKTool.hasSpace(player, kit)) {
                        // 背包空间不足，转为发送礼包
                        sendKitToOfflinePlayer(sender, player.getName(), kit, mode);
                        return;
                    }
                    if(PlayersReceiveKitEvent.callEvent(player, kit, ReceiveType.GIVE).isCancelled()) return;
                    WKTool.addItem(player, getItemList);
                    if(kit.getCommands() != null) new KitGetter().runCommands(kit, player);
                    break;
                case "3":
                    if(!WKTool.hasSpace(player, 1)) {
                        // 背包空间不足，转为发送礼包
                        sendKitToOfflinePlayer(sender, player.getName(), kit, mode);
                        return;
                    }
                    if(PlayersReceiveKitEvent.callEvent(player, kit, ReceiveType.GIVE).isCancelled()) return;
                    pinv.addItem(kit.getKitItem());
                    break;
                case "4":
                    if(!WKTool.hasSpace(player, kit)) {
                        // 背包空间不足，转为发送礼包
                        sendKitToOfflinePlayer(sender, player.getName(), kit, mode);
                        return;
                    }
                    if(PlayersReceiveKitEvent.callEvent(player, kit, ReceiveType.GIVE).isCancelled()) return;
                    WKTool.addItem(player, getItemList);
                    if(kit.getMythicMobs() != null) new KitGetter().runMythicMobs(kit, player);
                    break;
                default:
                    if(!WKTool.hasSpace(player, kit)) {
                        // 背包空间不足，转为发送礼包
                        sendKitToOfflinePlayer(sender, player.getName(), kit, mode);
                        return;
                    }
                    if(PlayersReceiveKitEvent.callEvent(player, kit, ReceiveType.GIVE).isCancelled()) return;
                    WKTool.addItem(player, getItemList);
            }
            sender.sendMessage(LangConfigLoader.getStringWithPrefix("KIT_GIVE_SUCCESS", ChatColor.GREEN));
        } catch (Exception e) {
            // 如果执行过程中出错，转为发送礼包
            sendKitToOfflinePlayer(sender, player.getName(), kit, mode);
        }
    }

    private void sendKitToOfflinePlayer(CommandSender sender, String playerName, Kit kit, String mode) {
        OfflinePlayer offlinePlayer = Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(p -> p.getName() != null && p.getName().equalsIgnoreCase(playerName))
                .findFirst()
                .orElse(null);

        if(offlinePlayer == null) {
            sender.sendMessage(LangConfigLoader.getStringWithPrefix("NO_PLAYER", ChatColor.RED));
            return;
        }

        String pname = offlinePlayer.getName();
        
        // 回调事件
        if(PlayersReceiveKitEvent.callEvent(offlinePlayer.getPlayer(), pname, kit, ReceiveType.SEND).isCancelled()) {
            return;
        }

        // 存储到玩家数据
        if(WkKit.getPlayerData().contain_Mail(pname, kit.getName())) {
            int num = WkKit.getPlayerData().getMailKitNum(pname, kit.getName());
            WkKit.getPlayerData().setMailNum(pname, kit.getName(), num + 1);
        } else {
            WkKit.getPlayerData().setMailNum(pname, kit.getName(), 1);
        }

        // 发送通知
        sender.sendMessage(LangConfigLoader.getStringWithPrefix("KIT_SEND_PLAYER", ChatColor.GREEN));
        if(offlinePlayer.isOnline()) {
            offlinePlayer.getPlayer().sendMessage(LangConfigLoader.getStringWithPrefix("KIT_SEND_PICKUP", ChatColor.GREEN));
        }
    }
}
