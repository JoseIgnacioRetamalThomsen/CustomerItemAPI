package com.billy.service;

import com.billy.database.ItemDAO;
import com.billy.objects.Item;
import com.billy.objects.ItemUpdateRequest;

import java.util.Iterator;
import java.util.Optional;

/**
 * Service layer for managing {@link Item} entities.
 * Provides CRUD operations and delegates persistence to {@link ItemDAO}.
 */
public class ItemService {

    private final ItemDAO dao;

    public ItemService(ItemDAO dao) {
        this.dao = dao;
    }

    /**
     * Returns an iterator over all items.
     * Useful for streaming large datasets without loading everything in memory.
     *
     * @return an iterator of Item objects
     */
    public Iterator<Item> iteratorAllItems() {
        return dao.iteratorAllItems();
    }

    /**
     * Finds an item by its ID.
     *
     * @param id the ID of the item
     * @return an Optional containing the Item if found, or empty if not
     */
    public Optional<Item> getItemById(long id) {
        return dao.find(id);
    }

    /**
     * Creates a new item.
     *
     * @param item the Item to create
     * @return the created Item with assigned ID
     */
    public Optional<Item> createItem(Item item) {
        return dao.save(item);
    }

    /**
     * Deletes an item by ID.
     *
     * @param id the ID of the item to delete
     * @return true if the item was deleted, false if not found
     */
    public boolean deleteItem(long id) {
        return dao.delete(id);
    }

    /**
     * Updates an existing item using the provided update request.
     *
     * @param idToUpdate the ID of the item to update
     * @param req        the ItemUpdateRequest containing updates
     * @return an Optional containing the updated Item if successful, empty if not found
     */
    public Optional<Item> updateItem(long idToUpdate, ItemUpdateRequest req) {
        return dao.update(idToUpdate, req);
    }
}
