package com.game.numbergrid.service;

import com.game.numbergrid.model.Block;
import com.game.numbergrid.model.GameState;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GameService {
    
    public GameState createNewGame() {
        GameState game = new GameState();
        game.setGameId(java.util.UUID.randomUUID().toString());
        return game;
    }

    public GameState selectBlock(GameState game, int row, int col) {
        game.clearMessages();
        
        if (game.isGameOver()) {
            game.addMessage("Game over! Start a new game.");
            return game;
        }

        Block[][] grid = game.getGrid();
        
        if (row < 0 || row >= 10 || col < 0 || col >= 20) {
            game.addMessage("Invalid position!");
            return game;
        }

        Block block = grid[row][col];
        
        if (block.isEmpty()) {
            game.addMessage("Cannot select empty block!");
            return game;
        }

        // Find all currently selected blocks
        List<Block> selectedBlocks = getSelectedBlocks(grid);
        
        if (selectedBlocks.isEmpty()) {
            // First selection
            block.setSelected(true);
            game.addMessage("Selected: " + block.getValue());
        } else if (selectedBlocks.size() == 1) {
            // Second selection - check if sum equals 10
            Block first = selectedBlocks.get(0);
            
            if (first.getRow() == row && first.getCol() == col) {
                // Deselect if clicking same block
                block.setSelected(false);
                game.addMessage("Deselected block");
                return game;
            }

            if (first.getValue() + block.getValue() == 10) {
                // Valid pair found
                first.setValue(0);
                block.setValue(0);
                first.setSelected(false);
                game.setScore(game.getScore() + 10);
                game.addMessage("Perfect match! Cleared blocks with values " + 
                    first.getValue() + " and " + block.getValue());
                
                // Check if game is over
                if (isGameComplete(grid)) {
                    game.setGameOver(true);
                    game.addMessage("Game Over! Final Score: " + game.getScore());
                }
            } else {
                // Invalid pair
                game.addMessage("Sum is " + (first.getValue() + block.getValue()) + 
                    ", not 10! Try again.");
                first.setSelected(false);
            }
        } else {
            // Should not happen, but clear all selections
            clearSelections(grid);
            block.setSelected(true);
        }
        
        return game;
    }

    private List<Block> getSelectedBlocks(Block[][] grid) {
        List<Block> selected = new ArrayList<>();
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 20; col++) {
                if (grid[row][col].isSelected()) {
                    selected.add(grid[row][col]);
                }
            }
        }
        return selected;
    }

    private void clearSelections(Block[][] grid) {
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 20; col++) {
                grid[row][col].setSelected(false);
            }
        }
    }

    private boolean isGameComplete(Block[][] grid) {
        // Check if there are any pairs that sum to 10
        for (int row1 = 0; row1 < 10; row1++) {
            for (int col1 = 0; col1 < 20; col1++) {
                if (!grid[row1][col1].isEmpty()) {
                    for (int row2 = 0; row2 < 10; row2++) {
                        for (int col2 = 0; col2 < 20; col2++) {
                            if (!grid[row2][col2].isEmpty() && 
                                !(row1 == row2 && col1 == col2)) {
                                if (grid[row1][col1].getValue() + 
                                    grid[row2][col2].getValue() == 10) {
                                    return false; // At least one valid pair exists
                                }
                            }
                        }
                    }
                }
            }
        }
        return true; // No valid pairs found
    }

    public GameState getHint(GameState game) {
        game.clearMessages();
        Block[][] grid = game.getGrid();
        
        // Find first available pair that sums to 10
        for (int row1 = 0; row1 < 10; row1++) {
            for (int col1 = 0; col1 < 20; col1++) {
                if (!grid[row1][col1].isEmpty()) {
                    for (int row2 = 0; row2 < 10; row2++) {
                        for (int col2 = 0; col2 < 20; col2++) {
                            if (!grid[row2][col2].isEmpty() && 
                                !(row1 == row2 && col1 == col2)) {
                                if (grid[row1][col1].getValue() + 
                                    grid[row2][col2].getValue() == 10) {
                                    game.addMessage("Hint: Try connecting (" + 
                                        row1 + "," + col1 + ")[" + grid[row1][col1].getValue() + 
                                        "] with (" + row2 + "," + col2 + ")[" + 
                                        grid[row2][col2].getValue() + "]");
                                    return game;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        game.addMessage("No more valid moves!");
        return game;
    }
}
