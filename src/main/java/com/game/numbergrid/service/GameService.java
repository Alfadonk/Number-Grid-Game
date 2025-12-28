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
                block.setHinted(false);
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
        // Check if there are any connected combos that sum to a multiple of 10
        return !hasConnectedCombo(grid);
    }

    // Check if there are any connected combos available
    private boolean hasConnectedCombo(Block[][] grid) {
        boolean[][] visited = new boolean[10][20];
        
        // Check from each block
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 20; col++) {
                if (!grid[row][col].isEmpty() && !visited[row][col]) {
                    if (hasConnectedComboFrom(grid, row, col, visited, 
                                            new ArrayList<>(), 0, 4)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private boolean hasConnectedComboFrom(Block[][] grid, int row, int col,
                                         boolean[][] visited, List<Block> currentCombo,
                                         int currentSum, int maxSize) {
        visited[row][col] = true;
        Block currentBlock = grid[row][col];
        currentCombo.add(currentBlock);
        int newSum = currentSum + currentBlock.getValue();
        
        // Check if current combo is valid
        if (currentCombo.size() >= 2 && newSum % 10 == 0) {
            return true;
        }
        
        // If we haven't reached max size, explore adjacent blocks
        if (currentCombo.size() < maxSize) {
            // Check adjacent blocks in all 8 directions
            int[][] directions = {
                {-1, -1}, {-1, 0}, {-1, 1},
                {0, -1},           {0, 1},
                {1, -1},  {1, 0},  {1, 1}
            };
            
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                
                if (newRow >= 0 && newRow < 10 && newCol >= 0 && newCol < 20 &&
                    !grid[newRow][newCol].isEmpty() && !visited[newRow][newCol]) {
                    
                    if (hasConnectedComboFrom(grid, newRow, newCol, visited,
                                            currentCombo, newSum, maxSize)) {
                        return true;
                    }
                }
            }
        }
        
        // Backtrack
        currentCombo.remove(currentCombo.size() - 1);
        visited[row][col] = false;
        
        return false;
    }

    public GameState getHint(GameState game) {
        game.clearMessages();
        Block[][] grid = game.getGrid();
        clearHints(grid);
        
        // Find connected combos (blocks that can be selected together)
        List<List<Block>> connectedCombos = findConnectedCombos(grid);
        
        if (!connectedCombos.isEmpty()) {
            // Get the best combo (largest or simplest)
            List<Block> hintCombo = selectBestHintCombo(connectedCombos);
            
            // Highlight the blocks in the hint combo
            for (Block block : hintCombo) {
                block.setHinted(true);
            }
            
            // Calculate the sum and score
            int totalSum = hintCombo.stream().mapToInt(Block::getValue).sum();
            int comboSize = hintCombo.size();
            int potentialScore = 10 * comboSize * comboSize;
            
            // Give helpful message about the hint
            String message;
            if (comboSize == 2) {
                message = "Hint: Two connected numbers (" + hintCombo.get(0).getValue() + 
                         " + " + hintCombo.get(1).getValue() + " = " + totalSum + 
                         ") - Score: +" + potentialScore;
            } else {
                message = "Hint: " + comboSize + " connected blocks sum to " + totalSum + 
                         " - Score: +" + potentialScore;
            }
            game.addMessage(message);
        } else {
            game.addMessage("No more valid moves!");
        }
        
        return game;
    }
    
    // Find all connected combos that sum to a multiple of 10
    private List<List<Block>> findConnectedCombos(Block[][] grid) {
        List<List<Block>> connectedCombos = new ArrayList<>();
        boolean[][] visited = new boolean[10][20];
        
        // Find all blocks and check for connected combos starting from each block
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 20; col++) {
                if (!grid[row][col].isEmpty()) {
                    // Reset visited for new starting point
                    for (int i = 0; i < 10; i++) {
                        for (int j = 0; j < 20; j++) {
                            visited[i][j] = false;
                        }
                    }
                    
                    // Try to find connected combos starting from this block
                    List<List<Block>> combosFromHere = findConnectedCombosFrom(
                        grid, row, col, visited, new ArrayList<>(), 0, 4);
                    connectedCombos.addAll(combosFromHere);
                }
            }
        }
        
        return connectedCombos;
    }
    
    // Find connected combos starting from a specific position
    private List<List<Block>> findConnectedCombosFrom(Block[][] grid, int row, int col, 
                                                     boolean[][] visited,
                                                     List<Block> currentCombo, 
                                                     int currentSum, int maxSize) {
        List<List<Block>> combos = new ArrayList<>();
        
        // Mark as visited for this search path
        visited[row][col] = true;
        Block currentBlock = grid[row][col];
        currentCombo.add(currentBlock);
        int newSum = currentSum + currentBlock.getValue();
        
        // Check if current combo is valid (at least 2 blocks, sum is multiple of 10)
        if (currentCombo.size() >= 2 && newSum % 10 == 0) {
            combos.add(new ArrayList<>(currentCombo));
        }
        
        // If we haven't reached max size, explore adjacent blocks
        if (currentCombo.size() < maxSize) {
            // Check adjacent blocks (up, down, left, right, and diagonals)
            int[][] directions = {
                {-1, -1}, {-1, 0}, {-1, 1},  // Top row
                {0, -1},           {0, 1},   // Middle (skip current)
                {1, -1},  {1, 0},  {1, 1}    // Bottom row
            };
            
            for (int[] dir : directions) {
                int newRow = row + dir[0];
                int newCol = col + dir[1];
                
                // Check bounds and if block is not empty
                if (newRow >= 0 && newRow < 10 && newCol >= 0 && newCol < 20 &&
                    !grid[newRow][newCol].isEmpty() && !visited[newRow][newCol]) {
                    
                    // Recursively search from adjacent block
                    combos.addAll(findConnectedCombosFrom(
                        grid, newRow, newCol, visited, currentCombo, newSum, maxSize));
                }
            }
        }
        
        // Backtrack
        currentCombo.remove(currentCombo.size() - 1);
        visited[row][col] = false;
        
        return combos;
    }
    
    // Select the best hint combo to show
    private List<Block> selectBestHintCombo(List<List<Block>> combos) {
        if (combos.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Prefer smaller combos for simplicity (easier to see and select)
        List<Block> bestCombo = combos.get(0);
        for (List<Block> combo : combos) {
            if (combo.size() < bestCombo.size()) {
                bestCombo = combo;
            } else if (combo.size() == bestCombo.size()) {
                // If same size, prefer combo with higher sum (more impressive)
                int comboSum = combo.stream().mapToInt(Block::getValue).sum();
                int bestSum = bestCombo.stream().mapToInt(Block::getValue).sum();
                if (comboSum > bestSum) {
                    bestCombo = combo;
                }
            }
        }
        
        return bestCombo;
    }

    // Method to clear hint after it's been viewed
    public GameState clearHint(GameState game) {
        Block[][] grid = game.getGrid();
        clearHints(grid);
        return game;
    }
}
