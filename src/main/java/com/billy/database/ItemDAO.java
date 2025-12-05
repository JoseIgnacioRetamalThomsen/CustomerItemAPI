package com.billy.database;

import com.billy.objects.Item;
import com.billy.objects.ItemUpdateRequest;
import org.mapdb.Atomic;
import org.mapdb.BTreeMap;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import java.util.Iterator;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data Access Object (DAO) for Item entities using MapDB.
 */
public class ItemDAO {
    private static final Logger logger = Logger.getLogger(ItemDAO.class.getName());
    public static final String TABLE_NAME = "items";
    public static final String SEQUENCE_NAME = "item_seq";
    private final BTreeMap<Long, Item> items;
    private final Atomic.Long itemIdSeq;

    public ItemDAO(MapDbWrapper dbWrapper) {
        itemIdSeq = dbWrapper.db().atomicLong(SEQUENCE_NAME).createOrOpen();

        @SuppressWarnings("unchecked")
        BTreeMap<Long, Item> map = (BTreeMap<Long, Item>) dbWrapper.db()
                .treeMap(TABLE_NAME, Serializer.LONG, Serializer.JAVA)
                .createOrOpen();
        this.items = map;
    }

    /**
     * Saves a new item and assigns a unique ID.
     *
     * @param item the item to save
     * @return the saved item, or empty if failed
     */
    public Optional<Item> save(Item item) {
        try {

            if (item == null) {
                return Optional.empty();
            }
            long id = itemIdSeq.incrementAndGet();
            Item itemWithId = new Item(id, item);
            items.put(id, itemWithId);
            return Optional.of(itemWithId);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save item ", e);
            return Optional.empty();
        }
    }

    /**
     * Finds an item by its ID.
     *
     * @param id the item ID
     * @return the found item, or empty if not found
     */
    public Optional<Item> find(long id) {
        try {
            return Optional.ofNullable(items.get(id));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to find item " + id, e);
            return Optional.empty();
        }
    }

    /**
     * Deletes an item by its ID.
     *
     * @param id the item ID
     * @return true if deleted, false otherwise
     */
    public boolean delete(long id) {
        try {
            return items.remove(id) != null;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to delete item " + id, e);
            return false;
        }
    }

    /**
     * Returns an iterator over all items.
     *
     * @return iterator for streaming all items
     */
    public Iterator<Item> iteratorAllItems() {
        return items.values().iterator();
    }

    /**
     * Updates an existing item by its ID.
     *
     * @param idToUpdate the ID of the item to update
     * @param updated    the update request with new field values
     * @return the updated item, or empty if not found
     */
    public Optional<Item> update(long idToUpdate, ItemUpdateRequest updated) {
        if (updated == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(
                    items.computeIfPresent(idToUpdate, (id, existing) -> new Item(
                            existing.id(),
                            updated.name().orElse(existing.name()),
                            updated.size().orElse(existing.size()),
                            updated.weight().orElse(existing.weight()),
                            updated.color().orElse(existing.color())
                    ))
            );
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to update item " + idToUpdate, e);
            return Optional.empty();
        }

    }
}
