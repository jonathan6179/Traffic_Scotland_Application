package org.me.gcu.mboue_jonathan_s2028595.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.me.gcu.mboue_jonathan_s2028595.model.Situation;

import java.util.List;

public class SituationFilterTest {

    private List<Situation> incidents;
    private List<Situation> roadworks;
    private List<Situation> all;

    @Before
    public void setUp() throws Exception {
        // Glasgow area M8, an M80 entry, an A74(M) entry far south, an A9 entry in the Highlands.
        incidents = SituationParser.parse(TestFeeds.feed(
                TestFeeds.incident("I_M8", "AbnormalTraffic", "Queue on the M8 near Kingston Bridge",
                        "abnormalTrafficType", "queuingTraffic", "M8", 55.863, -4.270),
                TestFeeds.incident("I_M80", "AbnormalTraffic", "Queue on the M80",
                        "abnormalTrafficType", "queuingTraffic", "M80", 55.95, -4.05),
                TestFeeds.incident("I_A74", "RoadOrCarriagewayOrLaneManagement", "A74(M) slip closed",
                        "roadOrCarriagewayOrLaneManagementType", "roadClosed", "A74(M)", 55.10, -3.35)),
                false);
        roadworks = SituationParser.parse(TestFeeds.feed(
                TestFeeds.roadworks("D_M8", "M8 WB Woodside - Closure", "delays", false, "M8", 55.870, -4.268),
                TestFeeds.roadworks("D_A9", "A9 Dalwhinnie lane closure", "veryLongDelays", false, "A9", 56.93, -4.24)),
                true);
        all = new java.util.ArrayList<>(incidents);
        all.addAll(roadworks);
    }

    @Test
    public void roadSearchMatchesExactRoadOnly() {
        List<Situation> result = SituationFilter.byRoad(all, "M8", SituationFilter.SCOPE_ALL);
        assertEquals(2, result.size()); // I_M8 and D_M8, but not I_M80
        for (Situation s : result) {
            assertFalse(s.getId().equals("I_M80"));
        }
    }

    @Test
    public void roadSearchIsCaseAndSpaceInsensitive() {
        assertEquals(2, SituationFilter.byRoad(all, "m8", SituationFilter.SCOPE_ALL).size());
        assertEquals(1, SituationFilter.byRoad(all, " a74 (m) ", SituationFilter.SCOPE_ALL).size());
    }

    @Test
    public void roadNumberWithoutBracketMatchesMotorwayVariant() {
        assertEquals(1, SituationFilter.byRoad(all, "A74", SituationFilter.SCOPE_ALL).size());
    }

    @Test
    public void roadSearchRespectsScope() {
        assertEquals(1, SituationFilter.byRoad(all, "M8", SituationFilter.SCOPE_INCIDENTS).size());
        assertEquals(1, SituationFilter.byRoad(all, "M8", SituationFilter.SCOPE_ROADWORKS).size());
    }

    @Test
    public void nonRoadQueryDoesFreeTextSearch() {
        List<Situation> result = SituationFilter.byRoad(all, "Kingston Bridge", SituationFilter.SCOPE_ALL);
        assertEquals(1, result.size());
        assertEquals("I_M8", result.get(0).getId());
    }

    @Test
    public void tooShortOrUnknownQueryGivesNoResults() {
        assertTrue(SituationFilter.byRoad(all, "", SituationFilter.SCOPE_ALL).isEmpty());
        assertTrue(SituationFilter.byRoad(all, "M", SituationFilter.SCOPE_ALL).isEmpty());
        assertTrue(SituationFilter.byRoad(all, "A999", SituationFilter.SCOPE_ALL).isEmpty());
    }

    @Test
    public void distanceSearchIsSortedAndWithinRadius() {
        // Glasgow city centre
        List<Situation> near = SituationFilter.byDistance(all, 55.8642, -4.2518, 10,
                SituationFilter.SCOPE_ALL);
        assertEquals(2, near.size()); // the two M8 entries
        assertTrue(near.get(0).getDistanceKm() <= near.get(1).getDistanceKm());
        for (Situation s : near) {
            assertTrue(s.getDistanceKm() <= 10);
        }
    }

    @Test
    public void biggerRadiusFindsMore() {
        int small = SituationFilter.byDistance(all, 55.8642, -4.2518, 10, SituationFilter.SCOPE_ALL).size();
        int large = SituationFilter.byDistance(all, 55.8642, -4.2518, 30, SituationFilter.SCOPE_ALL).size();
        assertTrue(large > small);
    }

    @Test
    public void distanceSearchRespectsScope() {
        List<Situation> near = SituationFilter.byDistance(all, 55.8642, -4.2518, 10,
                SituationFilter.SCOPE_ROADWORKS);
        assertEquals(1, near.size());
        assertTrue(near.get(0).isRoadworks());
    }

    @Test
    public void haversineGlasgowToEdinburghIsAbout67Km() {
        double km = SituationFilter.haversineKm(55.8642, -4.2518, 55.9533, -3.1883);
        assertEquals(67.0, km, 1.0);
    }

    @Test
    public void knownRoadsAreSortedNumerically() {
        List<String> roads = SituationFilter.knownRoads(all);
        assertTrue(roads.contains("A9") && roads.contains("A74(M)"));
        assertTrue(roads.indexOf("A9") < roads.indexOf("A74(M)")); // 9 before 74, not "A74" before "A9"
        assertTrue(roads.indexOf("A74(M)") < roads.indexOf("M8")); // A roads before M roads
        assertTrue(roads.indexOf("M8") < roads.indexOf("M80"));    // 8 before 80
    }
}
