(ns ghosthacker-timemachine.terminal
  "GHOST HACKER: TIME MACHINE -- minimal terminal host adapter (playable
  prototype).

  Like ghosthacker-tuning/ghosthacker-echoes's terminal prototypes (and
  unlike FLOW/HARMONY), there is no real-time beat to track here -- no
  `future`/agent thread pool, no wall-clock judging. `descend` is driven
  purely by the party's accumulated `:power`, so the only thing the
  terminal actually asks the player for is whether to keep going: press
  enter to descend into the next depth, or `q` to call off the whole
  expedition early (a voluntary retreat, distinct from a failed
  `:retreat` encounter judgment -- the party can always choose to stop,
  but never chooses to fail an encounter).

  Run: clojure -M -m ghosthacker-timemachine.terminal"
  (:require [kotoba.lang.text :as str]
            [ghosthacker-timemachine.core :as core]
            [ghosthacker-timemachine.logs :as logs]))

(defn- print-depth! [depth power]
  (println (format "-- depth %d (%d) %s -- difficulty %d  (party power %d)"
                    (:depth depth) (:year depth) (name (:label depth))
                    (:difficulty depth) power)))

(defn- read-command! []
  (print "[enter=descend / q=retreat] > ") (flush)
  (some-> (read-line) str/trim str/lower))

(defn- read-valid-command!
  "\"\"(descend)か\"q\"(retreat)が入力されるまで読み直し、:descend/:retreat
   のいずれかを返す。標準入力がEOFになったら:retreatを返す(打ち切りと同じ
   scaffoldにする -- ハングしない)。"
  []
  (let [cmd (read-command!)]
    (cond
      (nil? cmd) :retreat
      (= cmd "q") :retreat
      (= cmd "") :descend
      :else (do (println "enter(descend) か q(retreat) を入力してください。")
                (recur)))))

(defn- play-loop!
  "全depthを消化するまで、depth表示 → コマンド読み取り → descend、を
   繰り返す。qまたはEOFで打ち切られたら、そこまでのstateを返す。"
  [logs]
  (loop [state core/initial-state]
    (if (core/complete? state logs)
      state
      (let [depth (core/current-depth state logs)]
        (print-depth! depth (:power state))
        (case (read-valid-command!)
          :retreat state
          :descend (let [next-state (core/descend state logs)]
                     (println (format " -> %s (power %d)"
                                       (name (last (:judgments next-state)))
                                       (:power next-state)))
                     (recur next-state)))))))

(defn -main
  "Entry point for `clojure -M -m ghosthacker-timemachine.terminal`."
  [& _args]
  (println "GHOST HACKER: TIME MACHINE — inherited-server")
  (println "父の遺したサーバの過去ログを遡り、Ghostを仲間にする。")
  (println)
  (let [state (play-loop! logs/inherited-server)
        result (core/summary state logs/inherited-server)]
    (println)
    (println "=== RESULT ===")
    (println (format "grade=%s power=%d ghosts=%s depths-cleared=%d/%d"
                      (name (:grade result))
                      (:power result)
                      (:ghosts result)
                      (:depths-cleared result)
                      (count logs/inherited-server)))))
