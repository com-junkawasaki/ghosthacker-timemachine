(ns ghosthacker-timemachine.web
  "GHOST HACKER: TIME MACHINE -- browser host adapter (ADR-2607100900
  follow-up (b)). Plain reagent, no Web Audio -- like ECHOES/TUNING,
  TIME MACHINE has no real-time beat to track (`descend` is driven purely
  by the party's accumulated `:power`), so a single button-driven
  descend/retreat UI is the whole host.

  Same input/judgment shape as ghosthacker_timemachine/terminal.clj: a
  'descend' action resolves the current depth's encounter via
  `core/descend` and advances to the next depth (or the result screen
  once every depth is exhausted); a 'retreat' action ends the expedition
  early with the party's state so far, mirroring the terminal's q/EOF
  handling."
  (:require [clojure.string :as str]
            [reagent.core :as r]
            [reagent.dom :as rdom]
            [ghosthacker-timemachine.core :as core]
            [ghosthacker-timemachine.logs :as logs]))

(defn- fresh-state []
  {:phase :exploring        ; :exploring | :result
   :logs logs/inherited-server
   :game-state core/initial-state
   :last-judgment nil})

(defonce state (r/atom (fresh-state)))

(defn- descend! []
  (let [{:keys [logs game-state]} @state
        next-gs (core/descend game-state logs)
        judgment (last (:judgments next-gs))]
    (swap! state assoc
           :game-state next-gs
           :last-judgment judgment
           :phase (if (core/complete? next-gs logs) :result :exploring))))

(defn- retreat! []
  (swap! state assoc :phase :result))

(defn- restart! []
  (reset! state (fresh-state)))

(defn- judgment-label [judgment]
  (case judgment
    :recruited "GHOST RECRUITED"
    :cleared "cleared"
    :retreat "retreat"
    ""))

(defn- exploring-screen []
  (let [{:keys [logs game-state last-judgment]} @state
        depth (core/current-depth game-state logs)]
    [:div.tm-app
     [:h1 "GHOST HACKER: TIME MACHINE"]
     [:p.tm-sub "inherited-server"]
     [:div.tm-depth (str "depth " (:depth depth) " (" (:year depth) ") -- " (name (:label depth)))]
     [:div.tm-difficulty (str "difficulty " (:difficulty depth))]
     [:div.tm-power (str "party power " (:power game-state))]
     [:div.tm-controls
      [:button.tm-retreat {:on-click retreat!} "retreat"]
      [:button.tm-descend {:on-click descend!} "descend"]]
     (when last-judgment [:div.tm-judgment (judgment-label last-judgment)])
     [:p.tm-hint (str (:depth-index game-state) "/" (count logs) " depths explored -- "
                       (count (:ghosts game-state)) " ghosts recruited")]]))

(defn- result-screen []
  (let [{:keys [logs game-state]} @state
        summary (core/summary game-state logs)]
    [:div.tm-app
     [:h1 "GHOST HACKER: TIME MACHINE"]
     [:h2 (str "grade: " (name (:grade summary)))]
     [:p (str "power " (:power summary) " / depths cleared "
              (:depths-cleared summary) "/" (count logs))]
     [:p (str "ghosts recruited: " (:ghosts-recruited summary))]
     [:p.tm-ghosts (if (seq (:ghosts summary))
                     (str "roster: " (str/join ", " (map name (:ghosts summary))))
                     "roster: (none recruited)")]
     [:button.tm-restart {:on-click restart!} "もう一度"]]))

(defn app []
  (case (:phase @state)
    :result [result-screen]
    [exploring-screen]))

(defn ^:export mount []
  (when-let [el (.getElementById js/document "app")]
    (rdom/render [app] el)))

(defn ^:export init [] (mount))
