package ru.ifmo.rain.zagretdinov.bank;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractPerson implements Person {
    protected String name;
    protected String surname;
    protected int passId;
    protected ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();

    AbstractPerson() {}
    public AbstractPerson(String name, String surname, int passId) {
        this.name = name;
        this.surname = surname;
        this.passId = passId;
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

    @Override
    public ConcurrentMap getAccounts() {
        return accounts;
    }

}
