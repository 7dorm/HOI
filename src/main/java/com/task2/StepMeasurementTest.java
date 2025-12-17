package com.task2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Тестовая программа для измерения количества шагов сортировки
 */
public class StepMeasurementTest {
    private static final long DELAY_MS = 100; // Уменьшенная задержка для теста
    private static final int NUM_SORTING_THREADS = 2;
    private static final int TEST_DURATION_MS = 5000; // 5 секунд теста
    
    public static void main(String[] args) {
        System.out.println("=== Тест с собственной реализацией списка ===");
        testCustomLinkedList();
        
        System.out.println("\n=== Тест с ArrayList и Collections.synchronizedList() ===");
        testArrayList();
    }
    
    private static void testCustomLinkedList() {
        CustomLinkedList list = new CustomLinkedList();
        AtomicLong stepCounter = new AtomicLong(0);
        
        // Добавляем тестовые данные
        String[] testData = {"zebra", "apple", "banana", "cherry", "date"};
        for (String item : testData) {
            list.addFirst(item);
        }
        
        System.out.println("Начальное состояние списка:");
        for (String item : list) {
            System.out.println("  " + item);
        }
        
        // Создаем дочерние нити для сортировки
        SortingThread[] sortingThreads = new SortingThread[NUM_SORTING_THREADS];
        for (int i = 0; i < NUM_SORTING_THREADS; i++) {
            sortingThreads[i] = new SortingThread(list, DELAY_MS, stepCounter);
            sortingThreads[i].start();
        }
        
        // Ждем указанное время
        try {
            Thread.sleep(TEST_DURATION_MS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Останавливаем потоки
        for (SortingThread thread : sortingThreads) {
            thread.stopSorting();
        }
        for (SortingThread thread : sortingThreads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        System.out.println("\nФинальное состояние списка:");
        for (String item : list) {
            System.out.println("  " + item);
        }
        
        System.out.println("\nВсего шагов сортировки: " + stepCounter.get());
        System.out.println("Количество потоков: " + NUM_SORTING_THREADS);
        System.out.println("Время теста: " + TEST_DURATION_MS + " мс");
        System.out.println("Задержка: " + DELAY_MS + " мс");
        System.out.println("Теоретически максимальное количество шагов: " + 
            calculateTheoreticalMaxSteps(TEST_DURATION_MS, DELAY_MS, NUM_SORTING_THREADS));
    }
    
    private static void testArrayList() {
        List<String> list = Collections.synchronizedList(new ArrayList<>());
        AtomicLong stepCounter = new AtomicLong(0);
        
        // Добавляем тестовые данные
        String[] testData = {"zebra", "apple", "banana", "cherry", "date"};
        for (String item : testData) {
            list.add(0, item);
        }
        
        System.out.println("Начальное состояние списка:");
        synchronized (list) {
            for (int i = 0; i < list.size(); i++) {
                System.out.println("  " + list.get(i));
            }
        }
        
        // Создаем дочерние нити для сортировки
        ArrayListSortingThread[] sortingThreads = new ArrayListSortingThread[NUM_SORTING_THREADS];
        for (int i = 0; i < NUM_SORTING_THREADS; i++) {
            sortingThreads[i] = new ArrayListSortingThread(list, DELAY_MS, stepCounter);
            sortingThreads[i].start();
        }
        
        // Ждем указанное время
        try {
            Thread.sleep(TEST_DURATION_MS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Останавливаем потоки
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
        
        System.out.println("\nФинальное состояние списка:");
        synchronized (list) {
            for (int i = 0; i < list.size(); i++) {
                System.out.println("  " + list.get(i));
            }
        }
        
        System.out.println("\nВсего шагов сортировки: " + stepCounter.get());
        System.out.println("Количество потоков: " + NUM_SORTING_THREADS);
        System.out.println("Время теста: " + TEST_DURATION_MS + " мс");
        System.out.println("Задержка: " + DELAY_MS + " мс");
        System.out.println("Теоретически максимальное количество шагов: " + 
            calculateTheoreticalMaxSteps(TEST_DURATION_MS, DELAY_MS, NUM_SORTING_THREADS));
    }
    
    /**
     * Рассчитывает теоретически максимальное количество шагов
     * Примечание: это упрощенный расчет, реальное количество зависит от многих факторов
     */
    private static long calculateTheoreticalMaxSteps(long durationMs, long delayMs, int numThreads) {
        // Каждый шаг включает задержку внутри шага и между шагами = 2 * delayMs
        // Но потоки работают параллельно, поэтому умножаем на количество потоков
        // Это очень упрощенный расчет
        long stepsPerThread = durationMs / (2 * delayMs);
        return stepsPerThread * numThreads;
    }
}

