# Dice and move animation

You tap two pip dice at the bottom to roll on your turn. The CPU rolls itself, then both sides’ checkers slide so every move is visible.

## Flow

- New game waits: **Tap the dice to roll**. Board is locked until the roll settles.
- Tap plays a short tumble, then the real faces lock. Doubles still show two dice.
- Choosing a destination slides that checker. Hits apply when the slide ends.
- After your last die, the CPU tumbles automatically and slides each planned move.
- Then the app waits for your tap again.
- Resume keeps an already-rolled position. Empty dice on your turn wait for a tap.

## Rules / engine

- `Engine.planTurn` returns the CPU move list without applying a visual skip.
- `boardWithoutMover` is the committed board minus the in-flight checker.
- Physical dice stay `rollA`/`rollB`; remaining moves stay in `dice`.
