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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Entity
public class NodeNetworkProperties {
	// sys
	private static Logger logger = LoggerFactory.getLogger(NodeNetworkProperties.class);
	// --
	@Id
	public long uid;
	@Column
	public String hostname;
	@Column
	public String address;

	public long getUid() {
		return uid;
	}
	public void setUid(long uid) {
		this.uid = uid;
	}
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	/**
	 * A null pointer exception safe getter.
	 * 
	 * @param node
	 * @return
	 */
	public static String getHostname( Node node ){
		String hostname = "not set";
		if( node!=null && node.net!=null && node.net.hostname!=null){
			hostname = node.net.hostname;
		}
		return hostname;
	}
	/**
	 * A null pointer exception safe getter.
	 * 
	 * @param node
	 * @return
	 */
	public static String getAddress( Node node ){
		String address = "not set";
		if( node!=null && node.net!=null && node.net.address!=null){
			address = node.net.address;
		}
		return address;
	}
	/**
	 * Set node's address allocating node network properties if needed.
	 * 
	 * @param node
	 * @param address
	 */
	public static synchronized void setAddress( Node node, String address ){
		if( node==null){
			logger.warn("trying to set the address '{}' to a null node ", address );
			return;
		}
		if( node.net==null){
			node.net = new NodeNetworkProperties();
		}
		node.net.setAddress(address);
	}
	/**
	 * Set node's hostname allocating node network properties if needed
	 * 
	 * @param node
	 * @param hostname
	 */
	public static synchronized void setHostname( Node node, String hostname ){
		if( node==null){
			logger.warn("trying to set the hostname '{}' to a null node ", hostname );
			return;
		}
		if( node.net==null){
			node.net = new NodeNetworkProperties();
		}
		node.net.setHostname(hostname);
		
	}

}
