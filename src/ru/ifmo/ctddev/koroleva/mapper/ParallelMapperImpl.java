package ru.ifmo.ctddev.koroleva.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

/**
 * This class helps to calculate function on list of elements in parellel.
 * Provides one and more different requests.
 *
 * @author KorolevaYana
 */
public class ParallelMapperImpl implements ParallelMapper {

    private class ClosedException extends RuntimeException {
        public ClosedException() {
            super();
        }
    }

    private final Queue<Runnable> queue;

    private int threadCount;
    private Thread[] threads;
    private boolean isClosed = false;


    /**
     * Constructor that makes <tt>threadCount</tt> threads and starts them.
     * Threads take tasks from queue if it's not empty.
     *
     * @param threadCount count of threads that should make calculations.
     */
    public ParallelMapperImpl(int threadCount) {
        queue = new LinkedList<>();
        this.threadCount = threadCount;
        threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            threads[i] = new Thread(
                    () -> {
                        try {
                            while (true) {
                                Runnable r;
                                try {
                                    synchronized (queue) {
                                        while (queue.isEmpty())
                                            queue.wait();
                                        r = queue.remove();
                                    }
                                    r.run();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (ClosedException ignored) {
                        }
                    }
            );
            threads[i].start();
        }
    }


    /**
     * Add tasks to calculate <tt>function</tt> on <tt>list</tt> elements.
     *
     * @param function function to calculate.
     * @param list     elements, on which function should be calculated.
     * @param <T>      type of elements of list.
     * @param <R>      type of result of function.
     * @return list of results of calculating function on elements of list.
     * @throws InterruptedException  if something was interrupted.
     * @throws IllegalStateException if there is a try to add something to queue after closing.
     */
    @Override
    public <T, R> List<R> map(Function<? super T, ? extends R> function, List<? extends T> list)
            throws InterruptedException {
        synchronized (this) {
            if (isClosed) {
                throw new IllegalStateException("ParallelMapperImpl was already closed");
            }
        }
        int cnt = Math.min(threadCount, list.size());
        @SuppressWarnings("unchecked")
        R[] results = (R[]) new Object[list.size()];


        final Integer[] counter = {0};
        for (int j = 0; j < cnt - 1; j++) {
            final int jj = j;
            synchronized (queue) {
                queue.add(() -> {
                    for (int i = jj * (list.size() / cnt); i < (jj + 1) * (list.size() / cnt); i++) {
                        results[i] = function.apply(list.get(i));
                    }
                    synchronized (counter) {
                        counter[0]++;
                        counter.notify();
                    }
                });
                queue.notify();
            }
        }
        synchronized (queue) {
            queue.add(() -> {
                for (int i = (list.size() / cnt) * (cnt - 1); i < list.size(); i++) {
                    results[i] = function.apply(list.get(i));
                }
                synchronized (counter) {
                    counter[0]++;
                    counter.notify();
                }
            });
            queue.notify();
        }

        synchronized (counter) {
            while (counter[0] < cnt) {
                counter.wait();
            }
        }
        return Arrays.asList(results);
    }


    /**
     * Close all threads.
     * Wait for executing current tasks and stop threads then.
     *
     * @throws InterruptedException if something was interrupted.
     */
    @Override
    public void close() throws InterruptedException {
        synchronized (this) {
            if (!isClosed) {
                isClosed = true;
                for (int i = 0; i < threadCount; i++) {
                    synchronized (queue) {
                        queue.add(() -> {
                            throw new ClosedException();
                        });
                        queue.notify();
                    }
                }
            }
        }
    }
}
