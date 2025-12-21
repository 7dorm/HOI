(ns task-c2.core-test
  (:require [clojure.test :refer :all]
            [task-c2.core :refer :all]))

(deftest test-primes-sequence-basic
  (testing "Первые несколько простых чисел"
    (is (= (take 10 primes) [2 3 5 7 11 13 17 19 23 29]))))

(deftest test-primes-sequence-first-primes
  (testing "Первые 20 простых чисел"
    (is (= (take 20 primes)
           [2 3 5 7 11 13 17 19 23 29 31 37 41 43 47 53 59 61 67 71]))))

(deftest test-primes-sequence-no-duplicates
  (testing "В последовательности нет дубликатов"
    (let [first-100 (take 100 primes)]
      (is (= (count first-100) (count (distinct first-100)))))))

(deftest test-primes-sequence-sorted
  (testing "Последовательность отсортирована по возрастанию"
    (let [first-50 (take 50 primes)]
      (is (= first-50 (sort first-50))))))

(deftest test-nth-prime
  (testing "Получение n-го простого числа"
    (is (= (nth-prime 0) 2))
    (is (= (nth-prime 1) 3))
    (is (= (nth-prime 2) 5))
    (is (= (nth-prime 3) 7))
    (is (= (nth-prime 4) 11))
    (is (= (nth-prime 9) 29))
    (is (= (nth-prime 99) 541))
    (is (= (nth-prime 999) 7919))))

(deftest test-primes-up-to
  (testing "Простые числа до указанного предела"
    (is (= (primes-up-to 10) [2 3 5 7]))
    (is (= (primes-up-to 20) [2 3 5 7 11 13 17 19]))
    (is (= (primes-up-to 30) [2 3 5 7 11 13 17 19 23 29]))
    (is (= (primes-up-to 2) [2]))
    (is (= (primes-up-to 1) []))))

(deftest test-prime-predicate-true
  (testing "Проверка простых чисел (должны быть true)"
    (is (prime? 2))
    (is (prime? 3))
    (is (prime? 5))
    (is (prime? 7))
    (is (prime? 11))
    (is (prime? 13))
    (is (prime? 17))
    (is (prime? 19))
    (is (prime? 23))
    (is (prime? 29))
    (is (prime? 31))
    (is (prime? 37))
    (is (prime? 41))
    (is (prime? 43))
    (is (prime? 47))
    (is (prime? 53))
    (is (prime? 59))
    (is (prime? 61))
    (is (prime? 67))
    (is (prime? 71))
    (is (prime? 73))
    (is (prime? 79))
    (is (prime? 83))
    (is (prime? 89))
    (is (prime? 97))
    (is (prime? 101))
    (is (prime? 541))
    (is (prime? 7919))))

(deftest test-prime-predicate-false
  (testing "Проверка составных чисел (должны быть false)"
    (is (not (prime? 1)))
    (is (not (prime? 0)))
    (is (not (prime? -1)))
    (is (not (prime? 4)))
    (is (not (prime? 6)))
    (is (not (prime? 8)))
    (is (not (prime? 9)))
    (is (not (prime? 10)))
    (is (not (prime? 12)))
    (is (not (prime? 14)))
    (is (not (prime? 15)))
    (is (not (prime? 16)))
    (is (not (prime? 18)))
    (is (not (prime? 20)))
    (is (not (prime? 21)))
    (is (not (prime? 22)))
    (is (not (prime? 24)))
    (is (not (prime? 25)))
    (is (not (prime? 27)))
    (is (not (prime? 28)))
    (is (not (prime? 30)))
    (is (not (prime? 100)))
    (is (not (prime? 1000)))))

(deftest test-primes-sequence-infinite
  (testing "Последовательность действительно бесконечна (можно взять много элементов)"
    (let [first-1000 (take 1000 primes)]
      (is (= (count first-1000) 1000))
      (is (every? #(> % 1) first-1000))
      (is (every? prime? first-1000)))))

(deftest test-primes-sequence-lazy
  (testing "Последовательность ленивая (можно взять элементы по требованию)"
    (let [first-5 (take 5 primes)
          first-10 (take 10 primes)]
      (is (= (count first-5) 5))
      (is (= (count first-10) 10))
      (is (= (take 5 first-10) first-5)))))

(deftest test-sieve-algorithm-correctness
  (testing "Правильность алгоритма Решета Эратосфена"
    ; Проверяем, что все числа в последовательности действительно простые
    (let [first-100 (take 100 primes)]
      (doseq [p first-100]
        ; Проверяем, что число больше 1
        (is (> p 1))
        ; Проверяем, что число не делится ни на одно из предыдущих простых
        (let [previous-primes (take-while #(< % p) primes)]
          (is (every? #(not (zero? (mod p %))) previous-primes)))))))

(deftest test-edge-cases
  (testing "Граничные случаи"
    ; Первое простое число
    (is (= (first primes) 2))
    ; Второе простое число
    (is (= (second primes) 3))
    ; Простые числа до 2
    (is (= (primes-up-to 2) [2]))
    ; Простые числа до 3
    (is (= (primes-up-to 3) [2 3]))
    ; Большие простые числа
    (is (prime? 997))
    (is (prime? 9973))
    (is (not (prime? 9999)))))

(deftest test-primes-sequence-performance
  (testing "Последовательность работает эффективно для больших индексов"
    ; Проверяем, что можем получить 1000-е простое число
    (let [thousandth-prime (nth-prime 999)]
      (is (> thousandth-prime 7900))
      (is (< thousandth-prime 8000))
      (is (prime? thousandth-prime)))))

