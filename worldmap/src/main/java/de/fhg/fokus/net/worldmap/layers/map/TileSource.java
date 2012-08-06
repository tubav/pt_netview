// License: GPL. Copyright 2008 by Jan Peter Stotz

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

/**
 * 
 * @author Jan Peter Stotz (original)
 */
public interface TileSource {


    /**
     * Specifies the maximum zoom value. The number of zoom levels is [0..
     * {@link #getMaxZoom()}].
     * 
     * @return maximum zoom value that has to be smaller or equal to
     *         {@link JMapViewer#MAX_ZOOM}
     */
    public int getMaxZoom();

    /**
     * Specifies the minimum zoom value. This value is usually 0. 
     * Only for maps that cover a certain region up to a limited zoom level 
     * this method should return a value different than 0.  
     * 
     * @return minimum zoom value - usually 0
     */
    public int getMinZoom();

    /**
     * A tile layer name has to be unique and has to consist only of characters
     * valid for filenames.
     * 
     * @return Name of the tile layer
     */
    public String getName();

    /**
     * Constructs the tile url.
     * 
     * @param zoom
     * @param tilex
     * @param tiley
     * @return fully qualified url for downloading the specified tile image
     */
    public String getTileUrl(int xtile, int ytile, int zoom );
    /**
     * Specifies the tile image type. For tiles rendered by Mapnik or
     * Osmarenderer this is usually <code>"png"</code>.
     * 
     * @return file extension of the tile image type
     */
    /**
     * A unique tile id used for caching. Use this instead of URL to avoid wasting time and memory with strings.
     */
    public long getTileId( int xtile, int ytile, int zoom );
    
    public String getTileType();
}
