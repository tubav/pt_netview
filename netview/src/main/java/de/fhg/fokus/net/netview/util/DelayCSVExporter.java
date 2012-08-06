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

package de.fhg.fokus.net.netview.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;

import de.fhg.fokus.net.netview.model.Model;
import de.fhg.fokus.net.netview.model.Node;
import de.fhg.fokus.net.netview.model.db.NetViewDB;

public class DelayCSVExporter extends SwingWorker<Boolean, Void> {
	private final static Logger logger = LoggerFactory.getLogger(Model.class);
	
	private final NetViewDB db;
	private final long trackid;
	private final long from;
	private final long to;
	private final BufferedWriter out;
	
	public DelayCSVExporter(NetViewDB db, long trackid, long from,	long to, BufferedWriter out) {
		this.db = db;
		this.trackid = trackid;
		this.from = from;
		this.to = to;
		this.out = out;
	}


	@Override
	protected Boolean doInBackground() throws Exception {
		int lastProgress = 0;
		setProgress(0);
		
		writeHeader();
		ArrayList<Long> recordIds = db.getTrackRepository().findTrackRecords(trackid, from, to);

		for(int i = 0; i < recordIds.size(); i++) {
			if(isCancelled())
				return false;
			writeRecord(recordIds.get(i));
			int progress = (int)(100 * ((double)i / recordIds.size()));
			if(progress != lastProgress)
				setProgress(progress);			
		}		
		
		out.flush();
		out.close();
		
		return true;
	}
	
	private void writeLine(String line) throws IOException {
		out.write(line + "\n");
	}
	
	private void writeHeader() throws IOException {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
		writeLine("# " + df.format(new Date(from)) + " - " + df.format(new Date(to)));
		writeLine("# Track: " + trackid);
		ArrayList<Node> nodes = db.getTrackRepository().findNodesOnTrack(trackid);
		for(int i = 0; i < nodes.size(); i++) {
			writeLine("#  " + i + ": " + nodes.get(i).view.getLabel());
		}
		writeLine("# record_id, timestamp, delay(0,1), delay(1,2), ..., delay(n-1,n)");
		writeLine("#");
	}
	
	private void writeRecord(Long recordId) throws IOException {
		EbeanServer eServer = db.getEbeanServer();
		SqlQuery query = eServer.createSqlQuery("SELECT ts, delay FROM raw_track_data WHERE record_id = :record ORDER BY hop_number")
							.setParameter("record", recordId);
		List<SqlRow> rows = query.findList();
		Iterator<SqlRow> it = rows.iterator();
		StringBuffer line = new StringBuffer();
		
		line.append(recordId.toString());
		for(int i = 0; i < (rows.size() - 1); i++) {
			SqlRow row = it.next();
			if(i == 0) {
				line.append(", " + row.getString("ts"));
			} 
			line.append(", " + row.getString("delay"));
		}
		writeLine(line.toString());
	}

	@Override
	protected void done() {
		logger.debug("DONE");
		setProgress(100);
	}
}
