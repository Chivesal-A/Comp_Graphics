import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LabFrame().setVisible(true));
    }
}

class LabFrame extends JFrame {
    private DrawPanel drawPanel;
    private JComboBox<String> algoBox;
    private JTextField x1Field, y1Field, x2Field, y2Field, rField;
    private JCheckBox gridBox, axesBox, animateBox;
    private JSlider scaleSlider;
    private JTextArea logArea;
    private JButton drawBtn, clearBtn, stepBtn;

    public LabFrame() {
        setTitle("Лабораторная 3 — растровые алгоритмы");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);

        drawPanel = new DrawPanel();
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(drawPanel, BorderLayout.CENTER);

        JPanel left = new JPanel();
        left.setPreferredSize(new Dimension(340, 0));
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        algoBox = new JComboBox<>(new String[]{"Пошаговый (Step)", "ЦДА (DDA)", "Брезенхем (Line)", "Брезенхем (Circle)"});
        left.add(new JLabel("Алгоритм:"));
        left.add(algoBox);

        JPanel coords = new JPanel(new GridLayout(5,2,4,4));
        coords.setMaximumSize(new Dimension(320, 140));
        x1Field = new JTextField("-20"); y1Field = new JTextField("-10");
        x2Field = new JTextField("20"); y2Field = new JTextField("10"); rField = new JTextField("15");
        coords.add(new JLabel("x1 / xc:")); coords.add(x1Field);
        coords.add(new JLabel("y1 / yc:")); coords.add(y1Field);
        coords.add(new JLabel("x2 (для линии):")); coords.add(x2Field);
        coords.add(new JLabel("y2 (для линии):")); coords.add(y2Field);
        coords.add(new JLabel("R (для окружности):")); coords.add(rField);
        left.add(Box.createRigidArea(new Dimension(0,8)));
        left.add(coords);

        drawBtn = new JButton("Draw");
        clearBtn = new JButton("Clear");
        stepBtn = new JButton("Step");
        stepBtn.setEnabled(false);

        JPanel btns = new JPanel();
        btns.add(drawBtn); btns.add(clearBtn); btns.add(stepBtn);
        left.add(Box.createRigidArea(new Dimension(0,6)));
        left.add(btns);

        gridBox = new JCheckBox("Показывать сетку", true);
        axesBox = new JCheckBox("Показывать оси", true);
        animateBox = new JCheckBox("Анимация");
        left.add(gridBox); left.add(axesBox); //left.add(animateBox);

        left.add(Box.createRigidArea(new Dimension(0,6)));
        left.add(new JLabel("Масштаб (пикселей на единицу):"));
        scaleSlider = new JSlider(6, 40, 12);
        scaleSlider.setMajorTickSpacing(6);
        scaleSlider.setPaintTicks(true);
        scaleSlider.setPaintLabels(true);
        left.add(scaleSlider);

        left.add(Box.createRigidArea(new Dimension(0,6)));
        left.add(new JLabel("Отчёт / вычисления:"));
        logArea = new JTextArea(10, 20);
        logArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(logArea);
        left.add(scroll);

        getContentPane().add(left, BorderLayout.WEST);

        scaleSlider.addChangeListener(e -> drawPanel.setScale(scaleSlider.getValue()));
        gridBox.addActionListener(e -> drawPanel.setShowGrid(gridBox.isSelected()));
        axesBox.addActionListener(e -> drawPanel.setShowAxes(axesBox.isSelected()));
        algoBox.addActionListener(e -> updateStepAvailability());
        animateBox.addActionListener(e -> drawPanel.setAnimate(animateBox.isSelected()));

        drawBtn.addActionListener(e -> doDraw(false));
        clearBtn.addActionListener(e -> { drawPanel.clear(); logArea.setText(""); });
        stepBtn.addActionListener(e -> doDraw(true));

        updateStepAvailability();
    }

    private void updateStepAvailability() {
        String algo = (String) algoBox.getSelectedItem();
        boolean circle = algo.contains("Circle");
        rField.setEnabled(circle);
        x2Field.setEnabled(!circle);
        y2Field.setEnabled(!circle);
        stepBtn.setEnabled(algo.contains("Step") || algo.contains("DDA") || algo.contains("Brez"));
    }

    private void doDraw(boolean stepMode) {
        try {
            int x1 = Integer.parseInt(x1Field.getText().trim());
            int y1 = Integer.parseInt(y1Field.getText().trim());
            String algoStr = (String) algoBox.getSelectedItem();
            drawPanel.setShowGrid(gridBox.isSelected());
            drawPanel.setShowAxes(axesBox.isSelected());
            drawPanel.setScale(scaleSlider.getValue());
            logArea.setText("");

            if (algoStr.contains("Circle")) {
                int r = Integer.parseInt(rField.getText().trim());
                drawPanel.drawCircle(x1, y1, r, algoStr, stepMode, logArea);
            } else {
                int x2 = Integer.parseInt(x2Field.getText().trim());
                int y2 = Integer.parseInt(y2Field.getText().trim());
                drawPanel.drawLine(x1, y1, x2, y2, algoStr, stepMode, logArea);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Неверные числовые параметры", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }
}

class DrawPanel extends JPanel {
    private BufferedImage canvas;
    private int scale = 12;
    private boolean showGrid = true;
    private boolean showAxes = true;
    private boolean animate = false;

    private List<Point> currentPoints = new ArrayList<>();
    private int animateIndex = 0;
    private Timer timer;

    public DrawPanel() {
        setBackground(Color.WHITE);
        timer = new Timer(30, e -> {
            if (animate && animateIndex < currentPoints.size()) {
                drawPointToCanvas(currentPoints.get(animateIndex));
                animateIndex++;
                repaint();
            } else {
                timer.stop();
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                recreateCanvas();
                repaint();
            }
        });
    }

    public void setScale(int s) { this.scale = s; recreateCanvas(); repaint(); }
    public void setShowGrid(boolean v) { this.showGrid = v; repaint(); }
    public void setShowAxes(boolean v) { this.showAxes = v; repaint(); }
    public void setAnimate(boolean v) { this.animate = v; }

    private void recreateCanvas() {
        int w = Math.max(400, getWidth());
        int h = Math.max(300, getHeight());
        canvas = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0,0,w,h);
        g.dispose();
    }

    public void clear() {
        if (canvas == null) recreateCanvas();
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.WHITE); g.fillRect(0,0,canvas.getWidth(), canvas.getHeight());
        g.dispose();
        currentPoints.clear();
        animateIndex = 0;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (canvas == null) recreateCanvas();
        Graphics2D g2 = (Graphics2D) g;
        g2.drawImage(canvas, 0, 0, null);
        drawGridAndAxes(g2);
    }

    private void drawGridAndAxes(Graphics2D g2) {
        int w = getWidth(), h = getHeight();
        int ox = w/2, oy = h/2;
        g2.setStroke(new BasicStroke(1f));
        if (showGrid) {
            g2.setColor(new Color(230,230,230));
            for (int x = ox % scale; x < w; x += scale) g2.drawLine(x,0,x,h);
            for (int y = oy % scale; y < h; y += scale) g2.drawLine(0,y,w,y);
        }
        if (showAxes) {
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(0, oy, w, oy); // X axis
            g2.drawLine(ox, 0, ox, h); // Y axis
            g2.setFont(g2.getFont().deriveFont(12f));
            for (int xi = - (ox/scale); xi <= (ox/scale); xi++) {
                int px = ox + xi*scale;
                g2.drawString(String.valueOf(xi), px+2, oy+12);
            }
            for (int yi = - (oy/scale); yi <= (oy/scale); yi++) {
                int py = oy - yi*scale;
                g2.drawString(String.valueOf(yi), ox+4, py-2);
            }
        }
    }

    private void showPoints(List<Point> pts, JTextArea log, boolean stepMode) {
        currentPoints = new ArrayList<>(pts);
        animateIndex = 0;
        clear();
        Graphics2D g = canvas.createGraphics();
        if (animate && !stepMode) {
            timer.start();
        } else if (stepMode) {
            if (currentPoints.size() > 0) {
                drawPointToCanvas(currentPoints.get(0));
                animateIndex = 1;
                repaint();
            }
        } else {
            for (Point p : pts) drawPointToCanvas(p);
            repaint();
        }
        g.dispose();
    }

    private void drawPointToCanvas(Point p) {
        if (canvas == null) recreateCanvas();
        int w = canvas.getWidth(), h = canvas.getHeight();
        int ox = w/2, oy = h/2;
        int px = ox + p.x * scale;
        int py = oy - p.y * scale;
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(px - scale/2 + 1, py - scale/2 + 1, Math.max(1, scale-2), Math.max(1, scale-2));
        g.setColor(Color.BLACK);
        g.drawRect(px - scale/2 + 1, py - scale/2 + 1, Math.max(1, scale-2), Math.max(1, scale-2));
        g.dispose();
    }

    public void drawLine(int x1, int y1, int x2, int y2, String algo, boolean stepMode, JTextArea log) {
        List<Point> pts = null;
        long t0 = System.nanoTime();
        if (algo.contains("Step")) pts = stepAlgorithm(x1,y1,x2,y2, log);
        else if (algo.contains("DDA")) pts = ddaAlgorithm(x1,y1,x2,y2, log);
        else pts = bresenhamLine(x1,y1,x2,y2, log);
        long t1 = System.nanoTime();
        log.append("Время выполнения (нс): " + (t1 - t0) + "\n");
        log.append("Точек: " + pts.size() + "\n");
        showPoints(pts, log, stepMode);
    }

    public void drawCircle(int xc, int yc, int r, String algo, boolean stepMode, JTextArea log) {
        List<Point> pts = new ArrayList<>();
        long t0 = System.nanoTime();
        if (algo.contains("Circle")) pts = bresenhamCircle(xc,yc,r, log);
        long t1 = System.nanoTime();
        log.append("Время выполнения (нс): " + (t1 - t0) + "\n");
        log.append("Точек: " + pts.size() + "\n");
        showPoints(pts, log, stepMode);
    }

    private List<Point> stepAlgorithm(int x0, int y0, int x1, int y1, JTextArea log) {
        List<Point> res = new ArrayList<>();

        int dx = x1 - x0;
        int dy = y1 - y0;

        if (Math.abs(dx) >= Math.abs(dy)) {
            int step = dx > 0 ? 1 : -1;
            double k = (double) dy / dx;
            double b = y0 - k * x0;

            log.append("Пошаговый: шаг по X\n");
            log.append(String.format("dx=%d dy=%d k=%.4f b=%.4f\n", dx, dy, k, b));

            for (int x = x0; x != x1 + step; x += step) {
                int y = (int) Math.round(k * x + b);
                res.add(new Point(x, y));
                if (res.size() <= 12)
                    log.append(String.format("x=%d → y=%.4f → y_round=%d\n", x, (k * x + b), y));
            }
        }

        else {
            int step = dy > 0 ? 1 : -1;
            double k_inv = (double) dx / dy;
            double b_inv = x0 - k_inv * y0;

            log.append("Пошаговый: шаг по Y\n");
            log.append(String.format("dx=%d dy=%d k_inv=%.4f b_inv=%.4f\n", dx, dy, k_inv, b_inv));

            for (int y = y0; y != y1 + step; y += step) {
                int x = (int) Math.round(k_inv * y + b_inv);
                res.add(new Point(x, y));
                if (res.size() <= 12)
                    log.append(String.format("y=%d → x=%.4f → x_round=%d\n", y, (k_inv * y + b_inv), x));
            }
        }

        return res;
    }


    private List<Point> ddaAlgorithm(int x0,int y0,int x1,int y1, JTextArea log) {
        List<Point> res = new ArrayList<>();
        int dx = x1 - x0, dy = y1 - y0;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        double x = x0, y = y0;
        double sx = (double) dx / steps, sy = (double) dy / steps;
        log.append(String.format("DDA: dx=%d, dy=%d, steps=%d, sx=%.6f, sy=%.6f\n", dx, dy, steps, sx, sy));
        for (int i = 0; i <= steps; i++) {
            res.add(new Point((int)Math.round(x), (int)Math.round(y)));
            if (i < 10) log.append(String.format("i=%d: x=%.4f y=%.4f -> (%d,%d)\n", i, x, y, (int)Math.round(x),(int)Math.round(y)));
            x += sx; y += sy;
        }
        return res;
    }

    private List<Point> bresenhamLine(int x0,int y0,int x1,int y1, JTextArea log) {
        List<Point> res = new ArrayList<>();
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        boolean swap = false;
        if (dy > dx) {
            int t = dx; dx = dy; dy = t; swap = true;
        }
        int D = 2*dy - dx;
        int x = x0, y = y0;
        log.append(String.format("Bresenham: dx=%d dy=%d sx=%d sy=%d swap=%b\n", dx, dy, sx, sy, swap));
        for (int i = 0; i <= dx; i++) {
            res.add(new Point(x,y));
            if (i < 10) log.append(String.format("i=%d: (x,y)=(%d,%d) D=%d\n", i, x, y, D));
            if (D > 0) {
                if (swap) x += sx; else y += sy;
                D -= 2*dx;
            }
            if (swap) y += sy; else x += sx;
            D += 2*dy;
        }
        return res;
    }

    private List<Point> bresenhamCircle(int xc,int yc,int r, JTextArea log) {
        List<Point> res = new ArrayList<>();
        int x = 0, y = r;
        int d = 3 - 2 * r;
        log.append(String.format("Circle: xc=%d yc=%d r=%d\n", xc, yc, r));
        while (x <= y) {
            addCircleSymmetryPoints(res, xc, yc, x, y);
            if (d < 0) {
                d = d + 4*x + 6;
            } else {
                d = d + 4*(x - y) + 10;
                y--;
            }
            x++;
            if (res.size() <= 40) log.append(String.format("x=%d y=%d d=%d\n", x, y, d));
        }
        return res;
    }

    private void addCircleSymmetryPoints(List<Point> res, int xc, int yc, int x, int y) {
        res.add(new Point(xc + x, yc + y));
        res.add(new Point(xc - x, yc + y));
        res.add(new Point(xc + x, yc - y));
        res.add(new Point(xc - x, yc - y));
        res.add(new Point(xc + y, yc + x));
        res.add(new Point(xc - y, yc + x));
        res.add(new Point(xc + y, yc - x));
        res.add(new Point(xc - y, yc - x));
    }
}
