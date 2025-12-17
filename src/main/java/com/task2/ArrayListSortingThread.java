package com.task2;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Дочерняя нить для сортировки списка ArrayList алгоритмом пузырька
 */
public class ArrayListSortingThread extends Thread {
    private final List<String> list;
    private final long delayMs;
    private final AtomicLong stepCounter;
    private volatile boolean running = true;
    
    public ArrayListSortingThread(List<String> list, long delayMs, AtomicLong stepCounter) {
        this.list = list;
        this.delayMs = delayMs;
        this.stepCounter = stepCounter;
    }
    
    public void stopSorting() {
        running = false;
    }
    
    @Override
    public void run() {
        while (running) {
            try {
                synchronized (list) {
                    if (list.isEmpty() || list.size() < 2) {
                        // Освобождаем блокировку перед sleep
                    }
                }
                
                if (list.isEmpty() || list.size() < 2) {
                    Thread.sleep(delayMs);
                    continue;
                }
                
                // Алгоритм пузырька: бесконечный проход по списку
                int i = 0;
                
                while (running) {
                    // Задержка внутри шага (до сравнения)
                    Thread.sleep(delayMs);
                    
                    synchronized (list) {
                        if (list.size() < 2) {
                            break;
                        }
                        
                        if (i >= list.size() - 1) {
                            i = 0;
                            // Освобождаем блокировку перед следующей итерацией
                            continue;
                        }
                        
                        // Сравниваем строки лексикографически
                        if (list.get(i).compareTo(list.get(i + 1)) > 0) {
                            // Обмениваем элементы местами
                            Collections.swap(list, i, i + 1);
                            stepCounter.incrementAndGet();
                            
                            // После перестановки начинаем новый проход с начала
                            i = 0;
                        } else {
                            // Переходим к следующему элементу
                            i++;
                        }
                    }
                    
                    // Задержка между шагами
                    Thread.sleep(delayMs);
                }
                
                // Небольшая пауза перед следующим проходом
                Thread.sleep(delayMs);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}

