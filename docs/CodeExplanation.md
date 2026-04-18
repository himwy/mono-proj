# Code Explanation — file-by-file walkthrough

This document explains every class in the project. Its purpose is to
prove that the code is our own work: it records what each file does,
*why* certain design choices were made, and how the pieces connect.
Skim it before the demo so every team member can answer "why did you
do it that way?" for any file.

Package layout mirrors MVC:

```
src/
├── Main.java                  ← bootstrap
├── model/                     ← rules + state (no Swing)
│   ├── Slot.java
│   ├── Player.java
│   ├── Dice.java
│   ├── Board.java
│   ├── DataLoader.java
│   └── GameModel.java
├── view/                      ← Swing UI (no rules)
│   ├── GameView.java
│   ├── BoardPanel.java
│   ├── StatusPanel.java
│   ├── ControlPanel.java
│   ├── LogPanel.java
│   └── GameEditorDialog.java
└── controller/
    └── GameController.java
```

---

## Model layer

### `Slot.java`

A single position on the board.

- Fields: `number`, `name`, `price`, `isLand`, `side`, `owner`.
- The `side` integer (1=South, 2=West, 3=North, 4=East) is used by
  the consecutive-ownership calculation. Non-land slots use
  `SIDE_NONE`.
- `getBaseRent()` returns **10 % of the land price** — the base
  rent. Any multiplier is applied later by `Board.computeRent`, not
  here, because the multiplier depends on neighbours.
- We kept `Slot` deliberately small so it's easy to test in isolation.

### `Player.java`

Represents one of the four players.

- Holds `position`, `balance`, `status` (ACTIVE/BANKRUPT),
  `ownedLands`, plus an identity (`id`, `name`, `color`).
- Movement logic is **one method**: `advance(int steps, int boardSize)`
  returns `true` if the player passed GO. We keep the wrap-around
  arithmetic here so the GameModel doesn't repeat it.
- `debit(int)` returns `false` when the player cannot pay — the
  caller uses that as the bankruptcy signal. We deliberately do
  **not** throw exceptions for normal game events.
- `declareBankruptcy()` resets every owned land's `owner` to `null`
  and clears the list. This matches the rulebook's "lose all lands"
  behaviour.

### `Dice.java`

A thin wrapper over `java.util.Random`. Constructed with an
injectable `Random` so we *could* seed it for repeatable tests. The
game spec says a dice produces 1–10, so the constants live here.

### `Board.java`

An ordered list of `Slot`s plus the consecutive-ownership logic.

- `countConsecutiveOwnership(Slot land)` walks left and right from
  the given land and counts how many **same-side** lands share the
  same owner. It stops as soon as the side changes or an unowned /
  different-owner slot is hit.
- `computeRent(Slot land)` maps that count to the rent multiplier:
  1 → 1×, 2 → 2×, ≥3 → 3× (capped). Base × multiplier is the total
  rent.

Putting this logic on `Board` (not `Slot` or `Player`) is a
deliberate choice: rent depends on a span of slots, which is a board
concern, not a single-slot or single-player concern.

### `DataLoader.java`

Reads `data/slots.csv`. Each non-blank, non-`#` line is
`slotNumber,slotName,price,isLand,side`. We used plain CSV rather
than JSON / XML so the product owner (Mr. Chan) can edit it in
Notepad, fulfilling the *"do not hard-code slot data"* requirement.

### `GameModel.java`

The heart of the project. It owns:

- the `Board`,
- the four `Player`s (constructed with fixed colours and starting
  balances),
- the `Dice`,
- a `currentPlayerIndex` turn cursor,
- a `Phase` enum (AWAITING_ROLL, AWAITING_BUY_DECISION,
  AWAITING_END_TURN, GAME_OVER), and
- a list of `Listener`s used by the view (observer pattern).

Main methods:

| Method | What it does |
|---|---|
| `rollDice()` | rolls, advances, applies GO bonus, calls `resolveLanding` |
| `resolveLanding(Player, Slot)` | computes next phase: buy-opportunity, auto-rent, or just end-turn |
| `buyCurrentLand()` / `declineBuy()` | handle the two post-roll choices on an unowned land |
| `endTurn()` | advance the turn cursor, skipping bankrupt players |
| `trade(...)` | advanced-requirement peer-to-peer land sale |
| `editorSet*` | editor-only mutators, each emitting a `[Editor]` log line |
| `checkForWinner()` | detects "last player standing" and sets GAME_OVER |

Every mutator ends with `fireChanged()` so the view re-renders; log
messages go via `onLog(String)` so the view doesn't need to parse
state changes for the activity stream.

---

## View layer (Swing)

### `GameView.java`

The `JFrame` window. It assembles four sub-panels:

- `BoardPanel` in the centre,
- `StatusPanel` + `LogPanel` stacked in the east column,
- `ControlPanel` along the south.

It exposes the sub-panels as `public final` fields so the controller
can attach listeners. The view itself intentionally has no rules.

### `BoardPanel.java`

Paints the 7×7 perimeter board.

- `perimeterCoords()` returns the grid positions in order starting
  at GO (bottom-right) and going counter-clockwise, matching the
  `Slot` order.
- Each perimeter position becomes a `SlotCell` (inner class). The
  cell draws:
  - the owner colour band across the top,
  - the slot name (word-wrapped so long names like "Ma On Shan" fit),
  - the price for land slots, `"GO"` for slot 0,
  - player tokens (small coloured circles) for any player on that slot,
  - an orange highlight rectangle if it is the current player's slot.
- `refresh()` just repaints every cell — the panel is stateless aside
  from the cell references.

### `StatusPanel.java`

Shows a coloured "current turn" header plus four `PlayerCard`
widgets. Each card prints the player's balance, position, status, and
a comma-separated list of owned lands. Refreshes completely on every
model change; the dataset is tiny so there is no performance issue.

### `ControlPanel.java`

Row of buttons. `updateForPhase(phase, gameOver)` enables/disables
each button depending on the current phase — e.g. `Roll Dice` is only
enabled when the phase is `AWAITING_ROLL`. This single method keeps
all button-enablement rules in one place.

### `LogPanel.java`

A read-only `JTextArea` wrapped in a scroll pane. Exposes `append()`
for the controller to push log lines.

### `GameEditorDialog.java`

The hidden Game Editor. Three tabs:

1. **Player** — select a player, type a new balance / position, pick a
   status (ACTIVE / BANKRUPT), click Apply.
2. **Land** — pick any land, pick a new owner (or "— None —"), apply.
3. **Turn** — force the current turn to any of the four players.

The dialog never mutates model state directly — it always calls
`GameModel.editorSet*` methods so all validation and logging stays
centralised.

---

## Controller layer

### `GameController.java`

The glue.

- Constructor: registers itself as a `GameModel.Listener`, attaches
  action listeners to every control button, and installs the
  Ctrl + Shift + E key binding for the editor.
- `onGameStateChanged` and `onLog` are called by the model; the
  controller schedules view refreshes on the Swing EDT via
  `SwingUtilities.invokeLater`.
- Dialog helpers (`openTradeDialog`, `showLandOwnership`,
  `openEditor`) host all the user-facing prompts. They never contain
  rule checks; they only collect input and hand it to the model,
  which decides whether the action is legal.
- `gameOverDialogShown` is a simple flag so the victory pop-up only
  appears once even though the model fires multiple "changed" events
  after the game ends.

---

## `Main.java`

Entry point. It:

1. Runs on the Swing Event Dispatch Thread via
   `SwingUtilities.invokeLater`.
2. Loads `data/slots.csv` through `DataLoader`.
3. Builds the `Board`, `GameModel`, `GameView`, and
   `GameController`.
4. Shows the window and prints a welcome banner in the log.

Any error loading the data file is surfaced as a `JOptionPane` error
dialog so the user is never faced with a silent failure.

---

## Design-pattern notes

- **MVC**: `model/` has no Swing imports; `view/` has no rules logic;
  `controller/` owns only wiring and dialogs.
- **Observer**: `GameModel.Listener` decouples the model from the
  view. The current code only has one listener, but the hook would
  let us add e.g. a network client or a replay recorder later.
- **Encapsulation**: mutating methods live on the object that owns
  the invariant — `Player.declareBankruptcy()`,
  `Board.computeRent()`, etc.
- **Single source of truth**: every mutation goes through
  `GameModel`, which ends with `fireChanged()` + a log line. Nothing
  else changes player balances or slot owners.
