package org.me.gcu.mboue_jonathan_s2028595.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.me.gcu.mboue_jonathan_s2028595.R;
import org.me.gcu.mboue_jonathan_s2028595.model.Situation;
import org.me.gcu.mboue_jonathan_s2028595.model.SituationLocation;
import org.me.gcu.mboue_jonathan_s2028595.viewmodel.TrafficViewModel;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

/**
 * Fills the view_situation_detail layout. Used by the full-screen detail
 * fragment (portrait) and by the side pane of the list screens (landscape).
 */
public final class DetailRenderer {

    private DetailRenderer() {
    }

    public static void bind(View root, final Situation s, final TrafficViewModel vm,
                            final Navigator nav) {
        View empty = root.findViewById(R.id.detail_empty);
        View content = root.findViewById(R.id.detail_content);
        if (s == null) {
            empty.setVisibility(View.VISIBLE);
            content.setVisibility(View.GONE);
            return;
        }
        empty.setVisibility(View.GONE);
        content.setVisibility(View.VISIBLE);

        ((ImageView) root.findViewById(R.id.detail_icon))
                .setImageResource(SituationUi.iconFor(s.getType()));
        ((TextView) root.findViewById(R.id.detail_title)).setText(s.getTypeLabel());
        ((TextView) root.findViewById(R.id.detail_comment)).setText(
                s.getComment().isEmpty() ? root.getContext().getString(R.string.no_description)
                        : s.getComment());
        SituationUi.styleDelayChip((TextView) root.findViewById(R.id.detail_chip), s);

        LinearLayout sections = root.findViewById(R.id.detail_sections);
        sections.removeAllViews();
        addSection(sections, "Validity", validityText(s));
        addSection(sections, "Impact", impactText(s));
        addSection(sections, "Location", locationText(s));
        addSection(sections, "Record", recordText(s));

        Button mapButton = root.findViewById(R.id.detail_map_button);
        mapButton.setEnabled(s.hasLocation());
        mapButton.setOnClickListener(v -> {
            vm.showSituationOnMap(s);
            nav.showMap();
        });
    }

    private static void addSection(LinearLayout parent, String title, String body) {
        Context ctx = parent.getContext();
        View section = LayoutInflater.from(ctx)
                .inflate(R.layout.item_detail_section, parent, false);
        ((TextView) section.findViewById(R.id.section_title)).setText(title);
        ((TextView) section.findViewById(R.id.section_body)).setText(body);
        parent.addView(section, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private static String validityText(Situation s) {
        StringBuilder sb = new StringBuilder();
        line(sb, "Starts", SituationUi.formatTime(s.getStartTime()));
        if (s.getEndTime() != null) {
            line(sb, "Ends", SituationUi.formatTime(s.getEndTime()));
        } else {
            line(sb, "Ends", "Not given (open ended)");
        }
        if (s.getStartTime() != null && s.getEndTime() != null) {
            long days = ChronoUnit.DAYS.between(s.getStartTime(), s.getEndTime());
            line(sb, "Planned length", days + (days == 1 ? " day" : " days"));
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (s.getStartTime() != null && now.isBefore(s.getStartTime())) {
            line(sb, "Status", "Not started yet");
        } else if (s.getEndTime() != null && now.isAfter(s.getEndTime())) {
            line(sb, "Status", "Past its end time");
        } else if (s.getStartTime() != null) {
            line(sb, "Status", "In progress");
        }
        line(sb, "Validity", SituationUi.humanise(s.getValidityStatus()));
        return sb.toString().trim();
    }

    private static String impactText(Situation s) {
        StringBuilder sb = new StringBuilder();
        if (s.getOperationalLanes() == 0) {
            line(sb, "Operational lanes", "0 (closed to traffic)");
        } else if (s.getOperationalLanes() > 0) {
            line(sb, "Operational lanes", String.valueOf(s.getOperationalLanes()));
        }
        if (s.getLanesRestricted() >= 0) {
            line(sb, "Lanes restricted", String.valueOf(s.getLanesRestricted()));
        }
        if (s.isRoadworks()) {
            line(sb, "Delays", SituationUi.delayLabel(s.getDelayLevel()));
            line(sb, "Urgent roadworks", s.isUrgent() ? "Yes" : "No");
        }
        if (!s.getSubType().isEmpty()) {
            line(sb, "Kind", SituationUi.humanise(s.getSubType()));
        }
        if (sb.length() == 0) {
            return "No impact information supplied.";
        }
        return sb.toString().trim();
    }

    private static String locationText(Situation s) {
        StringBuilder sb = new StringBuilder();
        if (!s.getRoads().isEmpty()) {
            line(sb, "Roads", android.text.TextUtils.join(", ", s.getRoads()));
        }
        if (!s.getDirectionLabel().isEmpty()) {
            line(sb, "Direction", s.getDirectionLabel());
        }
        List<SituationLocation> locations = s.getLocations();
        for (SituationLocation loc : locations) {
            String label = SituationLocation.FROM.equals(loc.getRole()) ? "From" : "To";
            String names = loc.getNames().isEmpty() ? "" :
                    android.text.TextUtils.join(" / ", loc.getNames());
            String coords = loc.hasCoordinates()
                    ? String.format(Locale.UK, "(%.5f, %.5f)", loc.getLatitude(), loc.getLongitude())
                    : "";
            line(sb, label, (names + " " + coords).trim());
        }
        if (sb.length() == 0) {
            return "No location information supplied.";
        }
        return sb.toString().trim();
    }

    private static String recordText(Situation s) {
        StringBuilder sb = new StringBuilder();
        line(sb, "Reference", s.getId() + (s.getVersion().isEmpty() ? "" : " (version "
                + s.getVersion() + ")"));
        line(sb, "First recorded", SituationUi.formatTime(s.getCreationTime()));
        line(sb, "Last updated", SituationUi.formatTime(s.getVersionTime()));
        line(sb, "Probability", SituationUi.humanise(s.getProbability()));
        line(sb, "Compliance", SituationUi.humanise(s.getCompliance()));
        line(sb, "Source", s.getSource());
        return sb.toString().trim();
    }

    private static void line(StringBuilder sb, String label, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append(label).append(": ").append(value).append('\n');
        }
    }
}
