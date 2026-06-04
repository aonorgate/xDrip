package com.eveningoutpost.dexdrip.carblookup;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.carblookup.model.FavoriteItem;

import java.util.List;
import java.util.Locale;

public class FavoriteItemAdapter extends ArrayAdapter<FavoriteItem> {
    interface FavoriteItemActionListener {
        void onSelect(FavoriteItem item);
        void onEdit(FavoriteItem item);
        void onDelete(FavoriteItem item);
    }

    private final FavoriteItemActionListener actionListener;

    public FavoriteItemAdapter(Context context, List<FavoriteItem> items) {
        this(context, items, null);
    }

    public FavoriteItemAdapter(Context context, List<FavoriteItem> items,
            FavoriteItemActionListener actionListener) {
        super(context, 0, items);
        this.actionListener = actionListener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FavoriteItem item = getItem(position);

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_favorite_row, parent, false);
        }

        TextView nameTextView = convertView.findViewById(R.id.favoriteNameTextView);
        TextView detailTextView = convertView.findViewById(R.id.favoriteDetailTextView);
        TextView usedTextView = convertView.findViewById(R.id.favoriteUsedTextView);
        ImageButton editButton = convertView.findViewById(R.id.editFavoriteButton);
        ImageButton deleteButton = convertView.findViewById(R.id.deleteFavoriteButton);

        nameTextView.setText(item.productName);

        StringBuilder detail = new StringBuilder();
        if (item.brand != null && !item.brand.isEmpty()) {
            detail.append(item.brand).append(" | ");
        }
        detail.append(String.format(Locale.getDefault(),
                getContext().getString(R.string.carblookup_carbs_per_100g_format), item.carbsPer100g));
        if (item.defaultPortionGrams > 0) {
            detail.append(String.format(Locale.getDefault(), " | %s",
                    String.format(Locale.getDefault(),
                            getContext().getString(R.string.carblookup_favorite_portion_format),
                            item.defaultPortionGrams)));
        }
        detailTextView.setText(detail);

        if (item.useCount > 0) {
            usedTextView.setText(String.format(Locale.getDefault(),
                    getContext().getString(R.string.carblookup_favorite_used_format), item.useCount));
        } else {
            usedTextView.setText("");
        }

        if (actionListener != null) {
            convertView.setOnClickListener(v -> actionListener.onSelect(item));
            editButton.setVisibility(View.VISIBLE);
            deleteButton.setVisibility(View.VISIBLE);
            editButton.setOnClickListener(v -> actionListener.onEdit(item));
            deleteButton.setOnClickListener(v -> actionListener.onDelete(item));
        } else {
            convertView.setOnClickListener(null);
            editButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
        }

        return convertView;
    }
}
