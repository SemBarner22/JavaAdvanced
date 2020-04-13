package ru.ifmo.rain.zagretdinov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class which provides methods to multi-thread tasks. Class is able to process functions using given amount of threads.
 *
 * @author sem
 * @version 1.0.0
 */
public class IterativeParallelism implements AdvancedIP {

    private final ParallelMapper parallelMapper;

    /**
     * Constructor with {@link ParallelMapper} as a parameter.
     * @param parallelMapper implementation of {@link ParallelMapper}
     */
    public IterativeParallelism(final ParallelMapper parallelMapper) {
        this.parallelMapper = parallelMapper;
    }

    /**
     * Constructor without {@link ParallelMapper} to use included thread regulation.
     */
    public IterativeParallelism() {
        this.parallelMapper = null;
    }

    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return working(threads, values, s -> s.map(Object::toString).collect(Collectors.joining()),
                s -> s.collect(Collectors.joining()));
    }

    private <T> T sameFunctions(final int threads, final List<T> values, final Function<Stream<T>, T> function)
            throws InterruptedException {
        return working(threads, values, function, function);
    }

    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator)
            throws InterruptedException {
        // :NOTE: Унифицировать с max //DONE
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator)
            throws InterruptedException {
        return sameFunctions(threads, values, s -> s.max(comparator).orElse(null));
    }

    private <T, U, R> R working(final int threads, final List<T> values,
                                final Function<Stream<T>, U> functionTo,
                                final Function<Stream<U>, R> functionFrom)
            throws InterruptedException {
        final List<Stream<T>> newList = split(threads, values);
        final List<U> result;
        if (parallelMapper != null) {
            result = parallelMapper.map(functionTo, newList);
        } else {
            final List<Thread> workers = new ArrayList<>();
            result = new ArrayList<>(Collections.nCopies(newList.size(), null));
            for (int i = 0; i < newList.size(); i++) {
                final int nextIndex = i;
                final Thread thread = new Thread(() -> result.set(nextIndex, functionTo.apply(newList.get(nextIndex))));
                workers.add(thread);
                thread.start();
            }
            joinAll(workers, true);
        }
        return functionFrom.apply(result.stream());
    }

    /**
     * Method stopping threads and throwing exceptions if bool is set. Stopping all threads one by one and adds
     * {@link Exception} is bool set on {@code false}
     *
     * @param workers {@link List} consisting
     * @param hideExceptions whether {@link Exception} should be thrown.
     * @throws InterruptedException if interruption of {@link Thread} had thrown {@link Exception}.
     */
    static void joinAll(final List<Thread> workers, final boolean hideExceptions) throws InterruptedException {
        final int threads = workers.size();
        for (int i = 0; i < threads; i++) {
            try {
                workers.get(i).join();
            } catch (final InterruptedException e) {
                InterruptedException exception = new InterruptedException("Some threads have been interrupted");
                exception.addSuppressed(e);
                for (int j = i; j < threads; j++) {
                    workers.get(j).interrupt();
                }
                for (int j = i; j < threads; j++) {
                    try {
                        workers.get(j).join();
                    } catch (final InterruptedException e1) {
                        exception.addSuppressed(e1);
                        // :NOTE: ++C
                        j--;
                    }
                }
                if (!hideExceptions) {
                    throw exception;
                }
            }
        }
    }


    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        // :NOTE: Унифицировать c map //DONE
        return working(threads, values, s -> s.filter(predicate).collect(Collectors.toList()),
                IterativeParallelism::streamOfLists);
    }

    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values,
                              final Function<? super T, ? extends U> f)
            throws InterruptedException {
        return working(threads, values, s -> s.map(f).collect(Collectors.toList()),
                IterativeParallelism::streamOfLists);
    }

    private static <I> List<I> streamOfLists(final Stream<? extends List<? extends I>> streams) {
        return streams.flatMap(List::stream).collect(Collectors.toList());
    }

    private <T> List<Stream<T>> split(final int threads, final List<T> values) {
        final int amountPerThreadDivided = values.size() / threads;
        final int additional = values.size() % threads;
        final List<Stream<T>> streams = new ArrayList<>();
        int amountPerThread;
        int processed = 0;
        for (int i = 0; i < threads; ++i) {
            amountPerThread = amountPerThreadDivided;
            if (i < additional) {
                amountPerThread++;
            }
            if (amountPerThread > 0) {
                streams.add(values.subList(processed, processed + amountPerThread).stream());
            }
            processed += amountPerThread;
        }
        return streams;
    }

    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return working(threads, values, s -> s.allMatch(predicate), s -> s.allMatch(Boolean::booleanValue));
    }

    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate)
            throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    @Override
    public <T> T reduce(final int threads, final List<T> values, final Monoid<T> monoid) throws InterruptedException {
        return sameFunctions(threads, values, s -> getReducer(s, monoid));
    }

    private static <T> T getReducer(final Stream<T> stream, final Monoid<T> monoid) {
        return stream.reduce(monoid.getIdentity(), monoid.getOperator());
    }

    @Override
    public <T, R> R mapReduce(final int threads, final List<T> values, final Function<T, R> lift, final Monoid<R> monoid)
            throws InterruptedException {
        // :NOTE: копипаста //DONE
        final Function<Stream<T>, R> functionReduce = s -> s.map(lift).reduce(monoid.getIdentity(), monoid.getOperator());
        return working(threads, values, functionReduce, s -> getReducer(s, monoid));
    }
}