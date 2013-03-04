package counters;

import java.nio.channels.IllegalSelectorException;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class CounterRunner implements Runnable
{
    private CountDownLatch stopLatch;
    private CyclicBarrier startBarrier;
    private long iterations;
    private Counter counter;

    @Override
    public void run()
    {
        try
        {
            startBarrier.await();
            
            for (long l = 0; l < iterations; l++)
            {
                counter.increment();
            }
            
            stopLatch.countDown();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    public void setAll(Counter counter, long iteartions, CyclicBarrier startBarrier, CountDownLatch stopLatch)
    {
        this.counter = counter;
        this.iterations = iteartions;
        this.startBarrier = startBarrier;
        this.stopLatch = stopLatch;
    }
    
    public static void main(String[] args) throws Exception
    {
        RunnerType type = RunnerType.valueOf(args[0]);
        int threads = Integer.parseInt(args[1]);
        long iterations = Long.parseLong(args[2]);
        
        ExecutorService executor = Executors.newCachedThreadPool();
        
        Counter counter = type.newInstance();
        
        CounterRunner[] runners = new CounterRunner[threads];
        
        for (int i = 0; i < runners.length; i++)
        {
            runners[i] = new CounterRunner();
        }
        
        for (int i = 0; i < 5; i++)
        {
            double timeTaken = run(executor, threads, iterations, counter, runners);
            System.out.printf("Ops/Sec: %,.2f%n", ((iterations*threads) * 1000.0)/timeTaken);
        }
        
        executor.shutdown();
    }
    
    private static long run(ExecutorService executor, int threads, long iterations, Counter counter, CounterRunner[] runners) throws InterruptedException, BrokenBarrierException
    {
        CyclicBarrier barrier = new CyclicBarrier(threads + 1);
        CountDownLatch latch = new CountDownLatch(threads);
        
        for (CounterRunner counterRunner : runners)
        {
            counterRunner.setAll(counter, iterations, barrier, latch);
            executor.execute(counterRunner);
        }

        long t0 = System.currentTimeMillis();
        barrier.await();
        latch.await();
        long t1 = System.currentTimeMillis();
        
        long value = counter.getValue();
        if (value == 0)
        {
            throw new IllegalStateException();
        }
        
        return t1-t0;
    }
    
    private enum RunnerType implements Factory<Counter>
    {
        ATOMIC, SYNC, THREAD_LOCAL, LOCKED;
        
        @Override
        public Counter newInstance()
        {
            switch (this)
            {
                case ATOMIC:
                    return new AtomicCounter();
                case SYNC:
                    return new SynchronizedCounter();
                case THREAD_LOCAL:
                    return new ThreadLocalCounter();
                case LOCKED:
                    return new LockedCounter();
                default:
                    throw new IllegalStateException(this.toString());
            }
        }
    }
}
