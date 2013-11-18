package me.libraryaddict.Currency;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MoneyLoadThread extends Thread {
    public Connection con = null;
    private CurrencyMain plugin;

    MoneyLoadThread(CurrencyMain MoneySystem) {
        this.plugin = MoneySystem;
    }

    public void SQLdisconnect() {
        try {
            System.out.println("[MoneyLoadThread] Disconnecting from MySQL database...");
            this.con.close();
        } catch (SQLException ex) {
            System.err.println("[MoneyLoadThread] Error while closing the connection...");
        } catch (NullPointerException ex) {
            System.err.println("[MoneyLoadThread] Error while closing the connection...");
        }
    }

    public void SQLconnect() {
        try {
            System.out.println("[MoneyLoadThread] Connecting to MySQL database...");
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            String conn = "jdbc:mysql://" + this.plugin.SQL_HOST + "/" + this.plugin.SQL_DATA;
            this.con = DriverManager.getConnection(conn, this.plugin.SQL_USER, this.plugin.SQL_PASS);
        } catch (ClassNotFoundException ex) {
            System.err.println("[MoneyLoadThread] No MySQL driver found!");
        } catch (SQLException ex) {
            System.err.println("[MoneyLoadThread] Error while fetching MySQL connection!");
        } catch (Exception ex) {
            System.err.println("[MoneyLoadThread] Unknown error while fetchting MySQL connection.");
        }

        boolean exists = true;
        try {
            DatabaseMetaData dbm = this.con.getMetaData();

            ResultSet tables = dbm.getTables(null, null, "AGMoney", null);
            if (!tables.next()) exists = false;
        } catch (SQLException localSQLException1) {
        } catch (NullPointerException localNullPointerException1) {
        }
        if (!exists) {
            String sta = "CREATE TABLE AGMoney (ID int(10) unsigned NOT NULL AUTO_INCREMENT, Name varchar(20) NOT NULL, Money int(20) NOT NULL, PRIMARY KEY (`ID`))";
            try {
                Statement st = this.con.createStatement();
                st.executeUpdate(sta);
                st.close();
            } catch (SQLException ex) {
                System.err.println("[Money] Error with following query: " + sta);
                System.err.println("[Money] MySQL-Error: " + ex.getMessage());
            } catch (NullPointerException ex) {
                System.err.println("[Money] Error while performing a query. (NullPointerException)");
            }
        }
    }

    public void run() {
        System.out.println("Money load thread started");
        SQLconnect();
        while (true) {
            if (plugin.refreshers.peek() != null) {
                String playername = plugin.refreshers.poll();
                try {
                    Statement stmt = this.con.createStatement();
                    ResultSet r = stmt.executeQuery("SELECT * FROM `AGMoney` WHERE `Name` = '" + playername + "' ;");
                    r.last();
                    if (r.getRow() == 0) {
                        stmt.close();
                        r.close();
                        System.out.println(playername + "'s money doesn't exist, Creating it");
                        String insert = "INSERT INTO AGMoney (Name, Money) VALUES ('" + playername + "', '0')";
                        try {
                            Statement stamt = this.con.createStatement();
                            stamt.executeUpdate(insert);
                            stamt.close();
                            plugin.refreshers.add(playername);
                        } catch (SQLException ex) {
                            System.err.println("[MoneyLoadThread] MySql error while creating new balance for " + playername + ", Error: " + ex);
                        } catch (NullPointerException ex) {
                            System.err.println("[MoneyLoadThread] MySql error while creating new balance for " + playername + ", Error: " + ex);
                        }
                    } else {
                        plugin.balance.put(playername, r.getInt("Money"));
                        stmt.close();
                        r.close();
                    }
                } catch (SQLException ex) {
                    System.out.println("[MoneyLoadThread] Error while fetching " + playername + "'s Money: " + ex);
                } catch (NullPointerException ex) {
                    System.out.println("[MoneyLoadThread] Error while fetching " + playername + "'s Money: " + ex);
                }
            }
            if (plugin.refreshers.peek() == null) try {
                Thread.currentThread();
                Thread.sleep(1000L);
            } catch (InterruptedException localInterruptedException) {
            }
        }
    }
}