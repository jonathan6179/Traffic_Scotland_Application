// Name                 Jonathan Mboue
// Student ID           S2028595
// Programme of Study   BSc Computing

/*
 * Mobile Platform Development coursework: Traffic Scotland
 *
 * Architecture: single activity + fragments + MVVM.
 *   Model      model/ and data/ (Situation classes, PullParser, repository)
 *   ViewModel  viewmodel/TrafficViewModel
 *   View       this activity and the fragments in ui/
 */
package org.me.gcu.mboue_jonathan_s2028595.ui;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.navigation.NavigationBarView;

import org.me.gcu.mboue_jonathan_s2028595.R;
import org.me.gcu.mboue_jonathan_s2028595.data.TrafficRepository;
import org.me.gcu.mboue_jonathan_s2028595.viewmodel.TrafficViewModel;

import java.util.Date;

public class MainActivity extends AppCompatActivity implements Navigator {

    private static final String TAG_DETAIL = "detail";

    private TrafficViewModel vm;
    private NavigationBarView navBar;
    private TextView statusText;
    private ProgressBar statusProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        // activity_main exists twice: layout/ (portrait, bottom navigation bar) and
        // layout-land/ (landscape, navigation rail at the side). Same ids in both.
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right,
                    getResources().getConfiguration().orientation
                            == android.content.res.Configuration.ORIENTATION_PORTRAIT ? 0 : bars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (getSupportActionBar() == null) {
            setSupportActionBar(toolbar);
        } else {
            toolbar.setVisibility(View.GONE); // theme already supplies an action bar
        }

        vm = new ViewModelProvider(this).get(TrafficViewModel.class);

        statusText = findViewById(R.id.status_text);
        statusProgress = findViewById(R.id.status_progress);
        vm.getFeedState().observe(this, this::renderStatus);

        navBar = findViewById(R.id.nav_bar);
        navBar.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        if (savedInstanceState == null) {
            navBar.setSelectedItemId(R.id.nav_incidents); // also shows the first fragment
        }
    }

    // ---- navigation ------------------------------------------------------

    private void showTab(int itemId) {
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        Fragment fragment;
        if (itemId == R.id.nav_roadworks) {
            fragment = SituationListFragment.newInstance(true);
        } else if (itemId == R.id.nav_search) {
            fragment = new SearchFragment();
        } else if (itemId == R.id.nav_map) {
            fragment = new TrafficMapFragment();
        } else {
            fragment = SituationListFragment.newInstance(false);
        }
        fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    @Override
    public void showDetail() {
        FragmentManager fm = getSupportFragmentManager();
        Fragment current = fm.findFragmentById(R.id.fragment_container);
        if (current instanceof SituationDetailFragment) {
            return; // already showing; the fragment follows the ViewModel's selection
        }
        fm.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, new SituationDetailFragment(), TAG_DETAIL)
                .addToBackStack(TAG_DETAIL)
                .commit();
    }

    @Override
    public void showMap() {
        if (navBar.getSelectedItemId() == R.id.nav_map) {
            // Already on the map tab (for example, detail opened from a map marker):
            // selecting it again would not fire the listener, so show it directly.
            showTab(R.id.nav_map);
        } else {
            navBar.setSelectedItemId(R.id.nav_map);
        }
    }

    // ---- status banner ---------------------------------------------------

    private void renderStatus(TrafficRepository.FeedState state) {
        String time = state.dataTimeMillis == 0 ? ""
                : DateFormat.getTimeFormat(this).format(new Date(state.dataTimeMillis));
        statusProgress.setVisibility(state.status == TrafficRepository.Status.LOADING
                ? View.VISIBLE : View.GONE);

        int colour;
        String text;
        switch (state.status) {
            case LIVE:
                colour = R.color.tr_status_live;
                text = getString(R.string.status_live, time);
                break;
            case OFFLINE_CACHE:
                colour = R.color.tr_status_offline;
                text = getString(R.string.status_offline, time);
                break;
            case ERROR:
                colour = R.color.tr_status_error;
                text = getString(R.string.status_error, state.message);
                break;
            case LOADING:
            default:
                colour = R.color.tr_status_live;
                text = time.isEmpty() ? getString(R.string.status_loading)
                        : getString(R.string.status_updating, time);
                break;
        }
        statusText.setBackgroundColor(ContextCompat.getColor(this, colour));
        statusText.setText(text);
    }

    // ---- menu ------------------------------------------------------------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int minutes = vm.getRefreshMinutes();
        int id = minutes == 1 ? R.id.interval_1 : minutes == 15 ? R.id.interval_15 : R.id.interval_60;
        MenuItem item = menu.findItem(id);
        if (item != null) {
            item.setChecked(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            vm.refresh();
            return true;
        } else if (id == R.id.interval_1) {
            vm.setRefreshMinutes(1);
            item.setChecked(true);
            return true;
        } else if (id == R.id.interval_15) {
            vm.setRefreshMinutes(15);
            item.setChecked(true);
            return true;
        } else if (id == R.id.interval_60) {
            vm.setRefreshMinutes(60);
            item.setChecked(true);
            return true;
        } else if (id == R.id.action_about) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.about_title)
                    .setMessage(R.string.about_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
