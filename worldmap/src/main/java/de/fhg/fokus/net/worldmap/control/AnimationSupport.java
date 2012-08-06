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

package de.fhg.fokus.net.worldmap.control;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.Animator.RepeatBehavior;

import de.fhg.fokus.net.worldmap.model.Controllable;
/**
 * Holds objects such as timers (animators) to be reused through out the application.
 * There's usually only on object of this class. Objects that need a reference to
 * this class should be injected (please avoid static single tone references).
 * 
 * @author FhG-FOKUS NETwork Research
 *
 */
public class AnimationSupport implements Controllable {
//	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final ScheduledExecutorService scheduler ;
	private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private final Animator trackAnimator;
	
	
	public Animator getTrackAnimator() {
		return trackAnimator;
	}
	public static enum Events {
		/**
		 * <p>a selection stroke has changed</p>
		 * <pre>
		 * oldvalue = old stroke
		 * newvalue = new stroke
		 * </pre>
		 */
		SELECTION_STROKE_CHANGED
	}

	public AnimationSupport(ScheduledExecutorService scheduler) {
		super();
		this.scheduler = scheduler;
		trackAnimator = new Animator(1000);
		trackAnimator.setRepeatBehavior(RepeatBehavior.LOOP);
		trackAnimator.setResolution(50);
		trackAnimator.setRepeatCount(Double.POSITIVE_INFINITY);

		
	}

	final float[][] DASH_PATTERNS = {{ 5f },{5f}};
	private final Stroke stroke0 = new BasicStroke(1.0f,BasicStroke.CAP_SQUARE,BasicStroke.CAP_SQUARE,4f,DASH_PATTERNS[0],0f);
	private final Stroke stroke1 = new BasicStroke(1.0f,BasicStroke.CAP_SQUARE,BasicStroke.CAP_SQUARE,4f,DASH_PATTERNS[1],4f);
	private final Stroke [] selectionStrokes = {
			stroke0,stroke1
	}; 
	private byte strokeIndex = 0;
	private ScheduledFuture<?> strokeSelectionFuture;
	private final Runnable strokeSelectionTask = new Runnable() {
		@Override
		public void run() {
			// selection stroke
			Stroke oldValue = selectionStrokes[(strokeIndex++)%selectionStrokes.length];
			Stroke newValue = selectionStrokes[strokeIndex%selectionStrokes.length];
			pcs.firePropertyChange(Events.SELECTION_STROKE_CHANGED+"", oldValue, newValue);
			if(strokeIndex == selectionStrokes.length){
				strokeIndex=0;
			}
		}
	};

	@Override
	public Controllable init() {

		return this;
	}
	@Override
	public void start() {

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}
	/**
	 * Start stroke animation.
	 */
	public AnimationSupport startStrokeSelection(){
		if(strokeSelectionFuture!=null){
			if(!strokeSelectionFuture.isDone()){
				return this;
			}
		}
		strokeSelectionFuture = scheduler.scheduleAtFixedRate(strokeSelectionTask, 0, 330, TimeUnit.MILLISECONDS);
		return this;
	}
	/**
	 * Stop stroke animation
	 */
	public AnimationSupport stopStrokeSelection(){
		if( strokeSelectionFuture!=null && !strokeSelectionFuture.isCancelled()){
			strokeSelectionFuture.cancel(false);
		}
		return this;
	}
	/**
	 * returns default stroke used for selections
	 */
	public Stroke getDefaultSelectionStroke(){
		return stroke0;
	}
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(propertyName,listener);
	}
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

}

