package ru.ifmo.rain.zagretdinov.bank;

import java.rmi.RemoteException;

public class RemotePerson extends AbstractPerson {
    private final Bank bank;

    public RemotePerson(String name, String surname, int passId, Bank bank) throws RemoteException {
        super(name, surname, passId);
        this.bank = bank;
    }

    @Override
    public void createAccount(String bankAccount) throws RemoteException {
        accounts.putIfAbsent(bankAccount, bank.createAccount(bankAccount));
    }
}
