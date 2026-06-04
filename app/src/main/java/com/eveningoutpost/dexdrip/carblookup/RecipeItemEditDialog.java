package com.eveningoutpost.dexdrip.carblookup;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.carblookup.model.RecipeItem;

import java.util.Locale;

/**
 * Dialog that lets the user edit the portion weight (grams) of a single RecipeItem.
 * Calls back with the updated item when the user confirms.
 */
public class RecipeItemEditDialog {

    public interface Listener {
        void onItemUpdated(RecipeItem item, int position);
    }

    public static void show(Context context, RecipeItem item, int position, Listener listener) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(context, 16);
        layout.setPadding(pad, pad, pad, pad);

        EditText nameEdit = new EditText(context);
        nameEdit.setHint(R.string.carblookup_ingredient_name_hint);
        nameEdit.setText(item.productName != null ? item.productName : "");
        layout.addView(nameEdit);

        EditText brandEdit = new EditText(context);
        brandEdit.setHint(R.string.carblookup_brand_hint);
        brandEdit.setText(item.brand != null ? item.brand : "");
        layout.addView(brandEdit);

        EditText barcodeEdit = new EditText(context);
        barcodeEdit.setHint(R.string.carblookup_barcode_hint);
        barcodeEdit.setInputType(InputType.TYPE_CLASS_TEXT);
        barcodeEdit.setText(item.barcode != null ? item.barcode : "");
        layout.addView(barcodeEdit);

        TextView carbsLabel = new TextView(context);
        carbsLabel.setText(R.string.carblookup_carbs_per_100g_label);
        carbsLabel.setPadding(0, dpToPx(context, 8), 0, 0);
        layout.addView(carbsLabel);

        EditText carbsEdit = new EditText(context);
        carbsEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        carbsEdit.setHint(R.string.carblookup_decimal_zero_hint);
        carbsEdit.setText(String.format(Locale.getDefault(), "%.1f", item.carbsPer100g));
        layout.addView(carbsEdit);

        EditText weightEdit = new EditText(context);
        weightEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        weightEdit.setHint(R.string.carblookup_weight_hint);
        weightEdit.setText(String.format(Locale.getDefault(), "%.0f", item.itemWeightGrams));
        weightEdit.setPadding(0, dpToPx(context, 8), 0, 0);
        layout.addView(weightEdit);

        TextView previewText = new TextView(context);
        previewText.setText(carbsPreview(context, item.carbsPer100g, item.itemWeightGrams));
        previewText.setPadding(0, dpToPx(context, 8), 0, 0);
        layout.addView(previewText);

        TextWatcher previewWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                try {
                    double grams = Double.parseDouble(weightEdit.getText().toString());
                    double carbsPer100g = Double.parseDouble(carbsEdit.getText().toString());
                    previewText.setText(carbsPreview(context, carbsPer100g, grams));
                } catch (NumberFormatException e) {
                    previewText.setText(context.getString(R.string.carblookup_carbs_preview_empty));
                }
            }
        };
        weightEdit.addTextChangedListener(previewWatcher);
        carbsEdit.addTextChangedListener(previewWatcher);

        TextView giLabel = new TextView(context);
        giLabel.setText(R.string.carblookup_gi_label);
        giLabel.setPadding(0, dpToPx(context, 8), 0, 0);
        layout.addView(giLabel);

        EditText giEdit = new EditText(context);
        giEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
        giEdit.setHint(R.string.carblookup_integer_zero_hint);
        if (item.giEstimate > 0) {
            giEdit.setText(String.valueOf(item.giEstimate));
        }
        layout.addView(giEdit);

        new AlertDialog.Builder(context)
                .setTitle(R.string.carblookup_edit_ingredient_title)
                .setView(layout)
                .setPositiveButton(R.string.carblookup_update, (dialog, which) -> {
                    try {
                        String productName = nameEdit.getText().toString().trim();
                        double carbsPer100g = Double.parseDouble(carbsEdit.getText().toString());
                        double grams = Double.parseDouble(weightEdit.getText().toString());
                        if (!productName.isEmpty() && grams > 0 && carbsPer100g >= 0) {
                            item.productName = productName;
                            item.brand = brandEdit.getText().toString().trim();
                            String barcode = barcodeEdit.getText().toString().trim();
                            item.barcode = barcode.isEmpty() ? null : barcode;
                            item.carbsPer100g = carbsPer100g;
                            item.itemWeightGrams = grams;
                            item.itemCarbsGrams = Math.round((carbsPer100g * grams / 100.0) * 10.0) / 10.0;
                            String giText = giEdit.getText().toString().trim();
                            item.giEstimate = giText.isEmpty()
                                    ? 0
                                    : Math.min(100, Math.max(0, Integer.parseInt(giText)));
                            listener.onItemUpdated(item, position);
                        }
                    } catch (NumberFormatException ignored) {}
                })
                .setNegativeButton(R.string.carblookup_cancel, null)
                .show();
    }

    private static String carbsPreview(Context context, double carbsPer100g, double grams) {
        double carbs = carbsPer100g * grams / 100.0;
        return String.format(Locale.getDefault(), context.getString(R.string.carblookup_carbs_preview_format), carbs);
    }

    private static int dpToPx(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
}
