(ns cubs-sign.core
  (:require [environ.core :refer [env]]
            [org.httpkit.client :as http]
            [clojure.data.json :as json])
  (:gen-class))

;;;;;;;;;;;;;;;;;;;
;; Some globals. ;;
;;;;;;;;;;;;;;;;;;;

(def cubs-score (atom -1))
(def opponent-score (atom -1))

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
  (let [gamedata
        (-> data
            (get-in ["scoreboard" "gameScore"])
            (->> (filter #(or (= (get-in % ["game" "homeTeam" "ID"]) *cubs-id*)
                              (= (get-in % ["game" "awayTeam" "ID"]) *cubs-id*))))
            (last))]
    (cond
      (nil? gamedata) (do (reset! cubs-score -1)
                          (reset! opponent-score -1))
      (= (get-in gamedata ["game" "homeTeam" "ID"] *cubs-id*))
      (do
        (reset! cubs-score (Integer/parseInt (get gamedata "homeScore")))
        (reset! opponent-score (Integer/parseInt (get gamedata  "awayScore"))))
      :default
      (do
        (reset! cubs-score (Integer/parseInt (get gamedata  "awayScore")))
        (reset! opponent-score (Integer/parseInt (get gamedata  "homeScore")))))
    [@cubs-score @opponent-score]))

(defn get-scoreboards
  "Retrieve the all game scoreboards from the server."
  []
  (GET (str "mlb/current/scoreboard.json?fordate=" (.format (java.text.SimpleDateFormat. "yyyyMMdd")
                                                            (java.util.Date.))
            "&force=false")
       (fn [{:keys [status body error]}]
         (if (= status 200)
           (println (get-cubs-data (json/read-str body)))
           (println status)))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (while true
    (get-scoreboards)
    (Thread/sleep (* 15 1000))))


;;;;;;;;;;;
;; DEBUG ;;
;;;;;;;;;;;

(def mockdata
  {"scoreboard" {"gameScore" [{"game" {"awayTeam" {"ID" *cubs-id*}
                                       "homeTeam" {"ID" "666"}}
                               "homeScore" "7"
                               "awayScore" "10"}]}})
