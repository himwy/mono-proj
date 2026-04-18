package model;

public class Slot {

    public static final int SIDE_NONE = 0;
    public static final int SIDE_SOUTH = 1;
    public static final int SIDE_WEST = 2;
    public static final int SIDE_NORTH = 3;
    public static final int SIDE_EAST = 4;

    private final int number;
    private final String name;
    private final int price;
    private final boolean land;
    private final int side;

    private Player owner;

    public Slot(int number, String name, int price, boolean land, int side) {
        this.number = number;
        this.name = name;
        this.price = price;
        this.land = land;
        this.side = side;
        this.owner = null;
    }

    public int getNumber() { return number; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public boolean isLand() { return land; }
    public int getSide() { return side; }

    public Player getOwner() { return owner; }
    public void setOwner(Player owner) { this.owner = owner; }
    public boolean isOwned() { return owner != null; }

    public int getBaseRent() {
        if (!land) return 0;
        return price / 10;
    }

    @Override
    public String toString() {
        return "[" + number + "] " + name;
    }
}
