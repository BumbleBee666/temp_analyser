package sixthform;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class Data
{
    private Map<String,Path> processedFiles = new TreeMap<>();
    protected Map<String,Map<String,Double>> data = new TreeMap<>();
    private Properties config;

    public Data(Properties config)
    {
        this.config = config;

        // Setup a timer to run once a minute.
        Timer timer = new Timer("Timer");
        long delay = 0L;
        long period = 60000L;
        timer.schedule(task, delay, period);
    }

    public interface DataListener
    {
        public void onUpdate();
    }
    private List<DataListener> listeners = new ArrayList<>();
    public void register(DataListener listener)
    {
        synchronized(listeners)
        {
            listeners.add(listener);
        }
    }

    private TimerTask task = new TimerTask()
    {
        public void run()
        {
            // We need to look for new files.
            Map<String,Path> newFiles = GetNewFiles();

            // Process any new files.
            for (Path path : newFiles.values())
            {
                ProcessFile(path);
            }

            // Tell listeners of any update.
            if (!newFiles.isEmpty())
            {
                synchronized(listeners)
                {
                    for (DataListener listener: listeners)
                    {
                        listener.onUpdate();
                    }
                }
            }
        }
    };

    private String transformDate(String date)
    {
        return date.substring(6) + date.substring(3,5) + date.substring(0,2);
    }

    /*
        * Returns map of YYYYMMDDhhss -> measure -> value
        */
    public Map<String, Map<String,Double>> GetByTime(String startDate, String endDate)
    {
        // Expects date in format dd-mm-yyyy.
        Long filterStartDate = Long.parseLong(transformDate(startDate));
        Long filterEndDate = Long.parseLong(transformDate(endDate));

        Map<String, Map<String,Double>> measuresByTime = new TreeMap<>();
        synchronized(data)
        {
            for (String timestamp : data.keySet())
            {
                // Filter by date.
                Long date = Long.parseLong(timestamp.substring(0,8));
                if (date >= filterStartDate && date <= filterEndDate)
                {
                    Map<String,Double> seriesByMeasure = new TreeMap<>();
                    for (String measure : data.get(timestamp).keySet())
                    {
                        Double value = data.get(timestamp).get(measure).doubleValue();
                        seriesByMeasure.put(measure,value);
                    }

                    measuresByTime.put(timestamp, seriesByMeasure);
                }
            }
        }

        return measuresByTime;
    }

    /*
        * Returns map of hhss -> measure -> value
        */
    public Map<String, Map<String, HighLowAverage>> GetAverageByTime(String startDate, String endDate)
    {
        // Expects date in format dd-mm-yyyy.
        Long filterStartDate = Long.parseLong(transformDate(startDate));
        Long filterEndDate = Long.parseLong(transformDate(endDate));

        Map<String, Map<String,List<Double>>> measuresByTime = new TreeMap<>();
        synchronized(data)
        {
            for (String timestamp : data.keySet())
            {
                // Filter by date.
                Long date = Long.parseLong(timestamp.substring(0,8));
                if (date >= filterStartDate && date <= filterEndDate)
                {
                    String time = timestamp.substring(8);

                    Map<String,List<Double>> sensorTemps = measuresByTime.containsKey(time) ? measuresByTime.get(time) : new TreeMap<>();
                    measuresByTime.put(time, sensorTemps);

                    for (String sensor : data.get(timestamp).keySet())
                    {
                        List<Double> temperatures = sensorTemps.containsKey(sensor) ? sensorTemps.get(sensor) : new ArrayList<>();
                        sensorTemps.put(sensor, temperatures);

                        Double temp = data.get(timestamp).get(sensor).doubleValue() / 100.0;
                        temperatures.add(temp);
                    }
                }
            }
        }
        
        Map<String, Map<String, HighLowAverage>> averageByTime = new TreeMap<>();
        for (String time : measuresByTime.keySet())
        {
            Map<String, HighLowAverage> timeHLA = new TreeMap<>();
            averageByTime.put(time, timeHLA);

            for (String measure : measuresByTime.get(time).keySet())
            {
                Double high = Double.MIN_VALUE, low = Double.MAX_VALUE, average = 0.0;
                for (Double value : measuresByTime.get(time).get(measure))
                {
                    if (value > high) high = value;
                    if (value < low) low = value;
                    average += value;
                }
                average = average / measuresByTime.get(time).get(measure).size();

                timeHLA.put(measure, new HighLowAverage(high, low, average));
            }
        }

        return averageByTime;
    }

    /*
        * Returns map of hhss -> sensor -> temp
        */
    public Map<String, Map<String, HighLowAverage>> GetAverageByDoWTime(String startDate, String endDate)
    {
        // Expects date in format dd-mm-yyyy.
        Long filterStartDate = Long.parseLong(transformDate(startDate));
        Long filterEndDate = Long.parseLong(transformDate(endDate));

        Map<String, Map<String,List<Double>>> measuresByTime = new TreeMap<>();
        synchronized(data)
        {
            for (String timestamp : data.keySet())
            {
                // Filter by date.
                Long date = Long.parseLong(timestamp.substring(0,8));
                if (date >= filterStartDate && date <= filterEndDate)
                {
                    String time = timestamp.substring(8);
                    LocalDate localDate = LocalDate.of(Integer.parseInt(timestamp.substring(0, 4)), Integer.parseInt(timestamp.substring(4, 6)), Integer.parseInt(timestamp.substring(6,8)));
                    time = localDate.getDayOfWeek().getValue() + time;

                    Map<String,List<Double>> sensorTemps = measuresByTime.containsKey(time) ? measuresByTime.get(time) : new TreeMap<>();
                    measuresByTime.put(time, sensorTemps);

                    for (String sensor : data.get(timestamp).keySet())
                    {
                        List<Double> temperatures = sensorTemps.containsKey(sensor) ? sensorTemps.get(sensor) : new ArrayList<>();
                        sensorTemps.put(sensor, temperatures);

                        Double temp = data.get(timestamp).get(sensor).doubleValue() / 100.0;
                        temperatures.add(temp);
                    }
                }
            }
        }
        
        Map<String, Map<String, HighLowAverage>> averageByTime = new TreeMap<>();
        for (String time : measuresByTime.keySet())
        {
            Map<String, HighLowAverage> timeSensorsHLA = new TreeMap<>();
            averageByTime.put(time, timeSensorsHLA);

            for (String sensor : measuresByTime.get(time).keySet())
            {
                Double high = Double.MIN_VALUE, low = Double.MAX_VALUE, average = 0.0;
                for (Double temp : measuresByTime.get(time).get(sensor))
                {
                    if (temp > high) high = temp;
                    if (temp < low) low = temp;
                    average += temp;
                }
                average = average / measuresByTime.get(time).get(sensor).size();

                timeSensorsHLA.put(sensor, new HighLowAverage(high, low, average));
            }
        }

        return averageByTime;
    }

    protected void ProcessFile(Path path)
    {
    }

    private Map<String,Path> GetNewFiles()
    {
        Map<String,Path> newFiles = new TreeMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(config.getProperty("datafiles.location"))))
        {
            for (Path path : stream)
            {
                String filename = path.getFileName().toString();
                if (!Files.isDirectory(path) && filename.matches(config.getProperty("datafiles.filter")) && !processedFiles.containsKey(filename))
                {
                    newFiles.put(filename, path);
                }
            }
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
        return newFiles;
    }
}
