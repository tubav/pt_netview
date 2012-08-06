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
package de.fhg.fokus.net.netview.control;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

import de.fhg.fokus.net.geo.WayPoint;
import de.fhg.fokus.net.netview.model.Model;
import de.fhg.fokus.net.netview.model.Model.ModelEventData;
import de.fhg.fokus.net.netview.model.Model.ModelEventType;
import de.fhg.fokus.net.netview.model.Node;
import de.fhg.fokus.net.netview.model.NodeMeasurementProperties;
import de.fhg.fokus.net.netview.model.NodePhysicalProperties;
import de.fhg.fokus.net.netview.model.NodeViewProperties;
import de.fhg.fokus.net.netview.sys.NetViewConfig;
import de.fhg.fokus.net.netview.view.NodeDialog;
import de.fhg.fokus.net.netview.view.ResourceView;
import de.fhg.fokus.net.netview.view.ViewMain;
import de.fhg.fokus.net.netview.view.map.MapPopupMenu;
import de.fhg.fokus.net.netview.view.map.MarkerPopupMenu;
import de.fhg.fokus.net.netview.view.map.NetViewMapMarker;
import de.fhg.fokus.net.netview.view.map.NodeStatsLayer;
import de.fhg.fokus.net.netview.view.map.TrackPopupMenu;
import de.fhg.fokus.net.netview.view.map.BearerPopupMenu;
import de.fhg.fokus.net.netview.view.map.FlowPopupMenu;
import de.fhg.fokus.net.worldmap.MapMarker;
import de.fhg.fokus.net.worldmap.WorldMap;
import de.fhg.fokus.net.worldmap.WorldMap.Events;
import de.fhg.fokus.net.worldmap.layers.map.Tile;
import de.fhg.fokus.net.worldmap.layers.track.SplineLayer;
import de.fhg.fokus.net.worldmap.util.EventSupport.EventListener;
import de.fhg.fokus.net.worldmap.layers.track.MapObject;
import de.fhg.fokus.net.worldmap.layers.track.Track;
import de.fhg.fokus.net.worldmap.layers.track.Bearer;
import de.fhg.fokus.net.worldmap.layers.track.Flow;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.fhg.fokus.net.netview.model.Probe;

public class MapController implements Controllable {
//	private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final WorldMap worldmap;
    private final ViewMain view;
    private final Model model;
    private final Set<Tile> tileSet = new ConcurrentSkipListSet<Tile>();
    private final NetViewConfig config;
    private final ConcurrentMap<Long, MapMarker> probeIdMarkerMap;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public MapController(ViewMain view, final Model model, NetViewConfig config) {
        super();
        this.view = view;
        this.model = model;
        this.probeIdMarkerMap = this.model.getProbeIdMarkerMap();
        this.worldmap = new WorldMap(view.getJPanelMap(), config.getCacheDir());
        this.worldmap.init();
        this.config = config;

        //
        // setup
        //
        //  map popup menu
        final MapPopupMenu mapPopupMenu = new MapPopupMenu(
                ResourceView.getViewStyle(MapPopupMenu.class), worldmap);
        worldmap.getLayeredPane().addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e.getComponent(), e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e.getComponent(), e.getX(), e.getY());
                }
            }

            private void showPopup(Component c, int x, int y) {
                SplineLayer tl = model.getSplineLayer();
                if (tl != null) {
                    MapObject selectedObject = model.getSplineLayer().getSelectedSpline();
                    if (selectedObject != null) {
                        if(selectedObject.type == MapObject.Type.TRACK) {
                            Track t = model.getSplineLayer().findTrack(selectedObject.id);
                            TrackPopupMenu tpm = new TrackPopupMenu(t);
                            tpm.show(c, x, y);
                            return;
                        }
                        else if(selectedObject.type == MapObject.Type.BEARER) {
                            Bearer b = model.getSplineLayer().findBearer(selectedObject.id);
                            BearerPopupMenu bpm = new BearerPopupMenu(b);
                            bpm.show(c, x, y);
                            return;
                        }
                        else if(selectedObject.type == MapObject.Type.FLOW) {
                            Flow f = model.getSplineLayer().findFlow(selectedObject.id);
                            FlowPopupMenu fpm = new FlowPopupMenu(f);
                            fpm.show(c, x, y);
                            return;
                        }  
                    }
                }

                mapPopupMenu.show(c, x, y);
            }
        });

        setupBusyIcon();
        setupMarkers();
        setupLayers();
    }

    private void setupLayers() {
        SplineLayer tl = model.getSplineLayer();
        NodeStatsLayer nl = model.getNodeStatsLayer();
        nl.setWorldmap(worldmap);

        nl.injectWaypointScreenLocator(worldmap.getMapLayer());

        worldmap.addLayer(tl);
        worldmap.addLayer(nl);
    }

    private void setupMarkers() {
        final NodeDialog nodeDialog = new NodeDialog(view.getFrame(), false);
        final MarkerPopupMenu popupmenu = new MarkerPopupMenu(
                ResourceView.getViewStyle(MarkerPopupMenu.class),
                nodeDialog);
        //
        // Node added
        //
        model.addNodeEventListener(ModelEventType.NODE_ADDED, new EventListener<ModelEventData>() {

            @Override
            public void onEvent(ModelEventData e) {
                final Node node = e.node;

                Probe probe = NodeMeasurementProperties.getProbe(node);
                long probeId = probe.getProbeId();

                // Test if Marker already exists
                List<MapMarker> currentMarkers = worldmap.getMapMarkers();
                if (currentMarkers.size() > 0) {
                    for (MapMarker tempMarker : currentMarkers) {
                        if (tempMarker.oid == probeId) {
                            return;
                        }
                    }
                }

                MapMarker marker = NodeViewProperties.getMapMarker(node);

                WayPoint waypoint = NodePhysicalProperties.getWaypoint(node);
                if (waypoint == null) {
                    return;
                }
                if (marker == null) {
                    node.view.marker = new NetViewMapMarker(node, config);
                    node.view.marker.addMouseListener(new MouseAdapter() {

                        @Override
                        public void mousePressed(MouseEvent e) {
                            if (e.isPopupTrigger()) {
                                popupmenu.setMarker(node.view.marker);
                                popupmenu.show(e.getComponent(), e.getX(), e.getY());
                            }
                        }

                        @Override
                        public void mouseReleased(MouseEvent e) {
                            if (e.isPopupTrigger()) {
                                popupmenu.setMarker(node.view.marker);
                                popupmenu.show(e.getComponent(), e.getX(), e.getY());
                            }
                        }
                });
					
				} else {
					node.view.marker.waypoint = node.phy.waypoint;
                }
//				marker.setLabel(NodeViewProperties.getLabel(node));
                node.view.marker.setWaypoint(NodePhysicalProperties.getWaypoint(node));
                probeIdMarkerMap.putIfAbsent(node.mp.getProbe().getProbeId(), node.view.marker);
                node.view.marker.setLabel(NodeViewProperties.getLabel(node));
                node.view.marker.oid = probeId;

                worldmap.addMapMarker(node.view.marker);
                model.getNodeStatsLayer().addNode(node);
            }
        });
        //
        // Node removed
        //
        model.addNodeEventListener(ModelEventType.NODE_REMOVED,
                new EventListener<ModelEventData>() {

            @Override
            public void onEvent(ModelEventData e) {
//				logger.debug("Removing marker for node: {}",NodeViewProperties.getLabel(node));
                // FIXME objects from ORM are not the same
                // QUICKFIX: removing all markers
                worldmap.resetMarkers();
                probeIdMarkerMap.clear();
                //worldmap.removeMapMarker(marker);
                // FIXME should remove only the respective node
                // to make purge work.
                model.getNodeStatsLayer().removeAllNodes();
            }
        });
    }

    private void setupBusyIcon() {
        this.worldmap.addEventListener(Events.TILE_LOAD_STARTED,
                new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (tileSet.isEmpty()) {
                    MapController.this.view.getBusyIconAnimator().start();
                }
                Tile tile = (Tile) evt.getNewValue();
                tileSet.add(tile);
            }
        });
        this.worldmap.addEventListener(Events.TILE_LOAD_FINISHED,
                new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Tile tile = (Tile) evt.getNewValue();
                tileSet.remove(tile);
                if (tileSet.isEmpty()) {
                    MapController.this.view.getBusyIconAnimator().stop();
                }
            }
        });
    }

    @Override
    public void init() {
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        tileSet.clear();
        worldmap.stop();

    }

    public void showConfigDialog() {
        worldmap.showConfigDialog();
    }

    public WorldMap getWorldmap() {
        return worldmap;
    }
}
