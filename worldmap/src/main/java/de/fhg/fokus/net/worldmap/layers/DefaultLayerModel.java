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

package de.fhg.fokus.net.worldmap.layers;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.JLayeredPane;
import javax.swing.OverlayLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.worldmap.control.AnimationSupport;
import de.fhg.fokus.net.worldmap.control.MapController;
import de.fhg.fokus.net.worldmap.control.SelectController;
import de.fhg.fokus.net.worldmap.layers.map.DefaultTileLoader;
import de.fhg.fokus.net.worldmap.layers.map.MapLayer;
import de.fhg.fokus.net.worldmap.layers.map.OsmTileSource;
import de.fhg.fokus.net.worldmap.layers.map.TileLoader;
import de.fhg.fokus.net.worldmap.layers.markers.MarkersLayer;
import de.fhg.fokus.net.worldmap.layers.track.SplineLayer;
import de.fhg.fokus.net.worldmap.model.LayerModel;
import de.fhg.fokus.net.worldmap.model.PersistentPreferences;

public class DefaultLayerModel implements LayerModel {
	private final JLayeredPane layeredPane;
	private final AnimationSupport animationSupport ;
	
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	// layers
	private final Map<String, Layer> layersMap = new ConcurrentHashMap<String, Layer>();
	private final List<Layer> layersList = new Vector<Layer>();

	private final MapLayer mapLayer;
	private final MarkersLayer markersLayer;
	private final ToolsLayer toolsLayer;
	private final FumeLayer fumeLayer;
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private final MapController mapController;

	public DefaultLayerModel( JLayeredPane layeredpane, ScheduledExecutorService scheduler, ExecutorService downloadExecutor,
			AnimationSupport animationSupport, String cacheDir ) {
		this.animationSupport=animationSupport;
		this.layeredPane = layeredpane;
		// -----------------------------------
		//  LAYERS (from the bottom to top)
		// -----------------------------------
		layeredPane.setLayout(new OverlayLayout(layeredPane));

		// 1. Map layer
		TileLoader tileLoader =new DefaultTileLoader(scheduler,downloadExecutor,cacheDir);
		this.mapLayer= new MapLayer(tileLoader,new OsmTileSource.Mapnik());
		addLayer(this.mapLayer,JLayeredPane.DEFAULT_LAYER);
		this.mapLayer.loadPreferences();
		// 1.1 map controller
		this.mapController = new MapController(this,layeredPane,animationSupport).init();


		// 2. Fume Layer 
		this.fumeLayer = new FumeLayer();
		addLayer(this.fumeLayer, 3 );
		this.fumeLayer.setVisible(false);
		// 3. Markers' Layer
		this.markersLayer = new MarkersLayer(this.mapLayer, this.mapController.getController(SelectController.class), 
				animationSupport);
		addLayer(this.markersLayer,100);
		// 4. Tools' layer
		this.toolsLayer = new ToolsLayer(this, this.mapController,this.animationSupport);
		addLayer(this.toolsLayer, 110);
		this.toolsLayer.loadPreferences();


		
		
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public <L extends Layer> L getLayer(String layerId, Class<L> layerClass) {
		return (L)layersMap.get(layerId);
	}
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(propertyName,listener);
	}
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}
	@Override
	public FumeLayer getFumeLayer() {
		return fumeLayer;
	}
	@Override
	public MapLayer getMapLayer() {
		return mapLayer;
	}
	@Override
	public ToolsLayer getToolsLayer() {
		return toolsLayer;
	}
	@Override
	public MarkersLayer getMarkersLayer() {
		return markersLayer;
	}
	@Override
	public Iterator<Layer> iterator() {
		return layersMap.values().iterator();
	}
	@Override
	public int size() {
		return layersList.size();
	}
	@Override
	public Layer getByOrder(int order) {
		try {
			return layersList.get(order);
		} catch (Exception e) {
			logger.debug(e.getMessage());
			return null;
		}
	}
	@Override
	public DefaultLayerModel addLayer(Layer layer, int level ){
		if (layer instanceof SplineLayer ) {
			SplineLayer trackLayer = (SplineLayer) layer;
			trackLayer.injectAnimationSupport(animationSupport);
			trackLayer.injectWaypointScreenLocator(mapLayer);
			
		}
		
		layeredPane.add(layer.setLevel(level), (Integer) (level));
		layersMap.put(layer.getLayerId(), layer);
		layersList.add(layer);
		Collections.sort(layersList);
		pcs.firePropertyChange(LayerModel.Events.LAYER_ADDED+"", null, layer);
		return this;
	}


	@Override
	public void loadPreferences() {
		for(Layer layer:this){
			if( layer instanceof PersistentPreferences ){
				((PersistentPreferences)layer).loadPreferences();
			}
		}
		
	}


	@Override
	public void savePreferences() {
		for(Layer layer:this){
			if( layer instanceof PersistentPreferences ){
				((PersistentPreferences)layer).savePreferences();
			}
		}
	}

	@Override
	public JLayeredPane getLayeredPane() {
		return layeredPane;
	}
	

}
