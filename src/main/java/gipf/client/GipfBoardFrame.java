package gipf.client;

import gipf.shared.*;
import gipf.shared.data.*;
import lombok.*;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.*;

/**
 * Finestra principale della GUI per il gioco GIPF.
 */
public class GipfBoardFrame extends JFrame {
    @Getter private static GipfBoardFrame frame;
    private final BoardPanel board;
    public GipfBoardFrame() {
        // Configurazione della finestra principale
        setTitle("Tabellone di Gioco GIPF");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(750, 800);
        setLocationRelativeTo(null);

        board = new BoardPanel();
        add(board);

        addMouseListener(new MouseAdapter() {
            //TODO: non funziona con duble-click, forse è da usare mousePressed
            public void mouseClicked(MouseEvent me) { // quando viene cliccato il mouse me => è il riferimento alla posizione del mouse
                board.mossaOld = new Point2D.Double(me.getX()-10, me.getY()-35); // mi salvo il clic
                repaint(); // ridisegno tutto
            }
        });
    }

    public static void connectToServer() {
        var info = new JDialog(frame, "Seleziona server", true);
        var panel = new JPanel();
        var ipField = new JTextField("localhost");
        var portField = new JTextField(String.valueOf(SharedConstants.defaultPort));
        var roomField = new JTextField("localhost");
        var btn = new JButton("Connettiti");

        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(new JLabel("Ip"));
        panel.add(ipField);
        panel.add(new JLabel("Porta"));
        panel.add(portField);
        panel.add(new JLabel("Stanza (vuoto = stanza casuale)"));
        panel.add(roomField);
        panel.add(btn);

        btn.addActionListener(e -> {
            String ip = ipField.getText();
            try {
                int port = Integer.parseInt(portField.getText());
                ClientConnector.init(ip, port);
                Events.CLIENT_PING.fire(true);

                if (roomField.getText().isBlank()) {
                    ClientManager.joinRandomRoom();
                } else {
                    ClientManager.joinRoom(roomField.getText());
                }

                info.dispose();
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                JOptionPane.showMessageDialog(
                        info,
                        ex.getClass().getName() + ": " + ex.getLocalizedMessage(),
                        "Errore nella connessione al server",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        info.getContentPane().add(panel);
        info.setLocationRelativeTo(null);
        info.setSize(100, 250);
        info.setVisible(true);
    }

    public static void main(String[] args) {
        ClientManager.registerListeners();

        EventQueue.invokeLater(() -> {
            try {
                frame = new GipfBoardFrame();

                // Centra la finestra sullo schermo
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

                connectToServer();
            } catch (Exception e) {
                e.printStackTrace(System.err);
            }
        });

        //enable with -Ddebug=true
        if(System.getProperty("debug", "false").equalsIgnoreCase("true")) {
            Thread helper = new Thread(() -> {
                while (true) {
                    try {
                        Runtime.getRuntime().wait(1_000);
                        Thread.yield();
                    } catch (Throwable ignored) {
                    }
                }
            }, "debug-helper");
            helper.setDaemon(true);
            helper.setPriority(Thread.MIN_PRIORITY);
            helper.start();
        }
    }
}
