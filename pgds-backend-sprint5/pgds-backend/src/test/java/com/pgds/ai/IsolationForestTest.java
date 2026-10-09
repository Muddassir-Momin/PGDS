package com.pgds.ai;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class IsolationForestTest {

    private static double[][] normalCluster(int n) {
        Random r = new Random(1);
        double[][] d = new double[n][2];
        for (int i = 0; i < n; i++) { d[i][0] = 1.0 + r.nextGaussian() * 0.1; d[i][1] = 5.0 + r.nextGaussian() * 0.2; }
        return d;
    }

    @Test
    void outlierScoresHigherThanTypicalPoint() {
        IsolationForest f = new IsolationForest(100, 128, 42L);
        f.fit(normalCluster(300));
        double typical = f.score(new double[]{1.0, 5.0});
        double outlier = f.score(new double[]{9.0, 30.0});
        assertTrue(outlier > typical, "outlier " + outlier + " should exceed typical " + typical);
        assertTrue(outlier > 0.65, "outlier score was " + outlier);
        assertTrue(typical < 0.55, "typical score was " + typical);
    }

    @Test
    void sameSeedGivesSameScores() {
        double[][] data = normalCluster(200);
        IsolationForest a = new IsolationForest(50, 64, 7L), b = new IsolationForest(50, 64, 7L);
        a.fit(data); b.fit(data);
        assertEquals(a.score(new double[]{3, 3}), b.score(new double[]{3, 3}), 1e-12);
    }

    @Test
    void scoreBeforeFitFails() {
        assertThrows(IllegalStateException.class, () -> new IsolationForest(10, 16, 1L).score(new double[]{1}));
    }

    @Test
    void normalisationConstantMatchesPaper() {
        assertEquals(0.0, IsolationForest.c(1));
        assertEquals(1.0, IsolationForest.c(2));
        assertEquals(10.24, IsolationForest.c(256), 0.1);   // c(256) is about 10.2
    }
}
