package com.eveningoutpost.dexdrip.carblookup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.carblookup.api.FoodDbSource;
import com.eveningoutpost.dexdrip.carblookup.api.OpenFoodFactsClient;
import com.eveningoutpost.dexdrip.carblookup.api.ProductData;
import com.eveningoutpost.dexdrip.models.JoH;

import java.util.List;

public class FoodSearchActivity extends AppCompatActivity {
    public static final String EXTRA_BARCODE = "BARCODE";

    private static final int MIN_QUERY_LENGTH = 2;
    private static final long SEARCH_DELAY_MS = 2000L;
    private static final int SEARCH_PAGE_SIZE = 20;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private int searchGeneration;

    private EditText searchEditText;
    private TextView statusTextView;
    private FoodSearchResultAdapter resultAdapter;
    private FoodDbSource foodDbSource;
    private OpenFoodFactsClient openFoodFactsClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_food_search);
        JoH.fixActionBar(this);
        setTitle(R.string.carblookup_title_item_search);

        foodDbSource = createFoodDbSource();
    openFoodFactsClient = createOpenFoodFactsClient(foodDbSource);
        searchEditText = findViewById(R.id.itemSearchEditText);
        TextView sourceTextView = findViewById(R.id.itemSearchSourceTextView);
        statusTextView = findViewById(R.id.itemSearchStatusTextView);
        ListView resultsListView = findViewById(R.id.itemSearchResultsListView);

        resultAdapter = new FoodSearchResultAdapter(this);
        resultsListView.setAdapter(resultAdapter);
        resultsListView.setOnItemClickListener((parent, view, position, id) -> selectProduct(position));

        sourceTextView.setText(getString(R.string.carblookup_item_search_source_format, foodDbSource.label()));
        statusTextView.setText(R.string.carblookup_item_search_min_chars);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                scheduleSearch(s.toString());
            }
        });
    }

    private void scheduleSearch(String rawQuery) {
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
        }
        String query = normalizeSearchQuery(rawQuery);
        searchGeneration++;
        if (query.length() < MIN_QUERY_LENGTH) {
            resultAdapter.setProducts(null);
            statusTextView.setText(R.string.carblookup_item_search_min_chars);
            return;
        }

        final int generation = searchGeneration;
        pendingSearch = () -> {
            pendingSearch = null;
            searchNow(query, generation);
        };
        searchHandler.postDelayed(pendingSearch, getSearchDelayMs());
    }

    private void searchNow(String query, int generation) {
        statusTextView.setText(R.string.carblookup_item_search_searching);
        openFoodFactsClient.searchProducts(query, SEARCH_PAGE_SIZE,
                new OpenFoodFactsClient.SearchCallback() {
                    @Override
                    public void onSuccess(List<ProductData.Product> products) {
                        if (generation != searchGeneration) {
                            return;
                        }
                        resultAdapter.setProducts(products);
                        if (products == null || products.isEmpty()) {
                            statusTextView.setText(R.string.carblookup_item_search_no_results);
                        } else {
                            statusTextView.setText(getString(R.string.carblookup_item_search_results_format,
                                    products.size()));
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (generation != searchGeneration) {
                            return;
                        }
                        resultAdapter.setProducts(null);
                        statusTextView.setText(getString(R.string.carblookup_error_format, errorMessage));
                    }
                });
    }

    private String normalizeSearchQuery(String rawQuery) {
        return rawQuery == null ? "" : rawQuery.trim().replaceAll("\\s+", " ");
    }

    private void selectProduct(int position) {
        ProductData.Product product = resultAdapter.getItem(position);
        if (product == null || product.code == null || product.code.trim().isEmpty()) {
            Toast.makeText(this, R.string.carblookup_barcode_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent result = new Intent();
        result.putExtra(EXTRA_BARCODE, product.code);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    protected FoodDbSource createFoodDbSource() {
        return FoodDbSource.current();
    }

    protected OpenFoodFactsClient createOpenFoodFactsClient(FoodDbSource source) {
        return new OpenFoodFactsClient(source);
    }

    protected long getSearchDelayMs() {
        return SEARCH_DELAY_MS;
    }
}
