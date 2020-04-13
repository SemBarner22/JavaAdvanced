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
    private final ConcurrentMap<String, HostDownloader> hostDownloaders;

    public WebCrawler(Downloader downloader, int downloaders, int extractors, int perHost) {
        this.downloader = downloader;
        this.perHost = perHost;
        downloadersPool = Executors.newFixedThreadPool(downloaders);
        extractorsPool = Executors.newFixedThreadPool(extractors);
        hostDownloaders = new ConcurrentHashMap<>();
    }

    public static void main(String[] args) {
        if (args == null || args.length == 0 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Arguments should not be null");
        } else {
            try {
                int depth = argumentOrDefault(args, 1);
                int downloaderAmount = argumentOrDefault(args, 2);
                int extractorAmount = argumentOrDefault(args, 3);
                int perHost = argumentOrDefault(args, 4);
                try (Crawler crawler = new WebCrawler(new CachingDownloader(), downloaderAmount, extractorAmount, perHost)) {
                    crawler.download(args[0], depth);

                } catch (NumberFormatException e) {
                    System.err.println("Arguments should be an integer");
                }
            } catch (IOException e) {
                System.err.println("An error happened during downloader initialization " + e.getMessage());
            }
        }
    }

    private static int argumentOrDefault(String[] args, int index) {
        return index >= args.length ? 1 : Integer.parseInt(args[index]);
    }

    private class HostDownloader {
        private final Queue<Runnable> queueTasks;

        private int inProcess;

        HostDownloader() {
            queueTasks = new ArrayDeque<>();
            inProcess = 0;
        }

        synchronized void add(Runnable task) {
            queueTasks.add(task);
            runAndMarkNext();
        }

        synchronized private void runAndMarkNext() {
            if (inProcess < perHost) {
                Runnable task = queueTasks.poll();
                if (task != null) {
                    inProcess++;
                    downloadersPool.submit(() -> {
                        try {
                            task.run();
                        } finally {
                            finishAndStartNext();
                        }
                    });
                }
            }
        }

        synchronized private void finishAndStartNext() {
            inProcess--;
            runAndMarkNext();
        }

    }

    private class Worker {
        private final Set<String> processed = ConcurrentHashMap.newKeySet();
        private final ConcurrentMap<String, IOException> exceptions = new ConcurrentHashMap<>();
        private final Set<String> nextLevel = ConcurrentHashMap.newKeySet();
        private final ConcurrentLinkedQueue<String> awaits = new ConcurrentLinkedQueue<>();

        public Worker(String url, int depth) {
            awaits.add(url);
            for (int j = 0; j < depth; j++) {
                final int currentDepth = depth - j;
                final Phaser level = new Phaser(1);
                List<String> processing = new ArrayList<>(awaits);
                awaits.clear();
                processing.stream()
                        .filter(nextLevel::add)
                        .forEach(link -> queueDownload(link, currentDepth, level));
                level.arriveAndAwaitAdvance();
            }
        }

        private void linksExtraction(final Document document, final Phaser level) {
            level.register();
            extractorsPool.submit(() -> {
                try {
                    List<String> links = document.extractLinks();
                    awaits.addAll(links);
                } catch (IOException ignored) {
                    // ignore
                } finally {
                    level.arrive();
                }
            });
        }

        void queueDownload(final String url, final int depth, final Phaser level) {
            String host;
            try {
                host = URLUtils.getHost(url);
            } catch (MalformedURLException e) {
                exceptions.put(url, e);
                return;
            }
            HostDownloader hostDownloader = hostDownloaders.
                    computeIfAbsent(host, s -> new HostDownloader());
            level.register();
            hostDownloader.add(() -> {
                try {
                    Document document = downloader.download(url);
                    processed.add(url);
                    if (depth > 1) {
                        linksExtraction(document, level);
                    }
                } catch (IOException e) {
                    exceptions.put(url, e);
                } finally {
                    level.arrive();
                }
            });
        }

        Result getResult() {
            return new Result(new ArrayList<>(processed), exceptions);
        }

    }

    @Override
    public Result download(String s, int i) {
        return new Worker(s, i).getResult();
    }

    @Override
    public void close() {
        extractorsPool.shutdown();
        downloadersPool.shutdown();
    }

}
