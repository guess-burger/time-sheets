(ns time-sheets.lib
  (:require [clojure.string :as str]))

(defn ^:private comment?
  [line]
  (str/starts-with? line ";"))

(defn ^:private header?
  [line]
  (str/starts-with? line "#"))

(defn ^:private ensure-header-first
  "Ensure that a header value comes first"
  [coll]
  (let [not-header? (complement header?)]
    (if (some-> coll ffirst not-header?)
      (conj coll "#")
      coll)))

(defn ^:private split-time
  [[time name]]
  (let [hour (-> time (subs 0 2) parse-long)
        min  (-> time (subs 2)   parse-long)]
    [hour min name]))

(defn ^:private valid-time
  [[hour min]]
  (and (<= 0 hour 23)
       (<= 0 min 59)))

(defn ^:private time-spent
  [task-a task-b]
  (let [[hour-a min-a name-a] task-a
        [hour-b min-b]        task-b]
    (when (or (< hour-a hour-b)
              (< min-a min-b))
      [(+ (* 60 (- hour-b hour-a)) (- min-b min-a)) name-a])))

(defn ^:private add-times
  [[k v]]
  [k (->> v
          (map first)
          (reduce +))])

(defn ^:private all-day-time-spent
  [times]
  (let [formatted-times (->> times
                             (filter #(re-matches #"\d{4}\s+.*" %))
                             (map #(str/split % #"\s+" 2))
                             (map split-time)
                             (filter valid-time))]

    (->> (map list formatted-times (rest formatted-times))
         (keep #(apply time-spent %))
         (group-by second)
         (map add-times)
         (into {}))))

(defn parse-time-sheet
  [text]
  (->> text
       str/split-lines
       (map str/trim)
       (remove (some-fn str/blank? comment?))
       (partition-by header?)
       ensure-header-first
       (partition 2)
       (map (fn [[headers times]]
              [(-> headers last (str/replace  #"^#+" "") str/trim)
               (all-day-time-spent times)]))))

(defn day-total
  "Take a days worth of tasks and times, along with a list of tasks to excluded from the total
   and produce total time spent on non-excluded times"
  [day excl-tasks]
  (->> (apply dissoc day excl-tasks)
       vals
       (apply +)))

(comment

 (def input
   "
   ;#Monday
   0930 foo
   1000 bar
   1200 lunch
   1300 foo
   1700 home

   ; comment?

   #TUE
   0930 foo
   1000 bar
   1200 lunch
   1300 bar
   1700 home

   #WED
   #THU")

 (parse-time-sheet input)
 ;; =>
 #_([""    {"foo" 270, "bar" 120, "lunch" 60}]
    ["TUE" {"foo" 30, "bar" 360, "lunch" 60}])


 (-> (parse-time-sheet input)
     first
     second
     (day-total ["lunch"]))
 ;; => 390

 ;;
 ,)
