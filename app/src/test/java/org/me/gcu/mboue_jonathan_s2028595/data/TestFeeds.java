package org.me.gcu.mboue_jonathan_s2028595.data;

/** Builds small DATEX II style feeds for the unit tests. */
final class TestFeeds {
    private TestFeeds() {
    }

    static String feed(String... situations) {
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .append("<root xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><payload>");
        for (String s : situations) {
            sb.append(s);
        }
        return sb.append("</payload></root>").toString();
    }

    static String point(String tag, double lat, double lon, String road) {
        return "<" + tag + " xsi:type=\"TpegJunction\"><pointCoordinates><latitude>" + lat
                + "</latitude><longitude>" + lon + "</longitude></pointCoordinates>"
                + "<ilc><descriptor><values><value lang=\"en\">" + road
                + "</value></values></descriptor></ilc></" + tag + ">";
    }

    /** One unplanned incident. typeTag/typeValue is e.g. roadOrCarriagewayOrLaneManagementType/roadClosed. */
    static String incident(String id, String recordType, String comment, String typeTag,
                           String typeValue, String road, double lat, double lon) {
        return "<situation id=\"" + id + "\" version=\"1\"><situationRecord xsi:type=\""
                + recordType + "\" id=\"" + id + "\" version=\"2\">"
                + "<situationRecordCreationTime>2026-08-10T11:38:10+01:00</situationRecordCreationTime>"
                + "<probabilityOfOccurrence>certain</probabilityOfOccurrence>"
                + "<validity><validityStatus>definedByValidityTimeSpec</validityStatus>"
                + "<validityTimeSpecification><overallStartTime>2026-08-10T11:37:42+01:00</overallStartTime>"
                + "</validityTimeSpecification></validity>"
                + "<impact><numberOfLanesRestricted>2</numberOfLanesRestricted></impact>"
                + "<generalPublicComment><comment><values><value lang=\"en\">" + comment
                + "</value></values></comment></generalPublicComment>"
                + "<groupOfLocations xsi:type=\"Linear\"><tpegLinearLocation>"
                + "<tpegDirection>eastBound</tpegDirection>"
                + point("to", lat + 0.01, lon + 0.01, road) + point("from", lat, lon, road)
                + "</tpegLinearLocation></groupOfLocations>"
                + "<unknownFutureTag>ignored</unknownFutureTag>"
                + "<" + typeTag + ">" + typeValue + "</" + typeTag + ">"
                + "</situationRecord></situation>";
    }

    static String roadworks(String id, String comment, String delays, boolean urgent,
                            String road, double lat, double lon) {
        return "<situation id=\"" + id + "\" version=\"1\"><situationRecord xsi:type=\"MaintenanceWorks\" id=\""
                + id + "\" version=\"1\">"
                + "<validity><validityTimeSpecification>"
                + "<overallStartTime>2026-08-23T23:50:00+01:00</overallStartTime>"
                + "<overallEndTime>2026-09-23T23:59:00+01:00</overallEndTime>"
                + "</validityTimeSpecification></validity>"
                + (delays == null ? "" : "<impact><delays><delaysType>" + delays
                + "</delaysType></delays></impact>")
                + "<generalPublicComment><comment><values><value lang=\"en\">" + comment
                + "</value></values></comment></generalPublicComment>"
                + "<groupOfLocations xsi:type=\"Linear\"><tpegLinearLocation>"
                + "<tpegDirection>westBound</tpegDirection>"
                + point("to", lat + 0.01, lon, road) + point("from", lat, lon, road)
                + "</tpegLinearLocation></groupOfLocations>"
                + "<urgentRoadworks>" + urgent + "</urgentRoadworks>"
                + "<roadMaintenanceType>roadworks</roadMaintenanceType>"
                + "</situationRecord></situation>";
    }
}
