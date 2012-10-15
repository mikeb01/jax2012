package com.lmax.jax2012;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

public class SpinMinusOneDisruptor implements Disruptor {
    private final long[] data;
    private final int size;
    private final int mask;
    private final AtomicLong nextSequence = new AtomicLong(-1);
    private final AtomicLong cursor = new AtomicLong(-1);
    private final AtomicLong readSequence = new AtomicLong(-1);

    public SpinMinusOneDisruptor(int size) {
        if (Integer.bitCount(size) != 1) {
            throw new IllegalArgumentException(
                    "Size must be power of 2, found: " + size);
        }
        this.data = new long[size];
        this.size = size;
        this.mask = size - 1;
    }

    public long next() {
        long next;
        long current;

        do {
            current = nextSequence.get();
            next = current + 1;
            while (next > (readSequence.get() + size)) {
                LockSupport.parkNanos(1L);
                continue;
            }
        } while (!nextSequence.compareAndSet(current, next));

        return next;
    } 

    public void setValue(long sequence, long value) {
        int index = indexOf(sequence);
        data[index] = value;
    }

    public void publish(long sequence) {
        long sequenceMinusOne = sequence - 1;
        while (cursor.get() != sequenceMinusOne) {
            // Spin
        }

        cursor.lazySet(sequence);
    }

    public void drain(EventHandler handler) {
        long available = cursor.get();

        for (long current = readSequence.get() + 1; current <= available; current++) {
            handler.onEvent(data[indexOf(current)], current == available);
        }

        readSequence.lazySet(available);
    }

    private int indexOf(long sequence) {
        return ((int) sequence) & mask;
    }
}
