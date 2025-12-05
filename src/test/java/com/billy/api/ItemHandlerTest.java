package com.billy.api;

import com.billy.common.JsonUtils;
import com.billy.objects.Item;
import com.billy.service.ItemService;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class ItemHandlerTest {

    private static Undertow server;
    private static int port = 8083;
    private static ItemService itemService;
    private static ItemHandler itemHandler;

    private static RoutingHandler routingHandler;

    @BeforeAll
    static void startServer() {
        itemService = mock(ItemService.class);
        itemHandler = new ItemHandler(itemService);
        routingHandler = Handlers.routing();
        ItemRouter.register(routingHandler, itemHandler);
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

    @Test
    void testGetAllItemsHttpRequest() throws IOException {
        when(itemService.iteratorAllItems()).thenReturn(java.util.List.of(
                new Item(1L, "Item1", 10, 99.99, "RED"),
                new Item(2L, "Item2", 5, 49.99, "GREEN")
        ).iterator());

        URL url = new URL("http://localhost:" + port + "/items");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");

        assertEquals(200, connection.getResponseCode());

        String response = new String(connection.getInputStream().readAllBytes());
        assertTrue(response.contains("Item1"));
        assertTrue(response.contains("Item2"));
        assertTrue(response.startsWith("["));
        assertTrue(response.endsWith("]"));
    }

    @Test
    void testGetItemById() throws IOException {
        Item item = new Item(1L, "Item1", 10, 99.99, "RED");
        when(itemService.getItemById(1L)).thenReturn(Optional.of(item));

        URL url = new URL("http://localhost:" + port + "/items/1");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        assertEquals(200, conn.getResponseCode());
        String response = new String(conn.getInputStream().readAllBytes());
        assertTrue(response.contains("Item1"));
    }

    @Test
    void testCreateItem() throws IOException {
        Item item = new Item(null, "NewItem", 10, 99.99, "RED");
        Item savedItem = new Item(1L, "NewItem", 10, 99.99, "BLUE");
        when(itemService.createItem(any())).thenReturn(Optional.of(savedItem));

        URL url = new URL("http://localhost:" + port + "/items");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.getOutputStream().write(JsonUtils.toJson(item).getBytes());

        assertEquals(201, conn.getResponseCode());
        String response = new String(conn.getInputStream().readAllBytes());
        assertTrue(response.contains("NewItem"));
    }

    @Test
    void testDeleteItem() throws IOException {
        when(itemService.deleteItem(1L)).thenReturn(true);

        URL url = new URL("http://localhost:" + port + "/items/1");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setDoOutput(true);
        conn.getOutputStream().write("id=1".getBytes());

        assertEquals(200, conn.getResponseCode());
        String response = new String(conn.getInputStream().readAllBytes());
        assertTrue(response.contains("1"));
    }
}
