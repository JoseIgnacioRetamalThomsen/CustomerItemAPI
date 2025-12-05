package com.billy.api;

import io.undertow.server.RoutingHandler;
import io.undertow.util.Methods;

public class ItemRouter {

    public static final String BASE_ITEMS = "/items";

    public static void register(RoutingHandler router, ItemHandler itemHandler) {
        router.get(BASE_ITEMS, itemHandler::getAllItems);
        router.post(BASE_ITEMS, itemHandler::createItem);
        router.get(BASE_ITEMS + "/{id}", itemHandler::getItemById);
        router.add(Methods.PATCH, BASE_ITEMS + "/{id}", itemHandler::updateItem);
        router.delete(BASE_ITEMS + "/{id}", itemHandler::deleteItem);
    }
}
