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
import java.beans.PropertyChangeSupport;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.worldmap.layers.map.Tile.TileState;

/**
 * A cached tile loader.
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */
public class DefaultTileLoader implements TileLoader {
	public static String USER_AGENT = "Mozilla/5.0";
	public static String ACCEPT = "text/html, image/png, image/jpeg, image/gif, */*";
	private final String cacheDir;
	private TileLoaderListener tileLoaderListener;
	private final int cacheSize = 220;
	private final ScheduledExecutorService bossExecutor ;
	private final ExecutorService downloadExecutor ;
	private boolean useOneHttpConnection = true;
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	public boolean isUseOneHttpConnection() {
		return useOneHttpConnection;
	}

	public void setUseOneHttpConnection(boolean useOneHttpConnection) {
		this.useOneHttpConnection = useOneHttpConnection;
	}

	/**
	 * Tiles are stored in this map.
	 * 
	 */
	private final Map<Long, Tile> tileMap = new ConcurrentHashMap<Long, Tile>();
	private final Deque<Long> tileLifo = new ArrayDeque<Long>();
	/**
	 * Input queue used for processing tiles. A background processes get tiles
	 * from this queue and load them either from file or tile server.
	 */
	private final Queue<Tile> tileQueue = new ConcurrentLinkedQueue<Tile>();

	public DefaultTileLoader(ScheduledExecutorService bossExecutor, ExecutorService downloadExecutor, String cacheDir) {
		this.bossExecutor = bossExecutor;
		this.downloadExecutor = downloadExecutor;
		this.cacheDir = cacheDir;
		bossExecutor.scheduleAtFixedRate(new Runnable() {
			public void run() {
				handleCacheSize();
			}
		}, 2, 5, TimeUnit.SECONDS);

	}

	private static final String DS = File.separator;

	private String tileFilename(Tile tile) {
        return cacheDir + DS + tile.getDirname()+DS+tile.getFilename();
	}

	/**
	 * Creates tile directory if it does not exist
	 * 
	 * @param tile
	 */
	private void createTileCacheDir(Tile tile) {
		String dir = cacheDir + DS + tile.getDirname();
        System.out.println("CacheDir: " + dir);
		File file = new File(dir);
		if (!file.isDirectory()) {
			logger.debug("trying to create tile cache dir: " + file);
			if(!file.mkdirs()) {
				logger.warn("failed to create cachedir: " + file);
			}
		}
	}

	private void loadTileFromFile(Tile tile, File file) {
		try {
			InputStream fin = new FileInputStream(file);
			tile.loadImage(fin);
			fin.close();
			tile.getState().set(TileState.LOADED);
		} catch (Exception e) {
			tile.getState().set(TileState.FAILED);
			logger.debug(e.toString());
		}
	}

	/**
	 * Load tile from server. It actually first downloads tile from server to a
	 * file and loads the file.
	 * 
	 * @param tile
	 * @param file
	 */
	private void  loadTileFromServer(Tile tile, File file) {
		pcs.firePropertyChange(Events.TILE_LOAD_STARTED+"", null, tile);
		HttpURLConnection conn = null;
		try {
			createTileCacheDir(tile);
			conn = (HttpURLConnection) tile.getUrl().openConnection();
			setupHttpConn(conn);
			InputStream is = conn.getInputStream();
			FileOutputStream fos = new FileOutputStream(file);
			BufferedInputStream in = new BufferedInputStream(is);
			OutputStream out = new BufferedOutputStream(fos);
			byte[] buf = new byte[1024];
			int n = 0;
			while ((n = in.read(buf)) >= 0) {
				out.write(buf, 0, n);
			}
			out.flush();
			out.close();
			// load tile from file
			loadTileFromFile(tile, file);
			is.close();
			logger.debug("loaded from server: "+tile);
		} catch (IOException e) {
			logger.debug(e + "");
			if (conn != null) {
				try {
					int respCode = ((HttpURLConnection) conn).getResponseCode();
					InputStream es = ((HttpURLConnection) conn)
					.getErrorStream();
					logger.warn(conn + " returned: " + respCode);
					// read the response body
					byte[] buf = new byte[256];
					while ((es.read(buf)) > 0) {
						// do nothing, just read
					}
					// close the error stream
					es.close();
				} catch (IOException ex) {
					logger.debug(ex + "");
				}
			}
		} finally {
			pcs.firePropertyChange(Events.TILE_LOAD_FINISHED+"", null, tile);
		}
	}

	private void processQueue() {
		Tile tile = null;
		do {
			tile = tileQueue.poll();
			if (tile != null) {
				if (tile.getState().compareAndSet(TileState.NEW, TileState.LOADING)) {
					final File file = new File(tileFilename(tile));
					if (file.canRead()) {
						loadTileFromFile(tile, file);
					} else {
						final Tile targetTile = tile;
						downloadExecutor.execute(new Runnable() {
							@Override
							public void run() {
								if( useOneHttpConnection ){
									logger.debug("using only one connection");
									synchronized (DefaultTileLoader.this) {
										loadTileFromServer(targetTile, file);		
									}
								} else {
									loadTileFromServer(targetTile, file);		
								}
								if (tileLoaderListener != null && targetTile.getState().get()==TileState.LOADED) {
									tileLoaderListener.tileLoadingFinished(targetTile);
								}
							}
						});

					}
				}
			}
			// TODO process when tile not loaded,
			if (tileLoaderListener != null && tile.getState().get()==TileState.LOADED) {
				tileLoaderListener.tileLoadingFinished(tile);
			}
		} while (tile != null);
	}

	Logger logger = LoggerFactory.getLogger(this.getClass());

	private void setupHttpConn(HttpURLConnection conn) {
		conn.setRequestProperty("User-agent", USER_AGENT);
		conn.setRequestProperty("Accept", ACCEPT);
	}
	/**
	 * Deletes unused entries from memory cache
	 */
	private void handleCacheSize() {
		// handling cache size
		int low = cacheSize / 2;
		while (tileMap.size() > low) {
			final Long id = tileLifo.pollLast();
			if (id != null) {
				tileMap.remove(id);
			}
		}
	}

	public Tile getTile(TileSource source, int x, int y, int z) {
//		 logger.debug("map size: "+tileMap.size()+"  lifo size: "+tileLifo.size());
		Tile tile = tileMap.get(source.getTileId(x, y, z));
		if (tile == null) {
			try {
				tile = new DefaultTile(source, x, y, z);
			} catch (MalformedURLException e) {
				// TODO generate an asynchronous notification so
				// someone can help to fix the problem
				logger.error("Could not get tile");
				return null;
			}
			final long id = tile.getId();
			tileMap.put(id, tile);
			tileQueue.offer(tile);
			tileLifo.offerFirst(id);
			bossExecutor.execute(new Runnable() {
				public void run() {
					processQueue();
				}
			});
		} else {
			if (tileLifo.removeLastOccurrence(tile.getId())) {
				tileLifo.offerFirst(tile.getId());
			}
		}
		return tile;
	}

	public void addTileLoaderListener(TileLoaderListener tileLoaderListener) {
		this.tileLoaderListener = tileLoaderListener;
	}

	public void removeTileLoaderListener(TileLoaderListener tileLoaderListener) {
		this.tileLoaderListener = null;
	}

	@Override
	public void addPropertyChangeListener(String propertyName,
			PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(propertyName,listener);
	}
	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener){
		pcs.removePropertyChangeListener(listener);
	}

	@Override
	public void reloadFromServer(final Tile tile) {
		final File file= new File(tileFilename(tile));
		tile.getState().set(TileState.LOADING);
		downloadExecutor.execute(new Runnable(){
			@Override
			public void run() {
				if( useOneHttpConnection ){
					logger.debug("using only one connection");
					synchronized (DefaultTileLoader.this) {
						loadTileFromServer(tile, file);		
					}
				} else {
					loadTileFromServer(tile, file);		
				}
				if (tileLoaderListener != null && tile.getState().get()==TileState.LOADED) {
					tileLoaderListener.tileLoadingFinished(tile);
				}
			}
		});
	}

}
