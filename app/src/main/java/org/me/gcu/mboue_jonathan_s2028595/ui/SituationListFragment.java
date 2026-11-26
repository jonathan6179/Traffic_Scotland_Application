package org.me.gcu.mboue_jonathan_s2028595.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.me.gcu.mboue_jonathan_s2028595.R;
import org.me.gcu.mboue_jonathan_s2028595.model.Situation;
import org.me.gcu.mboue_jonathan_s2028595.viewmodel.TrafficViewModel;

import java.util.List;

/**
 * The list of incidents or of roadworks (the same fragment, told which by an argument).
 * In landscape the layout has a detail pane beside the list; in portrait a click
 * opens the full-screen detail fragment instead.
 */
public class SituationListFragment extends Fragment implements SituationAdapter.Listener {

    private static final String ARG_ROADWORKS = "roadworks";

    private TrafficViewModel vm;
    private SituationAdapter adapter;
    private View detailPane; // null in portrait

    public static SituationListFragment newInstance(boolean roadworks) {
        SituationListFragment f = new SituationListFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_ROADWORKS, roadworks);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_situation_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final boolean roadworks = requireArguments().getBoolean(ARG_ROADWORKS);
        vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        final TextView header = view.findViewById(R.id.list_header);
        final TextView emptyText = view.findViewById(R.id.list_empty);
        RecyclerView list = view.findViewById(R.id.list);

        adapter = new SituationAdapter(this);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        if (roadworks) {
            showLegend(view);
        }

        LiveData<List<Situation>> source = roadworks ? vm.getRoadworks() : vm.getIncidents();
        final String noun = roadworks ? "Roadworks" : "Unplanned incidents";
        source.observe(getViewLifecycleOwner(), items -> {
            adapter.setItems(items, false);
            header.setText(getString(R.string.list_header, noun, items.size()));
            emptyText.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        });

        detailPane = view.findViewById(R.id.detail_pane);
        if (detailPane != null) {
            final Navigator nav = (Navigator) requireActivity();
            DetailRenderer.bind(detailPane, null, vm, nav);
            vm.getSelected().observe(getViewLifecycleOwner(), s -> {
                DetailRenderer.bind(detailPane, s, vm, nav);
                adapter.setSelectedId(s == null ? null : s.getId());
            });
        }
    }

    /** Colour key for the roadworks delay chips. */
    private void showLegend(View root) {
        View legend = root.findViewById(R.id.delay_legend);
        legend.setVisibility(View.VISIBLE);
        SituationUi.styleChip((TextView) root.findViewById(R.id.legend_delays),
                Situation.DelayLevel.DELAYS);
        SituationUi.styleChip((TextView) root.findViewById(R.id.legend_long),
                Situation.DelayLevel.LONG_DELAYS);
        SituationUi.styleChip((TextView) root.findViewById(R.id.legend_very_long),
                Situation.DelayLevel.VERY_LONG_DELAYS);
    }

    @Override
    public void onSituationClicked(Situation situation) {
        vm.select(situation);
        if (detailPane == null) {
            ((Navigator) requireActivity()).showDetail();
        }
    }
}
