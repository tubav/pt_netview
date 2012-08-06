package de.fhg.fokus.net.netview.model;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.awt.Color;
import java.awt.Composite;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.awt.AlphaComposite;

import de.fhg.fokus.net.worldmap.MapMarker;
import de.fhg.fokus.net.worldmap.layers.track.View;

public class ViewFactory {
    private static final Stroke SOLID_STROKE_THIN = new BasicStroke(1.5f,
			BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke SOLID_STROKE_MEDIUM = new BasicStroke(2.5f,
			BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke SOLID_STROKE_THICK = new BasicStroke(4.0f,
			BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Composite DEFAULT_COMPOSITE =
            AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
    
    public static Color COLOR_ANIM_01 = new Color(0x99, 0x99, 0x77);
	public static Color COLOR_ANIM_02 = new Color(0x77, 0x77, 0x77);
	public static Color COLOR_ANIM_03 = new Color(0x77, 0x77, 0x77);
	public static Color COLOR_ANIM_04 = new Color(0x77, 0x77, 0x77);
        
    public static View createAnimatedView(long id,
            View.Type viewType, int volume, List<MapMarker> markers) {
        View view = new View(new CopyOnWriteArrayList<MapMarker>(markers));
        view.composite = DEFAULT_COMPOSITE;
        view.type = viewType;
        view.volume = volume;
        
		view.line.color = COLOR_ANIM_01;
		view.line.stroke = SOLID_STROKE_THIN;
		if( 0 < view.volume && view.volume < 10   ){
			view.line.color = COLOR_ANIM_01;
			view.line.stroke = SOLID_STROKE_MEDIUM;
		} 
		if (10 < view.volume && view.volume < 15 ) {
			view.line.stroke = SOLID_STROKE_MEDIUM;
			view.line.color = COLOR_ANIM_01;
		} 
		if( view.volume > 15 ){
			view.line.stroke = SOLID_STROKE_THICK;
			view.line.color = COLOR_ANIM_01;
		}

		view.line.color = getColorFromId(id);
        return view;
    }
    
    private static Color getColorFromId(long id) {
		long trackId = id;
		trackId = trackId >> 32 ^ trackId & 0xFFFFFFFFL;
		trackId = trackId >> 16 ^ trackId & 0xFFFF;
		return Color.getHSBColor(trackId * 0.6180339887f, 0.6f, 0.8f);
	}
}
