package org.me.gcu.mboue_jonathan_s2028595.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import org.me.gcu.mboue_jonathan_s2028595.R;
import org.me.gcu.mboue_jonathan_s2028595.model.Situation;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shows situations as a list: icon, type, description, and a delay chip for roadworks. */
public class SituationAdapter extends RecyclerView.Adapter<SituationAdapter.Holder> {

    public interface Listener {
        void onSituationClicked(Situation situation);
    }

    private final List<Situation> items = new ArrayList<>();
    private final Listener listener;
    private boolean showDistance;
    private String selectedId;

    public SituationAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<Situation> newItems, boolean showDistance) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        this.showDistance = showDistance;
        notifyDataSetChanged();
    }

    public void setSelectedId(String id) {
        selectedId = id;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_situation, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int position) {
        final Situation s = items.get(position);

        h.icon.setImageResource(SituationUi.iconFor(s.getType()));
        h.icon.setContentDescription(s.getTypeLabel());
        h.type.setText(s.getTypeLabel());
        h.comment.setText(s.getComment().isEmpty()
                ? h.itemView.getContext().getString(R.string.no_description) : s.getComment());

        StringBuilder meta = new StringBuilder();
        if (!s.getRoads().isEmpty()) {
            meta.append(android.text.TextUtils.join(", ", s.getRoads()));
        }
        if (!s.getDirectionLabel().isEmpty()) {
            if (meta.length() > 0) {
                meta.append(" · ");
            }
            meta.append(s.getDirectionLabel());
        }
        if (showDistance && !Double.isNaN(s.getDistanceKm())) {
            if (meta.length() > 0) {
                meta.append(" · ");
            }
            meta.append(String.format(Locale.UK, "%.1f km away", s.getDistanceKm()));
        }
        h.meta.setText(meta);
        h.meta.setVisibility(meta.length() == 0 ? View.GONE : View.VISIBLE);

        SituationUi.styleDelayChip(h.chip, s);

        boolean isSelected = s.getId().equals(selectedId);
        int stroke = (int) (2 * h.itemView.getResources().getDisplayMetrics().density);
        ((MaterialCardView) h.itemView).setStrokeWidth(isSelected ? stroke : 0);

        h.itemView.setOnClickListener(v -> listener.onSituationClicked(s));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView type;
        final TextView comment;
        final TextView meta;
        final TextView chip;

        Holder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.item_icon);
            type = itemView.findViewById(R.id.item_type);
            comment = itemView.findViewById(R.id.item_comment);
            meta = itemView.findViewById(R.id.item_meta);
            chip = itemView.findViewById(R.id.item_chip);
        }
    }
}
