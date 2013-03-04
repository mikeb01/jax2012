package counters;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadLocalCounter implements Counter
{   
    ConcurrentLinkedQueue<AtomicLong> values = new ConcurrentLinkedQueue<AtomicLong>();

    private final ThreadLocal<AtomicLong> counterLocal = new ThreadLocal<AtomicLong>()
    {
        @Override
        protected AtomicLong initialValue()
        {
            AtomicLong value = new AtomicLong();
            values.add(value);
            
            return value;
        }
    };

    @Override
    public void increment()
    {
        AtomicLong atomicLong = counterLocal.get();
        long l = atomicLong.get();
        atomicLong.lazySet(l + 1);
    }

    @Override
    public long getValue()
    {
        long l = 0;
        
        for (AtomicLong value : values)
        {
            l += value.get();
        }
        
        return l;
    }
}
