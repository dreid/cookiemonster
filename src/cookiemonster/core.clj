(ns cookiemonster.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.core.async
              :refer (timeout <!!)]))

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

(defn read-config [fname]
  (edn/read-string (slurp fname)))

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
   :fields (for [l locations]
            (let [local-cookies (cookies l)
                  location (first (id->loc l))]
              (when (not (empty? local-cookies))
                {:title (:StreetAddress location)
                 :short false
                 :value (format-cookies location local-cookies)})))})

(defn slack! [url message]
  (client/post url {:form-params message
                    :content-type :json}))

(defn -main [& argv]
  (let [conf (read-config (first argv))
        period (* (:poll-minutes conf) 60 1000)
        id->loc (group-by :CellId (fetch locations-url))
        locations (:preferred-locations conf)]
    (while true
      (log/info "Polling for cookies every: " period "ms")
      (let [cookies (fetch-cookies locations)]
        (when (not (empty? cookies))
          (log/info "Found some cookies")
          (slack! (:notification-url conf)
                  (cookie-message (:botname conf)
                                  locations
                                  id->loc
                                  cookies))))
      (<!! (timeout period)))))
