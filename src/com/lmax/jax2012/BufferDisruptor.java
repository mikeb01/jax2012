package com.lmax.jax2012;

import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class BufferDisruptor implements Disruptor
{
    private final long[] data;
    private final int size;
    private final int mask;
    private final AtomicLong cursor = new AtomicLong(-1);
    private final AtomicLong readSequence = new AtomicLong(-1);
    private final AtomicIntegerArray published;
    private final int indexShift;
    
    public BufferDisruptor(int size)
    {
        if (Integer.bitCount(size) != 1)
        {
            throw new IllegalArgumentException("Size must be power of 2, found: " + size);
        }
        this.data = new long[size];
        this.size = size;
        this.mask = size - 1;
        this.published = new AtomicIntegerArray(size);
        this.indexShift = log2(size);
        
        for (int i = 0; i < size; i++)
        {
            published.set(i, -1);
        }
    }
    
    @Override
    public long next()
    {
        long next;
        long current;
        
        do
        {
            current = cursor.get();
            next = current + 1;
            while (next > (readSequence.get() + size))
            {
                LockSupport.parkNanos(1L);
                continue;
            }
        }
        while (!cursor.compareAndSet(current, next));
        
        return next;
    }
    
    @Override
    public void setValue(long sequence, long value)
    {
        int index = indexOf(sequence);
        data[index] = value;
    }
    
    @Override
    public void publish(long sequence)
    {
        int publishedValue = (int) (sequence >>> indexShift);
        published.set(indexOf(sequence), publishedValue);
    }
    
    @Override
    public void drain(EventHandler handler)
    {
        long available = cursor.get();
        
        for (long current = readSequence.get() + 1; current <= available; current++)
        {
            int availableValue = (int) (current >>> indexShift);
            int index = indexOf(current);
            while (published.get(index) != availableValue)
            {
                // Spin
            }
            
            handler.onEvent(data[index], current == available);
        }
        
        readSequence.lazySet(available);
    }

    private int indexOf(long sequence)
    {
        return ((int) sequence) & mask;
    }
    
    /**
     * Calculate the log base 2 of the supplied integer, essentially reports the location
     * of the highest bit.
     *
     * @param i Value to calculate log2 for.
     * @return The log2 value
     */
    public static int log2(int i)
    {
        int r = 0;
        while ((i >>= 1) != 0)
        {
            ++r;
        }
        return r;
    }
}
