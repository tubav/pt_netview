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
import java.io.Serializable;

import de.fhg.fokus.net.ptapi.PtProbeLocation;
import java.net.Inet4Address;

/**
 * Location
 *
 * @author FhG-FOKUS NETwork Research
 *
 */
@Entity
public class ProbeLocation implements Serializable {

        private static final long serialVersionUID = -4426594681151449857L;

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
	 * The latitude received from the probe
	 */
        @Column
	public String latitude;

	/**
	 * The longitude received from the probe
	 */
        @Column
	public String longitude;

        /**
	 * The name of the probe
	 */
        @Column
	public String name;

        /**
	 * The name of the probe
	 */
        @Column
	public String locationName;

        /**
	 * The name of the probe
	 */
        @Column
	public String address;


        public ProbeLocation(){
        }

        public ProbeLocation(PtProbeLocation location) {
		super();
		this.oid = location.oid;
		this.timestamp = location.observationTimeMilliseconds;
		this.latitude = location.latitude;
		this.longitude = location.longitude;
                this.name = location.probeName;
                this.locationName = location.probeLocationName;
                this.address = location.sourceIpv4Address.toString().substring(1);
	}

	public ProbeLocation(long oid, long timestamp,
			String latitude, String longitude, String name,
                        String locationName, String address) {
		super();
		this.oid = oid;
		this.timestamp = timestamp;
		this.latitude = latitude;
		this.longitude = longitude;
                this.name = name;
                this.locationName = locationName;
                this.address = address;
	}
        
        public String toString(){
                return "oid:"+this.oid+" | tstmp:"+this.timestamp
                        +" | lat:"+this.latitude+" | lon:"+this.longitude
                        +" | name:"+this.name+" | locName:"+locationName
                        +" | address:"+this.address;
        }

        public static long getSerialversionuid() {
		return serialVersionUID;
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

	public String getLatitude() {
		return latitude;
	}

	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}

	public String getLongitude() {
		return longitude;
	}

	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}

        public String getName() {
                return name;
        }

        public void setName(String name) {
                this.name = name;
        }

        public String getLocationName() {
                return locationName;
        }

        public void setLocationName(String locationName) {
                this.locationName = locationName;
        }

        public String getAddress() {
                return address;
        }

        public void setAddress(String address) {
                this.address = address;
        }

}