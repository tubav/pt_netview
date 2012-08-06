/* Netview - a software component to visualize packet tracks, hop-by-hop delays,
 *           sampling stats and resource consumption. Netview requires the deployment of
 *           distributed probes (impd4e) and a central packet matcher to correlate the
 *           obervations.
 *
 *           The probe can be obtained at http://impd4e.sourceforge.net/downloads.html
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

package de.fhg.fokus.net.netview.model;

import java.util.List;
import java.util.concurrent.TimeUnit;

import de.fhg.fokus.net.worldmap.MapMarker;
import de.fhg.fokus.net.worldmap.layers.track.Track;
import de.fhg.fokus.net.worldmap.layers.track.Flow;
import de.fhg.fokus.net.worldmap.layers.track.Bearer;
import de.fhg.fokus.net.worldmap.layers.track.View;

public class ObjectFactory {
	
	public static Track createAnimated(long trackId, long startTs,
            long stopTs, int volume, long avgDelay,
            List<MapMarker> markers, TimeUnit timeUnit ) {
		Track track = new Track();
		track.setStartTs( timeUnit.convert(startTs, timeUnit));
		track.setStopTs( timeUnit.convert(stopTs, timeUnit) );
		track.setTrackId(trackId);
		track.setAvgDelay(avgDelay);
        
        track.view = ViewFactory.createAnimatedView(track.getTrackId(),
                View.Type.ANIMATED, volume, markers);
		return track;
	}
    
    public static Flow createAnimated(Bearer bearer, long flowId, String srcIp,
            String dstIp, int srcPort, int dstPort, int volume,
            List<MapMarker> markers, double curveFactor) {
        
        Flow flow = new Flow();
        flow.setBearer(bearer);
        flow.setFlowId(flowId);
        flow.setSrcIp(srcIp);
        flow.setSrcPort(srcPort);
        flow.setDstIp(dstIp);
        flow.setDstPort(dstPort);
        
        flow.view = ViewFactory.createAnimatedView(flow.getFlowId(),
                View.Type.ANIMATED, volume, markers);
        flow.view.curveFactor = curveFactor;
        return flow;
    }
    
    public static Bearer createAnimated(Track track, int volume, long bearerId,
            long ruleId, String apn, String ruleName, String imsi,
            long qci, long maxDownload, long maxUpload,
            long guaDownload, long guaUpload, long apnDownload,
            long apnUpload, String srcIp, String dstIp, int srcPort,
            int dstPort, List<MapMarker> markers, double curveFactor) {
        Bearer bearer = new Bearer();
        bearer.setTrack(track);
        bearer.setBearerId(bearerId);
        bearer.setRuleId(ruleId);
        bearer.setApn(apn);
        bearer.setRuleName(ruleName);
        bearer.setImsi(imsi);
        bearer.setQci(qci);
        bearer.setMaxDownload(maxDownload);
        bearer.setMaxUpload(maxUpload);
        bearer.setGuaDownload(guaDownload);
        bearer.setGuaUpload(guaUpload);
        bearer.setApnDownload(apnDownload);
        bearer.setApnUpload(apnUpload);
        bearer.setSrcIp(srcIp);
        bearer.setDstIp(dstIp);
        bearer.setSrcPort(srcPort);
        bearer.setDstPort(dstPort);
        
        bearer.view = ViewFactory.createAnimatedView(bearer.getBearerId(),
                View.Type.ANIMATED, volume, markers);
        bearer.view.curveFactor = curveFactor;
        return bearer;
    }
}
