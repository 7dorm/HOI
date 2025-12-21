package com.task2;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Собственная реализация двусвязного списка с поддержкой Iterable
 * для синхронизированного доступа к списку строк
 */
public class CustomLinkedList implements Iterable<String> {
    
    /**
     * Узел двусвязного списка
     */
    public static class Node {
        String data;
        Node prev;
        Node next;
        
        public Node(String data) {
            this.data = data;
        }
    }
    
    private Node head;
    private Node tail;
    private int size;
    private final Object lock = new Object();
    
    /**
     * Добавляет элемент в начало списка
     */
    public void addFirst(String data) {
        synchronized (lock) {
            Node newNode = new Node(data);
            if (head == null) {
                head = tail = newNode;
            } else {
                newNode.next = head;
                head.prev = newNode;
                head = newNode;
            }
            size++;
        }
    }
    
    /**
     * Получает размер списка
     */
    public int size() {
        synchronized (lock) {
            return size;
        }
    }
    
    /**
     * Проверяет, пуст ли список
     */
    public boolean isEmpty() {
        synchronized (lock) {
            return size == 0;
        }
    }
    
    /**
     * Получает головной узел (для сортировки)
     */
    public Node getHead() {
        synchronized (lock) {
            return head;
        }
    }
    
    /**
     * Получает объект блокировки для синхронизации
     */
    public Object getLock() {
        return lock;
    }
    
    /**
     * Меняет местами два соседних узла, переустанавливая ссылки
     * Узлы должны быть захвачены в порядке от головы к хвосту
     * Предполагается, что node1.next == node2
     */
    public boolean swapAdjacentNodes(Node node1, Node node2) {
        if (node1 == null || node2 == null || node1.next != node2) {
            return false;
        }
        
        synchronized (lock) {
            // Сохраняем ссылки
            Node prev1 = node1.prev;
            Node next2 = node2.next;
            
            // Переустанавливаем ссылки
            node1.next = next2;
            node1.prev = node2;
            node2.next = node1;
            node2.prev = prev1;
            
            // Обновляем соседние узлы
            if (prev1 != null) {
                prev1.next = node2;
            } else {
                head = node2;
            }
            
            if (next2 != null) {
                next2.prev = node1;
            } else {
                tail = node1;
            }
            
            return true;
        }
    }
    
    /**
     * Создает копию списка для безопасного вывода
     */
    public String[] toArray() {
        synchronized (lock) {
            String[] result = new String[size];
            Node current = head;
            int index = 0;
            while (current != null) {
                result[index++] = current.data;
                current = current.next;
            }
            return result;
        }
    }
    
    @Override
    public Iterator<String> iterator() {
        return new CustomLinkedListIterator();
    }
    
    /**
     * Итератор для списка
     */
    private class CustomLinkedListIterator implements Iterator<String> {
        private Node current;
        private final String[] snapshot;
        private int index;
        
        public CustomLinkedListIterator() {
            synchronized (lock) {
                snapshot = toArray();
                index = 0;
            }
        }
        
        @Override
        public boolean hasNext() {
            return index < snapshot.length;
        }
        
        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return snapshot[index++];
        }
    }
}

