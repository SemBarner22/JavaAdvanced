package ru.ifmo.rain.zagretdinov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentMap;

public interface Person extends Remote {
    int getPassId() throws RemoteException;

    String getName() throws RemoteException;

    String getSurname() throws RemoteException;

    ConcurrentMap getAccounts() throws RemoteException;

    void createAccount(String bankAccount) throws RemoteException;

//    void addMoney(String bankAccount, int amount) throws RemoteException;
}
