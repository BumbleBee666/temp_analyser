package sixthform;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.jfree.chart.ui.UIUtils;

public class App
{
    private static Properties config = new Properties();

    public static void main( String[] args )
    {
        try
        {
            // Read configuration.
            ReadConfig(args);

            // Load data into database.
            Data data = null;
            if (config.getProperty("data.type").contentEquals("bandwidth"))
            {
                data = new BandwidthData(config);
            }
            else
            {
                data = new TemperatureData(config);
            }

            // Display the data.
            Chart chart = new Chart(config, data);
            chart.pack( );
            UIUtils.centerFrameOnScreen( chart );
            chart.setVisible( true );

            // Make sure the chart gets updates to the data.
            data.register(chart);
        }
        catch (Exception e)
        {
            System.out.println(e.getMessage());
        }
    }

    private static void ReadConfig(String[] args) throws Exception
    {
        // Read in config.
        String fileName = args[0] + "app.config";
        try (FileInputStream fis = new FileInputStream(fileName)) {
            config.load(fis);
        } catch (FileNotFoundException ex) {
            throw new Exception("Can't find app.config.");
        } catch (IOException ex) {
            throw new Exception("Can't load app.config.");
        }
    }

}
