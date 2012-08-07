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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

import de.fhg.fokus.net.ptapi.PacketTrackRecord;

@Entity
public class TrackData {

	public TrackData() {

	}

	public TrackData(int uID, int trackID, long start_ts, long stop_ts, int delay) {
		this.uid = uID;
		this.trackID = trackID;
		this.start_ts = start_ts;
		this.stop_ts = stop_ts;
		this.delay = delay;
	}
	
	
	public TrackData(PacketTrackRecord record){
		this.trackID = record.trackid;
		this.start_ts = record.ts[0];
		this.stop_ts = record.ts[record.ts.length-1];
		this.delay = record.ts[record.ts.length-1] - record.ts[0];
	}
	
	
	/**
	 * Unique Id
	 */
	@Id
	private int uid;

	@Column
	private int trackID;

	@Column
	private long start_ts;

	@Column
	private long stop_ts;

	@Column
	private long delay;

	
	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public int getTrackID() {
		return trackID;
	}

	public void setTrackID(int trackID) {
		this.trackID = trackID;
	}

	public long getStart_ts() {
		return start_ts;
	}

	public void setStart_ts(long start_ts) {
		this.start_ts = start_ts;
	}

	public long getStop_ts() {
		return stop_ts;
	}

	public void setStop_ts(long stop_ts) {
		this.stop_ts = stop_ts;
	}

	public long getDelay() {
		return delay;
	}

	public void setDelay(long delay) {
		this.delay = delay;
	}
	


	
}
