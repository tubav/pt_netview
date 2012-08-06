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

package de.fhg.fokus.net.worldmap.layers.track;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import de.fhg.fokus.net.worldmap.util.EventSupport;

/**
 * Models a track player
 * @author FhG-FOKUS NETwork Research
 *
 */
public interface TrackPlayer {
	public interface TrackSource {
		/**
		 * Get tracks
		 * @param start start in unix milliseconds
		 * @param stop in unix milliseconds
		 * 
		 */
		public Set<Track> getTracks(long startTs, long stopTs );
        
		/**
		 * A track source should support caching tracks, which is triggered
		 * by this method. Subsequent getTracks should return cached values.
		 * 
		 * @param startTs
		 * @param stopTs
		 */
		public void preloadTracks( long startTs, long stopTs);
		public long getNumberOfTracks(long startTs, long stopTs);
		public TimeUnit getTimestampUnit();
		public int numberOfPreloadedTracks();
		public void refresh();
		
		/* find next/previous track. timestamps are in milliseconds */
		public long nextTrackStart(long ts);
		public long previousTrackStart(long ts);
	}
	
	public static final class EventData {
		public long startTs;
		public long stopTs;
		public long currentTs;
		public long numberOfTracks;
		public EventData(long startTs, long stopTs) {
			this.startTs = startTs;
			this.stopTs = stopTs;
		}
		public EventData(long startTs, long stopTs, long currentTs, long numberOfTracks) {
			super();
			this.startTs = startTs;
			this.stopTs = stopTs;
			this.currentTs = currentTs;
			this.numberOfTracks = numberOfTracks;
		}
	}
	public static enum State {
		INIT,
		STOPPED,
		PAUSED,
		PLAYING
	}
	/**
	 * Track player settings
	 */
	public interface Settings {
		
	}
	/**
	 * Initialize component
	 */
	public void init();
	/**
	 * Start component
	 */
	public void start();
	public void stop();
	public void addEventListener( State state, EventSupport.EventListener<EventData> lsn );
	public void removeEventListener( EventSupport.EventListener<EventData> lsn );
	
	public TrackPlayerPanel getPanel();
	
	/**
	 * Start playing tracks using registered settings
	 */
	public void playStart();
	public void playStop();
	public void updateStatus();
	public long getStartTimestamp();
	public long getStopTimestamp();
	public long getCurrentTimestamp();
	public void reloadCurrentTracks();
	public TrackPlayer.State getState();
	
	
}
