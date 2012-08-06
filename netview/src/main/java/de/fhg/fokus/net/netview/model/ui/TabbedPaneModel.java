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

package de.fhg.fokus.net.netview.model.ui;

import java.awt.Component;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.JTabbedPane;

import org.jdesktop.application.ResourceMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fhg.fokus.net.netview.model.PersistentPreferences;
/**
 * Extends TabbedPane model with some functions.
 * 
 * @author FhG-FOKUS NETwork Research
 * @apiviz.stereotype entity
 */
public class TabbedPaneModel implements PersistentPreferences {
	//==[ External services ]===
	/**
	 * Logger
	 */
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	//==[ Constants ]==
	//==[ Properties ]==
	/**
	 * Used to save initial model
	 */
	protected final Map<String, Component> tabMap = new TreeMap<String, Component>();
	/**
	 * 
	 * tabbed pane
	 */
	protected final JTabbedPane tabbedPane;
	/**
	 * Resource map
	 */
	protected final ResourceMap resourceMap;
	public JTabbedPane getTabbedPane() {
		return tabbedPane;
	}


	public TabbedPaneModel(JTabbedPane tabbedPane, ResourceMap resourceMap) {
		super();
		this.tabbedPane = tabbedPane;
		this.resourceMap = resourceMap;
	}
	/**
	 * Initialize tabbed pane. The tab components are save in
	 * the the tabs list so we have hide/show them without losing
	 * the initial model.
	 */
	private void init() {
		tabMap.clear();
		for( Component c: tabbedPane.getComponents() ){
			tabMap.put(c.getName(), c);
		}
		//		for( String s : resourceMap.keySet() ){
		//			//			logger.debug(s );
		//		}
	}
	/**
	 * Return whether controlled tabbed pane contains tab
	 * 
	 * @param tab
	 * @return
	 */
	protected boolean paneModelContainsTab( String name ){
		for( Component component: tabbedPane.getComponents() ){
			if (component.getName().contentEquals(name)){
				return true;
			}
		}
		return false;
	}
	/**
	 * Show/Hide tab
	 * @param name tab name
	 * @param show true to show, false to hide
	 */
	public void setTabVisible( String name, boolean show ){
		Component c = tabMap.get(name);
		if( c==null){
			logger.error("tab "+name+" was not found");
			return;
		}
		int index = tabbedPane.indexOfComponent(c);
		if( !show && index > -1 ){
			tabbedPane.remove(index);
			return;
		}
		if( show ){
			tabbedPane.add(c);
			tabbedPane.setSelectedComponent(c);
			index = tabbedPane.indexOfComponent(c);
			if( index> -1 ){
				String title = resourceMap.getString(c.getName()+".TabConstraints.tabTitle");
				tabbedPane.setTitleAt(index, title);
			}
		}
	}

	@Override
	public void loadPreferences() {
		logger.debug("init");
		init();
		// TODO Auto-generated method stub

	}

	@Override
	public void savePreferences() {
		// TODO Auto-generated method stub

	}

}
