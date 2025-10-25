#!/bin/bash

echo "=== Интенсивное тестирование производительности ==="
echo "Запуск сервера..."

# Ждем запуска сервера
sleep 3

echo ""
echo "1. Тест высокой нагрузки (10 одновременных клиентов)"
echo "===================================================="

# Запускаем 10 клиентов одновременно для проверки мониторинга пула
for i in {1..10}; do
    echo "Запуск клиента $i..."
    java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "load_user$i" &
done

# Ждем завершения всех клиентов
wait

echo ""
echo "2. Тест с разными задержками (проверка graceful shutdown)"
echo "========================================================"

# Запускаем клиентов с разными задержками
echo "Запуск клиента с задержкой 5 секунд..."
java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "long_delay_user" --delay 5 &

echo "Запуск клиента с задержкой 3 секунды..."
java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "medium_delay_user" --delay 3 &

echo "Запуск клиента с задержкой 1 секунда..."
java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "short_delay_user" --delay 1 &

# Ждем завершения
wait

echo ""
echo "3. Тест множественных ранних прерываний"
echo "======================================"

# Запускаем несколько клиентов с ранним прерыванием
for i in {1..3}; do
    echo "Запуск клиента с ранним прерыванием $i..."
    java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "early_user$i" --exit-before-read &
done

# Ждем завершения
wait

echo ""
echo "4. Тест стресс-нагрузки (20 клиентов)"
echo "====================================="

echo "Запуск 20 клиентов для стресс-тестирования..."
for i in {1..20}; do
    java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "stress_user$i" &
done

# Ждем завершения всех клиентов
wait

echo ""
echo "5. Тест смешанной нагрузки"
echo "=========================="

# Запускаем смешанные типы клиентов
echo "Запуск обычных клиентов..."
for i in {1..5}; do
    java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "mixed_normal$i" &
done

echo "Запуск клиентов с ранним прерыванием..."
for i in {1..3}; do
    java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "mixed_early$i" --exit-before-read &
done

echo "Запуск клиентов с задержкой..."
for i in {1..2}; do
    java -cp build/libs/Task_1J.jar ru.nsu.nocode.client.KeyClient localhost 8080 "mixed_delay$i" --delay 2 &
done

# Ждем завершения
wait

echo ""
echo "=== Интенсивное тестирование завершено ==="
echo "Проверьте логи сервера для анализа:"
echo "- Мониторинга пула потоков"
echo "- Производительности при высокой нагрузке"
echo "- Graceful shutdown при активных задачах"
echo "- Кэширования и TTL"
