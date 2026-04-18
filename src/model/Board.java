package model;

import java.util.ArrayList;
import java.util.List;

public class Board {

    private final List<Slot> slots;

    public Board(List<Slot> slots) {
        this.slots = new ArrayList<>(slots);
    }

    public int size() { return slots.size(); }
    public List<Slot> getSlots() { return slots; }
    public Slot getSlot(int index) { return slots.get(index); }

    public int countConsecutiveOwnership(Slot land) {
        if (!land.isLand() || !land.isOwned()) return 0;
        Player owner = land.getOwner();
        int side = land.getSide();
        int idx = slots.indexOf(land);
        int count = 1;

        for (int i = idx - 1; i >= 0; i--) {
            Slot s = slots.get(i);
            if (s.getSide() != side) break;
            if (!s.isLand() || s.getOwner() != owner) break;
            count++;
        }
        for (int i = idx + 1; i < slots.size(); i++) {
            Slot s = slots.get(i);
            if (s.getSide() != side) break;
            if (!s.isLand() || s.getOwner() != owner) break;
            count++;
        }
        return count;
    }

    public int computeRent(Slot land) {
        if (!land.isLand() || !land.isOwned()) return 0;
        int base = land.getBaseRent();
        int consecutive = countConsecutiveOwnership(land);
        int multiplier = 1;
        if (consecutive == 2) multiplier = 2;
        else if (consecutive >= 3) multiplier = 3;
        return base * multiplier;
    }
}
