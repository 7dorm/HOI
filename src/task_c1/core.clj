(ns task-c1.core)

(defn generate-strings
  "Генерирует все возможные строки длины N из алфавита,
   не содержащие одинаковых подряд идущих символов.
   
   alphabet - список однобуквенных строк (алфавит)
   n - длина результирующих строк
   
   Возвращает список строк длины N без повторяющихся подряд символов.
   
   Пример:
   (generate-strings [\"a\" \"b\" \"c\"] 2)
   => (\"ab\" \"ac\" \"ba\" \"bc\" \"ca\" \"cb\")"
  [alphabet n]
  (if (<= n 0)
    '()
    (let [; Начальное состояние: строки длины 1 (все символы алфавита)
          initial-strings (map str alphabet)]
      ; Используем reduce для построения строк нужной длины
      (reduce (fn [current-strings _]
                ; Для каждой текущей строки добавляем все возможные символы,
                ; которые отличаются от последнего символа строки
                (apply concat
                       (map (fn [s]
                              ; Получаем последний символ текущей строки
                              (let [last-char (str (last s))
                                    ; Фильтруем алфавит, убирая символ, совпадающий с последним
                                    available-chars (remove #(= % last-char) alphabet)]
                                ; Создаем новые строки, добавляя каждый доступный символ
                                (map (fn [ch]
                                       (str s ch))
                                     available-chars)))
                            current-strings)))
              initial-strings
              ; Повторяем (n-1) раз, так как начальное состояние уже имеет длину 1
              (range (dec n))))))

(defn -main
  "Пример использования функции"
  [& args]
  (println "Пример 1: алфавит (\"a\" \"b\" \"c\"), N=2")
  (println (generate-strings ["a" "b" "c"] 2))
  (println)
  
  (println "Пример 2: алфавит (\"a\" \"b\" \"c\"), N=3")
  (println (generate-strings ["a" "b" "c"] 3))
  (println)
  
  (println "Пример 3: алфавит (\"x\" \"y\"), N=4")
  (println (generate-strings ["x" "y"] 4))
  (println)
  
  (println "Пример 4: алфавит (\"a\" \"b\" \"c\" \"d\"), N=2")
  (println (generate-strings ["a" "b" "c" "d"] 2)))

