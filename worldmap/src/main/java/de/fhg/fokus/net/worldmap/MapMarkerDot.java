/**
 * worldmap - an extension to JMapViewer which provides additional
 *            functionality. New functions allow setting markers,
 *            adding layers, and printing tracks on the map. (see
 *            http://wiki.openstreetmap.org/wiki/JMapViewer for more
 *            information on JMapViewer)
 *
 * Copyright (c) 2008
 * 
 * Jan Peter Stotz
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

//License: GPL. Copyright 2008 by Jan Peter Stotz

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.geo.WayPoint;


/**
 * A simple implementation of the {@link MapMarkerIF} interface. Each map marker
 * is painted as a circle with a black border line and filled with a specified
 * color.
 * 
 * @author Jan Peter Stotz
 * 
 */
public class MapMarkerDot extends MapMarker {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	private static final long serialVersionUID = -2273892217528508495L;
	int size_h = 10;
	Color color;	

	// Event support

	//    protected final List<ActionListener> mouse

	public MapMarkerDot(WayPoint waypoint) {
		this(Color.YELLOW, waypoint);
	}

	public MapMarkerDot(Color color, WayPoint waypoint) {
		this.waypoint = waypoint;
		this.color = color;
	}
	
	public MapMarkerDot(double lat, double lon) {
		this.waypoint = new WayPoint(lat,lon);
	}
	private AlphaComposite transparentComposite =
		AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4F);

	@Override
	public void paint(Graphics g, Point p) {
		//		super.paintComponent(g);
		if( !isVisible()){
			return;
		}
		int size = size_h * 2;

		final int left_x=p.x-size_h;
		final int left_y=p.y-size_h;
		setBounds(left_x, left_y, size+1, size+1);
//		
//		g.translate(left_x, left_y);
//		paintBorder(g);
//		g.translate(-left_x, -left_y);
		
		Graphics2D g2d = (Graphics2D)g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
				RenderingHints.VALUE_ANTIALIAS_ON);

		Composite origComposite = g2d.getComposite();


		
		g.setColor(color);
		g2d.setComposite(transparentComposite);
		g.fillOval(p.x - size_h, p.y - size_h, size, size);
		g.setColor(Color.BLACK);
		g.drawOval(p.x - size_h, p.y - size_h, size, size);
		g2d.setComposite(origComposite);
		
		if( selected ){
			g.setColor(Color.DARK_GRAY);
			g.drawRect(p.x-size_h, p.y-size_h, size, size);
		}
		

	}



}
