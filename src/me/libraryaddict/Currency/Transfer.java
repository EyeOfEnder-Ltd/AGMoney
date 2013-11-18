package me.libraryaddict.Currency;

public class Transfer {
    private String sender;
    private String receiver;
    private int amount;
    private String type;
    private boolean status;
    private String error;
    public static String PLAYER_PAYMENT = "player payment";
    public static String SIGN_PURCHASE = "sign purchase";
    public static String MONEY_RESET = "money reset";
    public static String MONEY_GIVE = "money give";
    public static String MONEY_SET = "money set";
    public static String SILENT = null;

    Transfer(String send, String receive, int money, String msg) {
        this.sender = send;
        this.receiver = receive;
        this.amount = money;
        this.type = msg;
    }

    void setError(String er) {
        this.error = er;
    }

    String getError() {
        return this.error;
    }

    void setStatus(boolean stat) {
        this.status = stat;
    }

    boolean getStatus() {
        return this.status;
    }

    String getSender() {
        return this.sender;
    }

    String getReceiver() {
        return this.receiver;
    }

    int getAmount() {
        return this.amount;
    }

    String getType() {
        return this.type;
    }
}