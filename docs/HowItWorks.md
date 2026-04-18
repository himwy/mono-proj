# How Mini-Monopoly Works — Live Demonstration Script

This document walks through the Mini-Monopoly game end-to-end. Use it
as a script during the Week 14 live demo. Each section lists what to
show, what to say, and what the audience should see on screen.

---

## 1. Starting the game

**Action:** run `run.bat` (or `java -cp bin Main`).

**What appears:** a 7×7 game board with 24 perimeter slots. The
bottom-right corner is **GO** (slot 0). Slots 1–22 are purchasable
lands named after Hong Kong locations. Slot 23 (top-right corner) is
"Free Parking" — a non-functional decorative slot.

**Right-hand panel:** four player cards (Player 1–4), each showing
balance ($2000), position (0), status (ACTIVE), and owned-land
summary (empty at start).

**Bottom panel:** action buttons — Roll Dice, Buy Land, Decline Buy,
End Turn, Trade, Show Land Ownership.

**Game log:** bottom-right area, shows every action taken.

---

## 2. Basic turn — Roll & Move

**Action:** click **Roll Dice**.

**What happens internally:**

1. `GameController` fires on the button.
2. It calls `GameModel.rollDice()`.
3. The model rolls the `Dice` (1–10), advances the current player's
   position by `(oldPos + roll) % 24`, awards $2000 if GO is passed,
   and then resolves the landing slot.
4. The model emits a state-changed event. The view repaints the
   board (new token position) and the status panel (new balance /
   position).

**What to show:**

- The current-player badge (top of status panel) is highlighted in
  the player's colour.
- The token dot moves to the new slot.
- The log prints `"Player 1 rolled a 5."` and
  `"Player 1 landed on Lamma Island (slot 5)."`.

---

## 3. Buying an unowned land

**Action:** if the landed slot is an **unowned land** and the player
can afford it, the **Buy Land** button becomes enabled.

Click **Buy Land**.

**What happens:**

- `GameModel.buyCurrentLand()` deducts the price from the current
  player's balance, sets the slot's owner to the player, and adds
  the slot to the player's owned-lands list.
- The cell's top stripe changes to the owner's colour.
- The player's status card now lists this land under "Owns: …".

You may also click **Decline Buy** to skip.

---

## 4. Paying rent (base 10%)

**Action:** roll again so that another player lands on a slot owned
by Player 1 (use the Editor to speed this up — see §8).

**What happens:**

- The model detects an owner mismatch and calls
  `Board.computeRent(slot)`.
- Base rent = 10 % of land price.
- The model deducts rent from the visitor, credits the owner, and
  logs `"Player 2 owes $150 rent to Player 1."`.

---

## 5. Consecutive-land bonus (Advanced Requirement A)

**Set-up** (via Editor):

1. Give Player 1 ownership of slots 1, 2, 3 (three consecutive lands
   on the south side).
2. Force Player 2's turn and position onto slot 3.

**What happens next dice roll (or land on slot 3):**

- `Board.countConsecutiveOwnership(slot 3)` walks left and right on
  the same side, counting Player 1's consecutive lands → 3.
- Multiplier becomes **3×** (capped at 3× no matter how long the run).
- Rent paid is `price × 0.10 × 3`.
- The log annotates: `"… (3x consecutive bonus)"`.

**Edge cases to demo:**

- Remove slot 2 → run breaks → only 1 consecutive on each side → 1× multiplier.
- Own two adjacent only → 2× multiplier.
- Span across a side boundary (e.g. slots 6 and 7 are on different
  sides) → they do **not** combine.

---

## 6. Trading (Advanced Requirement B)

**Pre-condition:** the current player is in the `AWAITING_ROLL`
phase (hasn't rolled yet this turn).

**Action:** click **Trade**.

Dialog flow:

1. Choose Buy or Sell direction.
2. Choose a counterparty from other active players.
3. Choose a land that the seller owns.
4. Enter an agreed price.

**What happens:**

- `GameModel.trade(buyer, seller, land, price)` validates the
  request (buyer can afford, seller owns the land, both active),
  transfers money, and re-assigns ownership.
- The board repaints with the new owner colour on the slot.

---

## 7. Bankruptcy & winning

**Set-up:** lower a player's balance to, say, $50 via the Editor,
then force them to land on an expensive owned land.

**What happens:**

- The rent exceeds the visitor's balance.
- The model transfers whatever balance remains to the owner, sets
  the visitor's balance to 0, and calls
  `Player.declareBankruptcy()`: all their lands reset to "No Owner".
- The model calls `checkForWinner()`. When only one active player
  remains, the phase becomes `GAME_OVER` and the winner is announced
  in a pop-up.

---

## 8. Game Editor (Ctrl + Shift + E)

**Action:** press **Ctrl + Shift + E** anywhere in the window.

A tabbed dialog opens:

- **Player tab** — change any player's balance, position, or status.
- **Land tab** — force the owner of any land to any player, or None.
- **Turn tab** — force the turn to any player.

Every editor action is logged as `[Editor] …` so graders can see
exactly what was changed during the demo.

This dialog is the "hidden Game Editor / Easter Egg" mandated by the
project outline.

---

## 9. Reloading / editing slot data

The file `data/slots.csv` holds every slot's number, name, price,
land flag, and side index. Because nothing is hard-coded, a teacher
can open the file in Notepad, rename "Repulse Bay" to something else
or change the price, save, and re-launch the game to see the change
immediately.

---

## Closing notes for the demo

- Emphasise MVC separation: the view does no rules, the model does
  no drawing, the controller only wires events.
- Point out the observer pattern: `GameModel.Listener` lets any
  number of views subscribe without tight coupling.
- Finish with the editor to show the "what if" scenarios quickly.
