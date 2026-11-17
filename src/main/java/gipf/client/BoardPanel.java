package gipf.client;

import gipf.shared.data.*;
import lombok.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Pannello personalizzato per il disegno vettoriale del tabellone GIPF.
 */
public class BoardPanel extends JPanel {
    static Tabellone t = new Tabellone();
    @Getter private Posizione mossa;
    @Getter private Posizione direzione;
    //TODO (longaretti): rimuovi questa vecchia cosa
    public Point2D.Double mossaOld = null; // coordinate del clic null perchè non ho ancora cliccato

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
            int rMin = Math.max(-4, -4 + q);
            int rMax = Math.min(4, 4 + q);
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
            while (qMin <= 4 && (k + qMin < Math.max(-4, -4 + qMin) || k + qMin > Math.min(4, 4 + qMin))) {
                qMin++;
            }
            int qMax = 4;
            while (qMax >= -4 && (k + qMax < Math.max(-4, -4 + qMax) || k + qMax > Math.min(4, 4 + qMax))) {
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
            for (int r = Math.max(-4, -4 + q); r <= Math.min(4, 4 + q); r++) {
                Point2D.Double pt = getPointCoords(q, r, centerX, centerY, spacing);

                double radius;
                if (q == -4 || q == 4 || r == Math.max(-4, -4 + q) || r == Math.min(4, 4 + q)) {
                    // Punti neri esterni (dove vengono posizionate le pedine prima della spinta)
                    radius = 7.0;
                } else {
                    // Punti di intersezione interni
                    radius = 3.0;
                }
                g2.setColor(Color.BLACK);
                g2.fill(new Ellipse2D.Double(pt.x - radius, pt.y - radius, radius * 2, radius * 2));
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
            int rMax = Math.min(4, 4 + q);
            Point2D.Double ptTop = getPointCoords(q, rMax, centerX, centerY, spacing);
            String labelTop = topLabels[i];
            int textWidthTop = fm.stringWidth(labelTop);
            g2.drawString(labelTop, (float) (ptTop.x - textWidthTop / 2.0), (float) (ptTop.y - 14));

            // Etichetta Inferiore
            int rMin = Math.max(-4, -4 + q);
            Point2D.Double ptBottom = getPointCoords(q, rMin, centerX, centerY, spacing);
            String labelBottom = colLetters[i] + "1";
            int textWidthBottom = fm.stringWidth(labelBottom);
            g2.drawString(labelBottom, (float) (ptBottom.x - textWidthBottom / 2.0), (float) (ptBottom.y + 22));

        }

        if (t != null) disegna(g2, centerX, centerY, spacing);

        if(mossaOld != null) {// se ho cliccato
            disegnaPedina(g2, (int) mossaOld.getX(), (int) mossaOld.getY(), Color.cyan);
            System.out.println(mossaOld.getX() + " " + mossaOld.getY());//ùTODO: longa: la pedina deve essere in una coordinata valida
        }
    }

    // Verifica se un punto appartiene al bordo esterno

    // Trasforma le coordinate assiali (q, r) in coordinate pixel (x, y)
    private Point2D.Double getPointCoords(int q, int r, double centerX, double centerY, double spacing) {
        double x = centerX + q * spacing * Math.sqrt(3) / 2.0;
        double y = centerY - r * spacing + q * (spacing / 2.0);
        return new Point2D.Double(x, y);
    }

    // Genera in sequenza oraria i 24 punti esterni per il tratteggio
    private Point2D.Double[] getOuterRingPoints(double centerX, double centerY, double spacing) {
        java.util.List<Point2D.Double> points = new ArrayList<>();

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
        final int raggioPedina = 22;

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

    // metodo per disegnare una pedina data la sua posizione rispetto al centro del campo
    private void disegnaPedinaSuCampo(Graphics2D g2D, int posx, int posy, boolean bianco, double cx, double cy, double space){
        Color colore = bianco? Color.LIGHT_GRAY: Color.DARK_GRAY;
        Point2D.Double p= getPointCoords(posx,posy,cx, cy, space);
        disegnaPedina(g2D, (int) p.x, (int)p.y, colore);
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

        g2d.setColor(Color.BLACK);
        g2d.drawString(Integer.toString(nBianche), getWidth() - 60-x, 80+y); // Contatore
    }

    // disegna tutte le pedine bianche e nere e le riserve come definite nelle liste di Tabellone
    public void disegna(Graphics2D g2d, double cx, double cy, double spacing) {
        for(int i=0;i<t.nero.size();i++){
            disegnaPedinaSuCampo(g2d,t.nero.get(i).getX(),t.nero.get(i).getY(),false, cx, cy, spacing);
        }

        for(int i=0;i<t.bianco.size();i++){
            disegnaPedinaSuCampo(g2d,t.bianco.get(i).getX(),t.bianco.get(i).getY(),true, cx, cy, spacing);
        }

        disegnaRiserveGrafico(g2d,t.riserveBianco,t.riserveNero);
    }
}
