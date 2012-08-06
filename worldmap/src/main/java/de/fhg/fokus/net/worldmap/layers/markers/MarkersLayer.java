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

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.worldmap.MapMarker;
import de.fhg.fokus.net.worldmap.control.AnimationSupport;
import de.fhg.fokus.net.worldmap.control.SelectController;
import de.fhg.fokus.net.worldmap.model.PredefinedLayer;
import de.fhg.fokus.net.worldmap.model.WayPointScreenLocator;

public class MarkersLayer extends PredefinedLayer {

    private final Preferences prefs = Preferences.userNodeForPackage(this.getClass());

    private enum PrefKeys {

        visible
    }
    private static final long serialVersionUID = 762951963415085350L;
    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private final WayPointScreenLocator wayPointLocator;
    private final SelectController selectController;
    private final MarkerSetModel model = new MarkerSetModel();
    private final AnimationSupport animationSupport;

    public MarkersLayer(WayPointScreenLocator wayPointLocator, SelectController selectController, AnimationSupport animationSupport) {
        super("Markers");
        setLayout(null);
        setOpaque(false);
        this.wayPointLocator = wayPointLocator;
        this.selectController = selectController;
        this.animationSupport = animationSupport;
        //setupSelectionListeners();
    }

    private void setupSelectionListeners() {
        this.selectController.addPropertyChangeListener(SelectController.Events.SELECT_ONE + "", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                MapMarker marker = (MapMarker) evt.getNewValue();
                MouseEvent mouseEvt = (MouseEvent) evt.getOldValue();
                if (!mouseEvt.isControlDown()) {
                    model.setSelected(marker, true);
                    animationSupport.startStrokeSelection();
                }
            }
        });
        this.selectController.addPropertyChangeListener(SelectController.Events.DESELECT_ALL + "", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                model.deselectAll();
                animationSupport.stopStrokeSelection();
            }
        });
        this.selectController.addPropertyChangeListener(SelectController.Events.SELECT_AREA + "", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Rectangle rect = (Rectangle) evt.getNewValue();
                int i = 0;
                for (MapMarker marker : model) {
                    if (marker.isVisible() && rect.contains(marker.getLocation())) {
                        marker.setSelected(true);
                        i++;
                    }
                }
                if (i > 0) {
                    animationSupport.startStrokeSelection();
                }
            }
        });
    }

    /**
     * Do not paint children
     */
    @Override
    public void paint(Graphics g) {
        paintBorder(g);
        paintComponent(g);
    }
    private final Point refPoint = new Point();

    @Override
    protected void paintComponent(Graphics g) {
        for (MapMarker marker : model) {
            wayPointLocator.setXYFromWaypoint(refPoint, marker.getWaypoint());
            marker.paint(g, refPoint);
        }
    }

    private MapMarker setupMarker(MapMarker marker) {
        selectController.addSelectControls(marker);


        return marker;
    }

    private MapMarker resetMarker(MapMarker marker) {
        selectController.removeSelectControls(marker);
        return marker;
    }

    public void addAllMapMarker(List<MapMarker> markers) {
        if (markers != null) {
            for (MapMarker m : markers) {
                add(setupMarker(m));
            }
        }
    }

    public void addMapMarker(MapMarker marker) {
//		logger.debug("adding marker: "+marker);
        model.add(marker);
        add(setupMarker(marker));
        repaint();
    }

    public void removeMapMarker(MapMarker marker) {
        model.remove(marker);
        remove(resetMarker(marker));
        // TODO do we need to call validate/revalidate here? Better if not.
    }

    public List<MapMarker> getMapMarkers() {
        return model.getAll();
    }

    @Override
    public void loadPreferences() {
        setVisible(prefs.getBoolean(PrefKeys.visible + "", false));

    }

    @Override
    public void savePreferences() {
        prefs.putBoolean(PrefKeys.visible + "", isVisible());

    }

    /**
     * Reset model, i.e. remove all markers.
     * 
     */
    public void resetModel() {
        logger.debug("resetModel()");
        removeAll();
        model.reset();
    }

    public void showHiddenMarkers() {
        for (MapMarker marker : model) {
            marker.setVisible(true);
        }

    }
}
