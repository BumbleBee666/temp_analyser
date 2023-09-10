package sixthform;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

public class TemperatureData extends Data
{
    public TemperatureData(Properties config)
    {
        super(config);
    }

    @Override
    protected void ProcessFile(Path path)
    {
        Map<String,Double> records = new TreeMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path.toAbsolutePath().toString())))
        {
            String line;
            while ((line = br.readLine()) != null) 
            {
                String[] values = line.split(",");
                if (values.length == 2)
                    records.put(values[0].trim(), Integer.parseInt(values[1]) / 100.0);
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
                synchronized(data)
                {
                    data.put(timestamp, records);
                }
            }
        }
    }
}
