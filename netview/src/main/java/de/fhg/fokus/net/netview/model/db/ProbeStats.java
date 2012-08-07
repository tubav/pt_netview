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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import de.fhg.fokus.net.ptapi.PtProbeStats;

/**
 * Probe statistics.
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */
@Entity
public class ProbeStats implements Serializable {

	private static final long serialVersionUID = 7100984799313833995L;
	/**
	 * Unique Id. The uid id local o the program, i.e. it won't be transfered
	 * over the network and must therefore be set by the persistence layer in
	 * case one is available.
	 * 
	 */
	
	@Id
	public long uid;

	/**
	 * Observation Domain Id.
	 */
	@Column
	public long oid;

	/**
	 * Export Timestamp.
	 * 
	 */
	@Column
	public long timestamp;

	/**
	 * System idle CPU, see "man mpstat" for more information.
	 */
	@Column
	public float systemCpuIdle;

	/**
	 * System free memory in kilobytes, see "man pidstat" for more information.
	 */
	@Column
	public long systemMemFree;

	/**
	 * percentage of CPU used in user level (application), see "man pidstat" for
	 * more information"
	 */
	@Column
	public float processCpuUser;

	/**
	 * percentage of CPU used in system level (kernel), see "man pidstat" for
	 * more information"
	 */
	@Column
	public float processCpuSys;

	/**
	 * the process virtual memory used in kilobytes, see "man pidstat" for more
	 * information"
	 */
	@Column
	public long processMemVzs;

	/**
	 * the process resident set size in kilobytes, see "man pidstat" for more
	 * information"
	 */
	@Column
	public long processMemRss;


	public ProbeStats() {
	}

	public ProbeStats(PtProbeStats probe) {
		super();
		this.oid = probe.oid;
		this.timestamp = probe.observationTimeMilliseconds;
		this.systemCpuIdle = probe.systemCpuIdle;
		this.systemMemFree = probe.systemMemFree.longValue();
		this.processCpuUser = probe.processCpuUser;
		this.processCpuSys = probe.processCpuSys;
		this.processMemVzs = probe.processMemVzs.longValue();
		this.processMemRss = probe.processMemRss.longValue();
	}

	public ProbeStats(long oid, long timestamp, float systemCpuIdle,
			long systemMemFree, float processCpuUser,
			float processCpuSys, long processMemVzs,
			long processMemRss, long pcapStatRecv,
			long pcapStatDrop) {
		super();
		this.oid = oid;
		this.timestamp = timestamp;
		this.systemCpuIdle = systemCpuIdle;
		this.systemMemFree = systemMemFree;
		this.processCpuUser = processCpuUser;
		this.processCpuSys = processCpuSys;
		this.processMemVzs = processMemVzs;
		this.processMemRss = processMemRss;
	}

//	@Override
//	public String toString() {
//		return String.format("{uid:%d,  oid: %d, timestamp: %d, "
//				+ "systemCpuIdle: %f, systemMemFree: %d, "
//				+ "processCpuUser: %f, processMemVzs: %d, "
//				+ "processMemRss: %d,  pcapStatRecv: %s, pcapStatDrop: %s}",
//				uid, oid, timestamp, systemCpuIdle, systemMemFree,
//				processCpuUser, processCpuSys, processMemVzs, processMemRss,
//				pcapStatRecv.toString(), pcapStatDrop.toString());
//	}

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

	public float getSystemCpuIdle() {
		return systemCpuIdle;
	}

	public void setSystemCpuIdle(float systemCpuIdle) {
		this.systemCpuIdle = systemCpuIdle;
	}

	public long getSystemMemFree() {
		return systemMemFree;
	}

	public void setSystemMemFree(long systemMemFree) {
		this.systemMemFree = systemMemFree;
	}

	public float getProcessCpuUser() {
		return processCpuUser;
	}

	public void setProcessCpuUser(float processCpuUser) {
		this.processCpuUser = processCpuUser;
	}

	public float getProcessCpuSys() {
		return processCpuSys;
	}

	public void setProcessCpuSys(float processCpuSys) {
		this.processCpuSys = processCpuSys;
	}

	public long getProcessMemVzs() {
		return processMemVzs;
	}

	public void setProcessMemVzs(long processMemVzs) {
		this.processMemVzs = processMemVzs;
	}

	public long getProcessMemRss() {
		return processMemRss;
	}

	public void setProcessMemRss(long processMemRss) {
		this.processMemRss = processMemRss;
	}
}
