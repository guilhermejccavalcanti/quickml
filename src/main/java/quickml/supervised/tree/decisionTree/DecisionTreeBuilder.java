package quickml.supervised.tree.decisionTree;

import com.google.common.collect.Lists;
import org.javatuples.Pair;
import quickml.data.ClassifierInstance;
import quickml.data.PredictionMap;
import quickml.supervised.tree.TreeBuilder;
import quickml.supervised.tree.attributeIgnoringStrategies.AttributeIgnoringStrategy;
import quickml.supervised.tree.attributeIgnoringStrategies.IgnoreAttributesWithConstantProbability;
import quickml.supervised.tree.attributeValueIgnoringStrategies.AttributeValueIgnoringStrategyBuilder;
import quickml.supervised.tree.branchFinders.branchFinderBuilders.BranchFinderBuilder;
import quickml.supervised.tree.decisionTree.branchingConditions.DTBranchingConditions;
import quickml.supervised.tree.decisionTree.scorers.GiniImpurityScorer;
import quickml.supervised.tree.decisionTree.treeBuildContexts.DTreeContextBuilder;
import quickml.supervised.tree.decisionTree.nodes.DTNode;
import quickml.supervised.tree.decisionTree.valueCounters.ClassificationCounter;
import quickml.supervised.tree.scorers.Scorer;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by alexanderhawk on 6/20/15.
 */
public class DecisionTreeBuilder<I extends ClassifierInstance> implements TreeBuilder<PredictionMap, I> {
    public static final int DEFAULT_MAX_DEPTH = 7;
    public static final int DEFAULT_NUM_SAMPLES_PER_NUMERIC_BIN = 50;
    public static final IgnoreAttributesWithConstantProbability DEFAULT_ATTRIBUTE_IGNORING_STRATEGY = new IgnoreAttributesWithConstantProbability(0.7);
    public static final int DEFAULT_NUM_NUMERIC_BINS = 6;
    public static final GiniImpurityScorer DEFAULT_SCORER = new GiniImpurityScorer();
    public static final DTBranchingConditions DEFAULT_BRANCHING_CONDITIONS = new DTBranchingConditions();
    public static final double DEFAULT_DEGREE_OF_GAIN_RATIO_PENALTY = 1.0;
    public static final double DEFAULT_IMBALANCE_PENALTY_POWER = 0.0;
    public static final double DEFAULT_MIN_SPLIT_FRACTION = 0.01;

    private final DTreeContextBuilder<I> tcb;

    private DecisionTreeBuilder(DTreeContextBuilder<I> tcb) {
        this.tcb = tcb;
    }

    public DecisionTreeBuilder(){
        this.tcb = new DTreeContextBuilder<>();
    }

    @Override
    public DecisionTree buildPredictiveModel(Iterable<I> trainingData) {
        tcb.setDefaultsAsNeeded();
        DecisionTreeBuilderHelper<I> treeBuilderHelper = new DecisionTreeBuilderHelper<>(tcb);
        Pair<DTNode, Set<Object>> rootAndClassifications = treeBuilderHelper.computeNodesAndClasses(Lists.newArrayList(trainingData));
        DTNode root = rootAndClassifications.getValue0();
        Set<Object> classifications = rootAndClassifications.getValue1();
        return new DecisionTree(root, classifications);
    }

    @Override
    public void updateBuilderConfig(Map<String, Object> config) {
        tcb.updateBuilderConfig(config);
    }

    @Override
    public DecisionTreeBuilder<I> copy() {
        return new DecisionTreeBuilder<>(tcb.copy());
    }

    //check that haven't missed any settings.
    public DecisionTreeBuilder<I> maxDepth(int maxDepth) {
        tcb.maxDepth(maxDepth);
        return this;
    }

    public DecisionTreeBuilder<I> ignoreAttributeProbability(int ignoreAttributeProbability) {
        tcb.ignoreAttributeProbability(ignoreAttributeProbability);
        return this;
    }

    public DecisionTreeBuilder<I> minSplitFraction(double minSplitFraction) {
        tcb.minSplitFraction(minSplitFraction);
        return this;
    }

    public DecisionTreeBuilder<I> exemptAttributes(Set<String> exemptAttributes) {
        tcb.exemptAttributes(exemptAttributes);
        return this;
    }

    public DecisionTreeBuilder<I> attributeIgnoringStrategy(AttributeIgnoringStrategy attributeIgnoringStrategy) {
        tcb.attributeIgnoringStrategy(attributeIgnoringStrategy);
        return this;
    }

    public DecisionTreeBuilder<I> attributeValueIgnoringStrategyBuilder(AttributeValueIgnoringStrategyBuilder<ClassificationCounter> attributeValueIgnoringStrategyBuilder) {
        tcb.attributeValueIgnoringStrategyBuilder(attributeValueIgnoringStrategyBuilder);
        return this;
    }

    public DecisionTreeBuilder<I> numSamplesPerNumericBin(int numSamplesPerNumericBin) {
        tcb.numSamplesPerNumericBin(numSamplesPerNumericBin);
        return this;
    }

    public DecisionTreeBuilder<I> numNumericBins(int numNumericBins) {
        tcb.numNumericBins(numNumericBins);
        return this;
    }

    public DecisionTreeBuilder<I> branchingConditions(DTBranchingConditions branchingConditions) {
        tcb.branchingConditions(branchingConditions);
        return this;
    }

    public DecisionTreeBuilder<I> scorer(Scorer<ClassificationCounter> scorer) {
        tcb.scorer(scorer);
        return this;
    }

    public DecisionTreeBuilder<I> degreeOfGainRatioPenalty(double degreeOfGainRatioPenalty) {
        tcb.degreeOfGainRatioPenalty(degreeOfGainRatioPenalty);
        return this;
    }

    public DecisionTreeBuilder<I> imbalancePenaltyPower(double imbalancePenaltyPower) {
        tcb.imbalancePenaltyPower(imbalancePenaltyPower);
        return this;
    }

    public DecisionTreeBuilder<I> branchFinderBuilders(List<? extends BranchFinderBuilder<ClassificationCounter, DTNode>> branchFinderBuilders) {
        tcb.branchFinderBuilders(branchFinderBuilders);
        return this;
    }


}
