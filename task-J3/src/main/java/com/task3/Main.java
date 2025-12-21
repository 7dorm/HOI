package com.task3;

import java.util.List;

/**
 * Главный класс программы для асинхронного обхода ресурсов HTTP-сервера
 */
public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Использование: java -jar Task_J3-client.jar <student_id> [host] [port]");
            System.err.println("Пример: java -jar Task_J3-client.jar IvanIvanov localhost 8080");
            System.exit(1);
        }
        
        String studentId = args[0];
        String host = args.length > 1 ? args[1] : "localhost";
        int port = args.length > 2 ? Integer.parseInt(args[2]) : 8080;
        
        System.out.println("Запуск обхода сервера:");
        System.out.println("  Host: " + host);
        System.out.println("  Port: " + port);
        System.out.println("  Student ID: " + studentId);
        System.out.println();
        
        WebCrawler crawler = new WebCrawler(host, port);
        
        long startTime = System.currentTimeMillis();
        
        try {
            List<String> messages = crawler.crawl();
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.println("Обход завершен за " + duration + " мс");
            System.out.println("Найдено сообщений: " + messages.size());
            System.out.println();
            System.out.println("Результат (отсортированный в лексикографическом порядке):");
            System.out.println("=" .repeat(80));
            
            for (String message : messages) {
                System.out.println(message);
            }
            
            System.out.println("=" .repeat(80));
            
        } catch (Exception e) {
            System.err.println("Ошибка при выполнении обхода: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}

