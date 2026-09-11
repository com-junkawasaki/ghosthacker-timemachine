(ns ghosthacker-timemachine.logs
  "GHOST HACKER: TIME MACHINE -- sample log-depth set (pure data,
  ADR-2607023200 addendum 2).

  \"inherited-server\": the five past-log depths Ren & Nei descend through
  on the father's inherited server, oldest surviving boot log first and
  the most recent (and hardest) commit last. :difficulty climbs unevenly
  on purpose (a big jump between :backup-archive and :firewall-diary) so
  a default full playthrough exercises all three
  ghosthacker-timemachine.core outcomes (:recruited / :cleared / :retreat)
  instead of a flat, monotonic grind."
  (:require [ghosthacker-timemachine.core :as core]))

(def inherited-server
  [{:depth 1 :year 2016 :label :boot-log         :difficulty 5  :ghost :static-whisper}
   {:depth 2 :year 2018 :label :backup-archive   :difficulty 9  :ghost :flicker-echo}
   {:depth 3 :year 2020 :label :firewall-diary   :difficulty 20 :ghost :packet-drift}
   {:depth 4 :year 2022 :label :quarantine-notes :difficulty 24 :ghost :null-choir}
   {:depth 5 :year 2024 :label :final-commit     :difficulty 30 :ghost :last-commit-ghost}])

(defn play-inherited-server
  "inherited-serverをフルプレイし、summaryを返す(core/play-summaryの薄い
  ラッパー)。"
  []
  (core/play-summary inherited-server))
