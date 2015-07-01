package quickml.supervised.tree.decisionTree.treeBuildContexts;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import quickml.data.ClassifierInstance;
import quickml.supervised.tree.attributeIgnoringStrategies.AttributeIgnoringStrategy;
import quickml.supervised.tree.attributeIgnoringStrategies.IgnoreAttributesWithConstantProbability;
import quickml.supervised.tree.attributeValueIgnoringStrategies.AttributeValueIgnoringStrategyBuilder;
import quickml.supervised.tree.branchFinders.BranchFinder;
import quickml.supervised.tree.branchFinders.branchFinderBuilders.BranchFinderBuilder;

import quickml.supervised.tree.constants.AttributeType;
import quickml.supervised.tree.decisionTree.branchFinders.branchFinderBuilders.DTBinaryCatBranchFinderBuilder;
import quickml.supervised.tree.decisionTree.branchFinders.branchFinderBuilders.DTBranchFinderBuilder;
import quickml.supervised.tree.decisionTree.branchFinders.branchFinderBuilders.DTCatBranchFinderBuilder;
import quickml.supervised.tree.dataExploration.DTreeTrainingDataSurveyor;
import quickml.supervised.tree.decisionTree.branchFinders.branchFinderBuilders.DTNumBranchFinderBuilder;
import quickml.supervised.tree.decisionTree.branchingConditions.DTBranchingConditions;
import quickml.supervised.tree.decisionTree.reducers.*;
import quickml.supervised.tree.decisionTree.valueCounters.ClassificationCounterProducer;
import quickml.scorers.Scorer;
import quickml.supervised.tree.nodes.LeafBuilder;
import quickml.supervised.tree.treeBuildContexts.TreeContextBuilder;
import quickml.supervised.tree.constants.BranchType;
import quickml.supervised.tree.decisionTree.valueCounters.ClassificationCounter;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static quickml.supervised.tree.decisionTree.DecisionTreeBuilder.*;

import static quickml.supervised.tree.constants.ForestOptions.*;

/**
 * Created by alexanderhawk on 6/20/15.
 */
public class DTreeContextBuilder<I extends ClassifierInstance> extends TreeContextBuilder<I, ClassificationCounter> {

    @Override
    public DTreeContextBuilder<I> createTreeBuildContext() {
        return new DTreeContextBuilder<>();
    }

    @Override
    public DTreeContext<I> buildContext(List<I> trainingData) {
        boolean considerBooleanAttributes = hasBranchFinderBuilder(BranchType.BOOLEAN);
        DTreeTrainingDataSurveyor<I> decTreeTrainingDataSurveyor = new DTreeTrainingDataSurveyor<>(considerBooleanAttributes);
        Map<AttributeType, Set<String>> candidateAttributesByType = decTreeTrainingDataSurveyor.groupAttributesByType(trainingData);
        ClassificationCounter classificationCounts = getValueCounterProducer().getValueCounter(trainingData);
        List<DTreeBranchFinderAndReducer<I>> branchFinderAndReducers = intializeBranchFindersAndReducers(classificationCounts, candidateAttributesByType);
        updateAll(branchFinderAndReducers);
        return new DTreeContext<I>(classificationCounts.allClassifications(), branchingConditions, scorer, branchFinderAndReducers, leafBuilder, getValueCounterProducer());
    }

    @Override
    public ClassificationCounterProducer<I> getValueCounterProducer() {
        return new ClassificationCounterProducer<>();
    }

    @Override
    public synchronized DTreeContextBuilder<I> copy() {
        //TODO: should only copy the config, and make sure the others get updated.  This is redundant.
        DTreeContextBuilder<I> copy = createTreeBuildContext();
        copy.updateBuilderConfig(this.config);
        return copy;
    }

    private List<BranchFinderBuilder<ClassificationCounter>> copyBranchFinderBuilders(Map<String, Object> config) {
        List<BranchFinderBuilder<ClassificationCounter>> copiedBranchFinderBuilders = Lists.newArrayList();
        if (config.containsKey(BRANCH_FINDER_BUILDERS.name())) {
            List<BranchFinderBuilder<ClassificationCounter>> bfbs = (List<BranchFinderBuilder<ClassificationCounter>>) config.get(BRANCH_FINDER_BUILDERS.name());
            if (bfbs != null && !bfbs.isEmpty()) {
                for (BranchFinderBuilder<ClassificationCounter> branchFinderBuilder : bfbs) {
                    copiedBranchFinderBuilders.add(branchFinderBuilder.copy());
                }
                return copiedBranchFinderBuilders;
            } else {
                return getDefaultBranchFinderBuilders();
            }
        }
        return getDefaultBranchFinderBuilders();
    }


    private void updateAll(List<DTreeBranchFinderAndReducer<I>> branchFinderAndReducers) {
        for (DTreeBranchFinderAndReducer<I> branchFinderAndReducer : branchFinderAndReducers) {
            branchFinderAndReducer.getReducer().updateBuilderConfig(config);
        }
        updateBuilderConfig(config);
    }

    private List<DTreeBranchFinderAndReducer<I>> intializeBranchFindersAndReducers(ClassificationCounter classificationCounts, Map<AttributeType, Set<String>> candidateAttributesByType) {
        /**Branch finders should be paired with the correct reducers. With this method, we don't leave open the possibility for a user to make a mistake with the pairings.
         * */
        List<DTreeBranchFinderAndReducer<I>> branchFindersAndReducers = Lists.newArrayList();
        int numClasses = classificationCounts.allClassifications().size();
        Object minorityClassification = ClassificationCounter.getLeastPopularClass(classificationCounts);
        Map<BranchType, DTreeReducer<I>> reducerMap = getDefaultReducers(minorityClassification);
        for (BranchFinderBuilder<ClassificationCounter> branchFinderBuilder : getBranchFinderBuilders()) {
            if (useBranchFinder(branchFinderBuilder, numClasses)) {
                AttributeType attributeType = AttributeType.convertBranchTypeToAttributeType(branchFinderBuilder.getBranchType());
                BranchFinder<ClassificationCounter> branchFinder = branchFinderBuilder.buildBranchFinder(classificationCounts, candidateAttributesByType.get(attributeType));
                DTreeReducer<I> reducer = reducerMap.get(branchFinderBuilder.getBranchType());
                branchFindersAndReducers.add(new DTreeBranchFinderAndReducer<I>(branchFinder, reducer));
            }
        }
        return branchFindersAndReducers;
    }


    private boolean useBranchFinder(BranchFinderBuilder<ClassificationCounter> branchFinderBuilder, int numClasses) {
        if (branchFinderBuilder.getBranchType().equals(BranchType.BINARY_CATEGORICAL) && numClasses != 2) {
            return false;
        }
        if (branchFinderBuilder.getBranchType().equals(BranchType.CATEGORICAL) && numClasses == 2) {
            return false;
        }
        return true;
    }

    public static <I extends ClassifierInstance> Map<BranchType, DTreeReducer<I>> getDefaultReducers(Object minorityClassification) {
        Map<BranchType, DTreeReducer<I>> reducers = Maps.newHashMap();
        reducers.put(BranchType.BINARY_CATEGORICAL, new BinaryCatBranchReducer<I>(minorityClassification));
        reducers.put(BranchType.CATEGORICAL, new DTCatBranchReducer<I>());
        reducers.put(BranchType.NUMERIC, new DTNumBranchReducer<I>());
        reducers.put(BranchType.BOOLEAN, new DTCatBranchReducer<I>());
        return reducers;
    }

    public static <I extends ClassifierInstance> List<BranchFinderBuilder<ClassificationCounter>> getDefaultBranchFinderBuilders() {
        List<BranchFinderBuilder<ClassificationCounter>> branchFinderBuilders = Lists.newArrayList();
        branchFinderBuilders.add(new DTBinaryCatBranchFinderBuilder());
        branchFinderBuilders.add(new DTCatBranchFinderBuilder());
        branchFinderBuilders.add(new DTNumBranchFinderBuilder());
        return branchFinderBuilders;
    }

    public void setDefaultsAsNeededAndUpdateBuilderConfig() {
        if (!config.containsKey(BRANCH_FINDER_BUILDERS.name())) {
            branchFinderBuilders(DTreeContextBuilder.getDefaultBranchFinderBuilders());
        }
        if (!config.containsKey(MAX_DEPTH.name())) {
            maxDepth(DEFAULT_MAX_DEPTH);
        }
        if (!config.containsKey(MIN_SLPIT_FRACTION.name())) {
            minSplitFraction(DEFAULT_MIN_SPLIT_FRACTION);
        }
        if (!config.containsKey(ATTRIBUTE_IGNORING_STRATEGY.name())) {
            attributeIgnoringStrategy(DEFAULT_ATTRIBUTE_IGNORING_STRATEGY);
        }
        if (!config.containsKey(NUM_SAMPLES_PER_NUMERIC_BIN.name())) {
            numSamplesPerNumericBin(DEFAULT_NUM_SAMPLES_PER_NUMERIC_BIN);
        }
        if (!config.containsKey(NUM_NUMERIC_BINS.name())) {
            numNumericBins(DEFAULT_NUM_NUMERIC_BINS);
        }
        if (!config.containsKey(BRANCHING_CONDITIONS.name())) {
            branchingConditions(DEFAULT_BRANCHING_CONDITIONS);
        }
        if (!config.containsKey(SCORER.name())) {
            scorer(DEFAULT_SCORER);
        }
        if (!config.containsKey(DEGREE_OF_GAIN_RATIO_PENALTY.name())) {
            degreeOfGainRatioPenalty(DEFAULT_DEGREE_OF_GAIN_RATIO_PENALTY);
        }
        if (!config.containsKey(IMBALANCE_PENALTY_POWER.name())) {
            imbalancePenaltyPower(DEFAULT_IMBALANCE_PENALTY_POWER);
        }
        if (!config.containsKey(MIN_ATTRIBUTE_VALUE_OCCURRENCES.name())) {
            minAttributeOccurences(DEFAULT_MIN_ATTRIBUTE_OCCURENCES);
        }
        if (!config.containsKey(LEAF_BUILDER.name())) {
            leafBuilder(DEFAULT_LEAF_BUILDER);
        }

        if (!config.containsKey(MIN_LEAF_INSTANCES.name())) {
            minLeafInstances(DEFAULT_MIN_LEAF_INSTANCES);
        }
        updateBuilderConfig(this.config);
    }

    @Override
    public Map<String, Object> copyConfig(Map<String, Object> config) {
        Map<String, Object> copiedConfig = Maps.newHashMap();
        if (config.containsKey(BRANCH_FINDER_BUILDERS.name())) {
            copiedConfig.put(BRANCH_FINDER_BUILDERS.name(), copyBranchFinderBuilders(config));
        }
        if (config.containsKey(MAX_DEPTH.name())) {
            copiedConfig.put(MAX_DEPTH.name(), config.get(MAX_DEPTH.name()));
        }
        if (config.containsKey(MIN_SLPIT_FRACTION.name())) {
            copiedConfig.put(MIN_SLPIT_FRACTION.name(), config.get(MIN_SLPIT_FRACTION.name()));
        }
        if (config.containsKey(ATTRIBUTE_IGNORING_STRATEGY.name())) {
            copiedConfig.put(ATTRIBUTE_IGNORING_STRATEGY.name(), config.get(ATTRIBUTE_IGNORING_STRATEGY.name()));
        }
        if (config.containsKey(NUM_SAMPLES_PER_NUMERIC_BIN.name())) {
            copiedConfig.put(NUM_SAMPLES_PER_NUMERIC_BIN.name(), config.get(NUM_SAMPLES_PER_NUMERIC_BIN.name()));
        }
        if (config.containsKey(NUM_NUMERIC_BINS.name())) {
            copiedConfig.put(NUM_NUMERIC_BINS.name(), config.get(NUM_NUMERIC_BINS.name()));
        }
        if (config.containsKey(BRANCHING_CONDITIONS.name())) {
            copiedConfig.put(BRANCHING_CONDITIONS.name(), config.get(BRANCHING_CONDITIONS.name()));
        }
        if (config.containsKey(SCORER.name())) {
            copiedConfig.put(SCORER.name(), config.get(SCORER.name()));
        }
        if (config.containsKey(DEGREE_OF_GAIN_RATIO_PENALTY.name())) {
            copiedConfig.put(DEGREE_OF_GAIN_RATIO_PENALTY.name(), config.get(DEGREE_OF_GAIN_RATIO_PENALTY.name()));
        }
        if (config.containsKey(IMBALANCE_PENALTY_POWER.name())) {
            copiedConfig.put(IMBALANCE_PENALTY_POWER.name(), config.get(IMBALANCE_PENALTY_POWER.name()));
        }
        if (config.containsKey(MIN_ATTRIBUTE_VALUE_OCCURRENCES.name())) {
            copiedConfig.put(MIN_ATTRIBUTE_VALUE_OCCURRENCES.name(), config.get(MIN_ATTRIBUTE_VALUE_OCCURRENCES.name()));
        }
        if (config.containsKey(LEAF_BUILDER.name())) {
            copiedConfig.put(LEAF_BUILDER.name(), config.get(LEAF_BUILDER.name()));
        }

        if (config.containsKey(MIN_LEAF_INSTANCES.name())) {
            copiedConfig.put(MIN_LEAF_INSTANCES.name(), config.get(MIN_LEAF_INSTANCES.name()));
        }
        return copiedConfig;
    }

    //check that haven't missed any settings.
    public void maxDepth(int maxDepth) {
        config.put(MAX_DEPTH.name(), maxDepth);
    }

    public void leafBuilder(LeafBuilder<ClassificationCounter> leafBuilder) {
        config.put(LEAF_BUILDER.name(), leafBuilder);
    }

    public void ignoreAttributeProbability(int ignoreAttributeProbability) {
        config.put(ATTRIBUTE_IGNORING_STRATEGY.name(), new IgnoreAttributesWithConstantProbability(ignoreAttributeProbability));
    }

    public void minSplitFraction(double minSplitFraction) {
        config.put(MIN_SLPIT_FRACTION.name(), minSplitFraction);
    }

    public void minLeafInstances(int minLeafInstances) {
        config.put(MIN_LEAF_INSTANCES.name(), minLeafInstances);
    }

    public void exemptAttributes(Set<String> exemptAttributes) {
        config.put(EXEMPT_ATTRIBUTES.name(), exemptAttributes);
    }

    public void attributeIgnoringStrategy(AttributeIgnoringStrategy attributeIgnoringStrategy) {
        config.put(ATTRIBUTE_IGNORING_STRATEGY.name(), attributeIgnoringStrategy);
    }

    public void attributeValueIgnoringStrategyBuilder(AttributeValueIgnoringStrategyBuilder<ClassificationCounter> attributeValueIgnoringStrategyBuilder) {
        config.put(ATTRIBUTE_VALUE_IGNORING_STRATEGY_BUILDER.name(), attributeValueIgnoringStrategyBuilder);
    }

    public void numSamplesPerNumericBin(int numSamplesPerNumericBin) {
        config.put(NUM_SAMPLES_PER_NUMERIC_BIN.name(), numSamplesPerNumericBin);
    }

    public void numNumericBins(int numNumericBins) {
        config.put(NUM_NUMERIC_BINS.name(), numNumericBins);
    }

    public void branchingConditions(DTBranchingConditions branchingConditions) {
        config.put(BRANCHING_CONDITIONS.name(), branchingConditions);
    }

    public void scorer(Scorer<ClassificationCounter> scorer) {
        config.put(SCORER.name(), scorer);
    }

    public void degreeOfGainRatioPenalty(double degreeOfGainRatioPenalty) {
        config.put(DEGREE_OF_GAIN_RATIO_PENALTY.name(), degreeOfGainRatioPenalty);
    }

    public void imbalancePenaltyPower(double imbalancePenaltyPower) {
        config.put(IMBALANCE_PENALTY_POWER.name(), imbalancePenaltyPower);
    }

    public void branchFinderBuilders(List<? extends BranchFinderBuilder<ClassificationCounter>> branchFinderBuilders) {
        config.put(BRANCH_FINDER_BUILDERS.name(), branchFinderBuilders);
    }

    public void minAttributeOccurences(int minAttributeOccurences) {
        config.put(MIN_ATTRIBUTE_VALUE_OCCURRENCES.name(), minAttributeOccurences);
    }


}