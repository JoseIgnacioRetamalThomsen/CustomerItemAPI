package com.billy.service;

import com.billy.database.CustomerDAO;
import com.billy.objects.Customer;
import com.billy.objects.CustomerUpdateRequest;

import java.util.Iterator;
import java.util.Optional;

/**
 * Service layer for managing Customer entities.
 * Provides CRUD operations and delegates to CustomerDAO.
 */
public class CustomerService {

    public static final String USER_NOT_FOUND = "User not found";
    private final CustomerDAO dao;

    public CustomerService(CustomerDAO dao) {
        this.dao = dao;
    }

    /**
     * Returns an iterator over all customers.
     * Useful for streaming large datasets without loading everything in memory.
     *
     * @return an iterator of Customer objects
     */
    public Iterator<Customer> iteratorAllCustomers() {
        return dao.iteratorAllCustomers();
    }

    /**
     * Finds a customer by their ID.
     *
     * @param id the ID of the customer
     * @return an Optional containing the Customer if found, or empty if not
     */
    public Optional<Customer> getCustomerById(long id) {
        return dao.find(id);
    }

    /**
     * Creates a new customer.
     *
     * @param customer the Customer to create
     * @return the created Customer with assigned ID
     */
    public Optional<Customer> createCustomer(Customer customer) {
        return dao.save(customer);
    }

    /**
     * Deletes a customer by ID.
     *
     * @param id the ID of the customer to delete
     * @return true if the customer was deleted, false if not found
     */
    public boolean deleteCustomer(long id) {
        return dao.delete(id);
    }

    /**
     * Updates an existing customer using the provided update request.
     *
     * @param req the CustomerUpdateRequest containing updates
     * @return a Response indicating success or failure
     */
    public Optional<Customer> updateCustomer(long idToUpdate, CustomerUpdateRequest req) {
        return dao.update(idToUpdate, req);

    }
}

