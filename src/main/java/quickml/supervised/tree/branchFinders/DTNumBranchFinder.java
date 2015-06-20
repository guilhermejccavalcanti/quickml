package quickml.supervised.tree.branchFinders;

import com.google.common.base.Optional;
import quickml.supervised.tree.attributeIgnoringStrategies.AttributeIgnoringStrategy;
import quickml.supervised.tree.attributeIgnoringStrategies.AttributeValueIgnoringStrategy;
import quickml.supervised.tree.constants.BranchType;
import quickml.supervised.tree.decisionTree.ClassificationCounter;
import quickml.supervised.tree.nodes.AttributeStats;
import quickml.supervised.tree.nodes.Branch;
import quickml.supervised.tree.nodes.DTNode;
import quickml.supervised.tree.nodes.DTNumBranch;
import quickml.supervised.tree.scorers.Scorer;
import quickml.supervised.tree.terminationConditions.BranchingConditions;

import java.util.Collection;

/**
 * Created by alexanderhawk on 6/12/15.
 */
public class DTNumBranchFinder extends  NumericBranchFinder<ClassificationCounter, DTNode> {
    public DTNumBranchFinder(Collection<String> candidateAttributes, BranchingConditions<ClassificationCounter, DTNode> branchingConditions, Scorer<ClassificationCounter> scorer, AttributeValueIgnoringStrategy<ClassificationCounter> attributeValueIgnoringStrategy, AttributeIgnoringStrategy attributeIgnoringStrategy, BranchType branchType) {
        super(candidateAttributes, branchingConditions, scorer, attributeValueIgnoringStrategy, attributeIgnoringStrategy, branchType);
    }

    @Override
    protected Optional<? extends Branch<ClassificationCounter, DTNode>> createBranch(Branch<ClassificationCounter, DTNode> parent, AttributeStats<ClassificationCounter> attributeStats, SplittingUtils.SplitScore splitScore, double bestThreshold) {
        return Optional.of(new DTNumBranch(parent, attributeStats.getAttribute(),
                splitScore.probabilityOfBeingInTrueSet, splitScore.score,
                attributeStats.getAggregateStats(), bestThreshold));
    }
}