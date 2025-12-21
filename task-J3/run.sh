#!/bin/bash

# Скрипт для запуска программы

if [ $# -lt 1 ]; then
    echo "Использование: ./run.sh <student_id> [host] [port]"
    echo "Пример: ./run.sh IvanIvanov localhost 8080"
    exit 1
fi

STUDENT_ID=$1
HOST=${2:-localhost}
PORT=${3:-8080}

# Проверяем наличие скомпилированных классов
if [ ! -d "target/classes" ]; then
    echo "Классы не скомпилированы. Запускаю компиляцию..."
    ./compile.sh
fi

# Проверяем наличие JAR файла
if [ -f "target/Task_J3-client.jar" ]; then
    echo "Запуск из JAR файла..."
    java -jar target/Task_J3-client.jar "$STUDENT_ID" "$HOST" "$PORT"
else
    echo "Запуск из скомпилированных классов..."
    # Проверяем наличие библиотек
    if [ -d "lib" ]; then
        java -cp "target/classes:lib/*" com.task3.Main "$STUDENT_ID" "$HOST" "$PORT"
    else
        echo "Ошибка: библиотеки не найдены. Выполните: mvn dependency:copy-dependencies"
        exit 1
    fi
fi

