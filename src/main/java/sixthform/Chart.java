package sixthform;

import java.util.Calendar;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JFormattedTextField.AbstractFormatter;

import java.awt.FlowLayout;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.awt.event.ActionEvent;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.jdatepicker.impl.JDatePanelImpl;
import org.jdatepicker.impl.JDatePickerImpl;
import org.jdatepicker.impl.UtilDateModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.jfree.data.xy.AbstractXYDataset;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;

public class Chart extends ApplicationFrame implements TemperatureData.DataListener
{

   private Properties config;
   private Data data;

   private ChartPanel chartPanel;
   private boolean initialised = false;

   UtilDateModel startModel, endModel;
   JComboBox<String> chartType;
   
   public class DateLabelFormatter extends AbstractFormatter {

      private String datePattern = "dd/MM/yyyy";
      private SimpleDateFormat dateFormatter = new SimpleDateFormat(datePattern);
  
      @Override
      public Object stringToValue(String text) throws ParseException {
          return dateFormatter.parseObject(text);
      }
  
      @Override
      public String valueToString(Object value) throws ParseException {
          if (value != null) {
              Calendar cal = (Calendar) value;
              return dateFormatter.format(cal.getTime());
          }
  
          return "";
      }
  
   }
   
   public Chart(Properties config, Data data)
   {
      super(config.getProperty("chart.title"));

      this.config = config;
      this.data = data;

      String[] chartTypes = { "Average", "Week", "Today" };
      chartType = new JComboBox<>(chartTypes);
      chartType.setSelectedIndex(0);
      chartType.addActionListener((ActionEvent e) -> {
         onUpdate();
       });

      String today = LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));

      Properties p = new Properties();
      p.put("text.today","Today");
      p.put("text.month","Month");
      p.put("text.year","Year");

      startModel = new UtilDateModel();
      startModel.setDate(Integer.parseInt(today.substring(6)), Integer.parseInt(today.substring(3,5))-1, Integer.parseInt(today.substring(0,2)));
      startModel.setSelected(true);
      JDatePanelImpl startDatePanel=new JDatePanelImpl(startModel,p);
      JDatePickerImpl myStartDatePicker = new JDatePickerImpl(startDatePanel, new DateLabelFormatter());
      startDatePanel.addActionListener((ActionEvent e) -> {
         onUpdate();
      });

      endModel = new UtilDateModel();
      endModel.setDate(Integer.parseInt(today.substring(6)), Integer.parseInt(today.substring(3,5))-1, Integer.parseInt(today.substring(0,2)));
      endModel.setSelected(true);
      JDatePanelImpl endDatePanel=new JDatePanelImpl(endModel,p);
      JDatePickerImpl myEndDatePicker = new JDatePickerImpl(endDatePanel, new DateLabelFormatter());
      myEndDatePicker.addActionListener((ActionEvent e) -> {
         onUpdate();
      });

      JPanel controlsPanel = new JPanel();
      controlsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
      controlsPanel.add(chartType);
      controlsPanel.add(myStartDatePicker);
      controlsPanel.add(myEndDatePicker);

      JFreeChart chart = createChart();

      chartPanel = new ChartPanel( chart );
      chartPanel.setPreferredSize( new java.awt.Dimension( 1000 , 367 ) );

      JPanel mainPanel = new JPanel();
      mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
      mainPanel.add(controlsPanel);
      mainPanel.add(chartPanel);

      setContentPane( mainPanel );

      initialised = true;
   }

   private JFreeChart createChart()
   {
      JFreeChart chart = null;
      switch (chartType.getSelectedIndex())
      {
         case 0 :
            chart = ChartFactory.createHighLowChart(
               config.getProperty("chart.chartTitle"),
               config.getProperty("chart.h_title"),
               config.getProperty("chart.v_title"),
               (OHLCDataset)createDataset(),
               true);
            break;
         case 1,2 :
            chart = ChartFactory.createTimeSeriesChart(
               config.getProperty("chart.chartTitle"),
               config.getProperty("chart.h_title"),
               config.getProperty("chart.v_title"),
               (XYDataset)createDataset(),
               true,true,false);
      }
      return chart;
   }

   public void onUpdate()
   {
      if (initialised)
      {
         JFreeChart chart = createChart();
         if (chart != null)
         {
            chartPanel.setChart(chart);
         }
      }
   }

   private String startDate()
   {
      return String.format("%02d-%02d-%04d", startModel.getDay(), startModel.getMonth()+1, startModel.getYear());
   }

   private String endDate()
   {
      return String.format("%02d-%02d-%04d", endModel.getDay(), endModel.getMonth()+1, endModel.getYear());
   }

   private AbstractXYDataset createDataset( ) 
   {
      AbstractXYDataset dataset = null;
      switch (chartType.getSelectedIndex())
      {
         case 0 :
            {
               Map<String,Map<String,HighLowAverage>> temperatureData = data.GetAverageByTime(startDate(), endDate());
               if (temperatureData != null)
               {
                  OHLCSeriesCollection tsDataset = new OHLCSeriesCollection();
                  Map<String, OHLCSeries> seriesList = new TreeMap<>();
                  for (String timestamp : temperatureData.keySet())
                  {
                     Integer mm = Integer.parseInt(timestamp.substring(2,4));
                     Integer hh = Integer.parseInt(timestamp.substring(0,2));
                     Integer DD = 1;
                     Integer MM = 1;
                     Integer YYYY = 1900;
                     Minute minute = new Minute(mm, hh, DD, MM, YYYY);
                     for (String sensor : temperatureData.get(timestamp).keySet())
                     {
                        OHLCSeries series = seriesList.containsKey(sensor) ? seriesList.get(sensor) : new OHLCSeries(sensor);
                        seriesList.put(sensor, series);

                        HighLowAverage hla = temperatureData.get(timestamp).get(sensor);
                        series.add(minute, hla.Average(), hla.High(), hla.Low(), hla.Average());
                     }
                  }
                  for (OHLCSeries series : seriesList.values())
                  {
                     tsDataset.addSeries(series);
                  }
                  dataset = tsDataset;
               }
            }
            break;
         case 1 :
            {
               Map<String,Map<String,HighLowAverage>> temperatureData = data.GetAverageByDoWTime(startDate(), endDate());
               if (temperatureData != null)
               {
                  TimeSeriesCollection tsDataset = new TimeSeriesCollection();
                  Map<String, TimeSeries> seriesList = new TreeMap<>();
                  for (String timestamp : temperatureData.keySet())
                  {
                     Integer mm = Integer.parseInt(timestamp.substring(3,5));
                     Integer hh = Integer.parseInt(timestamp.substring(1,3));
                     Integer DD = Integer.parseInt(timestamp.substring(0,1));
                     Integer MM = 1;
                     Integer YYYY = 1900;
                     Minute minute = new Minute(mm, hh, DD, MM, YYYY);
                     for (String sensor : temperatureData.get(timestamp).keySet())
                     {
                        TimeSeries series = seriesList.containsKey(sensor) ? seriesList.get(sensor) : new TimeSeries(sensor);
                        seriesList.put(sensor, series);

                        series.add(minute, temperatureData.get(timestamp).get(sensor).Average());
                     }
                  }
                  for (TimeSeries series : seriesList.values())
                  {
                     tsDataset.addSeries(series);
                  }
                  dataset = tsDataset;
               }
            }
            break;
         case 2 :
            {
               Map<String,Map<String,Double>> temperatureData = data.GetByTime(startDate(), endDate());
               if (temperatureData != null)
               {
                  TimeSeriesCollection tsDataset = new TimeSeriesCollection();
                  Map<String, TimeSeries> seriesList = new TreeMap<>();
                  for (String timestamp : temperatureData.keySet())
                  {
                     Integer mm = Integer.parseInt(timestamp.substring(10,12));
                     Integer hh = Integer.parseInt(timestamp.substring(8,10));
                     Integer DD = Integer.parseInt(timestamp.substring(6,8));
                     Integer MM = Integer.parseInt(timestamp.substring(4,6));
                     Integer YYYY = Integer.parseInt(timestamp.substring(0,4));
                     Minute minute = new Minute(mm, hh, DD, MM, YYYY);
                     for (String sensor : temperatureData.get(timestamp).keySet())
                     {
                        TimeSeries series = seriesList.containsKey(sensor) ? seriesList.get(sensor) : new TimeSeries(sensor);
                        seriesList.put(sensor, series);

                        series.add(minute, temperatureData.get(timestamp).get(sensor));
                     }
                  }
                  for (TimeSeries series : seriesList.values())
                  {
                     tsDataset.addSeries(series);
                  }
                  dataset = tsDataset;
               }
            }
            break;
      }
      return dataset;
   }

}
