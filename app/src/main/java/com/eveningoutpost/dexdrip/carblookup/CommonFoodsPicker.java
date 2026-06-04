package com.eveningoutpost.dexdrip.carblookup;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.carblookup.api.FoodDbSource;
import com.eveningoutpost.dexdrip.carblookup.standard.StandardCarbCategory;
import com.eveningoutpost.dexdrip.carblookup.standard.StandardCarbCategoryAdapter;
import com.eveningoutpost.dexdrip.carblookup.standard.StandardCarbItem;
import com.eveningoutpost.dexdrip.carblookup.standard.StandardCarbItemAdapter;
import com.eveningoutpost.dexdrip.carblookup.standard.StandardCarbRepository;

import java.util.List;

class CommonFoodsPicker {

    interface SelectionListener {
        void onStandardCarbItemSelected(StandardCarbItem item);
    }

    private final Context context;
    private final FoodDbSource source;
    private final SelectionListener listener;

    CommonFoodsPicker(Context context, FoodDbSource source, SelectionListener listener) {
        this.context = context;
        this.source = source;
        this.listener = listener;
    }

    void show() {
        List<StandardCarbCategory> categories = StandardCarbRepository.categoriesForSource(source);
        StandardCarbCategoryAdapter adapter = new StandardCarbCategoryAdapter(context, categories, source);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.carblookup_common_foods_title, source.label()))
                .setAdapter(adapter, (clickedDialog, which) -> showCategory(categories.get(which)))
                .setNegativeButton(R.string.carblookup_cancel, null)
                .show();
        stylePickerDialogList(dialog);
    }

    private void showCategory(StandardCarbCategory category) {
        List<StandardCarbItem> items = StandardCarbRepository.forSourceAndCategory(source, category);
        StandardCarbItemAdapter adapter = new StandardCarbItemAdapter(context, items);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(category.label())
                .setAdapter(adapter, (clickedDialog, which) -> listener.onStandardCarbItemSelected(items.get(which)))
                .setNeutralButton(R.string.carblookup_back, null)
                .setNegativeButton(R.string.carblookup_cancel, null)
                .show();
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
            dialog.dismiss();
            show();
        });
        stylePickerDialogList(dialog);
    }

    private void stylePickerDialogList(AlertDialog dialog) {
        if (dialog == null || dialog.getListView() == null) {
            return;
        }
        int rowGap = Math.round(6 * context.getResources().getDisplayMetrics().density);
        dialog.getListView().setDivider(new ColorDrawable(Color.TRANSPARENT));
        dialog.getListView().setDividerHeight(rowGap);
        dialog.getListView().setPadding(0, rowGap, 0, rowGap);
    }
}