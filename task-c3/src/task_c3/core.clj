(ns task-c3.core)

(defn partition-chunks
  "Разбивает последовательность на блоки заданного размера.
   
   coll - входная последовательность
   chunk-size - размер блока
   
   Возвращает ленивую последовательность блоков."
  [coll chunk-size]
  (when (seq coll)
    (lazy-seq
      (let [chunk (take chunk-size coll)
            rest-coll (drop chunk-size coll)]
        (cons chunk
              (when (seq rest-coll)
                (partition-chunks rest-coll chunk-size)))))))

(defn pfilter-block
  "Применяет предикат к блоку элементов и возвращает отфильтрованный блок.
   
   pred - предикат для фильтрации
   block - блок элементов для обработки
   
   Возвращает последовательность элементов блока, удовлетворяющих предикату."
  [pred block]
  (filter pred block))

(defn pfilter
  "Параллельный вариант filter, обрабатывающий элементы блоками.
   
   pred - предикат для фильтрации
   coll - входная последовательность (может быть конечной или бесконечной)
   chunk-size - размер блока для параллельной обработки (по умолчанию 1000)
   
   Возвращает ленивую последовательность отфильтрованных элементов.
   
   Примеры:
   (pfilter even? (range 100))
   (pfilter #(> % 10) (range))
   (pfilter odd? [1 2 3 4 5 6 7 8 9 10] 3)"
  ([pred coll] (pfilter pred coll 1000))
  ([pred coll chunk-size]
   (if (empty? coll)
     '()
     (let [chunks (partition-chunks coll chunk-size)]
       (letfn [(process-chunks [chunk-seq]
                 (lazy-seq
                   (when (seq chunk-seq)
                     (let [chunk (first chunk-seq)
                           ; Создаем future для обработки текущего блока
                           ; Future создается сразу для параллелизма, но результат вычисляется лениво
                           future-result (future (pfilter-block pred chunk))
                           ; Обрабатываем следующий блок параллельно
                           next-chunks (rest chunk-seq)]
                       ; Объединяем результаты текущего блока с результатами следующих блоков
                       ; Используем lazy-cat для сохранения ленивости
                       ; @future-result будет вычислен только при обращении к результатам
                       (lazy-cat (lazy-seq @future-result)
                                  (process-chunks next-chunks))))))]
         (process-chunks chunks))))))

(defn pfilter-optimized
  "Оптимизированная версия параллельного filter с предварительной загрузкой блоков.
   
   pred - предикат для фильтрации
   coll - входная последовательность
   chunk-size - размер блока (по умолчанию 1000)
   prefetch - количество блоков для предварительной загрузки (по умолчанию 2)
   
   Эта версия предварительно загружает несколько блоков для лучшей производительности."
  ([pred coll] (pfilter-optimized pred coll 1000 2))
  ([pred coll chunk-size] (pfilter-optimized pred coll chunk-size 2))
  ([pred coll chunk-size prefetch]
   (if (empty? coll)
     '()
     (let [chunks (partition-chunks coll chunk-size)]
       (letfn [(process-with-prefetch [chunk-seq]
                 (lazy-seq
                   (when (seq chunk-seq)
                     (let [; Создаем futures для предварительной загрузки нескольких блоков
                           futures-to-process (take prefetch chunk-seq)
                           futures-results (map (fn [chunk]
                                                  (future (pfilter-block pred chunk)))
                                                futures-to-process)
                           ; Остальные блоки для последующей обработки
                           remaining-chunks (drop prefetch chunk-seq)]
                       ; Объединяем результаты предзагруженных блоков
                       ; Используем lazy-cat для сохранения ленивости
                       (let [current-results (apply concat (map deref futures-results))]
                         (lazy-cat current-results
                                   (process-with-prefetch remaining-chunks)))))))]
         (process-with-prefetch chunks))))))

(defn benchmark-filter
  "Сравнивает производительность стандартного и параллельного filter.
   
   pred - предикат для фильтрации
   coll - входная последовательность
   chunk-size - размер блока для параллельного filter (по умолчанию 1000)
   
   Выводит время выполнения для каждого варианта."
  ([pred coll] (benchmark-filter pred coll 1000))
  ([pred coll chunk-size]
   (println "=== Сравнение производительности filter ===")
   (println "Размер входной последовательности:" (if (seq? coll) "бесконечная" (count coll)))
   (println "Размер блока для параллельного filter:" chunk-size)
   (println)
   
   (println "Стандартный filter:")
   (let [start-time (System/currentTimeMillis)
         result-seq (filter pred coll)
         _ (doall (take 10000 result-seq)) ; Принудительно вычисляем первые 10000 элементов
         end-time (System/currentTimeMillis)]
     (println "  Время выполнения:" (- end-time start-time) "мс")
     (println "  Первые 10 элементов:" (take 10 result-seq)))
   
   (println)
   (println "Параллельный filter:")
   (let [start-time (System/currentTimeMillis)
         result-seq (pfilter pred coll chunk-size)
         _ (doall (take 10000 result-seq)) ; Принудительно вычисляем первые 10000 элементов
         end-time (System/currentTimeMillis)]
     (println "  Время выполнения:" (- end-time start-time) "мс")
     (println "  Первые 10 элементов:" (take 10 result-seq)))
   
   (println)
   (println "Оптимизированный параллельный filter:")
   (let [start-time (System/currentTimeMillis)
         result-seq (pfilter-optimized pred coll chunk-size)
         _ (doall (take 10000 result-seq)) ; Принудительно вычисляем первые 10000 элементов
         end-time (System/currentTimeMillis)]
     (println "  Время выполнения:" (- end-time start-time) "мс")
     (println "  Первые 10 элементов:" (take 10 result-seq)))))

(defn -main
  "Демонстрация работы параллельного filter"
  [& args]
  (println "=== Демонстрация параллельного filter ===\n")
  
  (println "Пример 1: Фильтрация конечной последовательности")
  (let [coll (range 10000)
        result (pfilter even? coll 100)]
    (println "Входная последовательность: (range 10000)")
    (println "Предикат: even?")
    (println "Первые 10 результатов:" (take 10 result))
    (println "Всего элементов:" (count result)))
  
  (println "\n" "=" 50 "\n")
  
  (println "Пример 2: Фильтрация бесконечной последовательности")
  (let [result (pfilter #(zero? (mod % 7)) (range) 1000)]
    (println "Входная последовательность: (range) - бесконечная")
    (println "Предикат: #(zero? (mod % 7)) - числа, кратные 7")
    (println "Первые 20 результатов:" (take 20 result)))
  
  (println "\n" "=" 50 "\n")
  
  (println "Пример 3: Сравнение производительности")
  (benchmark-filter even? (range 100000) 5000)
  
  (println "\n" "=" 50 "\n")
  
  (println "Пример 4: Фильтрация с тяжелым предикатом")
  (let [heavy-pred (fn [n] 
                     (Thread/sleep 1) ; Имитация тяжелой операции
                     (even? n))
        coll (range 1000)]
    (benchmark-filter heavy-pred coll 100)))

