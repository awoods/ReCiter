package reciter.algorithm.evidence.article.title;

import java.util.List;

import reciter.algorithm.evidence.article.RemoveReCiterArticleStrategyContext;
import reciter.database.mongo.model.Identity;
import reciter.model.article.ReCiterArticle;

public class ArticleTitleStrategyContext implements RemoveReCiterArticleStrategyContext {

	private final RemoveReCiterArticleStrategyContext strategy;
	
	public ArticleTitleStrategyContext(RemoveReCiterArticleStrategyContext strategy) {
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
