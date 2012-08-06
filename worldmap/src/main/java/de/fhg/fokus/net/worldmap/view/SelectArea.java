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

package de.fhg.fokus.net.worldmap.view;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;

import de.fhg.fokus.net.worldmap.control.AnimationSupport;

public class SelectArea {
	private final Point start;
	private final Point end;
	private final Composite selectComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f);
	private Color selectColor = new Color(250, 250, 95);
	private Color borderColor = new Color(0,0,0);
	//	final float[] DASH_PATTERN = { 4f };
	//	final Stroke stroke = new BasicStroke(1.0f,BasicStroke.CAP_SQUARE,BasicStroke.CAP_SQUARE,4f,DASH_PATTERN,2f);
	private Stroke stroke = null;
	private final AnimationSupport animationSupport;
	private final JComponent reference;
	public SelectArea(JComponent reference, AnimationSupport animationSupport,  Point start, Point end) {
		super();
		this.animationSupport = animationSupport;
		this.start = start;
		this.end = end;
		this.reference = reference;
		this.stroke=this.animationSupport.getDefaultSelectionStroke();
		setupStrokeListener();
	}

	private void setupStrokeListener() {
		animationSupport.addPropertyChangeListener(AnimationSupport.Events.SELECTION_STROKE_CHANGED+"",
				new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				// it should be safe to cast to Stroke
				stroke = (Stroke)evt.getNewValue();
				reference.repaint();
			}
		});
	}

	public void  paint( Graphics g ){
		Graphics2D g2 = (Graphics2D)g;
		//		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Composite oldComposite = g2.getComposite();
		g2.setComposite(selectComposite);
		g2.setColor(selectColor);
		int x = start.x < end.x?start.x:end.x;
		int y = start.y < end.y?start.y:end.y;
		g2.fillRect(x, y, Math.abs(end.x-start.x), Math.abs(end.y-start.y));
		g2.setColor(borderColor);
		if(stroke!=null){
			g2.setStroke(stroke);
		}
		g2.drawRect(x, y, Math.abs(end.x-start.x), Math.abs(end.y-start.y));
		g2.setComposite(oldComposite);

	}
}
