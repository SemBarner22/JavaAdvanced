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

    public Person getRemotePerson(final String name, final String surname, final int passId) throws RemoteException, ValidateException {
        final Person person = new RemotePerson(name, surname, passId, this);
        if (bankClients.putIfAbsent(passId, person) == null) {
            UnicastRemoteObject.exportObject(person, port);
            return bankClients.get(passId);
        } else {
            if (person.getName().equals(bankClients.get(passId).getName()) &&
                    person.getSurname().equals(bankClients.get(passId).getSurname()) &&
                    person.getPassId() == bankClients.get(passId).getPassId()) {
                return bankClients.get(passId);
            }
            throw new ValidateException();
        }
    }

    public Person getLocalPerson(final String name, final String surname, final int passId) throws RemoteException,
            ValidateException {
        final LocalPerson person = new LocalPerson(name, surname, passId);
        final Person person1 = bankClients.get(passId);
        if (bankClients.putIfAbsent(passId, person) == null) {
            return person;
        } else {
            if (person1 != null) {
                var setSubId = person1.getAccounts().keySet();
                for (var s : setSubId) {
                    Account account = accounts.get(s);
                    person.accounts.putIfAbsent(s.toString(), new LocalAccount(account.getId(), account.getAmount()));
                }
                //person.getAccounts().putIfAbsent(new LocalAccount(ac));
            }

            if (person.getName().equals(bankClients.get(passId).getName()) &&
                    person.getSurname().equals(bankClients.get(passId).getSurname()) &&
                    person.getPassId() == bankClients.get(passId).getPassId()) {
                // Should be .exportObject?
                return person;
            }
            throw new ValidateException();
        }
    }

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

//    public Account createAccount(final Person person, final String id) throws RemoteException {
//        final Account account = new RemoteAccount(id);
//        if (accounts.putIfAbsent(id, account) == null) {
//            UnicastRemoteObject.exportObject(account, port);
//            return account;
//        } else {
//            return getAccount(id);
//        }
//    }

    public Account getAccount(final String id) {
        System.out.println("Retrieving account " + id);
        return accounts.get(id);
    }
}
