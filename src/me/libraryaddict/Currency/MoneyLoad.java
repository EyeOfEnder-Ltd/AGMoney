package me.libraryaddict.Currency;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MoneyLoad {
    private Connection con;
    private CurrencyMain plugin;

    public MoneyLoad(CurrencyMain MoneySystem) {
        this.plugin = MoneySystem;
    }

    public void SQLdisconnect() {
        try {
            plugin.getLogger().info("Disconnecting from MySQL database...");
            this.con.close();
        } catch (SQLException ex) {
            plugin.getLogger().severe("Error while closing the connection...");
        } catch (NullPointerException ex) {
            plugin.getLogger().severe("Error while closing the connection...");
        }
    }

    public void SQLconnect() {
        try {
            plugin.getLogger().info("Connecting to MySQL database...");
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            String conn = "jdbc:mysql://" + this.plugin.SQL_HOST + "/" + this.plugin.SQL_DATA;
            this.con = DriverManager.getConnection(conn, this.plugin.SQL_USER, this.plugin.SQL_PASS);
        } catch (ClassNotFoundException ex) {
            plugin.getLogger().severe("No MySQL driver found!");
        } catch (SQLException ex) {
            plugin.getLogger().severe("Error while fetching MySQL connection!");
        } catch (Exception ex) {
            plugin.getLogger().severe("Unknown error while fetchting MySQL connection.");
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
                plugin.getLogger().severe("Error with following query: " + sta);
                plugin.getLogger().severe("MySQL-Error: " + ex.getMessage());
            } catch (NullPointerException ex) {
                plugin.getLogger().severe("Error while performing a query. (NullPointerException)");
            }
        }
    }

    public void refresh(String player) {
        try {
            Statement stmt = this.con.createStatement();
            ResultSet r = stmt.executeQuery("SELECT * FROM `AGMoney` WHERE `Name` = '" + player + "' ;");
            r.last();
            if (r.getRow() == 0) {
                stmt.close();
                r.close();
                plugin.getLogger().info(player + "'s money doesn't exist, Creating it");
                String insert = "INSERT INTO AGMoney (Name, Money) VALUES ('" + player + "', '0')";
                try {
                    Statement stamt = this.con.createStatement();
                    stamt.executeUpdate(insert);
                    stamt.close();
                    refresh(player);
                } catch (SQLException ex) {
                    plugin.getLogger().severe("MySql error while creating new balance for " + player + ", Error: " + ex);
                } catch (NullPointerException ex) {
                    plugin.getLogger().severe("MySql error while creating new balance for " + player + ", Error: " + ex);
                }
            } else {
                plugin.balance.put(player, r.getInt("Money"));
                stmt.close();
                r.close();
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Error while fetching " + player + "'s Money: " + ex);
        } catch (NullPointerException ex) {
            plugin.getLogger().severe("Error while fetching " + player + "'s Money: " + ex);
        }
    }
}