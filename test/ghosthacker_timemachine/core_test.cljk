(ns ghosthacker-timemachine.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [ghosthacker-timemachine.core :as core]
            [ghosthacker-timemachine.logs :as logs]))

(deftest judge-encounter-tiers
  (testing "margin(power - difficulty)によるrecruited/cleared/retreatの境界"
    (is (= :recruited (core/judge-encounter 14 10)))              ; margin 4 == recruit-margin
    (is (= :recruited (core/judge-encounter 20 10)))               ; margin 10, well over
    (is (= :cleared (core/judge-encounter 13 10)))                 ; margin 3, just under recruit-margin
    (is (= :cleared (core/judge-encounter 10 10)))                 ; margin 0, exact tie still a win
    (is (= :retreat (core/judge-encounter 9 10)))                  ; margin -1
    (is (= :retreat (core/judge-encounter 0 30)))))                ; margin way negative

(deftest apply-judgment-recruited
  (testing ":recruitedはghostを加え、:powerをrecruit-gain分増やす"
    (let [depth {:depth 1 :difficulty 5 :ghost :static-whisper}
          after (core/apply-judgment core/initial-state depth :recruited)]
      (is (= [:static-whisper] (:ghosts after)))
      (is (= (+ core/initial-power core/recruit-gain) (:power after)))
      (is (= [:recruited] (:judgments after)))
      (is (= 1 (:depth-index after))))))

(deftest apply-judgment-cleared
  (testing ":clearedはghostを加えず、:powerをclear-gain分だけ増やす"
    (let [depth {:depth 1 :difficulty 10 :ghost :packet-drift}
          after (core/apply-judgment core/initial-state depth :cleared)]
      (is (= [] (:ghosts after)))
      (is (= (+ core/initial-power core/clear-gain) (:power after)))
      (is (= [:cleared] (:judgments after)))
      (is (= 1 (:depth-index after))))))

(deftest apply-judgment-retreat
  (testing ":retreatはghostを加えず、:powerをretreat-cost分減らす"
    (let [depth {:depth 1 :difficulty 99 :ghost :null-choir}
          after (core/apply-judgment core/initial-state depth :retreat)]
      (is (= [] (:ghosts after)))
      (is (= (- core/initial-power core/retreat-cost) (:power after)))
      (is (= [:retreat] (:judgments after)))
      (is (= 1 (:depth-index after))))))

(deftest apply-judgment-retreat-floors-at-min-power
  (testing "連敗しても:powerはmin-power未満にならない"
    (let [depth {:depth 1 :difficulty 999 :ghost :void}
          low-state (assoc core/initial-state :power 2)
          after (core/apply-judgment low-state depth :retreat)]
      ;; 2 - retreat-cost(3) would be -1 without the floor.
      (is (= core/min-power (:power after)))
      (let [after2 (core/apply-judgment after depth :retreat)]
        (is (= core/min-power (:power after2)))))))

(def ^:private depths
  [{:depth 1 :difficulty 5 :ghost :a}
   {:depth 2 :difficulty 10 :ghost :b}
   {:depth 3 :difficulty 15 :ghost :c}])

(deftest current-depth-and-complete
  (testing "depth-indexに応じてcurrent-depth/complete?が正しく動く"
    (is (= {:depth 1 :difficulty 5 :ghost :a} (core/current-depth core/initial-state depths)))
    (is (not (core/complete? core/initial-state depths)))
    (let [s3 (assoc core/initial-state :depth-index 3)]
      (is (nil? (core/current-depth s3 depths)))
      (is (core/complete? s3 depths)))))

(deftest descend-judges-current-depth-and-advances
  (testing "descendは現在depthのdifficultyに対して判定し、次depthへ進む"
    (let [s1 (core/descend core/initial-state depths)] ; power 10 vs difficulty 5 -> margin 5 -> recruited
      (is (= :recruited (last (:judgments s1))))
      (is (= [:a] (:ghosts s1)))
      (is (= 1 (:depth-index s1)))
      (let [s2 (core/descend s1 depths)] ; power 15 vs difficulty 10 -> margin 5 -> recruited
        (is (= :recruited (last (:judgments s2))))
        (is (= 2 (:depth-index s2)))
        (let [s3 (core/descend s2 depths)] ; power 20 vs difficulty 15 -> margin 5 -> recruited
          (is (= :recruited (last (:judgments s3))))
          (is (core/complete? s3 depths)))))))

(deftest descend-no-op-when-complete
  (testing "全depth消化済みならdescendはstateをそのまま返す"
    (let [done (assoc core/initial-state :depth-index (count depths))]
      (is (= done (core/descend done depths))))))

(deftest recruited-cleared-retreat-counts
  (testing "各カウンタは:judgmentsから正しく集計される"
    (let [s (assoc core/initial-state
                   :judgments [:recruited :recruited :cleared :retreat])]
      (is (= 2 (core/recruited-count s)))
      (is (= 1 (core/cleared-count s)))
      (is (= 1 (core/retreat-count s))))))

(deftest grade-thresholds
  (testing "grade は recruited-count/total の比率(整数比較)で決まる"
    (let [total4 (vec (repeat 4 {}))]
      ;; legend: recruited == total
      (is (= :legend (core/grade (assoc core/initial-state
                                        :judgments [:recruited :recruited :recruited :recruited])
                                 total4)))
      ;; hero: recruited/total >= 3/4 (but not all recruited)
      (is (= :hero (core/grade (assoc core/initial-state
                                      :judgments [:recruited :recruited :recruited :cleared])
                               total4)))
      ;; veteran: recruited/total >= 1/2, below 3/4
      (is (= :veteran (core/grade (assoc core/initial-state
                                         :judgments [:recruited :recruited :cleared :cleared])
                                  total4)))
      ;; rookie: recruited/total >= 1/4, below 1/2
      (is (= :rookie (core/grade (assoc core/initial-state
                                        :judgments [:recruited :cleared :cleared :cleared])
                                 total4)))
      ;; dropout: below 1/4 (including zero)
      (is (= :dropout (core/grade (assoc core/initial-state
                                         :judgments [:cleared :cleared :retreat :retreat])
                                  total4))))
    (testing "logsが空なら:untested"
      (is (= :untested (core/grade core/initial-state []))))))

(deftest summary-shape
  (testing "summaryはpowerと勧誘済みghosts、depths-cleared、gradeを含む"
    (let [state (-> core/initial-state
                    (core/apply-judgment {:depth 1 :difficulty 5 :ghost :a} :recruited)
                    (core/apply-judgment {:depth 2 :difficulty 10 :ghost :b} :cleared))
          result (core/summary state depths)]
      (is (= (+ core/initial-power core/recruit-gain core/clear-gain) (:power result)))
      (is (= [:a] (:ghosts result)))
      (is (= 1 (:ghosts-recruited result)))
      (is (= 2 (:depths-cleared result)))
      (is (= 1 (:recruited-count result)))
      (is (= 1 (:cleared-count result)))
      (is (= 0 (:retreat-count result))))))

(deftest play-and-play-summary-full-inherited-server
  (testing "inherited-serverをフルプレイした結果は手計算と一致する
            (power 10 -recruited-> 15 -recruited-> 20 -cleared-> 22
             -retreat-> 19 -retreat-> 16)"
    (let [state (core/play logs/inherited-server)]
      (is (core/complete? state logs/inherited-server))
      (is (= [:recruited :recruited :cleared :retreat :retreat] (:judgments state)))
      (is (= [:static-whisper :flicker-echo] (:ghosts state)))
      (is (= 16 (:power state))))
    (let [result (core/play-summary logs/inherited-server)]
      (is (= 16 (:power result)))
      (is (= 2 (:ghosts-recruited result)))
      (is (= 5 (:depths-cleared result)))
      (is (= 2 (:recruited-count result)))
      (is (= 1 (:cleared-count result)))
      (is (= 2 (:retreat-count result)))
      ;; recruited 2 / total 5: 2*4=8 >= 5 (rookie), 2*2=4 < 5 (not veteran)
      (is (= :rookie (:grade result))))))

(deftest play-n-stops-early
  (testing "play-nはn回descendしたところで打ち切る(全depth未消化)"
    (let [state (core/play-n logs/inherited-server 2)]
      (is (not (core/complete? state logs/inherited-server)))
      (is (= 2 (:depth-index state)))
      (is (= [:recruited :recruited] (:judgments state)))
      (is (= 20 (:power state))))
    (testing "nがdepth数以上ならフルプレイと同じ結果になる"
      (is (= (core/play logs/inherited-server)
             (core/play-n logs/inherited-server 100))))))
