package ru.ifmo.rain.zagretdinov.bank.tests;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.ifmo.rain.zagretdinov.bank.*;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class BankTests extends BaseTest {
    private static final String name = "sem";
    private static final String surname = "barner";
    private static final int passId = 1234;
    private static final String acc = "1234:1";
    private Bank bank;

    private boolean checkEqualityPerson(Person person1, Person person2) throws RemoteException {
        return person1.getPassId() == person2.getPassId() && person1.getName().equals(person2.getName())
                && person1.getSurname().equals(person2.getSurname());
    }

    private boolean checkEqualityAccount(Account account1, Account account2) throws RemoteException {
        return account1.getId().equals(account2.getId()) && account1.getAmount() == account2.getAmount();
    }

    @BeforeClass
    public static void init() {
        Server.main();
    }

    @Before
    public void createRegistryAndBank() {
        Server.createBank();
        bank = Server.bank;
        assertNotNull(Server.bank);
    }

    @Test
    public void emptyAccount() throws RemoteException {
        Account account = Server.bank.createAccount(acc);
        Account getAccount = Server.bank.getAccount(acc);
        assertTrue(checkEqualityAccount(account, getAccount));
    }

    @Test
    public void emptyPerson() throws RemoteException {
        final Person person = bank.createRemotePerson(name, surname, passId);
        final Person bankPerson = bank.getRemotePerson(passId);
        assertNotNull(person);
        assertNotNull(bankPerson);
        assertTrue(checkEqualityPerson(person, bankPerson));
    }

    @Test
    public void checkAccount() throws RemoteException {
        final Account account = new RemoteAccount("abc");
        account.setAmount(100);
        account.setAmount(100 + account.getAmount());
        assertEquals("abc", account.getId());
        assertEquals(200, account.getAmount());
    }

    @Test
    public void singleAccMultipleTimes() throws RemoteException {
        final String[] bankAccounts = new String[]{acc, "1234:2"};
        for (int i = 0; i < 5; i++) {
            for (final var bankAccount : bankAccounts) {
                Account account = bank.getAccount(bankAccount);
                if (account == null) {
                    System.out.println("Creating account");
                    account = bank.createAccount(bankAccount);
                } else {
                    System.out.println("Account already exists");
                }
            }
        }
        assertEquals(2, bank.getAccountsSize());
        for (final String bankAccount : bankAccounts) {
            assertEquals(bankAccount, bank.getAccount(bankAccount).getId());
        }
    }

    @Test
    public void remotePerson() throws RemoteException {
        Server.bank.createRemotePerson(name, surname, 1234);
        Server.bank.getRemotePerson(1234).createAccount(acc);
        Account account = Server.bank.getRemotePerson(1234).getAccount(acc);
        for (int i = 0; i < 5; i++) {
            account.setAmount(account.getAmount() + 100);
        }
        assertEquals(500, account.getAmount());
        assertEquals(500, bank.getAccount(acc).getAmount());
    }

    @Test
    public void createRemotePerson() throws RemoteException {
        bank.createRemotePerson(name, surname, 1234);
        final Person person = bank.getRemotePerson(1234);
        assertEquals(name, person.getName());
        assertEquals(surname, person.getSurname());
        assertEquals(1234, person.getPassId());
    }

    @Test
    public void createLocalPerson() throws RemoteException {
        bank.createLocalPerson(name, surname, 1234);
        final Person person = bank.getLocalPerson(1234);
        assertEquals(name, person.getName());
        assertEquals(surname, person.getSurname());
        assertEquals(1234, person.getPassId());
    }

    @Test
    public void localPerson() throws RemoteException {
        final LocalPerson localPerson = (LocalPerson) bank.createLocalPerson(name, surname, 1234);
        localPerson.createAccount(acc);
        Account accountBank = Server.bank.createAccount(acc);
        Account account = localPerson.getAccount(acc);
        for (int i = 0; i < 5; i++) {
            account.setAmount(account.getAmount() + 100);
        }
        assertEquals(500, account.getAmount());
        assertEquals(0, accountBank.getAmount());
    }

    @Test
    public void NonExistingAccount() throws RemoteException {
        assertNull(bank.getAccount(acc));
    }

    @Test
    public void emptyMap() throws RemoteException {
        assertEquals(new ConcurrentHashMap<String, Account>(), bank.getAccountsSize());
    }

    @Test
    public void emptyClients() throws RemoteException {
        assertEquals(new ConcurrentHashMap<Integer, Person>(), bank.getBankClients());
    }

    @Test
    public void personWithSeveralAccountsTest() throws RemoteException {
        int accountAmount = 1000;
        Person person = createPersonWithSeveralAccounts(name, surname, passId, accountAmount);
        assertEquals(accountAmount, person.getAccountSize());
        assertEquals(accountAmount, bank.getAccountsSize());
    }

    public void severalPersonsConcurrent(int personAmount, int accAmountPerPerson, int threadAmount) throws RemoteException {
        for (int i = 0; i < personAmount; i++) {
            bank.createRemotePerson(String.valueOf(i), String.valueOf(i), i);
        }
        final ExecutorService threads = Executors.newFixedThreadPool(threadAmount);
        final List<Integer> ordered = new ArrayList<>();
        IntStream.range(1, accAmountPerPerson).forEach(ordered::add);
        for (int i = 0; i < threadAmount; i++) {
            threads.submit(() -> {
//                List<Integer> unordered = new ArrayList<>();
                final List<Integer> unordered = new ArrayList<>(ordered);
                Collections.shuffle(unordered);
//                IntStream.range(1, amountPerThread).forEach(unordered::add);
                for (final var a : unordered) {
                    IntStream.range(1, accAmountPerPerson).forEach(j -> {
                        try {
                            final Person person = bank.getRemotePerson(a);
                            Account account;
                            synchronized (Server.bank.getRemotePerson(a)) {
                                String match = a + ":" + j;
                                if ((account = person.getAccount(match)) == null) {
                                    person.createAccount(match);
                                    account = person.getAccount(match);
                                }
                                account.setAmount(account.getAmount() + 100);
                            }
                        } catch (final RemoteException e) {
                            throw new AssertionError(e);
                        }
                    });
                }
            });
        }
        threads.shutdown();
        try {
            threads.awaitTermination(accAmountPerPerson * personAmount * 1000, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            System.err.println("Procession have been interrupted " + e.getMessage());
            throw new AssertionError(e);
        }
        IntStream.range(1, personAmount).forEach(i -> {
            IntStream.range(1, accAmountPerPerson).forEach(j -> {
                try {
                    assertEquals(100 * threadAmount, bank.getRemotePerson(i).
                            getAccount(i + ":" + j).getAmount());
                } catch (RemoteException e) {
                    throw new AssertionError(e);
                }
            });
        });
    }

    @Test
    public void severalRemotePersonsConcurrent() throws RemoteException {
        severalPersonsConcurrent(100, 100, 10);
    }

    public Person createPersonWithSeveralAccounts(final String name, final String surname, final int passid, final int accountAmount) throws RemoteException {
        bank.createRemotePerson(name, surname, passid);
        for (int i = 0; i < accountAmount; i++) {
            bank.getRemotePerson(passid).createAccount(passid + String.valueOf(i));
        }
        return bank.getRemotePerson(passid);
    }

    public List<Person> createSeveralPersonsWithSeveralAccounts(final int personAmount, final int accsPerPerson) throws RemoteException {
        final List<Person> people = new ArrayList<>();
        for (int i = 0; i < personAmount; i++) {
            people.add(createPersonWithSeveralAccounts(String.valueOf(i), String.valueOf(i), i, accsPerPerson));
        }
        return people;
    }

    @Test
    public void severalPersonsWithSeveralAccountsTest() throws RemoteException {
        List<Person> people = createSeveralPersonsWithSeveralAccounts(10, 100);
        for (var p : people) {
            assertEquals(100, p.getAccountSize());
        }
        assertEquals(10 * 100, bank.getAccountsSize());
    }

    public void multiThreadNoThrow(final int threads, final int taskAmount, final int personAmount, final int accPerPerson) {
        final List<Command<Exception>> callables = new ArrayList<>();
        for (int i = 0; i < taskAmount; i++) {
            callables.add(() -> {
                createSeveralPersonsWithSeveralAccounts(personAmount, accPerPerson);
            });
        }
        parallelCommands(threads, callables);
    }

    @Test
    public void multiThreadNoThrowTest() {
        multiThreadNoThrow(10, 100, 4, 100);
    }

    public void concurrent(int accAmount, int threadsAmount, int amountPerThread) {
        final ExecutorService threads = Executors.newFixedThreadPool(threadsAmount);
        IntStream.range(1, amountPerThread).forEach(i -> {
            try {
                bank.createAccount(String.valueOf(i));
            } catch (final RemoteException e) {
                throw new AssertionError(e);
            }
        });
        for (int i = 0; i < threadsAmount; i++) {
            threads.submit(() -> {
                IntStream.range(1, accAmount).forEach(a -> {
                    try {
                        bank.getAccount(String.valueOf(a)).addAmount(100);
                    } catch (final RemoteException e) {
                        throw new AssertionError(e);
                    }
                });
            });
        }
        threads.shutdown();
        try {
            threads.awaitTermination(amountPerThread * 1000, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            System.err.println("Procession have been interrupted " + e.getMessage());
            throw new AssertionError(e);
        }
        IntStream.range(1, amountPerThread).forEach(i -> {
            try {
                assertEquals(100 * threadsAmount, bank.getAccount(String.valueOf(i)).getAmount());
            } catch (final RemoteException e) {
                throw new AssertionError(e);
            }
        });
    }


    @Test
    public void severalAccountConcurrent() {
        concurrent(100, 10, 100);
    }

    @Test
    public void severalAccountsConcurrent() {
        concurrent(1000, 100, 100);
    }

}
