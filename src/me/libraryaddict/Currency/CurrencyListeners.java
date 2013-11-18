package me.libraryaddict.Currency;

import java.util.List;

import me.libraryaddict.Hungergames.HungergamesApi;
import me.libraryaddict.Hungergames.Events.PlayerKilledEvent;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.google.common.collect.Lists;

public class CurrencyListeners implements Listener {
    CurrencyMain main;

    // IrcApi irc = new IrcApi();

    public CurrencyListeners(CurrencyMain currencyMain) {
        this.main = currencyMain;
    }

    @EventHandler
    public void onBlockSmash(BlockBreakEvent event) {
        if (isSign(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler
    public void onKill(PlayerKilledEvent event) {
        if (event.getKiller() == null) return;
        CurrencyMain.api.pay(event.getKiller().getName(), (int) Math.ceil(HungergamesApi.hgInstance.currentTime * 1000 / 1800), false);
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent event) {
        List<Block> remove = Lists.newArrayList();
        for (Block block : event.blockList())
            if (isSign(block)) remove.add(block);
        event.blockList().removeAll(remove);
    }

    @EventHandler
    public void onBurn(BlockBurnEvent event) {
        if (isSign(event.getBlock())) event.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.isCancelled()) return;
        if ((event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) && (event.getClickedBlock().getType() == Material.WALL_SIGN)) {
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign) event.getClickedBlock().getState();

            if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(sign.getLine(0))) {
                event.getPlayer().sendMessage(this.main.signPurchase(event.getPlayer(), sign));
                event.setCancelled(true);
            }
        }
    }

    boolean isSign(Block block) {
        if (block.getType() == Material.WALL_SIGN) {
            org.bukkit.block.Sign sign = (org.bukkit.block.Sign) block.getState();

            if (!ChatColor.stripColor(sign.getLine(0)).equalsIgnoreCase(sign.getLine(0))) {
                return true;
            }
        }
        return false;
    }

    public static BlockFace signFacing(org.bukkit.block.Sign sign) {
        org.bukkit.material.Sign signData = (org.bukkit.material.Sign) sign.getData();
        return signData.getFacing();
    }

    @EventHandler
    public void blockPhysics(BlockPhysicsEvent event) {
        if (isSign(event.getBlock())) event.setCancelled(true);
    }

    // @EventHandler
    // public void onChat(IrcMessageEvent event)
    // {
    // User user = this.irc.getUser(event.getChannel(), event.getNick());
    // if ((user.getRank().intValue() > 1) &&
    // (event.getChannel().equals("#ServerChat")))
    // {
    // String[] msg = event.getMessage().split(":");
    //
    // if ((msg[0].equals(Bukkit.getMotd().replaceAll(" ",
    // "").replaceAll("\\d*$", ""))) && (msg[1].equals("Voted")) && (msg.length
    // == 5))
    // {
    // int amount = Integer.parseInt(msg[3]);
    // String name = msg[2];
    // if (Bukkit.getPlayerExact(msg[2]) != null)
    // {
    // if
    // (Bukkit.getPlayerExact(msg[2]).getName().toLowerCase().equals(msg[2].toLowerCase()))
    // {
    // name = Bukkit.getPlayerExact(msg[2]).getName();
    // CurrencyMain.refreshers.add(name);
    // CurrencyMain.balance.put(name, Integer.valueOf(((Integer)
    // CurrencyMain.balance.get(name)).intValue() + amount));
    // }
    // }
    // Bukkit.broadcastMessage(ChatColor.GREEN + msg[4].replace("$name",
    // name).replace("$money", msg[3]));
    // }
    // }
    // }
}