package ru.ifmo.rain.zagretdinov.bank;

public class RemoteAccount extends AbstractAccount {
    public RemoteAccount(final String id) {
        super(id);
    }

    public synchronized int getAmount() {
        System.out.println("Getting amount of money for account " + id);
        return amount;
    }

    public synchronized void setAmount(final int amount) {
        System.out.println("Setting amount of money for account " + id);
        this.amount = amount;
    }
}
