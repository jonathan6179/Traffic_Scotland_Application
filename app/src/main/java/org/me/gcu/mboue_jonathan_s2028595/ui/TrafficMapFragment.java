package org.me.gcu.mboue_jonathan_s2028595.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.me.gcu.mboue_jonathan_s2028595.R;
import org.me.gcu.mboue_jonathan_s2028595.model.Situation;
import org.me.gcu.mboue_jonathan_s2028595.model.SituationType;
import org.me.gcu.mboue_jonathan_s2028595.viewmodel.TrafficViewModel;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Map of all situations (filterable by kind), of one selected situation, or of
 * a set of search results. Each marker uses the icon for its situation type;
 * situations with a from and a to point also get a line between them.
 */
public class TrafficMapFragment extends Fragment implements OnMapReadyCallback {

    private TrafficViewModel vm;
    private GoogleMap map;
    private CheckBox showIncidents;
    private CheckBox showRoadworks;
    private TextView summary;
    private Button showAll;
    private String lastCameraKey;
    private final Map<SituationType, BitmapDescriptor> iconCache = new EnumMap<>(SituationType.class);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        vm = new ViewModelProvider(requireActivity()).get(TrafficViewModel.class);

        showIncidents = view.findViewById(R.id.map_show_incidents);
        showRoadworks = view.findViewById(R.id.map_show_roadworks);
        summary = view.findViewById(R.id.map_summary);
        showAll = view.findViewById(R.id.map_show_all);

        showIncidents.setOnCheckedChangeListener((b, checked) -> redraw());
        showRoadworks.setOnCheckedChangeListener((b, checked) -> redraw());
        showAll.setOnClickListener(v -> vm.showAllOnMap());

        vm.getIncidents().observe(getViewLifecycleOwner(), list -> redraw());
        vm.getRoadworks().observe(getViewLifecycleOwner(), list -> redraw());
        vm.getMapSubset().observe(getViewLifecycleOwner(), list -> redraw());
        vm.getMapCentre().observe(getViewLifecycleOwner(), centre -> redraw());

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setOnInfoWindowClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Situation) {
                vm.select((Situation) tag);
                ((Navigator) requireActivity()).showDetail();
            }
        });
        redraw();
    }

    private void redraw() {
        if (map == null || !isAdded() || getView() == null) {
            return;
        }
        map.clear();

        List<Situation> subset = vm.getMapSubset().getValue();
        double[] centre = vm.getMapCentre().getValue();
        List<Situation> toShow = new ArrayList<>();

        if (subset != null) {
            toShow.addAll(subset);
            showIncidents.setEnabled(false);
            showRoadworks.setEnabled(false);
            showAll.setVisibility(View.VISIBLE);
        } else {
            showIncidents.setEnabled(true);
            showRoadworks.setEnabled(true);
            showAll.setVisibility(View.GONE);
            if (showIncidents.isChecked() && vm.getIncidents().getValue() != null) {
                toShow.addAll(vm.getIncidents().getValue());
            }
            if (showRoadworks.isChecked() && vm.getRoadworks().getValue() != null) {
                toShow.addAll(vm.getRoadworks().getValue());
            }
        }

        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        int pointCount = 0;
        int drawn = 0;
        LatLng onlyPoint = null;

        for (Situation s : toShow) {
            List<double[]> points = s.getPoints();
            double[] mid = s.getCentre();
            if (mid == null) {
                continue;
            }
            LatLng position = new LatLng(mid[0], mid[1]);
            Marker marker = map.addMarker(new MarkerOptions()
                    .position(position)
                    .title(s.getTypeLabel())
                    .snippet(SituationUi.shorten(s.getComment(), 140))
                    .icon(markerIcon(s.getType())));
            if (marker != null) {
                marker.setTag(s);
            }
            if (points.size() >= 2) {
                LatLng a = new LatLng(points.get(0)[0], points.get(0)[1]);
                LatLng b = new LatLng(points.get(1)[0], points.get(1)[1]);
                map.addPolyline(new PolylineOptions().add(a, b)
                        .color(SituationUi.lineColour(s)).width(10f));
                bounds.include(a);
                bounds.include(b);
                pointCount += 2;
            } else {
                bounds.include(position);
                pointCount++;
            }
            onlyPoint = position;
            drawn++;
        }

        if (subset != null && centre != null) {
            LatLng c = new LatLng(centre[0], centre[1]);
            map.addCircle(new CircleOptions().center(c).radius(centre[2] * 1000.0)
                    .strokeColor(Color.parseColor("#1565C0")).strokeWidth(4f)
                    .fillColor(Color.parseColor("#141565C0")));
            map.addMarker(new MarkerOptions().position(c).title(getString(R.string.search_centre))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            double dLat = centre[2] / 111.0;
            double dLon = centre[2] / (111.0 * Math.cos(Math.toRadians(centre[0])));
            bounds.include(new LatLng(centre[0] - dLat, centre[1] - dLon));
            bounds.include(new LatLng(centre[0] + dLat, centre[1] + dLon));
            pointCount += 2;
        }

        summary.setText(subset != null
                ? getString(R.string.map_summary_selected, drawn)
                : getString(R.string.map_summary_all, drawn));
        moveCameraIfNeeded(subset, bounds, pointCount, onlyPoint);
    }

    /** Only move the camera when the thing shown changes, not on each auto-update. */
    private void moveCameraIfNeeded(@Nullable List<Situation> subset, LatLngBounds.Builder bounds,
                                    int pointCount, @Nullable LatLng onlyPoint) {
        String key = subset == null ? "all" : "subset:" + System.identityHashCode(subset)
                + ":" + subset.size();
        if (key.equals(lastCameraKey)) {
            return;
        }
        if (subset == null) {
            lastCameraKey = key;
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(56.8, -4.2), 6.2f));
        } else if (pointCount > 0) {
            lastCameraKey = key;
            if (pointCount == 1 || subset.size() == 1 && onlyPoint != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(onlyPoint, 13f));
            } else {
                final LatLngBounds box = bounds.build();
                requireView().post(() -> {
                    try {
                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(box, 120));
                    } catch (IllegalStateException ignored) {
                        // Map not laid out yet; the next redraw will retry.
                        lastCameraKey = null;
                    }
                });
            }
        }
    }

    /** Draws the vector icon for a type into a bitmap the map can use as a marker. */
    private BitmapDescriptor markerIcon(SituationType type) {
        BitmapDescriptor cached = iconCache.get(type);
        if (cached != null) {
            return cached;
        }
        Drawable drawable = ContextCompat.getDrawable(requireContext(), SituationUi.iconFor(type));
        int size = (int) (34 * getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, size, size);
        drawable.draw(canvas);
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        iconCache.put(type, descriptor);
        return descriptor;
    }
}
