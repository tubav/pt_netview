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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.netview.view.map.NetViewMapMarker;

@Entity
public class NodeViewProperties {
	// sys
	private static Logger logger = LoggerFactory.getLogger(NodeViewProperties.class);
	
	// --
	@Id
	private long uid;
	
	/**
	 * Label to be displayed
	 */
	@Column
	public String label;

	@Column
	public String icon;
	
	@Transient
	public NetViewMapMarker marker;
	/**
	 * Helper for getting label for display
	 * @param node
	 * @return
	 */
	public static String getLabel( Node node ){
		if( node==null){
			return "not set";
		}
		if( node.view==null || node.view.label == null ){
			logger.warn("node label is null, node.uid={}",node.uid);
			return "not set";
		}
		return node.view.label;
	}
	/**
	 * Return map marker
	 * @param node
	 * @return node map marker or null if it does not exist
	 */
	public static NetViewMapMarker getMapMarker( Node node ){
		if( node!=null && node.view!=null && node.view.marker != null){
			return node.view.marker;
		}
		return null;
	}
	public String getLabel() {
		return label;
	}
	
	public void setLabel(String label) {
		this.label = label;
	}
	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}
	
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}

	/**
	 * Set node label. It allocate view structure as needed.
	 * 
	 * @param node 
	 * @param label
	 */
	public static void setLabel(Node node, String label ){
		if( node==null){
			logger.warn("trying to set the label '{}' to a null node ", label);
			return;
		}
		synchronized (node) {
			if( node.view==null){
				node.view = new NodeViewProperties();
			}
			node.view.setLabel(label);
		}
	}
	/**
	 * Return icon path or null if not set
	 * @param node
	 * @return
	 */
	public static String getIcon(Node node) {
		if( node!=null && node.view!=null && node.view.icon!=null){
			return node.view.icon;
		}
		return null;
	}
	
	
}
