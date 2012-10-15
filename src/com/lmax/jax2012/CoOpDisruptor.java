package com.lmax.jax2012;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.locks.LockSupport;

public class CoOpDisruptor implements Disruptor
{
    private static final int RETRIES = 1000;
    private final long[] data;
    private final int size;
    private final int mask;
    private final AtomicLong nextSequence = new AtomicLong(-1);
    private final AtomicLong cursor = new AtomicLong(-1);
    private final AtomicLong readSequence = new AtomicLong(-1);
    private final AtomicLongArray pendingPublication;
    private final int pendingMask;
    
    public CoOpDisruptor(int pendingSize, int size)
    {
        if (Integer.bitCount(size) != 1)
        {
            throw new IllegalArgumentException("Size must be power of 2, found: " + size);
        }
        this.data = new long[size];
        this.size = size;
        this.mask = size - 1;
        this.pendingPublication = new AtomicLongArray(pendingSize);
        this.pendingMask = pendingSize - 1;
    }
    
    public long next()
    {
        long next;
        long current;
        
        do
        {
            current = nextSequence.get();
            next = current + 1;
            while (next > (readSequence.get() + size))
            {
                LockSupport.parkNanos(1L);
                continue;
            }
        }
        while (!nextSequence.compareAndSet(current, next));
        
        return next;
    }
    
    public void setValue(long sequence, long value)
    {
        int index = indexOf(sequence);
        data[index] = value;
    }
    
    public void publish(long sequence) {
        int counter = RETRIES;
        while (sequence - cursor.get() > pendingPublication.length()) {
            if (--counter == 0) {
                Thread.yield();
                counter = RETRIES;
            }
        }

        long expectedSequence = sequence - 1;
        pendingPublication.set((int) sequence & pendingMask, sequence);

        if (cursor.get() >= sequence) { return; }

        long nextSequence = sequence;
        while (cursor.compareAndSet(expectedSequence, nextSequence)) {
            expectedSequence = nextSequence;
            nextSequence++;
            if (pendingPublication.get((int) nextSequence & pendingMask) != nextSequence) {
                break;
            }
        }
    }
    
    public void drain(EventHandler handler)
    {
        long available = cursor.get();
        
        for (long current = readSequence.get() + 1; current <= available; current++)
        {
            handler.onEvent(data[indexOf(current)], current == available);
        }
        
        readSequence.lazySet(available);
    }

    private int indexOf(long sequence)
    {
        return ((int) sequence) & mask;
    }
}
