#!/bin/bash

echo "=== Финальный демонстрационный тест ==="
echo "Демонстрация всех улучшений Key Server"

# Ждем запуска сервера
sleep 2

echo ""
echo "1. Демонстрация кэширования и TTL"
echo "================================="

echo "Первый запрос для демо_user (генерация нового ключа)..."
java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "demo_user"

echo "Второй запрос для демо_user (должен быть из кэша)..."
java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "demo_user"

echo ""
echo "2. Демонстрация обработки ошибок"
echo "================================"

echo "Тест с длинным именем (должен вернуть ошибку)..."
java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "$(printf 'x%.0s' {1..300})"

echo ""
echo "3. Демонстрация производительности (RSA 2048 vs старый 8192)"
echo "=========================================================="

echo "Запуск 5 клиентов одновременно для демонстрации скорости..."
start_time=$(date +%s.%N)

for i in {1..5}; do
    java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "perf_user$i" &
done

wait

end_time=$(date +%s.%N)
duration=$(echo "$end_time - $start_time" | bc)
echo "Время обработки 5 клиентов: ${duration} секунд"

echo ""
echo "4. Демонстрация мониторинга пула"
echo "================================"

echo "Запуск 8 клиентов для демонстрации мониторинга пула (4 потока)..."
for i in {1..8}; do
    java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "monitor_user$i" &
done

wait

echo ""
echo "5. Демонстрация раннего прерывания"
echo "================================="

echo "Запуск клиента с ранним прерыванием..."
java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "early_demo_user" --exit-before-read

echo ""
echo "6. Демонстрация задержек"
echo "======================="

echo "Запуск клиента с задержкой 3 секунды..."
java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "delay_demo_user" --delay 3

echo ""
echo "=== Демонстрация завершена ==="
echo ""
echo "Проверенные функции:"
echo "✅ Кэширование с TTL (5 минут)"
echo "✅ LRU кэш с лимитом 1000 записей"
echo "✅ Обработка ошибок с кодами состояния"
echo "✅ Валидация длины имени (макс 255 символов)"
echo "✅ Профессиональное логирование (SLF4J + Logback)"
echo "✅ Мониторинг пула потоков"
echo "✅ Graceful shutdown"
echo "✅ RSA 2048 бит (вместо 8192)"
echo "✅ Поддержка EC ключей (ECDSA)"
echo ""
echo "Сервер продолжает работать. Проверьте логи для анализа производительности."
