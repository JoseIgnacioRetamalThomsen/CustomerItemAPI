package com.billy.objects;

import java.io.Serializable;

public record Item(Long id, String name, double size, double weight, String color) implements Serializable {
    private static final long serialVersionUID = 1L;

    public Item(Long id, Item item) {
        this(id, item.name, item.size, item.weight, item.color);
    }
}
