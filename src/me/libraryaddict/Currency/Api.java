package me.libraryaddict.Currency;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Api
{
	CurrencyMain main;
	
	Api(CurrencyMain cur)
	{
		this.main = cur;
	}
	
	public void pay(String player, int amount, boolean message)
	{
		if (CurrencyMain.balance.contains(player))
			CurrencyMain.balance.put(player, Integer.valueOf(((Integer) CurrencyMain.balance.get(player)).intValue() - amount));
		if (message)
			CurrencyMain.transfers.add(new Transfer(null, player, Math.abs(amount), Transfer.MONEY_GIVE));
		else
			CurrencyMain.transfers.add(new Transfer(null, player, Math.abs(amount), Transfer.SILENT));
	}
	
	public void pay(String sender, String receiver, int amount, boolean message)
	{
		if (CurrencyMain.balance.contains(sender))
			CurrencyMain.balance.put(sender, Integer.valueOf(((Integer) CurrencyMain.balance.get(sender)).intValue() - amount));
		if (message)
			CurrencyMain.transfers.add(new Transfer(sender, receiver, Math.abs(amount), Transfer.PLAYER_PAYMENT));
		else
			CurrencyMain.transfers.add(new Transfer(sender, receiver, Math.abs(amount), Transfer.SILENT));
	}
	
	public void withdraw(String player, int amount)
	{
		if (CurrencyMain.balance.contains(player))
			CurrencyMain.balance.put(player, Integer.valueOf(((Integer) CurrencyMain.balance.get(player)).intValue() - amount));
		CurrencyMain.transfers.add(new Transfer(player, null, Math.abs(amount), Transfer.SILENT));
	}
	
	public void refresh(String player)
	{
		CurrencyMain.refreshers.add(player);
	}
}