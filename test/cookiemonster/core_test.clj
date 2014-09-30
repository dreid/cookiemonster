(ns cookiemonster.core-test
  (:require [clojure.test :refer :all]
            [cookiemonster.core :refer :all]))

(def id->loc (group-by :CellId [
  {:CellId "SF10" :StreetAddress "680 Folsom St."}
  {:CellId "SF02" :StreetAddress "101 New Montgomery."}
  {:CellId "SF09" :StreetAddress "500 Howard"}]))

(def cookies (group-by :CellId [
  {:CellId "SF10" :Name "Oatmeal Raisin" :Age 3}
  {:CellId "SF02" :Name "Chocolate Chip" :Age 56}
  {:CellId "SF09" :Name "Weatgerm" :Age 54}]))

(def locations ["SF10" "SF02"])

(deftest test-cookie-message
  (testing "Format cookies.")
    (is (= (cookie-message "Monster" locations id->loc cookies)
           {:text "Warm cookies nearby!"
            :username "Monster"
            :icon_emoji ":cookie:"
            :fields [{:title "680 Folsom St."
                      :short false
                      :value "Oatmeal Raisin (3m)"}
                     {:title "101 New Montgomery."
                      :short false
                      :value "Chocolate Chip (56m)"}]})))
