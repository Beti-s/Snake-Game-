public class Board extends JPanel implements ActionListener {

    private final int B_WIDTH = 300;
    private final int B_HEIGHT = 300;
    private final int DOT_SIZE = 10;
    private final int ALL_DOTS = 900;
    private final int INITIAL_DELAY = 140;
    private int delay = INITIAL_DELAY;

    private final int x[] = new int[ALL_DOTS];
    private final int y[] = new int[ALL_DOTS];

    private int length = 5;
    private int foodEaten;
    private int foodX;
    private int foodY;
    private int bonusFoodX;
    private int bonusFoodY;
    private boolean bonusFoodVisible = false;
    private char direction = 'R'; 
    private boolean running = false;
    private final FoodProducer foodProducer = new FoodProducer(); // Producer thread
    private Timer timer;

    private int highScore = 0; 
    private JLabel scoreLabel; // To display the score and high score

    private JButton startButton;
    private JFrame frame;

    // Load the saved high score when the game starts
    public void loadHighScore() {
        try (BufferedReader reader = new BufferedReader(new FileReader("highscore.txt"))) {
            String line = reader.readLine();
            if (line != null) {
                highScore = Integer.parseInt(line);
            }
        } catch (IOException e) {
            // If file doesn't exist, start with score 0
        }
    }

    // Save the high score to a file
    public void saveHighScore() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("highscore.txt"))) {
            writer.write(String.valueOf(highScore));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Board(JFrame frame, JButton startButton, JLabel scoreLabel) {
        this.frame = frame;
        this.startButton = startButton;
        this.scoreLabel = scoreLabel;

        this.setPreferredSize(new Dimension(B_WIDTH, B_HEIGHT));
        this.setFocusable(true);
        this.addKeyListener(new MyKeyAdapter());
        loadHighScore();  // Load the high score at the start of the game
    }

    public void play() {
        foodProducer.start(); // Start food generation
        running = true;
        timer = new Timer(delay, this);
        timer.start();
    }

    public void resetGame() {
        length = 5;
        foodEaten = 0;
        direction = 'R';
        bonusFoodVisible = false; // Hide bonus food
        running = true;
        play();
    }

    private synchronized void addFood(int x, int y) {
        foodX = x;
        foodY = y;
    }

    private synchronized void addBonusFood(int x, int y) {
        bonusFoodX = x;
        bonusFoodY = y;
        bonusFoodVisible = true; // Show bonus food
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawBackground(g);
        draw(g);
    }

    private void drawBackground(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        GradientPaint gradient = new GradientPaint(0, 0, new Color(30, 30, 50), B_WIDTH, B_HEIGHT, new Color(10, 10, 20));
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, B_WIDTH, B_HEIGHT);
    }

    private void draw(Graphics g) {
        if (running) {
            // Draw food
            g.setColor(new Color(255, 85, 85)); // Vibrant red for food
            g.fillOval(foodX, foodY, DOT_SIZE, DOT_SIZE);

            // Draw bonus food if visible
            if (bonusFoodVisible) {
                g.setColor(new Color(0, 255, 0)); // Bright green for bonus food
                g.fillOval(bonusFoodX, bonusFoodY, DOT_SIZE * 2, DOT_SIZE * 2); // Increased size for bonus food
            }

            // Draw snake
            for (int i = 0; i < length; i++) {
                if (i == 0) {
                    g.setColor(new Color(255, 215, 0)); // Gold for the head
                } else {
                    g.setColor(new Color(100, 149, 237)); // Cornflower blue for the body
                }
                g.fillRect(x[i], y[i], DOT_SIZE, DOT_SIZE);
            }
        } else {
            gameOver(g);
        }
    }

    private void move() {
        for (int i = length; i > 0; i--) {
            x[i] = x[i - 1];
            y[i] = y[i - 1];
        }

        switch (direction) {
            case 'L' -> x[0] -= DOT_SIZE;
            case 'R' -> x[0] += DOT_SIZE;
            case 'U' -> y[0] -= DOT_SIZE;
            case 'D' -> y[0] += DOT_SIZE;
        }
    }

    private synchronized void checkFood() {
        // Check if regular food is eaten
        if (x[0] == foodX && y[0] == foodY) {
            length++;
            foodEaten++;

            // Increase speed
            delay = Math.max(50, delay - 5); // Minimum delay is 50ms
            timer.setDelay(delay);

            synchronized (foodProducer) {
                foodProducer.notify(); // Notify producer to generate new food
            }

            // Check if bonus food should appear after eating 5 regular food
            if (foodEaten % 5 == 0) {
                // Generate bonus food at a random location
                int bx = generateFoodPosition();
                int by = generateFoodPosition();
                addBonusFood(bx, by);
            }
        }

        // Check if bonus food is eaten
        if (bonusFoodVisible && x[0] == bonusFoodX && y[0] == bonusFoodY) {
            bonusFoodVisible = false; // Remove bonus food after it is eaten
            foodEaten += 5; // Add 5 points for consuming the bonus food
        }
    }

    private void checkCollision() {
        for (int i = length; i > 0; i--) {
            if (x[0] == x[i] && y[0] == y[i]) {
                running = false;
            }
        }

        if (x[0] < 0 || x[0] >= B_WIDTH || y[0] < 0 || y[0] >= B_HEIGHT) {
            running = false;
        }

        if (!running) {
            if (foodEaten > highScore) {
                highScore = foodEaten; // Update the high score
            }
            saveHighScore(); // Save the new high score
            timer.stop();
            foodProducer.interrupt(); // Stop producer thread
            startButton.setVisible(true); // Show the start button again
        }
    }

    private void gameOver(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        // Gradient Game Over text
        Font font = new Font("Serif", Font.BOLD, 40);
        g2d.setFont(font);
        GradientPaint gradient = new GradientPaint(0, 0, Color.RED, B_WIDTH, B_HEIGHT, Color.ORANGE);
        g2d.setPaint(gradient);
        FontMetrics metrics = getFontMetrics(font);
        String gameOverText = "GAME OVER";
        g2d.drawString(gameOverText, (B_WIDTH - metrics.stringWidth(gameOverText)) / 2, B_HEIGHT / 2);

        // Score display
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 20));
        metrics = getFontMetrics(g.getFont());
        String scoreText = "Score: " + foodEaten;
        g.drawString(scoreText, (B_WIDTH - metrics.stringWidth(scoreText)) / 2, B_HEIGHT / 2 + 30);

        // High Score display
        String highScoreText = "High Score: " + highScore;
        g.drawString(highScoreText, (B_WIDTH - metrics.stringWidth(highScoreText)) / 2, B_HEIGHT / 2 + 60);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (running) {
            move();
            checkFood();
            checkCollision();
        }
        repaint();
        scoreLabel.setText("Score: " + foodEaten + " | High Score: " + highScore); // Update the score label
    }

    private class MyKeyAdapter extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_LEFT -> {
                    if (direction != 'R') direction = 'L';
                }
                case KeyEvent.VK_RIGHT -> {
                    if (direction != 'L') direction = 'R';
                }
                case KeyEvent.VK_UP -> {
                    if (direction != 'D') direction = 'U';
                }
                case KeyEvent.VK_DOWN -> {
                    if (direction != 'U') direction = 'D';
                }
            }
        }
    }

    private class FoodProducer extends Thread {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    synchronized (Board.this) {
                        // Generate a random food position within the valid grid cells
                        int x = generateFoodPosition();
                        int y = generateFoodPosition();

                        // Ensure the food is not placed on the snake's body or outside the board
                        while (isFoodOnSnake(x, y) || x < 0 || x >= B_WIDTH || y < 0 || y >= B_HEIGHT) {
                            x = generateFoodPosition();
                            y = generateFoodPosition();
                        }

                        // Add the food to the game board
                        addFood(x, y);

                        // Wait for the snake to eat the food and then generate new food
                        Board.this.wait();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Exit on interruption
                }
            }
        }

        // Helper method to check if the food is placed on the snake's body
        private boolean isFoodOnSnake(int foodX, int foodY) {
            for (int i = 0; i < length; i++) {
                if (x[i] == foodX && y[i] == foodY) {
                    return true; // Food overlaps with snake body
                }
            }
            return false; // No overlap
        }

        // Notify producer to generate new food after the current food is eaten
        public synchronized void notifyProducer() {
            Board.this.notify(); // Notify producer to generate new food
        }
    }

    // Generate food position
    private int generateFoodPosition() {
        return (new Random().nextInt(B_WIDTH / DOT_SIZE)) * DOT_SIZE;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Snake Game");

        // Create a panel for the score and high score, and add it to the top
        JPanel scorePanel = new JPanel();
        scorePanel.setPreferredSize(new Dimension(300, 50));
        scorePanel.setBackground(new Color(30, 30, 50));
        JLabel scoreLabel = new JLabel("Score: 0 | High Score: 0");
        scoreLabel.setForeground(Color.WHITE);
        scorePanel.add(scoreLabel);

        // Create the "Start Game" button
        JButton startButton = new JButton("Start Game");
        startButton.setPreferredSize(new Dimension(300, 50));
        startButton.setFont(new Font("Arial", Font.PLAIN, 20));
        startButton.addActionListener(e -> {
            Board board = new Board(frame, startButton, scoreLabel); 
            frame.add(board, BorderLayout.CENTER);
            board.resetGame(); // Reset and start the game
            startButton.setVisible(false); // Hide the start button after the game starts
        });

        frame.add(scorePanel, BorderLayout.NORTH); // Add score panel at the top
        frame.add(startButton, BorderLayout.SOUTH); // Add the start button at the bottom

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 400); // Set appropriate frame size
        frame.setLocationRelativeTo(null); // Center the window
        frame.setVisible(true);
    }
}
