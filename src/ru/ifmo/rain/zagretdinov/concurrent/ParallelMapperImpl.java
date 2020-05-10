package ru.ifmo.rain.zagretdinov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

import static java.util.stream.IntStream.range;

/**
 * Class which provides methods to perform queueTasks with created threads. Adds new queueTasks in common pool and
 * Next free {@link Thread} is going to perform it.
 *
 * @author sem
 * @version 1.0.0
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final QueueTasks queueTasks;
    private final List<Thread> threadList;

    /**
     * Constructor taking amount of threads as parameter. Starts every {@link Thread} waiting for queueTasks in common
     * {@link ArrayDeque}
     *
     * @param threads amount of threads needed to be created.
     */
    public ParallelMapperImpl(final int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Threads number should not be negative");
        }
        queueTasks = new QueueTasks();
        threadList = new ArrayList<>();
        Runnable TASK = () -> {
            try {
                while (!Thread.interrupted()) {
                    queueTasks.poll().run();
                }
            } catch (final InterruptedException e) {
                // ignore
            } finally {
                Thread.currentThread().interrupt();
            }
        };
        range(0, threads).forEach(i -> threadList.add(new Thread(TASK)));
        threadList.forEach(Thread::start);
    }

    public class QueueTasks {
        private final Deque<CounterList<?, ?>> counterLists;

        QueueTasks() {
            counterLists = new ArrayDeque<>();
        }

        synchronized void add(final CounterList<?, ?> value) {
            counterLists.add(value);
            notifyAll();
        }

        public synchronized void remove() {
            counterLists.remove();
        }

        synchronized Runnable poll() throws InterruptedException {
            while (isEmpty()) {
                wait();
            }
            return counterLists.element().poll();
        }

        private synchronized boolean isEmpty() {
            return counterLists.isEmpty();
        }
    }

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args)
            throws InterruptedException {
        CounterList<T, R> result = new CounterList<>(f, args);

        synchronized (this) {
            queueTasks.add(result);
        }

        return result.getResult();
    }

    @Override
    public void close() {

        synchronized (this) {
            threadList.forEach(Thread::interrupt);
            queueTasks.counterLists.forEach(CounterList::finish);
        }

        threadList.forEach(t -> {
            while (true) {
                try {
                    t.join();
                    break;
                } catch (final InterruptedException ignored) {
                }
            }
        });
    }

    public class CounterList<E, Q> {
        private final List<Q> result;
        private final Deque<Runnable> subtasks;
        private int remain, emptyCounterList;
        private boolean finishes;
        private RuntimeException e;

        CounterList(Function<? super E, ? extends Q> f, List<? extends E> args) {
            result = new ArrayList<>(Collections.nCopies(args.size(), null));
            subtasks = new ArrayDeque<>();
            remain = emptyCounterList = result.size();

            finishes = false;
            int idx = 0;
            for (final E value : args) {
                final int index = idx++;
                subtasks.add(() -> {
                    try {
                        setResult(index, f.apply(value));
                    } catch (final RuntimeException e) {
                        addError(e);
                    }
                });
            }
        }

        synchronized Runnable poll() throws InterruptedException {
            while (subtasks.isEmpty()) {
                wait();
            }
            var task = subtasks.poll();
            if (--emptyCounterList == 0) {
                queueTasks.remove();
            }
            return task;
        }

        synchronized void setResult(final int index, Q value) {
            result.set(index, value);
            if (--remain == 0) {
                finishes = true;
                finish();
            }
        }

        synchronized void addError(final RuntimeException error) {
            if (e == null) {
                e = error;
            } else {
                e.addSuppressed(error);
            }
            --remain;
        }

        private synchronized void finishWaiter() throws InterruptedException {
            while (!finishes) {
                wait();
            }
        }

        public synchronized RuntimeException getError() throws InterruptedException {
            finishWaiter();
            return e;
        }

        public synchronized List<Q> getResult() throws InterruptedException {
            finishWaiter();
            if (emptyCounterList != 0) {
                return null;
            }
            if (e != null) {
                throw e;
            }

            return result;
        }

        synchronized void finish() {
            finishes = true;
            notifyAll();
        }
    }

}