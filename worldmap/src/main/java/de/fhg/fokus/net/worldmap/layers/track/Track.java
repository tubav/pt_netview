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
package de.fhg.fokus.net.worldmap.layers.track;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.worldmap.MapMarker;
/**
 * @author FhG-FOKUS NETwork Research
 * 
 */
public class Track implements Comparable<Track> {

    private static Logger logger = LoggerFactory.getLogger(Track.class);

    private ArrayList<Bearer> bearerList = new ArrayList<Bearer>();
    private long trackId;
    private boolean bearerView = false;
    private long startTs;
    private long stopTs;
    
    public View view;
    public long avgDelay;

    public long getTrackId() {
        return trackId;
    }

    public void setTrackId(long trackId) {
        this.trackId = trackId;
    }
    
    public void clearBearerList() {
        bearerList.clear();
    }

    public void setBearerList(ArrayList<Bearer> bearerList) {
        this.bearerList = bearerList;
    }
    
    public boolean isBearerId(long bid) {
        if(this.hasBearers()) {
            for(Bearer b : bearerList) {
                if(b.getBearerId() == bid)
                    return true;
            }
        }
        return false;
    }
    
    public boolean isFlowId(long fid) {
        if(this.hasBearers()) {
            for(Bearer b : bearerList) {
                if(b.isFlowId(fid))
                    return true;
            }
        }
        return false;
    }
    
    public ArrayList<Bearer> getBearers() {
        return bearerList;
    }
    
    public void setBearers(boolean bearerView) {
        this.bearerView = bearerView;
    }
    
    public boolean hasBearers() {
        return bearerView;
    }

    public Bearer getBearer(long bid) {
        for(Bearer bearer : bearerList) {
            if(bearer.getBearerId() == bid)
                return bearer;
        }
        return null;
    }
    
    public Flow getFlow(long fid) {
        for(Bearer bearer : bearerList) {
            for(Flow flow : bearer.getFlows()) {
                if(flow.getFlowId() == fid)
                    return flow;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        for (MapMarker marker : view.markers) {
            sbuf.append("- " + marker + "\n");
        }
        return String.format("Track: {uid: %d, type:'%s', startTs: %d, stopTs: %d } \nmarkers:\n%s",
                trackId, view.type.toString(), startTs, stopTs, sbuf.toString());
    }

    public long getStartTs() {
        return startTs;
    }

    public void setStartTs(long startTs) {
        this.startTs = startTs;
    }

    public long getStopTs() {
        return stopTs;
    }

    public void setStopTs(long stopTs) {
        this.stopTs = stopTs;
    }

    public long getAvgDelay() {
        return avgDelay;
    }

    public void setAvgDelay(long avgDelay) {
        this.avgDelay = avgDelay;
    }

    @Override
    public int compareTo(Track o) {
        return Long.valueOf(startTs).compareTo(o.getStartTs());
    }
}
