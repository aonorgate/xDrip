package com.eveningoutpost.dexdrip.carblookup;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ListView;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.carblookup.db.FavoriteItemRepository;
import com.eveningoutpost.dexdrip.carblookup.model.FavoriteItem;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

public class FavoriteItemsActivityTest extends RobolectricTestWithConfig {

    @Before
    public void resetOverrides() {
        TestFavoriteItemsActivity.favoriteItemRepositoryOverride = null;
    }

    @Test
    public void tappingFavoriteRow_returnsFavoriteForCurrentMeal() {
        FavoriteItemRepository repository = mock(FavoriteItemRepository.class);
        when(repository.getAll()).thenAnswer(invocation -> new ArrayList<>(
                Collections.singletonList(favorite())));
        TestFavoriteItemsActivity.favoriteItemRepositoryOverride = repository;

        Intent intent = new Intent();
        intent.putExtra(FavoriteItemsActivity.EXTRA_PICK_MODE, true);
        TestFavoriteItemsActivity activity = Robolectric.buildActivity(
                        TestFavoriteItemsActivity.class,
                        intent)
                .create()
                .start()
                .resume()
                .get();

        ListView listView = activity.findViewById(R.id.favoritesListView);
        View row = listView.getAdapter().getView(0, null, listView);
        row.performClick();

        Intent result = shadowOf(activity).getResultIntent();
        assertEquals(Activity.RESULT_OK, shadowOf(activity).getResultCode());
        assertNotNull(result);
        assertEquals(7L, result.getLongExtra(FavoriteItemsActivity.EXTRA_FAVORITE_ID, 0L));
        assertEquals("Toast", result.getStringExtra(FavoriteItemsActivity.EXTRA_PRODUCT_NAME));
        assertEquals("Bakery", result.getStringExtra(FavoriteItemsActivity.EXTRA_BRAND));
        assertEquals("12345", result.getStringExtra(FavoriteItemsActivity.EXTRA_BARCODE));
        assertEquals(45.0, result.getDoubleExtra(FavoriteItemsActivity.EXTRA_CARBS_PER_100G, 0.0), 0.01);
        assertEquals(40.0, result.getDoubleExtra(FavoriteItemsActivity.EXTRA_PORTION_GRAMS, 0.0), 0.01);
    }

    private FavoriteItem favorite() {
        FavoriteItem item = new FavoriteItem();
        item.id = 7L;
        item.productName = "Toast";
        item.brand = "Bakery";
        item.barcode = "12345";
        item.carbsPer100g = 45.0;
        item.defaultPortionGrams = 40.0;
        return item;
    }

    public static class TestFavoriteItemsActivity extends FavoriteItemsActivity {
        static FavoriteItemRepository favoriteItemRepositoryOverride;

        @Override
        protected FavoriteItemRepository createFavoriteItemRepository() {
            return favoriteItemRepositoryOverride != null
                    ? favoriteItemRepositoryOverride : super.createFavoriteItemRepository();
        }
    }
}