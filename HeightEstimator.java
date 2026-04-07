import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class HeightEstimator {
    private static final double REFERENCE_HEIGHT_CM = 180.0;

    private record Point(double x, double y) {}

    private record Person(String name, Point foot, Point head) {}

    private static class PhotoMeasurement {
        final String photoName;
        final Point horizonA;
        final Point horizonB;
        final Person reference;
        final List<Person> targets;

        PhotoMeasurement(String photoName, Point horizonA, Point horizonB, Person reference, List<Person> targets) {
            this.photoName = photoName;
            this.horizonA = horizonA;
            this.horizonB = horizonB;
            this.reference = reference;
            this.targets = targets;
        }
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.US);
        Scanner scanner = new Scanner(System.in);

        System.out.println("=== Vanishing Line 身高估測工具 (Java) ===");
        System.out.println("基準同學: Just do it 衣服, 身高 180 公分");
        System.out.println("請由照片讀取像素座標，格式範例: x y");
        System.out.println();

        int photoCount = readInt(scanner, "請輸入照片數量 (例如 4): ");
        List<PhotoMeasurement> allPhotos = new ArrayList<>();

        for (int i = 1; i <= photoCount; i++) {
            System.out.println();
            System.out.println("--- 照片 " + i + " ---");
            System.out.print("照片名稱: ");
            String photoName = scanner.nextLine().trim();
            if (photoName.isEmpty()) {
                photoName = "photo-" + i;
            }

            System.out.println("請輸入 vanishing line (地平線) 上兩點座標:");
            Point horizonA = readPoint(scanner, "horizon point A (x y): ");
            Point horizonB = readPoint(scanner, "horizon point B (x y): ");

            System.out.println("\n請輸入基準同學座標 (Just do it, 180cm):");
            Point refFoot = readPoint(scanner, "reference foot (x y): ");
            Point refHead = readPoint(scanner, "reference head (x y): ");
            Person reference = new Person("reference-180cm", refFoot, refHead);

            int targetCount = readInt(scanner, "此照片要測量幾位目標同學: ");
            List<Person> targets = new ArrayList<>();
            for (int t = 1; t <= targetCount; t++) {
                System.out.println("\n目標同學 " + t + ":");
                System.out.print("名稱: ");
                String name = scanner.nextLine().trim();
                if (name.isEmpty()) {
                    name = "student-" + t;
                }
                Point foot = readPoint(scanner, "foot (x y): ");
                Point head = readPoint(scanner, "head (x y): ");
                targets.add(new Person(name, foot, head));
            }

            allPhotos.add(new PhotoMeasurement(photoName, horizonA, horizonB, reference, targets));
        }

        System.out.println();
        System.out.println("=== 計算結果 ===");
        for (PhotoMeasurement photo : allPhotos) {
            System.out.println("\n照片: " + photo.photoName);
            double refProjectedHeight = projectedHeight(photo.reference, photo.horizonA, photo.horizonB);
            if (Math.abs(refProjectedHeight) < 1e-9) {
                System.out.println("  [錯誤] 基準資料無法計算 (可能腳點過於接近 vanishing line)");
                continue;
            }

            for (Person target : photo.targets) {
                double targetProjectedHeight = projectedHeight(target, photo.horizonA, photo.horizonB);
                double estimatedHeight = REFERENCE_HEIGHT_CM * (targetProjectedHeight / refProjectedHeight);
                System.out.printf("  %-20s -> %.2f cm%n", target.name, estimatedHeight);
            }
        }

        System.out.println("\n完成。若要提高準確度，請確保頭頂/腳底點與地平線標註一致。\n");
        scanner.close();
    }

    private static double projectedHeight(Person person, Point horizonA, Point horizonB) {
        double pixelHeight = Math.abs(person.foot.y - person.head.y);
        double horizonYAtFootX = yOnLine(horizonA, horizonB, person.foot.x);
        double footToHorizon = Math.abs(person.foot.y - horizonYAtFootX);

        if (footToHorizon < 1e-9) {
            return 0.0;
        }
        return pixelHeight / footToHorizon;
    }

    private static double yOnLine(Point a, Point b, double x) {
        double dx = b.x - a.x;
        if (Math.abs(dx) < 1e-9) {
            return (a.y + b.y) / 2.0;
        }
        double slope = (b.y - a.y) / dx;
        return a.y + slope * (x - a.x);
    }

    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException ex) {
                System.out.println("請輸入整數。");
            }
        }
    }

    private static Point readPoint(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            String[] parts = line.split("\\s+");
            if (parts.length != 2) {
                System.out.println("格式錯誤，請輸入: x y");
                continue;
            }
            try {
                double x = Double.parseDouble(parts[0]);
                double y = Double.parseDouble(parts[1]);
                return new Point(x, y);
            } catch (NumberFormatException ex) {
                System.out.println("座標必須是數字，請重新輸入。");
            }
        }
    }
}
