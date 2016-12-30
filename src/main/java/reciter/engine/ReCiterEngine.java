package reciter.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.cluster.Clusterer;
import reciter.algorithm.cluster.ReCiterClusterer;
import reciter.algorithm.cluster.model.ReCiterCluster;
import reciter.algorithm.cluster.targetauthor.ClusterSelector;
import reciter.algorithm.cluster.targetauthor.ReCiterClusterSelector;
import reciter.algorithm.evidence.StrategyContext;
import reciter.algorithm.evidence.article.mesh.MeshMajorStrategyContext;
import reciter.algorithm.evidence.article.mesh.strategy.MeshMajorStrategy;
import reciter.algorithm.evidence.targetauthor.TargetAuthorStrategyContext;
import reciter.algorithm.evidence.targetauthor.affiliation.AffiliationStrategyContext;
import reciter.algorithm.evidence.targetauthor.affiliation.strategy.WeillCornellAffiliationStrategy;
import reciter.algorithm.evidence.targetauthor.department.DepartmentStrategyContext;
import reciter.algorithm.evidence.targetauthor.department.strategy.DepartmentStringMatchStrategy;
import reciter.algorithm.evidence.targetauthor.email.EmailStrategyContext;
import reciter.algorithm.evidence.targetauthor.email.strategy.EmailStringMatchStrategy;
import reciter.algorithm.evidence.targetauthor.knownrelationship.KnownRelationshipStrategyContext;
import reciter.algorithm.evidence.targetauthor.knownrelationship.strategy.KnownRelationshipStrategy;
import reciter.algorithm.evidence.targetauthor.scopus.ScopusStrategyContext;
import reciter.algorithm.evidence.targetauthor.scopus.strategy.StringMatchingAffiliation;
import reciter.algorithm.util.ArticleTranslator;
import reciter.engine.erroranalysis.Analysis;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleMeshHeading;
import reciter.model.identity.Identity;
import reciter.model.pubmed.PubMedArticle;
import reciter.model.scopus.ScopusArticle;

public class ReCiterEngine implements Engine {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(ReCiterEngine.class);
	
	@Override
	public List<Feature> generateFeature(EngineParameters parameters) {
		
		Identity identity = parameters.getIdentity();
		List<PubMedArticle> pubMedArticles = parameters.getPubMedArticles();
		List<ScopusArticle> scopusArticles = parameters.getScopusArticles();
		
		Map<Long, ScopusArticle> map = new HashMap<Long, ScopusArticle>();
		for (ScopusArticle scopusArticle : scopusArticles) {
			map.put(scopusArticle.getPubmedId(), scopusArticle);
		}
		List<ReCiterArticle> reCiterArticles = new ArrayList<ReCiterArticle>();
		for (PubMedArticle pubMedArticle : pubMedArticles) {
			long pmid = pubMedArticle.getMedlineCitation().getMedlineCitationPMID().getPmid();
			if (map.containsKey(pmid)) {
				reCiterArticles.add(ArticleTranslator.translate(pubMedArticle, map.get(pmid)));
			} else {
				reCiterArticles.add(ArticleTranslator.translate(pubMedArticle, null));
			}
		}
		
		Analysis.assignGoldStandard(reCiterArticles, parameters.getKnownPmids());
		
		List<Feature> features = new ArrayList<Feature>();
		for (ReCiterArticle reCiterArticle : reCiterArticles) {
			Feature feature = new Feature();
			feature.setPmid(reCiterArticle.getArticleId());
			feature.setIsGoldStandard(reCiterArticle.getGoldStandard());
			
			TargetAuthorStrategyContext emailStrategyContext = new EmailStrategyContext(new EmailStringMatchStrategy());
			emailStrategyContext.populateFeature(reCiterArticle, identity, feature);
			
			TargetAuthorStrategyContext departmentStringMatchStrategyContext = new DepartmentStrategyContext(new DepartmentStringMatchStrategy());
			departmentStringMatchStrategyContext.populateFeature(reCiterArticle, identity, feature);
			
			TargetAuthorStrategyContext grantCoauthorStrategyContext = new KnownRelationshipStrategyContext(new KnownRelationshipStrategy());
			grantCoauthorStrategyContext.populateFeature(reCiterArticle, identity, feature);
			
			TargetAuthorStrategyContext affiliationStrategyContext = new AffiliationStrategyContext(new WeillCornellAffiliationStrategy());
			affiliationStrategyContext.populateFeature(reCiterArticle, identity, feature);
			
			TargetAuthorStrategyContext scopusStrategyContext = new ScopusStrategyContext(new StringMatchingAffiliation());
			scopusStrategyContext.populateFeature(reCiterArticle, identity, feature);
			
			features.add(feature);
		}
		return features;
	}
	
	@Override
	public EngineOutput run(EngineParameters parameters) {
		
		Identity identity = parameters.getIdentity();
		List<PubMedArticle> pubMedArticles = parameters.getPubMedArticles();
		List<ScopusArticle> scopusArticles = parameters.getScopusArticles();
		
		Map<Long, ScopusArticle> map = new HashMap<Long, ScopusArticle>();
		for (ScopusArticle scopusArticle : scopusArticles) {
			map.put(scopusArticle.getPubmedId(), scopusArticle);
		}
		List<ReCiterArticle> reCiterArticles = new ArrayList<ReCiterArticle>();
		for (PubMedArticle pubMedArticle : pubMedArticles) {
			long pmid = pubMedArticle.getMedlineCitation().getMedlineCitationPMID().getPmid();
			if (map.containsKey(pmid)) {
				reCiterArticles.add(ArticleTranslator.translate(pubMedArticle, map.get(pmid)));
			} else {
				reCiterArticles.add(ArticleTranslator.translate(pubMedArticle, null));
			}
		}
		
		Analysis.assignGoldStandard(reCiterArticles, parameters.getKnownPmids());

		// Perform Phase 1 clustering.
		Clusterer clusterer = new ReCiterClusterer(identity, reCiterArticles);
		int seedSize = ((Double) (parameters.getKnownPmids().size() * 1.0)).intValue();
		Set<Long> initialSeed = new HashSet<Long>();
		slf4jLogger.info("Initial seed size=[" + seedSize + "].");
		int taken = 0;
		for (long pmid : parameters.getKnownPmids()) {
			if (taken < seedSize) {
				initialSeed.add(pmid);
			} else {
				break;
			}
			taken++;
		}
		
		if (!initialSeed.isEmpty()) {
			clusterer.cluster(initialSeed);
		} else {
			clusterer.cluster();
		}
		slf4jLogger.info("Phase 1 Clustering result");
		slf4jLogger.info(clusterer.toString());

		// Perform Phase 2 clusters selection.
		ClusterSelector clusterSelector = new ReCiterClusterSelector(clusterer.getClusters(), identity);
		clusterSelector.runSelectionStrategy(clusterer.getClusters(), identity);

		// Perform Mesh Heading recall improvement.
		// Use MeSH major to improve recall after phase two (https://github.com/wcmc-its/ReCiter/issues/131)
		List<ReCiterArticle> selectedArticles = new ArrayList<ReCiterArticle>();

		for (long id : clusterSelector.getSelectedClusterIds()) {
			selectedArticles.addAll(clusterer.getClusters().get(id).getArticleCluster());
		}
		
		StrategyContext meshMajorStrategyContext = new MeshMajorStrategyContext(new MeshMajorStrategy(selectedArticles, EngineParameters.getMeshCountMap()));
		clusterSelector.handleNonSelectedClusters((MeshMajorStrategyContext) meshMajorStrategyContext, clusterer.getClusters(), identity);
		Analysis analysis = Analysis.performAnalysis(clusterer, clusterSelector, parameters.getKnownPmids());
		slf4jLogger.info(clusterer.toString());
		slf4jLogger.info("Analysis for cwid=[" + identity.getCwid() + "]");
		slf4jLogger.info("Precision=" + analysis.getPrecision());
		slf4jLogger.info("Recall=" + analysis.getRecall());

		double accuracy = (analysis.getPrecision() + analysis.getRecall()) / 2;
		slf4jLogger.info("Accuracy=" + accuracy);

		slf4jLogger.info("True Positive List [" + analysis.getTruePositiveList().size() + "]: " + analysis.getTruePositiveList());
		slf4jLogger.info("True Negative List: [" + analysis.getTrueNegativeList().size() + "]: " + analysis.getTrueNegativeList());
		slf4jLogger.info("False Positive List: [" + analysis.getFalsePositiveList().size() + "]: " + analysis.getFalsePositiveList());
		slf4jLogger.info("False Negative List: [" + analysis.getFalseNegativeList().size() + "]: " + analysis.getFalseNegativeList());
		slf4jLogger.info("\n");

		for (ReCiterArticle reCiterArticle : reCiterArticles) {
			slf4jLogger.info(reCiterArticle.getArticleId() + ": " + reCiterArticle.getClusterInfo());
		}
		
		// add mesh major to analysis
		Map<Long, Map<String, Long>> clusterIdToMeshCount = new HashMap<Long, Map<String, Long>>();
		for (long id : clusterSelector.getSelectedClusterIds()) {
			Map<String, Long> meshCount = new HashMap<String, Long>();
			for (ReCiterArticle reCiterArticle : clusterer.getClusters().get(id).getArticleCluster()) {
				List<ReCiterArticleMeshHeading> meshHeadings = reCiterArticle.getMeshHeadings();
				for (ReCiterArticleMeshHeading meshHeading : meshHeadings) {
					String descriptorName = meshHeading.getDescriptorName().getDescriptorName();
					if (MeshMajorStrategy.isMeshMajor(meshHeading)) { // check if this is a mesh major. (i.e., An article A may say mesh
						if (!meshCount.containsKey(descriptorName)) {
							meshCount.put(descriptorName, 1L);
						} else {
							long count = meshCount.get(descriptorName);
							meshCount.put(descriptorName, ++count);
						}
					}
				}
			}
			clusterIdToMeshCount.put(id, meshCount);
		}
//		analysis.setClusterIdToMeshCount(clusterIdToMeshCount);
		EngineOutput engineOutput = new EngineOutput();
		engineOutput.setAnalysis(analysis);
		List<ReCiterCluster> reCiterClusters = new ArrayList<>();
		for (ReCiterCluster cluster : clusterer.getClusters().values()) {
			reCiterClusters.add(cluster);
		}
		engineOutput.setReCiterClusters(reCiterClusters);
		return engineOutput;
	}
}
