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

package de.fhg.fokus.net.netview.sys;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;

/**
 * NetView configuration.
 * 
 * @author FhG-FOKUS NETwork Research
 * 
 */
public class NetViewConfig {
	/**
	 * NetView home directory
	 */
	public static String DEFAULT_NETVIEW_HOME = "." + File.separator + "nvhome";
	private File netviewHome;
	private String cacheDir;
	private String markersDirectory;
	private String dbName="netview";
	private String dbDir;
	private String dbUser="netview";
	private String dbPass="";
	private String dbDriver="org.h2.Driver";
	private File dbFile;
	private File dbInitScriptFile;
	private String dbCharsetName = "utf8";
	private boolean dbContinueOnError = false;

	private static final Logger logger = LoggerFactory
	.getLogger(NetViewConfig.class);

	public NetViewConfig() {
		this(new File(DEFAULT_NETVIEW_HOME));
	}
	public NetViewConfig(File netviewHome) {
		this.netviewHome = netviewHome;
	}

	public File getNetviewHome() {
		return netviewHome;
	}

	public NetViewConfig setNetviewHome(File netviewHome) {
		this.netviewHome = netviewHome;
		return this;
	}

	public String getCacheDir() {
		if(cacheDir == null)
			return netviewHome.getAbsolutePath() + File.separator + "cache";
		else
			return cacheDir;
	}

	public void setCacheDir(String cacheDir) {
		this.cacheDir = cacheDir;
	}

	private static XStream xstream = new XStream();

	// FIXME config file needs redesign
	public static NetViewConfig loadFromXmlFile( File file ){
		NetViewConfig cfg = null;
		//		try {
		//			BufferedReader in = new BufferedReader(new FileReader(file));
		//			cfg = (NetViewConfig) xstream.fromXML(in);
		//			logger.info("using configuration settings from {}",file);
		//		} catch (FileNotFoundException e) {
		//			logger.warn("Could not load config settings from {}, using defaults.",file);
		cfg = new NetViewConfig();
		//		}
		return cfg;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getDbDir() {
		if(dbDir==null){
			dbDir = netviewHome.getAbsolutePath()+File.separator+"db";
		}
		return dbDir;
	}

	public void setDbDir(String dbDir) {
		this.dbDir = dbDir;
	}

	public File getDbFile() {
		dbFile = new File(getDbDir()+File.separator+getDbName()+".h2.db");
		return dbFile;
	}

	public void setDbFile(File dbFile) {
		this.dbFile = dbFile;
	}

	public String getDbUser() {
		return dbUser;
	}

	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}

	public String getDbPass() {
		return dbPass;
	}

	public void setDbPass(String dbPass) {
		this.dbPass = dbPass;
	}

	public String getDbDriver() {
		return dbDriver;
	}

	public void setDbDriver(String dbDriver) {
		this.dbDriver = dbDriver;
	}
	public String getDbUrl(){
		return "jdbc:h2:"+getDbDir()+"/"+getDbName()+";AUTO_SERVER=TRUE";
	}
	/**
	 * Get initialization db script file. 
	 * @return
	 */
	public File getDbInitScriptFile() {
		return dbInitScriptFile;
	}

	public void setDbScriptFile(File dbScriptFile) {
		this.dbInitScriptFile = dbScriptFile;
	}

	public String getDbCharsetName() {
		return dbCharsetName;
	}

	public void setDbCharsetName(String dbCharsetName) {
		this.dbCharsetName = dbCharsetName;
	}

	public boolean isDbContinueOnError() {
		return dbContinueOnError;
	}

	public void setDbContinueOnError(boolean dbContinueOnError) {
		this.dbContinueOnError = dbContinueOnError;
	}

	public String getMarkersDirectory() {
		if(markersDirectory == null)
			return netviewHome.getAbsolutePath() + File.separator + "markers";
		else
			return markersDirectory;
	}

	public void setMarkersDirectory(String markersDirectory) {
		this.markersDirectory = markersDirectory;
	}



}
