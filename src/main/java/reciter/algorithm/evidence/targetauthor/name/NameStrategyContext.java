package reciter.algorithm.evidence.targetauthor.name;

import java.util.List;

import reciter.algorithm.evidence.targetauthor.TargetAuthorStrategy;
import reciter.algorithm.evidence.targetauthor.TargetAuthorStrategyContext;
import reciter.database.mongo.model.Identity;
import reciter.model.article.ReCiterArticle;

public class NameStrategyContext implements TargetAuthorStrategyContext {

	private final TargetAuthorStrategy strategy;
	
	public NameStrategyContext(TargetAuthorStrategy strategy) {
		this.strategy = strategy;
	}
	
	@Override
	public double executeStrategy(ReCiterArticle reCiterArticle, Identity identity) {
		return strategy.executeStrategy(reCiterArticle, identity);
	}

	@Override
	public double executeStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		return strategy.executeStrategy(reCiterArticles, identity);
	}

}
