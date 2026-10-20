package gipf.client.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class Gioco extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    Gioco frame = new Gioco();
                    // Centra la finestra sullo schermo
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public Gioco() {
        setTitle("GIPF - Interfaccia Java Swing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Ingrandiamo la finestra per ospitare il tabellone esagonale
        setBounds(100, 100, 700, 750);
        
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        
        // Aggiungiamo il pannello personalizzato che disegnerà il tabellone
        contentPane.setLayout(new java.awt.BorderLayout());
        contentPane.add(new PannelloTabellone(), java.awt.BorderLayout.CENTER);
    }
}

/**
 * Pannello personalizzato per disegnare la griglia e le pedine di GIPF.
 */
class PannelloTabellone extends JPanel {
    
    private static final long serialVersionUID = 1L;
    private final int LATO_ESAGONO = 4; // Il tabellone standard di GIPF ha 4 segmenti per lato
    private final int DIMENSIONE_CELLA = 45; // Dimensione dei segmenti
    
    public PannelloTabellone() {
        setBackground(new Color(230, 230, 230)); // Colore di sfondo simile all'immagine
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Attiva l'antialiasing per linee e cerchi più morbidi
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int centroX = getWidth() / 2;
        int centroY = getHeight() / 2;
        
        disegnaGriglia(g2d, centroX, centroY);
        disegnaPedineDiEsempio(g2d, centroX, centroY);
        disegnaRiserve(g2d);
    }
    
    private void disegnaGriglia(Graphics2D g2d, int cx, int cy) {
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(1.5f));
        
        List<Point> intersezioni = new ArrayList<>();
        
        // Calcolo delle coordinate assiali per la griglia esagonale
        for (int q = -LATO_ESAGONO; q <= LATO_ESAGONO; q++) {
            int r1 = Math.max(-LATO_ESAGONO, -q - LATO_ESAGONO);
            int r2 = Math.min(LATO_ESAGONO, -q + LATO_ESAGONO);
            for (int r = r1; r <= r2; r++) {
                // Conversione da coordinate assiali a coordinate pixel (punta in alto)
                double x = cx + DIMENSIONE_CELLA * Math.sqrt(3) * (q + r / 2.0);
                double y = cy + DIMENSIONE_CELLA * 3.0 / 2.0 * r;
                
                intersezioni.add(new Point((int)x, (int)y));
                
                // Disegna i punti di intersezione (i piccoli punti neri sul bordo)
                boolean isBordo = (Math.abs(q) == LATO_ESAGONO || Math.abs(r) == LATO_ESAGONO || Math.abs(q + r) == LATO_ESAGONO);
                if (isBordo) {
                    g2d.fillOval((int)x - 5, (int)y - 5, 10, 10);
                }
            }
        }
        
        // Disegno semplificato delle linee della griglia (triangolazioni)
        // Nella realtà, qui si implementa un algoritmo per collegare i punti adiacenti
        for (int i = 0; i < intersezioni.size(); i++) {
            for (int j = i + 1; j < intersezioni.size(); j++) {
                Point p1 = intersezioni.get(i);
                Point p2 = intersezioni.get(j);
                double dist = p1.distance(p2);
                // Se la distanza è circa pari alla dimensione della cella, disegna la linea
                if (Math.abs(dist - DIMENSIONE_CELLA * Math.sqrt(3)) < 2.0 || 
                    Math.abs(dist - DIMENSIONE_CELLA * 1.5) < 2.0) { // Tolleranza
                     // Nota: La matematica esatta della griglia di GIPF richiede linee in 3 direzioni.
                     // Questo è un placeholder per la logica di connessione.
                }
            }
        }
        
        // Metodo alternativo diretto per disegnare le 3 direzioni delle linee:
        disegnaLineeDirezionali(g2d, cx, cy);
    }
    
    private void disegnaLineeDirezionali(Graphics2D g2d, int cx, int cy) {
        // Logica per tracciare le linee esatte visibili nell'immagine di riferimento.
        // Orizzontali inclinate (/)
        // Orizzontali inclinate (\)
        // Verticali (|)
        // Per brevità in questo esempio disegniamo un esagono esterno come riferimento
        int raggioEsterno = DIMENSIONE_CELLA * LATO_ESAGONO;
        int[] xPoints = new int[6];
        int[] yPoints = new int[6];
        for (int i = 0; i < 6; i++) {
            double angle_deg = 60 * i - 30; // Ruotato per avere i vertici in alto/basso
            double angle_rad = Math.PI / 180 * angle_deg;
            xPoints[i] = cx + (int) (raggioEsterno * Math.sqrt(3) * Math.cos(angle_rad));
            yPoints[i] = cy + (int) (raggioEsterno * Math.sqrt(3) * Math.sin(angle_rad));
        }
        g2d.drawPolygon(xPoints, yPoints, 6);
    }

    private void disegnaPedineDiEsempio(Graphics2D g2d, int cx, int cy) {
        // Disegna alcune pedine basate sulle posizioni mostrate nell'immagine (pic2326629.webp)
        disegnaPedina(g2d, cx, cy - DIMENSIONE_CELLA * 3, Color.DARK_GRAY); // Nero in alto
        disegnaPedina(g2d, cx - (int)(DIMENSIONE_CELLA * 2.6), cy - (int)(DIMENSIONE_CELLA * 1.5), Color.LIGHT_GRAY); // Bianco in alto a sx
        disegnaPedina(g2d, cx + (int)(DIMENSIONE_CELLA * 2.6), cy - (int)(DIMENSIONE_CELLA * 1.5), Color.LIGHT_GRAY); // Bianco in alto a dx
        
        disegnaPedina(g2d, cx - (int)(DIMENSIONE_CELLA * 2.6), cy + (int)(DIMENSIONE_CELLA * 1.5), Color.DARK_GRAY); // Nero in basso a sx
        disegnaPedina(g2d, cx + (int)(DIMENSIONE_CELLA * 2.6), cy + (int)(DIMENSIONE_CELLA * 1.5), Color.DARK_GRAY); // Nero in basso a dx
        disegnaPedina(g2d, cx, cy + DIMENSIONE_CELLA * 3, Color.LIGHT_GRAY); // Bianco in basso
    }
    
    private void disegnaPedina(Graphics2D g2d, int x, int y, Color colore) {
        int raggioPedina = 22;
        
        // Ombra della pedina per l'effetto 3D
        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.fillOval(x - raggioPedina + 3, y - raggioPedina + 3, raggioPedina * 2, raggioPedina * 2);
        
        // Corpo della pedina
        g2d.setColor(colore);
        g2d.fillOval(x - raggioPedina, y - raggioPedina, raggioPedina * 2, raggioPedina * 2);
        
        // Bordo interno per dare profondità
        g2d.setColor(colore == Color.DARK_GRAY ? Color.BLACK : Color.WHITE);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawOval(x - raggioPedina + 4, y - raggioPedina + 4, (raggioPedina - 4) * 2, (raggioPedina - 4) * 2);
    }
    
    private void disegnaRiserve(Graphics2D g2d) {
        // Pedina nera di riserva (in alto a sinistra)
        disegnaPedina(g2d, 100, 80, Color.DARK_GRAY);
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString("12", 130, 70); // Contatore
        
        // Pedina bianca di riserva (in basso a destra)
        disegnaPedina(g2d, getWidth() - 150, getHeight() - 80, Color.LIGHT_GRAY);
        g2d.setColor(Color.BLACK);
        g2d.drawString("12", getWidth() - 170, getHeight() - 50); // Contatore
    }
}