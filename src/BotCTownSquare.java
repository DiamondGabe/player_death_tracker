import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import javax.imageio.ImageIO;

public class BotCTownSquare extends JPanel {

    static class Player {
        String name;
        boolean isAlive = true;
        int x, y;

        public Player(String name) {
            this.name = name;
        }
    }

    private List<Player> players;
    private Image bgImage;
    private int nodeRadius = 30;

    public BotCTownSquare(List<String> playerNames) {
        this.players = new ArrayList<>();
        for (String name : playerNames) {
            this.players.add(new Player(name));
        }

        // --- BACKGROUND IMAGE LOADING ---
        try {
            File bgFile = new File("botc-icon.png");
            System.out.println("Looking for background at: " + bgFile.getAbsolutePath());
            
            if (bgFile.exists()) {
                bgImage = ImageIO.read(bgFile);
                System.out.println("Background image loaded successfully!");
            } else {
                System.out.println("WARNING: File botc-icon.png not found at the above location. Defaulting to black.");
            }
        } catch (Exception e) {
            System.out.println("Error loading background image:");
            e.printStackTrace();
        }

        // --- CLICK LISTENER ---
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                for (Player p : players) {
                    int dx = e.getX() - p.x;
                    int dy = e.getY() - p.y;
                    if (dx * dx + dy * dy <= nodeRadius * nodeRadius) {
                        p.isAlive = !p.isAlive;
                        repaint();
                        break;
                    }
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        // 1. Draw Background
        if (bgImage != null) {
            g2.drawImage(bgImage, 0, 0, width, height, this);
        } else {
            g2.setColor(new Color(128, 40, 158));
            g2.fillRect(0, 0, width, height);
        }

        if (players.isEmpty()) return;

        // 2. Calculate Circle Math
        int cx = width / 2;
        int cy = height / 2;
        int mainRadius = Math.min(width, height) / 2 - 120; 

        nodeRadius = Math.max(20, Math.min(50, (int) (Math.PI * mainRadius / players.size() * 0.85)));
        double angleStep = 2 * Math.PI / players.size();
        double startingAngle = -Math.PI / 2;

        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(14, nodeRadius / 2 + 5)));
        FontMetrics fm = g2.getFontMetrics();

        // 3. Draw Players
        int aliveCount = 0;
        
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (p.isAlive) aliveCount++;
            
            double angle = startingAngle + i * angleStep;
            p.x = cx + (int) (mainRadius * Math.cos(angle));
            p.y = cy + (int) (mainRadius * Math.sin(angle));

            // Draw Node
            if (p.isAlive) {
                g2.setColor(new Color(40, 120, 220)); 
                g2.fillOval(p.x - nodeRadius, p.y - nodeRadius, nodeRadius * 2, nodeRadius * 2);
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(3));
                g2.drawOval(p.x - nodeRadius, p.y - nodeRadius, nodeRadius * 2, nodeRadius * 2);
            } else {
                g2.setColor(new Color(40, 40, 40, 220)); 
                g2.fillOval(p.x - nodeRadius, p.y - nodeRadius, nodeRadius * 2, nodeRadius * 2);
                g2.setColor(new Color(200, 40, 40)); 
                g2.setStroke(new BasicStroke(3));
                g2.drawOval(p.x - nodeRadius, p.y - nodeRadius, nodeRadius * 2, nodeRadius * 2);
                
                g2.drawLine(p.x - nodeRadius / 2, p.y - nodeRadius / 2, p.x + nodeRadius / 2, p.y + nodeRadius / 2);
                g2.drawLine(p.x + nodeRadius / 2, p.y - nodeRadius / 2, p.x - nodeRadius / 2, p.y + nodeRadius / 2);
            }

            // Draw Name tag
            int textWidth = fm.stringWidth(p.name);
            int textHeight = fm.getAscent();
            
            int textCenterX = p.x + (int) ((nodeRadius + 25) * Math.cos(angle));
            int textCenterY = p.y + (int) ((nodeRadius + 25) * Math.sin(angle));

            int tx = textCenterX - textWidth / 2;
            int ty = textCenterY + textHeight / 3;

            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRoundRect(tx - 8, ty - textHeight - 2, textWidth + 16, textHeight + 8, 15, 15);
            
            g2.setColor(p.isAlive ? Color.WHITE : new Color(150, 150, 150));
            g2.drawString(p.name, tx, ty);
        }

        // 4. Draw Center HUD
        String hudText = "Alive: " + aliveCount + "   |   Dead: " + (players.size() - aliveCount);
        g2.setFont(new Font("SansSerif", Font.BOLD, 28));
        FontMetrics hudFm = g2.getFontMetrics();
        int hudWidth = hudFm.stringWidth(hudText);
        int hudHeight = hudFm.getAscent();
        
        int hudX = cx - (hudWidth / 2);
        int hudY = cy + (hudHeight / 3);

        // Dark pill background for HUD
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRoundRect(hudX - 20, hudY - hudHeight - 10, hudWidth + 40, hudHeight + 20, 20, 20);

        // HUD Text
        g2.setColor(Color.WHITE);
        g2.drawString(hudText, hudX, hudY);
    }

    public static void main(String[] args) {
        List<String> names = new ArrayList<>();
        File jsonFile = new File("players.json");

        // --- JSON LOADING LOGIC ---
        if (jsonFile.exists()) {
            System.out.println("Found players.json! Loading names...");
            try {
                String content = Files.readString(jsonFile.toPath());
                // Simple parsing: split by double quotes and take every odd index
                String[] parts = content.split("\"");
                for (int i = 1; i < parts.length; i += 2) {
                    if (!parts[i].trim().isEmpty()) {
                        names.add(parts[i]);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error reading players.json:");
                e.printStackTrace();
            }
        } else {
            System.out.println("players.json not found. Enter names manually.");
            Scanner scanner = new Scanner(System.in);
            int count = 1;
            while (true) {
                System.out.print("Player " + count + ": ");
                String line = scanner.nextLine().trim();
                
                if (line.equalsIgnoreCase("done") || line.equalsIgnoreCase("start")) {
                    break;
                }
                if (!line.isEmpty()) {
                    names.add(line);
                    count++;
                }
            }
            
            // Auto-generate the JSON file so the GM doesn't have to type it again
            if (!names.isEmpty()) {
                try {
                    StringBuilder sb = new StringBuilder("[\n");
                    for (int i = 0; i < names.size(); i++) {
                        sb.append("  \"").append(names.get(i)).append("\"");
                        if (i < names.size() - 1) sb.append(",");
                        sb.append("\n");
                    }
                    sb.append("\n]");
                    Files.writeString(jsonFile.toPath(), sb.toString());
                    System.out.println("Saved your names to players.json for next time!");
                } catch (Exception e) {
                    System.out.println("Could not save to players.json.");
                }
            }
        }
        
        System.out.println("\nStarting Town Square with " + names.size() + " players...");

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Blood on the Clocktower - Masquerade");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1280, 720); 
            frame.setLocationRelativeTo(null);
            
            BotCTownSquare panel = new BotCTownSquare(names);
            frame.add(panel);
            frame.setVisible(true);
        });
    }
}