import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.DecimalFormat;

public class Main extends JFrame {

    // Поля для RGB
    private final JSlider sliderR = new JSlider(0, 255, 0);
    private final JSlider sliderG = new JSlider(0, 255, 0);
    private final JSlider sliderB = new JSlider(0, 255, 0);
    private final JTextField tfR = new JTextField("0", 4);
    private final JTextField tfG = new JTextField("0", 4);
    private final JTextField tfB = new JTextField("0", 4);

    // Поля для CMYK (0..100 %)
    // ЗАМЕНА: C,M,Y по умолчанию 0, K по умолчанию 100 (чёрный)
    private final JSlider sliderC = new JSlider(0, 100, 0);
    private final JSlider sliderM = new JSlider(0, 100, 0);
    private final JSlider sliderY = new JSlider(0, 100, 0);
    private final JSlider sliderK = new JSlider(0, 100, 100);
    private final JTextField tfC = new JTextField("0", 4);
    private final JTextField tfM = new JTextField("0", 4);
    private final JTextField tfY = new JTextField("0", 4);
    private final JTextField tfK = new JTextField("100", 4);

    // Поля для HSV: H 0..360, S,V 0..100
    private final JSlider sliderH = new JSlider(0, 360, 0);
    private final JSlider sliderS = new JSlider(0, 100, 0);
    private final JSlider sliderV = new JSlider(0, 100, 0);
    private final JTextField tfH = new JTextField("0", 5);
    private final JTextField tfS = new JTextField("0", 4);
    private final JTextField tfV = new JTextField("0", 4);

    // Поле-предпросмотр цвета
    private final JPanel preview = new JPanel();

    // Флаг чтобы избежать циклических обновлений при синхронизации полей
    private boolean updating = false;

    // Формат для вывода дробных значений (используется только для внутренних вычислений)
    private final DecimalFormat df = new DecimalFormat("0.##");

    public Main() {
        super("Лабораторная работа 1 — Цветовые модели: CMYK / RGB / HSV (без JColorChooser)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 640);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        getContentPane().setLayout(new BorderLayout(8, 8));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        left.add(createRGBPanel());
        left.add(Box.createVerticalStrut(8));
        left.add(createCMYKPanel());
        left.add(Box.createVerticalStrut(8));
        left.add(createHSVPanel());
        left.add(Box.createVerticalStrut(8));
        left.add(createSwatchesPanel()); // заменили палитру на свои swatches

        getContentPane().add(left, BorderLayout.CENTER);

        preview.setPreferredSize(new Dimension(320, 320));
        preview.setBorder(BorderFactory.createTitledBorder("Preview"));
        getContentPane().add(preview, BorderLayout.EAST);

        setColorFromRGB(0, 0, 0);
    }

    // Панель RGB: ползунки + текстовые поля
    private JPanel createRGBPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("RGB (0..255)"));

        JPanel sliders = new JPanel(new GridLayout(3, 1, 4, 4));

        sliders.add(createSliderRow("R", sliderR, tfR));
        sliders.add(createSliderRow("G", sliderG, tfG));
        sliders.add(createSliderRow("B", sliderB, tfB));

        p.add(sliders, BorderLayout.CENTER);

        RGBListener rgbListener = new RGBListener();
        sliderR.addChangeListener(rgbListener);
        sliderG.addChangeListener(rgbListener);
        sliderB.addChangeListener(rgbListener);

        tfR.addActionListener(rgbListener);
        tfG.addActionListener(rgbListener);
        tfB.addActionListener(rgbListener);

        // Обработчики потери фокуса (ввод через клавиатуру)
        addFocusParse(tfR, this::rgbTextChanged);
        addFocusParse(tfG, this::rgbTextChanged);
        addFocusParse(tfB, this::rgbTextChanged);

        return p;
    }

    // Панель CMYK: ползунки + текстовые поля
    private JPanel createCMYKPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("CMYK (0..100%)"));

        JPanel sliders = new JPanel(new GridLayout(4, 1, 4, 4));

        sliders.add(createSliderRow("C", sliderC, tfC));
        sliders.add(createSliderRow("M", sliderM, tfM));
        sliders.add(createSliderRow("Y", sliderY, tfY));
        sliders.add(createSliderRow("K", sliderK, tfK));

        p.add(sliders, BorderLayout.CENTER);

        CMYKListener cmykListener = new CMYKListener();
        sliderC.addChangeListener(cmykListener);
        sliderM.addChangeListener(cmykListener);
        sliderY.addChangeListener(cmykListener);
        sliderK.addChangeListener(cmykListener);

        tfC.addActionListener(cmykListener);
        tfM.addActionListener(cmykListener);
        tfY.addActionListener(cmykListener);
        tfK.addActionListener(cmykListener);

        addFocusParse(tfC, this::cmykTextChanged);
        addFocusParse(tfM, this::cmykTextChanged);
        addFocusParse(tfY, this::cmykTextChanged);
        addFocusParse(tfK, this::cmykTextChanged);

        return p;
    }

    // Панель HSV: ползунки + текстовые поля
    private JPanel createHSVPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("HSV (H:0..360, S,V:0..100)"));

        JPanel sliders = new JPanel(new GridLayout(3, 1, 4, 4));

        sliders.add(createSliderRow("H", sliderH, tfH));
        sliders.add(createSliderRow("S", sliderS, tfS));
        sliders.add(createSliderRow("V", sliderV, tfV));

        p.add(sliders, BorderLayout.CENTER);

        HSVListener hsvListener = new HSVListener();
        sliderH.addChangeListener(hsvListener);
        sliderS.addChangeListener(hsvListener);
        sliderV.addChangeListener(hsvListener);

        tfH.addActionListener(hsvListener);
        tfS.addActionListener(hsvListener);
        tfV.addActionListener(hsvListener);

        addFocusParse(tfH, this::hsvTextChanged);
        addFocusParse(tfS, this::hsvTextChanged);
        addFocusParse(tfV, this::hsvTextChanged);

        return p;
    }

    // Новая панель: набор заранее заданных swatches (кнопки/квадраты цветов)
    private JPanel createSwatchesPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder("Swatches (предустановленные цвета)"));

        Color[] colors = new Color[]{
                new Color(0, 0, 0),       // черный
                new Color(255, 255, 255), // белый
                new Color(255, 0, 0),     // красный
                new Color(0, 255, 0),     // зеленый
                new Color(0, 0, 255),     // синий
                new Color(255, 255, 0),   // желтый
                new Color(255, 0, 255),   // пурпурный
                new Color(0, 255, 255),   // циан
                new Color(128, 128, 128), // серый
                new Color(128, 0, 0),     // темно-красный
                new Color(0, 128, 0),     // темно-зеленый
                new Color(0, 0, 128),     // темно-синий
                new Color(255, 165, 0),   // оранжевый
                new Color(128, 0, 128),   // фиолетовый
                new Color(255, 192, 203), // розовый
                new Color(210, 180, 140)  // tan
        };

        JPanel grid = new JPanel(new GridLayout(4, 4, 6, 6));
        for (Color c : colors) {
            JButton btn = new JButton();
            btn.setBackground(c);
            btn.setOpaque(true);
            btn.setBorderPainted(false);
            btn.setPreferredSize(new Dimension(36, 36));
            btn.setToolTipText("R=" + c.getRed() + " G=" + c.getGreen() + " B=" + c.getBlue());
            btn.addActionListener(e -> setColorFromRGB(c.getRed(), c.getGreen(), c.getBlue()));
            grid.add(btn);
        }

        p.add(grid, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField hexField = new JTextField("#RRGGBB", 8);
        JButton applyHex = new JButton("Применить HEX");
        applyHex.addActionListener(e -> {
            String s = hexField.getText().trim();
            if (s.startsWith("#")) s = s.substring(1);
            if (s.length() == 6) {
                try {
                    int r = Integer.parseInt(s.substring(0, 2), 16);
                    int g = Integer.parseInt(s.substring(2, 4), 16);
                    int b = Integer.parseInt(s.substring(4, 6), 16);
                    setColorFromRGB(r, g, b);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Неверный HEX", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "HEX должен быть в формате RRGGBB", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        });
        bottom.add(hexField);
        bottom.add(applyHex);

        p.add(bottom, BorderLayout.SOUTH);

        return p;
    }

    private JPanel createSliderRow(String name, JSlider slider, JTextField tf) {
        JPanel row = new JPanel(new BorderLayout(4, 4));
        JLabel lbl = new JLabel(name);
        lbl.setPreferredSize(new Dimension(20, 20));
        row.add(lbl, BorderLayout.WEST);

        row.add(slider, BorderLayout.CENTER);

        tf.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(tf, BorderLayout.EAST);

        return row;
    }

    private void addFocusParse(JTextField tf, Runnable onChange) {
        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                onChange.run();
            }
        });
    }

    // Установка цвета по RGB и обновление всех представлений (как было)
    private void setColorFromRGB(int r, int g, int b) {
        if (updating) return;
        updating = true;
        try {
            sliderR.setValue(r);
            sliderG.setValue(g);
            sliderB.setValue(b);
            tfR.setText(String.valueOf(r));
            tfG.setText(String.valueOf(g));
            tfB.setText(String.valueOf(b));

            preview.setBackground(new Color(r, g, b));

            // Вычисляем CMYK из RGB
            double[] cmyk = rgbToCmyk(r, g, b);
            sliderC.setValue((int) Math.round(cmyk[0]));
            sliderM.setValue((int) Math.round(cmyk[1]));
            sliderY.setValue((int) Math.round(cmyk[2]));
            sliderK.setValue((int) Math.round(cmyk[3]));
            tfC.setText(Integer.toString((int) Math.round(cmyk[0])));
            tfM.setText(Integer.toString((int) Math.round(cmyk[1])));
            tfY.setText(Integer.toString((int) Math.round(cmyk[2])));
            tfK.setText(Integer.toString((int) Math.round(cmyk[3])));

            // Вычисляем HSV из RGB
            double[] hsv = rgbToHsv(r, g, b); // H:0..360, S,V:0..100
            sliderH.setValue((int) Math.round(hsv[0]));
            sliderS.setValue((int) Math.round(hsv[1]));
            sliderV.setValue((int) Math.round(hsv[2]));
            tfH.setText(Integer.toString((int) Math.round(hsv[0])));
            tfS.setText(Integer.toString((int) Math.round(hsv[1])));
            tfV.setText(Integer.toString((int) Math.round(hsv[2])));
        } finally {
            updating = false;
        }
    }

    // НОВЫЙ: установка цвета по CMYK без повтора CMYK->RGB->CMYK
    private void setColorFromCMYK(double c, double m, double y, double k) {
        if (updating) return;
        updating = true;
        try {
            // Обновим CMYK-ползунки и текстовые поля
            sliderC.setValue((int) Math.round(c));
            sliderM.setValue((int) Math.round(m));
            sliderY.setValue((int) Math.round(y));
            sliderK.setValue((int) Math.round(k));
            tfC.setText(Integer.toString((int) Math.round(c)));
            tfM.setText(Integer.toString((int) Math.round(m)));
            tfY.setText(Integer.toString((int) Math.round(y)));
            tfK.setText(Integer.toString((int) Math.round(k)));

            // Получим RGB из CMYK и обновим RGB-поля и preview
            int[] rgb = cmykToRgb(c, m, y, k);
            sliderR.setValue(rgb[0]);
            sliderG.setValue(rgb[1]);
            sliderB.setValue(rgb[2]);
            tfR.setText(String.valueOf(rgb[0]));
            tfG.setText(String.valueOf(rgb[1]));
            tfB.setText(String.valueOf(rgb[2]));

            preview.setBackground(new Color(rgb[0], rgb[1], rgb[2]));

            // Обновим HSV (для согласованности интерфейса)
            double[] hsv = rgbToHsv(rgb[0], rgb[1], rgb[2]);
            sliderH.setValue((int) Math.round(hsv[0]));
            sliderS.setValue((int) Math.round(hsv[1]));
            sliderV.setValue((int) Math.round(hsv[2]));
            tfH.setText(Integer.toString((int) Math.round(hsv[0])));
            tfS.setText(Integer.toString((int) Math.round(hsv[1])));
            tfV.setText(Integer.toString((int) Math.round(hsv[2])));
        } finally {
            updating = false;
        }
    }

    // Обработчики изменений: RGB
    private void rgbTextChanged() {
        if (updating) return;
        try {
            int r = parseIntClamped(tfR.getText(), 0, 255);
            int g = parseIntClamped(tfG.getText(), 0, 255);
            int b = parseIntClamped(tfB.getText(), 0, 255);
            setColorFromRGB(r, g, b);
        } catch (NumberFormatException ex) {
            // Игнор
        }
    }

    // Обработчики изменений: CMYK
    private void cmykTextChanged() {
        if (updating) return;
        try {
            double c = parseDoubleClamped(tfC.getText(), 0, 100);
            double m = parseDoubleClamped(tfM.getText(), 0, 100);
            double y = parseDoubleClamped(tfY.getText(), 0, 100);
            double k = parseDoubleClamped(tfK.getText(), 0, 100);

            // Обновляем CMYK и соответствующий RGB/preview без повторного пересчёта CMYK
            setColorFromCMYK(c, m, y, k);

        } catch (NumberFormatException ex) {
            // игнор
        }
    }

    // Обработчики изменений: HSV
    private void hsvTextChanged() {
        if (updating) return;
        updating = true;
        try {
            double h = parseDoubleClamped(tfH.getText(), 0, 360);
            double s = parseDoubleClamped(tfS.getText(), 0, 100);
            double v = parseDoubleClamped(tfV.getText(), 0, 100);

            sliderH.setValue((int) Math.round(h));
            sliderS.setValue((int) Math.round(s));
            sliderV.setValue((int) Math.round(v));
            tfH.setText(Integer.toString((int) Math.round(h)));
            tfS.setText(Integer.toString((int) Math.round(s)));
            tfV.setText(Integer.toString((int) Math.round(v)));

            int[] rgb = hsvToRgb(h, s, v);
            setColorFromRGB(rgb[0], rgb[1], rgb[2]);
        } catch (NumberFormatException ex) {
            // игнор
        } finally {
            updating = false;
        }
    }

    private int parseIntClamped(String s, int min, int max) {
        int v = Integer.parseInt(s.trim());
        if (v < min) v = min;
        if (v > max) v = max;
        return v;
    }

    private double parseDoubleClamped(String s, double min, double max) {
        double v = Double.parseDouble(s.trim());
        if (v < min) v = min;
        if (v > max) v = max;
        return v;
    }

    // Конвертация RGB -> CMYK
    private double[] rgbToCmyk(int r, int g, int b) {
        double rd = r / 255.0;
        double gd = g / 255.0;
        double bd = b / 255.0;

        double k = 1 - Math.max(rd, Math.max(gd, bd));
        double c = 0, m = 0, y = 0;
        if (k < 1.0) {
            c = (1 - rd - k) / (1 - k);
            m = (1 - gd - k) / (1 - k);
            y = (1 - bd - k) / (1 - k);
        } else {
            c = 0; m = 0; y = 0;
        }
        return new double[]{c * 100.0, m * 100.0, y * 100.0, k * 100.0};
    }

    // Конвертация CMYK -> RGB
    private int[] cmykToRgb(double c, double m, double y, double k) {
        double cd = c / 100.0;
        double md = m / 100.0;
        double yd = y / 100.0;
        double kd = k / 100.0;

        int r = (int) Math.round(255 * (1 - cd) * (1 - kd));
        int g = (int) Math.round(255 * (1 - md) * (1 - kd));
        int b = (int) Math.round(255 * (1 - yd) * (1 - kd));

        r = clamp(r, 0, 255);
        g = clamp(g, 0, 255);
        b = clamp(b, 0, 255);
        return new int[]{r, g, b};
    }

    // Конвертация RGB -> HSV
    private double[] rgbToHsv(int r, int g, int b) {
        double rd = r / 255.0;
        double gd = g / 255.0;
        double bd = b / 255.0;

        double max = Math.max(rd, Math.max(gd, bd));
        double min = Math.min(rd, Math.min(gd, bd));
        double delta = max - min;

        double h = 0;
        if (delta == 0) {
            h = 0;
        } else if (max == rd) {
            h = 60 * (((gd - bd) / delta) % 6);
        } else if (max == gd) {
            h = 60 * (((bd - rd) / delta) + 2);
        } else {
            h = 60 * (((rd - gd) / delta) + 4);
        }
        if (h < 0) h += 360;

        double s = (max == 0) ? 0 : (delta / max);
        double v = max;

        return new double[]{h, s * 100.0, v * 100.0};
    }

    // Конвертация HSV -> RGB
    private int[] hsvToRgb(double h, double s, double v) {
        double sd = s / 100.0;
        double vd = v / 100.0;

        double c = vd * sd; // chroma
        double hh = (h % 360) / 60.0;
        double x = c * (1 - Math.abs(hh % 2 - 1));

        double r1 = 0, g1 = 0, b1 = 0;
        if (0 <= hh && hh < 1) {
            r1 = c; g1 = x; b1 = 0;
        } else if (1 <= hh && hh < 2) {
            r1 = x; g1 = c; b1 = 0;
        } else if (2 <= hh && hh < 3) {
            r1 = 0; g1 = c; b1 = x;
        } else if (3 <= hh && hh < 4) {
            r1 = 0; g1 = x; b1 = c;
        } else if (4 <= hh && hh < 5) {
            r1 = x; g1 = 0; b1 = c;
        } else if (5 <= hh && hh < 6) {
            r1 = c; g1 = 0; b1 = x;
        }

        double m = vd - c;
        int r = (int) Math.round((r1 + m) * 255);
        int g = (int) Math.round((g1 + m) * 255);
        int b = (int) Math.round((b1 + m) * 255);

        r = clamp(r, 0, 255);
        g = clamp(g, 0, 255);
        b = clamp(b, 0, 255);
        return new int[]{r, g, b};
    }

    private int clamp(int v, int lo, int hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    // RGB listener: реагирует на изменения ползунков или Enter в текстовых полях
    private class RGBListener implements ChangeListener, ActionListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            // НЕ ставим флаг updating здесь — setColorFromRGB сам управляет флагом.
            int r = sliderR.getValue();
            int g = sliderG.getValue();
            int b = sliderB.getValue();
            tfR.setText(String.valueOf(r));
            tfG.setText(String.valueOf(g));
            tfB.setText(String.valueOf(b));
            setColorFromRGB(r, g, b);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            rgbTextChanged();
        }
    }

    // CMYK listener
    private class CMYKListener implements ChangeListener, ActionListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            // Читаем CMYK значения и применяем setColorFromCMYK (чтобы не делать CMYK->RGB->CMYK)
            double c = sliderC.getValue();
            double m = sliderM.getValue();
            double y = sliderY.getValue();
            double k = sliderK.getValue();
            tfC.setText(Integer.toString((int) Math.round(c)));
            tfM.setText(Integer.toString((int) Math.round(m)));
            tfY.setText(Integer.toString((int) Math.round(y)));
            tfK.setText(Integer.toString((int) Math.round(k)));
            setColorFromCMYK(c, m, y, k);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            cmykTextChanged();
        }
    }

    // HSV listener
    private class HSVListener implements ChangeListener, ActionListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            // НЕ ставим updating здесь — setColorFromRGB сам управляет флагом.
            double h = sliderH.getValue();
            double s = sliderS.getValue();
            double v = sliderV.getValue();
            tfH.setText(Integer.toString((int) Math.round(h)));
            tfS.setText(Integer.toString((int) Math.round(s)));
            tfV.setText(Integer.toString((int) Math.round(v)));
            int[] rgb = hsvToRgb(h, s, v);
            setColorFromRGB(rgb[0], rgb[1], rgb[2]);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            hsvTextChanged();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main app = new Main();
            app.setVisible(true);
        });
    }
}
