package com.eveningoutpost.dexdrip.carblookup;

import com.eveningoutpost.dexdrip.carblookup.model.MealItem;
import com.eveningoutpost.dexdrip.carblookup.utils.CarbLookupCalculator;

import java.util.ArrayList;
import java.util.List;

class CurrentMeal {
    private final List<MealItem> items = new ArrayList<>();
    private int editingItemIndex = -1;

    List<MealItem> getItems() {
        return items;
    }

    boolean isEmpty() {
        return items.isEmpty();
    }

    void beginNewItem() {
        editingItemIndex = -1;
    }

    boolean beginEditItem(int position) {
        if (position < 0 || position >= items.size()) {
            return false;
        }
        editingItemIndex = position;
        return true;
    }

    MealItem getItem(int position) {
        if (position < 0 || position >= items.size()) {
            return null;
        }
        return items.get(position);
    }

    boolean addOrUpdate(MealItem item) {
        if (editingItemIndex >= 0 && editingItemIndex < items.size()) {
            items.set(editingItemIndex, item);
            editingItemIndex = -1;
            return true;
        }
        items.add(item);
        return false;
    }

    void add(MealItem item) {
        items.add(item);
    }

    MealItem remove(int position) {
        if (position < 0 || position >= items.size()) {
            return null;
        }
        MealItem removedItem = items.remove(position);
        if (editingItemIndex == position) {
            editingItemIndex = -1;
        } else if (editingItemIndex > position) {
            editingItemIndex--;
        }
        return removedItem;
    }

    void addAll(List<MealItem> newItems) {
        if (newItems != null && !newItems.isEmpty()) {
            items.addAll(newItems);
        }
    }

    void clear() {
        items.clear();
        editingItemIndex = -1;
    }

    void cancelEdit() {
        editingItemIndex = -1;
    }

    double totalCarbs() {
        return CarbLookupCalculator.totalMealCarbs(items);
    }
}