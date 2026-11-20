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
        int[] cutoffs = {0, 1, 2, 5, 7, 10, 16, 32, 100};
        int[] sizes = {0, 1, 2, 3, 7, 10, 15, 16, 100, 1001, 5000};
        Random rng = new Random(42);

        for (int cutoff : cutoffs) {
            for (int n : sizes) {
                verify("Random", generateRandom(n, rng), cutoff);
                verify("Sorted", generateSorted(n), cutoff);
                verify("Reverse", generateReverse(n), cutoff);
                verify("AllEqual", generateAllEqual(n), cutoff);
                verify("FewUnique", generateFewUnique(n, rng), cutoff);
            }
            verifyStability(1000, cutoff, rng);
        }

        System.out.println("ALL TESTS PASSED!");
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