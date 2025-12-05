package com.billy.database;

import com.billy.objects.Customer;
import com.billy.objects.CustomerUpdateRequest;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CustomerDAOTest {
    private static final String TEST_DB_FILE = "test.db";
    private static MapDbWrapper dbWrapper;
    private CustomerDAO customerDAO;

    @BeforeAll
    static void setupAll() {
        dbWrapper = new MapDbWrapper(TEST_DB_FILE);
    }

    @AfterAll
    static void tearDownAll() {
        if (dbWrapper != null) {
            dbWrapper.close();
        }

        File file = new File(TEST_DB_FILE);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                System.err.println("Failed to delete test DB file: " + TEST_DB_FILE);
            }
        }
    }

    @BeforeEach
    void setup() {
        customerDAO = new CustomerDAO(dbWrapper);
    }

    @AfterEach
    void cleanup() {
        // Clear the map after each test
        dbWrapper.db().treeMap(CustomerDAO.TABLE_NAME).createOrOpen().clear();
        dbWrapper.commit();
    }

    @Test
    void testSaveAndFind() {
        Customer c = new Customer(-1, "John", "Doe", "male", "john.doe@example.com");
        Optional<Customer> savedOptional = customerDAO.save(c);
        assertTrue(savedOptional.isPresent());
        Customer saved = savedOptional.get();
        assertNotNull(saved);
        assertTrue(saved.id() > 0);

        Optional<Customer> foundOptional = customerDAO.find(saved.id());
        assertTrue(foundOptional.isPresent());
        Customer found = foundOptional.get();
        assertEquals("John", found.name());
        assertEquals("Doe", found.lastName());
        assertEquals("male", found.gender());
        assertEquals("john.doe@example.com", found.email());
    }

    @Test
    void testDelete() {
        Customer c = new Customer(0, "Alice", "Smith", "female", "alice@example.com");
        Optional<Customer> savedOptional = customerDAO.save(c);
        assertTrue(savedOptional.isPresent());
        Customer saved = savedOptional.get();

        boolean deleted = customerDAO.delete(saved.id());
        assertTrue(deleted);

        Optional<Customer> foundOptional = customerDAO.find(saved.id());
        assertTrue(foundOptional.isEmpty());
    }

    @Test
    void testIteratorAll() {
        Optional<Customer> c1Optional = customerDAO.save(new Customer(0, "John", "Doe", "male", "john@example.com"));
        Optional<Customer> c2Optional = customerDAO.save(new Customer(0, "Alice", "Smith", "female", "alice@example.com"));

        int count = 0;
        for (Iterator<Customer> it = customerDAO.iteratorAllCustomers(); it.hasNext(); ) {
            Customer c = it.next();
            assertNotNull(c);
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    void testUpdate() {
        Optional<Customer> savedOptional = customerDAO.save(new Customer(0, "John", "Doe", "male", "john@example.com"));
        assertTrue(savedOptional.isPresent());
        Customer saved = savedOptional.get();


        CustomerUpdateRequest updateRequest = new CustomerUpdateRequest(
                saved.id(),
                Optional.of("Johnny"),
                Optional.empty(),
                Optional.empty(),
                Optional.of("johnny@example.com")
        );

        Optional<Customer> updatedOpt = customerDAO.update(saved.id(),updateRequest);
        assertTrue(updatedOpt.isPresent());

        Customer updated = updatedOpt.get();
        assertEquals("Johnny", updated.name());
        assertEquals("Doe", updated.lastName());
        assertEquals("male", updated.gender());
        assertEquals("johnny@example.com", updated.email());
    }

    @Test
    void testFindInvalidId() {
        Optional<Customer> foundOptional = customerDAO.find(-2);
        assertTrue(foundOptional.isEmpty());
    }
}