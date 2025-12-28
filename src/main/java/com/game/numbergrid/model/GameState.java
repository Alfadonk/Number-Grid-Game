package com.game.numbergrid.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class GameState {
    private String gameId;
    private Block[][] grid;
    private int score;
    private List<String> messages;
    private boolean gameOver;

    public GameState() {
        this.grid = new Block[10][20];
        this.score = 0;
        this.messages = new ArrayList<>();
        this.gameOver = false;
        initializeGrid();
    }

    private void initializeGrid() {
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 20; col++) {
                grid[row][col] = new Block(row, col);
                grid[row][col].setValue((int) (Math.random() * 9) + 1);
            }
        }
    }

    // Getters and Setters
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    
    @JsonIgnore
    public Block[][] getGrid() { return grid; }
    public void setGrid(Block[][] grid) { this.grid = grid; }
    
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    
    public List<String> getMessages() { return messages; }
    public void setMessages(List<String> messages) { this.messages = messages; }
    
    public boolean isGameOver() { return gameOver; }
    public void setGameOver(boolean gameOver) { this.gameOver = gameOver; }

    public void addMessage(String message) {
        this.messages.add(message);
    }

    public void clearMessages() {
        this.messages.clear();
    }
    
    // Helper method to get grid as JSON string
    public String getGridJson() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(grid);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
    
    // Helper method to get grid as simple 2D array
    public int[][] getGridArray() {
        int[][] gridArray = new int[10][20];
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 20; col++) {
                gridArray[row][col] = grid[row][col].getValue();
            }
        }
        return gridArray;
    }
}
