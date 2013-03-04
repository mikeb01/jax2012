package counters;

public class SynchronizedCounter implements Counter
{
    private long value;
    private final Object mutex = new Object();

    @Override
    public void increment()
    {
        synchronized (mutex)
        {
            value++;
        }
    }

    @Override
    public long getValue()
    {
        return value;
    }
}
