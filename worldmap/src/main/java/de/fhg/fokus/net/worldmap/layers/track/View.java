package de.fhg.fokus.net.worldmap.layers.track;

import java.awt.Color;
import java.awt.Composite;
import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.worldmap.MapMarker;
import de.fhg.fokus.net.worldmap.view.ViewUtil;

public class View {

    private static Logger logger = LoggerFactory.getLogger(Track.class);
    
    final static float[] DASH_PATTERN = {4f};
    final static Stroke defaultDashedStroke = new BasicStroke(1.0f,
            BasicStroke.CAP_SQUARE, BasicStroke.CAP_SQUARE, 4f, DASH_PATTERN,
            2f);
    final static Stroke defaultSolidStroke = new BasicStroke(2.5f,
            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    
    private static BufferedImage defaultPacketImage;
    private static int defaultPacketImageOffset = 0;
    
    public final List<MapMarker> markers;
    
    public Composite composite;
    public int volume;
    public double curveFactor;
    
    public static enum Type {
        SIMPLE, ANIMATED
    }
    
    public View(List<MapMarker> markers) {
        this.markers = markers;
        this.curveFactor = 1.0;
    }
    
    static {
        String packetImage = "/de/fhg/fokus/net/worldmap/view/resources/packet18.png";
        try {
            defaultPacketImage = ViewUtil.toCompatibleImage(ImageIO.read(Track.class.getResource(packetImage)));
            defaultPacketImageOffset = -defaultPacketImage.getWidth() / 2;
        } catch (Exception e) {
            logger.error("File missing: " + packetImage
                    + " \nDisabling track animation!");
        }
    }
    
      public Point2D.Double[] toArray() {
        // TODO review this
        List<Point2D.Double> pts = new ArrayList<Point2D.Double>(markers.size());
        for (MapMarker m : markers) {
            pts.add(new Point2D.Double(m.getLongitude(), m.getLatitude()));
        }
        return pts.toArray(new Point2D.Double[0]);
    }
    
    public static class Line {

        public boolean visible = true;
        public Stroke stroke = defaultDashedStroke;
        public Color color = Color.RED;
        public AlphaComposite alphaComposite;
        public int[] xpoints;
        public int[] ypoints;
    }

    public Type type;
    public final Line line = new Line();
    public BufferedImage packetImage = defaultPacketImage;
    public int packetImageOffset = defaultPacketImageOffset;
    // public int numberOfAnimatedPackets = 4;
    TrackRenderer.TrackCurve curve;

    public View setType(Type type) {
        this.type = type;
        return this;
    }
}