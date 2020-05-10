package ru.ifmo.rain.zagretdinov.bank;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;
    private final ConcurrentMap<String, Account> accounts = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Person> bankClients = new ConcurrentHashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public Person getRemotePerson(final int passId) {
        return bankClients.get(passId);
    }

    @Override
    public synchronized Person getLocalPerson(final int passId) throws RemoteException {
        Person remotePerson = bankClients.get(passId);
        if (remotePerson != null) {
            final Person person = new LocalPerson(remotePerson.getName(), remotePerson.getSurname(), remotePerson.getPassId());
            var setSubId = remotePerson.getAccounts().keySet();
            for (var s : setSubId) {
                Account account = accounts.get(s);
                person.getAccounts().putIfAbsent(s.toString(), new LocalAccount(account.getId(), account.getAmount()));
            }
            return person;
        }
        return null;
    }

    @Override
    public Person createRemotePerson(final String name, final String surname, final int passId) throws RemoteException {
        Person person = new RemotePerson(name, surname, passId, this);
        bankClients.put(passId, person);
        UnicastRemoteObject.exportObject(person, port);
        return person;
    }

    @Override
    public Person createLocalPerson(String name, String surname, int passId) throws RemoteException {
        Person person = new RemotePerson(name, surname, passId, this);
        bankClients.put(passId, person);
        UnicastRemoteObject.exportObject(person, port);
        return new LocalPerson(name, surname, passId);
    }

    @Override
    public Account createAccount(final String id) throws RemoteException {
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
    public Account getAccount(final String id) {
        System.out.println("Retrieving account " + id);
        return accounts.get(id);
    }

}
