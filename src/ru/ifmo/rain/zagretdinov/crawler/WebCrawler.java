package ru.ifmo.rain.zagretdinov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.*;

public class WebCrawler implements Crawler {
    private final Downloader downloader;
    private final int perHost;
    private final ExecutorService extractorsPool;
    private final ExecutorService downloadersPool;
    private final ConcurrentMap<String, HostManager> hosts;
    private int AWAIT_TERMINATION_TIME = 5;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;
        downloadersPool = Executors.newFixedThreadPool(downloaders);
        extractorsPool = Executors.newFixedThreadPool(extractors);
        hosts = new ConcurrentHashMap<>();
    }

    private static int argumentOrDefault(String[] args, int index) {
        return index >= args.length ? 1 : Integer.parseInt(args[index]);
    }

    public static void main(String[] args) {
        if (args == null || args.length == 0 || args.length > 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Arguments should not be null");
        } else {
            try {
                int depth = argumentOrDefault(args, 1);
                int downloaderAmount = argumentOrDefault(args, 2);
                int extractorAmount = argumentOrDefault(args, 3);
                int perHost = argumentOrDefault(args, 4);
                try (Crawler crawler = new WebCrawler(new CachingDownloader(), downloaderAmount, extractorAmount,
                        perHost)) {
                    crawler.download(args[0], depth);

                } catch (NumberFormatException e) {
                    System.err.println("Arguments should be an integer");
                }
            } catch (IOException e) {
                System.err.println("An error happened during downloader initialization " + e.getMessage());
            }
        }
    }

    @Override
    public Result download(String s, int i) {
        Worker worker = new Worker(s, i);
        return new Result(worker.getDownloadedSet(), worker.getErrors());
    }

    @Override
    public void close() {
        extractorsPool.shutdown();
        downloadersPool.shutdown();
        try {
            // Await terminations added
            extractorsPool.awaitTermination(AWAIT_TERMINATION_TIME, TimeUnit.SECONDS);
            extractorsPool.awaitTermination(AWAIT_TERMINATION_TIME, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
    }

    private class Worker {
        private final Set<String> downloadedSet = ConcurrentHashMap.newKeySet();
        private final Set<String> nextLevelSet = ConcurrentHashMap.newKeySet();
        private final ConcurrentMap<String, IOException> errors = new ConcurrentHashMap<>();
        private final ConcurrentLinkedQueue<String> awaits = new ConcurrentLinkedQueue<>();
        private int depth;

        Worker(String url, int depth) {
            awaits.add(url);
            this.depth = depth;
            bfsWork();
        }

        private void bfsWork() {
            for (int j = 0; j < depth; j++) {
                final boolean extract = j < depth - 1;
                final Phaser phaser = new Phaser(1);
                awaits.stream()
                        .filter(nextLevelSet::add)
                        .forEach(page -> queueDownload(page, phaser, extract));
                awaits.clear();
                phaser.arriveAndAwaitAdvance();
            }
        }

        void queueDownload(final String url, final Phaser level, boolean extract) {
            try {
                String host = URLUtils.getHost(url);
                HostManager manager = hosts.
                        computeIfAbsent(host, s -> new HostManager());
                level.register();
                manager.add(() -> {
                    try {
                        Document document = downloader.download(url);
                        downloadedSet.add(url);
                        if (extract) {
                            linksExtraction(document, level);
                        }
                    } catch (IOException e) {
                        errors.put(url, e);
                    } finally {
                        level.arrive();
                    }
                });
            } catch (MalformedURLException e) {
                errors.put(url, e);
            }
        }

        private void linksExtraction(final Document document, final Phaser level) {
            level.register();
            extractorsPool.submit(() -> {
                try {
                    List<String> links = document.extractLinks();
                    awaits.addAll(links);
                } catch (IOException e) {
                    // ignore
                } finally {
                    level.arrive();
                }
            });
        }

        List<String> getDownloadedSet() {
            return new ArrayList<>(downloadedSet);
        }

        Map<String, IOException> getErrors() {
            return errors;
        }

    }

    private class HostManager {
        private int running;
        private Queue<Runnable> tasks;

        HostManager() {
            tasks = new ArrayDeque<>();
            running = 0;
        }

        synchronized void add(Runnable task) {
            tasks.add(task);
            tryRun();
        }

        synchronized private void tryRun() {
            Runnable task;
            if (perHost > running && (task = tasks.poll()) != null) {
                running++;
                downloadersPool.submit(() -> {
                    try {
                        task.run();
                    } finally {
                        running--;
                        tryRun();
                    }
                });
            }
        }
    }
}
