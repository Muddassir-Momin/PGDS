package com.pgds.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Isolation Forest (Liu, Ting, Zhou - ICDM 2008), unsupervised anomaly detection.
 * Anomalies are isolated by random splits in fewer steps, so they have shorter average path lengths.
 * score = 2^(-E[h(x)] / c(psi)):  ~0.5 normal, towards 1.0 anomalous.
 */
public final class IsolationForest {

    private static final double EULER_GAMMA = 0.5772156649;

    private record Node(int feature, double split, Node left, Node right, int size) {
        boolean isLeaf() { return left == null; }
    }

    private final int trees;
    private final int sampleSize;
    private final long seed;
    private final List<Node> forest = new ArrayList<>();
    private int psi;

    public IsolationForest(int trees, int sampleSize, long seed) {
        if (trees < 1 || sampleSize < 2) throw new IllegalArgumentException("trees >= 1 and sampleSize >= 2 required");
        this.trees = trees;
        this.sampleSize = sampleSize;
        this.seed = seed;
    }

    public void fit(double[][] data) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("no training data");
        forest.clear();
        Random rnd = new Random(seed);
        psi = Math.min(sampleSize, data.length);
        int maxDepth = (int) Math.ceil(Math.log(Math.max(psi, 2)) / Math.log(2));
        List<Integer> idx = new ArrayList<>(data.length);
        for (int i = 0; i < data.length; i++) idx.add(i);
        for (int t = 0; t < trees; t++) {
            Collections.shuffle(idx, rnd);
            List<double[]> sample = new ArrayList<>(psi);
            for (int i = 0; i < psi; i++) sample.add(data[idx.get(i)]);
            forest.add(build(sample, 0, maxDepth, rnd));
        }
    }

    public double score(double[] x) {
        if (forest.isEmpty()) throw new IllegalStateException("model not fitted");
        double sum = 0;
        for (Node tree : forest) sum += pathLength(x, tree, 0);
        double avg = sum / forest.size();
        double norm = c(psi);
        return norm == 0 ? 0.5 : Math.pow(2.0, -avg / norm);
    }

    private Node build(List<double[]> rows, int depth, int maxDepth, Random rnd) {
        if (depth >= maxDepth || rows.size() <= 1) return new Node(-1, 0, null, null, rows.size());
        int dims = rows.get(0).length;
        List<Integer> order = new ArrayList<>(dims);
        for (int i = 0; i < dims; i++) order.add(i);
        Collections.shuffle(order, rnd);

        int feature = -1;
        double min = 0, max = 0;
        for (int f : order) {                       // pick a random feature that still varies
            double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
            for (double[] r : rows) { lo = Math.min(lo, r[f]); hi = Math.max(hi, r[f]); }
            if (hi > lo) { feature = f; min = lo; max = hi; break; }
        }
        if (feature < 0) return new Node(-1, 0, null, null, rows.size());

        double split = min + rnd.nextDouble() * (max - min);
        List<double[]> left = new ArrayList<>(), right = new ArrayList<>();
        for (double[] r : rows) (r[feature] < split ? left : right).add(r);
        return new Node(feature, split, build(left, depth + 1, maxDepth, rnd), build(right, depth + 1, maxDepth, rnd), 0);
    }

    private double pathLength(double[] x, Node node, int depth) {
        if (node.isLeaf()) return depth + c(node.size());
        return pathLength(x, x[node.feature()] < node.split() ? node.left() : node.right(), depth + 1);
    }

    /** Average path length of an unsuccessful BST search, used to normalise tree depth. */
    static double c(int n) {
        if (n <= 1) return 0;
        if (n == 2) return 1;
        return 2.0 * (Math.log(n - 1) + EULER_GAMMA) - 2.0 * (n - 1) / n;
    }
}
