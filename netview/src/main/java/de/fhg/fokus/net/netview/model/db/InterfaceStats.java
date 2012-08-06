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

package de.fhg.fokus.net.netview.model.db;

import java.io.Serializable;
import java.math.BigInteger;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import de.fhg.fokus.net.ptapi.PtInterfaceStats;

/**
 * Sampling
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */
@Entity
public class InterfaceStats implements Serializable {

	private static final long serialVersionUID = -4426394645556449457L;

	/**
	 * Unique Id. The uid id local o the program, i.e. it won't be transfered
	 * over the network and must therefore be set by the persistence layer in
	 * case one is available.
	 * 
	 */
	@Id
	public long uid;

	/**
	 * Observation Domain Id. Note that if you are using ipfix4java as transport you'll currently
	 * need to get this value from the ipfix message;
	 */
	@Column
	public long oid;

	/**
	 * Export Timestamp. 
	 */
	@Column
	public long timestamp;

	/**
	 * Sampling Size (n)
	 */
	@Column
	public long samplingSize;

	/**
	 * Packet Delta Count (N)
	 */
	@Column
	public long packetDeltaCount;

	/**
	 * number of packets received by pcap
	 */
	@Column
	public long pcapStatRecv;

	/**
	 * number of packets dropped by pcap
	 */
	@Column
	public long pcapStatDrop;
	
	/**
	 * A short name uniquely describing an interface, e.g. "eth0".
	 * May be the string "not available" on decoding failures.
	 */
	@Column
	public String interfaceName;

	/**
	 * A generic description of the interface, e.g. "Ethernet (10.0.0.1/24)"
	 * May be the string "not available" on decoding failures.
	 */
	@Column
	public String interfaceDescription;
	

	
	public InterfaceStats() {
	}
	

	public InterfaceStats(PtInterfaceStats sample) {
		super();
		this.oid = sample.oid;
		this.timestamp = sample.observationTimeMilliseconds;
		this.samplingSize = sample.samplingSize;
		this.packetDeltaCount = sample.packetDeltaCount.longValue();
		this.pcapStatRecv = sample.pcapStatRecv;
		this.pcapStatDrop = sample.pcapStatDrop;
		this.interfaceName= sample.interfaceName;
		this.interfaceDescription= sample.interfaceDescription;
	}

	public InterfaceStats(long oid, long timestamp, long samplingSize,
			BigInteger packetDeltaCount, long pcapStatRecv, long pcapStatDrop,
			String interfaceName, String interfaceDescription) {
		super();
		this.oid = oid;
		this.timestamp = timestamp;
		this.samplingSize = samplingSize;
		this.packetDeltaCount = packetDeltaCount.longValue();
		this.pcapStatRecv = pcapStatRecv;
		this.pcapStatDrop = pcapStatDrop;
		this.interfaceName = interfaceName;
		this.interfaceDescription = interfaceDescription;
	}


	public long getUid() {
		return uid;
	}


	public void setUid(long uid) {
		this.uid = uid;
	}


	public long getOid() {
		return oid;
	}


	public void setOid(long oid) {
		this.oid = oid;
	}


	public long getTimestamp() {
		return timestamp;
	}


	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getSamplingSize() {
		return samplingSize;
	}


	public void setSamplingSize(long samplingSize) {
		this.samplingSize = samplingSize;
	}


	public long getPacketDeltaCount() {
		return packetDeltaCount;
	}


	public void setPacketDeltaCount(long packetDeltaCount) {
		this.packetDeltaCount = packetDeltaCount;
	}


	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public long getPcapStatRecv() {
		return pcapStatRecv;
	}


	public void setPcapStatRecv(long pcapStatRecv) {
		this.pcapStatRecv = pcapStatRecv;
	}


	public long getPcapStatDrop() {
		return pcapStatDrop;
	}


	public void setPcapStatDrop(long pcapStatDrop) {
		this.pcapStatDrop = pcapStatDrop;
	}

	public String getInterfaceName() {
		return interfaceName;
	}


	public void setInterfaceName(String interfaceName) {
		this.interfaceName = interfaceName;
	}


	public String getInterfaceDescription() {
		return interfaceDescription;
	}


	public void setInterfaceDescription(String interfaceDescription) {
		this.interfaceDescription = interfaceDescription;
	}




//	@Override
//	public String toString() {
//		return String.format("{uid:%d, oid: %d, timestamp: %d, "
//				+ "samplingSize: %d, packetDeltaCount: %s}", uid, oid,
//				timestamp,
//				samplingSize,
//				packetDeltaCount.toString());
//	}

}
