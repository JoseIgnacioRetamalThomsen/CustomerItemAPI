package com.billy.objects;

import java.io.Serializable;
import java.rmi.server.UID;

public record Customer(long id , String name, String lastName, String gender, String email) implements Serializable {
    private static final long serialVersionUID = 1L;
    public Customer(long id, Customer customer) {
        this(id,customer.name,customer.lastName,customer.gender,customer.email);
    }
}
