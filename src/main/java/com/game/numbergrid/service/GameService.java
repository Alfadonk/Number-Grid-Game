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

    // New: Handle multiple block selection (drag combo)
    public GameState selectBlocks(GameState game, List<int[]> selectedPositions) {
        game.clearMessages();
        
        if (game.isGameOver()) {
            game.addMessage("Game over! Start a new game.");
            return game;
        }

        Block[][] grid = game.getGrid();
        
        // Validate all positions
        for (int[] pos : selectedPositions) {
            int row = pos[0];
            int col = pos[1];
            if (row < 0 || row >= 10 || col < 0 || col >= 20) {
                game.addMessage("Invalid position at (" + row + "," + col + ")!");
                return game;
            }
            if (grid[row][col].isEmpty()) {
                game.addMessage("Cannot select empty block at (" + row + "," + col + ")!");
                return game;
            }
        }

        // If only one block selected, just select it
        if (selectedPositions.size() == 1) {
            int[] pos = selectedPositions.get(0);
            Block block = grid[pos[0]][pos[1]];
            block.setSelected(!block.isSelected()); // Toggle selection
            if (block.isSelected()) {
                game.addMessage("Selected: " + block.getValue());
            } else {
                game.addMessage("Deselected block");
            }
            return game;
        }

        // Calculate sum of all selected blocks
        int totalSum = 0;
        List<Block> selectedBlocks = new ArrayList<>();
        for (int[] pos : selectedPositions) {
            Block block = grid[pos[0]][pos[1]];
            totalSum += block.getValue();
            selectedBlocks.add(block);
        }

        // Check if sum is a multiple of 10
        if (totalSum % 10 == 0) {
            // Valid combo! Clear all selected blocks
            for (Block block : selectedBlocks) {
                block.setValue(0);
                block.setSelected(false);
            }
            
            // Calculate score: 10 points per block × combo multiplier
            int baseScore = 10 * selectedBlocks.size();
            int comboMultiplier = selectedBlocks.size(); // More blocks = higher multiplier
            int scoreEarned = baseScore * comboMultiplier;
            
            game.setScore(game.getScore() + scoreEarned);
            game.addMessage("Perfect combo! " + selectedBlocks.size() + 
                " blocks sum to " + totalSum + ". +" + scoreEarned + " points!");
            
            // Clear all hints
            clearHints(grid);
            
            // Check if game is over
            if (isGameComplete(grid)) {
                game.setGameOver(true);
                game.addMessage("Game Over! Final Score: " + game.getScore());
            }
        } else {
            // Invalid combo
            game.addMessage("Sum is " + totalSum + ", not a multiple of 10! Try again.");
            // Deselect all blocks
            for (Block block : selectedBlocks) {
                block.setSelected(false);
            }
        }
        
        return game;
    }

    // Original single block selection (kept for backward compatibility)
    public GameState selectBlock(GameState game, int row, int col) {
        List<int[]> positions = new ArrayList<>();
        positions.add(new int[]{row, col});
        return selectBlocks(game, positions);
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

    private void clearHints(Block[][] grid) {
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 20; col++) {
                grid[row][col].setHinted(false);
            }
        }
    }

    private boolean isGameComplete(Block[][] grid) {
        // Check if there are any combos that sum to a multiple of 10
        List<Block> availableBlocks = new ArrayList<>();
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 20; col++) {
                if (!grid[row][col].isEmpty()) {
                    availableBlocks.add(grid[row][col]);
                }
            }
        }
        
        // Check all possible combos (up to 4 blocks for performance)
        return !hasValidCombo(availableBlocks, 0, new ArrayList<>(), 0, 4);
    }

    // Recursive method to check for valid combos
    private boolean hasValidCombo(List<Block> blocks, int startIndex, 
                                 List<Block> currentCombo, int currentSum, int maxComboSize) {
        // If we have at least 2 blocks and sum is multiple of 10, return true
        if (currentCombo.size() >= 2 && currentSum % 10 == 0) {
            return true;
        }
        
        // If combo size limit reached, stop
        if (currentCombo.size() >= maxComboSize) {
            return false;
        }
        
        // Try adding more blocks
        for (int i = startIndex; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            currentCombo.add(block);
            int newSum = currentSum + block.getValue();
            
            if (hasValidCombo(blocks, i + 1, currentCombo, newSum, maxComboSize)) {
                return true;
            }
            
            currentCombo.remove(currentCombo.size() - 1);
        }
        
        return false;
    }

    public GameState getHint(GameState game) {
        game.clearMessages();
        Block[][] grid = game.getGrid();
        clearHints(grid);
        
        List<Block> availableBlocks = new ArrayList<>();
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 20; col++) {
                if (!grid[row][col].isEmpty()) {
                    availableBlocks.add(grid[row][col]);
                }
            }
        }
        
        // Find first valid combo for hint (up to 3 blocks for simplicity)
        List<Block> hintCombo = findHintCombo(availableBlocks, 0, new ArrayList<>(), 0, 3);
        
        if (hintCombo != null && hintCombo.size() >= 2) {
            // Highlight the blocks in the hint combo
            for (Block block : hintCombo) {
                block.setHinted(true);
            }
            
            // Calculate the sum
            int totalSum = hintCombo.stream().mapToInt(Block::getValue).sum();
            game.addMessage("Hint: Try connecting " + hintCombo.size() + 
                " blocks that sum to " + totalSum + " (multiple of 10)");
        } else {
            game.addMessage("No more valid moves!");
        }
        
        return game;
    }

    private List<Block> findHintCombo(List<Block> blocks, int startIndex,
                                     List<Block> currentCombo, int currentSum, int maxComboSize) {
        // Return if we found a valid combo of at least 2 blocks
        if (currentCombo.size() >= 2 && currentSum % 10 == 0) {
            return new ArrayList<>(currentCombo);
        }
        
        if (currentCombo.size() >= maxComboSize) {
            return null;
        }
        
        for (int i = startIndex; i < blocks.size(); i++) {
            Block block = blocks.get(i);
            currentCombo.add(block);
            int newSum = currentSum + block.getValue();
            
            List<Block> result = findHintCombo(blocks, i + 1, currentCombo, newSum, maxComboSize);
            if (result != null) {
                return result;
            }
            
            currentCombo.remove(currentCombo.size() - 1);
        }
        
        return null;
    }

    // Method to clear hint after it's been viewed
    public GameState clearHint(GameState game) {
        Block[][] grid = game.getGrid();
        clearHints(grid);
        return game;
    }
}