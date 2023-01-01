package sixthform;

public class HighLowAverage
{
    private Double high, low, average;

    public HighLowAverage(Double high, Double low, Double average)
    {
        this.high = high;
        this.low = low;
        this.average = average;
    }
    public Double High() { return high; }
    public Double Low() { return low; }
    public Double Average() { return average; }
}
