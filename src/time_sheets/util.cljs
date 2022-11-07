(ns time-sheets.util)

(defn get-local-storage
  [key]
  (some->> key
           name
           (.getItem js/localStorage)
           (.parse js/JSON)))

(defn put-local-storage
  [key value]
  (let [json-value (->> value
                        clj->js
                        (.stringify js/JSON))]
    (js/localStorage.setItem (name key) json-value)))