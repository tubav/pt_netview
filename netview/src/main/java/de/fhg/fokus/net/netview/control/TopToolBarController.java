/* Netview - a software component to visualize packet tracks, hop-by-hop delays,
 *           sampling stats and resource consumption. Netview requires the deployment of
 *           distributed probes (impd4e) and a central packet matcher to correlate the
 *           obervations.
 *
 *           The probe can be obtained at http://impd4e.sourceforge.net/downloads.html
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

package de.fhg.fokus.net.netview.control;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JToggleButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.netview.model.ui.ToolBarModel;
import de.fhg.fokus.net.netview.view.ViewMain;

/**
 * @author FhG-FOKUS NETwork Research
 * 
 *  Controls toolbar
 */
public class TopToolBarController implements Controllable {
	//== external services ==
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	//== related Controllers
	private final MainTabbedPaneController tabbedPaneMainController;
	private final MapController mapCtrl;
	// == properties ==
	private final ToolBarModel toolbarModel;

	private final ViewMain view ;

	// == constants ==
	/**
	 * Used to map changed event (we simply use the button name) to controlled tab. 
	 * This is converted afterwards to a Map<String,String>
	 * FIXME: use ViewMain.Events instead
	 */
	private static final String[]  BUTTON_TABBED_PANEL_MAPPING = {
		"jToggleButtonMap","jPanelMap",
		"jToggleButtonConsole","jScrollPaneConsole",
		"jToggleButtonDataSources","jPanelDataSources"

	};
	private final Map<String, String> buttonTabbedPanelMapping = new HashMap<String, String>();
	public TopToolBarController( ViewMain viewMain, MapController mapController,
			MainTabbedPaneController tabbedPaneMainController ) {
		this.view = viewMain;
		this.mapCtrl = mapController;
		this.toolbarModel = new ToolBarModel(view.getJToolBarTop());
		this.tabbedPaneMainController = tabbedPaneMainController;

		// initializing mappings 
		for(int i=0; i< BUTTON_TABBED_PANEL_MAPPING.length; i+=2 ){
			buttonTabbedPanelMapping.put(BUTTON_TABBED_PANEL_MAPPING[i], BUTTON_TABBED_PANEL_MAPPING[i+1]);
		}

		//==  synctab  ==
		for( String propertyChangeName: buttonTabbedPanelMapping.keySet()){
			view.addPropertyChangeListener(propertyChangeName ,new PropertyChangeListener(){
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					syncTab( event );
				}
			});
		}

		// show configuration depending on which tab is
		// active
		view.addPropertyChangeListener(ViewMain.Events.jButtonConfig+"" ,new PropertyChangeListener(){
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if( "jPanelMap".contentEquals(view.getJTabbedPaneMain().getSelectedComponent().getName())){
					mapCtrl.showConfigDialog();
				}
			}
		});
	}

	@Override
	public void init() {
		toolbarModel.loadPreferences();
		start();
	}

	@Override
	public void start() {
		// == synchronize tabs ==
		for( Entry<String, String> tuple: buttonTabbedPanelMapping.entrySet()  ){
			syncTab(tuple.getKey(), tuple.getValue());
		}

	}
	@Override
	public void stop() {
		toolbarModel.savePreferences();
	}
	/**
	 * Hide or show tab depending on the respective toogle button state.
	 * 
	 * @param buttonName
	 * @param tabName
	 */
	private void syncTab( String buttonName, String tabName ){
		JToggleButton btn = toolbarModel.getToggleButton(buttonName);
		if( btn!=null){
			if( btn.isSelected()){
				tabbedPaneMainController.setTabVisible(tabName, true);
			} else {
				tabbedPaneMainController.setTabVisible(tabName, false);
			}
		} else {
			logger.warn("button model not found: "+buttonName);
		}
	}
	public void syncTab(PropertyChangeEvent event) {
		syncTab(event.getPropertyName(), buttonTabbedPanelMapping.get(event.getPropertyName()));
	}
}
