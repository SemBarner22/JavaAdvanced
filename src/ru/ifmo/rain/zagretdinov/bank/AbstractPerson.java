package ru.ifmo.rain.zagretdinov.bank;

import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractPerson implements Person {
    protected final String name;
    protected final String surname;
    protected final int passId;
    protected final ConcurrentMap<String, Account> accounts;

    public AbstractPerson(String name, String surname, int passId) {
        this.name = name;
        this.surname = surname;
        this.passId = passId;
        this.accounts = new ConcurrentHashMap<>();
    }

    public AbstractPerson(String name, String surname, int passId, ConcurrentMap<String, Account> accounts) {
        this.name = name;
        this.surname = surname;
        this.passId = passId;
        this.accounts = accounts;
    }

    @Override
    public synchronized Account getAccount(String accId) {
        return accounts.get(accId);
    }

    @Override
    public int getPassId() {
        return passId;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSurname() {
        return surname;
    }

//    @Override
//    public synchronized ConcurrentMap<String, Account> getAccounts() {
//        return accounts;
//    }

}
