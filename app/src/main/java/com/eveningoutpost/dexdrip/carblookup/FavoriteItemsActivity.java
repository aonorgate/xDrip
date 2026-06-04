package com.eveningoutpost.dexdrip.carblookup;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.carblookup.db.FavoriteItemRepository;
import com.eveningoutpost.dexdrip.carblookup.model.FavoriteItem;
import com.eveningoutpost.dexdrip.models.JoH;

import java.util.List;

public class FavoriteItemsActivity extends AppCompatActivity {

    public static final String EXTRA_PICK_MODE = "PICK_MODE";
    public static final String EXTRA_PRODUCT_NAME = "PRODUCT_NAME";
    public static final String EXTRA_BRAND = "BRAND";
    public static final String EXTRA_BARCODE = "BARCODE";
    public static final String EXTRA_CARBS_PER_100G = "CARBS_PER_100G";
    public static final String EXTRA_PORTION_GRAMS = "PORTION_GRAMS";
    public static final String EXTRA_FAVORITE_ID = "FAVORITE_ID";

    private FavoriteItemRepository repository;
    private FavoriteItemAdapter adapter;
    private List<FavoriteItem> items;
    private boolean pickMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite_items);
        JoH.fixActionBar(this);
        setTitle(R.string.carblookup_title_favorites);

        pickMode = getIntent().getBooleanExtra(EXTRA_PICK_MODE, false);
        repository = createFavoriteItemRepository();

        ListView listView = findViewById(R.id.favoritesListView);
        listView.setEmptyView(findViewById(R.id.emptyTextView));

        items = repository.getAll();
        adapter = new FavoriteItemAdapter(this, items, new FavoriteItemAdapter.FavoriteItemActionListener() {
            @Override
            public void onSelect(FavoriteItem item) {
                returnFavorite(item);
            }

            @Override
            public void onEdit(FavoriteItem item) {
                launchAsProductDetail(item);
            }

            @Override
            public void onDelete(FavoriteItem item) {
                confirmDeleteFavorite(item);
            }
        });
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            FavoriteItem item = items.get(position);
            returnFavorite(item);
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            confirmDeleteFavorite(items.get(position));
            return true;
        });
    }

    protected FavoriteItemRepository createFavoriteItemRepository() {
        return new FavoriteItemRepository(this);
    }

    private void confirmDeleteFavorite(FavoriteItem item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.carblookup_delete_favorite_title)
                .setMessage(getString(R.string.carblookup_delete_favorite_message, item.productName))
                .setPositiveButton(R.string.carblookup_context_delete, (d, w) -> {
                    repository.delete(item.id);
                    reloadFavorites();
                    Toast.makeText(this, R.string.carblookup_favorite_deleted, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.carblookup_cancel, null)
                .show();
    }

    private void reloadFavorites() {
        items.clear();
        items.addAll(repository.getAll());
        adapter.notifyDataSetChanged();
    }

    private void returnFavorite(FavoriteItem item) {
        Intent result = new Intent();
        result.putExtra(EXTRA_FAVORITE_ID, item.id);
        result.putExtra(EXTRA_PRODUCT_NAME, item.productName);
        result.putExtra(EXTRA_BRAND, item.brand);
        result.putExtra(EXTRA_BARCODE, item.barcode);
        result.putExtra(EXTRA_CARBS_PER_100G, item.carbsPer100g);
        result.putExtra(EXTRA_PORTION_GRAMS, item.defaultPortionGrams);
        setResult(RESULT_OK, result);
        finish();
    }

    private void launchAsProductDetail(FavoriteItem item) {
        startActivity(ProductDetailContract.forFavorite(this, item));
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadFavorites();
    }

    private String formatDecimal(double value) {
        if (value == Math.rint(value)) {
            return String.format(java.util.Locale.US, "%.0f", value);
        }
        return String.format(java.util.Locale.US, "%.1f", value);
    }
}
