package com.billy.api;

import com.billy.common.JsonUtils;
import com.billy.objects.Customer;
import com.billy.objects.Response;
import com.billy.service.CustomerService;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerHandlerTest {

    private static Undertow server;
    private static int port = 8082;
    private static CustomerService service;
    private static CustomerHandler handler;

    private static  RoutingHandler routingHandler;
    @BeforeAll
    static void startServer() {
        service = mock(CustomerService.class);
        handler = new CustomerHandler(service);
        routingHandler = Handlers.routing();
        CustomerRouter.register(routingHandler,handler);
        server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(routingHandler)
                .build();
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void resetMocks() {
        Mockito.reset(service);
    }

    @Test
    void testGetCustomerById_Found() throws Exception {
        Customer customer = new Customer(1, "Alice", "Castro", "female", "alice@gmail.com");
        when(service.getCustomerById(1L)).thenReturn(Optional.of(customer));

        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + port + "/customers/1").openConnection();
        conn.setRequestMethod("GET");

        assertEquals(200, conn.getResponseCode());

        try (InputStream in = conn.getInputStream()) {
            byte[] data = in.readAllBytes();
            Customer response = JsonUtils.fromJson(data, Customer.class);
            assertEquals(customer.id(), response.id());
            assertEquals(customer.name(), response.name());
        }
    }

    @Test
    void testGetCustomerById_NotFound() throws Exception {
        when(service.getCustomerById(99L)).thenReturn(Optional.empty());

        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + port + "/customers/99").openConnection();
        conn.setRequestMethod("GET");

        assertEquals(404, conn.getResponseCode());

        try (InputStream in = conn.getErrorStream()) {
            byte[] data = in.readAllBytes();
            Response response = JsonUtils.fromJson(data, Response.class);
            assertTrue(response.error().contains("Not found id=99"));
        }
    }

    @Test
    void testCreateUser_Success() throws Exception {
        Customer newCustomer = new Customer(1, "Alice", "Castro", "female", "alice@gmail.com");
        when(service.createCustomer(newCustomer)).thenReturn(Optional.of(newCustomer));

        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + port + "/customers").openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.getOutputStream().write(JsonUtils.toJson(newCustomer).getBytes());

        assertEquals(201, conn.getResponseCode());

        try (InputStream in = conn.getInputStream()) {
            byte[] data = in.readAllBytes();
            Customer response = JsonUtils.fromJson(data, Customer.class);
            assertEquals(newCustomer.id(), response.id());
            assertEquals(newCustomer.name(), response.name());
        }
    }

    @Test
    void testDeleteUser_Success() throws Exception {
        when(service.deleteCustomer(1L)).thenReturn(true);

        HttpURLConnection conn = (HttpURLConnection) new URL("http://localhost:" + port + "/customers/1").openConnection();
        conn.setRequestMethod("DELETE");

        assertEquals(200, conn.getResponseCode());

        try (InputStream in = conn.getInputStream()) {
            byte[] data = in.readAllBytes();
            Response response = JsonUtils.fromJson(data, Response.class);
            assertEquals(1, response.id());
            assertTrue(response.ok());
        }
    }
}
