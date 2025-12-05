package com.billy.api;

import io.undertow.server.RoutingHandler;
import io.undertow.util.Methods;

public class CustomerRouter {

    public static final String CUSTOMERS_BASE = "/customers";
    public static void register(RoutingHandler router, CustomerHandler customerHandler) {
        router.get(CUSTOMERS_BASE, customerHandler::getAllCustomers);
        router.get(CUSTOMERS_BASE + "/{id}", customerHandler::getCustomerById);
        router.post(CUSTOMERS_BASE, customerHandler::createCustomer);
        router.delete(CUSTOMERS_BASE + "/{id}", customerHandler::deleteCustomer);
        router.add(Methods.PATCH, CUSTOMERS_BASE + "/{id}", customerHandler::updateCustomer);
    }
}