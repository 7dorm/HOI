#!/bin/bash

# Создаем директорию для скомпилированных классов
mkdir -p target/classes

# Компилируем все Java файлы
javac -d target/classes -encoding UTF-8 src/main/java/com/task2/*.java

if [ $? -eq 0 ]; then
    echo "Компиляция успешна!"
    echo "Для запуска используйте:"
    echo "  java -cp target/classes com.task2.Main"
    echo "  java -cp target/classes com.task2.MainArrayList"
    echo "  java -cp target/classes com.task2.StepMeasurementTest"
else
    echo "Ошибка компиляции!"
    exit 1
fi

