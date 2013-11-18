package me.libraryaddict.Currency;

import org.bukkit.entity.Player;

public class Api {
    private CurrencyMain main;

    protected Api(CurrencyMain cur) {
        this.main = cur;
    }

    public void pay(String sender, String receiver, int amount, boolean message) {
        if (sender != null) {
            withdraw(sender, amount);
        }

        if (!main.balance.contains(receiver)) main.balance.put(receiver, 0);
        main.balance.put(receiver, main.balance.get(receiver) + amount);
        main.transfers.add(new Transfer(sender, receiver, amount, message ? Transfer.MONEY_GIVE : Transfer.SILENT));
    }

    public void pay(String player, int amount, boolean message) {
        pay(null, player, amount, message);
    }

    public void withdraw(String player, int amount) {
        if (!main.balance.contains(player)) main.balance.put(player, 0);
        main.balance.put(player, main.balance.get(player) - amount);
        main.transfers.add(new Transfer(player, null, amount, Transfer.SILENT));
    }

    public void refresh(String player) {
        main.refreshers.add(player);
    }

    public boolean canAfford(Player player, int amount) {
        if (!main.balance.contains(player)) main.balance.put(player.getName(), 0);
        return player.isOp() || (main.balance.get(player.getName()) - amount) > 0;
    }

}