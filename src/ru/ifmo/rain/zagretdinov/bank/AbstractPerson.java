package ru.ifmo.rain.zagretdinov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractPerson implements Person {
    protected String name;
    protected String surname;
    protected int passId;
    protected ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();

    AbstractPerson() {};
    public AbstractPerson(String name, String surname, int passId) throws RemoteException {
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


    //    @Override
//    public void addMoney(String bankAccount, int amount) throws RemoteException {
//        if (getAccounts().putIfAbsent(bankAccount, new Re) != null) {
//            ((Account)getAccounts().get(bankAccount)).setAmount(((Account)getAccounts().get(bankAccount)).getAmount() + amount);
////            getAccounts().computeIfPresent(bankAccount, (key, account) -> {
////                try {
////                    return ((Account) account).getAmount() + amount;
////                } catch (RemoteException e) {
////                    e.printStackTrace();
////                } finally {
////                    return 0;
////                }
////            });
////        getAccounts().put(, Integer.parseInt((String) getAccounts().get(passId + ":" + subId)) + amount);
//        }
//    }

}
