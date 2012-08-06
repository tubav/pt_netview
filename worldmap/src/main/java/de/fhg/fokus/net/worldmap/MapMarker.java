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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.Timer;

import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.ModernBalloonStyle;
import de.fhg.fokus.net.geo.WayPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MapMarker extends JComponent {

    private static final long serialVersionUID = -4430894727540693536L;
    protected boolean selected = false;
    protected boolean toolTipEnabled = true;
    protected String label;
    public long oid;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected int showToolTipDelay = 400;
    
    public boolean isSelected() {
        return selected;
    }

    public MapMarker setSelected(boolean selected) {
        this.selected = selected;
        repaint();
        return this;
    }
    /**
     * the marker reference object, which is intended to be used externally
     * 
     */
    private Object reference;

    /**
     * <p> Throws class cast exceptions if reference were of different type </p>
     * 
     * @param <R>
     * @param klass
     * @return reference object
     */
    @SuppressWarnings("unchecked")
    public <R> R getReference(Class<R> klass) {

        return (R) reference;
    }

    public MapMarker setReference(Object reference) {
        this.reference = reference;
        return this;
    }

    public MapMarker() {
        if (getWidth() == 0 && getHeight() == 0) {
            setBounds(10, 10, 10, 10);
        }
        //logger.debug("Marker created!");
    }

    public abstract void paint(Graphics g, Point p);

    public MapMarker(double longitude, double latitude) {
        this();
        this.waypoint.longitude = longitude;
        this.waypoint.latitude = latitude;

    }

    public MapMarker(WayPoint waypoint) {
        super();
        this.waypoint = waypoint;

    }
    /**
     * Waypoint 
     */
    public WayPoint waypoint;

    public void setWaypoint(WayPoint waypoint) {
        this.waypoint = waypoint;
    }

    public WayPoint getWaypoint() {
        return waypoint;
    }

    @Override
    public String toString() {
        return String.format(getLabel());
    }

    public double getLongitude() {
        return waypoint.getLongitude();
    }

    public double getLatitude() {
        return waypoint.getLatitude();
    }
    protected BalloonTip balloonTip;

    public void setBalloonTip(BalloonTip balloonTip) {
        this.balloonTip = balloonTip;
    }
    private static final ModernBalloonStyle balloonStyle = new ModernBalloonStyle(10, 10, Color.WHITE, Color.LIGHT_GRAY, Color.GRAY);

    static {
        balloonStyle.setBorderThickness(3);
        balloonStyle.enableAntiAliasing(true);
        balloonStyle.setVerticalOffset(10);
    }

    /**
     * Shows a balloon tool tip. 
     * 
     * @param text tool tip contents (can be html)
     */
    @Override
    public void setToolTipText(String text) {
        if (balloonTip == null) {
            balloonTip = new BalloonTip(this, new JLabel(text), balloonStyle, false);
            balloonTip.setVisible(false);
            this.addMouseListener(new MouseAdapter() {

                private final Timer timer = new Timer(showToolTipDelay,
                        new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                        balloonTip.refreshLocation();
                        balloonTip.setVisible(true);
                    }
                });

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (toolTipEnabled) {
                        timer.setRepeats(false);
                        timer.restart();
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    balloonTip.setVisible(false);
                    timer.stop();
                }
            });
        } else {
            balloonTip.setContents(new JLabel(text));
        }
    }

    public boolean isToolTipEnabled() {
        return toolTipEnabled;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setToolTipEnabled(boolean toolTipEnabled) {
        this.toolTipEnabled = toolTipEnabled;
    }
}
