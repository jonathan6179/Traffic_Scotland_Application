package org.me.gcu.mboue_jonathan_s2028595.ui;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;

import org.me.gcu.mboue_jonathan_s2028595.R;
import org.me.gcu.mboue_jonathan_s2028595.model.Situation;
import org.me.gcu.mboue_jonathan_s2028595.model.SituationType;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Icons, colours and text formatting shared by the list, detail and map screens. */
public final class SituationUi {

    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.UK);
    private static final ZoneId UK = ZoneId.of("Europe/London");

    private SituationUi() {
    }

    public static int iconFor(SituationType type) {
        switch (type) {
            case ROAD_CLOSED:
                return R.drawable.ic_road_closed;
            case SLIP_ROAD:
                return R.drawable.ic_slip_road;
            case QUEUING_TRAFFIC:
                return R.drawable.ic_queuing_traffic;
            case LANE_RESTRICTION:
                return R.drawable.ic_lane_restriction;
            case ROADWORKS:
                return R.drawable.ic_roadworks;
            case URGENT_ROADWORKS:
                return R.drawable.ic_urgent_roadworks;
            case OTHER_INCIDENT:
            default:
                return R.drawable.ic_incident;
        }
    }

    // ---- delay colour coding (roadworks) ---------------------------------

    public static int delayColour(Situation.DelayLevel level) {
        switch (level) {
            case DELAYS:
                return Color.parseColor("#F9A825");      // amber: short delays
            case LONG_DELAYS:
                return Color.parseColor("#EF6C00");      // orange: long delays
            case VERY_LONG_DELAYS:
                return Color.parseColor("#C62828");      // red: very long delays
            case NONE:
            default:
                return Color.parseColor("#607D8B");      // grey: none reported
        }
    }

    public static int delayTextColour(Situation.DelayLevel level) {
        return level == Situation.DelayLevel.DELAYS ? Color.parseColor("#212121") : Color.WHITE;
    }

    public static String delayLabel(Situation.DelayLevel level) {
        switch (level) {
            case DELAYS:
                return "Delays";
            case LONG_DELAYS:
                return "Long delays";
            case VERY_LONG_DELAYS:
                return "Very long delays";
            case NONE:
            default:
                return "No delays reported";
        }
    }

    /** Colours a TextView as a rounded chip for the given delay level. */
    public static void styleChip(TextView chip, Situation.DelayLevel level) {
        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(48f);
        background.setColor(delayColour(level));
        chip.setBackground(background);
        chip.setTextColor(delayTextColour(level));
        chip.setText(delayLabel(level));
    }

    /** Roadworks get a delay chip; incidents hide it. */
    public static void styleDelayChip(TextView chip, Situation s) {
        if (s.isRoadworks()) {
            styleChip(chip, s.getDelayLevel());
            chip.setVisibility(View.VISIBLE);
        } else {
            chip.setVisibility(View.GONE);
        }
    }

    /** Colour of the line drawn on the map between the from and to points. */
    public static int lineColour(Situation s) {
        if (s.isRoadworks()) {
            return delayColour(s.getDelayLevel());
        }
        switch (s.getType()) {
            case ROAD_CLOSED:
            case SLIP_ROAD:
                return Color.parseColor("#C62828");
            case QUEUING_TRAFFIC:
                return Color.parseColor("#EF6C00");
            case LANE_RESTRICTION:
                return Color.parseColor("#1565C0");
            default:
                return Color.parseColor("#F9A825");
        }
    }

    // ---- text ------------------------------------------------------------

    /** "definedByValidityTimeSpec" becomes "Defined by validity time spec". */
    public static String humanise(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return "";
        }
        String spaced = camelCase.replaceAll("([a-z])([A-Z])", "$1 $2").toLowerCase(Locale.UK);
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }

    public static String formatTime(OffsetDateTime time) {
        return time == null ? "" : time.atZoneSameInstant(UK).format(TIME_FORMAT);
    }

    public static String shorten(String text, int max) {
        if (text == null || text.length() <= max) {
            return text == null ? "" : text;
        }
        return text.substring(0, max - 1).trim() + "…";
    }
}
