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

        synchronized void add(CounterList<?, ?> value) {
            counterLists.add(value);
            notifyAll();
        }

        synchronized CounterList<?, ?> poll() throws InterruptedException {
            while (isEmpty()) {
                wait();
            }
            return counterLists.poll();
        }

        boolean isEmpty() {
            return counterLists.isEmpty();
        }

    }

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args)
            throws InterruptedException {
        CounterList<T, R> result = new CounterList<>(f, args);
        queueTasks.add(result);

        if (result.hasErrors()) {
            throw result.getError();
        } else {
            return result.getResult();
        }
    }

    @Override
    public void close() {
        threadList.forEach(Thread::interrupt);

        synchronized (this) {
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
        private int remain;
        private boolean finishes;
        private RuntimeException e = null;

        CounterList(Function<? super E, ? extends Q> f, List<? extends E> args) {
            result = new ArrayList<>(Collections.nCopies(args.size(), null));
            subtasks = new ArrayDeque<>();
            remain = result.size();
            finishes = false;
            int idx = 0;
            for (final E value : args) {
                final int index = idx++;
                synchronized (this) {
                    Runnable task = () -> {
                        try {
                            setResult(index, f.apply(value));
                        } catch (final RuntimeException e) {
                            addError(e);
                        }
                    };
                    add(task);
                }
            }
        }

        synchronized void run() throws InterruptedException {
            while (!subtasks.isEmpty()) {
                poll().run();
            }
        }

        synchronized void add(Runnable value) {
            subtasks.add(value);
            notifyAll();
        }

        synchronized Runnable poll() throws InterruptedException {
            while (subtasks.isEmpty()) {
                wait();
            }
            return subtasks.poll();
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

        private synchronized void waitForFinish() throws InterruptedException {
            while (!finishes) {
                wait();
            }
        }

        synchronized boolean hasErrors() throws InterruptedException {
            waitForFinish();
            return !(e == null);
        }

        public synchronized RuntimeException getError() throws InterruptedException {
            waitForFinish();
            return e;
        }

        public synchronized List<Q> getResult() throws InterruptedException {
            waitForFinish();
            return result;
        }

        synchronized void finish() {
            finishes = true;
            notifyAll();
        }
    }

}
