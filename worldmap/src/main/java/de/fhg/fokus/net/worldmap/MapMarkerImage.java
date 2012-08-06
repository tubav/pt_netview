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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.geo.WayPoint;
import de.fhg.fokus.net.worldmap.control.AnimationSupport;
import de.fhg.fokus.net.worldmap.model.Animated;
import de.fhg.fokus.net.worldmap.view.ViewUtil;

/**
 * A map marker that uses an image (and shadow)
 * 
 * @author FhG-FOKUS NETwork Research
 *
 * @param <R>
 */
public class MapMarkerImage extends MapMarker implements Animated {

    protected static final Logger logger = LoggerFactory.getLogger(MapMarkerImage.class);
    private static BufferedImage defaultImage;
    private static BufferedImage defaultShadow;
    private static final String MARKERS_DIR = "view/resources/markers/";

    static {
        try {
            defaultImage = ViewUtil.toCompatibleImage(ImageIO.read(MapMarkerImage.class.getResource(MARKERS_DIR + "marker-small.png")));
            defaultShadow = ViewUtil.toCompatibleImage(ImageIO.read(MapMarkerImage.class.getResource(MARKERS_DIR + "marker-small-shadow.png")));
            if (defaultImage == null || defaultShadow == null) {
                throw new RuntimeException("Default marker image were not found: " + MARKERS_DIR);
            }


        } catch (IOException e) {
            logger.error("could not find marker images at: " + MARKERS_DIR + e.getMessage());
        }
    }
    private static final long serialVersionUID = -1402486629301742153L;
    protected volatile BufferedImage image;
    protected volatile BufferedImage shadow;
    protected int imageWidth, imageHeight;
    protected int offsetX, offsetY;

    public MapMarkerImage(WayPoint waypoint, long uid) {
        this.waypoint = waypoint;
        setRealImage(uid);

        setupImage();
    }

    public MapMarkerImage(WayPoint waypoint, long uid, String marker) {
        this(waypoint, uid);
        try {
            image = ViewUtil.toCompatibleImage(ImageIO.read(MapMarkerImage.class.getResource(MARKERS_DIR + marker)));
        } catch (Exception e) {
            image = defaultImage;
            logger.warn(String.format("Marker %s not found, using default", marker));
        }
        shadow = null;
        setupImage();
    }

    private void setupImage() {
        if (image != null) {
            imageWidth = image.getWidth();
            imageHeight = image.getHeight();
            offsetX = -imageWidth / 2;
            offsetY = -imageHeight;
        }
    }
    private final static float[] DASH_PATTERN = {4f};
    private final static Stroke defaultSelectStroke = new BasicStroke(1.0f, BasicStroke.CAP_SQUARE, BasicStroke.CAP_SQUARE, 4f, DASH_PATTERN, 0f);
    private final static Composite selectComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
//	private final static Color selectColor = new Color(145, 145, 55);
    private final static Color selectColor = new Color(250, 250, 95);
    private Stroke selectStroke = defaultSelectStroke;

    @Override
    public void paint(Graphics g, Point p) {
        setBounds(p.x + offsetX - 3, p.y + offsetY - 3, imageWidth + 6, imageHeight + 6);
        if (!isVisible()) {
            return;
        }
        if (image != null) {
            g.drawImage(image, p.x + offsetX, p.y + offsetY, imageWidth, imageHeight, null);
        }
        if (shadow != null) {
            g.drawImage(shadow, p.x + offsetX, p.y + offsetY, shadow.getWidth(), shadow.getHeight(), null);
        }
        if (selected) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Composite oldComposite = g2.getComposite();
            g2.setComposite(selectComposite);
            g2.setColor(selectColor);
            g.fillOval(getX(), getY(), getWidth() - 1, getHeight() - 1);
            g2.setComposite(oldComposite);
            g2.setStroke(selectStroke);

            g.setColor(Color.DARK_GRAY);
            g.drawOval(getX(), getY(), getWidth() - 1, getHeight() - 1);

        }

    }

    @Override
    public void injectAnimationSupport(AnimationSupport animationSupport) {
        animationSupport.addPropertyChangeListener(AnimationSupport.Events.SELECTION_STROKE_CHANGED + "", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent e) {
                selectStroke = (Stroke) e.getNewValue();
                repaint();
            }
        });
    }

    private void setRealImage(long uid) {
        if (uid == 100) {
            try {
                image = ViewUtil.toCompatibleImage(ImageIO.read(MapMarkerImage.class.getResource(MARKERS_DIR + "pdngw.png")));
                defaultShadow = ViewUtil.toCompatibleImage(ImageIO.read(MapMarkerImage.class.getResource(MARKERS_DIR + "marker-small-shadow.png")));
                if (image == null || defaultShadow == null) {
                    throw new RuntimeException("Default marker image were not found: " + MARKERS_DIR);
                }
            } catch (IOException e) {
                logger.error("could not find marker images at: " + MARKERS_DIR + e.getMessage());
            }
        } else if (uid == 200) {
            try {
                image = ViewUtil.toCompatibleImage(ImageIO.read(MapMarkerImage.class.getResource(MARKERS_DIR + "sgw.png")));
                defaultShadow = ViewUtil.toCompatibleImage(ImageIO.read(MapMarkerImage.class.getResource(MARKERS_DIR + "marker-small-shadow.png")));
                if (image == null || defaultShadow == null) {
                    throw new RuntimeException("Default marker image were not found: " + MARKERS_DIR);
                }
            } catch (IOException e) {
                logger.error("could not find marker images at: " + MARKERS_DIR + e.getMessage());
            }
        } else if (uid == 210) {
            try {
                image = ViewUtil.toCompatibleImage(ImageIO.read(MapMarkerImage.class.getResource(MARKERS_DIR + "epdg.png")));
                defaultShadow = ViewUtil.toCompatibleImage(ImageIO.read(MapMarkerImage.class.getResource(MARKERS_DIR + "marker-small-shadow.png")));
                if (image == null || defaultShadow == null) {
                    throw new RuntimeException("Default marker image were not found: " + MARKERS_DIR);
                }
            } catch (IOException e) {
                logger.error("could not find marker images at: " + MARKERS_DIR + e.getMessage());
            }
        } else if (uid == 300) {
            try {
                image = ViewUtil.toCompatibleImage(ImageIO.read(MapMarkerImage.class.getResource(MARKERS_DIR + "epc-enabler.png")));
                defaultShadow = ViewUtil.toCompatibleImage(ImageIO.read(MapMarkerImage.class.getResource(MARKERS_DIR + "marker-small-shadow.png")));
                if (image == null || defaultShadow == null) {
                    throw new RuntimeException("Default marker image were not found: " + MARKERS_DIR);
                }
            } catch (IOException e) {
                logger.error("could not find marker images at: " + MARKERS_DIR + e.getMessage());
            }
        } else if (uid == 133 || uid == 233) {
            try {
                image = ViewUtil.toCompatibleImage(ImageIO.read(MapMarkerImage.class.getResource(MARKERS_DIR + "client.png")));
                defaultShadow = ViewUtil.toCompatibleImage(ImageIO.read(MapMarkerImage.class.getResource(MARKERS_DIR + "marker-small-shadow.png")));
                if (image == null || defaultShadow == null) {
                    throw new RuntimeException("Default marker image were not found: " + MARKERS_DIR);
                }
            } catch (IOException e) {
                logger.error("could not find marker images at: " + MARKERS_DIR + e.getMessage());
            }
        } else {
            image = defaultImage;
            shadow = defaultShadow;
        }
    }

    /**
     * Set marker image 
     * @param image
     */
    public void setImage(final BufferedImage markerImage, long uid) {
        if (markerImage != null) {
            image = markerImage;
        } else {
            setRealImage(uid);
            //image = defaultImage;
        }
        setupImage();
    }

    public void setShadow(final BufferedImage markerShadow) {
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                shadow = markerShadow;

            }
        });
    }
}
