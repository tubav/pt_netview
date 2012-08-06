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

import java.beans.PropertyChangeListener;

/**
 * Implement this interface for creating your custom tile cache for
 * {@link JMapViewer}.
 * 
 * @author Jan Peter Stotz (original)
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */
public interface TileLoader {
	public static enum Events {
		/**
		 * <p>started to load tile </p>
		 * <pre>
		 * oldvalue = null
		 * newvalue = tile ({@link Tile})
		 * </pre>
		 */
		TILE_LOAD_STARTED,
		/**
		 * <p>finished to load tile </p>
		 * <pre>
		 * oldvalue = null
		 * newvalue = tile ({@link Tile})
		 * </pre>
		 */
		TILE_LOAD_FINISHED
	}

    /**
     * Retrieves a tile from the cache if present, otherwise a new
     * tile cache will be created.
     * 
     * @param source
     * @param x
     *            tile number on the x axis of the tile to be retrieved
     * @param y
     *            tile number on the y axis of the tile to be retrieved
     * @param z
     *            zoom level of the tile to be retrieved
     * @return the requested tile or null if it could not be found
     *  TODO asynchronous notification / event listener 
     * 
     */
    public Tile getTile(TileSource source, int x, int y, int z);
    
	public void addTileLoaderListener(TileLoaderListener tileLoaderListener);
	public void removeTileLoaderListener(TileLoaderListener tileLoaderListener);

	public void setUseOneHttpConnection(boolean defaltUseHttpConnection);
	public boolean isUseOneHttpConnection();
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener);
	public void removePropertyChangeListener(PropertyChangeListener listener);

	public void reloadFromServer(Tile tile);
	
}
