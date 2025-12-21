(ns task-c3.core-test
  (:require [clojure.test :refer :all]
            [task-c3.core :refer :all]))

(deftest test-pfilter-basic
  (testing "Базовый тест параллельного filter"
    (let [coll [1 2 3 4 5 6 7 8 9 10]
          result (pfilter even? coll 3)]
      (is (= (seq result) [2 4 6 8 10])))))

(deftest test-pfilter-empty-collection
  (testing "Фильтрация пустой коллекции"
    (is (empty? (pfilter even? [])))
    (is (empty? (pfilter even? '())))))

(deftest test-pfilter-all-match
  (testing "Все элементы удовлетворяют предикату"
    (let [coll (range 10)
          result (pfilter #(>= % 0) coll 5)]
      (is (= (count result) 10))
      (is (= (seq result) (seq coll))))))

(deftest test-pfilter-none-match
  (testing "Ни один элемент не удовлетворяет предикату"
    (let [coll (range 10)
          result (pfilter #(< % 0) coll 5)]
      (is (empty? result)))))

(deftest test-pfilter-infinite-sequence
  (testing "Фильтрация бесконечной последовательности"
    (let [result (pfilter even? (range) 1000)
          first-10 (take 10 result)]
      (is (= first-10 [0 2 4 6 8 10 12 14 16 18]))
      ; Проверяем, что первые 100 элементов результата - это 100 четных чисел
      (is (= (count (take 100 result)) 100))
      ; Проверяем, что все они четные
      (is (every? even? (take 100 result))))))

(deftest test-pfilter-laziness
  (testing "Проверка ленивости параллельного filter"
    (let [call-count (atom 0)
          pred (fn [x]
                 (swap! call-count inc)
                 (even? x))
          coll (range 1000)
          result (pfilter pred coll 100)
          first-5 (take 5 result)]
      ; Проверяем, что предикат вызывается только для необходимых элементов
      (is (= first-5 [0 2 4 6 8]))
      ; Количество вызовов должно быть ограничено (не все 1000)
      (is (< @call-count 1000)))))

(deftest test-pfilter-chunk-size
  (testing "Разные размеры блоков"
    (let [coll (range 100)
          result-small (pfilter even? coll 10)
          result-large (pfilter even? coll 50)
          result-default (pfilter even? coll)]
      (is (= (seq result-small) (seq result-large)))
      (is (= (seq result-small) (seq result-default))))))

(deftest test-pfilter-complex-predicate
  (testing "Сложный предикат"
    (let [coll (range 100)
          result (pfilter #(and (even? %) (> % 10)) coll 20)]
      (is (= (seq result) [12 14 16 18 20 22 24 26 28 30 32 34 36 38 40 42 44 46 48 50 52 54 56 58 60 62 64 66 68 70 72 74 76 78 80 82 84 86 88 90 92 94 96 98])))))

(deftest test-pfilter-optimized-basic
  (testing "Базовый тест оптимизированного параллельного filter"
    (let [coll (range 100)
          result (pfilter-optimized even? coll 20 2)]
      (is (= (seq result) (seq (filter even? coll)))))))

(deftest test-pfilter-optimized-infinite
  (testing "Оптимизированный filter с бесконечной последовательностью"
    (let [result (pfilter-optimized #(zero? (mod % 3)) (range) 1000 3)
          first-10 (take 10 result)]
      (is (= first-10 [0 3 6 9 12 15 18 21 24 27])))))

(deftest test-pfilter-equivalence
  (testing "Эквивалентность результатов стандартного и параллельного filter"
    (let [coll (range 1000)
          standard-result (filter even? coll)
          parallel-result (pfilter even? coll 100)]
      (is (= (seq standard-result) (seq parallel-result))))))

(deftest test-pfilter-large-collection
  (testing "Фильтрация большой коллекции"
    (let [coll (range 100000)
          result (pfilter odd? coll 5000)
          first-10 (take 10 result)]
      (is (= first-10 [1 3 5 7 9 11 13 15 17 19]))
      (is (= (count result) 50000)))))

(deftest test-pfilter-preserves-order
  (testing "Параллельный filter сохраняет порядок элементов"
    (let [coll [5 2 8 1 9 3 7 4 6 10]
          result (pfilter #(> % 5) coll 3)]
      (is (= (seq result) [8 9 7 6 10])))))

(deftest test-pfilter-with-nil
  (testing "Обработка nil значений"
    (let [coll [1 nil 2 nil 3 nil 4]
          result (pfilter nil? coll 3)]
      (is (= (seq result) [nil nil nil])))))

(deftest test-pfilter-with-strings
  (testing "Фильтрация строк"
    (let [coll ["apple" "banana" "cherry" "date" "elderberry"]
          result (pfilter #(> (count %) 5) coll 2)]
      (is (= (seq result) ["banana" "cherry" "elderberry"])))))

(deftest test-pfilter-performance-indicator
  (testing "Индикатор производительности - параллельный filter должен работать"
    (let [coll (range 10000)
          start-time (System/currentTimeMillis)
          result (doall (pfilter even? coll 500))
          end-time (System/currentTimeMillis)
          duration (- end-time start-time)]
      ; Проверяем, что функция выполняется за разумное время
      (is (< duration 10000)) ; Меньше 10 секунд
      (is (= (count result) 5000)))))

(deftest test-partition-chunks
  (testing "Разбиение на блоки"
    (let [coll (range 10)
          chunks (partition-chunks coll 3)]
      (is (= (seq chunks) [[0 1 2] [3 4 5] [6 7 8] [9]])))))

(deftest test-pfilter-block
  (testing "Фильтрация отдельного блока"
    (let [block [1 2 3 4 5 6 7 8 9 10]
          result (pfilter-block even? block)]
      (is (= (seq result) [2 4 6 8 10])))))

(deftest test-pfilter-chunk-boundaries
  (testing "Границы блоков обрабатываются корректно"
    (let [coll (range 25)
          result (pfilter odd? coll 7)]
      ; В диапазоне 0-24 нечетных чисел: 1,3,5,7,9,11,13,15,17,19,21,23 = 12 чисел
      (is (= (count result) 12))
      (is (= (first result) 1))
      (is (= (last result) 23)))))

