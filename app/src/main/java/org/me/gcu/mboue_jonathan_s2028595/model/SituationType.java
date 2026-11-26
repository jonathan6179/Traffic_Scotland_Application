package org.me.gcu.mboue_jonathan_s2028595.model;

/**
 * The kinds of situation the app distinguishes. Each kind has its own icon
 * (see ui/SituationUi), which gives 7 distinct icons for the icon requirement.
 */
public enum SituationType {
    ROAD_CLOSED("Road closed", false),
    SLIP_ROAD("Slip road closure", false),
    QUEUING_TRAFFIC("Queuing traffic", false),
    LANE_RESTRICTION("Lane restriction", false),
    OTHER_INCIDENT("Incident", false),
    ROADWORKS("Roadworks", true),
    URGENT_ROADWORKS("Urgent roadworks", true);

    private final String label;
    private final boolean roadworks;

    SituationType(String label, boolean roadworks) {
        this.label = label;
        this.roadworks = roadworks;
    }

    public String getLabel() {
        return label;
    }

    public boolean isRoadworks() {
        return roadworks;
    }
}
