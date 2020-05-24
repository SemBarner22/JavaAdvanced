package ru.ifmo.rain.zagretdinov.bank;



import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;

public class BankTests extends Assert {
    private Server server;


    private boolean checkEqualityPerson(Person person1, Person person2) throws RemoteException {
        return person1.getPassId() == person2.getPassId() && person1.getName().equals(person2.getName())
               && person1.getSurname().equals(person2.getSurname());
    }

    private boolean checkEqualityAccount(Account account1, Account account2) throws RemoteException {
        return account1.getId().equals(account2.getId()) && account1.getAmount() == account2.getAmount();
    }

    @Before
    public void createRegistryAndBank() {
        server = new Server();
        Server.main();
        assertNotNull(server);
        assertNotNull(Server.bank);
    }

    @Test
    public void emptyAccount() throws RemoteException {
        Account account = Server.bank.createAccount("1234:1");
        Account getAccount = Server.bank.getAccount("1234:1");
        assertTrue(checkEqualityAccount(account, getAccount));
    }

    @Test
    public void emptyPerson() throws RemoteException {
        Person person = Server.bank.createRemotePerson("sem", "barner", 1234);
        Person bankPerson = Server.bank.getRemotePerson(1234);
        assertNotNull(person);
        assertNotNull(bankPerson);
        assertTrue(checkEqualityPerson(person, bankPerson));
    }

    @Test
    public void checkAccount() throws RemoteException {
        Account account = new RemoteAccount("abc");
        account.setAmount(100);
        account.setAmount(100 + account.getAmount());
        Assert.assertEquals("abc", account.getId());
        Assert.assertEquals(200, account.getAmount());
    }

    @Test
    public void singleAccMultipleTimes() throws RemoteException {
        String[] bankAccounts = new String[]{"1234:1", "1234:2"};
        for (int i = 0; i < 5; i++) {
            for (var bankAccount : bankAccounts) {
                Account account = Server.bank.getAccount(bankAccount);
                if (account == null) {
                    System.out.println("Creating account");
                    account = Server.bank.createAccount(bankAccount);
                } else {
                    System.out.println("Account already exists");
                }
            }
        }
        assertEquals(2, Server.bank.getAccounts().size());
        for (String bankAccount : bankAccounts) {
            assertEquals(bankAccount, Server.bank.getAccount(bankAccount).getId());
        }
    }

    @Test
    public void remotePerson() throws RemoteException {
        Server.bank.createRemotePerson("sem", "barner", 1234);
        Server.bank.getRemotePerson(1234).createAccount("1234:1");
        Account account = Server.bank.getRemotePerson(1234).getAccount("1234:1");
        for (int i = 0; i < 5; i++) {
            account.setAmount(account.getAmount() + 100);
        }
        assertEquals(500, account.getAmount());
        assertEquals(500, Server.bank.getAccount("1234:1").getAmount());
    }

    @Test
    public void createRemotePerson() throws RemoteException {
        Server.bank.createRemotePerson("sem", "barner", 1234);
        Person person = Server.bank.getRemotePerson(1234);
        assertEquals("sem", person.getName());
        assertEquals("barner", person.getSurname());
        assertEquals(1234, person.getPassId());
    }

    @Test
    public void createLocalPerson() throws RemoteException {
        Server.bank.createLocalPerson("sem", "barner", 1234);
        Person person = Server.bank.getLocalPerson(1234);
        assertEquals("sem", person.getName());
        assertEquals("barner", person.getSurname());
        assertEquals(1234, person.getPassId());
    }

    @Test
    public void localPerson() throws RemoteException {
        LocalPerson localPerson = (LocalPerson) Server.bank.createLocalPerson("sem", "barner", 1234);
        localPerson.createAccount("1234:1");
        Account accountBank = Server.bank.createAccount("1234:1");
        Account account = localPerson.getAccount("1234:1");
        for (int i = 0; i < 5; i++) {
            account.setAmount(account.getAmount() + 100);
        }
        assertEquals(500, account.getAmount());
        assertEquals(0, accountBank.getAmount());
    }

    @Test
    public void NonExistingAccount() throws RemoteException {
        assertNull(Server.bank.getAccount("1234:1"));
    }

    @Test
    public void emptyMap() throws RemoteException {
        assertEquals(new ConcurrentHashMap<String, Account>(), Server.bank.getAccounts());
    }

    @Test
    public void emptyClients() throws RemoteException {
        assertEquals(new ConcurrentHashMap<Integer, Person>(), Server.bank.getBankClients());
    }


//    @Test
//    @DisplayName("Multithread account creation")
//    public void multiThreadAccounts() throws RemoteException {
//        int threadAmount = 2;
//        int reqPerThread = 2;
//        ExecutorService clientThreads = Executors.newFixedThreadPool(threadAmount);
//        IntStream.range(0, threadAmount)
//                .forEach(i -> clientThreads.submit(() -> {
//                    IntStream.range(0, reqPerThread).forEach(j -> {
//                        synchronized (Server.bank)
//                    });
//                }));
//        clientThreads.shutdown();
//        try {
//            clientThreads.awaitTermination(requests * threads, TimeUnit.SECONDS);
//        } catch (final InterruptedException e) {
//            System.err.println("Procession have been interrupted " + e.getMessage());
//        }
//        IntStream.range(0, reqPerThread).forEach(i -> threadPool.submit(() -> {
//
//        }));
//        threadPool.
//    }

 }
