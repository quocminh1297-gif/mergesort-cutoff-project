import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;

public class DatasetGenerator {

    public static final Path DATA_DIR = Paths.get("data");
    public static final int MAIN_N = 1_000_000;
    public static final int STRING_N = 300_000;
    public static final long SEED = 2026L;

    public static void main(String[] args) throws IOException {
        generateAllIfNeeded(true);
        System.out.println("Hoan tat sinh bo du lieu trong thu muc: " + DATA_DIR.toAbsolutePath());
    }

    public static void generateAllIfNeeded(boolean overwrite) throws IOException {
        Files.createDirectories(DATA_DIR);

        // 5 phan bo Integer cho Exp 1 (N = 1,000,000)
        writeIfNeeded(DATA_DIR.resolve("int_random_" + MAIN_N + ".txt"), overwrite,
                () -> generateRandomInts(MAIN_N, new Random(SEED + 1)));

        writeIfNeeded(DATA_DIR.resolve("int_partially_sorted_" + MAIN_N + ".txt"), overwrite,
                () -> generatePartiallySortedInts(MAIN_N, new Random(SEED + 2), 16, 0.10));

        writeIfNeeded(DATA_DIR.resolve("int_reverse_" + MAIN_N + ".txt"), overwrite,
                () -> generateReverseInts(MAIN_N));

        writeIfNeeded(DATA_DIR.resolve("int_sorted_" + MAIN_N + ".txt"), overwrite,
                () -> generateSortedInts(MAIN_N));

        writeIfNeeded(DATA_DIR.resolve("int_few_unique_" + MAIN_N + ".txt"), overwrite,
                () -> generateFewUniqueInts(MAIN_N, new Random(SEED + 3), 50));

        // 2 bo du lieu cho Exp 2 (so sanh Integer vs String, N = 200,000)
        writeIfNeeded(DATA_DIR.resolve("int_random_" + STRING_N + ".txt"), overwrite,
                () -> generateRandomInts(STRING_N, new Random(SEED + 4)));

        writeIfNeeded(DATA_DIR.resolve("str_random_" + STRING_N + ".txt"), overwrite,
                () -> generateRandomStrings(STRING_N, 12, new Random(SEED + 5)));
    }

    @FunctionalInterface
    private interface ArraySupplier<T> {
        T[] get();
    }

    private static <T> void writeIfNeeded(Path file, boolean overwrite, ArraySupplier<T> supplier) throws IOException {
        if (!overwrite && Files.exists(file)) {
            return;
        }
        T[] arr = supplier.get();
        writeArray(file, arr);
        System.out.println("Generated: " + file + " (" + arr.length + " items)");
    }

    public static Integer[] generateRandomInts(int n, Random rng) {
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) {
            a[i] = rng.nextInt();
        }
        return a;
    }

    public static Integer[] generateSortedInts(int n) {
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) {
            a[i] = i;
        }
        return a;
    }

    public static Integer[] generateReverseInts(int n) {
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) {
            a[i] = n - i;
        }
        return a;
    }

    // Hoan vi cuc bo trong cua so +/- window de so nghich the <= c*N (partially sorted)
    public static Integer[] generatePartiallySortedInts(int n, Random rng, int window, double swapRatio) {
        Integer[] a = generateSortedInts(n);
        int swaps = (int) (n * swapRatio);
        for (int k = 0; k < swaps; k++) {
            int i = rng.nextInt(n);
            int lo = Math.max(0, i - window);
            int hi = Math.min(n - 1, i + window);
            int j = lo + rng.nextInt(hi - lo + 1);
            Integer tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
        return a;
    }

    public static Integer[] generateFewUniqueInts(int n, Random rng, int uniqueCount) {
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) {
            a[i] = rng.nextInt(uniqueCount);
        }
        return a;
    }

    // Chuoi co tien to chung de ham String.compareTo() thuc su phai duyet nhieu ky tu
    public static String[] generateRandomStrings(int n, int randomSuffixLen, Random rng) {
        final String prefix = "UET_DSA_2026_";
        final String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
        String[] a = new String[n];
        StringBuilder sb = new StringBuilder(prefix.length() + randomSuffixLen);
        for (int i = 0; i < n; i++) {
            sb.setLength(0);
            sb.append(prefix);
            for (int c = 0; c < randomSuffixLen; c++) {
                sb.append(alphabet.charAt(rng.nextInt(alphabet.length())));
            }
            a[i] = sb.toString();
        }
        return a;
    }

    private static <T> void writeArray(Path path, T[] arr) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            bw.write(Integer.toString(arr.length));
            bw.newLine();
            for (T item : arr) {
                bw.write(item.toString());
                bw.newLine();
            }
        }
    }

    public static Integer[] readIntArray(Path path) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            int n = Integer.parseInt(br.readLine().trim());
            Integer[] a = new Integer[n];
            for (int i = 0; i < n; i++) {
                a[i] = Integer.parseInt(br.readLine().trim());
            }
            return a;
        }
    }

    public static String[] readStringArray(Path path) throws IOException {
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            int n = Integer.parseInt(br.readLine().trim());
            String[] a = new String[n];
            for (int i = 0; i < n; i++) {
                a[i] = br.readLine();
            }
            return a;
        }
    }
}