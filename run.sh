#!/bin/bash

# for i in {1..4}
# do
#     java -cp bin com.lmax.jax2012.DisruptorBenchmark 5 20000000 $i danny
#     java -cp bin com.lmax.jax2012.DisruptorBenchmark 5 20000000 $i mike
#     java -cp bin com.lmax.jax2012.DisruptorBenchmark 5 20000000 $i low
# done

for i in {5..8}
do
    # java -cp bin com.lmax.jax2012.DisruptorBenchmark 5 2000000 $i danny
    java -cp bin com.lmax.jax2012.DisruptorBenchmark 5 2000000 $i mike
    java -cp bin com.lmax.jax2012.DisruptorBenchmark 5 2000000 $i low
done
