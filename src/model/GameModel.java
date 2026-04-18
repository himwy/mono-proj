package model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class GameModel {

    public interface Listener {
        void onGameStateChanged(GameModel model);
        void onLog(String message);
    }

    public enum Phase { AWAITING_ROLL, AWAITING_BUY_DECISION, AWAITING_END_TURN, GAME_OVER }

    private final Board board;
    private final List<Player> players;
    private final Dice dice;
    private final int startingBalance;
    private final int goBonus;
    private final List<Listener> listeners = new ArrayList<>();

    private int currentPlayerIndex;
    private Phase phase;
    private Player winner;

    public GameModel(Board board, int startingBalance, int goBonus) {
        this.board = board;
        this.startingBalance = startingBalance;
        this.goBonus = goBonus;
        this.dice = new Dice();
        this.players = new ArrayList<>();
        this.currentPlayerIndex = 0;
        this.phase = Phase.AWAITING_ROLL;
        this.winner = null;

        players.add(new Player(1, "Player 1", new Color(220, 60, 60),  startingBalance));
        players.add(new Player(2, "Player 2", new Color(60, 140, 220), startingBalance));
        players.add(new Player(3, "Player 3", new Color(60, 180, 80),  startingBalance));
        players.add(new Player(4, "Player 4", new Color(230, 180, 40), startingBalance));
    }

    public Board getBoard() { return board; }
    public List<Player> getPlayers() { return players; }
    public Dice getDice() { return dice; }
    public Phase getPhase() { return phase; }
    public Player getWinner() { return winner; }
    public int getStartingBalance() { return startingBalance; }
    public int getGoBonus() { return goBonus; }

    public Player getCurrentPlayer() { return players.get(currentPlayerIndex); }
    public int getCurrentPlayerIndex() { return currentPlayerIndex; }

    public void addListener(Listener l) { listeners.add(l); }

    private void fireChanged() {
        for (Listener l : listeners) l.onGameStateChanged(this);
    }
    private void log(String message) {
        for (Listener l : listeners) l.onLog(message);
    }

    public int rollDice() {
        if (phase != Phase.AWAITING_ROLL) return 0;
        Player p = getCurrentPlayer();
        int roll = dice.roll();
        log(p.getName() + " rolled a " + roll + ".");

        boolean passedGo = p.advance(roll, board.size());
        if (passedGo) {
            p.credit(goBonus);
            log(p.getName() + " passed GO and collected $" + goBonus + ".");
        }

        Slot landed = board.getSlot(p.getPosition());
        log(p.getName() + " landed on " + landed.getName() + " (slot " + landed.getNumber() + ").");

        resolveLanding(p, landed);
        fireChanged();
        return roll;
    }

    private void resolveLanding(Player p, Slot landed) {
        if (!landed.isLand()) {
            phase = Phase.AWAITING_END_TURN;
            return;
        }
        if (!landed.isOwned()) {
            if (p.getBalance() >= landed.getPrice()) {
                phase = Phase.AWAITING_BUY_DECISION;
                log(p.getName() + " may buy " + landed.getName()
                    + " for $" + landed.getPrice() + ".");
            } else {
                phase = Phase.AWAITING_END_TURN;
                log(p.getName() + " cannot afford " + landed.getName() + ".");
            }
            return;
        }
        if (landed.getOwner() == p) {
            phase = Phase.AWAITING_END_TURN;
            log(p.getName() + " landed on own land.");
            return;
        }
        
        int rent = board.computeRent(landed);
        int consecutive = board.countConsecutiveOwnership(landed);
        String multiplierNote = consecutive >= 3 ? " (3x consecutive bonus)"
                              : consecutive == 2 ? " (2x consecutive bonus)" : "";
        log(p.getName() + " owes $" + rent + " rent to "
            + landed.getOwner().getName() + multiplierNote + ".");

        if (p.getBalance() >= rent) {
            p.debit(rent);
            landed.getOwner().credit(rent);
            phase = Phase.AWAITING_END_TURN;
        } else {
            log(p.getName() + " cannot pay rent and goes bankrupt!");
            landed.getOwner().credit(p.getBalance());
            p.setBalance(0);
            p.declareBankruptcy();
            checkForWinner();
        }
    }

    public void buyCurrentLand() {
        if (phase != Phase.AWAITING_BUY_DECISION) return;
        Player p = getCurrentPlayer();
        Slot land = board.getSlot(p.getPosition());
        if (!land.isLand() || land.isOwned()) return;
        if (!p.debit(land.getPrice())) return;
        land.setOwner(p);
        p.addLand(land);
        log(p.getName() + " bought " + land.getName() + " for $" + land.getPrice() + ".");
        phase = Phase.AWAITING_END_TURN;
        fireChanged();
    }

    public void declineBuy() {
        if (phase != Phase.AWAITING_BUY_DECISION) return;
        Player p = getCurrentPlayer();
        log(p.getName() + " declined to buy.");
        phase = Phase.AWAITING_END_TURN;
        fireChanged();
    }

    public void endTurn() {
        if (phase == Phase.GAME_OVER) return;
        if (phase == Phase.AWAITING_BUY_DECISION) {
            
            declineBuy();
        }
        advanceToNextActivePlayer();
        phase = (phase == Phase.GAME_OVER) ? Phase.GAME_OVER : Phase.AWAITING_ROLL;
        fireChanged();
    }

    private void advanceToNextActivePlayer() {
        if (phase == Phase.GAME_OVER) return;
        int tries = 0;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            tries++;
            if (tries > players.size()) break;
        } while (!getCurrentPlayer().isActive());
        log("It is now " + getCurrentPlayer().getName() + "'s turn.");
    }

    private void checkForWinner() {
        int activeCount = 0;
        Player lastActive = null;
        for (Player p : players) {
            if (p.isActive()) { activeCount++; lastActive = p; }
        }
        if (activeCount <= 1) {
            winner = lastActive;
            phase = Phase.GAME_OVER;
            log("GAME OVER! Winner: "
                + (winner != null ? winner.getName() : "nobody"));
        }
    }

    public boolean trade(Player buyer, Player seller, Slot land, int agreedPrice) {
        if (phase != Phase.AWAITING_ROLL) {
            log("Trade rejected: trading is only allowed before rolling.");
            return false;
        }
        if (buyer == null || seller == null || land == null) return false;
        if (!buyer.isActive() || !seller.isActive()) return false;
        if (!land.isLand() || land.getOwner() != seller) {
            log("Trade rejected: seller does not own that land.");
            return false;
        }
        if (buyer.getBalance() < agreedPrice) {
            log("Trade rejected: buyer cannot afford the agreed price.");
            return false;
        }
        if (agreedPrice < 0) return false;

        buyer.debit(agreedPrice);
        seller.credit(agreedPrice);
        seller.removeLand(land);
        buyer.addLand(land);
        land.setOwner(buyer);
        log("TRADE: " + buyer.getName() + " bought " + land.getName()
            + " from " + seller.getName() + " for $" + agreedPrice + ".");
        fireChanged();
        return true;
    }

    public void editorSetBalance(Player p, int newBalance) {
        p.setBalance(Math.max(0, newBalance));
        log("[Editor] Set " + p.getName() + " balance to $" + p.getBalance() + ".");
        fireChanged();
    }

    public void editorSetPosition(Player p, int newPos) {
        if (newPos < 0 || newPos >= board.size()) return;
        p.setPosition(newPos);
        log("[Editor] Moved " + p.getName() + " to slot " + newPos + ".");
        fireChanged();
    }

    public void editorSetStatus(Player p, Player.Status status) {
        if (status == Player.Status.BANKRUPT && p.isActive()) {
            p.declareBankruptcy();
        } else {
            p.setStatus(status);
        }
        log("[Editor] Set " + p.getName() + " status to " + status + ".");
        checkForWinner();
        fireChanged();
    }

    public void editorSetOwner(Slot land, Player newOwner) {
        if (!land.isLand()) return;
        Player prev = land.getOwner();
        if (prev != null) prev.removeLand(land);
        land.setOwner(newOwner);
        if (newOwner != null) newOwner.addLand(land);
        log("[Editor] " + land.getName() + " ownership set to "
            + (newOwner != null ? newOwner.getName() : "None") + ".");
        fireChanged();
    }

    public void editorSetCurrentPlayer(int index) {
        if (index < 0 || index >= players.size()) return;
        currentPlayerIndex = index;
        phase = Phase.AWAITING_ROLL;
        log("[Editor] Current turn forced to " + getCurrentPlayer().getName() + ".");
        fireChanged();
    }
}
