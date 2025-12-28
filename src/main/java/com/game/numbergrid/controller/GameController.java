package com.game.numbergrid.controller;

import com.game.numbergrid.model.Block;
import com.game.numbergrid.model.GameState;
import com.game.numbergrid.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/game")
public class GameController {
    
    @Autowired
    private GameService gameService;
    
    private Map<String, GameState> activeGames = new HashMap<>();

    @GetMapping("/start")
    public String startGame(Model model) {
        GameState game = gameService.createNewGame();
        activeGames.put(game.getGameId(), game);
        
        // Convert grid to simple 2D array for Thymeleaf
        int[][] gridArray = convertGridToArray(game.getGrid());
        boolean[][] hintArray = convertHintToArray(game.getGrid());
        
        model.addAttribute("gameId", game.getGameId());
        model.addAttribute("grid", gridArray);
        model.addAttribute("hints", hintArray);
        model.addAttribute("score", game.getScore());
        
        return "game";
    }

    @GetMapping("/api/state")
    @ResponseBody
    public Map<String, Object> getGameState(@RequestParam(required = false) String gameId) {
        Map<String, Object> response = new HashMap<>();
        
        if (gameId == null || gameId.isEmpty()) {
            // Create a new game if no gameId provided
            GameState newGame = gameService.createNewGame();
            activeGames.put(newGame.getGameId(), newGame);
            response.put("gameId", newGame.getGameId());
            response.put("grid", convertGridToArray(newGame.getGrid()));
            response.put("hints", convertHintToArray(newGame.getGrid()));
            response.put("score", newGame.getScore());
            response.put("messages", newGame.getMessages());
            response.put("gameOver", newGame.isGameOver());
        } else {
            GameState game = activeGames.get(gameId);
            if (game == null) {
                response.put("error", "Game not found");
                return response;
            }
            
            response.put("gameId", game.getGameId());
            response.put("grid", convertGridToArray(game.getGrid()));
            response.put("hints", convertHintToArray(game.getGrid()));
            response.put("score", game.getScore());
            response.put("messages", game.getMessages());
            response.put("gameOver", game.isGameOver());
        }
        
        return response;
    }

    @PostMapping("/select")
    @ResponseBody
    public Map<String, Object> selectBlock(
            @RequestParam String gameId,
            @RequestParam int row,
            @RequestParam int col) {
        
        Map<String, Object> response = new HashMap<>();
        
        GameState game = activeGames.get(gameId);
        if (game == null) {
            response.put("error", "Game not found");
            return response;
        }
        
        game = gameService.selectBlock(game, row, col);
        activeGames.put(gameId, game);
        
        response.put("grid", convertGridToArray(game.getGrid()));
        response.put("hints", convertHintToArray(game.getGrid()));
        response.put("score", game.getScore());
        response.put("messages", game.getMessages());
        response.put("gameOver", game.isGameOver());
        
        return response;
    }

    // SIMPLE WORKING ENDPOINT FOR MULTIPLE SELECTION
    @PostMapping("/select-multiple")
    @ResponseBody
    public Map<String, Object> selectMultipleBlocks(
            @RequestParam String gameId,
            @RequestParam String positions) { // Accept as comma-separated string
        
        Map<String, Object> response = new HashMap<>();
        
        GameState game = activeGames.get(gameId);
        if (game == null) {
            response.put("error", "Game not found");
            return response;
        }
        
        // Parse positions string like "1,2;3,4;5,6"
        List<int[]> positionsList = new ArrayList<>();
        if (positions != null && !positions.isEmpty()) {
            String[] pairs = positions.split(";");
            for (String pair : pairs) {
                String[] coords = pair.split(",");
                if (coords.length == 2) {
                    try {
                        int row = Integer.parseInt(coords[0].trim());
                        int col = Integer.parseInt(coords[1].trim());
                        positionsList.add(new int[]{row, col});
                    } catch (NumberFormatException e) {
                        response.put("error", "Invalid position format: " + pair);
                        return response;
                    }
                }
            }
        }
        
        if (positionsList.isEmpty()) {
            response.put("error", "No positions provided");
            return response;
        }
        
        game = gameService.selectBlocks(game, positionsList);
        activeGames.put(gameId, game);
        
        response.put("grid", convertGridToArray(game.getGrid()));
        response.put("hints", convertHintToArray(game.getGrid()));
        response.put("score", game.getScore());
        response.put("messages", game.getMessages());
        response.put("gameOver", game.isGameOver());
        
        return response;
    }

    @PostMapping("/hint")
    @ResponseBody
    public Map<String, Object> getHint(@RequestParam String gameId) {
        Map<String, Object> response = new HashMap<>();
        
        GameState game = activeGames.get(gameId);
        if (game == null) {
            response.put("error", "Game not found");
            return response;
        }
        
        game = gameService.getHint(game);
        
        response.put("grid", convertGridToArray(game.getGrid()));
        response.put("hints", convertHintToArray(game.getGrid()));
        response.put("messages", game.getMessages());
        return response;
    }

    @PostMapping("/clear-hint")
    @ResponseBody
    public Map<String, Object> clearHint(@RequestParam String gameId) {
        Map<String, Object> response = new HashMap<>();
        
        GameState game = activeGames.get(gameId);
        if (game == null) {
            response.put("error", "Game not found");
            return response;
        }
        
        game = gameService.clearHint(game);
        
        response.put("grid", convertGridToArray(game.getGrid()));
        response.put("hints", convertHintToArray(game.getGrid()));
        return response;
    }

    @PostMapping("/restart")
    @ResponseBody
    public Map<String, Object> restartGame(@RequestParam String gameId) {
        Map<String, Object> response = new HashMap<>();
        
        // Check if game exists
        if (!activeGames.containsKey(gameId)) {
            response.put("error", "Game not found");
            return response;
        }
        
        // Create a completely new game
        GameState newGame = gameService.createNewGame();
        
        // Replace the existing game with the new one (same ID)
        activeGames.put(gameId, newGame);
        newGame.setGameId(gameId); // Keep the same game ID
        
        response.put("grid", convertGridToArray(newGame.getGrid()));
        response.put("hints", convertHintToArray(newGame.getGrid()));
        response.put("score", newGame.getScore());
        response.put("messages", newGame.getMessages());
        response.put("gameOver", newGame.isGameOver());
        
        return response;
    }
    
    // Helper method to convert Block[][] to simple int[][]
    private int[][] convertGridToArray(Block[][] grid) {
        int[][] gridArray = new int[10][20];
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 20; col++) {
                if (grid[row][col] != null) {
                    gridArray[row][col] = grid[row][col].getValue();
                } else {
                    gridArray[row][col] = 0;
                }
            }
        }
        return gridArray;
    }
    
    // Helper method to convert hint flags to boolean[][]
    private boolean[][] convertHintToArray(Block[][] grid) {
        boolean[][] hintArray = new boolean[10][20];
        for (int row = 0; row < 10; row++) {
            for (int col = 0; col < 20; col++) {
                if (grid[row][col] != null) {
                    hintArray[row][col] = grid[row][col].isHinted();
                } else {
                    hintArray[row][col] = false;
                }
            }
        }
        return hintArray;
    }
    
    // Test endpoint
    @GetMapping("/test")
    @ResponseBody
    public String test() {
        return "Enhanced Game Controller is working! Active games: " + activeGames.size();
    }
}