(ns cubs-sign.core
  (:require [environ.core :refer [env]]
            [org.httpkit.client :as http]
            [clojure.data.json :as json])
  (:gen-class))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Retrieve data on the Chicago Cubs and do something with it. ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def sports-api-key (env :sports-api-key))
(def sports-api-password (env :sports-api-password))
(def sports-api-url (env :sports-api-url))
(def ^:dynamic *cubs-id* "131")

(defn GET
  ([url f] (GET url {} f))
  ([url options f]
   (println (str sports-api-url url))
   (http/get (str sports-api-url url)
             (merge options {:basic-auth [sports-api-key sports-api-password]
                             :insecure? true}) ;; sorry mama
             f)))

(defn get-cubs-data
  "Throw away games that don't involve the Cubbies"
  [data]
  (-> data
      (get-in ["scoreboard" "gameScore"])
      (->> (filter #(or (= (get-in % ["game" "homeTeam" "ID"]) *cubs-id*)
                        (= (get-in % ["game" "awayTeam" "ID"]) *cubs-id*))))))

(defn get-scoreboards
  "Retrieve the all game scoreboards from the server."
  []
  (GET "mlb/current/scoreboard.json?fordate=2018081"
       (fn [{:keys [status body error]}]
         (if (= status 200)
           (println (get-cubs-data (json/read-str body)))
           (println error)))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Go Cubs Go!"))


