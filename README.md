# GHOST HACKER: TIME MACHINE

![test](https://github.com/com-junkawasaki/ghosthacker-timemachine/actions/workflows/test.yml/badge.svg)

Ghost Hacker ゲームポートフォリオ第4弾。設計は
[ADR-2607023200](../../../90-docs/adr/2607023200-ghosthacker-game-portfolio-flow.md)
（superproject `com-junkawasaki/root`、addendum 2）を参照。

[Ghost Hacker](https://github.com/com-junkawasaki/ghosthacker)（既存カノン: Ren/Nei、
「情報は物理だ」、情報場、Ghost Battle / Daemon Battle）を土台に、FreeTEMPOの
『Life』（2010）収録曲 "Time Machine" に由来する、10ジャンル展開の第4弾。
FLOW/HARMONY/ECHOES/TUNINGがいずれもRen単独・Nei単独だったのに対し、
**Ren & Nei共同**が初めて主人公になるタイトル。

## コンセプト

- **ジャンル**: RPG（時間軸探索型）
- **主人公**: Ren & Nei 共同
- **コアループ**: 父の遺したサーバに残る過去ログを、番号の振られた
  "log-depth" として順に遡る。各depthには固定の`:difficulty`を持つ
  encounterが1つあり、パーティの単一集約値`:power`との差
  （margin = power - difficulty）で判定が3種類に一つに決まる:
  - `:recruited`（margin >= 4）— 余裕を持って勝ち、そのログに宿る
    Ghostが仲間になる（`:ghosts`へ追加）うえ`:power`も大きく伸びる
  - `:cleared`（0 <= margin < 4）— かろうじて勝つが、Ghostは付いて
    こない。経験値としてわずかに`:power`が伸びる
  - `:retreat`（margin < 0）— 負ける。TUNINGの`lock-in`が判定して
    必ず次のchannelへ進むのと同じように、core側では「ここで諦める」
    という分岐を作らず、必ず次のdepthへ進める——ただし`:power`が
    消耗分だけ下がる（下限あり、0や負値にはならない）。探索そのものを
    切り上げるかどうかはhostアダプタ側の関心事（terminal/webのq/retreat
    コマンド）
  - `:power`にはRen/Nei個別のステータスを持たせていない——ECHOESの
    `:connection`が単一スカラーのまま会話全体の結果を表すのと同じで、
    共同プレイの帰結を1つの集約値で表現している

## 実装範囲

`src/ghosthacker_timemachine/core.cljk` — pure、host-free。判定/state核:

- `judge-encounter` — margin(power - difficulty)を`:recruited`/`:cleared`/
  `:retreat`に判定
- `descend`/`current-depth`/`complete?` — 現在depthを判定して次へ進める、
  全depth消化判定
- `recruited-count`/`cleared-count`/`retreat-count`/`grade`/`summary` —
  リザルト画面向けの集計とグレード判定。**gradeは浮動小数の比率ではなく
  整数の掛け算で比較する**（ClojureScriptにRatio型が無いことと、
  0.5 + 0.05 + 0.05 が 0.6000000000000001 になるような丸め誤差の
  両方を踏まないため）
- `play`/`play-n`/`play-summary` — 全depthをまとめて消化する統合API

`src/ghosthacker_timemachine/logs.cljk` — サンプルの完結したログセット
（`inherited-server`、5depth）。`:difficulty`の伸び方をわざと不均一にし
（`:firewall-diary`で大きく跳ねる）、デフォルトのフルプレイで
`:recruited`/`:cleared`/`:retreat`の3judgmentすべてが出るよう調整済み
（testで検証: power 10 → 15(recruited) → 20(recruited) → 22(cleared) →
19(retreat) → 16(retreat)、grade=`:rookie`）。

**プレイ可能な最小プロトタイプ**として `src/ghosthacker_timemachine/terminal.cljk`
がある。FLOW/HARMONYと違い実時間のビート判定が無いため、`future`/agent
スレッドプールを一切使わない素朴なループ。`descend`は`:power`だけで
決まるため、terminalが実際にプレイヤーへ尋ねるのは「続けるか、ここで
探索を切り上げるか」だけ（enter=descend、q=retreat。EOFもretreat扱い）。

**ブラウザで遊べるホストアダプタ**が `src/ghosthacker_timemachine/web.cljk`
（reagent、ADR-2607100900 follow-up (b)）: TIME MACHINEもリアルタイムの
ビート判定が無いため、ECHOES/TUNINGと同じ低複雑度側の構成（Web Audio
不要、ボタン駆動のdescend/retreat UI）で足りる。

## 開発

```bash
clojure -M:test
```

Lint（clj-kondo、Clojars経由でHomebrew等の別インストール不要）:

```bash
clojure -M:lint
```

`main`へのpush/PRで `.github/workflows/test.yml` が自動でテスト+lintを実行する。

`src/ghosthacker_timemachine/bounded.kotoba` は、固定5深度の
`inherited-server`だけを対象にしたcapability-freeなKotobaプロファイル。
任意ログ、途中終了、Ghost identity roster、terminal/browser状態を同じ契約へ
暗黙に狭めず、それらはCLJC oracleに残す。CIは同じ整数判定と最終集計を
restricted Web JavaScriptとtyped Wasmの両方で実行する。

ターミナルで遊んでみる:

```bash
clojure -M -m ghosthacker-timemachine.terminal
```

ブラウザで遊んでみる（`npm install`は初回のみ）:

```bash
npm install
npx shadow-cljs watch app   # http://localhost:8298 で自動リロード開発
npx shadow-cljs release app # public/ に静的バンドルをビルド(デプロイ可能)
```

変更履歴は [CHANGELOG.md](CHANGELOG.md)。

## ライセンス

MIT License — [LICENSE](LICENSE) 参照。
