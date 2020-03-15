package ru.ifmo.rain.zagretdinov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import javax.print.StreamPrintServiceFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {

    @Override
    public String join(int threads, List<?> values) throws InterruptedException {
        return working(threads, values, s-> s.map(Object::toString).collect(Collectors.joining()),
                s -> s.collect(Collectors.joining()));
    }

    @Override
    public <T> T minimum(int threads, List<? extends T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        return working(threads, values, s -> s.min(comparator).orElse(null), s -> s.min(comparator).orElse(null));
    }

    private <T, U, R> R working(int threads, List<? extends T> values,
                                Function<Stream<? extends T>, U> functionTo,
                                Function<Stream<? extends U>, R> functionFrom)
            throws InterruptedException {
        List<Stream<? extends T>> newList = split(threads, values);
        List<Thread> workers = new ArrayList<>();
        List<U> result = new ArrayList<>(Collections.nCopies(newList.size(), null));
        for (int i = 0; i < newList.size(); ++i) {
            final int nextIndex = i;
            Thread thread = new Thread(() -> result.set(nextIndex, functionTo.apply(newList.get(nextIndex))));
            workers.add(thread);
            thread.start();
        }
        for (int i = 0; i < newList.size(); i++) {
            workers.get(i).join();
        }
        return functionFrom.apply(result.stream());
    }

    @Override
    public <T> List<T> filter(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return working(threads, values, s -> s.filter(predicate),
                s -> s.flatMap(Function.identity()).collect(Collectors.toList()));
    }

    @Override
    public <T, U> List<U> map(int threads, List<? extends T> values, Function<? super T, ? extends U> f)
            throws InterruptedException {
            return working(threads, values, s -> s.map(f),
                    s -> s.flatMap(Function.identity()).collect(Collectors.toList()));
//        List<Stream<? extends T>> newList = split(threads, values);
//        List<Thread> workers = new ArrayList<>();
//        List<Stream<? extends U>> result = new ArrayList<>(Collections.nCopies(newList.size(), null));
//        for (int i = 0; i < threads; ++i) {
//            final int nextIndex = i;
//            Thread thread = new Thread(() -> result.set(nextIndex, newList.get(nextIndex).map(f)));
//            workers.add(thread);
//            thread.start();
//        }
//        for (int i = 0; i < threads; i++) {
//            workers.get(i).join();
//        }
//        return result.stream().flatMap(Function.identity()).collect(Collectors.toList());
    }

    @Override
    public <T> T maximum(int threads, List<? extends T> values, Comparator<? super T> comparator)
            throws InterruptedException {
        return working(threads, values, s -> s.max(comparator).get(), s -> s.max(comparator).get());
//        List<Stream<? extends T>> newList = split(threads, values);
//        List<Thread> workers = new ArrayList<>();
//        List<T> result = new ArrayList<>(Collections.nCopies(newList.size(), null));
//        for (int i = 0; i < threads; ++i) {
//            final int nextIndex = i;
//            Thread thread = new Thread(() -> result.set(nextIndex, newList.get(nextIndex).max(comparator).orElse(null)));
//            workers.add(thread);
//            thread.start();
//        }
//        for (int i = 0; i < threads; i++) {
//            workers.get(i).join();
//        }
//        return result.stream().max(comparator).get();
    }

    private <T> List<Stream<? extends T>> split(int threads, List<? extends T> values) {
        int amountPerThreadDivided = values.size() / threads;
        int additional = values.size() % threads;
        List<Stream<? extends T>> streams = new ArrayList<>();
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
    public <T> boolean all(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return working(threads, values, s -> s.allMatch(predicate), s -> s.allMatch(Boolean::booleanValue));
    }

    @Override
    public <T> boolean any(int threads, List<? extends T> values, Predicate<? super T> predicate)
            throws InterruptedException {
        return working(threads, values, s -> s.anyMatch(predicate), s -> s.anyMatch(Boolean::booleanValue));
    }
}