# Changelog

pure `.cljc` 時間軸探索RPG核（`ghosthacker-timemachine.core`）と、それを
使うプロトタイプ実装の変更履歴（ADR-2607023200 addendum 2）。

## Unreleased

- 初期実装: `core.cljc`（log-depth降下判定`descend`、party `:power`と
  margin(power - difficulty)からの`:recruited`/`:cleared`/`:retreat`
  三択判定、ghosts roster、`recruited-count`/`cleared-count`/
  `retreat-count`、整数比較のみで浮動小数を使わない`grade`、
  `play`/`play-n`/`play-summary`）、`logs.cljc`（サンプルログセット
  `inherited-server`、5depth、`:recruited`/`:cleared`/`:retreat`の
  3judgmentすべてを default playthrough で踏む難易度カーブ）、
  `terminal.clj`（プレイ可能なenter=descend/q=retreat REPLプロトタイプ、
  `future`/agentスレッドプール不使用）、`web.cljs`（ブラウザhost
  アダプタ、reagent、ADR-2607100900 follow-up (b)）。18 tests /
  78 assertions、clj-kondo 0 errors/warnings。headless DOM上での実
  クリック操作による通し（5depth descend → result画面 → もう一度で
  初期状態に復帰）を検証済み。
