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
package de.fhg.fokus.net.worldmap;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.MutableComboBoxModel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.geo.WayPoint;
import de.fhg.fokus.net.geo.WayPoint.Format;
import de.fhg.fokus.net.worldmap.control.AnimationSupport;
import de.fhg.fokus.net.worldmap.layers.DefaultLayerModel;
import de.fhg.fokus.net.worldmap.layers.Layer;
import de.fhg.fokus.net.worldmap.layers.ToolsLayer;
import de.fhg.fokus.net.worldmap.layers.map.MapLayer;
import de.fhg.fokus.net.worldmap.layers.map.OsmTileSource;
import de.fhg.fokus.net.worldmap.layers.map.Tile;
import de.fhg.fokus.net.worldmap.layers.map.TileSource;
import de.fhg.fokus.net.worldmap.model.Animated;
import de.fhg.fokus.net.worldmap.model.Controllable;
import de.fhg.fokus.net.worldmap.model.LayerModel;
import de.fhg.fokus.net.worldmap.view.ConfigDialog;

/**
 * A layered world map viewer  (using openstreetmap tile servers) 
 *
 */
public class WorldMap implements Controllable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final JPanel panel;
    private final JFrame frame;
    private final LayerModel layerModel;
    private final ConfigDialog configDialog;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
    private final ExecutorService downloadExecutor = Executors.newFixedThreadPool(4);
    private final AnimationSupport animationSupport;
    private final JLayeredPane layeredpane = new JLayeredPane();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public static enum Events {

        /**
         * <p>started to load tile </p>
         * <pre>
         * oldvalue = null
         * newvalue = tile ({@link Tile})
         * </pre>
         */
        TILE_LOAD_STARTED,
        /**
         * <p>finished to load tile </p>
         * <pre>
         * oldvalue = null
         * newvalue = tile ({@link Tile})
         * </pre>
         */
        TILE_LOAD_FINISHED
    }

    public WorldMap(JPanel panel, String cacheDir) {
        super();
        // Saving references
        this.frame = (JFrame) SwingUtilities.getRoot(panel);
        this.panel = panel;
        // Animation Support
        this.animationSupport = new AnimationSupport(scheduler);

        // Adding layered pane to panel
        this.panel.setLayout(new BorderLayout());
        this.panel.add(layeredpane, BorderLayout.CENTER);
        this.layerModel = new DefaultLayerModel(layeredpane, scheduler, downloadExecutor, animationSupport, cacheDir);
        this.layerModel.loadPreferences();
        //------------------------
        // Configuration & Tools
        //------------------------
        // Configuration dialog
        this.configDialog = new ConfigDialog(this.frame, false);
        setupConfigDialog();
    }

    /**
     * Setup configuration dialog. It must be called after layers' initialization
     */
    private void setupConfigDialog() {
        final MapLayer mapLayer = layerModel.getMapLayer();
        if (configDialog.getX() == 0 && configDialog.getY() == 0) {
            configDialog.setLocationRelativeTo(frame);
        }
        //
        // setup available tile sources
        //
        MutableComboBoxModel cbmodel = new DefaultComboBoxModel(
                new TileSource[]{new OsmTileSource.Mapnik(),
                    new OsmTileSource.TilesAtHome(),
                    new OsmTileSource.CycleMap(),
                    new OsmTileSource.OpenStreetBrowser()});
        JComboBox tileSourceSelector = configDialog.getJComboBoxTileSource();
        tileSourceSelector.setModel(cbmodel);
        tileSourceSelector.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                mapLayer.setTileSource((TileSource) e.getItem());
            }
        });
        // 
        // show/hide grid
        //
        final JCheckBox showTileGrid = configDialog.getJCheckBoxShowTileGrid();
        showTileGrid.setSelected(layerModel.getMapLayer().isTileGridVisible());
        showTileGrid.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                mapLayer.setTileGridVisible(showTileGrid.isSelected());
            }
        });
        //
        // limit downloading tile using only one connection
        //
        final JCheckBox useOneHttpConnection = configDialog.getJCheckBoxOneHttpConnection();
        boolean defaltUseHttpConnection = mapLayer.getTileLoader().isUseOneHttpConnection();
        mapLayer.getTileLoader().setUseOneHttpConnection(defaltUseHttpConnection);
        useOneHttpConnection.setSelected(mapLayer.getTileLoader().isUseOneHttpConnection());
        useOneHttpConnection.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                mapLayer.getTileLoader().setUseOneHttpConnection(useOneHttpConnection.isSelected());
            }
        });

        //
        // show/hide palette (tools)
        //
        final JCheckBox showPalette = configDialog.getjCheckBoxShowPalette();
        showPalette.setSelected(layerModel.getToolsLayer().isPaletteVisible());
        showPalette.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                layerModel.getToolsLayer().setPaletteVisible(showPalette.isSelected());
            }
        });
    }

    @Override
    public Controllable init() {

        // log clicked location
        layeredpane.addMouseListener(new MouseAdapter() {

            WayPoint wp = new WayPoint(1, 1);
            Point pt = new Point();

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    pt.x = e.getX();
                    pt.y = e.getY();
                    layerModel.getMapLayer().setWaypointFromXY(wp, pt);
                    //				logger.debug(wp.getString(Format.WAYPOINT));
                    System.out.println(wp.getString(Format.ADD_WAYPOINT));
                }
            }
        });

        logger.debug("WorldMap: init()");
        animationSupport.init();
        start();
        return this;
    }

    @Override
    public void start() {
        //		logger.debug("WorldMap: start()");
    }

    @Override
    public void stop() {
        layerModel.savePreferences();

    }

    /**
     * Remove map marker
     * 
     * @param marker
     */
    public void removeMapMarker(MapMarker marker) {
        layerModel.getMarkersLayer().removeMapMarker(marker);
    }

    /**
     * Add a map marker
     * @param marker
     * @return
     */
    public WorldMap addMapMarker(MapMarker marker) {
        layerModel.getMarkersLayer().addMapMarker(marker);
        if (marker instanceof Animated) {
            Animated aMarker = (Animated) marker;
            aMarker.injectAnimationSupport(animationSupport);
        }
        return this;
    }

    public List<MapMarker> getMapMarkers() {
        return layerModel.getMarkersLayer().getMapMarkers();
    }

    /**
     * Shows configuration dialog.
     */
    public void showConfigDialog() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                configDialog.setVisible(true);
            }
        });
    }

    /**
     * Shows layer selection/configuration dialog.
     */
    public void showLayersDialog() {
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                layerModel.getToolsLayer().getLayersDialog().setVisible(true);
            }
        });
    }

    public void addEventListener(Events eventName, PropertyChangeListener listener) {
        // delegate listeners 
        switch (eventName) {
            case TILE_LOAD_STARTED:
                layerModel.getMapLayer().getTileLoader().addPropertyChangeListener(eventName + "", listener);
                break;
            case TILE_LOAD_FINISHED:
                layerModel.getMapLayer().getTileLoader().addPropertyChangeListener(eventName + "", listener);
                break;
            default:
                pcs.addPropertyChangeListener(eventName + "", listener);
                break;
        }

    }

    public void removeEventListener(PropertyChangeListener listener) {
        layerModel.getMapLayer().getTileLoader().removePropertyChangeListener(listener);
        pcs.removePropertyChangeListener(listener);
    }

    public JLayeredPane getLayeredPane() {
        return layeredpane;
    }

    public void showHiddenMarkers() {
        layerModel.getMarkersLayer().showHiddenMarkers();
    }

    public void addLayer(Layer layer) {
        layerModel.addLayer(layer, layer.getLevel());

    }

    public ToolsLayer getToolsLayer() {
        return layerModel.getToolsLayer();
    }

    public MapLayer getMapLayer() {
        return layerModel.getMapLayer();
    }

    public void resetMarkers() {
        layerModel.getMarkersLayer().resetModel();

    }
}
