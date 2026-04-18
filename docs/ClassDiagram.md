# Class Diagram

Text-based UML. Feed this to any tool that accepts PlantUML / Mermaid
if a rendered version is required for submission.

## Overview

```
                       ┌───────────────┐
                       │    Main       │
                       └──────┬────────┘
                              │ creates
                              ▼
 ┌────────────┐       ┌─────────────────┐        ┌─────────────┐
 │ GameView   │◀──────│ GameController  │───────▶│ GameModel   │
 │ (Swing UI) │       │ (listeners +    │        │ (rules +    │
 └─────┬──────┘       │  dialogs)       │        │  state)     │
       │ owns         └─────────────────┘        └──────┬──────┘
       ▼                                                 │ owns
 ┌──────────────┐                                        ▼
 │ BoardPanel   │                                 ┌───────────────┐
 │ StatusPanel  │◀──────── reads model ──────────│     Board     │
 │ ControlPanel │                                │  (List<Slot>) │
 │ LogPanel     │                                └──────┬────────┘
 │ GameEditor-  │                                       │ 1..*
 │   Dialog     │                                       ▼
 └──────────────┘                                ┌───────────────┐
                                                 │     Slot      │
                                                 └──────┬────────┘
                                                        │ 0..1 owner
                                                        ▼
                                                 ┌───────────────┐
                                                 │    Player     │
                                                 └───────────────┘
                                                        ▲
                                                        │ 1..*
                                                 ┌───────────────┐
                                                 │   GameModel   │
                                                 └───────────────┘
```

## Per-class details (simplified — getters/setters/constructors
omitted as allowed by the outline)

### Model

```
class Slot
  - number        : int
  - name          : String
  - price         : int
  - land          : boolean
  - side          : int
  - owner         : Player
  + getBaseRent() : int
  + isOwned()     : boolean

class Player «enum Status { ACTIVE, BANKRUPT }»
  - id            : int
  - name          : String
  - color         : Color
  - position      : int
  - balance       : int
  - status        : Status
  - ownedLands    : List<Slot>
  + advance(steps : int, boardSize : int) : boolean
  + credit(amount : int)
  + debit(amount : int) : boolean
  + addLand(s : Slot)
  + removeLand(s : Slot)
  + declareBankruptcy()

class Dice
  - random        : Random
  - lastRoll      : int
  + roll()        : int

class Board
  - slots         : List<Slot>
  + size()        : int
  + getSlot(i : int)   : Slot
  + countConsecutiveOwnership(land : Slot) : int
  + computeRent(land : Slot) : int

class DataLoader  «static utility»
  + loadSlots(path : String) : List<Slot>

class GameModel «enum Phase { AWAITING_ROLL, AWAITING_BUY_DECISION,
                               AWAITING_END_TURN, GAME_OVER }»
  - board              : Board
  - players            : List<Player>
  - dice               : Dice
  - currentPlayerIndex : int
  - phase              : Phase
  - winner             : Player
  - listeners          : List<Listener>
  + rollDice()           : int
  + buyCurrentLand()
  + declineBuy()
  + endTurn()
  + trade(buyer, seller, land, price) : boolean
  + editorSetBalance(p, amount)
  + editorSetPosition(p, pos)
  + editorSetStatus(p, status)
  + editorSetOwner(land, owner)
  + editorSetCurrentPlayer(index)
  + addListener(l : Listener)

interface GameModel.Listener
  + onGameStateChanged(m : GameModel)
  + onLog(message : String)
```

### View

```
class GameView  extends JFrame
  + boardPanel   : BoardPanel
  + statusPanel  : StatusPanel
  + controlPanel : ControlPanel
  + logPanel     : LogPanel

class BoardPanel extends JPanel
  - model  : GameModel
  - cells  : List<SlotCell>
  + refresh()

class BoardPanel.SlotCell extends JPanel
  - slot   : Slot
  - model  : GameModel

class StatusPanel extends JPanel
  - model      : GameModel
  - cards      : PlayerCard[]
  - turnLabel  : JLabel
  + refresh()

class StatusPanel.PlayerCard extends JPanel
  - player : Player

class ControlPanel extends JPanel
  + rollButton    : JButton
  + buyButton     : JButton
  + declineButton : JButton
  + endTurnButton : JButton
  + tradeButton   : JButton
  + landsButton   : JButton
  + editorButton  : JButton
  + updateForPhase(phase : String, gameOver : boolean)

class LogPanel extends JPanel
  - area    : JTextArea
  + append(msg : String)

class GameEditorDialog extends JDialog
  - model : GameModel
  (three tabs: Player, Land, Turn — each calls GameModel.editorSet*)
```

### Controller

```
class GameController  implements GameModel.Listener
  - model  : GameModel
  - view   : GameView
  - gameOverDialogShown : boolean
  + onGameStateChanged(m : GameModel)
  + onLog(message : String)
  - wireButtons()
  - wireEditorShortcut()
  - openTradeDialog()
  - showLandOwnership()
  - openEditor()
  - refreshView()
```

## Key associations

| Relationship | Cardinality | Where declared |
|---|---|---|
| `GameModel` → `Board` | 1-to-1 | `GameModel.board` |
| `GameModel` → `Player` | 1-to-many (4) | `GameModel.players` |
| `GameModel` → `Dice` | 1-to-1 | `GameModel.dice` |
| `Board` → `Slot` | 1-to-many (24) | `Board.slots` |
| `Slot` → `Player` | 0-or-1 (owner) | `Slot.owner` |
| `Player` → `Slot` | 0-to-many (owned lands) | `Player.ownedLands` |
| `GameView` → sub-panels | 1-to-1 each | `GameView` fields |
| `GameController` → `GameModel` | 1-to-1 | `GameController.model` |
| `GameController` → `GameView` | 1-to-1 | `GameController.view` |
| `GameModel` → `Listener` | 1-to-many | observer pattern |
