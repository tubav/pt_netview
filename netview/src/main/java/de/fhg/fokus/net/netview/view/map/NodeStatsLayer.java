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

package de.fhg.fokus.net.netview.view.map;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.geo.WayPoint;
import de.fhg.fokus.net.netview.model.Node;
import de.fhg.fokus.net.netview.model.db.ProbeLocation;
import de.fhg.fokus.net.netview.model.db.ProbeStats;
import de.fhg.fokus.net.netview.model.db.TrackRepository;
import de.fhg.fokus.net.netview.view.NodeViewUtil;
import de.fhg.fokus.net.worldmap.MapMarker;
import de.fhg.fokus.net.worldmap.WorldMap;
import de.fhg.fokus.net.worldmap.layers.Layer;
import de.fhg.fokus.net.worldmap.layers.track.TrackPlayer;
import de.fhg.fokus.net.worldmap.layers.track.TrackPlayer.EventData;
import de.fhg.fokus.net.worldmap.model.WayPointScreenLocator;
import de.fhg.fokus.net.worldmap.util.EventSupport.EventListener;
import java.util.HashMap;
import java.util.List;

public class NodeStatsLayer extends Layer {
	private static final Logger logger = LoggerFactory.getLogger(NodeStatsLayer.class);	
	private static final long serialVersionUID = 7139552703424473941L;
	
	private final Font font = new Font("Monospaced", Font.BOLD, 11);
	private Set<Node> nodes = new HashSet<Node>();
	private WayPointScreenLocator waypointScreenLocator;
	private long currentTs; 
	private final TrackRepository db;
	private final Map<Long, ProbeStats> probeStats = new ConcurrentHashMap<Long, ProbeStats>();
        private WorldMap worldmap = null;
	
	public NodeStatsLayer(String layerId, TrackRepository db) {
		super(layerId);
		this.db = db;
                this.db.setNodesHashSet(nodes);
	}
	
	public void addNode(Node node) {
		nodes.add(node);
	}

	public void removeAllNodes(){
		nodes.clear();
	}
	public void removeNode(Node node) {
		nodes.remove(node);
	}
	
	public void injectWaypointScreenLocator(WayPointScreenLocator waypointScreenLocator) {
		this.waypointScreenLocator = waypointScreenLocator;
	}

        public long getCurrentTs(){
                return this.currentTs;
        }
	
	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		Font oldFont = g2.getFont();
		Color oldColor = g2.getColor();
		FontMetrics fm;
		
		// anti-alias
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setFont(font);
		g2.setColor(Color.WHITE);
		fm = g2.getFontMetrics();
		
		//g2.drawString("ts: " + currentTs, 10, 10);

		for(Node n : nodes) {
                    
			Point p = new Point();
                        
			WayPoint wp = n.getPhy().getWaypoint();

			waypointScreenLocator.setXYFromWaypoint(p, wp);
			
			String s = getStatLine(n);
			g2.drawString(s, p.x - (fm.stringWidth(s)/2), p.y+fm.getHeight());
		}
		g2.setFont(oldFont);
		g2.setColor(oldColor);
	}

	private String getStatLine(Node n) {
		ProbeStats stats = probeStats.get(n.getUid());
		StringBuffer sb = new StringBuffer();
		
		if(stats != null) {
			sb.append(String.format("C: %.1f%%", (1 - stats.getSystemCpuIdle()) * 100));
			sb.append(" | ");
			sb.append("R: " + formatByteCount(stats.getSystemMemFree()*1024));
			
		} else {
			sb.append("C: - | R: - ");
		}
		return sb.toString();
	}

	private String formatByteCount(long c) {
		final String[] units = { "B", "KB", "MB", "GB", "TB", "PB" };
		double v = c;
		int unit = 0;
		
		while(v > 1024 && unit < (units.length-1)) {
			v /= 1024;
			unit++;
		}
		
		return String.format("%.1f%s", v, units[unit]);
	}

	public void setTrackPlayer(TrackPlayer trackPlayer) {
		if(trackPlayer != null) {
			trackPlayer.addEventListener(TrackPlayer.State.PLAYING, new EventListener<TrackPlayer.EventData>() {
				@Override
				public void onEvent(EventData e) {
					updateStats(e.currentTs);
                                        updateLocation(e.currentTs);
				}
			});
			trackPlayer.addEventListener(TrackPlayer.State.STOPPED, new EventListener<TrackPlayer.EventData>() {
				@Override
				public void onEvent(EventData e) {
					updateStats(e.currentTs);
                                        updateLocation(e.currentTs);
				}
			});
			trackPlayer.addEventListener(TrackPlayer.State.PAUSED, new EventListener<TrackPlayer.EventData>() {
				@Override
				public void onEvent(EventData e) {
					updateStats(e.currentTs);
                                        updateLocation(e.currentTs);
				}
			});
		}
	}

        public Map<Long,String> previousNames = new HashMap<Long,String>();
        public Map<Long,String> previousLocationNames = new HashMap<Long,String>();
        public Map<Long,String> previousAddresses = new HashMap<Long,String>();

        public void updateLocation(long currentTs) {

                if(!isVisible()) {
			return;
		}
		for(Node n : nodes) {

			ProbeLocation location = db.getLatestProbeLocation(n, currentTs);
			if(location == null) {
                                //logger.debug("No Entry for this point!");
			}
                        else {
                                //logger.debug("Entry exists: "+location.toString());
                                // Setting the current Position
                                try{
                                        double lat = Double.parseDouble(location.getLatitude());
                                        double lon = Double.parseDouble(location.getLongitude());
                                        n.getPhy().setWaypoint(n,new WayPoint(lat,lon));
                                }
                                catch(NumberFormatException e){
                                        //logger.debug("No Location for this point - Marker-Position will be unchanged!");
                                }

                                // Setting the current Label
                                String tempName;
                                if(location.getName().equals("")){
                                        if(previousNames.containsKey(n.getMp().getProbe().getProbeId())){
                                                tempName = (String)previousNames.get(n.getMp().getProbe().getProbeId());
                                                //logger.debug("Using previous name: "+tempName);
                                        }
                                        else{
                                                tempName = "Unknown";
                                        }
                                        
                                }
                                else{
                                        tempName = location.getName();
                                        previousNames.put(n.getMp().getProbe().getProbeId(),tempName);
                                }
                                String tempLocationName;
                                if(location.getLocationName().equals("")){
                                        if(previousLocationNames.containsKey(n.getMp().getProbe().getProbeId())){
                                                tempLocationName = (String)previousLocationNames.get(n.getMp().getProbe().getProbeId());
                                                //logger.debug("Using previous location_name: "+tempLocationName);
                                        }
                                        else{
                                                tempLocationName = "Unknown";
                                        }
                                }
                                else{
                                        tempLocationName = location.getLocationName();
                                        previousLocationNames.put(n.getMp().getProbe().getProbeId(),tempLocationName);
                                }
                                n.getView().setLabel(tempName+"@"+tempLocationName);

                                // Setting the current IP Address
                                String tempAddress;
                                if(location.getAddress().equals("")){
                                        if(previousAddresses.containsKey(n.getMp().getProbe().getProbeId())){
                                                tempAddress = (String)previousAddresses.get(n.getMp().getProbe().getProbeId());
                                                //logger.debug("Using previous address: "+tempAddress);
                                        }
                                        else{
                                                tempAddress = "Unknown IP";
                                        }
                                }
                                else{
                                        tempAddress = location.getAddress();
                                        previousAddresses.put(n.getMp().getProbe().getProbeId(),tempAddress);
                                }
                                n.getMp().getProbe().setLabel(tempAddress);

                                // Update the Markers
                                List<MapMarker> markers = worldmap.getMapMarkers();
                                for (MapMarker marker : markers){
                                        if(marker.oid == n.getMp().getProbe().getProbeId() && marker instanceof NetViewMapMarker) {
                                                NetViewMapMarker nvm = (NetViewMapMarker)marker;
                                                nvm.setToolTipText(NodeViewUtil.renderHtml(n));
                                        }
                                }
			}
		}

        }

        public void setWorldmap(WorldMap worldmap){
                this.worldmap = worldmap;
        }
	
	private void updateStats(long currentTs) {
		
		if(!isVisible()) {
			return;
		}
		
		if(this.currentTs != currentTs) {
			this.currentTs = currentTs;
			for(Node n : nodes) {
				ProbeStats stats = db.getLatestNodeSystemStats(n, currentTs);
				if(stats == null) {
					probeStats.remove(n.getUid());
				} else {
					probeStats.put(n.getUid(), stats);
				}
			}
		} else {
			/* NOP */
		}
	}
}
