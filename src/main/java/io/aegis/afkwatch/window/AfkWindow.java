package io.aegis.afkwatch.window;

import io.aegis.afkwatch.core.Mode;
import io.aegis.afkwatch.core.Palette;
import io.aegis.afkwatch.core.Session;
import io.aegis.afkwatch.core.Settings;
import io.aegis.afkwatch.core.View;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Small borderless always-on-top status window, for when the game is behind
 * another app. Purely a display: it never takes focus and never talks to the game.
 */
public final class AfkWindow {
    private static final int WIDTH = 248;
    private static final int HEIGHT = 86;
    private static final int MARGIN = 24;
    private static final int REFRESH_MS = 100;

    private final Session session;
    private final Settings settings;

    private JFrame frame;
    private View view = View.INACTIVE;
    private boolean dismissed;
    private long dismissedAtJoin = -1;
    private int lastSeverity;

    private AfkWindow(Session session, Settings settings) {
        this.session = session;
        this.settings = settings;
    }

    /** Creates the window if this platform supports it; reports why not through {@code note}. */
    public static void install(Session session, Settings settings, Consumer<String> note) {
        if (!settings.windowEnabled) {
            return;
        }
        if (GraphicsEnvironment.isHeadless()) {
            note.accept("Window mode unavailable: Java is running headless.");
            return;
        }
        if (System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac")) {
            note.accept("Window mode is disabled on macOS (AWT conflicts with the game's window).");
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                new AfkWindow(session, settings).start();
            } catch (RuntimeException e) {
                note.accept("Window mode failed to start: " + e);
            }
        });
    }

    private void start() {
        frame = new JFrame("AEGIS AFK Watch");
        frame.setUndecorated(true);
        frame.setType(Window.Type.UTILITY); // keeps it off the taskbar
        frame.setBackground(new Color(0, 0, 0, 0));
        frame.setAlwaysOnTop(settings.windowAlwaysOnTop);
        frame.setFocusableWindowState(false);
        frame.setAutoRequestFocus(false);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        StatusPanel panel = new StatusPanel();
        frame.setContentPane(panel);
        frame.setSize(WIDTH, HEIGHT);
        place();
        try {
            frame.setOpacity(settings.windowOpacity);
        } catch (RuntimeException ignored) {
            // Translucency not supported here; the window is simply fully opaque.
        }

        new Timer(REFRESH_MS, e -> refresh(panel)).start();
    }

    private void place() {
        if (settings.windowX >= 0 && settings.windowY >= 0) {
            frame.setLocation(settings.windowX, settings.windowY);
            return;
        }
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        frame.setLocation(screen.x + screen.width - WIDTH - MARGIN, screen.y + MARGIN);
    }

    private void refresh(StatusPanel panel) {
        view = session.view(System.currentTimeMillis());

        if (session.joinCount() != dismissedAtJoin) {
            dismissed = false; // a new connection brings the window back
        }

        boolean wanted = !dismissed && switch (view.mode()) {
            case INACTIVE -> false;
            case ACTIVE -> settings.windowShowWhenActive;
            default -> true;
        };

        int severity = view.mode() == Mode.WARNING ? view.severity() : 0;
        if (settings.beepOnWarning && severity > lastSeverity) {
            Toolkit.getDefaultToolkit().beep();
        }
        lastSeverity = severity;

        if (frame.isVisible() != wanted) {
            frame.setVisible(wanted);
        }
        if (wanted) {
            panel.repaint();
        }
    }

    private final class StatusPanel extends JPanel {
        private static final int ARC = 18;
        private final Font titleFont = new Font("Segoe UI", Font.BOLD, 10);
        private final Font headlineFont = new Font("Segoe UI", Font.BOLD, 20);
        private final Font detailFont = new Font("Segoe UI", Font.PLAIN, 12);
        private Point dragOffset;

        StatusPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(WIDTH, HEIGHT));

            JPopupMenu menu = new JPopupMenu();
            JCheckBoxMenuItem onTop = new JCheckBoxMenuItem("Always on top", settings.windowAlwaysOnTop);
            onTop.addActionListener(e -> {
                settings.windowAlwaysOnTop = onTop.isSelected();
                frame.setAlwaysOnTop(settings.windowAlwaysOnTop);
                settings.save();
            });
            JMenuItem hide = new JMenuItem("Hide until next connection");
            hide.addActionListener(e -> {
                dismissed = true;
                dismissedAtJoin = session.joinCount();
            });
            menu.add(onTop);
            menu.add(hide);

            MouseAdapter mouse = new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        menu.show(StatusPanel.this, e.getX(), e.getY());
                    } else {
                        dragOffset = e.getPoint();
                    }
                }

                @Override public void mouseDragged(MouseEvent e) {
                    if (dragOffset != null) {
                        Point p = e.getLocationOnScreen();
                        frame.setLocation(p.x - dragOffset.x, p.y - dragOffset.y);
                    }
                }

                @Override public void mouseReleased(MouseEvent e) {
                    if (e.isPopupTrigger()) {
                        menu.show(StatusPanel.this, e.getX(), e.getY());
                    } else if (dragOffset != null) {
                        dragOffset = null;
                        settings.windowX = frame.getX();
                        settings.windowY = frame.getY();
                        settings.save();
                    }
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            View v = view;
            Color accent = new Color(Palette.withAlpha(Palette.accent(v), Palette.pulse(v, System.currentTimeMillis())), true);

            g.setColor(new Color(16, 20, 24, 235));
            g.fillRoundRect(0, 0, WIDTH - 1, HEIGHT - 1, ARC, ARC);
            g.setColor(accent);
            g.setStroke(new java.awt.BasicStroke(2f));
            g.drawRoundRect(1, 1, WIDTH - 3, HEIGHT - 3, ARC, ARC);
            g.fillRoundRect(1, 1, 6, HEIGHT - 3, ARC, ARC);

            g.setFont(titleFont);
            g.setColor(new Color(130, 140, 155));
            g.drawString("AEGIS  ·  AFK WATCH", 20, 20);

            g.setFont(headlineFont);
            g.setColor(Color.WHITE);
            g.drawString(v.headline(), 20, 47);

            g.setFont(detailFont);
            g.setColor(new Color(184, 192, 204));
            g.drawString(v.detail(), 20, 69);
            g.dispose();
        }
    }
}
