package gipf.client;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.*;

/**
 * Finestra principale della GUI per il gioco GIPF.
 */
public class GipfBoardFrame extends JFrame {
	private BoardPanel board;
    public GipfBoardFrame() {
        // Configurazione della finestra principale
        setTitle("Tabellone di Gioco GIPF");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 800);
        setLocationRelativeTo(null);

        board=new BoardPanel();
        // Aggiunta del pannello di disegno del tabellone
        add(board);
        this.gestisciMouse();
        
    }
    public void gestisciMouse(){ 
    	this.addMouseListener(new MouseAdapter() { // assegno alla form il gestore del mouse
    		public void mouseClicked(MouseEvent me) { // quando viene cliccato il mouse me => è il riferimento alla posizione del mouse
    			//controllo correttezza della posizione del mouse
    			
    			board.mossa= new Point2D.Double(me.getX()-10, me.getY()-35); // mi salvo il clic
    			repaint(); // ridisegno tutto
    		}
    	});
    }
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    //Gioco frame = new Gioco();
                	GipfBoardFrame  frame = new GipfBoardFrame ();

                    // Centra la finestra sullo schermo
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                    
                    
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}

/**
 * Pannello personalizzato per il disegno vettoriale del tabellone GIPF.
 */
class BoardPanel extends JPanel {
	
	public Point2D.Double mossa=null; // coordinate del clic null perchè non ho ancora cliccato
	
    public BoardPanel() {
        setBackground(Color.WHITE);
        
        
        
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        // Attivazione dell'Antialiasing per linee nitide e prive di sgranature
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        int width = getWidth();
        int height = getHeight();

        // Coordinate del centro del pannello
        double centerX = width / 2.0;
        double centerY = height / 2.0;

        // Spaziatura adattiva tra i punti della griglia
        double spacing = Math.min(width, height) / 11.5;

        // ==========================================
        // 1. DISEGNO DELLE LINEE DELLA GRIGLIA
        // ==========================================
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(Color.BLACK);

        // Direzione 1: Linee verticali (costante q da -4 a 4)
        for (int q = -4; q <= 4; q++) {
            int rMin = getRMin(q);
            int rMax = getRMax(q);
            Point2D.Double pStart = getPointCoords(q, rMin, centerX, centerY, spacing);
            Point2D.Double pEnd = getPointCoords(q, rMax, centerX, centerY, spacing);
            g2.draw(new Line2D.Double(pStart, pEnd));
        }

        // Direzione 2: Linee diagonali a 30° verso il basso (costante r)
        for (int r = -4; r <= 4; r++) {
            int qMin = Math.max(-4, r - 4);
            int qMax = Math.min(4, r + 4);
            Point2D.Double pStart = getPointCoords(qMin, r, centerX, centerY, spacing);
            Point2D.Double pEnd = getPointCoords(qMax, r, centerX, centerY, spacing);
            g2.draw(new Line2D.Double(pStart, pEnd));
        }

        // Direzione 3: Linee diagonali a 30° verso l'alto (costante k = r - q)
        for (int k = -4; k <= 4; k++) {
            int qMin = -4;
            while (qMin <= 4 && (k + qMin < getRMin(qMin) || k + qMin > getRMax(qMin))) {
                qMin++;
            }
            int qMax = 4;
            while (qMax >= -4 && (k + qMax < getRMin(qMax) || k + qMax > getRMax(qMax))) {
                qMax--;
            }
            if (qMin <= qMax) {
                Point2D.Double pStart = getPointCoords(qMin, k + qMin, centerX, centerY, spacing);
                Point2D.Double pEnd = getPointCoords(qMax, k + qMax, centerX, centerY, spacing);
                g2.draw(new Line2D.Double(pStart, pEnd));
            }
        }

        // ==========================================
        // 2. DISEGNO DEL PERIMETRO INTERNO ED ESTERNO
        // ==========================================

        // Perimetro dell'area di gioco principale (Hexagon interno, linea continua più spessa)
        g2.setStroke(new BasicStroke(2.0f));
        Point2D.Double[] innerRing = getInnerRingPoints(centerX, centerY, spacing);
        for (int i = 0; i < innerRing.length; i++) {
            Point2D.Double p1 = innerRing[i];
            Point2D.Double p2 = innerRing[(i + 1) % innerRing.length];
            g2.draw(new Line2D.Double(p1, p2));
        }

        // Perimetro esterno tratteggiato che unisce i 24 punti neri esterni
        float[] dashPattern = {5.0f, 5.0f};
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dashPattern, 0.0f));
        g2.setColor(Color.DARK_GRAY);

        Point2D.Double[] outerRing = getOuterRingPoints(centerX, centerY, spacing);
        for (int i = 0; i < outerRing.length; i++) {
            Point2D.Double p1 = outerRing[i];
            Point2D.Double p2 = outerRing[(i + 1) % outerRing.length];
            g2.draw(new Line2D.Double(p1, p2));
        }

        // ==========================================
        // 3. DISEGNO DEI PUNTI ED INTERSEZIONI
        // ==========================================
        g2.setStroke(new BasicStroke(1.0f));

        for (int q = -4; q <= 4; q++) {
            for (int r = getRMin(q); r <= getRMax(q); r++) {
                Point2D.Double pt = getPointCoords(q, r, centerX, centerY, spacing);

                if (isOuterPoint(q, r)) {
                    // Punti neri esterni (dove vengono posizionate le pedine prima della spinta)
                    double radius = 7.0;
                    g2.setColor(Color.BLACK);
                    g2.fill(new Ellipse2D.Double(pt.x - radius, pt.y - radius, radius * 2, radius * 2));
                } else {
                    // Punti di intersezione interni
                    double radius = 3.0;
                    g2.setColor(Color.BLACK);
                    g2.fill(new Ellipse2D.Double(pt.x - radius, pt.y - radius, radius * 2, radius * 2));
                }
            }
        }

        // ==========================================
        // 4. DISEGNO DELLE ETICHETTE (A1-I1, A5-I5, ECC.)
        // ==========================================
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.setColor(Color.BLACK);
        FontMetrics fm = g2.getFontMetrics();

        String[] colLetters = {"A", "B", "C", "D", "E", "F", "G", "H", "I"};
        String[] topLabels = {"A5", "B6", "C7", "D8", "E9", "F8", "G7", "H6", "I5"};

        for (int i = 0; i < 9; i++) {
            int q = i - 4;

            // Etichetta Superiore
            int rMax = getRMax(q);
            Point2D.Double ptTop = getPointCoords(q, rMax, centerX, centerY, spacing);
            String labelTop = topLabels[i];
            int textWidthTop = fm.stringWidth(labelTop);
            g2.drawString(labelTop, (float) (ptTop.x - textWidthTop / 2.0), (float) (ptTop.y - 14));

            // Etichetta Inferiore
            int rMin = getRMin(q);
            Point2D.Double ptBottom = getPointCoords(q, rMin, centerX, centerY, spacing);
            String labelBottom = colLetters[i] + "1";
            int textWidthBottom = fm.stringWidth(labelBottom);
            g2.drawString(labelBottom, (float) (ptBottom.x - textWidthBottom / 2.0), (float) (ptBottom.y + 22));
            
        }
        /*
        Point2D.Double p= getPointCoords(-2,-2,centerX, centerY, spacing);
        disegnaPedina(g2, (int) p.x, (int)p.y, Color.DARK_GRAY);
        Point2D.Double p2= getPointCoords(-1,-1,centerX, centerY, spacing);
        disegnaPedina(g2, (int) p2.x, (int)p2.y, Color.LIGHT_GRAY);
        Point2D.Double p3= getPointCoords(1,2,centerX, centerY, spacing);
        disegnaPedina(g2, (int) p3.x, (int)p3.y, Color.DARK_GRAY);
        
        
        
        disegnaPedinaSuCampo(g2,4,4,true, centerX, centerY, spacing);
        disegnaPedinaSuCampo(g2,1,0,true, centerX, centerY, spacing);
        disegnaPedinaSuCampo(g2,1,1,false, centerX, centerY, spacing);
        disegnaPedinaSuCampo(g2,-2,0,true, centerX, centerY, spacing);
        disegnaPedinaSuCampo(g2,-2,-4,true, centerX, centerY, spacing);
        disegnaPedinaSuCampo(g2,4,1,true, centerX, centerY, spacing);
        
        */
        Point2D.Double[] nere = new Point2D.Double[3];
        Point2D.Double[] bianche = new Point2D.Double[5];
        nere[0] = new Point2D.Double(-2,-2);
        nere[1] = new Point2D.Double(1,2);
        nere[2] = new Point2D.Double(1,1);
        bianche[0] = new Point2D.Double(4,4);
        bianche[1] = new Point2D.Double(1,0);
        bianche[2] = new Point2D.Double(-2,0);
        bianche[3] = new Point2D.Double(-2,-4);
        bianche[4] = new Point2D.Double(4,1);
        
        disegna(g2,nere,bianche,centerX,centerY,spacing);
        
        disegnaRiserveGrafico (g2,12,10);
        
        if(mossa!=null) // se ho cliccato
        disegnaPedina(g2,(int)mossa.getX(),(int)mossa.getY(),Color.cyan);
        
    }

    // --- METODI GEOMETRICI AUSILIARI ---

    // Calcola il raggio minimo della colonna q
    private int getRMin(int q) {
        return Math.max(-4, -4 + q);
    }

    // Calcola il raggio massimo della colonna q
    private int getRMax(int q) {
        return Math.min(4, 4 + q);
    }

    // Verifica se un punto appartiene al bordo esterno
    private boolean isOuterPoint(int q, int r) {
        return q == -4 || q == 4 || r == getRMin(q) || r == getRMax(q);
    }

    // Trasforma le coordinate assiali (q, r) in coordinate pixel (x, y)
    private Point2D.Double getPointCoords(int q, int r, double centerX, double centerY, double spacing) {
        double x = centerX + q * spacing * Math.sqrt(3) / 2.0;
        double y = centerY - r * spacing + q * (spacing / 2.0);
        return new Point2D.Double(x, y);
    }

    // Genera in sequenza oraria i 24 punti esterni per il tratteggio
    private Point2D.Double[] getOuterRingPoints(double centerX, double centerY, double spacing) {
        List<Point2D.Double> points = new ArrayList<>();

        for (int r = -4; r <= 0; r++) points.add(getPointCoords(-4, r, centerX, centerY, spacing));
        points.add(getPointCoords(-3, 1, centerX, centerY, spacing));
        points.add(getPointCoords(-2, 2, centerX, centerY, spacing));
        points.add(getPointCoords(-1, 3, centerX, centerY, spacing));
        points.add(getPointCoords(0, 4, centerX, centerY, spacing));
        for (int q = 1; q <= 4; q++) points.add(getPointCoords(q, 4, centerX, centerY, spacing));
        for (int r = 3; r >= 0; r--) points.add(getPointCoords(4, r, centerX, centerY, spacing));
        points.add(getPointCoords(3, -1, centerX, centerY, spacing));
        points.add(getPointCoords(2, -2, centerX, centerY, spacing));
        points.add(getPointCoords(1, -3, centerX, centerY, spacing));
        points.add(getPointCoords(0, -4, centerX, centerY, spacing));
        for (int q = -1; q >= -3; q--) points.add(getPointCoords(q, -4, centerX, centerY, spacing));

        return points.toArray(new Point2D.Double[0]);
    }

    // Genera i punti del perimetro del tabellone di gioco interno
    private Point2D.Double[] getInnerRingPoints(double centerX, double centerY, double spacing) {
        List<Point2D.Double> points = new ArrayList<>();
        int[][] innerCoords = {
            {-3, 0}, {-2, 1}, {-1, 2}, {0, 3}, {1, 3}, {2, 3},
            {3, 3}, {3, 2}, {3, 1}, {3, 0}, {2, -1}, {1, -2},
            {0, -3}, {-1, -3}, {-2, -3}, {-3, -3}, {-3, -2}, {-3, -1}
        };

        for (int[] coord : innerCoords) {
            points.add(getPointCoords(coord[0], coord[1], centerX, centerY, spacing));
        }
        return points.toArray(new Point2D.Double[0]);
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
    // METODO PER DISEGNARE UNA PEDINA DATA LA SUA POSIZIONE RISPETTO AL CENTRO DEL CAMPO
    private void disegnaPedinaSuCampo(Graphics2D g2D, int posx, int posy, boolean bianco, double cx, double cy, double space){
    	
    	Color colore = bianco==true? Color.LIGHT_GRAY: Color.DARK_GRAY;
    	Point2D.Double p= getPointCoords(posx,posy,cx, cy, space);
        disegnaPedina(g2D, (int) p.x, (int)p.y, colore);
        
    }
    
    private void disegnaRiserve(Graphics2D g2d, int nNere, int nBianche) {
    	FontMetrics metrics = g2d.getFontMetrics(new Font("Dialog", Font.PLAIN, 12));
        // Determine the X coordinate for the text
        int x =  (26 - metrics.stringWidth(Integer.toString(nNere))) / 2;
        // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
        int y = (26 - metrics.getHeight()) / 2;
        // Set the font
        g2d.setFont(new Font("Dialog", Font.PLAIN, 12));
        // Pedina nera di riserva (in alto a sinistra)
        disegnaPedina(g2d, 80, 80, Color.DARK_GRAY);
        
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString(Integer.toString(nNere), 80-x, 80+y); // Contatore
        
        x =  (26 - metrics.stringWidth(Integer.toString(nBianche))) / 2;
        // Pedina bianca di riserva (in basso a destra)
        disegnaPedina(g2d, getWidth() - 80, getHeight() - 80, Color.LIGHT_GRAY);
        g2d.setColor(Color.BLACK);
        g2d.drawString(Integer.toString(nBianche), getWidth() - 80-x, getHeight() - 80+y); // Contatore
    }
    

    private void disegnaRiserveGrafico(Graphics2D g2d, int nNere, int nBianche) {
    	
        for(int i=0;i<nNere;i++){
        	disegnaPedina(g2d, 60, 80+i*45, Color.DARK_GRAY);
        }
    	
        for(int i=0;i<nBianche;i++){
        	disegnaPedina(g2d, getWidth() - 60, 80 + i*45, Color.LIGHT_GRAY);
        }
        
        FontMetrics metrics = g2d.getFontMetrics(new Font("Dialog", Font.PLAIN, 12));
        // Determine the X coordinate for the text
        int x =  (26 - metrics.stringWidth(Integer.toString(nNere))) / 2;
        // Determine the Y coordinate for the text (note we add the ascent, as in java 2d 0 is top of the screen)
        int y = (26 - metrics.getHeight()) / 2;
        // Set the font
        g2d.setFont(new Font("Dialog", Font.PLAIN, 12));
        // Pedina nera di riserva (in alto a sinistra)
        
        
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.drawString(Integer.toString(nNere), 60-x, 80+y); // Contatore
        
        x =  (26 - metrics.stringWidth(Integer.toString(nBianche))) / 2;
        // Pedina bianca di riserva (in basso a destra)
        
        g2d.setColor(Color.BLACK);
        g2d.drawString(Integer.toString(nBianche), getWidth() - 60-x, 80+y); // Contatore
       
    }
    
    // funzione che prende due array di punti e disegna in quei punti le corrispettive pedine nere e bianche
    public void disegna(Graphics2D g2d, Point2D.Double [] nere, Point2D.Double [] bianche, double cx, double cy, double spacing) {
    	for(int i=0;i<nere.length;i++) {
    		disegnaPedinaSuCampo(g2d,(int) nere[i].getX(),(int) nere[i].getY(),false, cx, cy, spacing);
    		
    	}
    	
    	for(int i=0;i<bianche.length;i++) {
    		disegnaPedinaSuCampo(g2d,(int) bianche[i].getX(),(int) bianche[i].getY(),true, cx, cy, spacing);
    	}
    }
    
    // disegna tutte le pedine bianche e nere e le riserve come definite nelle liste di Tabellone
    public void disegna(Graphics2D g2d, Tabellone t, double cx, double cy, double spacing) {
    	for(int i=0;i<t.nero.size();i++) {	disegnaPedinaSuCampo(g2d,t.nero.get(i).getX(),t.nero.get(i).getY(),false, cx, cy, spacing);
    	}
    	
    	for(int i=0;i<t.bianco.size();i++){	disegnaPedinaSuCampo(g2d,t.bianco.get(i).getX(),t.bianco.get(i).getY(),true, cx, cy, spacing);
    		
    	}
    	disegnaRiserveGrafico (g2d,t.riserveBianco,t.riserveNero);
    }
}
