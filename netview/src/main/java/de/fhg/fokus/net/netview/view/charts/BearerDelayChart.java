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
package de.fhg.fokus.net.netview.view.charts;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.TickUnits;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYSeries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;

import de.fhg.fokus.net.netview.model.Node;
import de.fhg.fokus.net.netview.model.db.NetViewDB;
import de.fhg.fokus.net.netview.view.util.ColorSequence;
import de.fhg.fokus.net.worldmap.layers.track.Bearer;

public class BearerDelayChart extends JFrame {

    private static final long serialVersionUID = 1L;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Bearer bearer;
    private long time;
    private String[] nodeNames = new String[0];
    private ChartPanel chartPanel;
    private JFreeChart chart;
    private final NetViewDB db;
    private final EbeanServer eServer;
    private final TimeUnit DB_TS_TIMEUNIT = TimeUnit.MICROSECONDS;
    private ColorSequence colorSequence;

    private static class TimeStampTickUnit extends NumberTickUnit {

        private static final long serialVersionUID = -1352559560794145125L;
        private final DateFormat df = new SimpleDateFormat("HH:mm:ss");

        public TimeStampTickUnit(double size) {
            super(size);
        }

        @Override
        public String valueToString(double value) {
            String s = df.format(new Date((long) value / 1000));
            return s;
        }
    }

    public BearerDelayChart(NetViewDB netViewDB, String title, Bearer bearer,
            long time) {
        super(title);

        this.db = netViewDB;
        this.eServer = db.getEbeanServer();
//		this.ef = eServer.getExpressionFactory();

        this.bearer = bearer;
        this.time = time;

        chart = setupChart();

        chartPanel = new ChartPanel(chart);
        setContentPane(chartPanel);
    }

    private JFreeChart setupChart() {

        nodeNames = fetchNodeNames(bearer.getTrack().getTrackId());
        DefaultTableXYDataset data = fetchData();
        colorSequence = new ColorSequence();

        chart = ChartFactory.createStackedXYAreaChart("Bearer Delay", "ts",
                "ms", data, PlotOrientation.VERTICAL, true, false, false);

        XYPlot plot = chart.getXYPlot();

        ValueAxis axis = plot.getDomainAxis();
        TickUnits tickUnits = new TickUnits();
        tickUnits.add(new TimeStampTickUnit(10000000));
        axis.setStandardTickUnits(tickUnits);

        XYItemRenderer renderer = plot.getRenderer();
        for (int i = 0; i < plot.getDataset(0).getSeriesCount(); i++) {
            renderer.setSeriesPaint(i, colorSequence.next());
        }
        return chart;
    }

    private String[] fetchNodeNames(long trackId) {
        ArrayList<Node> nodes = db.getTrackRepository().findNodesOnTrack(trackId);
        String[] names = new String[nodes.size()];
        for (int i = 0; i < nodes.size(); i++) {
            names[i] = nodes.get(i).view.getLabel();
        }
        return names;
    }

    private DefaultTableXYDataset fetchData() {
        // TODO fetch data from db

        DefaultTableXYDataset data = new DefaultTableXYDataset();
        long fixed_ts = DB_TS_TIMEUNIT.convert(time, TimeUnit.MILLISECONDS);
        long timeInterval = 100000000;
        String sql_query = "SELECT DISTINCT(hop_number) FROM "
                + "raw_track_data WHERE track_id = :track "
                + "AND rule_id = :rule ORDER BY hop_number ASC";
        SqlQuery query = eServer.createSqlQuery(sql_query)
                .setParameter("track", bearer.getTrack().getTrackId())
                .setParameter("rule", bearer.getRuleId());
        List<SqlRow> hopResults = query.findList();
        int numberOfHops = hopResults.size();
        ArrayList<Long> rowStore;
        ArrayList<ArrayList<Long>> tableStore = new ArrayList<ArrayList<Long>>();
        int i = 0;
        for (SqlRow row : hopResults) {
            int hopNumber = row.getInteger("hop_number");

            //Query delays for each hop (time frame of 3 seconds)
            sql_query = "SELECT delay, ts, packet_id FROM raw_track_data "
                    + "WHERE ts BETWEEN :playertsstart AND :playertsstop "
                    + "AND track_id = :track AND rule_id = :rule "
                    + "AND hop_number = :hop ORDER BY ts ASC";
            query = eServer.createSqlQuery(sql_query)
                    .setParameter("playertsstart", fixed_ts - timeInterval)
                    .setParameter("playertsstop", fixed_ts)
                    .setParameter("track", bearer.getTrack().getTrackId())
                    .setParameter("rule", bearer.getRuleId())
                    .setParameter("hop", hopNumber);

            List<SqlRow> delayResults = query.findList();
            rowStore = new ArrayList<Long>();
            for (SqlRow delays : delayResults) {
                rowStore.add(delays.getLong("ts"));
                rowStore.add(delays.getLong("packet_id"));
                rowStore.add(delays.getLong("delay") / 1000);

            }
            tableStore.add(rowStore);
            i++;
        }

        if (!tableStore.isEmpty()) {
            if (!tableStore.get(0).isEmpty()) {
                logger.debug("calling fixVector__Entry");
                tableStore = fixVectorFirstEntry(tableStore);
                tableStore = fixVectorLastEntry(tableStore);
            }
            XYSeries delays[] = new XYSeries[numberOfHops - 1];

            for (int init = 0; init < delays.length; init++) {
                String from, to;
                if (init < nodeNames.length) {
                    from = nodeNames[init];
                } else {
                    from = Integer.toString(init);
                }
                if ((init + 1) < nodeNames.length) {
                    to = nodeNames[init + 1];
                } else {
                    to = Integer.toString(init + 1);
                }
                delays[init] = new XYSeries(from + " -> " + to, true, false);
            }

            for (int x = 0; x < delays.length; x++) {
                if (tableStore.size() <= x) {
                    break;
                }
                for (int y = 0; y < tableStore.get(x).size(); y = y + 3) {
                    if ((tableStore.get(x).size() <= y + 3) ||
                            (tableStore.get(0).size() <= y + 3)) {
                        break;
                    }
                    delays[x].addOrUpdate(tableStore.get(0).get(y),
                            tableStore.get(x).get(y + 2));
                }
            }
            for (XYSeries s : delays) {
                data.addSeries(s);
            }
        }
        return data;
    }

    private ArrayList<ArrayList<Long>> fixVectorFirstEntry(
            ArrayList<ArrayList<Long>> data) {
        long packetId = data.get(0).get(1);

        for (int i = 1; i < data.size(); i++) {
            for (int j = 1; j < data.get(i).size(); j = j + 3) {
                if (packetId == data.get(i).get(j)) {
                    for (int shift = 1; shift < j; shift++)
                        data.get(i).remove(0);
                    break;
                }
            }
        }
        return data;
    }

    // TO BE DONE
    private ArrayList<ArrayList<Long>> fixVectorLastEntry(
            ArrayList<ArrayList<Long>> data) {

        int size = data.get(data.size() - 1).size();
        long packetId = data.get(data.size() - 1).get(size - 2);

        for (int i = 0; i < data.size() - 2; i++) {
            for (int j = size - 2; data.get(i).size() > j; j = j + 3) {
                if (packetId == data.get(i).get(j)) {
                    for (; data.get(i).size() > j + 2;)
                        data.get(i).remove(data.get(i).size() - 1);
                    break;
                }
            }
        }
        return data;
    }
}
