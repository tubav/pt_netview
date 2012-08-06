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

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.prefs.Preferences;

import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.worldmap.model.PersistentPreferences;
import de.fhg.fokus.net.worldmap.util.EventSupport;
import de.fhg.fokus.net.worldmap.util.EventSupport.EventListener;
import de.fhg.fokus.net.worldmap.util.TimestampSelectionDialog;
import de.fhg.fokus.net.worldmap.util.TimestampSelectionDialog.TimestampListener;

/**
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */
public class DefaultTrackPlayer implements TrackPlayer, PersistentPreferences {
	// sys
	private final Logger logger = LoggerFactory.getLogger(getClass());
	private final Preferences prefs = Preferences
	.userNodeForPackage(getClass());
	private final EventSupport<TrackPlayer.State, TrackPlayer.EventData> eventSupport;

	private static enum PrefKeys {
		START_TS, STOP_TS
	}

	// utils
	private static SimpleDateFormat sdfIso8601s = new SimpleDateFormat(
	"yyyy-MM-dd  HH:mm:ss.S");
	private static SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");
	private static SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");

	// view
	private final TimestampSelectionDialog tsDialog;
	private final Frame frame;
	private final TrackPlayerPanel panel;
	private final TrackPlayerSettingsDialog settingsDialog;
	// properties
	private final SplineLayer layer;
	private final ScheduledExecutorService scheduler;
	private final ExecutorService executor;
	private final TrackPlayer.TrackSource trackSource;

	private TrackPlayer.State state = State.STOPPED;
	private Set<Track> currentTracks = new ConcurrentSkipListSet<Track>();
	
	/**
	 * Slider resolution in milliseconds
	 * 
	 */
	private long sliderResolution = 1000;
	private long currentTs;
	private long startTs;
	private long stopTs;

	public DefaultTrackPlayer(TrackPlayerPanel panel, SplineLayer layer,
			TrackPlayer.TrackSource trackSource,
			ScheduledExecutorService scheduler) {
		this.panel = panel;
		this.layer = layer;
		this.trackSource = trackSource;
		this.scheduler = scheduler;
		this.frame = (Frame) SwingUtilities.getRoot(panel);
		this.tsDialog = new TimestampSelectionDialog(frame, false);
		this.settingsDialog = new TrackPlayerSettingsDialog(frame, false);
		this.eventSupport = new EventSupport<State, EventData>(scheduler);
		// TODO review
		this.executor = scheduler;

		setup();
	}

	private void updateStartLabels() {
		Date date = new Date(startTs);
		panel.getjLabelStartTime().setText(sdfTime.format(date));
		panel.getjLabelStartDate().setText(sdfDate.format(date));
		// TODO review
		// if(startTs> stopTs){
		// stopTs = startTs + 1000;
		// updateStopLabels();
		// }
	}

	private void updateStopLabels() {
		Date date = new Date(stopTs);
		panel.getjLabelStopTime().setText(sdfTime.format(date));
		panel.getjLabelStopDate().setText(sdfDate.format(date));
		// TODO review
		// if(startTs > stopTs ){
		// startTs = stopTs - 1000;
		// updateStartLabels();
		// }

	}

	private Timer autoplayTimer = new Timer();
	private TimerTask autoplayTask;

	private void setupTimestampLabels() {
		//
		//
		//

		final TimestampListener startTsLsn = new TimestampListener() {
			@Override
			public void onChange(long timestamp, String iso8601) {
				logger.debug("startTsLsn called: {}  ", timestamp);
				if (startTs != timestamp) {
					startTs = timestamp;
					updateStartLabels();
					setCurrentTimestamp(startTs);
					updateTimeSliderModel();
					executor.execute(new Runnable() {

						@Override
						public void run() {
							trackSource.preloadTracks(startTs, stopTs);
						}
					});
				}

			}
		};
		final TimestampListener stopTsLsn = new TimestampListener() {
			@Override
			public void onChange(long timestamp, String iso8601) {
				logger.debug("stopTsLsn called: {}  ", timestamp);
				if (stopTs != timestamp) {
					stopTs = timestamp;
					updateStopLabels();
					setCurrentTimestamp(startTs);
					updateTimeSliderModel();
					executor.execute(new Runnable() {

						@Override
						public void run() {
							trackSource.preloadTracks(startTs, stopTs);
						}
					});

				}
			}
		};

		// binding dialog to start labels
		MouseAdapter startMa = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (tsDialog.getX() == 0 && tsDialog.getY() == 0) {
					tsDialog.setLocationRelativeTo(frame);
				}
				tsDialog.reset("Start TS", startTsLsn, startTs);

			}
		};
		panel.getjLabelStartTime().addMouseListener(startMa);
		panel.getjLabelStartDate().addMouseListener(startMa);

		// binding dialog to stop labels
		MouseAdapter stopMa = new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (tsDialog.getX() == 0 && tsDialog.getY() == 0) {
					tsDialog.setLocationRelativeTo(frame);
				}
				tsDialog.reset("Stop TS", stopTsLsn, stopTs);
			}
		};
		panel.getjLabelStopTime().addMouseListener(stopMa);
		panel.getjLabelStopDate().addMouseListener(stopMa);

	}

	private void setup() {
		setupPlayerControls();
		setupSettingsDialog();
		setupConnectionDialog();
		setupLiveDisplay();
		setupTimestampLabels();
		setupTimeSlider();
		setupNumberOfTracks();
		setupFitInterval();
		setupRePreload();
		updateNumberOfTracks();
	}

	// Setup fit interval
	private void setupFitInterval() {
		panel.getjLabelFitInterval().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				long next = trackSource.nextTrackStart(startTs);
				long prev = trackSource.previousTrackStart(stopTs);
				if(next != Long.MAX_VALUE)
					startTs = next;
				if(prev != Long.MIN_VALUE)
					stopTs = prev;
				if(startTs > stopTs) {
					long tmp;
					tmp = stopTs;
					stopTs = startTs;
					startTs = tmp;
				}
				updateStartLabels();
				updateStopLabels();
				setCurrentTimestamp(startTs);
				updateTimeSliderModel();
			}
		});

	}
	
	// retrigger preload of current range
	private void setupRePreload() {
		panel.getjLabelReloadTracks().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				logger.debug("RePreload");
				executor.execute(new Runnable() {
					@Override
					public void run() {
						trackSource.refresh();
					}
				});
			}			
		});
		
	}

	private synchronized void setCurrentTimestamp(long ts) {
		currentTs = ts;
		panel.getjLabelCurrentTimestamp().setText(
				sdfIso8601s.format(new Date(currentTs)));

		if (startTs > stopTs) {
			return;
		}
        
        currentTracks.clear();
		layer.reset();
		long alignedTs = ts - (ts % sliderResolution);
		currentTracks.addAll(trackSource.getTracks(alignedTs, alignedTs + sliderResolution));
		
		for (Track track : currentTracks) {
			// logger.debug(track + "");
			layer.addTrack(track);
		}
		updateNumberOfTracks();
		
		eventSupport.dispatch(state, new TrackPlayer.EventData(startTs, stopTs,currentTs, currentTracks.size()));
	}

	private void setupNumberOfTracks() {
		panel.getjLabelNumberOfTracks().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent arg0) {
				executor.execute(new Runnable() {
					@Override
					public void run() {
						trackSource.preloadTracks(startTs, stopTs);
					}
				});
			}
		});
	}

	private void setupTimeSlider() {
		JSlider slider = panel.getjSliderTime();
		// slider.addMouseMotionListener(repaintMouseAdapter);
		// slider.addMouseListener(repaintMouseAdapter);
		final BoundedRangeModel model = slider.getModel();
		slider.setSnapToTicks(true);
		model.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				setCurrentTimestamp(startTs
						+ (sliderResolution * model.getValue()));
				layer.repaint();
			}
		});

	}

	private void setupLiveDisplay() {

	}

	private void setupConnectionDialog() {

	}

	private void setupSettingsDialog() {
		final JButton button = panel.getjButtonSettings();
		button.setVisible(false);

		// button.setEnabled(true);

		panel.getjLabelResolution().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (settingsDialog.getX() == 0 && settingsDialog.getY() == 0) {
					settingsDialog.setLocationRelativeTo(frame);
				}
				settingsDialog.getjTextFieldResolution().setText(
						sliderResolution + "");
				settingsDialog.setVisible(true);
			}
		});

		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if (settingsDialog.getX() == 0 && settingsDialog.getY() == 0) {
					settingsDialog.setLocationRelativeTo(frame);
				}
				settingsDialog.getjTextFieldResolution().setText(
						sliderResolution + "");
				settingsDialog.setVisible(true);
			}
		});
		settingsDialog.getjButtonOk().addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent evt) {
				String text = settingsDialog.getjTextFieldResolution()
				.getText();
				try {
					int resolution = Integer.parseInt(text);
					if (resolution < 1) {
						throw new Exception();
					}
					sliderResolution = resolution;
					panel.getjLabelResolution().setText(
							sliderResolution + " ms");
					settingsDialog.setVisible(false);
					updateTimeSliderModel();

				} catch (Exception e) {
					logger.error("Invalid resolution: {} , ignoring it.", text);
				}

			}
		});
		panel.getjLabelResolution().setText(sliderResolution + " ms");

	}

	private void setupPlayerControls() {
		panel.getjButtonPause().setVisible(true);
		panel.getjButtonStart().setVisible(true);
		panel.getjButtonStop().setVisible(true);
		
		panel.getjButtonStop().setEnabled(false);
		panel.getjButtonPause().setEnabled(false);
		panel.getjButtonStart().setEnabled(true);

		//
		// Start
		//
		panel.getjButtonStart().addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				synchronized(autoplayTimer) {
					autoplayTask = new TimerTask() {
						@Override
						public void run() {
							BoundedRangeModel range = panel.getjSliderTime()
							.getModel();
							long ts = currentTs + sliderResolution;
							int sliderPosition = range.getValue() + 1;
							if (ts > stopTs) {
								ts = startTs;
								sliderPosition = 0;
							}
							setCurrentTimestamp(ts);
							range.setValue(sliderPosition);
						}
					};
					autoplayTimer.scheduleAtFixedRate(autoplayTask, 0, 1000);
					panel.getjButtonStop().setEnabled(true);
					panel.getjButtonPause().setEnabled(true);
					panel.getjButtonStart().setEnabled(false);
				}
				playStart();
			} 
		});
		panel.getjButtonPause().addActionListener(
				new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent arg0) {
						synchronized (autoplayTimer) {
							if (autoplayTask != null) {
								autoplayTask.cancel();
							}
							panel.getjButtonStop().setEnabled(true);
							panel.getjButtonPause().setEnabled(false);
							panel.getjButtonStart().setEnabled(true);
						}
					}
				}
		);
		//
		// Stop
		//
		panel.getjButtonStop().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				setCurrentTimestamp(startTs);
				panel.getjSliderTime().getModel().setValue(0);
				layer.reset();
				synchronized (autoplayTimer) {
					if (autoplayTask != null) {
						autoplayTask.cancel();
					}
				}
				panel.getjButtonStop().setEnabled(false);
				panel.getjButtonPause().setEnabled(false);
				panel.getjButtonStart().setEnabled(true);
				playStop();
			}
		});
		//
		//
		//


	}
	private void updateNumberOfTracks() {
		panel.getjLabelNumberOfTracks().setText(
				String.format("# %03d / %d ", currentTracks.size(), trackSource.getNumberOfTracks(startTs, stopTs)));
		layer.repaint();
	}

	@Override
	public void playStart() {
		state = TrackPlayer.State.PLAYING;
		eventSupport.dispatch(state, new TrackPlayer.EventData(startTs, stopTs,currentTs, currentTracks.size()));
	}

	@Override
	public void playStop() {
		state = TrackPlayer.State.STOPPED;
		eventSupport.dispatch(state, new TrackPlayer.EventData(startTs, stopTs,currentTs, currentTracks.size()));		
	}

	@Override
	public void loadPreferences() {
		startTs = prefs.getLong(PrefKeys.START_TS + "", System
				.currentTimeMillis() - 300000);
		stopTs = prefs.getLong(PrefKeys.STOP_TS + "", System
				.currentTimeMillis());
		updateDisplay();
	}

	private void updateDisplay() {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				updateStartLabels();
				updateStopLabels();
				updateControls();
				updateTimeSliderModel();
			}
		});
	}

	private void updateControls() {
		// panel
		// .getjButtonStart()
		// .setEnabled(
		// trackSource.numberOfPreloadedTracks() > 0
		// && ((state == State.PAUSED) || (state == State.STOPPED)));
		// panel.getjButtonPause().setEnabled(state == State.PLAYING);
		// panel.getjButtonStop().setEnabled(state == State.PAUSED);
	}

	private void updateTimeSliderModel() {
		currentTs = startTs;
		int ext = 0;
		int max = (int) Math.ceil((stopTs - startTs) / (double)sliderResolution);
		BoundedRangeModel model = panel.getjSliderTime().getModel();
		model.setRangeProperties(0, ext, 0, max, false);
		logger.debug("time slider changed: max = {} ", max);
		setCurrentTimestamp(currentTs);

	}

	/**
	 * Synchronize with cache
	 * 
	 */
	@Override
	public void savePreferences() {
		prefs.putLong(PrefKeys.START_TS + "", startTs);
		prefs.putLong(PrefKeys.STOP_TS + "", stopTs);

	}

	@Override
	public void init() {
		loadPreferences();
	}

	@Override
	public void stop() {
		playStop();
		savePreferences();
		layer.reset();

	}

	@Override
	public void addEventListener(State state, EventListener<EventData> lsn) {
		eventSupport.addEventListener(state, lsn);
	}

	@Override
	public void removeEventListener(EventListener<EventData> lsn) {
		eventSupport.removeEventListener(lsn);
	}

	@Override
	public void start() {
		trackSource.preloadTracks(startTs, stopTs);
	}

	@Override
	public TrackPlayerPanel getPanel() {
		return panel;
	}

	@Override
	public void updateStatus() {
		updateNumberOfTracks();
	}

	@Override
	public long getCurrentTimestamp() {
		return currentTs;
	}
	
	public void reloadCurrentTracks() {
		setCurrentTimestamp(currentTs);
	}

	@Override
	public State getState() {
		return state;
	}

	@Override
	public long getStartTimestamp() {
		return startTs;
	}

	@Override
	public long getStopTimestamp() {
		return stopTs;
	}
}
