package com.pgds.complaint;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

/**
 * Complaint category classifier: TF-IDF word vectors + Multinomial Naive Bayes (Weka).
 * Trained at startup from ai/complaints_training.csv (format: LABEL|text).
 * Falls back to the rule-based classifier if the model is missing, the text is empty, or confidence is low.
 * Priority and department come from the category mapping shared with the rule-based classifier.
 */
@Slf4j
@Primary
@Component
public class WekaComplaintClassifier implements ComplaintClassifier {

    private static final double MIN_CONFIDENCE = 0.45;

    public record ModelInfo(String algorithm, boolean trained, int trainingSamples, List<String> categories,
                            Double crossValidationAccuracyPercent) {}

    private final RuleBasedComplaintClassifier rules;
    private FilteredClassifier model;
    private Instances header;           // string-free structure, copied for each prediction
    private List<String> labels = List.of();
    private int samples;
    private Double cvAccuracy;

    public WekaComplaintClassifier(RuleBasedComplaintClassifier rules) { this.rules = rules; }

    @PostConstruct
    public void train() {
        try {
            List<String[]> rows = loadTrainingData();
            if (rows.size() < 20) throw new IllegalStateException("too few training rows: " + rows.size());
            TreeSet<String> set = new TreeSet<>();
            rows.forEach(r -> set.add(r[0]));
            List<String> classValues = new ArrayList<>(set);

            ArrayList<Attribute> attrs = new ArrayList<>();
            attrs.add(new Attribute("text", (List<String>) null));
            attrs.add(new Attribute("category", classValues));
            Instances data = new Instances("complaints", attrs, rows.size());
            data.setClassIndex(1);
            for (String[] r : rows) {
                Instance inst = new DenseInstance(2);
                inst.setDataset(data);
                inst.setValue(0, r[1]);
                inst.setValue(1, r[0]);
                data.add(inst);
            }

            StringToWordVector filter = new StringToWordVector(1000);
            filter.setLowerCaseTokens(true);
            filter.setOutputWordCounts(true);
            filter.setTFTransform(true);
            filter.setIDFTransform(true);
            FilteredClassifier fc = new FilteredClassifier();
            fc.setFilter(filter);
            fc.setClassifier(new NaiveBayesMultinomial());

            try {   // informative only; never blocks startup
                Evaluation eval = new Evaluation(data);
                eval.crossValidateModel(fc, data, 5, new Random(1));
                cvAccuracy = Math.round(eval.pctCorrect() * 10.0) / 10.0;
            } catch (Exception e) {
                log.warn("Cross-validation skipped: {}", e.getMessage());
            }

            fc.buildClassifier(data);
            this.header = data.stringFreeStructure();
            this.labels = classValues;
            this.samples = rows.size();
            this.model = fc;
            log.info("Complaint classifier trained on {} samples, 5-fold CV accuracy {}%", samples, cvAccuracy);
        } catch (Exception e) {
            log.warn("Complaint ML model unavailable, using rule-based classifier: {}", e.toString());
            this.model = null;
        }
    }

    @Override
    public synchronized Classification classify(String text) {
        if (model == null || text == null || text.isBlank()) return rules.classify(text);
        try {
            Instances test = header.stringFreeStructure();
            Instance inst = new DenseInstance(2);
            inst.setDataset(test);
            inst.setValue(0, text);
            inst.setMissing(1);
            test.add(inst);
            double[] dist = model.distributionForInstance(test.firstInstance());
            int best = 0;
            for (int i = 1; i < dist.length; i++) if (dist[i] > dist[best]) best = i;
            if (dist[best] < MIN_CONFIDENCE) return rules.classify(text);

            String category = labels.get(best);
            Classification byRule = rules.classify(text);
            if ("OTHER".equals(category) && !"OTHER".equals(byRule.category())) return byRule;   // trust a clear keyword hit
            return RuleBasedComplaintClassifier.forCategory(category);
        } catch (Exception e) {
            log.debug("ML classification failed, using rules", e);
            return rules.classify(text);
        }
    }

    public ModelInfo info() {
        return new ModelInfo("TF-IDF + Multinomial Naive Bayes (Weka)", model != null, samples, labels, cvAccuracy);
    }

    private List<String[]> loadTrainingData() throws Exception {
        List<String[]> rows = new ArrayList<>();
        try (var in = new BufferedReader(new InputStreamReader(
                new ClassPathResource("ai/complaints_training.csv").getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int bar = line.indexOf('|');
                if (bar > 0) rows.add(new String[]{line.substring(0, bar).trim(), line.substring(bar + 1).trim()});
            }
        }
        return rows;
    }
}
