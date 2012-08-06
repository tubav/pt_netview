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

package de.fhg.fokus.net.worldmap.layers.markers;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import de.fhg.fokus.net.worldmap.MapMarker;

/**
 * Provides storage and querying of markers 
 *
 * TODO: improve implementation
 */
public class MarkerSetModel implements Iterable<MapMarker> {
	private List<MapMarker> markerList = new CopyOnWriteArrayList<MapMarker>();

	public MarkerSetModel add( MapMarker marker ){
		markerList.add(marker);
		return this;
	}
	public MarkerSetModel remove( MapMarker marker ){
		markerList.remove(marker);
		return this;
	}
	@Override
	public Iterator<MapMarker> iterator() {
		return markerList.iterator();
	}
	public MarkerSetModel setSelected(MapMarker marker, boolean select ) {
		if(marker!=null){
			marker.setSelected(select);
		}
		return this;
	}

	public MarkerSetModel deselectAll(){
		for(MapMarker m:this){
			m.setSelected(false);
		}
		return this;
	}
	
	public List<MapMarker> getAll() {
		return markerList;
	}
	/**
	 * Reset model - remove all markers
	 */
	public void reset(){
		markerList.clear();
	}

}
