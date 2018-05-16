(ns aikakone-backend.core
  (:require [cheshire.core :as json]
            [compojure.core :refer :all]
            [aikakone-backend.util :as util]
            [java-time :as t]
            [org.httpkit.server :as server]
            [ring.middleware.defaults :as defaults]
            [ring.middleware.cors :as cors]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer (get-sch-adapter)]
            ))


(let [{:keys [ch-recv send-fn connected-uids
              ajax-post-fn ajax-get-or-ws-handshake-fn]}
      (sente/make-channel-socket!
        (get-sch-adapter)
        {:user-id-fn (fn [req] (get-in req [:params :client-id]))})]
  (def ring-ajax-post ajax-post-fn)
  (def ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk ch-recv)
  (def chsk-send! send-fn)
  (def connected-uids connected-uids))

(def sprites-state (ref nil))

(def ranking (ref []))

(def game-start-time (ref nil))

(def sending-time-future (ref nil))

(defn- convert-to-millis [seconds nanos]
  (+ (* 1000 seconds) (/ nanos 1000000)))

(defn flip-row! [row]
  (alter sprites-state update-in [:row-flipped? row] not))

(defn flip-col! [col]
  (alter sprites-state update-in [:col-flipped? col] not))

(defn flip-diagonal-pieces! []
  (alter sprites-state update :diagonal-flipped? not))

(defn- randomize-puzzle []
  (let [non-flipped-row-or-col (reduce #(assoc %1 %2 false)
                                       {}
                                       (range util/row-col-num))]
    (ref-set sprites-state {:diagonal-flipped? false
                            :row-flipped?      non-flipped-row-or-col
                            :col-flipped?      non-flipped-row-or-col}))
  (util/randomly-execute-a-fn flip-diagonal-pieces!)
  (doseq [row-or-col (range util/row-col-num)]
    (util/randomly-execute-a-fn (fn [] (flip-row! row-or-col)))
    (util/randomly-execute-a-fn (fn [] (flip-col! row-or-col)))))

(defn- start-sending-current-playtime! []
  (future (loop []
            (Thread/sleep 200)
            (when-let [start-time @game-start-time]
              (let [duration (t/duration start-time (t/local-date-time))
                    seconds (t/value (t/property duration :seconds))
                    nanos (t/value (t/property duration :nanos))]
                (doseq [uid (:any @connected-uids)]
                  (chsk-send! uid [:aikakone/current-time (convert-to-millis seconds nanos)]))
                (recur))))))

(defn- send-data-to-all-except-message-sender [client-id message-type data]
  (doseq [uid (:any @connected-uids)]
    (when (not= client-id uid)
      (chsk-send! uid [message-type data]))))

(defn- handle-message! [{:keys [id client-id ?data]}]
  (case id
    :aikakone/sprites-state
    (dosync
      (ref-set sprites-state ?data)
      (send-data-to-all-except-message-sender client-id :aikakone/sprites-state ?data))

    :aikakone/game-start
    (dosync
      (when (empty? @sprites-state)
        (randomize-puzzle))
      (chsk-send! client-id [:aikakone/game-start @sprites-state]))

    :aikakone/start-timer
    (dosync
      (ref-set game-start-time (t/local-date-time))
      (ref-set sending-time-future (start-sending-current-playtime!)))

    :aikakone/puzzle-complete!
    (dosync
      (ref-set sprites-state nil)
      (when @game-start-time
        (ref-set game-start-time nil)
        (alter ranking (fn [ranking]
                         (take 10 (sort (conj ranking ?data))))))
      (send-data-to-all-except-message-sender client-id :aikakone/sprites-state {}))

    :aikakone/reset
    (dosync
      (ref-set game-start-time nil)
      (ref-set sprites-state nil)
      (send-data-to-all-except-message-sender client-id :aikakone/reset nil))

    nil))

(sente/start-chsk-router! ch-chsk handle-message!)

(defroutes app
           (GET "/rankings" req (json/generate-string @ranking))
           (GET "/chsk" req (ring-ajax-get-or-ws-handshake req))
           (POST "/chsk" req (ring-ajax-post req)))

(def handler
  (-> #'app
      (defaults/wrap-defaults (assoc-in defaults/site-defaults [:security :anti-forgery] false))
      (cors/wrap-cors :access-control-allow-origin [#".*"]
                      :access-control-allow-methods [:get :put :post :delete]
                      :access-control-allow-credentials ["true"])))

(defn -main []
  (server/run-server handler {:port 2222}))
