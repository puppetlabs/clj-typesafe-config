(ns puppetlabs.config.typesafe_test
  (:import (java.io FileInputStream))
  (:require [puppetlabs.config.typesafe :as ts]
            [clojure.test :refer :all]))

(def test-files-dir "./dev-resources/puppetlabs/config/file/")

(deftest configfile->map-test
  (testing "can parse .properties file with nested data structures"
    (let [cfg (ts/config-file->map (str test-files-dir "config.properties"))]
      (is (= {:foo {:bar "barbar"
                    :baz "bazbaz"
                    :bam 42
                    :bap {:boozle "boozleboozle"}}}
             cfg))))
  (testing "can parse .json file with nested data structures"
    (let [cfg (ts/config-file->map (str test-files-dir "config.json"))]
      (is (= {:foo {:bar "barbar"
                    :baz "bazbaz"
                    :bam 42
                    :bap {:boozle "boozleboozle"
                          :bip [1 2 {:hi "there"} 3]}}}
             cfg))))
  (testing "can parse .conf file with nested data structures"
    (let [cfg (ts/config-file->map (str test-files-dir "config.conf"))]
      (is (= {:foo {:bar "barbar"
                    :baz "bazbaz"
                    :bam 42
                    :bap {:boozle "boozleboozle"
                          :bip [1 2 {:hi "there"} 3]}}}
             cfg)))))

(deftest reader->map-test
  (testing "can parse .properties stream with nested data structures"
    (let [cfg (ts/reader->map
                (FileInputStream. (str test-files-dir "config.properties"))
                :properties)]
      (is (= {:foo {:bar "barbar"
                    :baz "bazbaz"
                    :bam 42
                    :bap {:boozle "boozleboozle"}}}
             cfg))))
  (testing "can parse .json stream with nested data structures"
    (let [cfg (ts/reader->map
                (FileInputStream. (str test-files-dir "config.json"))
                :json)]
      (is (= {:foo {:bar "barbar"
                    :baz "bazbaz"
                    :bam 42
                    :bap {:boozle "boozleboozle"
                          :bip [1 2 {:hi "there"} 3]}}}
             cfg))))
  (testing "can parse .conf stream with nested data structures"
    (let [cfg (ts/reader->map
                (FileInputStream. (str test-files-dir "config.conf"))
                :conf)]
      (is (= {:foo {:bar "barbar"
                    :baz "bazbaz"
                    :bam 42
                    :bap {:boozle "boozleboozle"
                          :bip [1 2 {:hi "there"} 3]}}}
             cfg)))))

