(ns ghosthacker-timemachine.core
  "GHOST HACKER: TIME MACHINE -- time-axis exploration RPG core (ADR-2607023200
  addendum 2: portfolio title #4, Ren & Nei's first co-op title after three
  solo-lead titles).

  Pure, host-free depth-descent/state engine: Ren & Nei descend through the
  father's inherited server's past logs, one numbered `:log-depth` at a
  time. Each depth holds exactly one encounter with a fixed `:difficulty`.
  The party has a single aggregate stat, `:power` -- there is no separate
  Ren/Nei split modeled at the core level; the co-op framing is that
  `:power` represents both of them acting together, the same way ECHOES's
  `:connection` stays a single scalar even though a whole conversation
  produced it.

  `descend` is the one clear state transition per depth (mirroring
  TUNING's `lock-in` per channel): it compares the party's current
  `:power` against the current depth's `:difficulty` and classifies the
  encounter into exactly one of three outcomes based on the margin
  (power - difficulty):

  - `:recruited` (margin >= recruit-margin) -- the party doesn't just
    survive the encounter, it wins comfortably enough that the Ghost
    living in that log agrees to join (conj onto `:ghosts`) and grants a
    real power boost (`recruit-gain`).
  - `:cleared` (0 <= margin < recruit-margin) -- the party wins, but only
    narrowly. No Ghost is won over, but a small amount of experience
    (`clear-gain`) is still gained.
  - `:retreat` (margin < 0) -- the party loses the encounter. Per this
    portfolio's convention of one unconditional transition per unit
    (no branching player choice modeled at the core level -- exactly like
    TUNING's `lock-in` always judges and always advances), the party does
    not stop here on its own: it presses on to the next depth
    automatically, but at reduced effectiveness -- `retreat-cost` is
    subtracted from `:power`, floored at `min-power` so a long losing
    streak degrades the party without ever reaching zero or negative
    power. Whether to keep going after a visible loss is a host-adapter
    decision (the terminal/web hosts let the player call off the whole
    expedition early via a quit/retreat command) -- the core itself has
    no notion of \"give up\", only of \"how did this one encounter go\".

  All three outcomes always advance `:depth-index` by exactly one, exactly
  like TUNING's `:channel-index` always advances after `lock-in`
  regardless of judgment.

  Grading deliberately avoids floating-point ratios: `grade` compares
  integer counts via cross-multiplication instead of dividing, so it never
  risks the kind of rounding surprise that bit an earlier title in this
  portfolio (0.5 + 0.05 + 0.05 is 0.6000000000000001, not 0.6) -- and,
  since this `.cljc` also compiles under ClojureScript (no `clojure.lang.Ratio`
  there), exact rational literals like `3/4` aren't portable either. Plain
  integer arithmetic sidesteps both problems.

  No rendering, input, or persistence I/O lives here -- those are host
  adapters layered on top, same split as every other title in this
  portfolio."
  )

(def initial-power
  "パーティの初期:power。ゴーストを一体も仲間にする前の、Ren & Nei二人
   だけの基礎値。"
  10)

(def recruit-margin
  "margin(power - difficulty)がこの値以上なら:recruited(ゴースト勧誘成功)。"
  4)

(def recruit-gain
  ":recruited時に:powerへ加算される値(仲間になったゴースト自身の力が
   パーティに上乗せされる)。"
  5)

(def clear-gain
  ":cleared時に:powerへ加算される値(ゴーストは付いてこないが、経験値
   としてわずかに:powerは伸びる)。"
  2)

(def retreat-cost
  ":retreat時に:powerから減算される値(反射神経の失敗ではなく、消耗として
   の敗北を表す)。"
  3)

(def min-power
  ":powerの下限。連敗してもここより下がらない(0や負値にはしない -- 探索
   が詰んで再起不能になる状態を core レベルでは作らない)。"
  1)

(defn judge-encounter
  "power(現在のパーティ:power)とdifficulty(このdepthの難易度)から margin
   (power - difficulty)を求め、:recruited / :cleared / :retreat のいずれか
   一つに分類する。"
  [power difficulty]
  (let [margin (- power difficulty)]
    (cond
      (>= margin recruit-margin) :recruited
      (>= margin 0) :cleared
      :else :retreat)))

(def initial-state
  "探索開始時点のstate。:depth-indexは呼び出し側が渡すlogsベクタに対する
   現在位置(0始まり)、:judgmentsは各depthの判定履歴(descend一回につき
   ちょうど1つ追加される)、:ghostsは勧誘に成功したゴーストのロスター。"
  {:power initial-power
   :ghosts []
   :judgments []
   :depth-index 0})

(defn- bump-power
  "stateの:powerにdeltaを加え、min-power未満にはならないようclampする。"
  [state delta]
  (update state :power #(max min-power (+ % delta))))

(defn apply-judgment
  "judgment(judge-encounterの結果)をstateに適用する。:recruitedはdepthの
   :ghostを:ghostsへ加えてrecruit-gain分:powerを増やし、:clearedは
   clear-gain分:powerを増やすのみ、:retreatはretreat-cost分:powerを
   減らす(min-power未満にはしない)。いずれの場合もjudgmentを:judgmentsへ
   積み、:depth-indexを1つ進めるのは共通の後処理。"
  [state depth judgment]
  (-> (case judgment
        :recruited (-> state
                       (update :ghosts conj (:ghost depth))
                       (bump-power recruit-gain))
        :cleared (bump-power state clear-gain)
        :retreat (bump-power state (- retreat-cost)))
      (update :judgments conj judgment)
      (update :depth-index inc)))

(defn current-depth
  "logs(ベクタ、各要素{:depth :label :difficulty :ghost ...})のうち、
   stateが現在挑戦すべきdepthを返す。全depth消化後はnil。"
  [state logs]
  (nth logs (:depth-index state) nil))

(defn complete?
  "state(logsに対する)が全depthを消化し終えたか。"
  [state logs]
  (>= (:depth-index state) (count logs)))

(defn descend
  "現在depthのdifficultyに対してstateの:powerを判定し、stateへ適用して
   次のdepthへ進める。全depth消化済みならstateをそのまま返す(呼び出し側の
   責任でcomplete?を先にチェックすること)。descendは:power駆動で決まる
   ため、外部からの入力(TUNINGのdialのようなもの)を一切必要としない --
   その分「進むか、ここで探索を切り上げるか」という player の選択は host
   アダプタ側の関心事になる(terminal/webのq/retreatコマンド)。"
  [state logs]
  (if (complete? state logs)
    state
    (let [depth (current-depth state logs)]
      (apply-judgment state depth (judge-encounter (:power state) (:difficulty depth))))))

(defn recruited-count
  "state中の:recruited判定の数。"
  [state]
  (count (filter #(= % :recruited) (:judgments state))))

(defn cleared-count
  "state中の:cleared判定の数。"
  [state]
  (count (filter #(= % :cleared) (:judgments state))))

(defn retreat-count
  "state中の:retreat判定の数。"
  [state]
  (count (filter #(= % :retreat) (:judgments state))))

(defn grade
  "logs全体に対する募集率(recruited-count / (count logs))から評価を返す。
   浮動小数(比率)へは変換せず、整数の掛け算による比較(a/b >= c/d を
   a*d >= c*b に変形)で判定する -- ClojureScriptにはRatio型が無く、
   doubleの丸め誤差も踏みたくないため。"
  [state logs]
  (let [total (count logs)
        recruited (recruited-count state)]
    (cond
      (zero? total) :untested
      (= recruited total) :legend
      (>= (* recruited 4) (* total 3)) :hero      ; recruited/total >= 3/4
      (>= (* recruited 2) total) :veteran         ; recruited/total >= 1/2
      (>= (* recruited 4) total) :rookie          ; recruited/total >= 1/4
      :else :dropout)))

(defn summary
  "runの結果サマリ。ホストアダプタ側のリザルト画面にそのまま渡せる形。"
  [state logs]
  {:power (:power state)
   :ghosts (:ghosts state)
   :ghosts-recruited (count (:ghosts state))
   :depths-cleared (:depth-index state)
   :recruited-count (recruited-count state)
   :cleared-count (cleared-count state)
   :retreat-count (retreat-count state)
   :grade (grade state logs)})

(defn play
  "logsに対し、全depthをdescendで消化するまで進める(ホストの入力を一切
   介さないpureなフルプレイ -- descendは:power駆動で決まるため外部入力が
   要らない)。"
  [logs]
  (loop [state initial-state]
    (if (complete? state logs)
      state
      (recur (descend state logs)))))

(defn play-n
  "logsに対し、最大n回だけdescendする(途中で全depth消化していれば打ち切る)。
   ホストアダプタが途中で探索を切り上げた場合の挙動を確認するための
   テスト/デモ用エントリポイント。"
  [logs n]
  (loop [state initial-state
         remaining n]
    (if (or (complete? state logs) (zero? remaining))
      state
      (recur (descend state logs) (dec remaining)))))

(defn play-summary
  "play + summaryの合成。ホストアダプタが1回の探索を録り終えた後に呼ぶ
   最短経路。"
  [logs]
  (summary (play logs) logs))
