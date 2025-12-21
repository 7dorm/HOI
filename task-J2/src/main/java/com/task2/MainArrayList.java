package com.task2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Главный класс программы с использованием ArrayList и Collections.synchronizedList()
 */
public class MainArrayList {
    private static final int MAX_LINE_LENGTH = 80;
    private static final long DELAY_MS = 1000; // 1 секунда
    private static final int NUM_SORTING_THREADS = 2;
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<String> list = Collections.synchronizedList(new ArrayList<>());
        AtomicLong stepCounter = new AtomicLong(0);
        
        // Создаем дочерние нити для сортировки
        ArrayListSortingThread[] sortingThreads = new ArrayListSortingThread[NUM_SORTING_THREADS];
        for (int i = 0; i < NUM_SORTING_THREADS; i++) {
            sortingThreads[i] = new ArrayListSortingThread(list, DELAY_MS, stepCounter);
            sortingThreads[i].start();
        }
        
        System.out.println("Программа запущена (версия с ArrayList). Введите строки (пустая строка для вывода списка, 'exit' для выхода):");
        System.out.println("Количество потоков сортировки: " + NUM_SORTING_THREADS);
        System.out.println("Задержка: " + DELAY_MS + " мс");
        
        while (true) {
            String input = scanner.nextLine();
            
            if (input.equals("exit")) {
                // Останавливаем потоки сортировки
                for (ArrayListSortingThread thread : sortingThreads) {
                    thread.stopSorting();
                }
                for (ArrayListSortingThread thread : sortingThreads) {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println("Всего шагов сортировки: " + stepCounter.get());
                break;
            }
            
            if (input.isEmpty()) {
                // Выводим текущее состояние списка
                printList(list);
                System.out.println("Всего шагов сортировки на данный момент: " + stepCounter.get());
            } else {
                // Разрезаем строки длиннее 80 символов
                String[] parts = splitLongString(input);
                synchronized (list) {
                    for (String part : parts) {
                        list.add(0, part); // Добавляем в начало
                    }
                }
            }
        }
        
        scanner.close();
    }
    
    /**
     * Разрезает строку на части длиной не более MAX_LINE_LENGTH символов
     */
    private static String[] splitLongString(String str) {
        if (str.length() <= MAX_LINE_LENGTH) {
            return new String[]{str};
        }
        
        int numParts = (str.length() + MAX_LINE_LENGTH - 1) / MAX_LINE_LENGTH;
        String[] parts = new String[numParts];
        
        for (int i = 0; i < numParts; i++) {
            int start = i * MAX_LINE_LENGTH;
            int end = Math.min(start + MAX_LINE_LENGTH, str.length());
            parts[i] = str.substring(start, end);
        }
        
        return parts;
    }
    
    /**
     * Выводит список в безопасном режиме
     */
    private static void printList(List<String> list) {
        System.out.println("--- Текущее состояние списка ---");
        synchronized (list) {
            if (list.isEmpty()) {
                System.out.println("(список пуст)");
            } else {
                for (int i = 0; i < list.size(); i++) {
                    System.out.println(i + ": " + list.get(i));
                }
            }
        }
        System.out.println("--- Конец списка ---");
    }
}

