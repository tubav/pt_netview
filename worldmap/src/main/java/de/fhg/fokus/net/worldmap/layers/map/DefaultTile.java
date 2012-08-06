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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

import de.fhg.fokus.net.worldmap.view.ViewUtil;


/**
 * 
 * @author FhG-FOKUS NETwork Research
 *
 */
public final class DefaultTile implements Tile {
	private final TileSource source;
	private final int xtile;
	private final int ytile;
	private final int zoom;
	private BufferedImage image;
	
	public BufferedImage getImage() {
		return image;
	}
	private final URL url;
	private final long id;
	public final AtomicReference<TileState> state = new AtomicReference<TileState>();
	private static final String DS = File.separator;
	

	public DefaultTile(TileSource source, int xtile, int ytile, int zoom) throws MalformedURLException {
		super();
		this.source = source;
		this.xtile = xtile;
		this.ytile = ytile;
		this.zoom = zoom;
		this.url=new URL(source.getTileUrl(xtile,ytile,zoom));
		this.state.set(TileState.NEW);
		this.id = source.getTileId(xtile, ytile, zoom);
				
	}


	@Override
	public String getFilename() {
		return  ytile + "." + source.getTileType();
	}
	@Override
	public String getDirname() {
		return source.getName() + DS + zoom
		+ DS + xtile ;
	}
	@Override
	public void loadImage(InputStream input) throws IOException {
		image =  ViewUtil.toCompatibleImage(ImageIO.read(input));
		
	}

	@Override
	public URL getUrl() {
		return url;
	}

	@Override
	public AtomicReference<TileState> getState() {
		return state;
	}
	public long getId() {
		return id;
	}
	@Override
	public String toString() {
		return getUrl().toString();
	}


	@Override
	public int compareTo(Tile tile) {
		return (int)(id-tile.getId());
	}
}
