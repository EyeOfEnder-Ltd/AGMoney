package me.libraryaddict.Currency;

import org.bukkit.entity.Player;

public class Api {
    private CurrencyMain main;

    protected Api(CurrencyMain cur) {
        this.main = cur;
    }

    public void pay(String player, int amount, boolean message) {
        if (main.balance.contains(player)) main.balance.put(player, main.balance.get(player) - amount);
        if (message) main.transfers.add(new Transfer(null, player, Math.abs(amount), Transfer.MONEY_GIVE));
        else
            main.transfers.add(new Transfer(null, player, Math.abs(amount), Transfer.SILENT));
    }

    public void pay(String sender, String receiver, int amount, boolean message) {
        if (main.balance.contains(sender)) main.balance.put(sender, main.balance.get(sender) - amount);
        if (message) main.transfers.add(new Transfer(sender, receiver, Math.abs(amount), Transfer.PLAYER_PAYMENT));
        else
            main.transfers.add(new Transfer(sender, receiver, Math.abs(amount), Transfer.SILENT));
    }

    public void withdraw(String player, int amount) {
        if (main.balance.contains(player)) main.balance.put(player, main.balance.get(player) - amount);
        main.transfers.add(new Transfer(player, null, Math.abs(amount), Transfer.SILENT));
    }

    public void refresh(String player) {
        main.refreshers.add(player);
    }

    public boolean canAfford(Player player, int amount) {
        return player.isOp() || (main.balance.get(player.getName()) - amount) > 0;
    }

}