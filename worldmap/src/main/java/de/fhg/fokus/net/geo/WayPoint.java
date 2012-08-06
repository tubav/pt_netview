/**
 * worldmap - an extension to JMapViewer which provides additional
 *            functionality. New functions allow setting markers,
 *            adding layers, and printing tracks on the map. (see
 *            http://wiki.openstreetmap.org/wiki/JMapViewer for more
 *            information on JMapViewer)
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

package de.fhg.fokus.net.geo;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Identify a point in physical space using latitude and longitude
 * (both in degrees in the WGS84)
 * TODO: make this waypoint generic to support other reference systems.
 * E.g. when we are in a room and it does not matter where we are on the earth.
 * 
 * @author FhG-FOKUS NETwork Research
 */
@Entity
public class WayPoint {
	@Id
	private long uid;
	public static enum Format {
		WGS89_DECIMAL,
		WAYPOINT,
		ADD_WAYPOINT
	}
	public static final double MAX_LONGITUDE = 180;
	public static final double MIN_LONGITUDE= -180;
	public static final double MAX_LATITUDE = 85.05112877980659;
	public static final double MIN_LATITUDE = -85.05112877980659;


	/**
	 * longitude in degrees
	 */
	@Column
	public double longitude;
	/**
	 * latitude in degrees -180 t0 180
	 */
	@Column
	public double latitude;
	/**
	 * 
	 */
	@Column
	public double altitude;
	
	@Column
	private String label;
	
	public WayPoint() {
	}
	public WayPoint(double latitude, double longitude,  double altitude) {
		this.longitude = Math.max(MIN_LONGITUDE, Math.min(longitude, MAX_LONGITUDE));
		this.latitude = Math.max(MIN_LATITUDE, Math.min(latitude, MAX_LATITUDE));
		this.altitude = altitude;
	}
	public WayPoint set( double latitude, double longitude ){
		this.latitude = latitude;
		this.longitude = longitude;
		return this;
	}
	/**
	 * Create a waypoint 
	 * 
	 * @param latitude WGS89 latitude
	 * @param longitude WGS89 longitude
	 */
	public WayPoint( double latitude , double longitude) {
		this(latitude,longitude,0);
	}
	public double getLatitude() {
		return latitude;
	}
	public double getLongitude() {
		return longitude;
	}
	public double getAltitude() {
		return altitude;
	}
	public String getString( Format format ){
		String str = "unsupported format";
		switch (format) {
		case WGS89_DECIMAL:
			str = String.format("%.6f  %.6f", latitude, longitude);
			break;
		case WAYPOINT:
			str = String.format("WayPoint(%s, %s)", latitude+"", longitude+"");
			break;
		case ADD_WAYPOINT:
			str = String.format(".addWaypoint(%s, %s)", latitude+"", longitude+"");
			break;
			

		default:
			break;
		}
		return str;
	}
	public void set(WayPoint waypoint) {
		if( waypoint!=null){
		   longitude = waypoint.longitude;
		   latitude = waypoint.latitude;
		   altitude = waypoint.altitude;
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
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}
	
	
}
