(ns aikakone-backend.util)

(def row-col-num 5)

(defn randomly-execute-a-fn [f]
  (when (< (rand) 0.5) (f)))
