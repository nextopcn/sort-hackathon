package cn.nextop;

import java.util.Comparator;
import java.util.List;

import static java.lang.Integer.numberOfLeadingZeros;
import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.lang.reflect.Array.newInstance;

/**
 * Modify based on openjdk official code of jdk-23+3 tag
 * https://github.com/openjdk/jdk/blob/jdk-23%2B3/src/java.base/share/classes/java/util/TimSort.java
 * @author liuqian
 */
public class TimArrSort<T> {

    //
    private int stackSize, tmpLen, length;
    private final Comparator<? super T> c;
    private final int[] runLen, runBase, lens;
    private T[] tmp; private final List<T[]> a;
    private int row, col, index; /* for read */
    private int row2, col2, index2; /* for write */
    private int row3, col3, index3; /* for cache */
    private boolean hasEmpty; /* exists empty arr */
    private static final int INITIAL_TMP_LENGTH = 256;

    private TimArrSort(List<T[]> a, Comparator<? super T> c) {
        this.a = a; this.c = c; this.lens = new int[a.size()]; this.init();
        /* Allocate temp storage (which may be increased later if necessary) */
        int l = INITIAL_TMP_LENGTH; int t = (length < 2 * l) ? length >>> 1 : l;
        this.tmp = (T[]) newInstance(a.get(0).getClass().getComponentType(), t);
        int sl = (length < 120 ? 5 : length < 1542 ? 10 : length < 119151 ? 24 : 49);
        this.tmpLen = t; this.runBase = new int[sl]; runLen = new int[sl]; /* run! */
    }

    private void init() {
        int i = 0; for (T[] t : a) {
            final int l = t.length;
            length += l; lens[i++] = length;
            if(l == 0) this.hasEmpty = true;
        } if (this.hasEmpty) this.initLoc();
    }

    /**
     * Sorts the given range
     */
    public static <T> void sort(List<T[]> a, Comparator<? super T> c) {
        if(a == null || a.size() == 0) return;

        //
        var ts = new TimArrSort(a, c);
        int base = 0; for (T[] t : a) {
            int l = t.length; if (l == 0) continue;
            int rl = countRunAndMakeAscending(t, 0, l, c);
            if (rl < l) { binarySort(t, 0, l, rl, c); rl = l; }
            ts.pushRun(base, rl); ts.mergeCollapse(); base += rl;
        }
        ts.mergeForceCollapse();
    }

    private static <T> void binarySort(T[] a, int lo, int hi, int start, Comparator<? super T> c) {
        assert lo <= start && start <= hi;
        if (start == lo) start++;
        for (; start < hi; start++) {
            int left = lo;
            T p = a[start];
            int right = start;
            assert left <= right;
            while (left < right) {
                int m = (left + right) >>> 1;
                var b = c.compare(p, a[m]) < 0;
                if (b) right = m; else left = m + 1;
            }

            //
            assert left == right;
            int n = start - left; switch (n) {
                case 2: a[left + 2] = a[left + 1];
                case 1: a[left + 1] = a[left]; break;
                default: arraycopy(a, left, a, left + 1, n);
            }
            a[left] = p;
        }
    }

    private static <T> int countRunAndMakeAscending(T[] a, int lo, int hi, Comparator<? super T> c) {
        assert lo < hi; int runHi = lo + 1; if (runHi == hi) return 1;

        //
        // Find end of run, and reverse range if descending
        if (c.compare(a[runHi++], a[lo]) < 0) { // Descending
            while (runHi < hi && c.compare(a[runHi], a[runHi - 1]) < 0) runHi++;
            reverseRange(a, lo, runHi);
        } else { // Ascending
            while (runHi < hi && c.compare(a[runHi], a[runHi - 1]) >= 0) runHi++;
        }
        return runHi - lo;
    }

    private static void reverseRange(Object[] a, int lo, int hi) {
        hi--;
        while (lo < hi) {
            Object t = a[lo];
            a[lo++] = a[hi];
            a[hi--] = t;
        }
    }

    /**
     * Pushes the specified run onto the pending-run stack.
     */
    private void pushRun(int runBase, int runLen) {
        this.runBase[stackSize] = runBase;
        this.runLen[stackSize++] = runLen;
    }

    /**
     *
     */
    private void mergeCollapse() {
        while (stackSize > 1) {
            int n = stackSize - 2;
            if (n > 0 && runLen[n - 1] <= runLen[n] + runLen[n + 1] ||
                n > 1 && runLen[n - 2] <= runLen[n] + runLen[n - 1]) {
                if ( this.runLen[n - 1] < this.runLen[n + 1] ) n--;
            } else if (n < 0 || runLen[n] > runLen[n + 1]) {
                break; // Invariant is established
            }
            mergeAt(n);
        }
    }

    private void mergeForceCollapse() {
        while (stackSize > 1) {
            final int[] r = this.runLen; int n = this.stackSize - 2;
            var b = n > 0 && r[n - 1] < r[n + 1]; if(b) n--; mergeAt(n);
        }
    }

    /**
     *
     */
    private void mergeAt(int i) {
        assert stackSize >= 2; assert i >= 0;
        assert i == stackSize - 2 || i == stackSize - 3;

        //
        int len1 = runLen[i];
        int base1 = runBase[i];
        int len2 = runLen[i + 1];
        int base2 = runBase[i + 1];
        assert len1 > 0 && len2 > 0;
        assert base1 + len1 == base2;

        //
        runLen[i] = len1 + len2;
        if (i == stackSize - 3) {
            runLen[i + 1] = runLen[i + 2];
            runBase[i + 1] = runBase[i + 2];
        } stackSize--;

        //
        final boolean b = len1 > len2;
        if(b) mergeHi(base1, len1, base2, len2);
        else this.mergeLo(base1, len1, base2, len2);
    }

    private void mergeLo(int base1, int len1, int base2, int len2) {
        T[] tmp = ensureCapacity(len1);
        this.copy(base1, tmp, 0, len1);
        var c = this.c; /* performance */

        //
        locate2(base1);
        int cursor1 = 0;
        int cursor2 = base2;
        int len = base2 + len2;
        while (len1 > 0 && len2 > 0) {
            T t1 = tmp[cursor1], t2 = get(cursor2);
            if(c.compare(t1, t2) < 0) {/* t1 win */
                set(t1); cursor1++; len1--;
            } else {
                set(t2); cursor2++; len2--;
            }
            next2(1);
        }
        if(len1 > 0) copy(tmp, cursor1, len - len1, len1);
    }

    private void mergeHi(int base1, int len1, int base2, int len2) {
        T[] tmp = ensureCapacity(len2);
        this.copy(base2, tmp, 0, len2);
        var c = this.c; /* performance */
        int len = base2 + len2, cursor = len - 1;

        //
        locate2(cursor);
        int cursor2 = len2 - 1;
        int cursor1 = base2 - 1;
        while (len1 > 0 && len2 > 0) {
            T t1 = get(cursor1), t2 = tmp[cursor2];
            if(c.compare(t1, t2) > 0) {/* t1 win */
                set(t1); cursor1--; len1--;
            } else {
                set(t2); cursor2--; len2--;
            }
            prev2(1);
        } if(len2 > 0) this.copy(tmp, 0, base1, len2);
    }

    /**
     *
     */
    private T[] ensureCapacity(int min) {
        if(min <= tmpLen) return tmp; /* no need */
        // Compute smallest power of 2 > minCapacity
        int ns = -1 >>> numberOfLeadingZeros(min); ns++;
        if (ns < 0) ns = min; else ns = min(ns, length >>> 1);
        Class t = this.a.get(0).getClass().getComponentType();
        tmpLen = ns; tmp = (T[]) newInstance(t, ns); return tmp;
    }

    /**
     * Copy
     */
    private static <T> void arraycopy(T[] src, int s, T[] dest, int d, int l) {
        if(l > 12) { System.arraycopy(src, s, dest, d, l); return; }
        if(s > d) for (int i = 0; i < l; i++) dest[d + i] = src[s + i];
        else for (int i = l - 1; i >= 0; i--) dest[d + i] = src[s + i];

    }

    private void copy(int srcPos, T[] dest, int destPos, int len) {
        locate(srcPos); int srs = this.row;
        int scs = this.col; while (len > 0) {
            int l = a.get(srs).length - scs, cl = min(l, len);
            this.arraycopy(a.get(srs), scs, dest, destPos, cl);
            len -= cl; if (len == 0) return;/**/ destPos += cl;
            srs++; scs = 0; /* src array is not long enough! */
        }
    }

    private void copy(T[] src, int srcPos, int destPos, int len) {
        locate(destPos); int drs = this.row;
        int dcs = this.col; while (len > 0) {
            int adl = a.get(drs).length - dcs;
            final int cl = Math.min(adl, len);
            arraycopy(src, srcPos, a.get(drs), dcs, cl);
            len -= cl; if (len == 0) return; srcPos += cl;
            drs++; dcs = 0; /* dest array not long enough */
        }
    }

    /**
     * Array operate
     */
    private T get() { /* for read */
        return a.get(row)[col];
    }

    private T get(int i) { /* for read */
        if (i != index) locate(i);
        return get(); /* current */
    }

    private void set(T s) { /* for write */
        this.a.get(row2)[col2] = s;
    }

    private void prev(int n) { /* for read */
        int[] l = this.lens; int i = index - n;
        if (this.row > 0) if (i < l[row - 1]) {
            do { this.row--; } /* change row */
            while ( row > 0 && i < l[row - 1] );
            col = (row > 0) ? i - l[row - 1] : i;
        } else col -= n; else col = i; index = i;
    }

    private void prev2(int n) { /* for write */
        int[] l = this.lens; int i = index2 - n;
        if (this.row2 > 0) if (i < l[row2 - 1]) {
            do { this.row2--; } /* change row! */
            while ( row2 > 0 && i < l[row2 - 1] );
            col2 = (row2 > 0) ? i - l[row2 - 1] : i;
        } else col2 -= n; else col2 = i; index2 = i;
    }

    private void next(int n) { /* for read */
        int i = index + n;
        if (i >= lens[row]) {
            do { this.row++; }
            while (i >= lens[row]);
            col = i - lens[row - 1];
        } else col += n;  index = i;
    }

    private void next2(int n) { /* for write */
        int i = index2 + n;
        if (i >= lens[row2]) {
            do { this.row2++; }
            while (i >= lens[row2]);
            col2 = i - lens[row2 - 1];
        } else col2 += n;  index2 = i;
    }

    private void initLoc() {
        this.skipEmpty();
        this.row2 = row3 = row;
        this.col2 = col3 = col;
        index2 = index3 = index;
    }

    private void skipEmpty() { /* skip */
        int rs = lens.length - 1;/* last row */
        while (row < rs && lens[row] == 0) row++;
    }

    private void locate(int i) { /* for read */
        if (i == index) return;
        if (hasCache(i)) return;
        updateCache();/* cache */
        int n = abs(i - this.index);
        final boolean b = i > index;
        if (b) next(n); else prev(n);
    }

    private void locate2(int i) { /* for write */
        if (i == this.index2) return;
        int n = abs(i - this.index2);
        final boolean b = i > index2;
        if (b) next2(n); else prev2(n);
    }

    /**
     *
     */
    private boolean hasCache(int i) {
        if(index3 != i) return false;
        this.index = i; this.row = row3;
        this.col = this.col3; return true;
    }

    private void updateCache() {
        row3 = row; col3 = col;
        this.index3 = this.index;
    }
}
