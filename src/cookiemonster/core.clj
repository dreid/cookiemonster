(ns cookiemonster.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [environ.core :refer [env]]
            [clojure.core.async
              :refer (timeout <!! go <!)]))

(def locations-url
  "http://www.specialtys.com/Wcf/SpProxy.svc/LoadPickupLocations")

(def cookies-url
  "http://www.specialtys.com/Wcf/SpProxy.svc/LoadCookieAlerts")

(defn fetch [url]
  (-> (client/post url {:as :json})
    :body
    :d))

(defn seq-contains? [s target]
  (some #(= target %) s))

(defn fetch-cookies [locations]
  (group-by :CellId
            (filter #(seq-contains? locations (:CellId %))
                    (distinct (fetch cookies-url)))))


(defn format-cookie [cookie]
  (str (:Name cookie) " (" (:Age cookie) "m)"))

(defn format-cookies [location cookies]
  (apply str (interpose ", " (map format-cookie cookies))))

(defn cookie-message [username locations id->loc cookies]
  {:text "Warm cookies nearby!"
   :username username
   :icon_emoji ":cookie:"
   :fields (for [l locations
                 :let [local-cookies (cookies l)
                       location (first (id->loc l))]
                 :when (not (empty? local-cookies))]
              {:title (:StreetAddress location)
               :short false
               :value (format-cookies location local-cookies)})})

(defn diff-cookies [previous-cookies new-cookies]
  (filter (fn [cookie]
             (not-any? (fn [old-cookie]
                         (and (= (:Name old-cookie) (:Name cookie))
                              (= (:CellId old-cookie) (:CellId cookie))
                              (< (:Age old-cookie) (:Age cookie))))
                       previous-cookies))
           new-cookies))

(defn slack! [url message]
  (client/post url {:form-params message
                    :content-type :json}))

(defn poll []
  (let [
        period (* (Integer/parseInt (env :poll-interval "15")) 60 1000)
        id->loc (group-by :CellId (fetch locations-url))
        notification-url (env :slack-url)
        botname (env :botname "Cookie Monster")
        locations (map str/trim
                    (str/split (env :preferred-locations) #","))]
    (let [previous-cookies (atom nil)]
        (go (while true
          (log/info "Polling for cookies every: " period "ms")
          (let [cookies (fetch-cookies locations)]
            (when (not (empty? cookies))
              (log/info "Found some cookies")
              (let [new-cookies (diff-cookies @previous-cookies cookies)]
                (when (not (empty? new-cookies))
                  (log/info "Found new cookies")
                  (slack! notification-url
                          (cookie-message botname
                                          locations
                                          id->loc
                                          cookies)))))
            (swap! previous-cookies cookies))
          (<! (timeout period)))))))

(defn -main [& argv]
  (case (first argv)
    "worker" (<!! (poll))))
