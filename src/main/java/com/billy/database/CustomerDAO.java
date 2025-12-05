package com.billy.database;

import com.billy.objects.Customer;
import com.billy.objects.CustomerUpdateRequest;
import org.mapdb.Atomic;
import org.mapdb.BTreeMap;
import org.mapdb.Serializer;

import java.util.Iterator;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) for Customer entities using MapDB.
 * Provides CRUD operations, iteration, and persistent storage
 * with periodic commits and safe shutdown handling.
 */
public class CustomerDAO {
    private static final Logger logger = Logger.getLogger(CustomerDAO.class.getName());
    public static final String TABLE_NAME = "customers";
    public static final String SEQUENCE_NAME = "customer_seq";
    private final BTreeMap<Long, Customer> customers;
    private final Atomic.Long customerIdSeq;

    public CustomerDAO(MapDbWrapper dbWrapper) {
        customerIdSeq = dbWrapper.db().atomicLong(SEQUENCE_NAME).createOrOpen();

        @SuppressWarnings("unchecked")
        BTreeMap<Long, Customer> map = (BTreeMap<Long, Customer>) dbWrapper.db()
                .treeMap(TABLE_NAME, Serializer.LONG, Serializer.JAVA)
                .createOrOpen();
        this.customers = map;
    }

    /**
     * Saves a new customer and assigns a unique ID.
     *
     * @param customer the customer to save
     * @return the saved customer with assigned ID
     */
    public Optional<Customer> save(Customer customer) {
        if (customer == null) {
            return Optional.empty();
        }
        long id = customerIdSeq.incrementAndGet();
        Customer customerWithId = new Customer(id, customer);
        customers.put(id, customerWithId);
        return Optional.of(customerWithId);
    }

    /**
     * Finds a customer by ID.
     *
     * @param id the customer ID as Long
     * @return the customer if found, otherwise null
     */
    public Optional<Customer> find(long id) {
        try {
            return Optional.ofNullable(customers.get(id));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to find customer " + id, e);
            return Optional.empty();
        }
    }

    /**
     * Deletes a customer by ID.
     *
     * @param id the customer ID
     * @return true if the customer was removed, false otherwise
     */
    public boolean delete(long id) {
        try {
            return customers.remove(id) != null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to delete customer " + id, e);
            return false;
        }
    }

    /**
     * Returns an iterator over all customers.
     *
     * @return iterator for streaming customers
     */
    public Iterator<Customer> iteratorAllCustomers() {
        return customers.values().iterator();
    }

    /**
     * Updates an existing customer.
     *
     * @param updated the update request
     * @return Optional containing updated customer if present
     */
    public Optional<Customer> update(long idToUpdate, CustomerUpdateRequest updated) {
        try {
            if (updated == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(
                    customers.computeIfPresent(idToUpdate, (id, existing) -> new Customer(
                            existing.id(),
                            updated.name().orElse(existing.name()),
                            updated.lastName().orElse(existing.lastName()),
                            updated.gender().orElse(existing.gender()),
                            updated.email().orElse(existing.email())
                    ))
            );
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update customer " + idToUpdate, e);
            return Optional.empty();
        }
    }
}
