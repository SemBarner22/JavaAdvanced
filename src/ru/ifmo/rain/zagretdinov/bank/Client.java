package ru.ifmo.rain.zagretdinov.bank;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Objects;

public class Client {
    public static void main(final String... args) throws RemoteException {
        if (args == null || args.length < 4 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Usage: [name], [surname], [passport id], [account id], [(optional) money amount change]");
            return;
        }
        try {
            final String name = args[0];
            final String surname = args[1];
            final int passId = Integer.parseInt(args[2]);
            final String accountId = args[3];
            final int moneyDelta = args.length > 4 ? Integer.parseInt(args[3]) : 0;
            final String bankAccount = passId + ":" + accountId;
            final Bank bank;
            try {
                bank = (Bank) Naming.lookup("//localhost:1895/bank");
            } catch (final NotBoundException e) {
                System.out.println("Bank is not bound");
                return;
            } catch (final MalformedURLException e) {
                System.out.println("Bank URL is invalid");
                return;
            }
            Person person = bank.getRemotePerson(passId);
            if (person == null) {
                System.out.println("Creating person");
                person = bank.createRemotePerson(name, surname, passId);
            } else {
                System.out.println("Person already exists");
            }
            System.out.println((person.getAccount(bankAccount) != null ?
                    ((person.getAccount(bankAccount).getAmount())) : 0));
            person.createAccount(bankAccount);
            (person.getAccount(bankAccount)).setAmount(
                    (person.getAccount(bankAccount)).getAmount() + moneyDelta);
            System.out.println((person.getAccount(bankAccount)) != null ?
                    ((person.getAccount(bankAccount)).getAmount()) : 0);

            Account account = bank.getAccount(bankAccount);
            if (account == null) {
                System.out.println("Creating account");
                account = bank.createAccount(bankAccount);
            } else {
                System.out.println("Account already exists");
            }
//            System.out.println("Account id: " + account.getId());
            System.out.println("Money: " + account.getAmount());
            System.out.println("Adding money");
            account.setAmount(account.getAmount() + moneyDelta);
            System.out.println("Money: " + account.getAmount());
        } catch (NumberFormatException e) {
            System.err.println("Incorrect number format");
        }
    }
}
