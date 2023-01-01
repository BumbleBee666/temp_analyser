package sixthform;

import java.util.Map;
import java.util.Properties;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

public class Chart extends ApplicationFrame implements Data.DataListener
{

   private Properties config;
   private Data data;

   private ChartPanel chartPanel;
   private boolean initialised = false;

   public Chart(Properties config, Data data)
   {
      super(config.getProperty("chart.title"));

      this.config = config;
      this.data = data;

      JFreeChart lineChart = ChartFactory.createTimeSeriesChart(
         config.getProperty("chart.chartTitle"),
         config.getProperty("chart.h_title"),
         config.getProperty("chart.v_title"),
         createDataset(),
         true,true,false);

      chartPanel = new ChartPanel( lineChart );
      chartPanel.setPreferredSize( new java.awt.Dimension( 1000 , 367 ) );

      setContentPane( chartPanel );

      initialised = true;
   }

   public void onUpdate()
   {
      if (initialised)
      {
         JFreeChart lineChart = ChartFactory.createTimeSeriesChart(
            config.getProperty("chart.chartTitle"),
            config.getProperty("chart.h_title"),
            config.getProperty("chart.v_title"),
            createDataset(),
            true,true,false);

         chartPanel.setChart(lineChart);
      }
   }

   private XYDataset createDataset( ) 
   {
      TimeSeries series1 = new TimeSeries("Average Bandwidth");
      Map<String,Long> bandwidthData = data.GetAverageBandwidthByTime();
      for (String timestamp : bandwidthData.keySet())
      {
         Minute minute = new Minute(Integer.parseInt(timestamp.substring(2,4)), Integer.parseInt(timestamp.substring(0,2)), 1, 1, 1900);
         series1.add(minute, bandwidthData.get(timestamp).doubleValue());
      }
      return new TimeSeriesCollection(series1);
   }

}
