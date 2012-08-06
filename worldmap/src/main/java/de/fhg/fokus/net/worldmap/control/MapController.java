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

//License: GPL. Copyright 2008 by Jan Peter Stotz

package de.fhg.fokus.net.worldmap.control;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.KeyStroke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.worldmap.model.Controllable;
import de.fhg.fokus.net.worldmap.model.LayerModel;

/**
 * Control Layer which implements map moving and zooming
 * 
 * @author Jan Peter Stotz (original author)
 * @author FhG-FOKUS NETwork Research (using layered pane)
 * 
 */

public class MapController {
	private final Map<KeyStroke, KeyHandler> keystrokeMap = new ConcurrentHashMap<KeyStroke, KeyHandler>();
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private final LayerModel layermodel;
	private final JLayeredPane layeredPane;
	private final Map<Class<?>, Controllable> controllerMap = new ConcurrentHashMap<Class<?>, Controllable>();
	private final AnimationSupport animationSupport;
	/**
	 * Possible controller states. It was created to support SHIFT/CTRL keys and other shortcuts.
	 * Depending on the the current state a controller is activated or deactivated.
	 * 
	 * 
	 */
	public static enum CtrlState {
		/**
		 * Panning mode
		 */
		PANNING,
		/**
		 * Panning mode, but currently selection controller is active (because e.g. SHIFT is pressed)
		 */
		PANNING_SELECT,
		/**
		 * Select mode 
		 */
		SELECT,
		/**
		 * Select mode, but CTRL was pressed so current controller is not panning
		 */
		SELECT_PANING

	}
	private final AtomicReference<CtrlState> state = new AtomicReference<CtrlState>();

	// Controllers
	private Controllable currentController;
	public MapController(LayerModel layermodel, JLayeredPane layeredPane,AnimationSupport animationSupport) {
		super();
		this.layermodel = layermodel;
		this.layeredPane = layeredPane;
		this.animationSupport = animationSupport;
		setupKeyListener();
	}
	private final KeyAdapter mapKeyAdapter = new KeyAdapter() {
		@Override
		public void keyPressed(KeyEvent e) {
//			logger.debug(KeyStroke.getKeyStrokeForEvent(e)+"");
			KeyHandler keyHandler =	keystrokeMap.get(KeyStroke.getKeyStrokeForEvent(e));
			if( keyHandler!=null){
				keyHandler.execute(e);
			}
		}
		@Override
		public void keyReleased(KeyEvent e) {
			KeyHandler keyHandler =	keystrokeMap.get(KeyStroke.getKeyStrokeForEvent(e));
			if( keyHandler!=null){
				keyHandler.execute(e);
			}
		}
	};
	private void setupKeyListener() {
		// request focus in order to receive key events
		loadDefaultKeyMap();
		this.layeredPane.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				layeredPane.requestFocusInWindow();
			}
		});
		this.layeredPane.addKeyListener(mapKeyAdapter);
	}
	private void loadDefaultKeyMap() {
		// 1. SHIFT PRESSED
//		keystrokeMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT,KeyEvent.SHIFT_DOWN_MASK), new KeyHandler() {
//			@Override
//			public void execute(KeyEvent e) {
//				if( state.compareAndSet(CtrlState.PANNING, CtrlState.PANNING_SELECT)){
//					setActiveController(SelectController.class);
//				}
//			}
//		});
		keystrokeMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SHIFT,0,true), new KeyHandler() {
			@Override
			public void execute(KeyEvent e) {
				if(state.compareAndSet(CtrlState.PANNING_SELECT, CtrlState.PANNING)) {
					setActiveController(PanningController.class);
				}
			}
		});
		// 2. CONTROL PRESSED
		keystrokeMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL,KeyEvent.CTRL_DOWN_MASK), new KeyHandler() {
			@Override
			public void execute(KeyEvent e) {
				if( state.compareAndSet(CtrlState.SELECT, CtrlState.SELECT_PANING)){
					setActiveController(PanningController.class);
				}
			}
		});
//		keystrokeMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_CONTROL,0,true), new KeyHandler() {
//			@Override
//			public void execute(KeyEvent e) {
//				if( state.compareAndSet(CtrlState.SELECT_PANING, CtrlState.SELECT)) {
//					setActiveController(SelectController.class);
//				}
//			}
//		});
		// 3. CONTROL+ L PRESSED -> show layers dialog
		keystrokeMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, KeyEvent.CTRL_DOWN_MASK), new KeyHandler() {
			@Override
			public void execute(KeyEvent e) {
				JDialog dialog =layermodel.getToolsLayer().getLayersDialog();
				dialog.setVisible(!dialog.isVisible());
				
			}
		});
		
		// 4. CONTROL+ K PRESSED -> show track layers 
//		keystrokeMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_K, KeyEvent.CTRL_DOWN_MASK), new KeyHandler() {
//			@Override
//			public void execute(KeyEvent e) {
//				JDialog dialog =layermodel.getToolsLayer().getSplineLayersDialog();
//				dialog.setVisible(!dialog.isVisible());
//				
//			}
//		});
		
		
		
		// 5. Zoom in / Zoom out
		keystrokeMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,KeyEvent.SHIFT_DOWN_MASK), new KeyHandler() {
			@Override
			public void execute(KeyEvent e) {
				layermodel.getMapLayer().zoomIn();
			}
		});
		keystrokeMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, 0), new KeyHandler() {
			@Override
			public void execute(KeyEvent e) {
				layermodel.getMapLayer().zoomOut();
			}
		});
		


	}
	public MapController setState(CtrlState targetState ){
		switch (targetState) {
		case PANNING:
			state.set(targetState);
			setActiveController(PanningController.class);
			layeredPane.requestFocusInWindow();
			break;
//		case SELECT:
//			state.set(targetState);
//			setActiveController(SelectController.class);
//			layeredPane.requestFocusInWindow();
		default:
			break;
		}
		return this;
	}
	public CtrlState getState(){
		return state.get();
	}

	/**
	 * Activate on controller. It should be called after init();
	 * @param <T>
	 * @param controllerClass
	 * @return
	 */
	private <T extends Controllable>MapController setActiveController( Class<T> controllerClass ){
		Controllable ctrl = controllerMap.get(controllerClass);
		if(ctrl==null){
			logger.warn("controller "+controllerClass+" not found, perhaps not loaded.");
			return this;
		}
		// stopping current controller
		if( currentController!=null){
			currentController.stop();
		}
		ctrl.start();
		this.currentController=ctrl;
		return this;
	}
	/**
	 * Return registered controller instance
	 * @param <C>
	 * @param controllerClass
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <C extends Controllable> C getController( Class<C> controllerClass  ){
		C ctrl = (C) controllerMap.get(controllerClass);
		return ctrl;
	}
	private boolean initialized=false;
	public MapController init() {
		if(initialized){
			return this;
		}
		// create controllers
		controllerMap.put(PanningController.class, new PanningController(layermodel.getMapLayer(), layeredPane).init());
		controllerMap.put(SelectController.class, new SelectController(layeredPane,animationSupport).init());
		// set panning as default
		setActiveController(PanningController.class);
		state.set(CtrlState.PANNING);
		initialized= true;
		return this;
	}

}
