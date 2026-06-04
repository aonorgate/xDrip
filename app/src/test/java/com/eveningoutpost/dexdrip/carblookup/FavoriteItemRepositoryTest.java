package com.eveningoutpost.dexdrip.carblookup;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.carblookup.db.CarbLookupDatabase;
import com.eveningoutpost.dexdrip.carblookup.db.FavoriteItemRepository;
import com.eveningoutpost.dexdrip.carblookup.model.FavoriteItem;

import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class FavoriteItemRepositoryTest extends RobolectricTestWithConfig {

    @Test
    public void saveGetAllIncrementAndDelete_roundTripsFavoriteItems() {
        CarbLookupDatabase database = CarbLookupDatabase.createInMemoryInstance(
                RuntimeEnvironment.getApplication());
        FavoriteItemRepository repository = new FavoriteItemRepository(database);

        try {
            FavoriteItem banana = favorite("Banana", null, null, 23.0, 118.0);
            FavoriteItem toast = favorite("Toast", "Bakery", "12345", 45.0, 40.0);
            long bananaId = repository.save(banana);
            long toastId = repository.save(toast);

            repository.incrementUseCount(toastId);
            repository.incrementUseCount(toastId);
            repository.incrementUseCount(bananaId);

            List<FavoriteItem> favorites = repository.getAll();
            assertEquals(2, favorites.size());
            assertEquals("Toast", favorites.get(0).productName);
            assertEquals("Bakery", favorites.get(0).brand);
            assertEquals("12345", favorites.get(0).barcode);
            assertEquals(45.0, favorites.get(0).carbsPer100g, 0.01);
            assertEquals(40.0, favorites.get(0).defaultPortionGrams, 0.01);
            assertEquals(2, favorites.get(0).useCount);
            assertEquals("Banana", favorites.get(1).productName);
            assertEquals(1, favorites.get(1).useCount);

            FavoriteItem updatedBanana = favorites.get(1);
            updatedBanana.productName = "Large banana";
            updatedBanana.defaultPortionGrams = 130.0;
            repository.update(updatedBanana);
            repository.resetUseCount(bananaId);

            favorites = repository.getAll();
            assertEquals("Toast", favorites.get(0).productName);
            assertEquals("Large banana", favorites.get(1).productName);
            assertEquals(130.0, favorites.get(1).defaultPortionGrams, 0.01);
            assertEquals(0, favorites.get(1).useCount);

            repository.delete(toastId);
            favorites = repository.getAll();
            assertEquals(1, favorites.size());
            assertEquals("Large banana", favorites.get(0).productName);
        } finally {
            database.close();
        }
    }

    private FavoriteItem favorite(String name, String brand, String barcode,
            double carbsPer100g, double defaultPortionGrams) {
        FavoriteItem item = new FavoriteItem();
        item.productName = name;
        item.brand = brand;
        item.barcode = barcode;
        item.carbsPer100g = carbsPer100g;
        item.defaultPortionGrams = defaultPortionGrams;
        return item;
    }
}
