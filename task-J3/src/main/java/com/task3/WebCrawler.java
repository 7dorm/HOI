package com.task3;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Класс для асинхронного обхода ресурсов HTTP-сервера
 * Использует виртуальные потоки Java 21
 */
public class WebCrawler {
    private final HttpClient httpClient;
    private final Set<String> visitedPaths;
    private final List<String> messages;
    private final ReentrantLock lock;
    private final ExecutorService executor;
    
    public WebCrawler(String host, int port) {
        this.httpClient = new HttpClient(host, port);
        this.visitedPaths = ConcurrentHashMap.newKeySet();
        this.messages = new ArrayList<>();
        this.lock = new ReentrantLock();
        
        // Создаем ExecutorService с виртуальными потоками (Java 21)
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }
    
    /**
     * Начинает обход с корневого пути "/"
     */
    public List<String> crawl() throws InterruptedException {
        Phaser phaser = new Phaser(1); // Регистрируем основной поток
        
        // Запускаем обход с корневого пути
        phaser.register(); // Регистрируем задачу для корневого пути
        executor.submit(() -> {
            try {
                crawlPath("/", phaser);
            } finally {
                phaser.arriveAndDeregister();
            }
        });
        
        // Ждем завершения всех задач
        phaser.arriveAndAwaitAdvance();
        
        // Завершаем executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(180, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Сортируем сообщения в лексикографическом порядке
        Collections.sort(messages);
        
        return new ArrayList<>(messages);
    }
    
    /**
     * Рекурсивно обходит путь и все его successors
     */
    private void crawlPath(String path, Phaser phaser) {
        // Проверяем, не посещали ли мы уже этот путь
        if (!visitedPaths.add(path)) {
            return;
        }
        
        try {
            // Выполняем HTTP запрос
            ServerResponse response = httpClient.fetch(path);
            
            // Добавляем сообщение в список
            lock.lock();
            try {
                messages.add(response.getMessage());
            } finally {
                lock.unlock();
            }
            
            // Если есть successors, обходим их параллельно
            List<String> successors = response.getSuccessors();
            if (successors != null && !successors.isEmpty()) {
                // Регистрируем все дочерние задачи в phaser
                phaser.bulkRegister(successors.size());
                
                for (String successor : successors) {
                    String nextPath = normalizePath(path, successor);
                    
                    executor.submit(() -> {
                        try {
                            crawlPath(nextPath, phaser);
                        } finally {
                            phaser.arriveAndDeregister();
                        }
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при обходе пути " + path + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Нормализует путь, объединяя текущий путь с successor
     */
    private String normalizePath(String currentPath, String successor) {
        if (successor.startsWith("/")) {
            return successor;
        }
        
        // Убираем завершающий слэш из currentPath если есть
        String base = currentPath.endsWith("/") ? currentPath.substring(0, currentPath.length() - 1) : currentPath;
        
        // Если base пустой или равен "/", просто возвращаем successor со слэшем
        if (base.isEmpty() || base.equals("/")) {
            return "/" + successor;
        }
        
        return base + "/" + successor;
    }
}

