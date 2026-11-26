package org.me.gcu.mboue_jonathan_s2028595.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One end of a linear location: the "from" or the "to" point of a situation.
 * Holds the coordinates and every descriptor name the feed gives for the point.
 */
public class SituationLocation {
    public static final String FROM = "from";
    public static final String TO = "to";

    private final String role;
    private double latitude = Double.NaN;
    private double longitude = Double.NaN;
    private final List<String> names = new ArrayList<>();

    public SituationLocation(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public List<String> getNames() {
        return names;
    }

    public void addName(String name) {
        if (name != null && !name.isEmpty() && !names.contains(name)) {
            names.add(name);
        }
    }

    public boolean hasCoordinates() {
        return !Double.isNaN(latitude) && !Double.isNaN(longitude);
    }

    @Override
    public String toString() {
        return role + " " + names + " (" + latitude + ", " + longitude + ")";
    }
}
