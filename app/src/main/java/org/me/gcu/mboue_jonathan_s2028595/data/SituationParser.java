package org.me.gcu.mboue_jonathan_s2028595.data;

import org.me.gcu.mboue_jonathan_s2028595.model.Situation;
import org.me.gcu.mboue_jonathan_s2028595.model.SituationLocation;
import org.me.gcu.mboue_jonathan_s2028595.model.SituationType;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses the Traffic Scotland DATEX II streams (unplanned events and current
 * roadworks) with XmlPullParser. The XML arrives as a String, as in the
 * starter code. Unknown tags are skipped, so new fields in the feed cannot
 * crash the app.
 *
 * This class has no Android imports so it can run in plain JUnit tests.
 */
public final class SituationParser {

    private static final String XSI = "http://www.w3.org/2001/XMLSchema-instance";

    /** Road numbers such as M8, A9, B957, A74(M). */
    private static final Pattern ROAD =
            Pattern.compile("(?<![A-Za-z0-9])([ABM]\\d{1,4}(?:\\s?\\(M\\))?)(?![0-9])");

    private SituationParser() {
    }

    /**
     * @param xml          the downloaded feed
     * @param roadworksFeed true for the roadworks stream, false for unplanned events
     */
    public static List<Situation> parse(String xml, boolean roadworksFeed)
            throws XmlPullParserException, IOException {

        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(new StringReader(xml));

        List<Situation> result = new ArrayList<>();
        Deque<String> path = new ArrayDeque<>();
        Situation current = null;
        SituationLocation location = null;

        int event = xpp.getEventType();
        while (event != XmlPullParser.END_DOCUMENT) {

            if (event == XmlPullParser.START_TAG) {
                String tag = xpp.getName();
                path.push(tag);

                if ("situation".equals(tag)) {
                    current = new Situation();
                    current.setRoadworks(roadworksFeed);
                    current.setId(xpp.getAttributeValue(null, "id"));
                    current.setVersion(xpp.getAttributeValue(null, "version"));
                    location = null;
                } else if (current != null) {
                    if ("situationRecord".equals(tag)) {
                        current.setRecordType(xpp.getAttributeValue(XSI, "type"));
                    } else if (("from".equals(tag) || "to".equals(tag))
                            && inside(path, "tpegLinearLocation")) {
                        location = new SituationLocation(tag);
                    }
                }

            } else if (event == XmlPullParser.TEXT) {
                if (current != null) {
                    String text = xpp.getText();
                    if (text != null) {
                        text = text.trim();
                        if (!text.isEmpty()) {
                            handleText(current, location, path, text);
                        }
                    }
                }

            } else if (event == XmlPullParser.END_TAG) {
                String tag = xpp.getName();
                if (!path.isEmpty()) {
                    path.pop();
                }
                if (current != null) {
                    if (location != null && tag.equals(location.getRole())) {
                        current.addLocation(location);
                        location = null;
                    } else if ("situation".equals(tag)) {
                        finish(current);
                        result.add(current);
                        current = null;
                    }
                }
            }

            event = xpp.next();
        }
        return result;
    }

    /** Called for each text node; the innermost open tag says what the text is. */
    private static void handleText(Situation s, SituationLocation loc,
                                   Deque<String> path, String text) {
        String tag = path.peek();
        if (tag == null) {
            return;
        }
        switch (tag) {
            case "value":
                if (inside(path, "generalPublicComment")) {
                    s.appendComment(text);
                } else if (loc != null) {
                    loc.addName(text);
                }
                break;
            case "latitude":
                if (loc != null) {
                    loc.setLatitude(toDouble(text));
                }
                break;
            case "longitude":
                if (loc != null) {
                    loc.setLongitude(toDouble(text));
                }
                break;
            case "overallStartTime":
                s.setStartTime(toTime(text));
                break;
            case "overallEndTime":
                s.setEndTime(toTime(text));
                break;
            case "situationRecordCreationTime":
                s.setCreationTime(toTime(text));
                break;
            case "situationRecordVersionTime":
                s.setVersionTime(toTime(text));
                break;
            case "validityStatus":
                s.setValidityStatus(text);
                break;
            case "probabilityOfOccurrence":
                s.setProbability(text);
                break;
            case "complianceOption":
                s.setCompliance(text);
                break;
            case "sourceIdentification":
                s.setSource(text);
                break;
            case "tpegDirection":
                s.setDirection(text);
                break;
            case "numberOfLanesRestricted":
                s.setLanesRestricted(toInt(text));
                break;
            case "numberOfOperationalLanes":
                s.setOperationalLanes(toInt(text));
                break;
            case "delaysType":
                s.setDelaysType(text);
                break;
            case "urgentRoadworks":
                s.setUrgent(Boolean.parseBoolean(text));
                break;
            case "roadMaintenanceType":
            case "abnormalTrafficType":
            case "roadOrCarriagewayOrLaneManagementType":
                s.setSubType(text);
                break;
            default:
                break; // unknown or unused tag: ignore
        }
    }

    /** Second stage: break the text down further (roads) and classify. */
    private static void finish(Situation s) {
        addRoads(s, s.getComment());
        for (String name : s.getAllLocationNames()) {
            addRoads(s, name);
        }
        s.setType(classify(s));
    }

    private static void addRoads(Situation s, String text) {
        Matcher m = ROAD.matcher(text);
        while (m.find()) {
            s.addRoad(m.group(1).replace(" ", "").toUpperCase(Locale.UK));
        }
    }

    static SituationType classify(Situation s) {
        String sub = s.getSubType().toLowerCase(Locale.UK);
        String rec = s.getRecordType().toLowerCase(Locale.UK);
        String comment = s.getComment().toLowerCase(Locale.UK);

        if (s.isRoadworks() || rec.contains("maintenance")) {
            return s.isUrgent() ? SituationType.URGENT_ROADWORKS : SituationType.ROADWORKS;
        }
        if (sub.contains("roadclosed")) {
            // Slip roads closed with a contraflow on the main carriageway is a lane
            // restriction, not a closed road.
            if (comment.contains("contraflow")) {
                return SituationType.LANE_RESTRICTION;
            }
            return comment.contains("slip") ? SituationType.SLIP_ROAD : SituationType.ROAD_CLOSED;
        }
        if (rec.contains("abnormaltraffic") || sub.contains("traffic") || sub.contains("queu")) {
            return SituationType.QUEUING_TRAFFIC;
        }
        if (sub.contains("lane") || sub.contains("contraflow") || sub.contains("narrow")
                || s.getLanesRestricted() > 0) {
            return SituationType.LANE_RESTRICTION;
        }
        if (comment.contains("slip")) {
            return SituationType.SLIP_ROAD;
        }
        return SituationType.OTHER_INCIDENT;
    }

    private static boolean inside(Deque<String> path, String ancestor) {
        return path.contains(ancestor);
    }

    private static double toDouble(String text) {
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    private static int toInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static OffsetDateTime toTime(String text) {
        try {
            return OffsetDateTime.parse(text);
        } catch (RuntimeException e) {
            return null;
        }
    }
}
