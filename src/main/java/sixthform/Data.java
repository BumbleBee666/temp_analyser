package sixthform;

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

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Data
{
    private Map<String,Path> processedFiles = new TreeMap<>();
    private Map<String,JSONObject> bandwidthData = new TreeMap<>();
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

    public Map<String, Long> GetAverageBandwidthByTime()
    {
        // Organise data into buckets by time.
        Map<String, List<Long>> bandwidthByTime = new TreeMap<>();
        synchronized(bandwidthData)
        {
            for (String timestamp : bandwidthData.keySet())
            {
                JSONObject download = (JSONObject) bandwidthData.get(timestamp).get("download");
                Long bandwidth = ((Long) download.get("bandwidth")) / 125000L;

                String time = timestamp.substring(8);
                List<Long> bandwidthsForTime = bandwidthByTime.containsKey(time) ? bandwidthByTime.get(time) : new ArrayList<Long>();
                bandwidthsForTime.add(bandwidth);
                bandwidthByTime.put(time, bandwidthsForTime);
            }
        }

        // Calculate average for each time bucket.
        Map<String, Long> averageBandwidthByTime = new TreeMap<>();
        for (String time : bandwidthByTime.keySet())
        {
            Long bandwidthTotal = 0L;
            for (Long bandwidth : bandwidthByTime.get(time))
                bandwidthTotal += bandwidth;
            averageBandwidthByTime.put(time, bandwidthTotal / bandwidthByTime.get(time).size());
        }
        return averageBandwidthByTime;
    }

    private void ProcessFile(Path path)
    {
        JSONParser parser = new JSONParser();
        try
        {
            JSONObject data = (JSONObject) parser.parse(new FileReader(path.toAbsolutePath().toString()));
            if (data != null)
            {
                String timestamp = (String) data.get("timestamp");
                if (timestamp != null)
                {
                    timestamp = timestamp.substring(0,4) + timestamp.substring(5,7) + timestamp.substring(8,10) + timestamp.substring(11,13) + timestamp.substring(14,16);
                    synchronized(bandwidthData)
                    {
                        bandwidthData.put(timestamp, data);
                    }
                }
            }
        }
        catch (ParseException e)
        {
            // We expect these to occur where there is no result in the file.
        }
        catch (IOException e) 
        {
            System.out.println(e.getMessage());
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
