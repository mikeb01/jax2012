package com.lmax.jax2012;

public interface Disruptor
{

    public abstract long next();

    public abstract void setValue(long sequence, long value);

    public abstract void publish(long sequence);

    public abstract void drain(EventHandler handler);

}