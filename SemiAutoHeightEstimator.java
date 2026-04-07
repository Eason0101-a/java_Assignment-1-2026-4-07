import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class SemiAutoHeightEstimator {
    private static final double REFERENCE_HEIGHT_CM = 180.0;

    private enum ClickPurpose {
        HORIZON_A,
        HORIZON_B,
        REF_HEAD,
        REF_FOOT,
        TARGET_HEAD,
        TARGET_FOOT
    }

    private record PixelPoint(double x, double y) {}

    private record Person(String name, PixelPoint head, PixelPoint foot) {}

    private static class ClickCollector {
        private final BufferedImage image;
        private final String title;
        private final List<PixelPoint> clickedPoints = new ArrayList<>();
        private final List<ClickPurpose> purposes;

        ClickCollector(BufferedImage image, String title, List<ClickPurpose> purposes) {
            this.image = image;
            this.title = title;
            this.purposes = purposes;
        }

        List<PixelPoint> collect() {
            final Object lock = new Object();
            AtomicReference<JFrame> frameRef = new AtomicReference<>();

            Runnable createUi = () -> {
                JFrame frame = new JFrame(title);
                ImagePanel panel = new ImagePanel(image, clickedPoints, purposes);

                panel.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (clickedPoints.size() >= purposes.size()) {
                            return;
                        }
                        clickedPoints.add(new PixelPoint(e.getX(), e.getY()));
                        panel.repaint();

                        int idx = clickedPoints.size() - 1;
                        ClickPurpose purpose = purposes.get(idx);
                        System.out.printf("  已記錄 %-11s -> (%.0f, %.0f)%n", purpose.name(), e.getX() * 1.0, e.getY() * 1.0);

                        if (clickedPoints.size() == purposes.size()) {
                            synchronized (lock) {
                                lock.notifyAll();
                            }
                        }
                    }
                });

                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent e) {
                        synchronized (lock) {
                            lock.notifyAll();
                        }
                    }
                });

                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                frame.setContentPane(new JScrollPane(panel));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                frameRef.set(frame);
            };

            try {
                if (SwingUtilities.isEventDispatchThread()) {
                    createUi.run();
                } else {
                    SwingUtilities.invokeAndWait(createUi);
                }
            } catch (Exception ex) {
                return new ArrayList<>();
            }

            synchronized (lock) {
                while (clickedPoints.size() < purposes.size()) {
                    JFrame frame = frameRef.get();
                    if (frame == null || !frame.isDisplayable()) {
                        break;
                    }
                    try {
                        lock.wait();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            JFrame frame = frameRef.get();
            if (frame != null && frame.isDisplayable()) {
                SwingUtilities.invokeLater(frame::dispose);
            }
            return new ArrayList<>(clickedPoints);
        }
    }

    private static class ImagePanel extends JPanel {
        private final BufferedImage image;
        private final List<PixelPoint> clickedPoints;
        private final List<ClickPurpose> purposes;

        ImagePanel(BufferedImage image, List<PixelPoint> clickedPoints, List<ClickPurpose> purposes) {
            this.image = image;
            this.clickedPoints = clickedPoints;
            this.purposes = purposes;
            setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(image, 0, 0, null);

            for (int i = 0; i < clickedPoints.size(); i++) {
                PixelPoint p = clickedPoints.get(i);
                ClickPurpose purpose = purposes.get(i);
                drawPoint(g2, p, colorFor(purpose));
            }

            if (clickedPoints.size() >= 2) {
                PixelPoint a = clickedPoints.get(0);
                PixelPoint b = clickedPoints.get(1);
                g2.setColor(Color.ORANGE);
                g2.setStroke(new BasicStroke(2f));
                g2.draw(new Line2D.Double(a.x, a.y, b.x, b.y));
            }

            g2.dispose();
        }

        private static void drawPoint(Graphics2D g2, PixelPoint p, Color color) {
            int r = 5;
            g2.setColor(color);
            g2.fillOval((int) Math.round(p.x) - r, (int) Math.round(p.y) - r, 2 * r, 2 * r);
        }

        private static Color colorFor(ClickPurpose purpose) {
            return switch (purpose) {
                case HORIZON_A, HORIZON_B -> Color.ORANGE;
                case REF_HEAD, REF_FOOT -> Color.CYAN;
                case TARGET_HEAD, TARGET_FOOT -> Color.GREEN;
            };
        }
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== 半自動 Vanishing Line 身高估測工具 ===");
        System.out.println("流程: 每張圖用滑鼠點選關鍵點，基準同學固定 180 cm\n");

        int photoCount = readInt(scanner, "請輸入照片數量 (例如 4): ");

        for (int i = 1; i <= photoCount; i++) {
            System.out.println("\n--- 照片 " + i + " ---");
            String imagePath = readLine(scanner, "請輸入圖片檔案路徑: ");
            BufferedImage image = readImage(imagePath);
            if (image == null) {
                System.out.println("  [錯誤] 無法讀取圖片: " + imagePath);
                System.out.println("  請確認路徑正確，或直接輸入檔名（例如 pic3.jpg）。");
                continue;
            }

            int targetCount = readInt(scanner, "此照片要測量幾位目標同學: ");
            List<String> targetNames = new ArrayList<>();
            for (int t = 1; t <= targetCount; t++) {
                targetNames.add(readLine(scanner, "目標同學 " + t + " 名稱: "));
            }

            List<ClickPurpose> purposes = new ArrayList<>();
            purposes.add(ClickPurpose.HORIZON_A);
            purposes.add(ClickPurpose.HORIZON_B);
            purposes.add(ClickPurpose.REF_HEAD);
            purposes.add(ClickPurpose.REF_FOOT);
            for (int t = 0; t < targetCount; t++) {
                purposes.add(ClickPurpose.TARGET_HEAD);
                purposes.add(ClickPurpose.TARGET_FOOT);
            }

            printGuideMessage(targetNames);
            System.out.println("正在開啟圖片視窗，請在圖片上依序點選...");
            List<PixelPoint> points = collectPointsOnEdt(image, "點選: " + new File(imagePath).getName(), purposes);
            if (points.size() != purposes.size()) {
                System.out.println("  [錯誤] 點選未完成，略過此張。");
                continue;
            }

            PixelPoint horizonA = points.get(0);
            PixelPoint horizonB = points.get(1);
            Person reference = new Person("reference-180cm", points.get(2), points.get(3));

            double refRatio = projectedHeight(reference, horizonA, horizonB);
            if (Math.abs(refRatio) < 1e-9) {
                System.out.println("  [錯誤] 基準資料無法計算 (可能腳點太接近 horizon)");
                continue;
            }

            System.out.println("\n照片結果: " + new File(imagePath).getName());
            int base = 4;
            for (int t = 0; t < targetCount; t++) {
                PixelPoint head = points.get(base + t * 2);
                PixelPoint foot = points.get(base + t * 2 + 1);
                Person target = new Person(targetNames.get(t), head, foot);
                double targetRatio = projectedHeight(target, horizonA, horizonB);
                double estimated = REFERENCE_HEIGHT_CM * (targetRatio / refRatio);
                System.out.printf("  %-20s -> %.2f cm%n", target.name, estimated);
            }
        }

        System.out.println("\n全部完成。");
        scanner.close();
    }

    private static List<PixelPoint> collectPointsOnEdt(BufferedImage image, String title, List<ClickPurpose> purposes) {
        ClickCollector collector = new ClickCollector(image, title, purposes);
        return collector.collect();
    }

    private static void printGuideMessage(List<String> targetNames) {
        StringBuilder sb = new StringBuilder();
        sb.append("請依序點選:\n");
        sb.append("1) Horizon 點 A\n");
        sb.append("2) Horizon 點 B\n");
        sb.append("3) 基準同學頭頂 (Just do it)\n");
        sb.append("4) 基準同學腳底\n");
        int idx = 5;
        for (String name : targetNames) {
            sb.append(idx++).append(") ").append(name).append(" 頭頂\n");
            sb.append(idx++).append(") ").append(name).append(" 腳底\n");
        }
        sb.append("關閉圖片視窗會中止此張照片。\n");
        System.out.println(sb);
    }

    private static BufferedImage readImage(String path) {
        try {
            return ImageIO.read(new File(path));
        } catch (IOException ex) {
            return null;
        }
    }

    private static double projectedHeight(Person person, PixelPoint horizonA, PixelPoint horizonB) {
        double pixelHeight = Math.abs(person.foot.y - person.head.y);
        double horizonYAtFootX = yOnLine(horizonA, horizonB, person.foot.x);
        double footToHorizon = Math.abs(person.foot.y - horizonYAtFootX);
        if (footToHorizon < 1e-9) {
            return 0.0;
        }
        return pixelHeight / footToHorizon;
    }

    private static double yOnLine(PixelPoint a, PixelPoint b, double x) {
        double dx = b.x - a.x;
        if (Math.abs(dx) < 1e-9) {
            return (a.y + b.y) / 2.0;
        }
        double slope = (b.y - a.y) / dx;
        return a.y + slope * (x - a.x);
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            String line = readLine(scanner, prompt);
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException ex) {
                System.out.println("請輸入整數。");
            }
        }
    }

    private static String readLine(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String line = scanner.nextLine().trim();
        return line.isEmpty() ? "unnamed" : line;
    }
}
