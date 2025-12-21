(ns task-c1.core-test
  (:require [clojure.test :refer :all]
            [task-c1.core :refer :all]))

(deftest test-generate-strings-basic
  (testing "Базовый тест: алфавит (\"a\" \"b\" \"c\"), N=2"
    (let [result (generate-strings ["a" "b" "c"] 2)
          expected #{"ab" "ac" "ba" "bc" "ca" "cb"}]
      (is (= (count result) 6))
      (is (= (set result) expected)))))

(deftest test-generate-strings-length-1
  (testing "Тест для N=1: должны быть все символы алфавита"
    (let [result (generate-strings ["a" "b" "c"] 1)]
      (is (= (set result) #{"a" "b" "c"})))))

(deftest test-generate-strings-length-3
  (testing "Тест для N=3: алфавит (\"a\" \"b\" \"c\")"
    (let [result (generate-strings ["a" "b" "c"] 3)]
      ; Должно быть 3 * 2 * 2 = 12 строк
      (is (= (count result) 12))
      ; Проверяем, что нет повторяющихся подряд символов
      (is (every? (fn [s]
                    (not-any? (fn [[a b]] (= a b))
                              (partition 2 1 s)))
                  result)))))

(deftest test-generate-strings-two-chars
  (testing "Тест для алфавита из 2 символов: (\"x\" \"y\"), N=4"
    (let [result (generate-strings ["x" "y"] 4)]
      ; Должно быть 2 * 1 * 1 * 1 = 2 строки (xyxy и yxyx)
      (is (= (count result) 2))
      (is (= (set result) #{"xyxy" "yxyx"})))))

(deftest test-generate-strings-no-consecutive
  (testing "Проверка отсутствия повторяющихся подряд символов"
    (let [result (generate-strings ["a" "b" "c" "d"] 5)]
      (is (every? (fn [s]
                    ; Проверяем, что нет двух одинаковых символов подряд
                    (not-any? (fn [[a b]] (= a b))
                              (partition 2 1 s)))
                  result)))))

(deftest test-generate-strings-empty-alphabet
  (testing "Тест для пустого алфавита"
    (let [result (generate-strings [] 2)]
      (is (empty? result)))))

(deftest test-generate-strings-zero-length
  (testing "Тест для N=0"
    (let [result (generate-strings ["a" "b" "c"] 0)]
      (is (empty? result)))))

(deftest test-generate-strings-negative-length
  (testing "Тест для отрицательного N"
    (let [result (generate-strings ["a" "b" "c"] -1)]
      (is (empty? result)))))

