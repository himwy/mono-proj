package model;

import java.util.Random;

public class Dice {

    private static final int MIN = 1;
    private static final int MAX = 10;

    private final Random random;
    private int lastRoll;

    public Dice() {
        this(new Random());
    }

    public Dice(Random random) {
        this.random = random;
        this.lastRoll = 0;
    }

    public int roll() {
        lastRoll = MIN + random.nextInt(MAX - MIN + 1);
        return lastRoll;
    }

    public int getLastRoll() { return lastRoll; }
}
