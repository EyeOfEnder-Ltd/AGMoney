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

        if (!main.balance.contains(receiver)) refresh(receiver);
        main.balance.put(receiver, main.balance.get(receiver) + amount);
        main.transfers.add(new Transfer(sender, receiver, amount, message ? Transfer.MONEY_GIVE : Transfer.SILENT));
    }

    public void pay(String player, int amount, boolean message) {
        pay(null, player, amount, message);
    }

    public void withdraw(String player, int amount) {
        if (!main.balance.contains(player)) refresh(player);
        main.balance.put(player, main.balance.get(player) - amount);
        main.transfers.add(new Transfer(null, player, -amount, Transfer.SILENT));
    }

    public void refresh(String player) {
        main.balance.put(player, 0);
        main.loadThread.refresh(player);
    }

    public boolean canAfford(Player player, int amount) {
        if (!main.balance.contains(player.getName())) refresh(player.getName());
        return main.balance.get(player.getName()) - amount > 0;
    }

}