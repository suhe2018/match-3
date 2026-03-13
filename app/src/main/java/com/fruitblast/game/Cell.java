package com.fruitblast.game;

public class Cell {
    /** 0 = empty, 1–8 = fruit type index */
    public int fruitIndex;
    /** Ice block in HARD mode — cannot be swapped */
    public boolean isLocked;

    public Cell(int fruitIndex, boolean isLocked) {
        this.fruitIndex = fruitIndex;
        this.isLocked = isLocked;
    }

    public boolean isEmpty() {
        return fruitIndex == 0;
    }
}
