(ns ghosthacker-timemachine.terminal-test
  "-mainそのものはテストせず(標準入出力をそのまま使うため)、private var
   経由でread-valid-command!/play-loop!を直接叩く。実プロセスとしての
   -main自体は手動検証済み(完走/q途中打ち切り/EOF/不正コマンドの再入力
   要求、いずれも正しく完了しプロセスがハングしないことを確認)。

   TIME MACHINEの:power/:difficultyはすべて整数なので、TUNINGの
   close-to?のようなfloat epsilonヘルパーは不要 -- ここでは登場しない。"
  (:require [clojure.test :refer [deftest is testing]]
            [ghosthacker-timemachine.core :as core]
            [ghosthacker-timemachine.logs :as logs]
            [ghosthacker-timemachine.terminal :as terminal]))

(def ^:private read-valid-command! #'terminal/read-valid-command!)
(def ^:private play-loop! #'terminal/play-loop!)

(defn- silently [thunk]
  (let [result (atom nil)]
    (with-out-str (reset! result (thunk)))
    @result))

(deftest read-valid-command-descend-test
  (testing "空行(enter)は:descendを返す"
    (is (= :descend (silently #(with-in-str "\n" (read-valid-command!)))))))

(deftest read-valid-command-retreat-test
  (testing "qは:retreatを返す"
    (is (= :retreat (silently #(with-in-str "q\n" (read-valid-command!))))))
  (testing "EOFも:retreatを返す(ハングしない)"
    (is (= :retreat (silently #(with-in-str "" (read-valid-command!)))))))

(deftest read-valid-command-invalid-input-test
  (testing "不正コマンドは読み飛ばし、次の有効なコマンドを処理する"
    (is (= :descend (silently #(with-in-str "xyz\n\n" (read-valid-command!)))))
    (is (= :retreat (silently #(with-in-str "nope\nq\n" (read-valid-command!)))))))

(deftest play-loop-full-descend-test
  (testing "全depthをenterで消化し切れば完走する(手計算と一致)"
    (let [state (silently
                 #(with-in-str "\n\n\n\n\n" (play-loop! logs/inherited-server)))]
      (is (core/complete? state logs/inherited-server))
      (is (= [:recruited :recruited :cleared :retreat :retreat] (:judgments state)))
      (is (= [:static-whisper :flicker-echo] (:ghosts state)))
      (is (= 16 (:power state))))))

(deftest play-loop-early-retreat-test
  (testing "途中でq/EOFになれば、そこまでのstateで打ち切る(未完走)"
    (let [state (silently #(with-in-str "\n\nq\n" (play-loop! logs/inherited-server)))]
      (is (not (core/complete? state logs/inherited-server)))
      (is (= 2 (:depth-index state)))
      (is (= [:recruited :recruited] (:judgments state)))
      (is (= 20 (:power state)))))
  (testing "1回もenterせずEOFになれば、開始stateのまま打ち切る"
    (let [state (silently #(with-in-str "" (play-loop! logs/inherited-server)))]
      (is (= core/initial-state state)))))
