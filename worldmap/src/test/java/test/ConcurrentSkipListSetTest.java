/**
 * worldmap - an extension to JMapViewer which provides additional
 *            functionality. New functions allow setting markers,
 *            adding layers, and printing tracks on the map. (see
 *            http://wiki.openstreetmap.org/wiki/JMapViewer for more
 *            information on JMapViewer)
 *
 * Copyright (c) 2011
 *
 * Fraunhofer FOKUS
 * www.fokus.fraunhofer.de
 *
 * in cooperation with
 *
 * Technical University Berlin
 * www.av.tu-berlin.de
 *
 * Ramon Masek <ramon.masek@fokus.fraunhofer.de>
 * Christian Henke <c.henke@tu-berlin.de>
 * Carsten Schmoll <carsten.schmoll@fokus.fraunhofer.de>
 * Julian Vetter <julian.vetter@fokus.fraunhofer.de>
 * Jens Krenzin <jens.krenzin@fokus.fraunhofer.de>
 * Michael Gehring <michael.gehring@fokus.fraunhofer.de>
 * Tacio Grespan Santos
 * Fabian Wolff
 *
 * For questions/comments contact packettracking@fokus.fraunhofer.de
 *
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <http://www.gnu.org/licenses/>.
 */
package test;

import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.assertEquals;

import java.util.Comparator;
import java.util.concurrent.ConcurrentSkipListSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.worldmap.MapMarker;
import de.fhg.fokus.net.worldmap.MapMarkerDot;

public class ConcurrentSkipListSetTest {

    private static Logger logger = LoggerFactory.getLogger(ConcurrentSkipListSetTest.class);

    public static void main(String[] args) {
        logger.debug("== Rest ==");

        ConcurrentSkipListSet<MapMarker> markerSet = new ConcurrentSkipListSet<MapMarker>(
                new Comparator<MapMarker>() {
//					@Override
//					public boolean equals(Object obj) {
//						return this.equals(obj);
//					}

                    @Override
                    public int compare(MapMarker o1,
                            MapMarker o2) {
                        if (o1.getLatitude() == o2.getLatitude() && o1.equals(o2)) {
                            return 0;
                        }
                        return o1.getLatitude() > o2.getLatitude() ? 1 : -1;
                    }
                });
//		markerSet = new ConcurrentSkipListSet<MapMarker>();
        MapMarker start, end;

        markerSet.add(start = new MapMarkerDot(10, 15));
        markerSet.add(new MapMarkerDot(10, 10));
        markerSet.add(new MapMarkerDot(11, 10));
        markerSet.add(new MapMarkerDot(12, 10));
        markerSet.add(end = new MapMarkerDot(13, 10));

        logger.debug("size: " + markerSet.size());
        logger.debug("first: " + markerSet.first());
        logger.debug("last: " + markerSet.last());
        logger.debug("==Contains==");
        if (markerSet.contains(start)) {
            logger.debug("Contains: " + start);
        }

        logger.debug("==Subset==");
        for (MapMarker m : markerSet.subSet(start, end)) {
            logger.debug(m + "");
        }
        logger.debug("== ALL ==");
        for (MapMarker m : markerSet) {
            logger.debug(m + "");
        }


    }

    @Ignore
    @Test
    public void dummy() {
        assertEquals(true, true);
    }
}
