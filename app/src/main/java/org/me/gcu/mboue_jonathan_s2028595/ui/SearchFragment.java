package org.me.gcu.mboue_jonathan_s2028595.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.me.gcu.mboue_jonathan_s2028595.R;
import org.me.gcu.mboue_jonathan_s2028595.model.Situation;
import org.me.gcu.mboue_jonathan_s2028595.viewmodel.TrafficViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Search by road name and search by distance. Results are shown in a list and
 * can be sent to the map as a set.
 */
public class SearchFragment extends Fragment implements SituationAdapter.Listener {

    /** Fixed fallback position: Glasgow Caledonian University. */
    private static final double FIXED_LAT = 55.8668;
    private static final double FIXED_LON = -4.2500;
    private static final String FIXED_LABEL = "the fixed location (GCU, Glasgow)";

    private TrafficViewModel vm;
    private SituationAdapter adapter;

    private RadioButton modeRoad;
    private RadioButton locationFixed;
    private View panelRoad;
    private View panelDistance;
    private AutoCompleteTextView roadInput;
    private SeekBar radiusSeek;
    private TextView radiusLabel;
    private Spinner scopeSpinner;
    private TextView resultSummary;
    private Button showOnMapButton;
    private View resultRow;
    private RecyclerView resultsList;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        if (hasLocationPermission()) {
                            useDeviceLocation();
                        } else {
                            Toast.makeText(requireContext(),
                                    R.string.location_denied, Toast.LENGTH_SHORT).show();
                            runDistanceSearch(FIXED_LAT, FIXED_LON, FIXED_LABEL);
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        modeRoad = view.findViewById(R.id.mode_road);
        locationFixed = view.findViewById(R.id.location_fixed);
        panelRoad = view.findViewById(R.id.panel_road);
        panelDistance = view.findViewById(R.id.panel_distance);
        roadInput = view.findViewById(R.id.road_input);
        radiusSeek = view.findViewById(R.id.radius_seek);
        radiusLabel = view.findViewById(R.id.radius_label);
        scopeSpinner = view.findViewById(R.id.scope_spinner);
        resultSummary = view.findViewById(R.id.result_summary);
        showOnMapButton = view.findViewById(R.id.show_results_on_map);
        resultRow = view.findViewById(R.id.result_row);
        Button roadSearch = view.findViewById(R.id.road_search_button);
        Button distanceSearch = view.findViewById(R.id.distance_search_button);
        RecyclerView results = view.findViewById(R.id.results_list);
        resultsList = results;

        ArrayAdapter<CharSequence> scopeAdapter = ArrayAdapter.createFromResource(
                requireContext(), R.array.scope_options, android.R.layout.simple_spinner_item);
        scopeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scopeSpinner.setAdapter(scopeAdapter);

        adapter = new SituationAdapter(this);
        results.setLayoutManager(new LinearLayoutManager(requireContext()));
        results.setAdapter(adapter);

        modeRoad.setOnCheckedChangeListener((button, checked) -> applyMode());
        applyMode();

        // Road suggestions come from the roads found in the downloaded data.
        vm.getIncidents().observe(getViewLifecycleOwner(), list -> refreshSuggestions());
        vm.getRoadworks().observe(getViewLifecycleOwner(), list -> refreshSuggestions());

        roadSearch.setOnClickListener(v -> runRoadSearch());
        roadInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runRoadSearch();
                return true;
            }
            return false;
        });
        roadInput.setOnItemClickListener((parent, v, position, id) -> runRoadSearch());

        radiusSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateRadiusLabel();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        updateRadiusLabel();
        distanceSearch.setOnClickListener(v -> onDistanceSearchClicked());

        showOnMapButton.setOnClickListener(v -> {
            vm.showSearchResultsOnMap();
            ((Navigator) requireActivity()).showMap();
        });

        // The ViewModel keeps the last results, so rotation shows them again.
        vm.getSearchResults().observe(getViewLifecycleOwner(), list -> {
            adapter.setItems(list, vm.isLastSearchByDistance());
            showOnMapButton.setVisibility(list.isEmpty() ? View.GONE : View.VISIBLE);
            updateResultsVisibility();
        });
        vm.getSearchSummary().observe(getViewLifecycleOwner(),
                text -> resultSummary.setText(text));
    }

    private void applyMode() {
        boolean road = modeRoad.isChecked();
        panelRoad.setVisibility(road ? View.VISIBLE : View.GONE);
        panelDistance.setVisibility(road ? View.GONE : View.VISIBLE);
        updateResultsVisibility();
    }

    /**
     * Shows the summary row and result list only when the selected mode matches the type of
     * the last search; the ViewModel's results are left untouched.
     */
    private void updateResultsVisibility() {
        boolean matches = modeRoad.isChecked() != vm.isLastSearchByDistance();
        resultRow.setVisibility(matches ? View.VISIBLE : View.GONE);
        resultsList.setVisibility(matches ? View.VISIBLE : View.GONE);
        List<Situation> current = vm.getSearchResults().getValue();
        boolean hasResults = current != null && !current.isEmpty();
        showOnMapButton.setVisibility(matches && hasResults ? View.VISIBLE : View.GONE);
    }

    private void refreshSuggestions() {
        List<String> roads = new ArrayList<>(vm.getKnownRoads());
        roadInput.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line, roads));
    }

    private void updateRadiusLabel() {
        radiusLabel.setText(getString(R.string.radius_label, radiusKm()));
    }

    private int radiusKm() {
        return radiusSeek.getProgress() + 1; // seek bar 0..49 gives 1..50 km
    }

    // ---- road search -----------------------------------------------------

    private void runRoadSearch() {
        String query = roadInput.getText().toString().trim();
        if (query.length() < 2) {
            roadInput.setError(getString(R.string.road_too_short));
            return;
        }
        hideKeyboard();
        vm.searchByRoad(query, scopeSpinner.getSelectedItemPosition());
    }

    // ---- distance search -------------------------------------------------

    private void onDistanceSearchClicked() {
        if (locationFixed.isChecked()) {
            runDistanceSearch(FIXED_LAT, FIXED_LON, FIXED_LABEL);
        } else if (hasLocationPermission()) {
            useDeviceLocation();
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION});
        }
    }

    private void useDeviceLocation() {
        Location fix = lastKnownLocation();
        if (fix == null) {
            Toast.makeText(requireContext(), R.string.no_location_fix, Toast.LENGTH_LONG).show();
            runDistanceSearch(FIXED_LAT, FIXED_LON, FIXED_LABEL);
        } else {
            runDistanceSearch(fix.getLatitude(), fix.getLongitude(), "your location");
        }
    }

    private void runDistanceSearch(double lat, double lon, String label) {
        hideKeyboard();
        vm.searchByDistance(lat, lon, radiusKm(), scopeSpinner.getSelectedItemPosition(), label);
    }

    private boolean hasLocationPermission() {
        Context c = requireContext();
        return ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** Most recent fix any provider has. Providers we are not allowed to read are skipped. */
    private Location lastKnownLocation() {
        LocationManager lm = (LocationManager) requireContext()
                .getSystemService(Context.LOCATION_SERVICE);
        Location best = null;
        String[] providers = {LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER,
                LocationManager.PASSIVE_PROVIDER};
        for (String provider : providers) {
            try {
                Location l = lm.getLastKnownLocation(provider);
                if (l != null && (best == null || l.getTime() > best.getTime())) {
                    best = l;
                }
            } catch (SecurityException | IllegalArgumentException ignored) {
                // Provider missing or not permitted with coarse access only.
            }
        }
        return best;
    }

    private void hideKeyboard() {
        View focus = requireActivity().getCurrentFocus();
        if (focus != null) {
            InputMethodManager imm = (InputMethodManager) requireContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
        }
    }

    @Override
    public void onSituationClicked(Situation situation) {
        vm.select(situation);
        ((Navigator) requireActivity()).showDetail();
    }
}
