import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main extends JFrame {
    private BufferedImage srcImage;
    private BufferedImage resultImage;
    private final JLabel imageLabel = new JLabel();
    private final JComboBox<String> filterCombo = new JComboBox<>(new String[]{
            "Нет", "Mean (Box) фильтр", "Gaussian фильтр", "Adaptive Mean (локальный)", "Sauvola (локальный)"
    });
    private final JSpinner kernelSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 101, 2));
    private final JSpinner sigmaSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 50.0, 0.1));
    private final JButton applyBtn = new JButton("Применить");
    private final JButton loadBtn = new JButton("Загрузить...");
    private final JButton saveBtn = new JButton("Сохранить...");
    private final JButton resetBtn = new JButton("Сброс");
    private final JLabel statusLabel = new JLabel("Файл не загружен");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Main app = new Main();
            app.setVisible(true);
        });
    }

    public Main() {
        setTitle("Компьютерная графика — Лабораторная (вариант 12)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(new EmptyBorder(10, 10, 10, 10));
        left.add(new JLabel("<html><b>Параметры и методы</b></html>"));
        left.add(Box.createVerticalStrut(8));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(new JLabel("Метод:"));
        filterCombo.setPreferredSize(new Dimension(260, 25));
        row1.add(filterCombo);
        left.add(row1);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(new JLabel("Ядро (нечёт):"));
        kernelSpinner.setPreferredSize(new Dimension(80, 25));
        row2.add(kernelSpinner);
        row2.add(new JLabel("σ (для Gaussian):"));
        sigmaSpinner.setPreferredSize(new Dimension(80, 25));
        row2.add(sigmaSpinner);
        left.add(row2);

        left.add(Box.createVerticalStrut(6));
        JPanel buttons = new JPanel(new GridLayout(0, 1, 6, 6));
        buttons.add(loadBtn);
        buttons.add(applyBtn);
        buttons.add(saveBtn);
        buttons.add(resetBtn);
        left.add(buttons);

        left.add(Box.createVerticalGlue());
        left.add(new JLabel("<html><i>Инструкция:</i></html>"));
        JTextArea instr = new JTextArea(
                "1) Нажмите «Загрузить...» и выберите изображение.\n" +
                        "2) Выберите метод и параметры (ядро, σ).\n" +
                        "3) Нажмите «Применить» для отображения результата.\n" +
                        "4) Сохраните результат \"Сохранить...\".\n\n" +
                        "Реализованные методы:\n" +
                        "- Mean: усреднение в квадратном окне (ядро x ядро)\n" +
                        "- Gaussian: свёртка с гауссовым ядром\n" +
                        "- Adaptive Mean: локальный порог = локальная средняя\n" +
                        "- Sauvola: локальный порог по формуле Sauvola"
        );
        instr.setEditable(false);
        instr.setBackground(left.getBackground());
        left.add(instr);

        JPanel right = new JPanel(new BorderLayout());
        JScrollPane scroll = new JScrollPane(imageLabel);
        right.add(scroll, BorderLayout.CENTER);
        right.add(statusLabel, BorderLayout.SOUTH);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(left, BorderLayout.WEST);
        getContentPane().add(right, BorderLayout.CENTER);

        loadBtn.addActionListener(e -> loadImage());
        applyBtn.addActionListener(e -> applySelected());
        saveBtn.addActionListener(e -> saveResult());
        resetBtn.addActionListener(e -> {
            if (srcImage != null) {
                resultImage = deepCopy(srcImage);
                updateImageLabel(resultImage);
                status("Сброшено к исходному изображению");
            }
        });

        filterCombo.addActionListener(e -> {
            String sel = (String) filterCombo.getSelectedItem();
            if ("Gaussian фильтр".equals(sel)) {
                sigmaSpinner.setEnabled(true);
            } else {
                sigmaSpinner.setEnabled(false);
            }
        });
        sigmaSpinner.setEnabled(false);
    }

    private void status(String s) {
        statusLabel.setText(s);
    }

    private void loadImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.addChoosableFileFilter(new FileNameExtensionFilter("Image files", ImageIO.getReaderFileSuffixes()));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                BufferedImage read = ImageIO.read(f);
                if (read == null) throw new IOException("Неподдерживаемый формат изображения");
                srcImage = toARGB(read);
                resultImage = deepCopy(srcImage);
                updateImageLabel(resultImage);
                status("Загружено: " + f.getName() + " (" + srcImage.getWidth() + "x" + srcImage.getHeight() + ")");
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка загрузки: " + ex.getMessage());
                status("Ошибка загрузки");
            }
        }
    }

    private void saveResult() {
        if (resultImage == null) {
            JOptionPane.showMessageDialog(this, "Нет результата для сохранения.");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("result.png"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File out = chooser.getSelectedFile();
            try {
                String ext = "png";
                String name = out.getName();
                int idx = name.lastIndexOf('.');
                if (idx > 0) ext = name.substring(idx + 1);
                ImageIO.write(resultImage, ext, out);
                status("Сохранено: " + out.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Ошибка сохранения: " + ex.getMessage());
                status("Ошибка сохранения");
            }
        }
    }

    private void applySelected() {
        if (srcImage == null) {
            JOptionPane.showMessageDialog(this, "Сначала загрузите изображение.");
            return;
        }
        String sel = (String) filterCombo.getSelectedItem();
        int k = ((Number) kernelSpinner.getValue()).intValue();
        if (k % 2 == 0) k++; // гарантируем нечётность
        double sigma = ((Number) sigmaSpinner.getValue()).doubleValue();

        switch (sel) {
            case "Mean (Box) фильтр":
                resultImage = meanFilterIntegral(srcImage, k);
                status("Применён Mean фильтр, ядро=" + k);
                break;
            case "Gaussian фильтр":
                resultImage = gaussianFilter(srcImage, k, sigma);
                status(String.format("Применён Gaussian фильтр, ядро=%d, σ=%.2f", k, sigma));
                break;
            case "Adaptive Mean (локальный)":
                resultImage = adaptiveMeanThresholdIntegral(srcImage, k);
                status("Применена локальная пороговая обработка (Adaptive Mean), окно=" + k);
                break;
            case "Sauvola (локальный)":
                resultImage = sauvolaThresholdIntegral(srcImage, k, 0.34, 128);
                status("Применена локальная пороговая обработка (Sauvola), окно=" + k);
                break;
            default:
                resultImage = deepCopy(srcImage);
                status("Ничего не применено");
        }
        updateImageLabel(resultImage);
    }

    private void updateImageLabel(BufferedImage img) {
        if (img == null) {
            imageLabel.setIcon(null);
            return;
        }
        ImageIcon icon = new ImageIcon(img);
        imageLabel.setIcon(icon);
        imageLabel.revalidate();
    }

    // Утилиты изображений
    private static BufferedImage deepCopy(BufferedImage bi) {
        BufferedImage copy = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = copy.createGraphics();
        g.drawImage(bi, 0, 0, null);
        g.dispose();
        return copy;
    }

    // Приводит любое изображение к TYPE_INT_ARGB
    private static BufferedImage toARGB(BufferedImage img) {
        if (img.getType() == BufferedImage.TYPE_INT_ARGB) return img;
        BufferedImage dst = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        return dst;
    }

    private static BufferedImage toGrayscale(BufferedImage src) {
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = gray.getGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return gray;
    }

    // Фильтры
    private static BufferedImage meanFilterIntegral(BufferedImage src, int k) {
        int w = src.getWidth(), h = src.getHeight();
        int[] in = src.getRGB(0, 0, w, h, null, 0, w);

        // Создаём интегральные таблицы для каналов (размер (w+1)*(h+1))
        long[][] sumA = new long[h + 1][w + 1];
        long[][] sumR = new long[h + 1][w + 1];
        long[][] sumG = new long[h + 1][w + 1];
        long[][] sumB = new long[h + 1][w + 1];

        for (int y = 1; y <= h; y++) {
            long ra = 0, rr = 0, rg = 0, rb = 0;
            for (int x = 1; x <= w; x++) {
                int rgb = in[(y - 1) * w + (x - 1)];
                ra += (rgb >>> 24) & 0xFF;
                rr += (rgb >>> 16) & 0xFF;
                rg += (rgb >>> 8) & 0xFF;
                rb += (rgb) & 0xFF;
                sumA[y][x] = sumA[y - 1][x] + ra;
                sumR[y][x] = sumR[y - 1][x] + rr;
                sumG[y][x] = sumG[y - 1][x] + rg;
                sumB[y][x] = sumB[y - 1][x] + rb;
            }
        }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int r = k / 2;
        int[] outPixels = new int[w * h];

        for (int y = 0; y < h; y++) {
            int y1 = Math.max(0, y - r);
            int y2 = Math.min(h - 1, y + r);
            for (int x = 0; x < w; x++) {
                int x1 = Math.max(0, x - r);
                int x2 = Math.min(w - 1, x + r);
                // координаты в интегральной таблице с +1 смещением
                int ix1 = x1;
                int iy1 = y1;
                int ix2 = x2 + 1;
                int iy2 = y2 + 1;
                int area = (x2 - x1 + 1) * (y2 - y1 + 1);

                long sum_a = sumA[iy2][ix2] - sumA[iy1][ix2] - sumA[iy2][ix1] + sumA[iy1][ix1];
                long sum_r = sumR[iy2][ix2] - sumR[iy1][ix2] - sumR[iy2][ix1] + sumR[iy1][ix1];
                long sum_g = sumG[iy2][ix2] - sumG[iy1][ix2] - sumG[iy2][ix1] + sumG[iy1][ix1];
                long sum_b = sumB[iy2][ix2] - sumB[iy1][ix2] - sumB[iy2][ix1] + sumB[iy1][ix1];

                int a = clamp((int) (sum_a / area));
                int rrC = clamp((int) (sum_r / area));
                int ggC = clamp((int) (sum_g / area));
                int bbC = clamp((int) (sum_b / area));
                outPixels[y * w + x] = (a << 24) | (rrC << 16) | (ggC << 8) | bbC;
            }
        }
        out.setRGB(0, 0, w, h, outPixels, 0, w);
        return out;
    }

    // Gaussian filter: создаём ядро и сворачиваем (двумерная свёртка)
    private static BufferedImage gaussianFilter(BufferedImage src, int k, double sigma) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        double[] kernel = makeGaussianKernel(k, sigma);
        int r = k / 2;
        int[] inPixels = src.getRGB(0, 0, w, h, null, 0, w);
        int[] outPixels = new int[inPixels.length];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double sumA = 0, sumR = 0, sumG = 0, sumB = 0, wsum = 0;
                for (int dy = -r; dy <= r; dy++) {
                    int yy = y + dy;
                    if (yy < 0 || yy >= h) continue;
                    for (int dx = -r; dx <= r; dx++) {
                        int xx = x + dx;
                        if (xx < 0 || xx >= w) continue;
                        double kval = kernel[(dy + r) * k + (dx + r)];
                        int rgb = inPixels[yy * w + xx];
                        sumA += ((rgb >>> 24) & 0xFF) * kval;
                        sumR += ((rgb >>> 16) & 0xFF) * kval;
                        sumG += ((rgb >>> 8) & 0xFF) * kval;
                        sumB += (rgb & 0xFF) * kval;
                        wsum += kval;
                    }
                }
                int a = clamp((int) Math.round(sumA / wsum));
                int rr = clamp((int) Math.round(sumR / wsum));
                int gg = clamp((int) Math.round(sumG / wsum));
                int bb = clamp((int) Math.round(sumB / wsum));
                outPixels[y * w + x] = (a << 24) | (rr << 16) | (gg << 8) | bb;
            }
        }
        out.setRGB(0, 0, w, h, outPixels, 0, w);
        return out;
    }

    private static double[] makeGaussianKernel(int k, double sigma) {
        int r = k / 2;
        double[] kernel = new double[k * k];
        double s2 = sigma * sigma;
        double sum = 0;
        for (int y = -r; y <= r; y++) {
            for (int x = -r; x <= r; x++) {
                double val = Math.exp(-(x * x + y * y) / (2 * s2));
                kernel[(y + r) * k + (x + r)] = val;
                sum += val;
            }
        }
        for (int i = 0; i < kernel.length; i++) kernel[i] /= sum;
        return kernel;
    }

    private static int clamp(int v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }

    // Локальные пороги (через интегралы)
    private static BufferedImage adaptiveMeanThresholdIntegral(BufferedImage src, int k) {
        BufferedImage gray = toGrayscale(src);
        int w = gray.getWidth(), h = gray.getHeight();
        int[] g = new int[w * h];
        gray.getRaster().getPixels(0, 0, w, h, g);

        // интегральная таблица для яркости
        long[][] sum = new long[h + 1][w + 1];
        for (int y = 1; y <= h; y++) {
            long rowSum = 0;
            for (int x = 1; x <= w; x++) {
                rowSum += g[(y - 1) * w + (x - 1)];
                sum[y][x] = sum[y - 1][x] + rowSum;
            }
        }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] outPixels = new int[w * h];
        int r = k / 2;

        for (int y = 0; y < h; y++) {
            int y1 = Math.max(0, y - r);
            int y2 = Math.min(h - 1, y + r);
            for (int x = 0; x < w; x++) {
                int x1 = Math.max(0, x - r);
                int x2 = Math.min(w - 1, x + r);
                int ix1 = x1;
                int iy1 = y1;
                int ix2 = x2 + 1;
                int iy2 = y2 + 1;
                int area = (x2 - x1 + 1) * (y2 - y1 + 1);
                long s = sum[iy2][ix2] - sum[iy1][ix2] - sum[iy2][ix1] + sum[iy1][ix1];
                int localMean = (int) (s / area);
                int val = g[y * w + x];
                int v = (val <= localMean) ? 0 : 255;
                outPixels[y * w + x] = (0xFF << 24) | (v << 16) | (v << 8) | v;
            }
        }
        out.setRGB(0, 0, w, h, outPixels, 0, w);
        return out;
    }

    private static BufferedImage sauvolaThresholdIntegral(BufferedImage src, int k, double ksau, double R) {
        BufferedImage gray = toGrayscale(src);
        int w = gray.getWidth(), h = gray.getHeight();
        int[] g = new int[w * h];
        gray.getRaster().getPixels(0, 0, w, h, g);

        // интегралы: sum и sumSq
        double[][] sum = new double[h + 1][w + 1];
        double[][] sumSq = new double[h + 1][w + 1];
        for (int y = 1; y <= h; y++) {
            double rowSum = 0;
            double rowSumSq = 0;
            for (int x = 1; x <= w; x++) {
                int val = g[(y - 1) * w + (x - 1)];
                rowSum += val;
                rowSumSq += (double) val * val;
                sum[y][x] = sum[y - 1][x] + rowSum;
                sumSq[y][x] = sumSq[y - 1][x] + rowSumSq;
            }
        }

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] outPixels = new int[w * h];
        int r = k / 2;

        for (int y = 0; y < h; y++) {
            int y1 = Math.max(0, y - r);
            int y2 = Math.min(h - 1, y + r);
            for (int x = 0; x < w; x++) {
                int x1 = Math.max(0, x - r);
                int x2 = Math.min(w - 1, x + r);
                int ix1 = x1;
                int iy1 = y1;
                int ix2 = x2 + 1;
                int iy2 = y2 + 1;
                int area = (x2 - x1 + 1) * (y2 - y1 + 1);

                double s = sum[iy2][ix2] - sum[iy1][ix2] - sum[iy2][ix1] + sum[iy1][ix1];
                double s2 = sumSq[iy2][ix2] - sumSq[iy1][ix2] - sumSq[iy2][ix1] + sumSq[iy1][ix1];
                double mean = s / area;
                double var = s2 / area - mean * mean;
                double std = var > 0 ? Math.sqrt(var) : 0;
                double T = mean * (1.0 + ksau * (std / R - 1.0));
                int val = g[y * w + x];
                int v = (val <= T) ? 0 : 255;
                outPixels[y * w + x] = (0xFF << 24) | (v << 16) | (v << 8) | v;
            }
        }
        out.setRGB(0, 0, w, h, outPixels, 0, w);
        return out;
    }
}
