import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

public class TetrisGame extends JPanel implements ActionListener {
    // Constants
    private static final int BOARD_WIDTH = 10;
    private static final int BOARD_HEIGHT = 20;
    private static final int CELL_SIZE = 30;
    private static final int DELAY = 500; // Initial delay for block falling (in milliseconds)

    // Data Structures
    private Queue<Block> blockQueue = new LinkedList<>();
    private int[][] gameBoard = new int[BOARD_HEIGHT][BOARD_WIDTH];
    private Block currentBlock;
    private int score = 0;
    private int level = 1;
    private javax.swing.Timer timer; // Explicitly use javax.swing.Timer

    // Block class
    private static class Block {
        int[][] shape;
        int x, y;

        Block(int[][] shape) {
            this.shape = shape;
            this.x = 0;
            this.y = BOARD_WIDTH / 2 - shape[0].length / 2;
        }
    }

    // Initialize the game
    public TetrisGame() {
        initializeBoard();
        generateBlock();
        startGame();
    }

    // Initialize the game board
    private void initializeBoard() {
        for (int i = 0; i < BOARD_HEIGHT; i++) {
            Arrays.fill(gameBoard[i], 0); // 0 represents an empty cell
        }
    }

    // Generate a random block and enqueue it
    private void generateBlock() {
        int[][][] blockShapes = {
                { { 1, 1, 1, 1 } }, // I-block
                { { 1, 1 }, { 1, 1 } }, // O-block
                { { 1, 1, 1 }, { 0, 1, 0 } }, // T-block
                { { 1, 1, 0 }, { 0, 1, 1 } }, // Z-block
                { { 0, 1, 1 }, { 1, 1, 0 } } // S-block
        };
        Random random = new Random();
        int[][] shape = blockShapes[random.nextInt(blockShapes.length)];
        Block block = new Block(shape);
        blockQueue.add(block);
        if (currentBlock == null) {
            currentBlock = blockQueue.poll();
        }
    }

    // Start the game
    private void startGame() {
        timer = new javax.swing.Timer(DELAY, this); // Explicitly use javax.swing.Timer
        timer.start();
    }

    // Check if the block can move or rotate without collision
    private boolean isValidMove(Block block, int newX, int newY) {
        for (int i = 0; i < block.shape.length; i++) {
            for (int j = 0; j < block.shape[i].length; j++) {
                if (block.shape[i][j] != 0) {
                    int x = newX + i;
                    int y = newY + j;
                    if (x < 0 || x >= BOARD_HEIGHT || y < 0 || y >= BOARD_WIDTH || gameBoard[x][y] != 0) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // Move the block left
    public void moveLeft() {
        if (isValidMove(currentBlock, currentBlock.x, currentBlock.y - 1)) {
            currentBlock.y--;
        }
        repaint();
    }

    // Move the block right
    public void moveRight() {
        if (isValidMove(currentBlock, currentBlock.x, currentBlock.y + 1)) {
            currentBlock.y++;
        }
        repaint();
    }

    // Rotate the block
    public void rotate() {
        int[][] rotatedShape = rotateShape(currentBlock.shape);
        if (isValidMove(new Block(rotatedShape), currentBlock.x, currentBlock.y)) {
            currentBlock.shape = rotatedShape;
        }
        repaint();
    }

    // Rotate the block's shape
    private int[][] rotateShape(int[][] shape) {
        int rows = shape.length;
        int cols = shape[0].length;
        int[][] rotated = new int[cols][rows];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                rotated[j][rows - 1 - i] = shape[i][j];
            }
        }
        return rotated;
    }

    // Move the block down or place it on the board
    public void moveDown() {
        if (isValidMove(currentBlock, currentBlock.x + 1, currentBlock.y)) {
            currentBlock.x++;
        } else {
            placeBlock();
        }
        repaint();
    }

    // Place the block on the board
    private void placeBlock() {
        for (int i = 0; i < currentBlock.shape.length; i++) {
            for (int j = 0; j < currentBlock.shape[i].length; j++) {
                if (currentBlock.shape[i][j] != 0) {
                    int x = currentBlock.x + i;
                    int y = currentBlock.y + j;
                    gameBoard[x][y] = 1; // 1 represents a filled cell
                }
            }
        }
        checkCompletedRows();
        generateBlock();
    }

    // Check for completed rows and update the score
    private void checkCompletedRows() {
        for (int i = 0; i < BOARD_HEIGHT; i++) {
            boolean isComplete = true;
            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (gameBoard[i][j] == 0) {
                    isComplete = false;
                    break;
                }
            }
            if (isComplete) {
                removeRow(i);
                score += 100; // Increase score for each completed row
                if (score % 500 == 0) {
                    activatePowerUp(); // Activate power-up every 500 points
                }
                if (score % 1000 == 0) {
                    level++; // Increase level every 1000 points
                    timer.setDelay(DELAY - level * 50); // Increase speed
                }
            }
        }
    }

    // Remove a completed row and shift rows above it down
    private void removeRow(int row) {
        for (int i = row; i > 0; i--) {
            System.arraycopy(gameBoard[i - 1], 0, gameBoard[i], 0, BOARD_WIDTH);
        }
        Arrays.fill(gameBoard[0], 0); // Add a new empty row at the top
    }

    // Activate a power-up (clear a random row)
    private void activatePowerUp() {
        Random random = new Random();
        int row = random.nextInt(BOARD_HEIGHT);
        Arrays.fill(gameBoard[row], 0); // Clear the row
    }

    // Check if the game is over
    public boolean isGameOver() {
        for (int j = 0; j < BOARD_WIDTH; j++) {
            if (gameBoard[0][j] != 0) {
                return true;
            }
        }
        return false;
    }

    // Paint the game board and current block
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Draw the game board
        for (int i = 0; i < BOARD_HEIGHT; i++) {
            for (int j = 0; j < BOARD_WIDTH; j++) {
                if (gameBoard[i][j] != 0) {
                    g.setColor(Color.BLUE);
                    g.fillRect(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
                g.setColor(Color.BLACK);
                g.drawRect(j * CELL_SIZE, i * CELL_SIZE, CELL_SIZE, CELL_SIZE);
            }
        }
        // Draw the current block
        if (currentBlock != null) {
            g.setColor(Color.RED);
            for (int i = 0; i < currentBlock.shape.length; i++) {
                for (int j = 0; j < currentBlock.shape[i].length; j++) {
                    if (currentBlock.shape[i][j] != 0) {
                        int x = (currentBlock.y + j) * CELL_SIZE;
                        int y = (currentBlock.x + i) * CELL_SIZE;
                        g.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    }
                }
            }
        }
        // Display score and level
        g.setColor(Color.BLACK);
        g.drawString("Score: " + score, 10, BOARD_HEIGHT * CELL_SIZE + 20);
        g.drawString("Level: " + level, 10, BOARD_HEIGHT * CELL_SIZE + 40);
    }

    // Handle timer events (block falling)
    @Override
    public void actionPerformed(ActionEvent e) {
        if (isGameOver()) {
            timer.stop();
            JOptionPane.showMessageDialog(this, "Game Over! Final Score: " + score);
        } else {
            moveDown();
        }
    }

    // Main function to start the game
    public static void main(String[] args) {
        JFrame frame = new JFrame("Tetris Game");
        TetrisGame game = new TetrisGame();
        frame.add(game);
        frame.setSize(BOARD_WIDTH * CELL_SIZE, BOARD_HEIGHT * CELL_SIZE + 60);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // Add key bindings for controls
        InputMap inputMap = game.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = game.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke("LEFT"), "left");
        inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "right");
        inputMap.put(KeyStroke.getKeyStroke("UP"), "rotate");
        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "down");
        actionMap.put("left", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                game.moveLeft();
            }
        });
        actionMap.put("right", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                game.moveRight();
            }
        });
        actionMap.put("rotate", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                game.rotate();
            }
        });
        actionMap.put("down", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                game.moveDown();
            }
        });
    }
}