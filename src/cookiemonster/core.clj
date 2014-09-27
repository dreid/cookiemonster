(ns cookiemonster.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [environ.core :refer [env]]
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
  (let [
        period (* (env :poll-interval 15) 60 1000)
        id->loc (group-by :CellId (fetch locations-url))
        notification-url (env :slack-url)
        botname (env :botname "Cookie Monster")
        locations (map str/trim
                    (str/split (env :preferred-locations) #","))]
    (while true
      (log/info "Polling for cookies every: " period "ms")
      (let [cookies (fetch-cookies locations)]
        (when (not (empty? cookies))
          (log/info "Found some cookies")
          (slack! notification-url
                  (cookie-message botname
                                  locations
                                  id->loc
                                  cookies))))
      (<!! (timeout period)))))
