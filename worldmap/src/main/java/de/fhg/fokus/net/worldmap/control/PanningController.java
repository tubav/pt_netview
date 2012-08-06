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
//License: GPL. Copyright 2008 by Jan Peter Stotz
package de.fhg.fokus.net.worldmap.control;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.worldmap.layers.map.MapLayer;
import de.fhg.fokus.net.worldmap.model.Controllable;

/**
 * 
 * @author Jan Peter Stotz (original author)
 * @author FhG-FOKUS NETwork Research
 * 
 */
public class PanningController implements Controllable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final MapLayer mapLayer;
    private final JComponent eventSrc;
    private final MouseControls mouseControls;
    private final JFrame frame;

    public PanningController(MapLayer mapLayer, JComponent eventSrc) {
        this.mapLayer = mapLayer;
        this.eventSrc = eventSrc;

        this.frame = (JFrame) SwingUtilities.getRoot(this.eventSrc);

        this.mouseControls = new MouseControls();


    }
    private static final int MOUSE_BUTTONS_MASK = MouseEvent.BUTTON3_DOWN_MASK
            | MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK;
    private Point lastDragPoint;
    private boolean isMoving = false;
    private boolean movementEnabled = true;
    private int movementMouseButton = MouseEvent.BUTTON1;
    private int movementMouseButtonMask = MouseEvent.BUTTON1_DOWN_MASK;
    private boolean wheelZoomEnabled = true;
    private boolean doubleClickZoomEnabled = true;

    @Override
    public Controllable init() {
        // nothing to do here
        return this;
    }

    @Override
    public void start() {
        eventSrc.addMouseListener(mouseControls);
        eventSrc.addMouseWheelListener(mouseControls);
        eventSrc.addMouseMotionListener(mouseControls);

    }

    @Override
    public void stop() {
        eventSrc.removeMouseListener(mouseControls);
        eventSrc.removeMouseWheelListener(mouseControls);
        eventSrc.removeMouseMotionListener(mouseControls);
    }

    /**
     * Mouse listener
     */
    private class MouseControls extends MouseAdapter {

        public void mouseDragged(MouseEvent e) {
            if (!movementEnabled || !isMoving) {
                return;
            }
            // Is only the selected mouse button pressed?
            if ((e.getModifiersEx() & MOUSE_BUTTONS_MASK) == movementMouseButtonMask) {
                Point p = e.getPoint();
                if (lastDragPoint != null) {
                    int diffx = lastDragPoint.x - p.x;
                    int diffy = lastDragPoint.y - p.y;
                    mapLayer.moveMap(diffx, diffy);
                }
                lastDragPoint = p;
            }
        }

        public void mouseClicked(MouseEvent e) {

            if (doubleClickZoomEnabled && e.getClickCount() == 2
                    && e.getButton() == MouseEvent.BUTTON1) {
                mapLayer.zoomIn(e.getPoint());
            }
        }

        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                frame.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                if (e.getButton() == movementMouseButton) {
                    lastDragPoint = null;
                    isMoving = true;
                }
            }
        }

        public void mouseReleased(MouseEvent e) {
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            if (e.getButton() == movementMouseButton) {
                lastDragPoint = null;
                isMoving = false;
            }
        }

        public void mouseMoved(MouseEvent e) {
            if (doubleClickZoomEnabled && e.getClickCount() == 2
                    && e.getButton() == MouseEvent.BUTTON1) {
                mapLayer.zoomIn(e.getPoint());
            }

        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (wheelZoomEnabled) {
                mapLayer.setZoom(mapLayer.getZoom() - e.getWheelRotation(), e.getPoint());
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        }

        @Override
        public void mouseExited(MouseEvent e) {
        }
    }
}
