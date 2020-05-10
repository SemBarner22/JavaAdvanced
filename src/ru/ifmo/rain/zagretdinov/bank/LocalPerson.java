package ru.ifmo.rain.zagretdinov.bank;

import java.io.Serializable;
import java.rmi.RemoteException;

public class LocalPerson extends AbstractPerson implements Serializable {

    public LocalPerson(String name, String surname, int passId) throws RemoteException {
        super(name, surname, passId);
    }


    @Override
    public void createAccount(String bankAccount) {
        accounts.putIfAbsent(bankAccount, new LocalAccount(bankAccount));
    }

//    void setAccounts(ConcurrentMap<String, Account> accounts) {
//        this.accounts = accounts;
//    }

}
