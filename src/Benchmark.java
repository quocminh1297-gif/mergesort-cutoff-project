import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@SuppressWarnings("rawtypes")
public class Benchmark {

    private static final Path RESULTS_DIR = Paths.get("results");

    private static final int[] CUTOFFS = {
            1, 2, 4, 6, 7, 8, 9, 10, 12, 14, 16, 20, 24, 32, 48, 64, 96, 128
    };

    private static final int[] DOUBLING_SIZES = {
            125_000, 250_000, 500_000, 1_000_000, 2_000_000
    };

    // Chong JIT loai bo ket qua tinh toan (Dead Code Elimination)
    private static volatile long blackhole = 0L;

    private static class Stats {
        final double mean;
        final double median;
        final double stdDev;
        final double ci95;
        final double min;
        final double max;

        Stats(double[] samples) {
            int n = samples.length;
            double[] sorted = Arrays.copyOf(samples, n);
            Arrays.sort(sorted);

            this.min = sorted[0];
            this.max = sorted[n - 1];
            this.median = (n % 2 == 1)
                    ? sorted[n / 2]
                    : (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;

            double sum = 0.0;
            for (double v : samples) sum += v;
            this.mean = sum / n;

            if (n > 1) {
                double sq = 0.0;
                for (double v : samples) {
                    double d = v - this.mean;
                    sq += d * d;
                }
                this.stdDev = Math.sqrt(sq / (n - 1));
                this.ci95 = tCritical95(n - 1) * this.stdDev / Math.sqrt(n);
            } else {
                this.stdDev = 0.0;
                this.ci95 = 0.0;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        boolean quickMode = false;
        for (String arg : args) {
            if ("--quick".equalsIgnoreCase(arg)) {
                quickMode = true;
            }
        }

        int warmupRounds = quickMode ? 2 : 5;
        int trialsExp1 = quickMode ? 3 : 15;
        int trialsExp2 = quickMode ? 3 : 15;
        int trialsExp3 = quickMode ? 3 : 11;

        Files.createDirectories(RESULTS_DIR);
        DatasetGenerator.generateAllIfNeeded(false);

        writeEnvironmentInfo(quickMode, warmupRounds, trialsExp1, trialsExp2, trialsExp3);

        System.out.println("=== BAT DAU BENCHMARK (" + (quickMode ? "QUICK MODE" : "FULL MODE") + ") ===");
        long totalStart = System.nanoTime();

        // Warm-up chung cho JIT compiler
        System.out.println("[0/3] Dang warm-up JVM...");
        Integer[] warmupData = DatasetGenerator.generateRandomInts(100_000, new Random(999));
        runWarmup(warmupData, warmupRounds);

        // Exp 1: Quet CUTOFF tren 5 phan bo du lieu (N = 500,000)
        System.out.println("\n[1/3] Chay Exp 1: Quet CUTOFF theo 5 phan bo du lieu...");
        int bestCutoffRandom = runExp1Distributions(trialsExp1);

        // Exp 2: So sanh kieu du lieu Integer vs String (N = 200,000)
        System.out.println("\n[2/3] Chay Exp 2: So sanh Integer vs String...");
        runExp2DataTypes(trialsExp2);

        // Exp 3: Doubling Test (Kiem chung N lg N theo bai 1.3)
        System.out.println("\n[3/3] Chay Exp 3: Doubling Test (bestCutoff=" + bestCutoffRandom + ")...");
        runExp3DoublingTest(bestCutoffRandom, trialsExp3);

        double totalSec = (System.nanoTime() - totalStart) / 1e9;
        System.out.printf(Locale.US, "\n=== HOAN TAT TRONG %.1f GIAY (blackhole=%d) ===%n", totalSec, blackhole);
        System.out.println("Ket qua da luu trong thu muc: " + RESULTS_DIR.toAbsolutePath());
    }

    private static void runWarmup(Comparable[] sample, int rounds) {
        int[] warmupCutoffs = {1, 7, 10, 16, 32};
        for (int r = 0; r < rounds; r++) {
            for (int c : warmupCutoffs) {
                Comparable[] copy = Arrays.copyOf(sample, sample.length);
                Merge.sort(copy, c);
                consume(copy);
            }
        }
    }

    private static int runExp1Distributions(int trials) throws IOException {
        Map<String, Path> datasets = new LinkedHashMap<>();
        int n = DatasetGenerator.MAIN_N;
        datasets.put("random", DatasetGenerator.DATA_DIR.resolve("int_random_" + n + ".txt"));
        datasets.put("partially_sorted", DatasetGenerator.DATA_DIR.resolve("int_partially_sorted_" + n + ".txt"));
        datasets.put("reverse", DatasetGenerator.DATA_DIR.resolve("int_reverse_" + n + ".txt"));
        datasets.put("sorted", DatasetGenerator.DATA_DIR.resolve("int_sorted_" + n + ".txt"));
        datasets.put("few_unique", DatasetGenerator.DATA_DIR.resolve("int_few_unique_" + n + ".txt"));

        Path rawCsv = RESULTS_DIR.resolve("exp1_cutoff_raw.csv");
        Path summaryCsv = RESULTS_DIR.resolve("exp1_cutoff_summary.csv");

        int bestCutoffForRandom = 10;

        try (BufferedWriter rawOut = Files.newBufferedWriter(rawCsv, StandardCharsets.UTF_8);
             BufferedWriter sumOut = Files.newBufferedWriter(summaryCsv, StandardCharsets.UTF_8)) {

            rawOut.write("distribution,n,cutoff,trial,time_ms\n");
            sumOut.write("distribution,n,cutoff,trials,median_ms,mean_ms,stddev_ms,ci95_ms,min_ms,max_ms,speedup_vs_pure_pct\n");

            for (Map.Entry<String, Path> entry : datasets.entrySet()) {
                String distName = entry.getKey();
                Integer[] source = DatasetGenerator.readIntArray(entry.getValue());
                System.out.println("  -> Phan bo: " + distName + " (N=" + source.length + ")");

                // Warm-up nhe cho phan bo hien tai
                runWarmup(Arrays.copyOf(source, Math.min(source.length, 50_000)), 2);
                System.gc();

                Map<Integer, double[]> rawSamples = measureCutoffsInterleaved(source, CUTOFFS, trials, 1001L + distName.hashCode());

                // Ghi du lieu tho
                for (int c : CUTOFFS) {
                    double[] times = rawSamples.get(c);
                    for (int t = 0; t < times.length; t++) {
                        rawOut.write(String.format(Locale.US, "%s,%d,%d,%d,%.4f%n",
                                distName, source.length, c, t + 1, times[t]));
                    }
                }

                // Tinh thong ke & tim toi uu
                Map<Integer, Stats> statsMap = new LinkedHashMap<>();
                for (int c : CUTOFFS) {
                    statsMap.put(c, new Stats(rawSamples.get(c)));
                }
                double baselineMedian = statsMap.get(1).median;

                int bestC = 1;
                double bestMed = Double.POSITIVE_INFINITY;

                for (int c : CUTOFFS) {
                    Stats s = statsMap.get(c);
                    double speedup = (baselineMedian - s.median) / baselineMedian * 100.0;
                    sumOut.write(String.format(Locale.US,
                            "%s,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f%n",
                            distName, source.length, c, trials,
                            s.median, s.mean, s.stdDev, s.ci95, s.min, s.max, speedup));

                    if (s.median < bestMed) {
                        bestMed = s.median;
                        bestC = c;
                    }
                }

                double bestSpeedup = (baselineMedian - bestMed) / baselineMedian * 100.0;
                System.out.printf(Locale.US,
                        "     Pure(1)=%.2f ms | Best CUTOFF=%d (%.2f ms, cai thien %.2f%%) | C7=%.2f ms | C10=%.2f ms%n",
                        baselineMedian, bestC, bestMed, bestSpeedup,
                        statsMap.get(7).median, statsMap.get(10).median);

                if ("random".equals(distName)) {
                    bestCutoffForRandom = bestC;
                }
            }
        }
        return bestCutoffForRandom;
    }

    private static void runExp2DataTypes(int trials) throws IOException {
        int n = DatasetGenerator.STRING_N;
        Integer[] intSource = DatasetGenerator.readIntArray(
                DatasetGenerator.DATA_DIR.resolve("int_random_" + n + ".txt"));
        String[] strSource = DatasetGenerator.readStringArray(
                DatasetGenerator.DATA_DIR.resolve("str_random_" + n + ".txt"));

        Map<String, Comparable[]> sources = new LinkedHashMap<>();
        sources.put("Integer", intSource);
        sources.put("String", strSource);

        Path summaryCsv = RESULTS_DIR.resolve("exp2_datatype_summary.csv");

        try (BufferedWriter sumOut = Files.newBufferedWriter(summaryCsv, StandardCharsets.UTF_8)) {
            sumOut.write("datatype,n,cutoff,trials,median_ms,mean_ms,stddev_ms,ci95_ms,speedup_vs_pure_pct\n");

            for (Map.Entry<String, Comparable[]> entry : sources.entrySet()) {
                String typeName = entry.getKey();
                Comparable[] source = entry.getValue();
                System.out.println("  -> Kieu du lieu: " + typeName + " (N=" + source.length + ")");

                runWarmup(Arrays.copyOf(source, Math.min(source.length, 40_000)), 3);
                System.gc();

                Map<Integer, double[]> rawSamples = measureCutoffsInterleaved(source, CUTOFFS, trials, 2002L + typeName.hashCode());
                Map<Integer, Stats> statsMap = new LinkedHashMap<>();
                for (int c : CUTOFFS) {
                    statsMap.put(c, new Stats(rawSamples.get(c)));
                }
                double baselineMedian = statsMap.get(1).median;

                int bestC = 1;
                double bestMed = Double.POSITIVE_INFINITY;

                for (int c : CUTOFFS) {
                    Stats s = statsMap.get(c);
                    double speedup = (baselineMedian - s.median) / baselineMedian * 100.0;
                    sumOut.write(String.format(Locale.US,
                            "%s,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.2f%n",
                            typeName, source.length, c, trials,
                            s.median, s.mean, s.stdDev, s.ci95, speedup));
                    if (s.median < bestMed) {
                        bestMed = s.median;
                        bestC = c;
                    }
                }
                System.out.printf(Locale.US,
                        "     Pure(1)=%.2f ms | Best CUTOFF=%d (%.2f ms) | C7=%.2f ms | C10=%.2f ms%n",
                        baselineMedian, bestC, bestMed,
                        statsMap.get(7).median, statsMap.get(10).median);
            }
        }
    }

    private static void runExp3DoublingTest(int bestCutoff, int trials) throws IOException {
        Set<Integer> targetSet = new LinkedHashSet<>();
        targetSet.add(1);
        targetSet.add(7);
        targetSet.add(10);
        targetSet.add(bestCutoff);

        int[] targetCutoffs = targetSet.stream().mapToInt(Integer::intValue).toArray();
        Path summaryCsv = RESULTS_DIR.resolve("exp3_doubling_summary.csv");

        // Luu median cua kich thuoc truoc do de tinh ty le nhan doi T(N)/T(N/2)
        Map<Integer, Double> prevMedians = new LinkedHashMap<>();

        try (BufferedWriter sumOut = Files.newBufferedWriter(summaryCsv, StandardCharsets.UTF_8)) {
            sumOut.write("n,cutoff,trials,median_ms,mean_ms,stddev_ms,doubling_ratio,log2_ratio,speedup_vs_pure_pct\n");

            for (int n : DOUBLING_SIZES) {
                Integer[] source = DatasetGenerator.generateRandomInts(n, new Random(DatasetGenerator.SEED + 100 + n));
                System.gc();

                Map<Integer, double[]> rawSamples = measureCutoffsInterleaved(source, targetCutoffs, trials, 3003L + n);
                Map<Integer, Stats> statsMap = new LinkedHashMap<>();
                for (int c : targetCutoffs) {
                    statsMap.put(c, new Stats(rawSamples.get(c)));
                }

                double baselineMedian = statsMap.get(1).median;
                System.out.printf(Locale.US, "  -> N=%7d:", n);

                for (int c : targetCutoffs) {
                    Stats s = statsMap.get(c);
                    double ratio = Double.NaN;
                    double log2Ratio = Double.NaN;
                    if (prevMedians.containsKey(c)) {
                        ratio = s.median / prevMedians.get(c);
                        log2Ratio = Math.log(ratio) / Math.log(2.0);
                    }
                    prevMedians.put(c, s.median);

                    double speedup = (baselineMedian - s.median) / baselineMedian * 100.0;
                    sumOut.write(String.format(Locale.US,
                            "%d,%d,%d,%.4f,%.4f,%.4f,%s,%s,%.2f%n",
                            n, c, trials, s.median, s.mean, s.stdDev,
                            Double.isNaN(ratio) ? "" : String.format(Locale.US, "%.4f", ratio),
                            Double.isNaN(log2Ratio) ? "" : String.format(Locale.US, "%.4f", log2Ratio),
                            speedup));

                    System.out.printf(Locale.US, "  C%d=%.2fms", c, s.median);
                }
                System.out.println();
            }
        }
    }

    // Chay dan xen tung vong trial va xao tron thu tu cutoff de chong lech nhiet/tai nen
    private static Map<Integer, double[]> measureCutoffsInterleaved(
            Comparable[] source, int[] cutoffs, int trials, long shuffleSeed) {

        Map<Integer, double[]> results = new LinkedHashMap<>();
        List<Integer> order = new ArrayList<>(cutoffs.length);
        for (int c : cutoffs) {
            results.put(c, new double[trials]);
            order.add(c);
        }

        Random rng = new Random(shuffleSeed);
        for (int t = 0; t < trials; t++) {
            Collections.shuffle(order, rng);
            for (int c : order) {
                Comparable[] work = Arrays.copyOf(source, source.length);

                long t0 = System.nanoTime();
                Merge.sort(work, c);
                long elapsedNs = System.nanoTime() - t0;

                // Kiem tra tinh dung dan ngoai khoang bam gio
                if (t == 0 && !Merge.isSorted(work)) {
                    throw new AssertionError("Mang chua duoc sap xep dung o cutoff=" + c);
                }
                consume(work);
                results.get(c)[t] = elapsedNs / 1_000_000.0;
            }
        }
        return results;
    }

    private static void consume(Comparable[] a) {
        if (a.length > 0) {
            blackhole ^= (long) a[0].hashCode() * 31L + a[a.length - 1].hashCode();
        }
    }

    // Gia tri toi han phan phoi Student-t (2 phia, alpha = 0.05) cho khoang tin cay 95%
    private static double tCritical95(int df) {
        double[] table = {
                12.706, 4.303, 3.182, 2.776, 2.571, // df 1..5
                2.447,  2.365, 2.306, 2.262, 2.228, // df 6..10
                2.201,  2.179, 2.160, 2.145, 2.131, // df 11..15
                2.120,  2.110, 2.101, 2.093, 2.086, // df 16..20
                2.080,  2.074, 2.069, 2.064, 2.060, // df 21..25
                2.056,  2.052, 2.048, 2.045, 2.042  // df 26..30
        };
        if (df <= 0) return 1.96;
        if (df <= table.length) return table[df - 1];
        return 1.96;
    }

    private static void writeEnvironmentInfo(
            boolean quickMode, int warmup, int t1, int t2, int t3) throws IOException {
        Path envFile = RESULTS_DIR.resolve("environment.txt");
        Runtime rt = Runtime.getRuntime();
        List<String> jvmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder sb = new StringBuilder();
        sb.append("Timestamp       : ").append(now).append("\n");
        sb.append("Mode            : ").append(quickMode ? "QUICK" : "FULL").append("\n");
        sb.append("Warmup rounds   : ").append(warmup).append("\n");
        sb.append("Trials (E1/E2/E3): ").append(t1).append(" / ").append(t2).append(" / ").append(t3).append("\n");
        sb.append("OS              : ").append(System.getProperty("os.name")).append(" ")
                .append(System.getProperty("os.version")).append(" (")
                .append(System.getProperty("os.arch")).append(")\n");
        sb.append("Logical CPUs    : ").append(rt.availableProcessors()).append("\n");
        sb.append("Java Version    : ").append(System.getProperty("java.version")).append(" (")
                .append(System.getProperty("java.vendor")).append(")\n");
        sb.append("JVM Name        : ").append(System.getProperty("java.vm.name")).append(" ")
                .append(System.getProperty("java.vm.version")).append("\n");
        sb.append("Max Heap (MB)   : ").append(rt.maxMemory() / (1024 * 1024)).append("\n");
        sb.append("JVM Flags       : ").append(jvmArgs).append("\n");

        Files.writeString(envFile, sb.toString(), StandardCharsets.UTF_8);
        System.out.println(sb);
    }
}