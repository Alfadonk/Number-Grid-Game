package com.game.numbergrid.model;

public class Block {
    private int value; // 0 = empty, 1-9 = number
    private int row;
    private int col;
    private boolean selected;

    public Block(int row, int col) {
        this.row = row;
        this.col = col;
        this.value = 0;
        this.selected = false;
    }

    // Getters and Setters
    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }
    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }
    public int getCol() { return col; }
    public void setCol(int col) { this.col = col; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
    public boolean isEmpty() { return value == 0; }
}
