package ru.ifmo.rain.zagretdinov.bank;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Person> bankClients = new ConcurrentHashMap<>();

    public int getPort() {
        return port;
    }

    @Override
    public ConcurrentMap getAccounts() {
        return accounts;
    }

    @Override
    public ConcurrentMap getBankClients() {
        return bankClients;
    }

    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public synchronized Person getRemotePerson(final int passId) {
        return bankClients.get(passId);
    }

    @Override
    public synchronized Person getLocalPerson(final int passId) throws RemoteException {
        Person remotePerson = bankClients.get(passId);
        if (remotePerson != null) {
            return new LocalPerson((RemotePerson) remotePerson);
        }
        return null;
    }

    @Override
    public synchronized Person createRemotePerson(final String name, final String surname, final int passId) throws RemoteException {
        Person person = new RemotePerson(name, surname, passId, this);
        bankClients.put(passId, person);
        UnicastRemoteObject.exportObject(person, port);
        return person;
    }

    @Override
    public synchronized Person createLocalPerson(String name, String surname, int passId) throws RemoteException {
        RemotePerson person = new RemotePerson(name, surname, passId, this);
        bankClients.put(passId, person);
        UnicastRemoteObject.exportObject(person, port);
        return new LocalPerson(person);
    }

    @Override
    public synchronized Account createAccount(final String id) throws RemoteException {
        System.out.println("Creating account " + id);
        final Account account = new RemoteAccount(id);
        if (accounts.putIfAbsent(id, account) == null) {
            UnicastRemoteObject.exportObject(account, port);
            return account;
        } else {
            return getAccount(id);
        }
    }

    @Override
    public synchronized Account getAccount(final String id) {
        System.out.println("Retrieving account " + id);
        return accounts.get(id);
    }

}
