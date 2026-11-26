package org.me.gcu.mboue_jonathan_s2028595.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.me.gcu.mboue_jonathan_s2028595.R;
import org.me.gcu.mboue_jonathan_s2028595.viewmodel.TrafficViewModel;

/** Full-screen detail of the selected situation (portrait, and from the map). */
public class SituationDetailFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_situation_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final TrafficViewModel vm = new ViewModelProvider(requireActivity())
                .get(TrafficViewModel.class);
        final Navigator nav = (Navigator) requireActivity();
        final View pane = view.findViewById(R.id.detail_pane);
        vm.getSelected().observe(getViewLifecycleOwner(),
                situation -> DetailRenderer.bind(pane, situation, vm, nav));
    }
}
