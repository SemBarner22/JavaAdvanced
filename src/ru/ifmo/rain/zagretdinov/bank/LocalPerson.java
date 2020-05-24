package ru.ifmo.rain.zagretdinov.bank;

import java.io.Serializable;
import java.rmi.RemoteException;

public class LocalPerson extends AbstractPerson implements Serializable {

    public LocalPerson(String name, String surname, int passId) {
        super(name, surname, passId);
    }

    public LocalPerson(RemotePerson remotePerson) throws RemoteException {
        super(remotePerson.getName(), remotePerson.getSurname(), remotePerson.getPassId());
        synchronized (remotePerson.accounts) {
            for (String s : remotePerson.accounts.keySet()) {
                Account account = accounts.get(s);
                remotePerson.accounts.putIfAbsent(s, new LocalAccount(account.getId(), account.getAmount()));
            }
        }
    }

    @Override
    public void createAccount(String bankAccount) {
        accounts.putIfAbsent(bankAccount, new LocalAccount(bankAccount));
    }

//    void setAccounts(ConcurrentMap<String, Account> accounts) {
//        this.accounts = accounts;
//    }

}
