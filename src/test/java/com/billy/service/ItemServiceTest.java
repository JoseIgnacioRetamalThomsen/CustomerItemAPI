package com.billy.service;

import com.billy.database.ItemDAO;
import com.billy.objects.Item;
import com.billy.objects.ItemUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ItemServiceTest {

    private ItemDAO dao;
    private ItemService service;

    @BeforeEach
    void setUp() {
        dao = mock(ItemDAO.class);
        service = new ItemService(dao);
    }

    @Test
    void testCreateItem() {
        Item item = new Item(null, "Box", 10, 5, "Red");
        Item savedItem = new Item(1L, item);

        when(dao.save(item)).thenReturn(Optional.of(savedItem));

        Optional<Item> result = service.createItem(item);
        assertTrue(result.isPresent());
        assertEquals(savedItem, result.get());
        verify(dao).save(item);
    }

    @Test
    void testGetItemById() {
        Item item = new Item(1L, "Box", 10, 5, "Red");
        when(dao.find(1L)).thenReturn(Optional.of(item));

        Optional<Item> result = service.getItemById(1L);
        assertTrue(result.isPresent());
        assertEquals(item, result.get());
        verify(dao).find(1L);
    }

    @Test
    void testGetItemByIdNotFound() {
        when(dao.find(99L)).thenReturn(Optional.empty());

        Optional<Item> result = service.getItemById(99L);
        assertFalse(result.isPresent());
        verify(dao).find(99L);
    }

    @Test
    void testIteratorAllItems() {
        Iterator<Item> iterator = Arrays.asList(
                new Item(1L, "Box", 10, 5, "Red"),
                new Item(2L, "Ball", 5, 1, "Blue")
        ).iterator();

        when(dao.iteratorAllItems()).thenReturn(iterator);

        Iterator<Item> result = service.iteratorAllItems();
        assertTrue(result.hasNext());
        verify(dao).iteratorAllItems();
    }

    @Test
    void testUpdateItem() {
        ItemUpdateRequest updateRequest = new ItemUpdateRequest(
                Optional.of("BoxUpdated"),
                Optional.empty(),
                Optional.empty(),
                Optional.of("Green")
        );

        Item updatedItem = new Item(1L, "BoxUpdated", 10, 5, "Green");

        when(dao.update(1L, updateRequest)).thenReturn(Optional.of(updatedItem));

        Optional<Item> result = service.updateItem(1L, updateRequest);
        assertTrue(result.isPresent());
        assertEquals(updatedItem, result.get());
        verify(dao).update(1L, updateRequest);
    }

    @Test
    void testUpdateItemNotFound() {
        ItemUpdateRequest updateRequest = new ItemUpdateRequest(
                Optional.of("NonExisting"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()
        );

        when(dao.update(99L, updateRequest)).thenReturn(Optional.empty());

        Optional<Item> result = service.updateItem(99L, updateRequest);
        assertFalse(result.isPresent());
        verify(dao).update(99L, updateRequest);
    }

    @Test
    void testDeleteItem() {
        when(dao.delete(1L)).thenReturn(true);

        boolean result = service.deleteItem(1L);
        assertTrue(result);
        verify(dao).delete(1L);
    }

    @Test
    void testDeleteItemNotFound() {
        when(dao.delete(99L)).thenReturn(false);

        boolean result = service.deleteItem(99L);
        assertFalse(result);
        verify(dao).delete(99L);
    }
}
