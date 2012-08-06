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

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.BalloonTip.*;
import net.java.balloontip.utils.TimingUtils;
import net.java.balloontip.styles.ModernBalloonStyle;
import javax.swing.Timer;
import javax.swing.JLabel;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.worldmap.control.AnimationSupport;
import de.fhg.fokus.net.worldmap.layers.Layer;
import de.fhg.fokus.net.worldmap.model.WayPointScreenLocator;

/**
 * <p> Layer responsible for showing Tracks.</p>
 * 
 * 
 * @author FhG-FOKUS NETwork Research
 *
 */
public class SplineLayer extends Layer implements AWTEventListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    public final long DEFAULT_RETENTION_WINDOW_MILLIS = 1000;
    private static final long serialVersionUID = 1L;
    private WayPointScreenLocator waypointScreenLocator;
    private AnimationSupport animationSupport;
    // Ring buffer
    private int writerIndex = 0;
    private final Track[] buffer;
    private Map<View.Type, TrackRenderer> trackRendererMap = new ConcurrentHashMap<View.Type, TrackRenderer>();
    // UI - Controls
    private Point mousePos;

    private class TrackSelectionMask implements ComponentListener, PropertyChangeListener {

        BufferedImage mask;
        private Map<MapObject, Color> colorMap = new HashMap<MapObject, Color>();
        private Map<Color, MapObject> objectMap = new HashMap<Color, MapObject>();
        private Stroke stroke = new BasicStroke(8.0f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND);
        private Color emptyColor = Color.black;

        @Override
        public void componentMoved(ComponentEvent e) {
            renderMask();
        }

        @Override
        public void componentResized(ComponentEvent e) {
            mask = null;
            renderMask();
        }

        @Override
        public void componentHidden(ComponentEvent arg0) {
            renderMask();
        }

        @Override
        public void componentShown(ComponentEvent arg0) {
            renderMask();
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            renderMask();
        }

        private Color addSplineToColorMap(Object obj) {
            Color color = emptyColor;
            MapObject mo = new MapObject();

            if (obj.getClass().equals(Track.class)) {
                Track t = (Track) obj;
                mo.id = t.getTrackId();
                mo.type = MapObject.Type.TRACK;
            } else if (obj.getClass().equals(Bearer.class)) {
                Bearer b = (Bearer) obj;
                mo.id = b.getBearerId();
                mo.type = MapObject.Type.BEARER;
            } else if (obj.getClass().equals(Flow.class)) {
                Flow f = (Flow) obj;
                mo.id = f.getFlowId();
                mo.type = MapObject.Type.FLOW;
            }

            color = colorMap.get(mo);
            if (color == null) {
                color = new Color((int) mo.id, false);
                colorMap.put(mo, color);
                objectMap.put(color, mo);
            }
            return color;
        }

        private void renderMask() {
            if (mask == null) {
                if (getHeight() == 0 && getWidth() == 0) {
                    return;
                }
                mask = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            }

            Graphics2D g = mask.createGraphics();
            g.setColor(emptyColor);
            g.clearRect(0, 0, mask.getWidth(), mask.getHeight());

            TrackRenderer tr = trackRendererMap.get(View.Type.ANIMATED);
            for (Track t : buffer) {
                if (t != null) {
                    Color color = emptyColor;
                    if (t.hasBearers()) {
                        for (Bearer b : t.getBearers()) {
                            if (b.hasFlows()) {
                                for (Flow f : b.getFlows()) {
                                    color = this.addSplineToColorMap(f);
                                    tr.refreshTrackView(f.view);
                                    tr.renderObject(g, f.view, color, stroke);
                                }
                            } else {
                                color = this.addSplineToColorMap(b);
                                tr.refreshTrackView(b.view);
                                tr.renderObject(g, b.view, color, stroke);
                            }
                        }
                    } else {
                        color = this.addSplineToColorMap(t);
                        tr.refreshTrackView(t.view);
                        tr.renderObject(g, t.view, color, stroke);
                    }

                }
            }
            g.dispose();
        }

        public MapObject getSplineAt(Point pos) {
            MapObject mo = null;

            if (mask != null
                    && pos.x >= 0 && pos.x < mask.getWidth()
                    && pos.y >= 0 && pos.y < mask.getHeight()) {

                Color c = new Color(mask.getRGB(pos.x, pos.y));
                if (!c.equals(emptyColor)) {
                    mo = objectMap.get(c);
                    if (mo == null) {
                        logger.warn("BUG: can't map color " + c + " to object");
                    }
                }
            }
            return mo;
        }
    }
    TrackSelectionMask selectionMask;

    public void refresh() {
        selectionMask.renderMask();
    }

    /**
     * Internal method used by Worldmap used to inject a waypoint ScreenLocator
     * 
     * @param waypointScreenLocator
     */
    public void injectWaypointScreenLocator(WayPointScreenLocator waypointScreenLocator) {
        this.waypointScreenLocator = waypointScreenLocator;
        for (TrackRenderer tr : trackRendererMap.values()) {
            tr.injectWaypointScreenLocator(waypointScreenLocator);
        }
        waypointScreenLocator.addViewChangeListener(selectionMask);
    }

    /**
     * Internal method used by Worldmap used to inject animation support
     * 
     * @param animationSupport
     */
    public void injectAnimationSupport(AnimationSupport animationSupport) {
        this.animationSupport = animationSupport;
        trackRendererMap.put(View.Type.ANIMATED, new TrackRenderer(this, this.animationSupport));
    }

    /**
     * Create a track layer.
     * 
     * @param layerId String representation
     * @param capacity Number of tracks that can be shown simultaneously, which must be greater than 0.
     */
    public SplineLayer(String layerId, int capacity) {
        super(layerId);
        this.buffer = new Track[capacity];

        selectionMask = new TrackSelectionMask();
        this.addComponentListener(selectionMask);
        getToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);

    }

    /**
     * Add a track. It overwrites track with the same timestamp, i.e. track.timestamp
     * should be unique.
     * 
     * @param track
     * @return
     */
    public void addTrack(Track track) {
        buffer[writerIndex++] = track;
        if (writerIndex == buffer.length) {
            writerIndex = 0;
        }
        // TODO: add addTracks(Track[] tracks);
        selectionMask.renderMask();
    }

    /**
     * <p> Reset internal track buffer.</p>
     * 
     */
    public void reset() {
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = null;
        }
        selectionMask.renderMask();
    }

    public Track[] getTrackBuffer() {
        return buffer;
    }

    public Track findTrack(long tid) {
        for(Track t : buffer) {
            if(t == null)
                continue;
            if(t.getTrackId() == tid)
                return t;
        }
        return null;
    }

    public Bearer findBearer(long bid) {
        for (Track t : buffer) {
            if (t == null) {
                continue;
            }
            if (t.hasBearers()) {
                Bearer b = t.getBearer(bid);
                if (b == null) {
                    continue;
                } else {
                    return b;
                }
            }
        }
        return null;
    }
    
    public Flow findFlow(long fid) {
        for (Track t : buffer) {
            if (t == null) {
                continue;
            }
            if (t.hasBearers()) {
                for (Bearer b : t.getBearers()) {
                    if (b == null) {
                        continue;
                    }
                    if (b.hasFlows()) {
                        Flow f = b.getFlow(fid);
                        if (f == null) {
                            continue;
                        } else {
                            return f;
                        }
                    }

                }
            }
        }
        return null;
    }

    private void showTooltip(Graphics2D g2, MapObject selectedObject) {
        g2.setColor(Color.BLACK);

        ModernBalloonStyle balloonStyle = new ModernBalloonStyle(10,
                10, Color.WHITE, Color.LIGHT_GRAY, Color.GRAY);
        BalloonTip bt = null;

        switch (selectedObject.type) {
            case TRACK:
                Track track = findTrack(selectedObject.id);
                if (track != null) {
                    bt = new BalloonTip(this,
                            new JLabel(createTrackInfoHTML(track)),
                            balloonStyle,
                            Orientation.LEFT_BELOW,
                            AttachLocation.ALIGNED,
                            10, 10, false);
                }
                break;
            case BEARER:
                Bearer bearer = findBearer(selectedObject.id);
                if (bearer != null) {
                    bt = new BalloonTip(this,
                            new JLabel(createBearerInfoHTML(bearer)),
                            balloonStyle,
                            Orientation.LEFT_BELOW,
                            AttachLocation.ALIGNED,
                            10, 10, false);
                }
                break;
            case FLOW:
                Flow flow = findFlow(selectedObject.id);
                if (flow != null) {
                    bt = new BalloonTip(this,
                            new JLabel(createFlowInfoHTML(flow)),
                            balloonStyle,
                            Orientation.LEFT_BELOW,
                            AttachLocation.ALIGNED,
                            10, 10, false);
                }
                break;
        }

        if (bt != null) {            
            bt.setLocation(mousePos.x - 10, mousePos.y + bt.getBounds().y - 10);
            TimingUtils.showTimedBalloon(bt, 1);
        }

    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        MapObject selectedObject = null;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        if (waypointScreenLocator == null) {
            return;
        }
  
        if (mousePos != null) {
            selectedObject = selectionMask.getSplineAt(mousePos);
            if (selectedObject != null) {
                this.showTooltip(g2, selectedObject);
            }
        }

        for (Track track : buffer) {
            if (track == null) {
                continue;
            }

            TrackRenderer tr = trackRendererMap.get(track.view.type);

            if (selectedObject != null) {
                switch (selectedObject.type) {
                    case TRACK:
                        if (selectedObject.id == track.getTrackId()) {
                            renderSelectedSpline(g2, tr, track.view);
                        } else {
                            renderSplines(g2, tr, track);
                        }
                        break;

                    case BEARER:
                        if (track.isBearerId(selectedObject.id)) {
                            Bearer bearer = track.getBearer(selectedObject.id);
                            renderSplines(g2, tr, track);
                            renderSelectedSpline(g2, tr, bearer.view);
                        } else {
                            renderSplines(g2, tr, track);
                        }
                        break;

                    case FLOW:
                        if (track.isFlowId(selectedObject.id)) {
                            Flow flow = track.getFlow(selectedObject.id);
                            renderSplines(g2, tr, track);
                            renderSelectedSpline(g2, tr, flow.view);
                        } else {
                            renderSplines(g2, tr, track);
                        }
                        break;
                }
            } else {
                renderSplines(g2, tr, track);
            }
        }

    }
    
    private String createTrackInfoHTML(Track track) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<html><table>"
                + "<tr><td><b>Track:</b></td><td><b>%s</b></td></tr>"
                + "<tr><td><small>Start Time:</small></td>"
                + "<td><small>%s ms</small></td></tr>"
                + "<tr><td><small>Stop Time:</small></td>"
                + "<td><small>%s ms</small></td></tr>"
                + "</table></html>", track.getTrackId(),
                track.getStartTs()/1000,
                track.getStopTs()/1000));
        
        return sb.toString();
    }

    private String createFlowInfoHTML(Flow flow) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<html><table>"
                + "<tr><td><b>Flow:</b></td><td><b>%s</b></td></tr>"
                + "<tr><td><small>Src. Ip:</small></td>"
                + "<td><small>%s:%s</small></td></tr>"
                + "<tr><td><small>Dst. Ip:</small></td>"
                + "<td><small>%s:%s</small></td></tr>"
                + "</table></html>", flow.getFlowId(),
                flow.getSrcIp(),
                flow.getSrcPort(),
                flow.getDstIp(),
                flow.getDstPort()));
        return sb.toString();
    }

    private String createBearerInfoHTML(Bearer bearer) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("<html><table>"
                + "<tr><td><b>Bearer:</b></td><td><b>%s</b></td></tr>"
                + "<tr><td><small>RuleId:</small></td>"
                + "<td><small>%s</small></td></tr>"
                + "<tr><td><small>APN:</small></td>"
                + "<td><small>%s</small></td></tr>"
                + "<tr><td><small>RuleName:</small></td>"
                + "<td><small>%s</td></tr>"
                + "<tr><td><small>IMSI:</small></td>"
                + "<td><small>%s</td></tr>"
                + "<tr><td><small>Max. Dl:</small></td>"
                + "<td><small>%s kbps</small></td></tr>"
                + "<tr><td><small>Max. Ul:</small></td>"
                + "<td><small>%s kbps</small></td></tr>"
                + "<tr><td><small>Gua. Dl:</small></td>"
                + "<td><small>%s kbps</small></td></tr>"
                + "<tr><td><small>Gua. Ul:</small></td>"
                + "<td><small>%s kbps</small></td></tr>"
                + "<tr><td><small>APN. Dl:</small></td>"
                + "<td><small>%s kbps</small></td></tr>"
                + "<tr><td><small>APN. Ul:</small></td>"
                + "<td><small>%s kbps</small></td></tr>"
                + "<tr><td><small>Src. IP:</small></td>"
                + "<td><small>%s:%s</small></td></tr>"
                + "<tr><td><small>Dst. IP:</small></td>"
                + "<td><small>%s:%s</small></td></tr>"
                + "</table></html>", bearer.getBearerId(),
                bearer.getRuleId(),
                bearer.getApn(),
                bearer.getRuleName(),
                bearer.getImsi(),
                bearer.getMaxDownload(),
                bearer.getMaxUpload(),
                bearer.getGuaDownload(),
                bearer.getGuaUpload(),
                bearer.getApnDownload(),
                bearer.getApnUpload(),
                bearer.getSrcIp(),
                bearer.getSrcPort(),
                bearer.getDstIp(),
                bearer.getDstPort()));
        return sb.toString();
    }

    private void renderSelectedSpline(Graphics2D g2, 
            TrackRenderer tr, View view) {
        tr.refreshTrackView(view);
        tr.renderObject(g2, view, view.line.color.brighter(), view.line.stroke);
        tr.renderPackets(g2, view);
    }

    private void renderSplines(Graphics2D g2,
            TrackRenderer tr, Track track) {
        if (track.hasBearers()) {
            for (Bearer bearer : track.getBearers()) {
                if (bearer.hasFlows()) {
                    for (Flow flow : bearer.getFlows()) {
                        tr.render(g2, flow.view);
                    }
                } else {
                    tr.render(g2, bearer.view);
                }
            }
        } else {
            tr.render(g2, track.view);
        }
    }

    public MapObject getSelectedSpline() {
        return selectionMask.getSplineAt(mousePos);
    }

    @Override
    public void eventDispatched(AWTEvent e) {
        if (e instanceof MouseEvent) {
            MouseEvent me = (MouseEvent) e;
            if (me.getComponent().equals(getParent())) {
                mousePos = me.getPoint();
            }
        }
    }
}
