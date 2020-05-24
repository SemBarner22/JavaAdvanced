package ru.ifmo.rain.zagretdinov.bank;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentMap;

public interface Bank extends Remote {
    /**
     * Creates a new account with specified identifier if it is not already exists.
     * @param id account id
     * @return created or existing account.
     */
    Account createAccount(String id) throws RemoteException;

    Person getRemotePerson(final int passId) throws RemoteException;

    Person getLocalPerson(final int passId) throws RemoteException;

    ConcurrentMap getAccounts() throws RemoteException;

    ConcurrentMap getBankClients() throws RemoteException;

    int getPort() throws RemoteException;
    /**
     * Returns account by identifier.
     * @param id account id
     * @return account with specified identifier or {@code null} if such account does not exists.
     */
    Account getAccount(String id) throws RemoteException;

    Person createRemotePerson(final String name, final String surname, final int passId) throws RemoteException;

    Person createLocalPerson(final String name, final String surname, final int passId) throws RemoteException;
}
