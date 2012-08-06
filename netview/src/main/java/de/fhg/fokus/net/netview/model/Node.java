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

import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToOne;
import javax.persistence.Transient;


/**
 * Node model
 * 
 * @author FhG-FOKUS NETwork Research
 *
 */
//@NamedQueries(
//		value={@NamedQuery(
//				name="nodeFromNetwork",
//				query=""
//
//		)}		
//)

@Entity
public class Node {
	/**
	 * Unique id. Every node must have an unique id. This id is unique across the application.
	 * 
	 */
	@Id
	public long uid;

	/**
	 * <p>View properties. This is used by renderer to display 
	 * the node;</p>
	 * <p> <b>Note:</b> this field can be null!
	 * 
	 */
	@OneToOne(cascade=CascadeType.ALL)
	public NodeViewProperties view;
	/**
	 * <p>Physical properties. Holds objects like a Waypoint. </p>
	 * 
	 * <p> <b>Note:</b> this field can be null!
	 */

	@OneToOne(cascade=CascadeType.ALL)
	public NodePhysicalProperties phy;
	/**
	 * <p>Network properties. </p>
	 * 
	 * <p> <b>Note:</b> this field can be null!
	 */
	@Transient
	public NodeNetworkProperties net;

	@OneToOne(cascade=CascadeType.ALL)
	public NodeMeasurementProperties mp;

	@ManyToMany(cascade=CascadeType.ALL)
	private Set<Network> networks;
	
	/**
	 * Node model
	 */
	public Node() {

	}

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public NodeViewProperties getView() {
		return view;
	}

	public void setView(NodeViewProperties view) {
		this.view = view;
	}

	public NodeMeasurementProperties getMp() {
		return mp;
	}

	public void setMp(NodeMeasurementProperties mp) {
		this.mp = mp;
	}

	public NodePhysicalProperties getPhy() {
		return phy;
	}

	public void setPhy(NodePhysicalProperties phy) {
		this.phy = phy;
	}
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Node) {
			Node node = (Node) obj;
			return node.uid == uid;
		}
		return false;
	}
	
	
	public Set<Network> getNetworks() {
		return networks;
	}

	public void setNetworks(Set<Network> networks) {
		this.networks = networks;
	}

	@Override
	public int hashCode() {
		return (int)uid;
	}
	/**
	 * Deep copy a node
	 *  
	 * @param src 
	 * @param dst
	 * @return
	 */
	public static Node deepcopy( Node src, Node dst ){
		if( src==null || dst==null){
			return null;
		}
		NodeViewProperties.setLabel(dst, NodeViewProperties.getLabel(src));
		NodePhysicalProperties.setWaypoint(dst, NodePhysicalProperties.getWaypoint(src));
		NodeMeasurementProperties.setProbe(dst, NodeMeasurementProperties.getProbe(src));

		return dst;
	}


}
