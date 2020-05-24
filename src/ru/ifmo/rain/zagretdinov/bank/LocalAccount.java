package ru.ifmo.rain.zagretdinov.bank;

import java.io.Serializable;

public class LocalAccount extends AbstractAccount implements Serializable {

    public LocalAccount(String id) {
        super(id);
    }

    public LocalAccount(String id, int amount) {
        this.amount = amount;this.id = id;
    }

}
