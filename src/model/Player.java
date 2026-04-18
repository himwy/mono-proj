package model;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Player {

    public enum Status { ACTIVE, BANKRUPT }

    private final int id;
    private final String name;
    private final Color color;

    private int position;
    private int balance;
    private Status status;
    private final List<Slot> ownedLands;

    public Player(int id, String name, Color color, int startingBalance) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.position = 0;
        this.balance = startingBalance;
        this.status = Status.ACTIVE;
        this.ownedLands = new ArrayList<>();
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public Color getColor() { return color; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public int getBalance() { return balance; }
    public void setBalance(int balance) { this.balance = Math.max(0, balance); }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public boolean isActive() { return status == Status.ACTIVE; }

    public List<Slot> getOwnedLands() { return ownedLands; }

    public boolean advance(int steps, int boardSize) {
        int oldPos = position;
        position = (position + steps) % boardSize;
        return position < oldPos || steps >= boardSize;
    }

    public void credit(int amount) {
        if (amount < 0) return;
        balance += amount;
    }

    public boolean debit(int amount) {
        if (amount < 0) return true;
        if (balance < amount) return false;
        balance -= amount;
        return true;
    }

    public void addLand(Slot land) {
        if (!ownedLands.contains(land)) ownedLands.add(land);
    }

    public void removeLand(Slot land) {
        ownedLands.remove(land);
    }

    public void declareBankruptcy() {
        for (Slot s : ownedLands) s.setOwner(null);
        ownedLands.clear();
        status = Status.BANKRUPT;
    }

    @Override
    public String toString() {
        return name + " ($" + balance + ", pos=" + position + ", " + status + ")";
    }
}
