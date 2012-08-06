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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.geo.OsmMercator;
import de.fhg.fokus.net.geo.WayPoint;
import de.fhg.fokus.net.worldmap.layers.map.Tile.TileState;
import de.fhg.fokus.net.worldmap.model.PredefinedLayer;
import de.fhg.fokus.net.worldmap.model.WayPointScreenLocator;
import de.fhg.fokus.net.worldmap.view.ViewUtil;

/**
 *  Map layer based on jmapviewer
 *  
 * @author Jan Peter Stotz (original author of jmapviewer)
 * 
 * @author FhG-FOKUS NETwork Research
 * TODO panning optimization 
 *
 */
public class MapLayer extends PredefinedLayer implements WayPointScreenLocator, TileLoaderListener {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final Preferences prefs = Preferences.userNodeForPackage(getClass());
	private final Deque<Tile> tileRefreshQueue = new LinkedList<Tile>();
	public static class MapState {
		public Point center = new Point();
		public int zoom = 0;
	}
	
	public static enum MapAction {
		STILL,
		ZOOM_IN_START,
		ZOOM_IN_FINISHED,
		PANNING_START,
		PANNING_FINISHED,
		JUMP_START,
		JUMP_FINISHED
	}
	private final AtomicReference<MapAction> action = new AtomicReference<MapAction>();
	public static enum Events {
		STILL,
		ZOOM_IN,
		ZOOM_OUT,
		/**
		 * Zoom or panning (or both) values have change.
		 * <pre>
		 * oldValue = null
		 * newValue = current MapState
		 * </pre>
		 */
		VIEW_CHANGED,
		JUMPING
	}


	public MapLayer(TileLoader tileLoader, TileSource tileSource) {
		super("Map");
		
		this.tileLoader = tileLoader;
		this.tileSource = tileSource;
		this.tileLoader.addTileLoaderListener(this);
		action.set(MapAction.STILL);
		setBackground(Color.LIGHT_GRAY);
		// == initialize back image ==
		GraphicsEnvironment gEnv = GraphicsEnvironment
		.getLocalGraphicsEnvironment();
		Rectangle bounds = gEnv.getMaximumWindowBounds();
		GraphicsConfiguration gConf = gEnv.getDefaultScreenDevice()
		.getDefaultConfiguration();
		this.cacheImage = gConf.createCompatibleImage(bounds.width,
				bounds.height);
		// paint it white
		Graphics g =this.cacheImage.getGraphics();
		g.setColor(Color.white);
		g.fillRect(0, 0, bounds.width, bounds.height);
		// 
		// reading loading.png image
		//
		try {
			this.loadingImage = ViewUtil.toCompatibleImage(ImageIO.read(this.getClass().getResource("resources/loading.png")));
		} catch (IOException e) {
			logger.debug(e.getMessage());
		}
		//
		// The following component listener is used to force redrawing the map.
		//
		addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent e) {
				forceMapRedraw=true;
				repaint();
			}
			@Override
			public void componentResized(ComponentEvent e) {
				forceMapRedraw=true;
				repaint();
			}
			@Override
			public void componentMoved(ComponentEvent e) {
				forceMapRedraw=true;
				repaint();
			}
			@Override
			public void componentHidden(ComponentEvent e) {
				forceMapRedraw=true;
				repaint();
			}
		});
		tileLoader.addPropertyChangeListener(de.fhg.fokus.net.worldmap.layers.map.TileLoader.Events.TILE_LOAD_STARTED+"", 
				new PropertyChangeListener() {
					
					@Override
					public void propertyChange(PropertyChangeEvent arg0) {
						forceMapRedraw=true;
						repaint();
						
					}
				});

	}
	private static final long serialVersionUID = 412096959993562763L;
	/**
	 * x- and y-position of the center of this map-panel on the world map
	 * denoted in screen pixel regarding the current zoom level.
	 */
	private final MapState state = new MapState();
//	private final Point center = new Point();
	private final Point lastCenter = new Point();


	private int lastZoom;

	/**
	 * Vectors for clock-wise tile painting
	 */
	protected static final Point[] move = { new Point(1, 0), new Point(0, 1),
		new Point(-1, 0), new Point(0, -1) };

	public static final int MAX_ZOOM = 22;
	public static final int MIN_ZOOM = 0;


	private TileLoader tileLoader;
	private TileSource tileSource;
	private boolean forceMapRedraw = false;


	private boolean tileGridVisible = false;
	/**
	 * Preference keys that are saved MapLayer is finalized.
	 */
	private static enum PrefKeys {
		USE_ONE_HTTP_CONNECTION,
		ZOOM,
		CENTER_X,
		CENTER_Y,
		TILE_GRID_VISIBLE,
		MAPLAYER_VISIBLE,
	}



	private final BufferedImage cacheImage;
	private BufferedImage loadingImage;
	@Override
	public void setXYFromWaypoint( Point p, WayPoint waypoint  ){
		p.x = OsmMercator.LonToX(waypoint.getLongitude(), state.zoom);
		p.y = OsmMercator.LatToY(waypoint.getLatitude(), state.zoom);
		p.x -= state.center.x - getWidth() / 2;
		p.y -= state.center.y - getHeight() / 2;
//		if (x < 0 || y < 0 || x > getWidth() || y > getHeight())
//			return null;
	}
	@Override
	public void setXYFromWaypoint2D(Point p, Double waypoint) {
		p.x = OsmMercator.LonToX(waypoint.x, state.zoom);
		p.y = OsmMercator.LatToY(waypoint.y, state.zoom);
		p.x -= state.center.x - getWidth() / 2;
		p.y -= state.center.y - getHeight() / 2;
		
	}
	final float[] DASH_PATTERN = { 8f };
	final Stroke tileGridStroke = new BasicStroke(1f,BasicStroke.CAP_SQUARE,BasicStroke.CAP_SQUARE,4f,DASH_PATTERN,2f);
	private Color tileGridColor = Color.LIGHT_GRAY;
	private void drawMap(Graphics g) {
		//		logger.debug("== draw map == ");
		int iMove = 0;
		int tilex = state.center.x / Tile.SIZE;
		int tiley = state.center.y / Tile.SIZE;
		int off_x = (state.center.x % Tile.SIZE);
		int off_y = (state.center.y % Tile.SIZE);

		int w2 = getWidth() / 2;
		int h2 = getHeight() / 2;
		int posx = w2 - off_x;
		int posy = h2 - off_y;

		int diff_left = off_x;
		int diff_right = Tile.SIZE - off_x;
		int diff_top = off_y;
		int diff_bottom = Tile.SIZE - off_y;

		boolean start_left = diff_left < diff_right;
		boolean start_top = diff_top < diff_bottom;

		if (start_top) {
			if (start_left)
				iMove = 2;
			else
				iMove = 3;
		} else {
			if (start_left)
				iMove = 1;
			else
				iMove = 0;
		} // calculate the visibility borders
		int x_min = -Tile.SIZE;
		int y_min = -Tile.SIZE;
		int x_max = getWidth();
		int y_max = getHeight();

		// paint the tiles in a spiral, starting from center of the map
		boolean painted = true;
		int x = 0;
		while (painted) {
			painted = false;
			for (int i = 0; i < 4; i++) {
				if (i % 2 == 0) {
					x++;
				}
				for (int j = 0; j < x; j++) {
					if (x_min <= posx && posx <= x_max && y_min <= posy
							&& posy <= y_max) {
						int max = (1 << state.zoom);
						if (tilex < 0 || tilex >= max || tiley < 0
								|| tiley >= max) {
							continue;
						} else {
							// tile is visible
							Tile tile = tileLoader.getTile(tileSource, tilex,
									tiley, state.zoom);
							//							logger.debug(tile.getId()+"");
							registerTileForRefresh(tile);
							BufferedImage image = null;
							if( tile.getState().get()==TileState.LOADED ){
								image = tile.getImage();
							}
							if (image != null){
								g.drawImage(image, posx, posy, null);
							} else if(loadingImage!=null) {
								g.drawImage(loadingImage, posx, posy, null);
							}
							if (tileGridVisible) {
								Graphics2D g2 = (Graphics2D) g;
								g2.setStroke(tileGridStroke);
								g.setColor(tileGridColor);
								g.drawRect(posx, posy, Tile.SIZE, Tile.SIZE);
							}
							painted = true;
						}
					}
					Point p = move[iMove];
					posx += p.x * Tile.SIZE;
					posy += p.y * Tile.SIZE;
					tilex += p.x;
					tiley += p.y;
				}
				iMove = (iMove + 1) % move.length;
			}
		}
		// outer border of the map
		int mapSize = Tile.SIZE << state.zoom;
		g.drawRect(w2 - state.center.x, h2 - state.center.y, mapSize, mapSize);
		lastCenter.x = state.center.x;
		lastCenter.y = state.center.y;
		lastZoom = state.zoom;
	}

	private final int N_REFRESH_TILES = 12;
	// FIXME load currently viewed tiles rather than the last viewed tiles
	private synchronized void registerTileForRefresh(Tile tile) {
		if(tileRefreshQueue.size()< N_REFRESH_TILES){
			tileRefreshQueue.offer(tile);
		} else {
			tileRefreshQueue.pollFirst();
		}
	}

	public boolean isTileGridVisible() {
		return tileGridVisible;
	}


	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if(hideMap){
			g.setColor(getBackground());
			g.fillRect(getX(), getY(), getWidth(), getHeight());
			return;
		}
		if (state.zoom>2 ){ 
			// draw only if underlying map has changed
			if(  forceMapRedraw || state.zoom != lastZoom || !state.center.equals(lastCenter) ){
				forceMapRedraw=false;
				Graphics big = cacheImage.getGraphics();
				drawMap(big);
				
			}
			g.drawImage(cacheImage, getX(), getY(), getX() + getWidth(), getY()
					+ getHeight(), getX(), getY(), getX() + getWidth(), getY()
					+ getHeight(), null);
		} else {
			drawMap(g);
		}
	}
	public void setDisplayPosition(int x, int y, int zoom) {
		setDisplayPosition(new Point(getWidth() / 2, getHeight() / 2), x, y,
				zoom);
	}
	public void setDisplayPosition(Point mapPoint, int x, int y, int zoom) {
		if (zoom > tileSource.getMaxZoom() || zoom < MIN_ZOOM)
			return;
		lastCenter.x = state.center.x;
		lastCenter.y = state.center.y;
		
		// Get the plain tile number
		state.center.x = x - mapPoint.x + getWidth() / 2;
		state.center.y = y - mapPoint.y + getHeight() / 2;

		if(!state.center.equals(lastCenter)){
			firePropertyChange(Events.VIEW_CHANGED+"", null,state);
		}
		setIgnoreRepaint(true);
		try {
			int oldZoom = this.state.zoom;
			this.state.zoom = zoom;
			if( this.state.zoom > oldZoom ){
				firePropertyChange(Events.ZOOM_IN+"", oldZoom, this.state.zoom);
				firePropertyChange(Events.VIEW_CHANGED+"", null, null);
			}
			if( this.state.zoom<oldZoom ){
				firePropertyChange(Events.ZOOM_OUT+"", oldZoom, this.state.zoom);
				firePropertyChange(Events.VIEW_CHANGED+"", null, null);
			}
		} finally {
			setIgnoreRepaint(false);
			repaint();
		}
	}


	@Override
	public void loadPreferences() {
		int x = prefs.getInt(PrefKeys.CENTER_X+"", 2264);
		int y = prefs.getInt(PrefKeys.CENTER_Y+"", 1441);
		int zoom = prefs.getInt(PrefKeys.ZOOM+"", 4);
		this.hideMap = !prefs.getBoolean(PrefKeys.MAPLAYER_VISIBLE+"", true);
		setDisplayPosition(x,y, zoom);
		boolean tileGridVisible = prefs.getBoolean(PrefKeys.TILE_GRID_VISIBLE+"", false);
		this.tileGridVisible=tileGridVisible;
		this.tileLoader.setUseOneHttpConnection(prefs.getBoolean(PrefKeys.USE_ONE_HTTP_CONNECTION+"", false));

	}

	@Override
	public void savePreferences() {
		prefs.putBoolean(PrefKeys.USE_ONE_HTTP_CONNECTION+"", tileLoader.isUseOneHttpConnection());
		prefs.putInt(PrefKeys.CENTER_X+"",state.center.x);
		prefs.putInt(PrefKeys.CENTER_Y+"",state.center.y);
		prefs.putInt(PrefKeys.ZOOM+"",state.zoom);
		prefs.putBoolean(PrefKeys.TILE_GRID_VISIBLE+"", tileGridVisible);
		prefs.putBoolean(PrefKeys.MAPLAYER_VISIBLE+"", !hideMap);

	}
	public synchronized void addEventListener(Events event, PropertyChangeListener listener) {
		addPropertyChangeListener(event+"",listener);
	}
	public void removeEventListener(PropertyChangeListener listener) {
		removePropertyChangeListener(listener);
	}

	/**
	 * Moves the visible map pane.
	 * 
	 * @param x
	 *            horizontal movement in pixel.
	 * @param y
	 *            vertical movement in pixel
	 */
	public void moveMap(int x, int y) {
		state.center.x += x;
		state.center.y += y;
		repaint();
		firePropertyChange(Events.VIEW_CHANGED+"", null, null);
	}
	/**
	 * Changes the map pane so that it is centered on the specified coordinate
	 * at the given zoom level.
	 * 
	 * @param lat
	 *            latitude of the specified coordinate
	 * @param lon
	 *            longitude of the specified coordinate
	 * @param zoom
	 *            {@link #MIN_ZOOM} <= zoom level <= {@link #MAX_ZOOM}
	 */
	public void setDisplayPositionByLatLon(double lat, double lon, int zoom) {
		setDisplayPositionByLatLon(new Point(getWidth() / 2, getHeight() / 2),
				lat, lon, zoom);
	}

	/**
	 * Changes the map pane so that the specified coordinate at the given zoom
	 * level is displayed on the map at the screen coordinate
	 * <code>mapPoint</code>.
	 * 
	 * @param mapPoint
	 *            point on the map denoted in pixels where the coordinate should
	 *            be set
	 * @param lat
	 *            latitude of the specified coordinate
	 * @param lon
	 *            longitude of the specified coordinate
	 * @param zoom
	 *            {@link #MIN_ZOOM} <= zoom level <=
	 *            {@link TileSource#getMaxZoom()}
	 */
	public void setDisplayPositionByLatLon(Point mapPoint, double lat,
			double lon, int zoom) {
		int x = OsmMercator.LonToX(lon, zoom);
		int y = OsmMercator.LatToY(lat, zoom);
		setDisplayPosition(mapPoint, x, y, zoom);
	}
	/**
	 * Sets the displayed map pane and zoom level so that all map markers are
	 * visible.
	 */
	public Point2D.Double getPosition() {
		double lon = OsmMercator.XToLon(state.center.x, state.zoom);
		double lat = OsmMercator.YToLat(state.center.y, state.zoom);
		return new Point2D.Double(lat, lon);
	}

	public Point2D.Double getPosition(Point mapPoint) {
		int x = state.center.x + mapPoint.x - getWidth() / 2;
		int y = state.center.y + mapPoint.y - getHeight() / 2;
		double lon = OsmMercator.XToLon(x, state.zoom);
		double lat = OsmMercator.YToLat(y, state.zoom);
		return new Point2D.Double(lat, lon);
	}


	public void setZoom(int zoom, Point mapPoint) {
		if (zoom > tileSource.getMaxZoom() || zoom < tileSource.getMinZoom()
				|| zoom == this.state.zoom){
			return;
		}

		Point2D.Double zoomPos = getPosition(mapPoint);

		// requests
		setDisplayPositionByLatLon(mapPoint, zoomPos.x, zoomPos.y, zoom);
		forceMapRedraw=true;
	}

	public void setZoom(int zoom) {
		setZoom(zoom, new Point(getWidth() / 2, getHeight() / 2));
	}

	/**
	 * Increases the current zoom level by one
	 */
	public void zoomIn() {
		setZoom(state.zoom + 1);
	}
	public void zoomOut() {
		setZoom(state.zoom - 1);
	}


	/**
	 * Increases the current zoom level by one
	 */
	public void zoomIn(Point mapPoint) {
		setZoom(state.zoom + 1, mapPoint);
	}

	/**
	 * @return the current zoom level
	 */
	public int getZoom() {
		return state.zoom;
	}

	public void setTileSource(TileSource tileSource) {
		if (tileSource.getMaxZoom() > MAX_ZOOM)
			throw new RuntimeException("Maximum zoom level too high");
		if (tileSource.getMinZoom() < MIN_ZOOM)
			throw new RuntimeException("Minumim zoom level too low");
		this.tileSource = tileSource;
		if (state.zoom > tileSource.getMaxZoom())
			setZoom(tileSource.getMaxZoom());
		forceMapRedraw=true;
		repaint();
	}

	public void setTileGridVisible(boolean tileGridVisible) {
		this.tileGridVisible = tileGridVisible;
		forceMapRedraw=true;
		repaint();
	}

	@Override
	public void tileLoadingFinished(Tile tile) {
		if (tile.getState().get() == TileState.LOADED) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					forceMapRedraw=true;
					repaint();
				}
			});
		}
	}
	private boolean hideMap = false;
	public void hideMap(boolean hidden) {
		hideMap = hidden;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				forceMapRedraw=true;
				repaint();
			}
		});
	}
	public boolean isHidden() {
		return hideMap;
	}

	public TileLoader getTileLoader() {
		return tileLoader;
	}
	public synchronized void  reloadMap(){
		for(Tile tile: tileRefreshQueue){
			tile.getState().set(TileState.LOADING);
		}

		for(Tile tile: tileRefreshQueue){
			tileLoader.reloadFromServer(tile);
		}

	}
    
	@Override
	public void setWaypointFromXY(WayPoint waypoint, Point p) {
		int x = state.center.x + p.x - getWidth() / 2;
		int y = state.center.y + p.y - getHeight() / 2;
		waypoint.set(OsmMercator.YToLat(y, state.zoom), OsmMercator.XToLon(x, state.zoom));
	
	}

	@Override
	public void addViewChangeListener(PropertyChangeListener listener) {
		addEventListener(Events.VIEW_CHANGED, listener);
	}


}
