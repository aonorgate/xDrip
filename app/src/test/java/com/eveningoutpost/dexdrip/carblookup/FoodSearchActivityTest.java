package com.eveningoutpost.dexdrip.carblookup;

import android.app.Activity;
import android.content.Intent;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.RobolectricTestWithConfig;
import com.eveningoutpost.dexdrip.carblookup.api.FoodDbSource;
import com.eveningoutpost.dexdrip.carblookup.api.OpenFoodFactsClient;
import com.eveningoutpost.dexdrip.carblookup.api.ProductData;

import org.junit.Before;
import org.junit.Test;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.robolectric.Shadows.shadowOf;

public class FoodSearchActivityTest extends RobolectricTestWithConfig {

    @Before
    public void resetOverrides() {
        TestFoodSearchActivity.clientOverride = null;
        TestFoodSearchActivity.sourceOverride = FoodDbSource.UK;
    }

    @Test
    public void typingSearchShowsFilteredOnlineResultsAndReturnsSelectedBarcode() {
        TestFoodSearchActivity.clientOverride = new OpenFoodFactsClient() {
            @Override
            public void searchProducts(String query, int pageSize, SearchCallback callback) {
                assertEquals("rice pudding", query);
                assertEquals(20, pageSize);
                callback.onSuccess(Collections.singletonList(buildProduct()));
            }
        };

        TestFoodSearchActivity activity = Robolectric.buildActivity(TestFoodSearchActivity.class)
                .create()
                .start()
                .resume()
                .get();

        EditText search = activity.findViewById(R.id.itemSearchEditText);
        ListView results = activity.findViewById(R.id.itemSearchResultsListView);
        TextView status = activity.findViewById(R.id.itemSearchStatusTextView);

        search.setText("r");
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals("Type at least 2 characters", status.getText().toString());
        assertEquals(0, results.getAdapter().getCount());

        search.setText("  rice   pudding  ");
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("1 result(s)", status.getText().toString());
        assertEquals(1, results.getAdapter().getCount());
        View row = results.getAdapter().getView(0, null, results);
        assertNotNull(row.getBackground());
        assertEquals("Rice pudding",
                ((TextView) row.findViewById(R.id.foodSearchNameTextView)).getText().toString());
        assertEquals("Kitchen Co",
                ((TextView) row.findViewById(R.id.foodSearchBrandTextView)).getText().toString());
        assertEquals("18.5g carbs per 100g",
                ((TextView) row.findViewById(R.id.foodSearchCarbsTextView)).getText().toString());

        results.performItemClick(row, 0, 0L);

        Intent result = shadowOf(activity).getResultIntent();
        assertEquals(Activity.RESULT_OK, shadowOf(activity).getResultCode());
        assertNotNull(result);
        assertEquals("1234567890123", result.getStringExtra(FoodSearchActivity.EXTRA_BARCODE));
    }

    @Test
    public void defaultSearchDelay_waitsForTwoSecondsOfTypingPause() {
        DefaultDelayFoodSearchActivity activity = Robolectric.buildActivity(DefaultDelayFoodSearchActivity.class)
                .create()
                .start()
                .resume()
                .get();

        assertEquals(2000L, activity.exposedSearchDelayMs());
    }

    @Test
    public void searchErrorClearsResultsAndShowsError() {
        TestFoodSearchActivity.clientOverride = new OpenFoodFactsClient() {
            @Override
            public void searchProducts(String query, int pageSize, SearchCallback callback) {
                callback.onError("Network unavailable");
            }
        };

        TestFoodSearchActivity activity = Robolectric.buildActivity(TestFoodSearchActivity.class)
                .create()
                .start()
                .resume()
                .get();

        ((EditText) activity.findViewById(R.id.itemSearchEditText)).setText("rice");
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals("Error: Network unavailable",
                ((TextView) activity.findViewById(R.id.itemSearchStatusTextView)).getText().toString());
        assertEquals(0, ((ListView) activity.findViewById(R.id.itemSearchResultsListView)).getAdapter().getCount());
    }

    @Test
    public void staleSearchResult_isIgnoredAfterQueryChanges() {
        DeferredSearchClient client = new DeferredSearchClient();
        TestFoodSearchActivity.clientOverride = client;

        TestFoodSearchActivity activity = Robolectric.buildActivity(TestFoodSearchActivity.class)
                .create()
                .start()
                .resume()
                .get();

        EditText search = activity.findViewById(R.id.itemSearchEditText);
        ListView results = activity.findViewById(R.id.itemSearchResultsListView);
        TextView status = activity.findViewById(R.id.itemSearchStatusTextView);

        search.setText("rice");
        shadowOf(Looper.getMainLooper()).idle();
        search.setText("pasta");
        shadowOf(Looper.getMainLooper()).idle();

        client.respond("rice", Collections.singletonList(buildProduct("111", "Old rice")));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(0, results.getAdapter().getCount());
        assertEquals("Searching...", status.getText().toString());

        client.respond("pasta", Collections.singletonList(buildProduct("222", "Fresh pasta")));
        shadowOf(Looper.getMainLooper()).idle();
        assertEquals(1, results.getAdapter().getCount());
        View row = results.getAdapter().getView(0, null, results);
        assertEquals("Fresh pasta",
                ((TextView) row.findViewById(R.id.foodSearchNameTextView)).getText().toString());
    }

    @Test
    public void selectingResultWithoutBarcode_showsErrorAndKeepsSearchOpen() {
        TestFoodSearchActivity.clientOverride = new OpenFoodFactsClient() {
            @Override
            public void searchProducts(String query, int pageSize, SearchCallback callback) {
                callback.onSuccess(Collections.singletonList(buildProduct("", "Rice pudding")));
            }
        };

        TestFoodSearchActivity activity = Robolectric.buildActivity(TestFoodSearchActivity.class)
                .create()
                .start()
                .resume()
                .get();
        EditText search = activity.findViewById(R.id.itemSearchEditText);
        ListView results = activity.findViewById(R.id.itemSearchResultsListView);

        search.setText("rice");
        shadowOf(Looper.getMainLooper()).idle();
        View row = results.getAdapter().getView(0, null, results);
        results.performItemClick(row, 0, 0L);

        assertEquals("Barcode not found", ShadowToast.getTextOfLatestToast());
        assertFalse(activity.isFinishing());
        assertEquals(Activity.RESULT_CANCELED, shadowOf(activity).getResultCode());
    }

    private ProductData.Product buildProduct() {
        return buildProduct("1234567890123", "Rice pudding");
    }

    private ProductData.Product buildProduct(String code, String name) {
        ProductData.Product product = new ProductData.Product();
        product.code = code;
        product.productName = name;
        product.brands = "Kitchen Co";
        product.nutriments = new ProductData.Nutriments();
        product.nutriments.carbohydrates100g = 18.5;
        return product;
    }

    private static class DeferredSearchClient extends OpenFoodFactsClient {
        private final List<Request> requests = new ArrayList<>();

        @Override
        public void searchProducts(String query, int pageSize, SearchCallback callback) {
            requests.add(new Request(query, callback));
        }

        void respond(String query, List<ProductData.Product> products) {
            for (Request request : requests) {
                if (query.equals(request.query)) {
                    request.callback.onSuccess(products);
                    return;
                }
            }
            throw new AssertionError("Missing deferred search request: " + query);
        }

        private static class Request {
            final String query;
            final SearchCallback callback;

            Request(String query, SearchCallback callback) {
                this.query = query;
                this.callback = callback;
            }
        }
    }

    public static class TestFoodSearchActivity extends FoodSearchActivity {
        static OpenFoodFactsClient clientOverride;
        static FoodDbSource sourceOverride;

        @Override
        protected FoodDbSource createFoodDbSource() {
            return sourceOverride != null ? sourceOverride : super.createFoodDbSource();
        }

        @Override
        protected OpenFoodFactsClient createOpenFoodFactsClient(FoodDbSource source) {
            return clientOverride != null ? clientOverride : super.createOpenFoodFactsClient(source);
        }

        @Override
        protected long getSearchDelayMs() {
            return 0L;
        }
    }

    public static class DefaultDelayFoodSearchActivity extends FoodSearchActivity {
        long exposedSearchDelayMs() {
            return getSearchDelayMs();
        }

        @Override
        protected FoodDbSource createFoodDbSource() {
            return FoodDbSource.UK;
        }

        @Override
        protected OpenFoodFactsClient createOpenFoodFactsClient(FoodDbSource source) {
            return new OpenFoodFactsClient() {
                @Override
                public void searchProducts(String query, int pageSize, SearchCallback callback) {
                    callback.onSuccess(Collections.emptyList());
                }
            };
        }
    }
}
