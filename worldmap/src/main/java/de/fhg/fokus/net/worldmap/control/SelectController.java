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
package de.fhg.fokus.net.worldmap.control;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import de.fhg.fokus.net.worldmap.model.Controllable;

public class SelectController implements Controllable {
    // private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final JComponent eventSrc;
    private final MouseControls mouseControls;
    private final AnimationSupport animationSupport;
    private final JFrame frame;
    private boolean selecting = false;
    private final Point start = new Point();
    private final Point end = new Point();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    public static enum Events {

        /**
         * <p>
         * Select one component.
         * </p>
         * 
         * <pre>
         * oldvalue = mouse event {@link MouseEvent}
         * newvalue = selected {@link JComponent}
         * </pre>
         */
        SELECT_ONE,
        /**
         * <p>
         * Deselect one component.
         * </p>
         * 
         * <pre>
         * oldvalue = null
         * newvalue = selected {@link JComponent}
         * </pre>
         */
        DESELECT_ONE,
        /**
         * <p>
         * Toggle component selection.
         * </p>
         * 
         * <pre>
         * oldvalue = null
         * newvalue = selected {@link JComponent}
         * </pre>
         */
        TOGGLE_ONE,
        /**
         * <p>
         * All components deselected.
         * </p>
         * 
         * <pre>
         * oldvalue = null
         * newvalue = null
         * </pre>
         */
        DESELECT_ALL,
        /**
         * <p>
         * Area on screen selected.
         * </p>
         * 
         * <pre>
         * oldvalue = null
         * newvalue = a {@link Rectangle} representing the area
         * </pre>
         */
        SELECT_AREA
    }
    private final MouseAdapter mouseComponentControls = new MouseAdapter() {

        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                pcs.firePropertyChange(Events.SELECT_ONE + "", e, e.getComponent());
            }
        }
    ;

    };

	public void addPropertyChangeListener(String propertyName,
            PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public Point getStart() {
        return start;
    }

    public Point getEnd() {
        return end;
    }

    public boolean isSelecting() {
        return selecting;
    }

    @Override
    public Controllable init() {
        return this;
    }

    public SelectController addSelectControls(JComponent c) {
        if (c != null) {
            c.addMouseListener(mouseComponentControls);
        }
        return this;
    }

    public SelectController removeSelectControls(JComponent c) {
        if (c != null) {
            c.removeMouseListener(mouseComponentControls);
        }

        return this;
    }

    public SelectController(JComponent eventSrc,
            AnimationSupport animationSupport) {
        super();
        this.eventSrc = eventSrc;
        this.frame = (JFrame) SwingUtilities.getRoot(this.eventSrc);
        this.mouseControls = new MouseControls();
        this.animationSupport = animationSupport;

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
     * Interfaces mouse
     * 
     */
    private class MouseControls extends MouseAdapter {

        @Override
        public void mouseEntered(MouseEvent e) {
            if (!selecting) {
                frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                SelectController.this.selecting = true;
                start.x = e.getX();
                start.y = e.getY();
                end.x = start.x;
                end.y = start.y;
                animationSupport.startStrokeSelection();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            end.x = e.getX();
            end.y = e.getY();
            eventSrc.repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                pcs.firePropertyChange(Events.DESELECT_ALL + "", null, null);

                selecting = false;
                int x = start.x < end.x ? start.x : end.x;
                int y = start.y < end.y ? start.y : end.y;
                pcs.firePropertyChange(Events.SELECT_AREA + "", null,
                        new Rectangle(x, y, Math.abs(end.x - start.x), Math.abs(end.y - start.y)));
                eventSrc.repaint();
            }

        }
    }
}
