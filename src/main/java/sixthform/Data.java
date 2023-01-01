package sixthform;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private Map<String,Map<String,Integer>> temperatureData = new TreeMap<>();
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
     * Returns map of YYYYMMDDhhss -> sensor -> temp
     */
    public Map<String, Map<String,Double>> GetTemperatureByTime(String startDate, String endDate)
    {
        // Expects date in format dd-mm-yyyy.
        Long filterStartDate = Long.parseLong(transformDate(startDate));
        Long filterEndDate = Long.parseLong(transformDate(endDate));

        Map<String, Map<String,Double>> temperatureByTime = new TreeMap<>();
        synchronized(temperatureData)
        {
            for (String timestamp : temperatureData.keySet())
            {
                // Filter by date.
                Long date = Long.parseLong(timestamp.substring(0,8));
                if (date >= filterStartDate && date <= filterEndDate)
                {
                    Map<String,Double> temperatures = new TreeMap<>();
                    for (String sensor : temperatureData.get(timestamp).keySet())
                    {
                        Double temp = temperatureData.get(timestamp).get(sensor).doubleValue() / 100.0;
                        temperatures.put(sensor,temp);
                    }

                    temperatureByTime.put(timestamp, temperatures);
                }
            }
        }
        return temperatureByTime;
    }

    /*
     * Returns map of hhss -> sensor -> temp
     */
    public Map<String, Map<String, HighLowAverage>> GetAverageTemperatureByTime(String startDate, String endDate)
    {
        // Expects date in format dd-mm-yyyy.
        Long filterStartDate = Long.parseLong(transformDate(startDate));
        Long filterEndDate = Long.parseLong(transformDate(endDate));

        Map<String, Map<String,List<Double>>> temperaturesByTime = new TreeMap<>();
        synchronized(temperatureData)
        {
            for (String timestamp : temperatureData.keySet())
            {
                // Filter by date.
                Long date = Long.parseLong(timestamp.substring(0,8));
                if (date >= filterStartDate && date <= filterEndDate)
                {
                    String time = timestamp.substring(8);

                    Map<String,List<Double>> sensorTemps = temperaturesByTime.containsKey(time) ? temperaturesByTime.get(time) : new TreeMap<>();
                    temperaturesByTime.put(time, sensorTemps);

                    for (String sensor : temperatureData.get(timestamp).keySet())
                    {
                        List<Double> temperatures = sensorTemps.containsKey(sensor) ? sensorTemps.get(sensor) : new ArrayList<>();
                        sensorTemps.put(sensor, temperatures);

                        Double temp = temperatureData.get(timestamp).get(sensor).doubleValue() / 100.0;
                        temperatures.add(temp);
                    }
                }
            }
        }
        
        Map<String, Map<String, HighLowAverage>> temperatureByTime = new TreeMap<>();
        for (String time : temperaturesByTime.keySet())
        {
            Map<String, HighLowAverage> timeSensorsHLA = new TreeMap<>();
            temperatureByTime.put(time, timeSensorsHLA);

            for (String sensor : temperaturesByTime.get(time).keySet())
            {
                Double high = Double.MIN_VALUE, low = Double.MAX_VALUE, average = 0.0;
                for (Double temp : temperaturesByTime.get(time).get(sensor))
                {
                    if (temp > high) high = temp;
                    if (temp < low) low = temp;
                    average += temp;
                }
                average = average / temperaturesByTime.get(time).get(sensor).size();

                timeSensorsHLA.put(sensor, new HighLowAverage(high, low, average));
            }
        }

        return temperatureByTime;
    }

    private void ProcessFile(Path path)
    {
        Map<String,Integer> records = new TreeMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path.toAbsolutePath().toString())))
        {
            String line;
            while ((line = br.readLine()) != null) 
            {
                String[] values = line.split(",");
                if (values.length == 2)
                    records.put(values[0].trim(),Integer.parseInt(values[1]));
            }
        }    
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }

        if (records.size() > 0)
        {
            String timestamp = path.getFileName().toString();
            timestamp = timestamp.substring(5,timestamp.length()-4);
            if (timestamp != null)
            {
                // Convert timestamp from YYYYMMDD-hh:mm:ss -> YYYYMMDDhhmm
                timestamp = timestamp.substring(0,8) + timestamp.substring(9,11) + timestamp.substring(12,14);
                synchronized(temperatureData)
                {
                    temperatureData.put(timestamp, records);
                }
            }
        }
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
