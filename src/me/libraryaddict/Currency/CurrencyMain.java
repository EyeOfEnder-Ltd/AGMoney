package me.libraryaddict.Currency;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.Lists;

public class CurrencyMain extends JavaPlugin implements Listener {
    public ConcurrentLinkedQueue<Transfer> transfers = new ConcurrentLinkedQueue<Transfer>();
    public ConcurrentLinkedQueue<String> refreshers = new ConcurrentLinkedQueue<String>();
    public ConcurrentHashMap<String, Integer> balance = new ConcurrentHashMap<String, Integer>();
    MoneyLoadThread loadThread = new MoneyLoadThread(this);
    MoneySaveThread saveThread = new MoneySaveThread(this);
    public String SQL_USER;
    public String SQL_PASS;
    public String SQL_DATA;
    public String SQL_HOST;
    public static CurrencyMain instance;
    ItemDb data;
    Enchantments enchants = new Enchantments();
    public static Api api;

    private Config config;

    public static synchronized void write(String arg) {
    }

    public synchronized ArrayList<String> getLogs(String key) {
        ArrayList<String> lines = Lists.newArrayList();
        try {
            FileInputStream fstream = new FileInputStream("Money.log");

            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                if (strLine.toLowerCase().contains(key.toLowerCase())) {
                    lines.add(strLine);
                }
            }
            in.close();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        return lines;
    }

    public void onEnable() {

        saveDefaultConfig();
        instance = this;
        api = new Api(this);
        this.data = new ItemDb();
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new CurrencyListeners(this), this);
        config = new Config(this);
        SQL_USER = config.getUsername();
        SQL_PASS = config.getPassword();
        SQL_HOST = config.getHost();
        SQL_DATA = config.getDatabase();
        this.loadThread.start();
        this.saveThread.start();
    }

    public void onDisable() {
        this.loadThread.stop();
        while (transfers.size() > 0)
            try {
                System.out.println("Waiting for saveQueue, " + transfers.size() + " left! Save thread dead: " + (!this.saveThread.isAlive()));
                Thread.currentThread();
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        this.saveThread.stop();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        balance.put(p.getName(), Integer.valueOf(0));
        refreshers.add(p.getName());
        if (!this.loadThread.isAlive()) System.out.println("Load thread is dead");
        if (!this.saveThread.isAlive()) System.out.println("Save thread is dead");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        balance.remove(p.getName());
    }

    void transferComplete(Transfer transfer) {
        if (transfer.getType() == null) return;
        if (transfer.getType().equals(Transfer.PLAYER_PAYMENT)) {
            Player sender = Bukkit.getPlayerExact(transfer.getSender());
            Player receiver = Bukkit.getPlayerExact(transfer.getReceiver());
            if (transfer.getStatus()) {
                String senderName = transfer.getSender();
                String receiverName = transfer.getReceiver();
                if (sender != null) senderName = sender.getDisplayName();
                if (receiver != null) receiverName = receiver.getDisplayName();
                if (transfer.getStatus()) {
                    if (sender != null) sender.sendMessage(ChatColor.GREEN + "$" + transfer.getAmount() + " has been sent to " + receiverName);
                    if (receiver != null) receiver.sendMessage(ChatColor.GREEN + "$" + transfer.getAmount() + " has been received from " + senderName);
                } else if (sender != null) {
                    sender.sendMessage(ChatColor.GREEN + "Your money transfer of " + transfer.getAmount() + " to " + transfer.getReceiver() + " failed! Reason: " + transfer.getError());
                }
            }
        } else {
            if (transfer.getType().equalsIgnoreCase(Transfer.SIGN_PURCHASE)) return;
            if ((transfer.getType().equalsIgnoreCase(Transfer.MONEY_GIVE)) && (transfer.getReceiver() != null)) {
                Player p = Bukkit.getPlayerExact(transfer.getReceiver());
                if (p == null) return;
                if (transfer.getStatus()) p.sendMessage(ChatColor.GREEN + "You were given $" + transfer.getAmount());
            }
        }
    }

    boolean canFit(Player p, ItemStack[] items) {
        Inventory inv = Bukkit.createInventory(null, p.getInventory().getContents().length);
        for (int i = 0; i < inv.getSize(); i++)
            if ((p.getInventory().getItem(i) != null) && (p.getInventory().getItem(i).getType() != Material.AIR)) {
                inv.setItem(i, p.getInventory().getItem(i).clone());
            }
        for (ItemStack i : items) {
            HashMap<Integer, ItemStack> item = inv.addItem(new ItemStack[] { i });
            if ((item != null) && (!item.isEmpty())) {
                return false;
            }
        }
        return true;
    }

    String signPurchase(Player p, Sign sign) {
        if (ChatColor.stripColor(sign.getLine(0)).equals("[Buy]")) {
            int amount = Integer.parseInt(sign.getLine(1));
            ItemStack item = null;
            try {
                item = this.data.get(sign.getLine(2).toLowerCase());
            } catch (IndexOutOfBoundsException e) {
                System.out.println("unknownItemName");
            } catch (Exception e) {
                System.out.println("unknownItemName");
            }
            if (item == null) return ChatColor.RED + "Error, Can't fetch item from database";
            int price = Integer.parseInt(sign.getLine(3).substring(1));
            item.setAmount(amount);
            if (!api.canAfford(p, price)) return ChatColor.RED + "Can't afford to purchase this item!";
            if (canFit(p, new ItemStack[] { item })) {
                item.setAmount(1);
                for (int i = 0; i < amount; i++)
                    p.getInventory().addItem(new ItemStack[] { item });
                balance.put(p.getName(), Integer.valueOf(((Integer) balance.get(p.getName())).intValue() - price));
                transfers.add(new Transfer(p.getName(), null, price, Transfer.SIGN_PURCHASE));
                p.updateInventory();
                return ChatColor.RED + "Item purchased for $" + price;
            }
            return ChatColor.RED + "Can't fit in inventory";
        }
        if ((ChatColor.stripColor(sign.getLine(0)).equals("[Enchant]")) && (sign.getLine(1).equals("Any"))) {
            int price = Integer.parseInt(sign.getLine(3).substring(1));
            String[] wording = sign.getLine(2).split(":");
            if (wording.length > 0) {
                Enchantment enchant = Enchantments.getByName(wording[0]);
                if (enchant == null) return ChatColor.RED + "Bad enchantment detected, Please notify a admin";
                int level = 1;
                if (wording.length > 1) level = Integer.parseInt(wording[1]);
                ItemStack item = p.getItemInHand();
                if ((item == null) || (item.getType() == Material.AIR) || (!enchant.canEnchantItem(item))) {
                    return ChatColor.RED + "Can't enchant item in hand!";
                }
                if (!api.canAfford(p, price)) return ChatColor.RED + "Can't afford enchantment!";
                if ((item.containsEnchantment(enchant)) && (item.getEnchantmentLevel(enchant) <= level)) return ChatColor.RED + "Item is already enchanted!";
                balance.put(p.getName(), Integer.valueOf(((Integer) balance.get(p.getName())).intValue() - price));
                transfers.add(new Transfer(p.getName(), null, price, Transfer.SIGN_PURCHASE));
                item.addUnsafeEnchantment(enchant, level);
                p.updateInventory();
                return ChatColor.RED + "Item enchanted for $" + price;
            }
            return ChatColor.RED + "Unable to enchant";
        }

        return ChatColor.LIGHT_PURPLE + "Money plugin failed somewhere..";
    }

    boolean getBalance(CommandSender sender, String[] args) {
        Player p = Bukkit.getPlayerExact(sender.getName());
        if (args.length > 0) {
            Player p1 = Bukkit.getPlayer(args[0]);
            if (p1 == null) {
                sender.sendMessage(ChatColor.RED + "Error: " + ChatColor.DARK_RED + "Player not found");
                return true;
            }
            sender.sendMessage(ChatColor.GREEN + p1.getName() + "'s money: " + ChatColor.RED + "$" + balance.get(p1.getName()));
            return true;
        }
        if (p == null) {
            sender.sendMessage(ChatColor.GREEN + "You must be in the game");
            return true;
        }
        p.sendMessage(ChatColor.GREEN + "Money: " + ChatColor.RED + "$" + balance.get(p.getName()));
        return true;
    }

    void pay(CommandSender sender, String[] args) {
        if (args.length > 1) {
            int money = 0;
            if (isNumeric(args[1])) {
                money = Integer.parseInt(args[1]);
            } else {
                sender.sendMessage(ChatColor.GREEN + "That is not a number");
                return;
            }
            if (money <= 0) {
                sender.sendMessage(ChatColor.GREEN + "You must give a amount more then $" + money + "!");
                return;
            }
            Player receiver = Bukkit.getPlayer(args[0]);
            Player p = Bukkit.getPlayerExact(sender.getName());
            if ((p != null) && (!p.isOp())) money = Math.abs(money);
            if (receiver != null) {
                if ((p != null) && (!api.canAfford(p, money))) {
                    sender.sendMessage(ChatColor.GREEN + "You can't afford to pay them $" + money);
                    return;
                }
                api.pay(sender.getName(), receiver.getName(), money, true);
                return;
            }
            if (receiver == null) sender.sendMessage(ChatColor.GREEN + "Player not found");
        }
        sender.sendMessage(ChatColor.GREEN + "Pays another player from your balance");
        sender.sendMessage(ChatColor.GREEN + "/pay <player> <amount>");
    }

    public boolean isNumeric(String str) {
        try {
            Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        if (cmd.getName().equalsIgnoreCase("bal")) {
            getBalance(sender, args);
        } else if (cmd.getName().equalsIgnoreCase("pay")) {
            pay(sender, args);
        } else if ((cmd.getName().equalsIgnoreCase("moneylog")) && (sender.isOp())) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.LIGHT_PURPLE + "You need to use a arguement. Or its gonna fucking spam");
                return true;
            }
            ArrayList<String> lines = getLogs(StringUtils.join(args, " "));
            if (lines.size() == 0) sender.sendMessage(ChatColor.LIGHT_PURPLE + "No money logs found for '" + StringUtils.join(args, " ") + "'");
            else {
                for (int i = lines.size() - 1; (i >= 0) && (i >= lines.size() - 20); i--)
                    sender.sendMessage((String) lines.get(i));
            }
        } else if (cmd.getName().equalsIgnoreCase("setbal")) {
            if (!sender.isOp()) return false;
            if (args.length < 2) {
                sender.sendMessage(ChatColor.RED + "Usage: /setbal <player> <balance>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);

            if (target == null) {
                sender.sendMessage(ChatColor.RED + "Invalid player.");
                return true;
            }

            int money = 0;
            if (isNumeric(args[1])) {
                money = Integer.parseInt(args[1]);
            } else {
                sender.sendMessage(ChatColor.RED + "That is not a number");
                return true;
            }

            int diff = money - balance.get(target.getName());

            balance.put(target.getName(), money);
            transfers.add(new Transfer(null, target.getName(), diff, Transfer.MONEY_SET));
            sender.sendMessage(ChatColor.GREEN + "Set " + target.getDisplayName() + "'s balance to $" + money + ".");
        } else if (cmd.getName().equalsIgnoreCase("money")) {
            if (args.length == 0) {
                getBalance(sender, args);
                return true;
            }
            String[] newArgs = (String[]) Arrays.copyOfRange(args, 1, args.length);
            if ((args[0].equalsIgnoreCase("bal")) || (args[0].equalsIgnoreCase("balance"))) {
                getBalance(sender, newArgs);
            } else if (args[0].equalsIgnoreCase("pay")) {
                pay(sender, newArgs);
            } else if ((args[0].equalsIgnoreCase("give")) && (args.length > 2)) {
                Player player = Bukkit.getPlayer(args[0]);
                String name = args[1];
                if (player != null) name = player.getName();
                if (!isNumeric(args[2])) {
                    sender.sendMessage(ChatColor.RED + "Not a number!");
                    return true;
                }
                if (sender.isOp()) api.pay(name, Integer.parseInt(args[2]), true);
                else {
                    sender.sendMessage(ChatColor.GREEN + "You are not permitted to use this");
                }
            }
        }
        return true;
    }
}