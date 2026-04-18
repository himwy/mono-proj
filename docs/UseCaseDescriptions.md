# Use Case Diagram & Descriptions

## Actors

| Actor | Description |
|---|---|
| **Player** (×4) | A participant in the game, seated at the shared computer. Four players share one screen. |
| **Tester / Power User** | The person who knows the Ctrl+Shift+E shortcut. Uses the hidden Game Editor to set up test scenarios or to create "Easter Egg" states. |

## Use case diagram (text form)

```
                 ┌───────────────────────────┐
                 │        Mini-Monopoly      │
                 │                           │
  ┌─────────┐    │  (UC1)  Roll Dice         │
  │ Player  │────┤  (UC2)  Buy Land          │
  │  (x4)   │────┤  (UC3)  Decline Buy       │
  └─────────┘    │  (UC4)  Pay Rent*         │   *auto
                 │  (UC5)  End Turn          │
                 │  (UC6)  Trade Land        │
                 │  (UC7)  View Land List    │
                 │  (UC8)  Declare Bankruptcy*│  *auto
                 │                           │
  ┌─────────┐    │  (UC9)  Open Game Editor  │
  │ Tester  │────┤  (UC10) Edit Player State │
  │         │────┤  (UC11) Edit Land Owner   │
  └─────────┘    │  (UC12) Force Turn        │
                 │  (UC13) Load Slot Data*   │   *at start
                 └───────────────────────────┘
```

Note on *auto*-use-cases: "Pay Rent", "Declare Bankruptcy", and
"Load Slot Data" run automatically — the player does not click a
button to trigger them. They are listed here so markers can see the
full set of system responsibilities.

---

## Use case descriptions

Each description lists:

- **Actor**: who starts the use case.
- **Actor actions**: what the actor does.
- **System processing**: rules and calculations.
- **System response**: what the actor/other players see.

### UC1 — Roll Dice

- **Actor:** current Player (turn holder).
- **Actor actions:**
  1. Click **Roll Dice** while the phase is AWAITING_ROLL.
- **System processing:**
  1. Dice generates a random integer in `[1, 10]`.
  2. Player's `position = (position + roll) mod boardSize`.
  3. If wrap-around occurred, credit $2000 GO bonus.
  4. Resolve the landed slot (see branching table below).
- **System response:**
  - Token moves to the new slot.
  - Log: `"Player N rolled a X."`, `"Player N landed on <slot>."`.
  - Status panel balance / position update.
  - Buttons enable/disable to reflect the new phase.

Landing branches:

| Landed-on | Resulting phase |
|---|---|
| Non-land (GO, Free Parking) | AWAITING_END_TURN |
| Own land | AWAITING_END_TURN |
| Other player's land | Auto-pay rent, then AWAITING_END_TURN |
| Unowned land, affordable | AWAITING_BUY_DECISION |
| Unowned land, unaffordable | AWAITING_END_TURN |

### UC2 — Buy Land

- **Actor:** current Player.
- **Actor actions:**
  1. Click **Buy Land** while the phase is AWAITING_BUY_DECISION.
- **System processing:**
  1. Verify balance ≥ land price.
  2. Deduct price; set land's owner to the player; add land to
     player's `ownedLands`.
  3. Phase → AWAITING_END_TURN.
- **System response:** slot top-stripe colour changes to the buyer's
  colour; status card lists the new land; log entry recorded.

### UC3 — Decline Buy

- **Actor:** current Player.
- **Actor actions:** click **Decline Buy**.
- **System processing:** phase → AWAITING_END_TURN.
- **System response:** log entry `"Player N declined to buy."`.

### UC4 — Pay Rent (automatic)

- **Actor:** current Player (but triggered by the system during
  Roll Dice resolution).
- **System processing:**
  1. Compute base rent = 10 % of land price.
  2. Multiplier = consecutive same-side owned lands (1×, 2×, 3×
     cap).
  3. If payer can afford → debit payer, credit owner.
  4. Otherwise → transfer remaining balance, mark bankrupt (UC8).
- **System response:** balances update; log shows amount paid and
  multiplier bonus if any.

### UC5 — End Turn

- **Actor:** current Player.
- **Actor actions:** click **End Turn** (also available in
  AWAITING_BUY_DECISION — counts as decline).
- **System processing:**
  1. Advance `currentPlayerIndex` to the next ACTIVE player.
  2. Phase → AWAITING_ROLL.
- **System response:** current-turn header updates; all buttons
  reset to reflect the new player's phase.

### UC6 — Trade Land

- **Actor:** current Player during AWAITING_ROLL.
- **Actor actions:**
  1. Click **Trade**.
  2. Choose Buy or Sell direction.
  3. Choose counterparty.
  4. Choose a land owned by the seller.
  5. Enter an agreed price.
- **System processing:**
  1. Verify phase = AWAITING_ROLL.
  2. Verify seller actually owns the land.
  3. Verify buyer can afford the agreed price.
  4. Transfer money and ownership.
- **System response:** slot owner colour updates; both players' cards
  update; log records `"TRADE: …"`.

### UC7 — View Land Ownership

- **Actor:** any player.
- **Actor actions:** click **Show Land Ownership** at any time.
- **System response:** modal showing a table of every land with slot
  number, name, price, and current owner.

### UC8 — Declare Bankruptcy (automatic)

- **Actor:** system, triggered during rent resolution in UC4.
- **System processing:**
  1. Credit remaining balance to the creditor.
  2. Set player's balance to 0, status to BANKRUPT.
  3. Reset every owned land's owner to `null`; clear
     `ownedLands`.
  4. Call `checkForWinner()` — if only one ACTIVE player remains,
     phase → GAME_OVER.
- **System response:** all the player's lands' top-stripe becomes
  grey; status card shows BANKRUPT; the winner dialog pops up when
  applicable.

### UC9 — Open Game Editor

- **Actor:** Tester.
- **Actor actions:** press **Ctrl + Shift + E** anywhere in the
  window.
- **System response:** modal `GameEditorDialog` appears.

### UC10 — Edit Player State

- **Actor:** Tester.
- **Actor actions:** in the Player tab of the editor, choose a
  player, enter a new balance / position, pick a status, click
  **Apply**.
- **System processing:**
  - Balance clamped to ≥ 0.
  - Position validated against `[0, boardSize − 1]`.
  - Setting BANKRUPT triggers the full bankruptcy routine.
- **System response:** token, balance, and status card refresh;
  `[Editor]` log line recorded.

### UC11 — Edit Land Ownership

- **Actor:** Tester.
- **Actor actions:** in the Land tab, pick any land, pick the new
  owner (or "— None —"), Apply.
- **System processing:** previous owner loses the deed; new owner
  gains it (or land becomes unowned).
- **System response:** slot stripe colour changes; owner's card list
  updates.

### UC12 — Force Turn

- **Actor:** Tester.
- **Actor actions:** in the Turn tab, pick a player, Apply.
- **System processing:** `currentPlayerIndex` set; phase →
  AWAITING_ROLL.
- **System response:** current-turn header updates.

### UC13 — Load Slot Data (automatic, at startup)

- **Actor:** system.
- **System processing:**
  1. `DataLoader.loadSlots("data/slots.csv")` parses each line.
  2. Malformed data triggers an `IllegalArgumentException` that is
     surfaced in a `JOptionPane`.
- **System response:** board initialises with loaded slots, or an
  error dialog if the file is missing/corrupt.
