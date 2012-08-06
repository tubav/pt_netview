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
package de.fhg.fokus.net.worldmap.layers.track;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;
import java.util.ArrayList;

import javax.swing.JComponent;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.worldmap.control.AnimationSupport;
import de.fhg.fokus.net.worldmap.model.WayPointScreenLocator;

/**
 * A track renderer with animation support.
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */
public class TrackRenderer {

    private static final Logger logger = LoggerFactory.getLogger(TrackRenderer.class);
    /**
     * Last view (zoom, panning, resize) change timestamp
     * 
     */
    private long lastViewChangeTs = 0;

    /**
     * Update view changed timestamp so we know interpolated track points need
     * to be recalculated.
     */
    private void viewChanged() {
        lastViewChangeTs = System.currentTimeMillis();
    }
    /**
     * Provides a mapping between waypoints and screen location. This object is
     * injected by the framework (e.g. is a reference to the map layer of
     * worldmap)
     * 
     */
    private WayPointScreenLocator waypointScreenLocator;
    /**
     * A reference to the gui component to be refreshed after each animation
     * iteration.
     */
    private final JComponent reference;

    public static class Cubic {

        double a, b, c, d; /* a + b*u + c*u^2 +d*u^3 */


        public Cubic(double a, double b, double c, double d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }

        /** evaluate cubic */
        public double eval(float u) {
            return (((d * u) + c) * u + b) * u + a;
        }
    }

    public static class TrackCurve {

        public Cubic[] X;
        public Cubic[] Y;
        /**
         * Interpolated waypoints
         * 
         * <pre>
         * (x,y) = (longitude, latitude)
         * </pre>
         */
        public Point2D.Double[] wayPoints;
        /**
         * Interpolated points in screen coordinates. This should be
         * recalculated whenever screen changes. 
         */
        public Point[] screenPoints;
        /**
         * Interpolated points timestamp - used to decide whether screenPoints needed
         * to be updated. This is done by the track renderer when the map view
         * has changed (due to panning, zoom or resize)
         */
        public long screenPointsTs;
    }
    private final AnimationSupport animationSupport;
    // private final Map<Track, Point2D.Double[]> animationMap = new
    // ConcurrentHashMap<Track, Point2D.Double[]>();
    private int frame = 0;
    private Animator animator;

    public TrackRenderer(JComponent reference,
            AnimationSupport animationSupport) {
        this.reference = reference;
        this.animationSupport = animationSupport;
        animator = this.animationSupport.getTrackAnimator();
        if (!animator.isRunning()) {
            animator.start();
        }
        animator.addTarget(new TimingTarget() {

            @Override
            public void timingEvent(float fraction) {
                TrackRenderer.this.frame++;
                TrackRenderer.this.reference.repaint();
            }

            @Override
            public void repeat() {
            }

            @Override
            public void end() {
                // TODO Auto-generated method stub
            }

            @Override
            public void begin() {
                // TODO Auto-generated method stub
            }
        });
        reference.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                viewChanged();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                viewChanged();
            }
        });

    }

    public void injectWaypointScreenLocator(
            WayPointScreenLocator waypointScreenLocator) {
        if (waypointScreenLocator != null) {
            this.waypointScreenLocator = waypointScreenLocator;
            // recalculate points in case zoom of panning values have changed
            this.waypointScreenLocator.addViewChangeListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent arg0) {
                    viewChanged();
                }
            });
        }
    }

    void refreshTrackView(View view) {

        if (waypointScreenLocator == null || view == null) {
            return;
        }

        synchronized (view) {
            // initialize curve/curves

            if (view.curve == null) {
                view.curve = getCurve(view, -1, true, view.curveFactor);
            }

            // update screen points for the curve
            if (view.curve.screenPoints == null || view.curve.screenPointsTs < lastViewChangeTs) {
                relocateInterpolatedPoints(view.curve);
                // set new track.view coords
                if (view.line != null) {
                    if (view.line.xpoints == null || view.line.xpoints.length != view.curve.screenPoints.length) {
                        view.line.xpoints = new int[view.curve.screenPoints.length];
                    }
                    if (view.line.ypoints == null || view.line.ypoints.length != view.curve.screenPoints.length) {
                        view.line.ypoints = new int[view.curve.screenPoints.length];
                    }
                    for (int i = 0; i < view.curve.screenPoints.length; i++) {
                        view.line.xpoints[i] = view.curve.screenPoints[i].x;
                        view.line.ypoints[i] = view.curve.screenPoints[i].y;
                    }
                }
            }
        }

    }

    void renderObject(Graphics2D g2, View view, Color color, Stroke stroke) {
        g2.setStroke(stroke);
        g2.setColor(color);

        if (view.line.alphaComposite != null) {
            g2.setComposite(view.line.alphaComposite);
        }
        if (view.line.xpoints != null) {
            g2.drawPolyline(
                    view.line.xpoints,
                    view.line.ypoints,
                    view.line.xpoints.length);
        }
    }

    void renderObject(Graphics2D g2, View view) {
        if (view.line.visible) {
            renderObject(g2, view, view.line.color, view.line.stroke);
        }
    }

    void renderPackets(Graphics2D g2, View view) {
        int numberOfAnimatedPackets = 0;



        if (view.packetImage == null) {
            return;
        }

        numberOfAnimatedPackets = (int) (Math.sqrt(2 * Math.log(view.volume) * view.volume) + 0.5);
        if (numberOfAnimatedPackets == 0) {
            numberOfAnimatedPackets = 1;
        }

        int gap = view.curve.screenPoints.length / numberOfAnimatedPackets;
        if (frame > view.curve.screenPoints.length) {
            frame -= view.curve.screenPoints.length;
        }

        for (int i = 0; i < numberOfAnimatedPackets; i++) {
            int idx = frame + i * gap;
            if (idx >= view.curve.screenPoints.length) {
                idx -= view.curve.screenPoints.length;
            }
            if (idx < view.curve.screenPoints.length && view.curve.screenPoints[idx] != null) {
                g2.drawImage(view.packetImage,
                        view.curve.screenPoints[idx].x + view.packetImageOffset,
                        view.curve.screenPoints[idx].y + view.packetImageOffset, null);
            }
        }
    }

    public void render(Graphics2D g2, View view) {
        refreshTrackView(view);
        renderObject(g2, view);
        renderPackets(g2, view);
    }

    /**
     * Relocate interpolated points using a waypoint screen locator.
     * 
     * @param trackCurve
     */
    private void relocateInterpolatedPoints(TrackCurve trackCurve) {
        if (trackCurve.screenPoints == null
                || trackCurve.wayPoints.length != trackCurve.screenPoints.length) {
            trackCurve.screenPoints = new Point[trackCurve.wayPoints.length];
        }
        for (int i = 0; i < trackCurve.screenPoints.length; i++) {
            // allocate point as needed
            if (trackCurve.screenPoints[i] == null) {
                trackCurve.screenPoints[i] = new Point();
            }
            // relocate
            waypointScreenLocator.setXYFromWaypoint2D(trackCurve.screenPoints[i], trackCurve.wayPoints[i]);
        }
        trackCurve.screenPointsTs = lastViewChangeTs;
    }
    // TODO review 

    public TrackCurve getCurve(View view, int sign, boolean alternate, double flowFactor) {

        // TODO number of points should be configurable
        int pointsOnCurve = 200;  // approx. number of points on curve
        
        Point2D.Double[] fixedPoints = view.toArray();

        // generate a reference point between every 2 fixed points, to
        // define the shape of the curve
        Point2D.Double interpolated[] = new Point2D.Double[fixedPoints.length * 2 - 1];
        for (int i = 0; i < interpolated.length; i++) {
            if (i % 2 == 0) {
                interpolated[i] = fixedPoints[i / 2];
            } else {
                interpolated[i] = calcPoint(fixedPoints[i / 2], fixedPoints[i / 2 + 1], sign, view, flowFactor);
                if (alternate) {
                    sign *= -1;
                }
            }
        }

        // calculate cubics through the fixed/reference points
        double[] xpoints = new double[interpolated.length];
        double[] ypoints = new double[interpolated.length];
        for (int i = 0; i < interpolated.length; i++) {
            xpoints[i] = interpolated[i].getX();
            ypoints[i] = interpolated[i].getY();
        }
        Cubic xCubics[] = calcNaturalCubic(interpolated.length - 1, xpoints);
        Cubic yCubics[] = calcNaturalCubic(interpolated.length - 1, ypoints);

        // number of segments, and total distance
        int segments = xCubics.length;
        double totalDistance = 0;
        for (int i = 0; i < segments; i++) {
            totalDistance += interpolated[i].distance(interpolated[i + 1]);
        }

        // calculate points between fixed/reference points
        Vector<Point2D.Double> curvePoints = new Vector<Point2D.Double>();
        for (int i = 0; i < segments; i++) {
            double segmentDistance = interpolated[i].distance(interpolated[i + 1]);
            int pointsInSegment = (int) (((segmentDistance / totalDistance) * pointsOnCurve) + 0.5);

            if (pointsInSegment < 2) {
                pointsInSegment = 2;
            }

            for (int j = 0; j < pointsInSegment; j++) {
                float u = (float) j / pointsInSegment;
                curvePoints.add(new Point2D.Double(xCubics[i].eval(u), yCubics[i].eval(u)));
            }
        }
        curvePoints.add(new Point2D.Double(xCubics[segments - 1].eval(1), yCubics[segments - 1].eval(1)));

        TrackCurve curve = new TrackCurve();
        curve.X = xCubics;
        curve.Y = yCubics;
        curve.wayPoints = curvePoints.toArray(new Point2D.Double[0]);
        return curve;
    }

    private Point2D.Double calcPoint(Point2D.Double point1,
            Point2D.Double point2, int sign, View view, double flowFactor) {

        int lengthTrace = view.markers.size();

        double xmiddlepoint = (point1.getX() + point2.getX()) / 2;
        double ymiddlepoint = (point1.getY() + point2.getY()) / 2;
        double deltay = point2.getY() - point1.getY();
        double deltax = point2.getX() - point1.getX();
        if (deltay == 0) {
            deltay = 0.0001D;
        }
        if (deltax == 0) {
            deltax = 0.0001D;
        }
        double path_length = Math.sqrt(Math.pow(deltax, 2) + Math.pow(deltay, 2));
        double distance = path_length * (0.15 + lengthTrace / 50.);

        double slope = deltay / deltax;
        double normalslope = -1D / slope;
        double angle = Math.atan(normalslope);
        double newx = xmiddlepoint + (double) sign * flowFactor * distance * Math.cos(angle);
        double newy = ymiddlepoint + (double) sign * flowFactor * distance * Math.sin(angle);
        // logger.debug("point1 " + point1 + " point2 " + point2 + " deltax  " + deltax + " deltay " + deltay + " path_length " + path_length + " newx " + newx + " newy " + newy );

        Point2D.Double interpolated = new Point2D.Double(newx, newy);
        return interpolated;
    }

    private Cubic[] calcNaturalCubic(int n, double[] x) {
        double[] gamma = new double[n + 1];
        double[] delta = new double[n + 1];
        double[] D = new double[n + 1];
        int i;
        /**
         * We solve the equation
         * 
         * <pre>
         * 
         * [2 1       ] [D[0]]   [3(x[1] - x[0])  ]
         * |1 4 1     | |D[1]|   |3(x[2] - x[0])  |
         * |  1 4 1   | | .  | = |      .         |
         * |    ..... | | .  |   |      .         |
         * |     1 4 1| | .  |   |3(x[n] - x[n-2])|
         * [       1 2] [D[n]]   [3(x[n] - x[n-1])]
         * 
         * </pre>
         * 
         * by using row operations to convert the matrix to upper triangular and
         * then back substitution. The D[i] are the derivatives at the knots.
         */
        gamma[0] = 1.0f / 2.0f;
        for (i = 1; i < n; i++) {
            gamma[i] = 1 / (4 - gamma[i - 1]);
        }
        gamma[n] = 1 / (2 - gamma[n - 1]);

        delta[0] = 3 * (x[1] - x[0]) * gamma[0];
        for (i = 1; i < n; i++) {
            delta[i] = (3 * (x[i + 1] - x[i - 1]) - delta[i - 1]) * gamma[i];
        }
        delta[n] = (3 * (x[n] - x[n - 1]) - delta[n - 1]) * gamma[n];

        D[n] = delta[n];
        for (i = n - 1; i >= 0; i--) {
            D[i] = delta[i] - gamma[i] * D[i + 1];
        }

        /* now compute the coefficients of the cubics */
        Cubic[] C = new Cubic[n];
        for (i = 0; i < n; i++) {
            C[i] = new Cubic(x[i], D[i], 3 * (x[i + 1] - x[i]) - 2 * D[i]
                    - D[i + 1], 2 * (x[i] - x[i + 1]) + D[i] + D[i + 1]);
        }
        return C;
    }
}
