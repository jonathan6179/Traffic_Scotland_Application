package org.me.gcu.mboue_jonathan_s2028595.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.me.gcu.mboue_jonathan_s2028595.model.Situation;
import org.me.gcu.mboue_jonathan_s2028595.model.SituationType;
import org.xmlpull.v1.XmlPullParserException;

import java.util.List;

public class SituationParserTest {

    @Test
    public void parsesIncidentFields() throws Exception {
        String xml = TestFeeds.feed(TestFeeds.incident("I_1", "RoadOrCarriagewayOrLaneManagement",
                "The M8 eastbound is closed.", "roadOrCarriagewayOrLaneManagementType",
                "roadClosed", "M8", 55.86, -4.27));
        List<Situation> list = SituationParser.parse(xml, false);

        assertEquals(1, list.size());
        Situation s = list.get(0);
        assertEquals("I_1", s.getId());
        assertEquals("RoadOrCarriagewayOrLaneManagement", s.getRecordType());
        assertEquals("The M8 eastbound is closed.", s.getComment());
        assertEquals(SituationType.ROAD_CLOSED, s.getType());
        assertFalse(s.isRoadworks());
        assertEquals("Eastbound", s.getDirectionLabel());
        assertEquals(2, s.getLanesRestricted());
        assertTrue(s.getRoads().contains("M8"));
        assertNotNull(s.getStartTime());
        assertNull(s.getEndTime());
        assertEquals(2, s.getLocations().size());
        assertEquals(2, s.getPoints().size());
    }

    @Test
    public void parsesFromAndToCoordinates() throws Exception {
        String xml = TestFeeds.feed(TestFeeds.incident("I_1", "AbnormalTraffic", "Queue",
                "abnormalTrafficType", "queuingTraffic", "M8", 55.5, -4.5));
        Situation s = SituationParser.parse(xml, false).get(0);
        assertEquals("to", s.getLocations().get(0).getRole());
        assertEquals(55.51, s.getLocations().get(0).getLatitude(), 1e-9);
        assertEquals("from", s.getLocations().get(1).getRole());
        assertEquals(55.5, s.getLocations().get(1).getLatitude(), 1e-9);
        assertEquals(-4.5, s.getLocations().get(1).getLongitude(), 1e-9);
    }

    @Test
    public void parsesRoadworksDelaysAndEndTime() throws Exception {
        String xml = TestFeeds.feed(
                TestFeeds.roadworks("D_1", "M8 WB Woodside - Closure", "longDelays", false, "M8", 55.87, -4.26),
                TestFeeds.roadworks("D_2", "A9 lane closure", null, true, "A9", 56.0, -3.9));
        List<Situation> list = SituationParser.parse(xml, true);

        assertEquals(2, list.size());
        assertTrue(list.get(0).isRoadworks());
        assertEquals(Situation.DelayLevel.LONG_DELAYS, list.get(0).getDelayLevel());
        assertEquals(SituationType.ROADWORKS, list.get(0).getType());
        assertNotNull(list.get(0).getEndTime());
        assertEquals(Situation.DelayLevel.NONE, list.get(1).getDelayLevel());
        assertEquals(SituationType.URGENT_ROADWORKS, list.get(1).getType());
    }

    @Test
    public void allThreeDelayLevelsAreRecognised() {
        assertEquals(Situation.DelayLevel.DELAYS, Situation.DelayLevel.from("delays"));
        assertEquals(Situation.DelayLevel.LONG_DELAYS, Situation.DelayLevel.from("longDelays"));
        assertEquals(Situation.DelayLevel.VERY_LONG_DELAYS, Situation.DelayLevel.from("veryLongDelays"));
        assertEquals(Situation.DelayLevel.NONE, Situation.DelayLevel.from(""));
        assertEquals(Situation.DelayLevel.NONE, Situation.DelayLevel.from(null));
    }

    @Test
    public void slipRoadAndContraflowAreClassified() throws Exception {
        String slip = TestFeeds.feed(TestFeeds.incident("I_1", "RoadOrCarriagewayOrLaneManagement",
                "The M8 Junction 2a off slip is closed.", "roadOrCarriagewayOrLaneManagementType",
                "roadClosed", "M8", 55.8, -4.0));
        assertEquals(SituationType.SLIP_ROAD, SituationParser.parse(slip, false).get(0).getType());

        String contraflow = TestFeeds.feed(TestFeeds.incident("I_2", "RoadOrCarriagewayOrLaneManagement",
                "Slip roads closed and a contraflow is in place.", "roadOrCarriagewayOrLaneManagementType",
                "roadClosed", "A90", 56.7, -2.8));
        assertEquals(SituationType.LANE_RESTRICTION,
                SituationParser.parse(contraflow, false).get(0).getType());
    }

    @Test
    public void unknownTagsAreIgnoredAndRoadsAreExtractedFromText() throws Exception {
        String xml = TestFeeds.feed(TestFeeds.incident("I_1", "AbnormalTraffic",
                "Queues on the A74(M) near the A702 junction", "abnormalTrafficType",
                "queuingTraffic", "A74(M)", 55.3, -3.4));
        Situation s = SituationParser.parse(xml, false).get(0);
        assertTrue(s.getRoads().contains("A74(M)"));
        assertTrue(s.getRoads().contains("A702"));
        assertEquals(SituationType.QUEUING_TRAFFIC, s.getType());
    }

    @Test
    public void feedWithNoSituationsGivesEmptyList() throws Exception {
        assertTrue(SituationParser.parse(TestFeeds.feed(), false).isEmpty());
    }

    @Test
    public void missingCoordinatesGiveNoLocation() throws Exception {
        String xml = TestFeeds.feed("<situation id=\"X\"><situationRecord xsi:type=\"AbnormalTraffic\">"
                + "<generalPublicComment><comment><values><value>No place given</value></values>"
                + "</comment></generalPublicComment></situationRecord></situation>");
        Situation s = SituationParser.parse(xml, false).get(0);
        assertFalse(s.hasLocation());
        assertNull(s.getCentre());
    }

    @Test(expected = XmlPullParserException.class)
    public void malformedXmlThrows() throws Exception {
        SituationParser.parse("<root><situation id=\"1\"><oops></root>", false);
    }
}
