package com.lmax.jax2012;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class DisruptorBenchmark
{
    private static final int BUFFER_SIZE = 8 * 1024;
    private final Disruptor disruptor;

    public DisruptorBenchmark(Disruptor disruptor)
    {
        this.disruptor = disruptor;
    }
    
    public void runBenchmark(int runNumber, int numProducers, int numIterations) throws InterruptedException, BrokenBarrierException
    {
        int totalIteration = numProducers * numIterations;
        
        CyclicBarrier barrier = new CyclicBarrier(numProducers + 2);
        Consumer c = new Consumer(disruptor, totalIteration, barrier);
        Producer[] ps = new Producer[numProducers];
        
        for (int i = 0; i < ps.length; i++)
        {
            ps[i] = new Producer(disruptor, numIterations, barrier);
        }
        
        Thread[] ts = new Thread[numProducers + 1];
        
        ts[0] = new Thread(c);
        ts[0].setName("Consumer");
        for (int i = 1; i < ts.length; i++)
        {
            int producerNum = i-1;
            ts[i] = new Thread(ps[producerNum]);
            ts[i].setName("Producer-" + producerNum);
        }
        
        for (Thread t : ts)
        {
            t.start();
        }
        
        barrier.await();
        
        long t0 = System.nanoTime();
        
        for (Thread t: ts)
        {
            t.join();
        }
        
        long t1 = System.nanoTime();
     
        writeResults(runNumber, t1-t0, totalIteration, numProducers, 1, true, disruptor.getClass().getSimpleName());
    }
    
    public static void writeResults(final int runNumber,
                                    final long durationMs,
                                    final int repetitions,
                                    final int numProducers,
                                    final int numConsumers,
                                    final boolean batched,
                                    final String testName)
    {
        final long opsPerSec = (1000000000L * repetitions * numProducers) / durationMs;
        //String output = String.format("%d|%d producers|%d consumers|%,d ops/sec|smart batching %b|%s",
        String output = String.format("`runs insert (%d;%d;%,d;%,d;`%s)",
                                      Integer.valueOf(runNumber),
                                      Integer.valueOf(numProducers),
                                      Long.valueOf(repetitions),
                                      Long.valueOf(opsPerSec),
                                      testName);

        System.out.println(output);
    }
    
    private static class Consumer implements Runnable, EventHandler
    {
        private final Disruptor disruptor;
        private final int iterations;
        private final CyclicBarrier barrier;
        private int counter;

        public Consumer(Disruptor disruptor, int iterations, CyclicBarrier barrier)
        {
            this.disruptor = disruptor;
            this.iterations = iterations;
            this.barrier = barrier;
        }
        
        @Override
        public void run()
        {
            int iterations = this.iterations;
            Disruptor disruptor = this.disruptor;
            
            try
            {
                barrier.await();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return;
            }

            while (counter != iterations)
            {
                disruptor.drain(this);
            }
            
//            System.out.println("Consumed: " + iterations);
        }

        @Override
        public void onEvent(long value, boolean endOfBatch)
        {
            counter++;
        }
    }
    
    private static class Producer implements Runnable
    {
        private final Disruptor disruptor;
        private final int iterations;
        private final CyclicBarrier barrier;

        public Producer(Disruptor disruptor, int iterations, CyclicBarrier barrier)
        {
            this.disruptor = disruptor;
            this.iterations = iterations;
            this.barrier = barrier;
        }
        
        @Override
        public void run()
        {
            int iterations = this.iterations;
            Disruptor disruptor = this.disruptor;
            
            try
            {
                barrier.await();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return;
            }
            
            for (int i = 0; i < iterations; i++)
            {
                publish(disruptor, i);
            }
            
//            System.out.println("Published: " + iterations);
        }
        
        private void publish(Disruptor disruptor, long value)
        {
            long next = disruptor.next();
            disruptor.setValue(next, value);
            disruptor.publish(next);
        }
    }
    
    public static void main(String[] args) throws InterruptedException, BrokenBarrierException
    {
        if (args.length != 4)
        {
            System.err.println("Usage: DisruptorBenchmark <runs> <iterations> <threads> <name>");
            System.exit(-1);
        }
        
        int runs = Integer.parseInt(args[0]);
        int maxIterations = Integer.parseInt(args[1]);
        int threads = Integer.parseInt(args[2]);
        
        int iterations = maxIterations;
        for (int i = 0; i < runs; i++)
        {            
            DisruptorBenchmark benchmark = new DisruptorBenchmark(getDisruptor(args[3]));
            benchmark.runBenchmark(i, threads, iterations);
        }
    }

    private static Disruptor getDisruptor(String string)
    {
        switch (string)
        {
        case "danny":
            return new SpinMinusOneDisruptor(BUFFER_SIZE);
        case "mike":
            return new CoOpDisruptor(2048, BUFFER_SIZE);
        case "low":
            return new BufferDisruptor(BUFFER_SIZE);
        default:
            throw new IllegalStateException(string);
        }
    }
}
