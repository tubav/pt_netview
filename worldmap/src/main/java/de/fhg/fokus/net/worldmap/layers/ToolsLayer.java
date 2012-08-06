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

package de.fhg.fokus.net.worldmap.layers;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.prefs.Preferences;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.worldmap.control.AnimationSupport;
import de.fhg.fokus.net.worldmap.control.MapController;
import de.fhg.fokus.net.worldmap.control.MapController.CtrlState;
import de.fhg.fokus.net.worldmap.control.SelectController;
import de.fhg.fokus.net.worldmap.layers.track.TrackPlayerPanel;
import de.fhg.fokus.net.worldmap.model.LayerModel;
import de.fhg.fokus.net.worldmap.model.PersistentPreferences;
import de.fhg.fokus.net.worldmap.model.ui.UILayersModel;
import de.fhg.fokus.net.worldmap.view.LayersDialog;
import de.fhg.fokus.net.worldmap.view.SelectArea;
import de.fhg.fokus.net.worldmap.view.ToolPalette;

/**
 * Implements view and control functions to various map tools. Control functions are
 * performed in conjunction with {@link MapController} and its subcontrollers.
 * 
 * @author FhG-FOKUS NETwork Research
 *
 */
public class ToolsLayer extends Layer implements PersistentPreferences {
	private static final Logger logger = LoggerFactory.getLogger(ToolsLayer.class);
	private static final long serialVersionUID = 1L;
	private final ToolPalette toolpalette = new ToolPalette();
	private final TrackPlayerPanel trackPlayerPanel = new TrackPlayerPanel();
	private final LayerModel layerModel;
	private final MapController mapCtrl;
	private final SelectController selectCtrl;
	private final AnimationSupport animationSupport;
	private final SelectArea selectArea;
	private final JFrame frame;
	private final LayersDialog layersDialog;
	private final UILayersModel uiLayersModel;

	private static enum PrefKeys {
		TOOL_PALETTE_VISIBLE,
		MAP_CONTROLLER_STATE,
		TOOLSLAYER_VISIBLE,
		TRACKPLAYER_VISIBLE
	}
	// we are doing manual layout, so we need some wrappers
	private JPanel trackPanelWrapper;
	private JPanel toolPaletteWrapper;
	private final Preferences prefs = Preferences.userNodeForPackage(this.getClass());
	public ToolsLayer(LayerModel layerModel, MapController mapController, AnimationSupport animationSupport ) {
		super("Tools");
		this.frame = (JFrame) SwingUtilities.getRoot(this);
		this.layerModel = layerModel;
		this.uiLayersModel = new UILayersModel(this.layerModel);
		this.mapCtrl = mapController;
		this.selectCtrl = mapController.getController(SelectController.class);
		this.animationSupport = animationSupport;
		this.selectArea = new SelectArea(this,this.animationSupport,this.selectCtrl.getStart(), this.selectCtrl.getEnd());
		this.layersDialog = new LayersDialog(this.frame, false);

		
		// toolpalette
		toolpalette.setSize(toolpalette.getPreferredSize());
		toolpalette.setOpaque(false);
		toolpalette.getjToolBarBox().setOpaque(false);
		
		//
		// Laying out tools
		//
		
		// component layout
		toolPaletteWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0,0));
		toolPaletteWrapper.add(toolpalette);
		toolPaletteWrapper.setOpaque(false);

		trackPanelWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0,0));
		trackPanelWrapper.add(trackPlayerPanel);
		trackPanelWrapper.setOpaque(false);
//		trackPlayerPanel.rtsetOpaque(false);
//		trackPlayerPanel.setPreferredSize(new Dimension(1000, 50));
		// - main layout
		setLayout(new BorderLayout());
		add(toolPaletteWrapper, BorderLayout.LINE_END);
		add(trackPanelWrapper, BorderLayout.PAGE_END);
		
		
		
		// track player
//		add(trackPlayerPanel);
		
		// done layout
		
		setupZoomControls();
		setupControllerSelection();
		setupLayersDialog();
		setupTrackPlayerPanel();
		
		setupRefresh();

	}
	
	private void setupTrackPlayerPanel() {
		final JToggleButton btn = toolpalette.getjToggleButtonTrackPlayer();
		btn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
			  trackPanelWrapper.setVisible(btn.isSelected());
			}
		});
		
	}

	private void setupRefresh() {
		toolpalette.getjButtonRefresh().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				layerModel.getMapLayer().reloadMap();
			
			}
		});
		
	}
	
	public LayersDialog getLayersDialog() {
		return layersDialog;
	}
	
	
	private void setupLayersDialog() {
		if( layersDialog.getX()==0 && layersDialog.getY()==0 ){
			layersDialog.setLocationRelativeTo(frame);
		}
		try {
			layersDialog.setIconImage(ImageIO.read(this.getClass().getResource("../view/resources/layers.png")));
		} catch (Exception e) {
			logger.error("Could not find image: /view/resources/layers.png");
		}
		
		layersDialog.getjTableLayers().setModel(uiLayersModel.getTableModel());
		
		// key listener
		layersDialog.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
//				logger.debug("dialog shown");
				requestFocusInWindow();
			}
			@Override
			public void componentHidden(ComponentEvent e) {
//				logger.debug("dialog hidden");
				ToolsLayer.this.getParent().requestFocusInWindow();
			}
		});
		// Enable showing dialog via palette
		toolpalette.getjButtonLayers().addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				layersDialog.setVisible(true);
			}
		});
		

	}
	@Override
	protected void paintComponent(Graphics g) {
		if( selectCtrl.isSelecting() ){
			selectArea.paint(g);
		}

	}


	private void setupControllerSelection(){
//		toolpalette.getjButtonSelect().addMouseListener(new MouseAdapter() {
//			@Override
//			public void mouseClicked(MouseEvent e) {
//				//mapCtrl.setState(CtrlState.SELECT);
//			}
//		});
//		toolpalette.getjButtonMove().addMouseListener(new MouseAdapter() {
//			@Override
//			public void mouseClicked(MouseEvent e) {
//				mapCtrl.setState(CtrlState.PANNING);
//			}
//		});

	}

	private void setupZoomControls() {
		toolpalette.getjButtonZoomIn().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				layerModel.getMapLayer().zoomIn();
			}
		});
		toolpalette.getjButtonZoomOut().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				layerModel.getMapLayer().zoomOut();
			}
		});
	}

	// TODO fade in animation 
	public void setPaletteVisible(boolean selected) {
		toolpalette.setVisible(selected);
	}

	@Override
	public void loadPreferences() {
		// restoring track player state
		trackPanelWrapper.setVisible(prefs.getBoolean(PrefKeys.TRACKPLAYER_VISIBLE+"", false));
		toolpalette.getjToggleButtonTrackPlayer().setSelected( trackPanelWrapper.isVisible());
		
		
		setVisible(prefs.getBoolean(PrefKeys.TOOLSLAYER_VISIBLE+"", true));
		// restoring palette visible
		setPaletteVisible(prefs.getBoolean(PrefKeys.TOOL_PALETTE_VISIBLE+"", true));
		// restoring active controller
		CtrlState ctrlState = null;
		try {
			ctrlState= CtrlState.valueOf(prefs.get(PrefKeys.MAP_CONTROLLER_STATE+"", CtrlState.PANNING+""));
		} catch (Exception e) {
			ctrlState=CtrlState.PANNING;
		}
		mapCtrl.setState(ctrlState);


		
	}
	/**
	 * @return whether tool palette is visible.
	 */
	public boolean isPaletteVisible(){
		return toolpalette.isVisible();
	}

	@Override
	public void savePreferences() {
		prefs.putBoolean(PrefKeys.TOOL_PALETTE_VISIBLE+"", toolpalette.isVisible());
		prefs.put(PrefKeys.MAP_CONTROLLER_STATE+"", mapCtrl.getState()+"");
		prefs.putBoolean(PrefKeys.TOOLSLAYER_VISIBLE+"", isVisible());
		prefs.putBoolean(PrefKeys.TRACKPLAYER_VISIBLE+"", trackPanelWrapper.isVisible());
		
		// layers visibility
		
	}
	public TrackPlayerPanel getTrackPlayerPanel() {
		return trackPlayerPanel;
	}

}
