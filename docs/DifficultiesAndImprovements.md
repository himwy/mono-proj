# Difficulties Encountered & Possible Improvements

## Difficulties we encountered

### 1. Deciding on the board geometry

The outline mentions *~44 spots* on the visual board but *~23 functional
slots* in the additional notes. We initially tried to model both —
functional land slots plus decorative corners — but this complicated
the modulo wrap-around logic in `Player.advance` and made the
consecutive-ownership rule ambiguous across corners.

**Resolution:** we settled on a single model of 24 slots (indices
0–23) arranged around a 7×7 grid, where slot 0 is GO, slots 1–22 are
the 22 lands, and slot 23 is a non-functional "Free Parking". This
keeps the model simple (one flat list, one modulo operation) while
still giving the board the visual square shape the outline expects.

### 2. The consecutive-ownership rule on a ring

Monopoly sides are bounded: the corners break the run. But in our
flat list representation, slot 22 is adjacent to slot 23 which is
adjacent to slot 0 — no natural corner to stop at.

**Resolution:** we added a `side` field to each `Slot`. The
consecutive-counting loop in `Board.countConsecutiveOwnership` stops
as soon as the side changes, so slots 1–6 can form a chain but slot
6 and slot 7 (different sides) cannot, even though they are
physically adjacent in the ring.

### 3. When exactly is trading allowed?

The outline says trading happens "before rolling the dice" but does
not explicitly forbid trading after rolling. We tightened it to only
the `AWAITING_ROLL` phase because allowing it mid-turn would open
the door to avoiding rent by selling the land you're about to land
on.

**Resolution:** `GameModel.trade` rejects trades outside
`AWAITING_ROLL` and logs the reason.

### 4. Repeat game-over pop-ups

Our model fires a change event every time anything mutates. When the
game ends we kept receiving `onGameStateChanged` callbacks (e.g.
from the editor updating balances for a post-mortem), and the
victory dialog reappeared each time.

**Resolution:** added a `gameOverDialogShown` boolean in
`GameController` that suppresses subsequent pop-ups after the first
one.

### 5. Swing EDT safety

Model events can fire from any thread (in our case they happen to
all be on the EDT, but defensively we didn't want to assume so).
Refreshing Swing components off the EDT can produce subtle paint
bugs.

**Resolution:** every view refresh in the controller is wrapped in
`SwingUtilities.invokeLater`.

### 6. Long land names breaking the cell layout

"Tsim Sha Tsui", "Causeway Bay", and "Ma On Shan" did not fit on a
single line of the 90×90 px cells with a readable font.

**Resolution:** wrote a tiny word-wrapping routine
(`SlotCell.drawWrappedName`) that manually splits on whitespace
using `FontMetrics`. Not perfect typography but robust.

### 7. Editor-state mutation safety

The editor lets a tester do things that would normally be illegal
(teleport a player, change turns mid-round). Our first draft mutated
model fields directly from the dialog. That created an undocumented
path into the model that could bypass the log.

**Resolution:** every editor action funnels through a dedicated
`editorSet*` method on `GameModel`. These are the only editor-mode
mutators, they all log a `[Editor]` prefix, and they all call
`fireChanged()`. The dialog never touches fields directly.

---

## Improvements we would make with more time

### Gameplay

- **Bankruptcy to a creditor**: currently a bankrupt player's lands
  reset to unowned, per the spec. A richer rule would transfer them
  to the creditor, which makes late-game snowballing more realistic.
- **Auction on decline**: when a player lands on an unowned land and
  declines to buy, the spec currently just ends the turn. Real
  Monopoly auctions it to everyone else; that would be a nice
  optional flag.
- **Save / load**: persist the game state to JSON so a half-played
  match can resume later.
- **Undo**: a circular buffer of `GameModel` snapshots would give us
  an undo button for teaching demos.

### UI / UX

- **Dice animation**: currently we just print the roll. A quick
  300ms animated dice face would sell the interaction better at the
  demo.
- **Pathfinding highlight**: animate the token hopping across every
  slot it passes (including the GO crossing).
- **Responsive layout**: the board panel uses fixed preferred sizes;
  resizing the window leaves awkward gaps. Switching to
  `GridBagLayout` with weights would fix this.
- **Accessibility**: the token colours are not colour-blind safe.
  Adding small labels (P1, P2, …) on the tokens — we did this for
  the numbers, but the status cards could do more.

### Code quality

- **Unit tests**: `Board.computeRent` and
  `Board.countConsecutiveOwnership` are pure and easily testable.
  A JUnit test suite would lock in the consecutive-bonus rules.
- **Replace integer `side` with an enum**: currently we use
  `int side` with constants on `Slot`. A `Side` enum would give
  stronger type checking and nicer `toString()` logs.
- **Event payloads**: our listener callback passes the whole
  `GameModel`. A more fine-grained event (e.g. `PlayerMoved`,
  `LandBought`) would let future listeners be selective.
- **I18N**: string literals are hard-coded in English. Extracting
  them to a `ResourceBundle` would let the game switch to Chinese
  for the local user base.

### Architecture

- **Network play**: the observer-based model could be broadcast over
  a socket, letting four real computers share a game.
- **Pluggable AI opponents**: replace a `Player` with a bot that
  makes buy / trade decisions via a strategy interface. Handy for
  testing and for solo play.
- **Hex or variable-size boards**: because the board is data-driven,
  this would mostly be a `DataLoader` + `BoardPanel` change — the
  core rules would stay untouched.
