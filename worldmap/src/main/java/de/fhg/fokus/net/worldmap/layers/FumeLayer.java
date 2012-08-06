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

package de.fhg.fokus.net.worldmap.layers;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.prefs.Preferences;

import de.fhg.fokus.net.worldmap.model.PredefinedLayer;
/**
 * Used for fading map so graphics and network animations can be better visualized.
 * @author FhG-FOKUS NETwork Research
 *
 */
public class FumeLayer extends PredefinedLayer {
	private final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
	private enum PrefKeys {
		FUMELAYER_VISIBLE
	}
	public FumeLayer() {
		super("Fume");
	}
	private static final long serialVersionUID = 3501025944631382640L;
	private Color bgColor = new Color(55, 55, 55);
//	private Color bgColor = new Color(255, 255, 255);
	
	private final Composite bgComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f);
	@Override
	protected void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Composite oldComposite = g2.getComposite();
		g2.setComposite(bgComposite);
		Rectangle clip = g2.getClipBounds();
		g2.setColor(bgColor);
		g2.fillRect(clip.x,clip.y,clip.width,clip.height);
		g2.setComposite(oldComposite);

	}
	@Override
	public void loadPreferences() {
		setVisible(prefs.getBoolean(PrefKeys.FUMELAYER_VISIBLE+"", false));
		
	}
	@Override
	public void savePreferences() {
		prefs.putBoolean(PrefKeys.FUMELAYER_VISIBLE+"", isVisible());
		
	}
}
