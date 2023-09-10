package sixthform;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class BandwidthData extends Data
{
    public BandwidthData(Properties config)
    {
        super(config);
    }

    @Override
    protected void ProcessFile(Path path)
    {
        Map<String,Double> records = new TreeMap<>();
/*
        {
            "type":"result",
            "timestamp":"2023-03-12T16:10:23Z",
            "ping":
            {
                "jitter":0.579,
                "latency":6.750,
                "low":6.656,
                "high":7.742
            },
            "download":
            {
                "bandwidth":8587890,
                "bytes":122603608,
                "elapsed":15004,
                "latency":
                {
                    "iqm":16.723,
                    "low":6.713,
                    "high":370.428,
                    "jitter":8.741
                }
            },
            "upload":
            {
                "bandwidth":1795238,
                "bytes":8760400,
                "elapsed":5030,
                "latency":
                {
                    "iqm":8.773,
                    "low":7.279,
                    "high":236.002,
                    "jitter":5.360
                }
            },
            "packetLoss":0,
            "isp":"TalkTalk",
            "interface":
            {
                "internalIp":"192.168.0.34",
                "name":"eth0",
                "macAddr":"DC:A6:32:E0:8D:5B",
                "isVpn":false,
                "externalIp":"2.102.55.168"
            },
            "server":
            {
                "id":36718,
                "host":"speedtest.giga.net.uk",
                "port":8080,
                "name":"Giganet",
                "location":"Portsmouth",
                "country":"United Kingdom",
                "ip":"37.48.224.34"
            },
            "result":
            {
                "id":"ea04faa9-7b02-48c6-982e-ead9ca4b6446",
                "url":"https://www.speedtest.net/result/c/ea04faa9-7b02-48c6-982e-ead9ca4b6446",
                "persisted":true
            }
        }
*/

        JSONParser parser = new JSONParser();
        try
        {
            JSONObject jsonObject = (JSONObject) parser.parse(new FileReader(path.toAbsolutePath().toString()));
            if (data != null)
            {
                JSONObject download = (JSONObject) jsonObject.get("download");
                if (download != null)
                {
                    Long bandwidth = (Long) download.get("bandwidth");
                    if (bandwidth != null)
                    {
                        Double dBandwidth = bandwidth.doubleValue() / 125000.0;
                        records.put("download", dBandwidth);
                    }
                    JSONObject latency = (JSONObject) download.get("latency");
                    if (latency != null)
                    {
                        Double jitter = (Double) latency.get("jitter");
                        records.put("jitter", jitter);
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

        if (records.size() > 0)
        {
            String timestamp = path.getFileName().toString();
            timestamp = timestamp.substring(8,timestamp.length()-5);
            if (timestamp != null)
            {
                // Convert timestamp from YYYYMMDD-hhmm -> YYYYMMDDhhmm
                timestamp = timestamp.substring(0,8) + timestamp.substring(9,13);
                synchronized(data)
                {
                    data.put(timestamp, records);
                }
            }
        }
    }
}
