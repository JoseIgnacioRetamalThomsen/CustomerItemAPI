package com.billy.database;

import com.billy.objects.Item;
import com.billy.objects.ItemUpdateRequest;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ItemDAOTest {

    public static final String TEST_DB_FILE = "test_db";
    private static MapDbWrapper dbWrapper;
    private ItemDAO itemDAO;

    @BeforeAll
    static void setupDb() {
        dbWrapper = new MapDbWrapper(TEST_DB_FILE);
    }

    @AfterAll
    static void closeDb() {
        dbWrapper.close();

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
        itemDAO = new ItemDAO(dbWrapper);
    }

    @Test
    void testSaveAndFind() {
        Item item = new Item(null, "Sword", 10, 5, "Silver");
        Optional<Item> saved = itemDAO.save(item);

        assertTrue(saved.isPresent());
        Item savedItem = saved.get();
        assertNotNull(savedItem.id());
        assertEquals("Sword", savedItem.name());

        Optional<Item> found = itemDAO.find(savedItem.id());
        assertTrue(found.isPresent());
        assertEquals(savedItem, found.get());
    }

    @Test
    void testUpdate() {
        Item item = new Item(null, "Shield", 15, 10, "Black");
        Item savedItem = itemDAO.save(item).get();

        ItemUpdateRequest updateRequest = new ItemUpdateRequest(
                Optional.of("Golden Shield"),
                Optional.of(20.0),
                Optional.empty(),
                Optional.of("Gold")
        );

        Optional<Item> updatedOpt = itemDAO.update(savedItem.id(), updateRequest);
        assertTrue(updatedOpt.isPresent());

        Item updated = updatedOpt.get();
        assertEquals("Golden Shield", updated.name());
        assertEquals(20, updated.size());
        assertEquals(10, updated.weight()); // unchanged
        assertEquals("Gold", updated.color());
    }

    @Test
    void testDelete() {
        Item item = new Item(null, "Potion", 1, 1, "Red");
        Item savedItem = itemDAO.save(item).get();

        boolean deleted = itemDAO.delete(savedItem.id());
        assertTrue(deleted);

        Optional<Item> found = itemDAO.find(savedItem.id());
        assertFalse(found.isPresent());
    }

    @Test
    void testIteratorAll() {
        itemDAO.save(new Item(null, "ItemX", 1, 1, "X"));
        itemDAO.save(new Item(null, "ItemY", 2, 2, "Y"));

        var iterator = itemDAO.iteratorAllItems();
        assertTrue(iterator.hasNext());
        iterator.forEachRemaining(item -> assertNotNull(item));
    }

    @Test
    void testUpdateNonExisting() {
        ItemUpdateRequest updateRequest = new ItemUpdateRequest(
                Optional.of("Ghost Item"), Optional.empty(), Optional.empty(), Optional.empty()
        );
        Optional<Item> result = itemDAO.update(999L, updateRequest);
        assertFalse(result.isPresent());
    }

    @Test
    void testSaveNull() {
        Optional<Item> result = itemDAO.save(null);
        assertFalse(result.isPresent());
    }
}
