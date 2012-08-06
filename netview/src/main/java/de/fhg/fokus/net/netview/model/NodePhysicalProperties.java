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

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.geo.WayPoint;

@Entity
public class NodePhysicalProperties {
	@Id
	private long uid;
	
	private static Logger logger = LoggerFactory.getLogger(NodePhysicalProperties.class);
	/**
	 * Waypoint, which can be null
	 */
	@OneToOne(cascade=CascadeType.ALL)
	public WayPoint waypoint;
	/**
	 * Helper for getting waypoint for display
	 * 
	 * @param node
	 * @return waypoint or null if it does not exist
	 */
	public static WayPoint getWaypoint( Node node ){
		if( node==null|| node.phy==null || node.phy.waypoint == null ){
			return null;
		}
		return node.phy.waypoint;
	}
	public static List<Double> getLatLonAlt( Node node ){
		List<Double> wp = new ArrayList<Double>();
		if( node != null && node.phy!=null && node.phy.waypoint!=null ){
			wp.add(node.phy.waypoint.latitude);
			wp.add(node.phy.waypoint.longitude);
			if (node.phy.waypoint.altitude!=0 ){
				wp.add(node.phy.waypoint.altitude);
			}
		}
		return wp;
	}
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}
	public WayPoint getWaypoint() {
		return waypoint;
	}
	public void setWaypoint(WayPoint waypoint) {
		this.waypoint = waypoint;
	}
	
	/**
	 * Set the waypoint allocating the physical properties if needed.
	 * It also updates the waypoint in map marker.
	 * 
	 * @param node
	 * @param waypoint
	 */
	public static synchronized void setWaypoint( Node node, WayPoint waypoint ){
		if( node==null){
			logger.warn("trying to set the waypoint  '{}' to a null node ", waypoint );
			return;
		}
		if( node.phy==null){
			node.phy = new NodePhysicalProperties();
		}
		node.phy.setWaypoint(waypoint);
		// synchronizing waypoint in map marker
		if( node.view!=null && node.view.marker!=null){
			node.view.marker.setWaypoint(waypoint);
		}
		
	}
}
