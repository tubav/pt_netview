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

package de.fhg.fokus.net.worldmap.model;

import java.beans.PropertyChangeListener;

import javax.swing.JLayeredPane;

import de.fhg.fokus.net.worldmap.layers.FumeLayer;
import de.fhg.fokus.net.worldmap.layers.Layer;
import de.fhg.fokus.net.worldmap.layers.ToolsLayer;
import de.fhg.fokus.net.worldmap.layers.map.MapLayer;
import de.fhg.fokus.net.worldmap.layers.markers.MarkersLayer;

public interface LayerModel extends Iterable<Layer>, PersistentPreferences {
	public enum Events {
		/**
		 * <p>A new layer has been added.</p>
		 * <pre>
		 * oldvalue = null
		 * newvalue = new layer ({@link Layer})
		 * </pre>
		 */
		LAYER_ADDED,
		/**
		 * <p>A new layer has been added.</p>
		 * <pre>
		 * oldvalue = null
		 * newvalue = removed layer
		 * </pre>
		 */
		LAYER_REMOVED
	}
	/**
	 * Get a layer instance from the respective class
	 * 
	 * @param <L>
	 * @param layerClass
	 * @return
	 */
	public <L extends Layer>  L getLayer(String layerId, Class<L> layerClass );
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);
	public void removePropertyChangeListener(PropertyChangeListener listener);
	public MapLayer getMapLayer();
	public FumeLayer getFumeLayer();
	public ToolsLayer getToolsLayer();
	public MarkersLayer getMarkersLayer();
	public int size();
	/**
	 * <pre>
	 * 3 - tools
	 * 2 - markers
	 * 1 - fume
	 * 0 - map
	 * </pre>
	 * 
	 * @param order
	 * @return
	 */
	public Layer getByOrder(int order);
	public LayerModel addLayer(Layer layer, int level );
	public JLayeredPane getLayeredPane() ;

}
