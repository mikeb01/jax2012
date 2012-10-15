package com.lmax.jax2012;

public interface EventHandler
{
    void onEvent(long value, boolean endOfBatch);
}
