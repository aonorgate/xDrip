package com.eveningoutpost.dexdrip.carblookup.db;

import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.carblookup.api.FoodDbSource;
import com.eveningoutpost.dexdrip.carblookup.api.ProductData;

import org.junit.After;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ProductCacheRepositoryTest extends RobolectricTestWithConfig {
    private final List<CarbLookupDatabase> testDatabases = new ArrayList<>();

    @After
    public void closeTestDatabases() {
        for (CarbLookupDatabase database : testDatabases) {
            database.close();
        }
        testDatabases.clear();
    }

    @Test
    public void lookup_separatesProductsBySource() {
        ProductCacheRepository repository = createRepository();
        ProductData.Product worldProduct = product("World Rice", 28.0);
        ProductData.Product usProduct = product("US Rice", 28.2);

        repository.store(FoodDbSource.WORLD.key(), "123", worldProduct);
        repository.store(FoodDbSource.US.key(), "123", usProduct);

        ProductCacheRepository.CachedProduct worldCached = repository.lookup(FoodDbSource.WORLD.key(), "123");
        ProductCacheRepository.CachedProduct usCached = repository.lookup(FoodDbSource.US.key(), "123");

        assertNotNull(worldCached);
        assertNotNull(usCached);
        assertEquals("World Rice", worldCached.productName);
        assertEquals("US Rice", usCached.productName);
        assertEquals(28.0, worldCached.carbsPer100g, 0.01);
        assertEquals(28.2, usCached.carbsPer100g, 0.01);
    }

    @Test
    public void giOverride_isScopedBySource() {
        ProductCacheRepository repository = createRepository();
        repository.store(FoodDbSource.WORLD.key(), "123", product("World Rice", 28.0));
        repository.store(FoodDbSource.US.key(), "123", product("US Rice", 28.2));

        repository.updateGiOverride(FoodDbSource.US.key(), "123", 73);

        assertEquals(0, repository.getGiOverride(FoodDbSource.WORLD.key(), "123"));
        assertEquals(73, repository.getGiOverride(FoodDbSource.US.key(), "123"));
    }

    private ProductCacheRepository createRepository() {
        CarbLookupDatabase database = CarbLookupDatabase.createInMemoryInstance(
                RuntimeEnvironment.getApplication());
        testDatabases.add(database);
        return new ProductCacheRepository(database);
    }

    private ProductData.Product product(String name, double carbsPer100g) {
        ProductData.Product product = new ProductData.Product();
        product.productName = name;
        product.brands = "Test";
        product.servingSize = "100g";
        product.nutriments = new ProductData.Nutriments();
        product.nutriments.carbohydrates100g = carbsPer100g;
        return product;
    }
}