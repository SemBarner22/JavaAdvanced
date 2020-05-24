package ru.ifmo.rain.zagretdinov.bank;


public abstract class AbstractAccount implements Account {
    protected String id;
    protected int amount;

    public AbstractAccount(final String id) {
        this.id = id;
        amount = 0;
    }

    AbstractAccount() {}

    public String getId() {
        return id;
    }

    public synchronized int getAmount() {
        return amount;
    }

    public synchronized void setAmount(final int amount) {
        this.amount = amount;
    }
//
//    abstract public int getAmount();
//
//    abstract public void setAmount(final int amount);
}
