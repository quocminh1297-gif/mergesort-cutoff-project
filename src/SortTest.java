import java.util.Arrays;
import java.util.Random;

public class SortTest {

    // Lớp phụ trợ để kiểm tra tính ổn định (stability)
    private static class Item implements Comparable<Item> {
        final int key;
        final int originalIndex;

        Item(int key, int originalIndex) {
            this.key = key;
            this.originalIndex = originalIndex;
        }

        @Override
        public int compareTo(Item other) {
            return Integer.compare(this.key, other.key);
        }
    }

    public static void main(String[] args) {
        int[] cutoffs = {0, 1, 2, 5, 7, 10, 12, 16, 32, 100};
        int[] sizes = {0, 1, 2, 3, 7, 10, 15, 16, 100, 1001, 5000};
        Random rng = new Random(42);

        verifyEdgeCases(rng);
        verifyInsertionSubarray(rng);
        verifyInsertionStability(300, rng);

        for (int cutoff : cutoffs) {
            for (int n : sizes) {
                verify("Random", generateRandom(n, rng), cutoff);
                verify("Sorted", generateSorted(n), cutoff);
                verify("Reverse", generateReverse(n), cutoff);
                verify("PartiallySorted", generatePartiallySorted(n, rng), cutoff);
                verify("AllEqual", generateAllEqual(n), cutoff);
                verify("FewUnique", generateFewUnique(n, rng), cutoff);
            }
            verifyStability(1000, cutoff, rng);
        }

        System.out.println("ALL TESTS PASSED!");
    }

    // Kiểm tra các trường hợp biên: null, mảng rỗng, 1 phần tử và hàm sort mặc định
    private static void verifyEdgeCases(Random rng) {
        Merge.sort(null);
        Merge.sort(null, 10);
        Insertion.sort(null);

        Integer[] empty = new Integer[0];
        Merge.sort(empty);
        Insertion.sort(empty);

        Integer[] single = new Integer[]{42};
        Merge.sort(single);
        Insertion.sort(single);
        if (!single[0].equals(42)) {
            throw new AssertionError("Failed single-element test");
        }

        // Kiểm tra overload Merge.sort(Comparable[] a) dùng DEFAULT_CUTOFF
        Integer[] sample = generateRandom(257, rng);
        Integer[] expected = Arrays.copyOf(sample, sample.length);
        Merge.sort(sample);
        Arrays.sort(expected);
        if (!Arrays.equals(sample, expected) || !Merge.isSorted(sample)) {
            throw new AssertionError("Failed Merge.sort(a) default cutoff test");
        }

        // Kiểm tra trực tiếp Insertion.sort(Comparable[] a)
        Integer[] insSample = generateRandom(200, rng);
        Integer[] insExpected = Arrays.copyOf(insSample, insSample.length);
        Insertion.sort(insSample);
        Arrays.sort(insExpected);
        if (!Arrays.equals(insSample, insExpected) || !Merge.isSorted(insSample)) {
            throw new AssertionError("Failed Insertion.sort(a) full-array test");
        }
    }

    // Kiểm tra Insertion.sort(a, lo, hi) chỉ sắp xếp đoạn [lo..hi] và giữ nguyên phần tử ngoài đoạn
    private static void verifyInsertionSubarray(Random rng) {
        int n = 60;
        int[][] ranges = {
                {10, 25}, // đoạn ở giữa
                {0, 12},  // đoạn ở đầu
                {45, 59}, // đoạn ở cuối
                {15, 15}, // đoạn 1 phần tử (lo == hi)
                {20, 18}  // đoạn rỗng ngược (lo > hi)
        };

        for (int[] range : ranges) {
            int lo = range[0];
            int hi = range[1];
            Integer[] original = generateRandom(n, rng);
            Integer[] actual = Arrays.copyOf(original, n);
            Integer[] expected = Arrays.copyOf(original, n);

            Insertion.sort(actual, lo, hi);
            if (lo <= hi) {
                Arrays.sort(expected, lo, hi + 1); // Arrays.sort dùng cận trên exclusive
            }

            if (!Arrays.equals(actual, expected)) {
                throw new AssertionError("Failed Insertion.sort subarray [" + lo + ".." + hi + "]");
            }
        }
    }

    // Kiểm tra tính ổn định của Insertion.sort trên đoạn con [lo..hi]
    private static void verifyInsertionStability(int n, Random rng) {
        Item[] arr = new Item[n];
        for (int i = 0; i < n; i++) {
            arr[i] = new Item(rng.nextInt(10), i);
        }

        int lo = 30, hi = 220;
        Insertion.sort(arr, lo, hi);

        for (int i = lo + 1; i <= hi; i++) {
            if (arr[i].key < arr[i - 1].key) {
                throw new AssertionError("Insertion.sort subarray not sorted");
            }
            if (arr[i].key == arr[i - 1].key && arr[i].originalIndex < arr[i - 1].originalIndex) {
                throw new AssertionError("Insertion.sort subarray is not stable");
            }
        }
    }

    private static void verify(String caseName, Integer[] input, int cutoff) {
        Integer[] actual = Arrays.copyOf(input, input.length);
        Integer[] expected = Arrays.copyOf(input, input.length);

        Merge.sort(actual, cutoff);
        Arrays.sort(expected);

        if (!Arrays.equals(actual, expected) || !Merge.isSorted(actual)) {
            throw new AssertionError(
                    "Failed case=" + caseName + ", n=" + input.length + ", cutoff=" + cutoff
            );
        }
    }

    private static void verifyStability(int n, int cutoff, Random rng) {
        Item[] arr = new Item[n];
        for (int i = 0; i < n; i++) {
            arr[i] = new Item(rng.nextInt(10), i); // nhiều khóa trùng nhau
        }

        Merge.sort(arr, cutoff);

        for (int i = 1; i < n; i++) {
            if (arr[i].key < arr[i - 1].key) {
                throw new AssertionError("Not sorted in stability test, cutoff=" + cutoff);
            }
            if (arr[i].key == arr[i - 1].key && arr[i].originalIndex < arr[i - 1].originalIndex) {
                throw new AssertionError("Not stable at cutoff=" + cutoff);
            }
        }
    }

    private static Integer[] generateRandom(int n, Random rng) {
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) a[i] = rng.nextInt();
        return a;
    }

    private static Integer[] generateSorted(int n) {
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) a[i] = i;
        return a;
    }

    private static Integer[] generateReverse(int n) {
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) a[i] = n - i;
        return a;
    }

    // Mảng gần như đã sắp xếp (số nghịch thế tuyến tính O(n))
    private static Integer[] generatePartiallySorted(int n, Random rng) {
        Integer[] a = generateSorted(n);
        if (n <= 1) return a;
        int swaps = Math.max(1, n / 10);
        int window = 8;
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

    private static Integer[] generateAllEqual(int n) {
        Integer[] a = new Integer[n];
        Arrays.fill(a, 7);
        return a;
    }

    private static Integer[] generateFewUnique(int n, Random rng) {
        Integer[] a = new Integer[n];
        for (int i = 0; i < n; i++) a[i] = rng.nextInt(5);
        return a;
    }
}