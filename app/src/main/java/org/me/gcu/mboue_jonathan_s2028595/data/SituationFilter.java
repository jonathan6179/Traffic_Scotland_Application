package org.me.gcu.mboue_jonathan_s2028595.data;

import org.me.gcu.mboue_jonathan_s2028595.model.Situation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Search by road name and by distance. Plain Java, unit tested. */
public final class SituationFilter {

    public static final int SCOPE_ALL = 0;
    public static final int SCOPE_INCIDENTS = 1;
    public static final int SCOPE_ROADWORKS = 2;

    private static final Pattern ROAD_CODE = Pattern.compile("^[ABM]\\d{1,4}M?$");
    private static final double EARTH_RADIUS_KM = 6371.0088;

    private SituationFilter() {
    }

    /** "a74 (m)" and "A74(M)" both become "A74M". */
    public static String normaliseRoad(String text) {
        return text == null ? "" : text.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.UK);
    }

    /**
     * Road search. A road number (M8, A9, A74) matches the road exactly, so M8
     * does not return M80. Anything else (Kingston Bridge, Woodside) is a
     * free-text search over the description and place names.
     */
    public static List<Situation> byRoad(Collection<Situation> all, String query, int scope) {
        String normalised = normaliseRoad(query);
        List<Situation> out = new ArrayList<>();
        if (normalised.length() < 2) {
            return out;
        }
        boolean roadCode = ROAD_CODE.matcher(normalised).matches();
        String text = query.trim().toLowerCase(Locale.UK);

        for (Situation s : all) {
            if (!inScope(s, scope)) {
                continue;
            }
            boolean match = roadCode ? roadMatches(s, normalised) : s.getSearchText().contains(text);
            if (match) {
                s.setDistanceKm(Double.NaN);
                out.add(s);
            }
        }
        return out;
    }

    static boolean roadMatches(Situation s, String normalisedQuery) {
        for (String road : s.getRoads()) {
            String n = normaliseRoad(road);
            if (n.equals(normalisedQuery)) {
                return true;
            }
            // A74 matches A74(M), but M8 must not match M80.
            if (n.startsWith(normalisedQuery) && n.length() > normalisedQuery.length()
                    && !Character.isDigit(n.charAt(normalisedQuery.length()))) {
                return true;
            }
        }
        return false;
    }

    /** Situations within radiusKm of the point, nearest first. Sets distanceKm on each. */
    public static List<Situation> byDistance(Collection<Situation> all, double lat, double lon,
                                             double radiusKm, int scope) {
        List<Situation> out = new ArrayList<>();
        for (Situation s : all) {
            if (!inScope(s, scope)) {
                continue;
            }
            double d = distanceTo(s, lat, lon);
            if (!Double.isNaN(d) && d <= radiusKm) {
                s.setDistanceKm(d);
                out.add(s);
            }
        }
        Collections.sort(out, new Comparator<Situation>() {
            @Override
            public int compare(Situation a, Situation b) {
                return Double.compare(a.getDistanceKm(), b.getDistanceKm());
            }
        });
        return out;
    }

    /** Smallest distance from the point to the situation's end points or midpoint. */
    static double distanceTo(Situation s, double lat, double lon) {
        List<double[]> points = s.getPoints();
        if (points.isEmpty()) {
            return Double.NaN;
        }
        double[] centre = s.getCentre();
        double best = haversineKm(lat, lon, centre[0], centre[1]);
        for (double[] p : points) {
            best = Math.min(best, haversineKm(lat, lon, p[0], p[1]));
        }
        return best;
    }

    public static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    /** Every road number found in the data, sorted M8, M9, A7, A8 ... for the search suggestions. */
    public static List<String> knownRoads(Collection<Situation> all) {
        TreeSet<String> roads = new TreeSet<>(new Comparator<String>() {
            private final Pattern p = Pattern.compile("^([A-Z])(\\d+)(.*)$");

            @Override
            public int compare(String a, String b) {
                Matcher ma = p.matcher(a);
                Matcher mb = p.matcher(b);
                if (ma.matches() && mb.matches()) {
                    int byLetter = ma.group(1).compareTo(mb.group(1));
                    if (byLetter != 0) {
                        return byLetter;
                    }
                    int byNumber = Integer.compare(Integer.parseInt(ma.group(2)),
                            Integer.parseInt(mb.group(2)));
                    if (byNumber != 0) {
                        return byNumber;
                    }
                    return ma.group(3).compareTo(mb.group(3));
                }
                return a.compareTo(b);
            }
        });
        for (Situation s : all) {
            roads.addAll(s.getRoads());
        }
        return new ArrayList<>(roads);
    }

    private static boolean inScope(Situation s, int scope) {
        switch (scope) {
            case SCOPE_INCIDENTS:
                return !s.isRoadworks();
            case SCOPE_ROADWORKS:
                return s.isRoadworks();
            default:
                return true;
        }
    }
}
