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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Network model
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */

@Entity
public class Network implements Comparable<Network> {
	// sys
	private transient final Logger logger = LoggerFactory.getLogger(getClass());

	// properties
	@Id
	private long uid;
	@Column
	private String label;
	// used by the ORM
	
	@ManyToMany(cascade=CascadeType.ALL)
	private Set<Node> nodes;

	public static enum NodeGroupEvent {
		/**
		 * <p>
		 * A node with a waypoint is available.
		 * </p>
		 * 
		 * <pre>
		 * node.view != null
		 * node.phy != null
		 * </pre>
		 * 
		 */
		NEW_NODE_WITH_WAYPOINT

	}

	public Network() {
	}

	public Network(String label) {
		this.label = label;
	}


	/**
	 * Add node 
	 * 
	 * @param node
	 */
	public void addNode(Node node) {
		if (node == null) {
			logger.warn("node should not be null, ignoring it");
			return;
		}
	}

	public void addNodes(Node... nodes) {
		for (Node node : nodes) {
			addNode(node);
		}
	}

	public long getUid() {
		return uid;
	}

	public void setUid(long uid) {
		this.uid = uid;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Network) {
			Network net = (Network) obj;
			return net.uid == uid;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (int) uid;
	}

	public Set<Node> getNodes() {
		return nodes;
	}

	public void setNodes(Set<Node> nodes) {
		this.nodes = nodes;
	}
	
	@Override
	public String toString() {
		if(label!=null){
			return label;
		}
		return super.toString();
	}

	@Override
	public int compareTo(Network net) {
		return (int)(uid-net.uid);
	}
	
}
