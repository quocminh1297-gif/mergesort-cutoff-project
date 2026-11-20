@SuppressWarnings({"rawtypes", "unchecked"})
public class Merge {

    private static final int DEFAULT_CUTOFF = 10;

    private static void merge(Comparable[] a, Comparable[] aux, int lo, int mid, int hi) {
        for (int k = lo; k <= hi; k++) {
            aux[k] = a[k];
        }

        int i = lo, j = mid + 1;
        for (int k = lo; k <= hi; k++) {
            if      (i > mid)              a[k] = aux[j++];
            else if (j > hi)               a[k] = aux[i++];
            else if (less(aux[j], aux[i])) a[k] = aux[j++];
            else                           a[k] = aux[i++];
        }
    }

    // Mergesort thuần túy theo slide (baseline khi cutoff <= 1)
    private static void sortPure(Comparable[] a, Comparable[] aux, int lo, int hi) {
        if (hi <= lo) return;
        int mid = lo + (hi - lo) / 2;
        sortPure(a, aux, lo, mid);
        sortPure(a, aux, mid + 1, hi);
        merge(a, aux, lo, mid, hi);
    }

    // Mergesort có cải tiến CUTOFF sang Insertion sort theo slide
    private static void sortWithCutoff(Comparable[] a, Comparable[] aux, int lo, int hi, int cutoff) {
        if (hi <= lo + cutoff - 1) {
            Insertion.sort(a, lo, hi);
            return;
        }
        int mid = lo + (hi - lo) / 2;
        sortWithCutoff(a, aux, lo, mid, cutoff);
        sortWithCutoff(a, aux, mid + 1, hi, cutoff);
        merge(a, aux, lo, mid, hi);
    }

    public static void sort(Comparable[] a) {
        sort(a, DEFAULT_CUTOFF);
    }

    public static void sort(Comparable[] a, int cutoff) {
        if (a == null || a.length <= 1) return;
        Comparable[] aux = new Comparable[a.length];
        if (cutoff <= 1) {
            sortPure(a, aux, 0, a.length - 1);
        } else {
            sortWithCutoff(a, aux, 0, a.length - 1, cutoff);
        }
    }

    private static boolean less(Comparable v, Comparable w) {
        return v.compareTo(w) < 0;
    }

    public static boolean isSorted(Comparable[] a) {
        for (int i = 1; i < a.length; i++) {
            if (less(a[i], a[i - 1])) return false;
        }
        return true;
    }
}