package quickml.supervised.classifier.logRegression;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import quickml.data.AttributesMap;
import quickml.data.PredictionMap;
import quickml.supervised.Utils;
import quickml.supervised.classifier.AbstractClassifier;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Created by alexanderhawk on 10/9/15.
 */
public class LogisticRegression extends AbstractClassifier {
//make onehot encoder, and mean normalizer live in a class that processes a data set in its init method...and can process raw instances from there on out, let it take a sparse instanceConverter as well
    double[] weights;
    private final HashMap<String, Integer> nameToIndexMap;
    private static final Logger logger = LoggerFactory.getLogger(LogisticRegression.class);
    private final Map<Serializable, Double> classifications;
    Map<String, Utils.MeanAndStd> meanAndStdMap;

    public LogisticRegression(double [] weights, final HashMap<String, Integer> nameToIndexMap,
                              Map<Serializable, Double> classifications, Map<String, Utils.MeanAndStd> meanAndStdMap) {
        this.weights = weights;
        this.nameToIndexMap = nameToIndexMap;
        this.classifications= classifications;
        this.meanAndStdMap = meanAndStdMap;
    }

    @Override
    public double getProbability(final AttributesMap attributes, final Serializable classification) {
        //could add a method: getFastProbability, that does everything with arrays.
        double dotProduct = 0;
        for (String attribute : attributes.keySet()) {
            if (meanAndStdMap.containsKey(attribute)) {
                Utils.MeanAndStd meanAndStd = meanAndStdMap.get(attribute);
                int index = nameToIndexMap.get(attribute);
                 dotProduct += weights[index] * LogisticRegressionBuilder.meanNormalize(attributes, attribute, meanAndStd);
        }
        }
        return  quickml.math.Utils.sigmoid(dotProduct);
    }




    @Override
    public PredictionMap predict(final AttributesMap attributes) {
        PredictionMap predictionMap = new PredictionMap(new HashMap<Serializable, Double>());
        for (Serializable classification : classifications.keySet()) {
            predictionMap.put(classification, getProbability(attributes, classifications.get(classification)));
        }
        return predictionMap;
    }
    @Override
    public PredictionMap predictWithoutAttributes(final AttributesMap attributes, final Set<String> attributesToIgnore) {
        throw new RuntimeException("not implemented");
    }


}

