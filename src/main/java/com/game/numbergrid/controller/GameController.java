package com.game.numbergrid.controller;

import com.game.numbergrid.model.Block;
import com.game.numbergrid.model.GameState;
import com.game.numbergrid.service.GameService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/game")
public class GameController {
    
    @Autowired
    private GameService gameService;
    
    private Map<String, GameState> activeGames = new HashMap<>();
    private ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/start")
    public String startGame(Model model) {
        GameState game = gameService.createNewGame();
        activeGames.put(game.getGameId(), game);
        
        // Convert grid to simple 2D array for Thymeleaf
        int[][] gridArray = convertGridToArray(game.getGrid());
        
        model.addAttribute("gameId", game.getGameId());
        model.addAttribute("grid", gridArray);
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
        
        response.put("messages", game.getMessages());
        return response;
    }

    @PostMapping("/restart")
    @ResponseBody
    public Map<String, Object> restartGame(@RequestParam String gameId) {
        Map<String, Object> response = new HashMap<>();
        
        GameState newGame = gameService.createNewGame();
        activeGames.put(gameId, newGame);
        
        response.put("grid", convertGridToArray(newGame.getGrid()));
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
    
    // Test endpoint
    @GetMapping("/test")
    @ResponseBody
    public String test() {
        return "Game controller is working! Active games: " + activeGames.size();
    }
}
