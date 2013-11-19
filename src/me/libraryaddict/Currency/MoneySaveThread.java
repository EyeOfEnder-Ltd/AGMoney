package me.libraryaddict.Currency;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.Bukkit;

public class MoneySaveThread extends Thread {
    CurrencyMain plugin;
    public Connection con = null;
    private boolean running;

    public MoneySaveThread(CurrencyMain Money) {
        this.plugin = Money;
    }

    public void SQLdisconnect() {
        try {
            System.out.println("[MoneySaveThread] Disconnecting from MySQL database...");
            this.con.close();
        } catch (SQLException ex) {
            System.err.println("[MoneySaveThread] Error while closing the connection...");
        } catch (NullPointerException ex) {
            System.err.println("[MoneySaveThread] Error while closing the connection...");
        }
    }

    public void SQLconnect() {
        try {
            System.out.println("[MoneySaveThread] Connecting to MySQL database...");
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            String conn = "jdbc:mysql://" + this.plugin.SQL_HOST + "/" + this.plugin.SQL_DATA;
            this.con = DriverManager.getConnection(conn, this.plugin.SQL_USER, this.plugin.SQL_PASS);
        } catch (ClassNotFoundException ex) {
            System.err.println("[MoneySaveThread] No MySQL driver found!");
        } catch (SQLException ex) {
            System.err.println("[MoneySaveThread] Error while fetching MySQL connection!");
        } catch (Exception ex) {
            System.err.println("[MoneySaveThread] Unknown error while fetchting MySQL connection.");
        }
    }

    public void terminate() {
        running = false;
    }

    public void run() {
        System.out.println("Money Save Thread started");
        SQLconnect();
        running = true;
        while (running) {
            if (plugin.transfers.peek() != null) {
                final Transfer transfer = plugin.transfers.poll();
                String[] name = { transfer.getSender(), transfer.getReceiver() };
                Integer[] amount = { Integer.valueOf(-transfer.getAmount()), Integer.valueOf(transfer.getAmount()) };
                transfer.setStatus(true);
                for (int n = 0; n < 2; n++)
                    if (name[n] != null) {
                        String statement = "";
                        try {
                            Statement stamt = this.con.createStatement();
                            statement = "SELECT * FROM `AGMoney` WHERE `Name` = '" + name[n] + "' ;";
                            ResultSet r = stamt.executeQuery(statement);
                            r.last();
                            int money = r.getInt("Money");
                            r.close();
                            CurrencyMain.write(" Updating " + name[n] + "'s money, Setting it from " + money + " to " + (money + amount[n].intValue()) + ", Transfer type: " + transfer.getType());
                            System.out.println("Updating " + name[n] + "'s money, Setting it from " + money + " to " + (money + amount[n].intValue()));
                            statement = "UPDATE AGMoney SET Money='" + (money + amount[n].intValue()) + "' WHERE `Name` = '" + name[n] + "' ;";
                            stamt.executeUpdate(statement);
                            stamt.close();
                            plugin.balance.put(name[n], money + amount[n]);
                        } catch (SQLException ex) {
                            transfer.setStatus(false);
                            transfer.setError(ex.getMessage());
                            System.err.println("[MoneySaveThread] Error while doing statement: " + statement);
                            System.err.println("[MoneySaveThread] MySql error: " + ex.getMessage());
                        } catch (NullPointerException ex) {
                            transfer.setStatus(false);
                            transfer.setError(ex.getMessage());
                            System.err.println("[MoneySaveThread] Error while doing statement: " + statement);
                            System.err.println("[MoneySaveThread] Error while performing a query. (NullPointerException)");
                        }
                    }
                if (this.plugin.isEnabled()) Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, new Runnable() {
                    public void run() {
                        MoneySaveThread.this.plugin.transferComplete(transfer);
                    }
                });
            }
            if (plugin.transfers.peek() == null) try {
                Thread.currentThread();
                Thread.sleep(1000L);
            } catch (InterruptedException localInterruptedException) {
            }
        }
    }
}