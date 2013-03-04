package counters;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockedCounter implements Counter
{
    private final Lock l = new ReentrantLock();
    private long value = 0;

    @Override
    public void increment()
    {
        l.lock();
        try
        {
            value++;
        }
        finally
        {
            l.unlock();
        }
    }

    @Override
    public long getValue()
    {
        return value;
    }

}
