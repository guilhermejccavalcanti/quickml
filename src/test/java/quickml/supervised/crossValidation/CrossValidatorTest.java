package quickml.supervised.crossValidation;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.mockito.Mockito;
import quickml.data.AttributesMap;
import quickml.data.PredictionMap;
import quickml.supervised.crossValidation.crossValLossFunctions.CrossValLossFunction;
import quickml.supervised.crossValidation.crossValLossFunctions.LabelPredictionWeight;
import quickml.data.Instance;
import quickml.data.InstanceImpl;
import quickml.supervised.classifier.decisionTree.TreeBuilder;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Created by Chris on 5/6/2014.
 */
public class CrossValidatorTest {

    @Test
    public void testCrossValidator() {
        CrossValLossFunction<Serializable, PredictionMap> crossValLossFunction = mock(CrossValLossFunction.class);

        int folds = 4;
        ClassifierStationaryCrossValidator crossValidator = new ClassifierStationaryCrossValidator(folds, folds, crossValLossFunction);
        TreeBuilder treeBuilder = new TreeBuilder();
        List<Instance<AttributesMap, Serializable>> instances = getInstances();
        crossValidator.getCrossValidatedLoss(treeBuilder, instances);

        verify(crossValLossFunction, times(folds)).getLoss(anyList());
    }

    private List<Instance<AttributesMap, Serializable>> getInstances() {
        final List<Instance<AttributesMap, Serializable>> instances = Lists.newLinkedList();
        for (int i = 0; i < 5; i++) {
            AttributesMap attributes = AttributesMap.newHashMap();

            Instance<AttributesMap, Serializable> instance = mock(InstanceImpl.class);
            when(instance.getWeight()).thenReturn(1.0);
            when(instance.getLabel()).thenReturn("class");
            when(instance.getAttributes()).thenReturn(attributes);
            instances.add(instance);
        }
        return instances;
    }
}
