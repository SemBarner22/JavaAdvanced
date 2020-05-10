package ru.ifmo.rain.zagretdinov.bank;

import junit.framework.Assert;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.rmi.RemoteException;

public class BankTests extends org.junit.Assert {
    private Server server;

    @BeforeEach
    public void createRegistryAndBank() {
        server = new Server();
        Server.main();
    }

    @Test
    @DisplayName("Simple single Account check")
    public void checkAccount() throws RemoteException {
        Account account = new RemoteAccount("abc");
        account.setAmount(100);
        account.setAmount(100 + account.getAmount());
        Assert.assertEquals("abc", account.getId());
        Assert.assertEquals(200, account.getAmount());
    }

    @Test
    @DisplayName("Remote person single account")
    public void remotePerson() throws RemoteException {
        assertNotNull(server);
        assertNotNull(Server.bank);
        Server.bank.createRemotePerson("sem", "barner", 1234);
        Server.bank.getRemotePerson(1234).createAccount("1234:1");
        Account account = (Account) Server.bank.getRemotePerson(1234).getAccounts().get("1234:1");
        for (int i = 0; i < 5; i++) {
            account.setAmount(account.getAmount() + 100);
        }
        assertEquals(500, account.getAmount());
        assertEquals(500, Server.bank.getAccount("1234:1").getAmount());
    }

    @Test
    @DisplayName("Local person single account")
    public void localPerson() throws RemoteException {
        LocalPerson localPerson = (LocalPerson) Server.bank.createLocalPerson("sem", "barner", 1234);
        localPerson.createAccount("1234:1");
        Account accountBank = Server.bank.createAccount("1234:1");
        Account account = (Account) localPerson.getAccounts().get("1234:1");
        for (int i = 0; i < 5; i++) {
            account.setAmount(account.getAmount() + 100);
        }
        assertEquals(500, account.getAmount());
        assertEquals(0, accountBank.getAmount());
    }
 }
