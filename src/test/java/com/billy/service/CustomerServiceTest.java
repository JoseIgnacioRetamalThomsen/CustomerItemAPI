package com.billy.service;

import com.billy.database.CustomerDAO;
import com.billy.objects.Customer;
import com.billy.objects.CustomerUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomerServiceTest {

    private CustomerDAO mockDao;
    private CustomerService service;

    @BeforeEach
    void setUp() {
        mockDao = mock(CustomerDAO.class);
        service = new CustomerService(mockDao);
    }

    @Test
    void testIteratorAllUsers() {
        Customer customer = new Customer(1L, "John", "Doe", "male", "john@example.com");
        Iterator<Customer> iterator = Collections.singletonList(customer).iterator();

        when(mockDao.iteratorAllCustomers()).thenReturn(iterator);

        Iterator<Customer> result = service.iteratorAllCustomers();
        assertTrue(result.hasNext());
        assertEquals(customer, result.next());
        verify(mockDao, times(1)).iteratorAllCustomers();
    }

    @Test
    void testGetUserByIdFound() {
        Customer customer = new Customer(1L, "John", "Doe", "male", "john@example.com");
        when(mockDao.find(1L)).thenReturn(Optional.of(customer));

        Optional<Customer> result = service.getCustomerById(1L);
        assertTrue(result.isPresent());
        assertEquals(customer, result.get());
        verify(mockDao, times(1)).find(1L);
    }

    @Test
    void testGetUserByIdNotFound() {
        when(mockDao.find(1L)).thenReturn(Optional.empty());

        Optional<Customer> result = service.getCustomerById(1L);
        assertFalse(result.isPresent());
        verify(mockDao, times(1)).find(1L);
    }

    @Test
    void testCreateUser() {
        Customer customer = new Customer(0, "John", "Doe", "male", "john@example.com");
        Customer saved = new Customer(1L, "John", "Doe", "male", "john@example.com");
        when(mockDao.save(customer)).thenReturn(Optional.of(saved));

        Optional<Customer> result = service.createCustomer(customer);
        assertTrue(result.isPresent());
        assertEquals(saved, result.get());
        verify(mockDao, times(1)).save(customer);
    }

    @Test
    void testDeleteUser() {
        when(mockDao.delete(1L)).thenReturn(true);

        boolean result = service.deleteCustomer(1L);
        assertTrue(result);
        verify(mockDao, times(1)).delete(1L);
    }

    @Test
    void testUpdateUserSuccess() {
        CustomerUpdateRequest req = new CustomerUpdateRequest(1L, Optional.of("John"), Optional.empty(), Optional.empty(), Optional.empty());
        Customer updatedCustomer = new Customer(1L, "John", "Doe", "male", "john@example.com");
        when(mockDao.update(1,req)).thenReturn(Optional.of(updatedCustomer));
        Optional<Customer> optionalCustomer = service.updateCustomer(1,req);
        assertTrue(optionalCustomer.isPresent());
        verify(mockDao, times(1)).update(1,req);
    }

    @Test
    void testUpdateUserNotFound() {
        CustomerUpdateRequest req = new CustomerUpdateRequest(1L, Optional.of("John"), Optional.empty(), Optional.empty(), Optional.empty());
        when(mockDao.update(1,req)).thenReturn(Optional.empty());
        Optional<Customer> optionalCustomer = service.updateCustomer(1,req);
        assertTrue(optionalCustomer.isEmpty());
        verify(mockDao, times(1)).update(1,req);
    }
}