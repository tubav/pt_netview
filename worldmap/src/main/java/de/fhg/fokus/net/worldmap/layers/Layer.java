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

import javax.swing.JComponent;
/**
 * Base class for WorldMap Layers
 * 
 * @author FhG-FOKUS NETwork Research
 *
 */
public class Layer extends JComponent implements Comparable<Layer> {

	/**
	 * Layer level. It uses JLayeredPane integers, ie. higher number are topmost.
	 * 
	 */
	protected final String layerId;
	public Layer(String layerId) {
		super();
		this.layerId = layerId;
		this.setOpaque(false);
	}
	private int level=0;
	private static final long serialVersionUID = -890085583642273353L;
	public int getLevel() {
		return level;
	}
	public Layer setLevel(int level) {
		this.level = level;
		return this;
	}
	public String getLayerId() {
		return layerId;
	}
	@Override
	public int compareTo(Layer layer) {
		return level-layer.getLevel();
	}
	
	
}
