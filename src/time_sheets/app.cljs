(ns time-sheets.app
  (:require
   [reagent.dom :as dom]
   [re-frame.core :as rf]
   [time-sheets.lib :as lib]
   [time-sheets.util :as util]))

;; Effects
(rf/reg-fx
 :put-storage
 (fn [[key value]] (util/put-local-storage key value)))

(defn persist
  [key]
  (rf/->interceptor
   :id      :persist
   :after  (fn [context]
              (let [db (rf/get-effect context :db)]
                (rf/assoc-effect context :put-storage [key (get db key)])))))

(rf/reg-cofx
 :local-storage
 (fn [cofx store-key]
   (let [found (util/get-local-storage store-key)]
     (assoc-in cofx [:local-storage store-key] found))))

;; Event Handlers

(rf/reg-event-fx
 :initialise
 [(rf/inject-cofx :local-storage :text) (rf/inject-cofx :local-storage :ignored)]
 (fn [cofx _]
   (let [db      (:db cofx)
         text    (or (get-in cofx [:local-storage :text])
                     (str "# MON\n0900 Task A\n1200 Lunch\n1230 Task A\n1400 Meeting A\n1445 Task B\n1700 home\n\n"
                          "# TUE\n0900 Task A\n; comment\n1030 Meeting B\n1045 Task B\n1200 Lunch\n1230 Task C\n1600 Task B \n1700 home"))
         ignored (set (or (get-in cofx [:local-storage :ignored])
                          #{"Lunch"}))]
     {:db (assoc db
                 :text    text
                 :times   (lib/parse-time-sheet text)
                 :ignored ignored)})))

(rf/reg-event-db
 :text-change
 [(persist :text)]
 (fn [db [_ new-text]]
   (assoc db
          :text new-text
          ;; TODO is this technically a materialsed view of text
          ;;  so should be separate reg-sub dependent on text?
          :times (lib/parse-time-sheet new-text))))

(rf/reg-event-db
 :toggle-task
 [(persist :ignored)]
 (fn [db [_ task checked?]]
   (update db :ignored (if checked? disj conj) task)))

;; Subscriptions

(rf/reg-sub
 :text
 (fn [db _]
   (:text db)))

(rf/reg-sub
 :times
 (fn [db _]
   (:times db)))

(rf/reg-sub
 :ignored
 (fn [db _]
   (:ignored db)))

(rf/reg-sub
 :times-table
 :<- [:times]
 :<- [:ignored]
 (fn [[times ignored] _]
   ;; is kind of just how the lib should return that data... but with a key
   (let [fmted-times (map-indexed (fn [idx [day task-times]]
                              ;; using index because day can be non-unique...
                              ;; not 100% sure it's a great idea
                              {:key        (str idx "-" day)
                               :day        day
                               :task-times task-times})
                            times)
         ;; FIXME don't think we need the index here?
         tasks (->> times
                    (mapcat (comp keys second))
                    (into #{})
                    (map-indexed (fn [idx task] [idx task]))
                    (sort-by second))
         excl-totals ignored

         ;; convert minutes to decimal hours (like sales-force)
         fmt-time #(/ % 60)]

     {:days         (map #(select-keys % [:key :day]) fmted-times)

      :task-rows    (for [[_ task] tasks]
                      {:task      task
                       :disabled? (contains? excl-totals task)
                       :times     (for [{:keys [key day task-times]} fmted-times
                                        :let [time (fmt-time (get task-times task))]]
                                    ;; using the same key as days
                                    {:key  key
                                     :day  day
                                     :time time})})

      :daily-totals (map (fn [{:keys [key task-times]}]
                           {:key   key
                            :total (fmt-time (lib/day-total task-times excl-totals))})
                         fmted-times)})))

;; Views

(defn timesheet-table
  []
  (let [{:keys [days task-rows daily-totals]} @(rf/subscribe [:times-table])]
    [:table.table.table-bordered
     [:thead
      [:tr
       [:td ""]
       [:td ""]
       [:<>
        (for [{:keys [key day]} days]
          [:th {:key key :scope "col"} day])]]]
     [:tbody
      [:<>
       (for [{:keys [task disabled? times]} task-rows]
         [:tr {:class (when disabled? "disabled")
               :key   task}
          [:th {:scope "row"} [:input {:id        task :type "checkbox"
                                       :checked   (not disabled?)
                                       :on-change #(rf/dispatch [:toggle-task task (-> % .-target .-checked)])}]]
          [:th {:scope "row"} [:label {:for task} task]]
          [:<>
           (for [{:keys [time day]} times]
             [:td {:key (str  day "_" time)} time])]])]]
     [:tfoot
      [:tr
       [:td ""]
       [:th {:scope "row"} "Total"]
       [:<>
        (for [{:keys [key total]} daily-totals]
          [:td {:key key} total])]]]]))

(defn ui
  []
  [:div.row
   [:div.col.col-lg-4
    [:textarea.font-monospace
     {:type          "text"
      :default-value @(rf/subscribe [:text])
      :on-input      #(rf/dispatch [:text-change (-> % .-target .-value)])}]]
   [:div.col.col-lg-8
    [timesheet-table]]
   #_[:footer.text-center.text-lg-start.text-muted
      [:section.d-flex.justify-content-center.justify-content-lg-between.p-4.border-bottom
       [:div
        [:p "What goes here?"]]]]])

(defn init
  []
  (rf/dispatch-sync [:initialise])
  (dom/render [ui]
              (js/document.getElementById "container")))