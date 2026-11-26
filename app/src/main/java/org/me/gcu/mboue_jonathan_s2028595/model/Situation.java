package org.me.gcu.mboue_jonathan_s2028595.model;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * One <situation> from the Traffic Scotland DATEX II feeds (an unplanned
 * incident or a roadworks entry). Plain Java so it can be unit tested.
 */
public class Situation {

    /** How long the delays caused by roadworks are expected to be. */
    public enum DelayLevel {
        NONE, DELAYS, LONG_DELAYS, VERY_LONG_DELAYS;

        public static DelayLevel from(String delaysType) {
            if (delaysType == null) {
                return NONE;
            }
            switch (delaysType.trim().toLowerCase(Locale.UK)) {
                case "delays":
                    return DELAYS;
                case "longdelays":
                    return LONG_DELAYS;
                case "verylongdelays":
                    return VERY_LONG_DELAYS;
                default:
                    return NONE;
            }
        }
    }

    private String id = "";
    private String version = "";
    private String recordType = "";
    private String subType = "";
    private String comment = "";
    private String direction = "";
    private String probability = "";
    private String validityStatus = "";
    private String compliance = "";
    private String source = "";
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private OffsetDateTime creationTime;
    private OffsetDateTime versionTime;
    private int lanesRestricted = -1;
    private int operationalLanes = -1;
    private String delaysType = "";
    private boolean urgent;
    private boolean roadworks;
    private SituationType type = SituationType.OTHER_INCIDENT;
    private final Set<String> roads = new LinkedHashSet<>();
    private final List<SituationLocation> locations = new ArrayList<>();

    /** Set by a distance search only; NaN otherwise. */
    private double distanceKm = Double.NaN;

    // ---- derived helpers -------------------------------------------------

    public String getTypeLabel() {
        return type.getLabel();
    }

    public DelayLevel getDelayLevel() {
        return DelayLevel.from(delaysType);
    }

    /** "westBound" becomes "Westbound". */
    public String getDirectionLabel() {
        if (direction.isEmpty()) {
            return "";
        }
        String d = direction.replace("Bound", "bound");
        return Character.toUpperCase(d.charAt(0)) + d.substring(1);
    }

    /** Coordinates of every location point that has them, as {lat, lon}. */
    public List<double[]> getPoints() {
        List<double[]> points = new ArrayList<>();
        for (SituationLocation loc : locations) {
            if (loc.hasCoordinates()) {
                points.add(new double[]{loc.getLatitude(), loc.getLongitude()});
            }
        }
        return points;
    }

    /** Midpoint of the situation, or null if it has no coordinates. */
    public double[] getCentre() {
        List<double[]> points = getPoints();
        if (points.isEmpty()) {
            return null;
        }
        double lat = 0;
        double lon = 0;
        for (double[] p : points) {
            lat += p[0];
            lon += p[1];
        }
        return new double[]{lat / points.size(), lon / points.size()};
    }

    public List<String> getAllLocationNames() {
        List<String> all = new ArrayList<>();
        for (SituationLocation loc : locations) {
            for (String n : loc.getNames()) {
                if (!all.contains(n)) {
                    all.add(n);
                }
            }
        }
        return all;
    }

    /** Lower-case text used by free-text road search. */
    public String getSearchText() {
        StringBuilder sb = new StringBuilder(comment);
        for (String n : getAllLocationNames()) {
            sb.append(' ').append(n);
        }
        return sb.toString().toLowerCase(Locale.UK);
    }

    public boolean hasLocation() {
        return getCentre() != null;
    }

    // ---- accessors -------------------------------------------------------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id == null ? "" : id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version == null ? "" : version;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType == null ? "" : recordType;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType == null ? "" : subType;
    }

    public String getComment() {
        return comment;
    }

    public void appendComment(String text) {
        if (comment.isEmpty()) {
            comment = text;
        } else if (!comment.contains(text)) {
            comment = comment + " " + text;
        }
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        if (this.direction.isEmpty() && direction != null) {
            this.direction = direction;
        }
    }

    public String getProbability() {
        return probability;
    }

    public void setProbability(String probability) {
        this.probability = probability == null ? "" : probability;
    }

    public String getValidityStatus() {
        return validityStatus;
    }

    public void setValidityStatus(String validityStatus) {
        this.validityStatus = validityStatus == null ? "" : validityStatus;
    }

    public String getCompliance() {
        return compliance;
    }

    public void setCompliance(String compliance) {
        this.compliance = compliance == null ? "" : compliance;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source == null ? "" : source;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(OffsetDateTime startTime) {
        this.startTime = startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(OffsetDateTime endTime) {
        this.endTime = endTime;
    }

    public OffsetDateTime getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(OffsetDateTime creationTime) {
        this.creationTime = creationTime;
    }

    public OffsetDateTime getVersionTime() {
        return versionTime;
    }

    public void setVersionTime(OffsetDateTime versionTime) {
        this.versionTime = versionTime;
    }

    public int getLanesRestricted() {
        return lanesRestricted;
    }

    public void setLanesRestricted(int lanesRestricted) {
        this.lanesRestricted = lanesRestricted;
    }

    public int getOperationalLanes() {
        return operationalLanes;
    }

    public void setOperationalLanes(int operationalLanes) {
        this.operationalLanes = operationalLanes;
    }

    public String getDelaysType() {
        return delaysType;
    }

    public void setDelaysType(String delaysType) {
        this.delaysType = delaysType == null ? "" : delaysType;
    }

    public boolean isUrgent() {
        return urgent;
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    public boolean isRoadworks() {
        return roadworks;
    }

    public void setRoadworks(boolean roadworks) {
        this.roadworks = roadworks;
    }

    public SituationType getType() {
        return type;
    }

    public void setType(SituationType type) {
        this.type = type;
    }

    public Set<String> getRoads() {
        return roads;
    }

    public void addRoad(String road) {
        if (road != null && !road.isEmpty()) {
            roads.add(road);
        }
    }

    public List<SituationLocation> getLocations() {
        return locations;
    }

    public void addLocation(SituationLocation location) {
        locations.add(location);
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    @Override
    public String toString() {
        return type.getLabel() + " [" + id + "]: " + comment;
    }
}
