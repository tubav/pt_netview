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
 * @author OpenStreetMap (original)
 * @author FhG-FOKUS NETwork Research
 *
 */
public class OsmTileSource {
	protected static abstract class AbstractOsmTileSource implements TileSource {
		protected final String name;
		protected final String baseUrl;
		protected long srcId;
		public AbstractOsmTileSource(String name, String baseUrl) {
			super();
			this.name = name;
			this.baseUrl = baseUrl;
			this.srcId=hashCode();
		}
		public int getMaxZoom() {
			return 100;
		}

		public int getMinZoom() {
			return 0;
		}
		public String getTileFilename(int zoom, int tilex, int tiley ){
			return "/" + zoom + "/" + tilex + "/" + tiley + ".png";
		}
		public String getTileUrl(int tilex, int tiley,int zoom ) {
			return "/" + zoom + "/" + tilex + "/" + tiley + ".png";
		}
		@Override
		public String toString() {
			return getName();
		}
		/**
		 * Generate a hopefully unique (and fast) id for a tile;
		 * <pre>
		 * 0x F FFF FFFF FFFF FF FF
		 *    | \                +-- zoom
		 *    |  tilex (26 bits)   
		 *    |  
		 *    |        tiley (26 bits) 
		 *    |
		 *    +-- src id                  
		 * </pre>
		 * 
		 */
		@Override
		public long getTileId(int xtile, int ytile, int zoom) {
			return (srcId<<60)|((long)xtile<<34)|((long)ytile<<8)|zoom; 
		}
		public String getTileType() {
			return "png";
		}
		public String getName(){
			return name;
		}
	}
	public static class Mapnik extends AbstractOsmTileSource {
		public Mapnik() {
			super("Mapnik","http://a.tile.openstreetmap.org");
			this.srcId=1;
		}
		private static final String PATTERN = "http://%s.tile.openstreetmap.org/%d/%d/%d.png";
		private static final String[] SERVER = { "a", "b", "c" };
		private int SERVER_NUM = 0;

		@Override
		public String getTileUrl(int tilex, int tiley ,int zoom ) {
			String url = String.format(PATTERN, new Object[] { SERVER[SERVER_NUM], zoom, tilex, tiley });
			SERVER_NUM = (SERVER_NUM + 1) % SERVER.length;
			return url;
		}


	}


	public static class Localhost extends AbstractOsmTileSource {
		public Localhost() {
			super("Localhost","http://localhost/tiles/Mapnik");
		}

		@Override
		public String getTileUrl(int tilex, int tiley, int zoom  ) {
			return baseUrl + super.getTileUrl(tilex, tiley, zoom );
		}
	}

	public static class OpenStreetBrowser extends AbstractOsmTileSource {
		public OpenStreetBrowser() {
			super("OpenStreetBrowser","http://www.openstreetbrowser.org/tiles/base");
			this.srcId=2;
		}
		@Override
		public String getTileUrl( int tilex, int tiley, int zoom) {
			return baseUrl + super.getTileUrl(tilex, tiley, zoom );
		}
	}

	public static class CycleMap extends AbstractOsmTileSource {
		public CycleMap() {
			super("OSM Cycle Map","");
			this.srcId=3;
		}
		private static final String PATTERN = "http://%s.andy.sandbox.cloudmade.com/tiles/cycle/%d/%d/%d.png";
		private static final String[] SERVER = { "a", "b", "c" };
		private int SERVER_NUM = 0;

		@Override
		public String getTileUrl(int tilex, int tiley, int zoom) {
			String url = String.format(PATTERN, new Object[] { SERVER[SERVER_NUM], zoom, tilex, tiley });
			SERVER_NUM = (SERVER_NUM + 1) % SERVER.length;
			return url;
		}

		public int getMaxZoom() {
			return 17;
		}

	}

	public static class TilesAtHome extends AbstractOsmTileSource {
		public TilesAtHome() {
			super("TilesAtHome","http://tah.openstreetmap.org/Tiles/tile");
			this.srcId=4;
		}
		public int getMaxZoom() {
			return 17;
		}
		@Override
		public String getTileUrl(int tilex, int tiley, int zoom ) {
			return baseUrl + super.getTileUrl(tilex, tiley, zoom);
		}
	}
}
