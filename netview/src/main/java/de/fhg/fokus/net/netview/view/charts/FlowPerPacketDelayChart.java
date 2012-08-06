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

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.SqlQuery;
import com.avaje.ebean.SqlRow;

import de.fhg.fokus.net.netview.model.Node;
import de.fhg.fokus.net.netview.model.db.NetViewDB;
import de.fhg.fokus.net.netview.view.util.ColorSequence;
import de.fhg.fokus.net.worldmap.layers.track.Flow;

public class FlowPerPacketDelayChart extends JFrame {

    private static final long serialVersionUID = 1L;
    private Flow flow;
    private long time;
    private String[] nodeNames = new String[0];
    private ChartPanel chartPanel;
    private JFreeChart chart;
    private final NetViewDB db;
    private final EbeanServer eServer;
    private final TimeUnit DB_TS_TIMEUNIT = TimeUnit.MICROSECONDS;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private ColorSequence colorSequence;

    public FlowPerPacketDelayChart(NetViewDB netViewDB,
            String title, Flow flow, long time) {
        super(title);

        this.db = netViewDB;
        this.eServer = netViewDB.getEbeanServer();

        this.flow = flow;
        this.time = time;

        chart = setupChart();
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setDrawingSupplier(new NetViewChartDrawingSupplier());

        chartPanel = new ChartPanel(chart);
        setContentPane(chartPanel);
    }

    private JFreeChart setupChart() {
        nodeNames = fetchNodeNames(flow.getBearer().getTrack().getTrackId());
        DefaultCategoryDataset data = fetchData();
        colorSequence = new ColorSequence();
        chart = ChartFactory.createStackedBarChart("Per Packet Delays",
                "Packet", "ms", data, PlotOrientation.VERTICAL, true,
                false, false);
        chart.getCategoryPlot().getDomainAxis().setTickLabelsVisible(false);
        CategoryPlot plot = chart.getCategoryPlot();
        CategoryItemRenderer renderer = plot.getRenderer();
        for (int i = 0; i < data.getColumnCount(); i++) {
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

    private DefaultCategoryDataset fetchData() {
        //get last "numberOfResults" packets of a track
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        long fixed_ts = DB_TS_TIMEUNIT.convert(time, TimeUnit.MILLISECONDS);
        int numberOfResults = 150;
        int hops = 0;
        long minPacketId = 0;
        long maxPacketId = 0;
        String sql_query = "SELECT uid, start_ts FROM track_data "
                + "WHERE track_id = :trackId AND rule_id = :ruleId "
                + "AND stop_ts < :time ORDER BY start_ts DESC "
                + "LIMIT :limit";

        SqlQuery query = eServer.createSqlQuery(sql_query)
                .setParameter("trackId", flow.getBearer().getTrack().getTrackId())
                .setParameter("ruleId", flow.getBearer().getRuleId())
                .setParameter("time", fixed_ts)
                .setParameter("limit", numberOfResults);
        List<SqlRow> packetIds = query.findList();

        if (!packetIds.isEmpty()) {
            maxPacketId = packetIds.get(0).getLong("uid");
            minPacketId = packetIds.get(packetIds.size() - 1).getLong("uid");

            sql_query = "SELECT MAX(hop_number) as maxHop from "
                    + "raw_track_data WHERE track_id = :trackId "
                    + "AND rule_id = :ruleId AND source_address "
                    + "= :srcip AND source_port = :srcport AND "
                    + "destination_address = :dstip AND "
                    + "destination_port = :dstport";
            
            query = eServer.createSqlQuery(sql_query)
                    .setParameter("trackId", flow.getBearer().getTrack().getTrackId())
                    .setParameter("ruleId", flow.getBearer().getRuleId())
                    .setParameter("srcip", flow.getSrcIp())
                    .setParameter("srcport", flow.getSrcPort())
                    .setParameter("dstip", flow.getDstIp())
                    .setParameter("dstport", flow.getDstPort());
            List<SqlRow> hopNumber = query.findList();

            for (SqlRow hop : hopNumber) {
                hops = hop.getInteger("maxHop");
            }//for SqlRow hop	
            logger.debug("maxHop =" + hops);

            String[] names = new String[hops];
            for (int j = 0; j < names.length; j++) {
                String from, to;
                if (j < nodeNames.length) {
                    from = nodeNames[j];
                } else {
                    from = Integer.toString(j);
                }
                if ((j + 1) < nodeNames.length) {
                    to = nodeNames[j + 1];
                } else {
                    to = Integer.toString(j + 1);
                }
                names[j] = from + " -> " + to;
            }

            sql_query = "SELECT packet_id, record_id, delay, hop_number "
                    + "FROM raw_track_data WHERE track_id = :trackId AND "
                    + "rule_id = :ruleId AND source_address = :srcip AND "
                    + "source_port = :srcport AND destination_address = "
                    + ":dstip AND destination_port = :dstport AND record_id "
                    + "BETWEEN :minPacketId AND :maxPacketId ORDER BY "
                    + "record_id, hop_number ASC LIMIT :limit";

            query = eServer.createSqlQuery(sql_query)
                    .setParameter("trackId", flow.getBearer().getTrack().getTrackId())
                    .setParameter("ruleId", flow.getBearer().getRuleId())
                    .setParameter("srcip", flow.getSrcIp())
                    .setParameter("srcport", flow.getSrcPort())
                    .setParameter("dstip", flow.getDstIp())
                    .setParameter("dstport", flow.getDstPort())
                    .setParameter("maxPacketId", maxPacketId)
                    .setParameter("minPacketId", minPacketId)
                    .setParameter("limit", numberOfResults * hops);
            List<SqlRow> data = query.setFirstRow(1).findList();

            for (SqlRow delays : data) {
                int hopCount = delays.getInteger("hop_number");
                if (hopCount < hops) {
                    dataset.addValue(delays.getLong("delay") / 1000.,
                            names[hopCount], delays.getLong("record_id"));
                }
            }
        }
        return dataset;
    }
}
