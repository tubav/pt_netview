//License: GPL. Copyright 2008 by Jan Peter Stotz

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

package de.fhg.fokus.net.worldmap.layers.map;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Models a map tile. A tile is not intended to manage itself. It depends on a TileLoader and a TileSource for getting
 * it's image. That's the reason why the state is exported (via getState());
 * A tile is also a comparable type so we can use it on concurrent unsynchronized data structures such as
 * {@link ConcurrentSkipListSet}
 * 
 * @author Jan Peter Stotz (original)
 * @author FhG-FOKUS NETwork Research
 * 
 *
 */
public interface Tile extends Comparable<Tile>{
	public static int SIZE = 256;
	public static enum TileState {
		NEW,
		LOADING,
		LOADED,
		FAILED
	}
	/**
	 * Load tile image from an input stream
	 * 
	 * @param input
	 * @throws IOException
	 */
	 public void loadImage(InputStream input) throws IOException;
	/**
	 * 
	 * @return a proposed file name for storing the file on disk
	 * 
	 */
	public String getFilename();
	/**
	 * 
	 * @return a proposed directory name for storing the tile on disk
	 */
	public String getDirname();
	
	
	/**
	 * 
	 * @return tile image or null if it was not loaded yet
	 */
	public BufferedImage getImage();
	/**
	 * @return tile url
	 */
	public URL getUrl();
	/**
	 * 
	 * @return unique id used for tile caching
	 */
	public long getId();
	
	public AtomicReference<TileState> getState();
	
}
