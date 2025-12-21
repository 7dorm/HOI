package com.task2;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Дочерняя нить для сортировки списка алгоритмом пузырька
 */
public class SortingThread extends Thread {
    private final CustomLinkedList list;
    private final long delayMs;
    private final AtomicLong stepCounter;
    private volatile boolean running = true;
    
    public SortingThread(CustomLinkedList list, long delayMs, AtomicLong stepCounter) {
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
                if (list.isEmpty() || list.size() < 2) {
                    Thread.sleep(delayMs);
                    continue;
                }
                
                // Алгоритм пузырька: бесконечный проход по списку
                CustomLinkedList.Node current = list.getHead();
                
                // Проходим весь список от начала до конца
                while (current != null && current.next != null && running) {
                    CustomLinkedList.Node next = current.next;
                    
                    // Захватываем узлы в порядке от головы к хвосту (предотвращение deadlock)
                    synchronized (current) {
                        synchronized (next) {
                            // Проверяем, что узлы все еще соседние (могли измениться другим потоком)
                            if (current.next == next) {
                                // Задержка внутри шага (до сравнения)
                                Thread.sleep(delayMs);
                                
                                // Сравниваем строки лексикографически
                                if (current.data.compareTo(next.data) > 0) {
                                    // Переустанавливаем ссылки
                                    list.swapAdjacentNodes(current, next);
                                    stepCounter.incrementAndGet();
                                    
                                    // После перестановки начинаем новый проход с начала
                                    // для корректной работы алгоритма пузырька
                                    current = list.getHead();
                                } else {
                                    // Переходим к следующему узлу
                                    current = next;
                                }
                            } else {
                                // Структура изменилась другим потоком, начинаем заново
                                current = list.getHead();
                            }
                        }
                    }
                    
                    // Задержка между шагами
                    Thread.sleep(delayMs);
                    
                    // Если дошли до конца, начинаем новый проход с начала
                    if (current == null || current.next == null) {
                        current = list.getHead();
                    }
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

