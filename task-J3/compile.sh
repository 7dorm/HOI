#!/bin/bash

# Скрипт для компиляции проекта

echo "Компиляция проекта..."

# Создаем директорию для скомпилированных классов
mkdir -p target/classes

# Проверяем наличие Maven
if command -v mvn &> /dev/null; then
    echo "Использование Maven для сборки..."
    mvn clean compile
    if [ $? -eq 0 ]; then
        echo "Компиляция успешна!"
        echo "Для сборки JAR файла выполните: mvn package"
    else
        echo "Ошибка компиляции!"
        exit 1
    fi
else
    echo "Maven не найден. Компиляция вручную..."
    
    # Скачиваем зависимости (если нужно)
    if [ ! -d "lib" ]; then
        echo "Внимание: библиотеки не найдены. Установите Maven или скачайте зависимости вручную."
        echo "Для скачивания зависимостей выполните: mvn dependency:copy-dependencies"
    fi
    
    # Компилируем с зависимостями
    javac -d target/classes \
          -cp "lib/*:target/classes" \
          -encoding UTF-8 \
          src/main/java/com/task3/*.java
    
    if [ $? -eq 0 ]; then
        echo "Компиляция успешна!"
        echo "Для запуска используйте:"
        echo "  java -cp 'target/classes:lib/*' com.task3.Main <student_id> [host] [port]"
    else
        echo "Ошибка компиляции!"
        exit 1
    fi
fi

