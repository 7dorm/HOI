#!/bin/bash

echo "=== Тестирование множественных клиентов ==="
echo "Запуск сервера..."

# Ждем запуска сервера
sleep 3

echo ""
echo "1. Тест одновременных подключений (5 клиентов)"
echo "=============================================="

# Запускаем 5 клиентов одновременно
for i in {1..5}; do
    echo "Запуск клиента $i..."
    java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "user$i" &
done

# Ждем завершения всех клиентов
wait

echo ""
echo "2. Тест раннего прерывания (--exit-before-read)"
echo "=============================================="

# Запускаем клиента с ранним прерыванием
echo "Запуск клиента с ранним прерыванием..."
java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "early_exit_user" --exit-before-read

echo ""
echo "3. Тест с задержкой (--delay 2)"
echo "==============================="

# Запускаем клиента с задержкой
echo "Запуск клиента с задержкой 2 секунды..."
java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "delayed_user" --delay 2

echo ""
echo "4. Тест смешанных сценариев (одновременно)"
echo "==========================================="

# Запускаем разные типы клиентов одновременно
echo "Запуск обычного клиента..."
java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "normal_user" &

echo "Запуск клиента с ранним прерыванием..."
java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "early_user" --exit-before-read &

echo "Запуск клиента с задержкой..."
java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "delayed_user2" --delay 1 &

# Ждем завершения
wait

echo ""
echo "5. Тест обработки ошибок (длинное имя)"
echo "====================================="

java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "$(printf 'a%.0s' {1..300})"

echo ""
echo "6. Тест кэширования (повторные запросы)"
echo "======================================="

echo "Первый запрос для user_cached..."
java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "user_cached"

echo "Второй запрос для user_cached (должен быть из кэша)..."
java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "user_cached"

echo ""
echo "=== Тестирование завершено ==="
echo "Проверьте логи сервера для анализа производительности и мониторинга пула."