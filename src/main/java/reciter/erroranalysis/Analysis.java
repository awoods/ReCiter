package reciter.erroranalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.cluster.Clusterer;
import reciter.algorithm.cluster.model.ReCiterCluster;
import reciter.algorithm.cluster.targetauthor.ClusterSelector;
import reciter.model.article.ReCiterArticle;

/**
 * Class that performs analysis such as calculating precision and recall.
 * @author jil3004
 *
 */
public class Analysis {

	private int truePos;
	private int trueNeg;
	private int falseNeg;
	private int falsePos;
	private int goldStandardSize;
	private int selectedClusterSize;
	private List<Long> truePositiveList = new ArrayList<Long>();
	private List<Long> trueNegativeList = new ArrayList<Long>();
	private List<Long> falsePositiveList = new ArrayList<Long>();
	private List<Long> falseNegativeList = new ArrayList<Long>();
	
	private Map<String, Integer> truePositiveJournalCount = new HashMap<String, Integer>();
	private Map<String, Integer> trueNegativeJournalCount = new HashMap<String, Integer>();
	private Map<String, Integer> falsePositiveJournalCount = new HashMap<String, Integer>();
	private Map<String, Integer> falseNegativeJournalCount = new HashMap<String, Integer>();
	
	private List<AnalysisObject> analysisObjectList = new ArrayList<AnalysisObject>();

	private double precision;
	private double recall;
	
	private static final Logger slf4jLogger = LoggerFactory.getLogger(Analysis.class);	

	public Analysis() {}

	/**
	 * Assign gold standard to each ReCiterArticle.
	 * @param reCiterArticles
	 * @param cwid
	 */
	public static void assignGoldStandard(List<ReCiterArticle> reCiterArticles, List<Long> pmids) {
		Set<Long> pmidSet = new HashSet<Long>();
		for (long pmid : pmids) {
			pmidSet.add(pmid);
		}
		for (ReCiterArticle reCiterArticle : reCiterArticles) {
			if (pmidSet.contains(reCiterArticle.getArticleId())) {
				reCiterArticle.setGoldStandard(1);
			} else {
				reCiterArticle.setGoldStandard(0);
			}
		}
	}
	
	/**
	 * Single Selection.
	 * @param finalCluster
	 * @param selection
	 * @param cwid
	 * @return
	 */
	public static Analysis performAnalysis(Map<Integer, ReCiterCluster> finalCluster, int selection, List<Long> goldStandardPmids) {
		Analysis analysis = new Analysis();
		slf4jLogger.info("Gold Standard: " + goldStandardPmids);

		analysis.setGoldStandardSize(goldStandardPmids.size());
		analysis.setSelectedClusterSize(finalCluster.get(selection).getArticleCluster().size());
		int numTruePos = 0;

		// get number of true positives.


		for (ReCiterArticle reCiterArticle : finalCluster.get(selection).getArticleCluster()) {
			long pmid = reCiterArticle.getArticleId();
//			StatusEnum statusEnum;
			if (goldStandardPmids.contains(pmid)) {
				numTruePos++;
//				statusEnum = StatusEnum.TRUE_POSITIVE;
			} else {
				analysis.falsePositiveList.add(pmid);
//				statusEnum = StatusEnum.FALSE_POSITIVE;
			}

			//			AnalysisObject analysisObject = AnalysisTranslator.translate(reCiterArticle, statusEnum, cwid, targetAuthor, isClusterOriginator)
		}
		analysis.setTruePos(numTruePos);


		return analysis;
	}

	/**
	 * List of selections.
	 * @param finalCluster
	 * @param selection
	 * @param cwid
	 * @return
	 */
	public static Analysis performAnalysis(Clusterer reCiterClusterer, ClusterSelector clusterSelector, List<Long> goldStandardPmids) {
		
//		AnalysisReCiterCluster analyisReCiterCluster = new AnalysisReCiterCluster();
//	    Map<String, Integer> authorCount = 
//	    		analyisReCiterCluster.getTargetAuthorNameCounts(
//	    				reCiterClusterer.getReCiterArticles(), 
//	    				reCiterClusterer.getTargetAuthor());
//	    
//	    for (Entry<String, Integer> entry : authorCount.entrySet()) {
//	      slf4jLogger.info(entry.getKey() + ": " + entry.getValue());
//	    }
//	    slf4jLogger.info("Number of different author names: " + authorCount.size());
	    
		Map<Long, ReCiterCluster> finalCluster = reCiterClusterer.getClusters();
		Set<Long> selection = clusterSelector.getSelectedClusterIds();
		String cwid = reCiterClusterer.getIdentity().getCwid();
		
		Analysis analysis = new Analysis();
		slf4jLogger.info("Gold Standard [" + goldStandardPmids.size() + "]: " + goldStandardPmids);

		analysis.setGoldStandardSize(goldStandardPmids.size());

		// Combine all articles into a single list.
		List<ReCiterArticle> articleList = new ArrayList<ReCiterArticle>();
		for (long s : selection) {
			for (ReCiterArticle reCiterArticle : finalCluster.get(s).getArticleCluster()) {
				articleList.add(reCiterArticle);
			}
		}

		analysis.setSelectedClusterSize(articleList.size());
//		int numTruePos = 0;
//		// get number of true positives.
//		for (ReCiterArticle reCiterArticle : articleList) {
//			int pmid = reCiterArticle.getArticleId();
//			if (goldStandardPmids.contains(Integer.toString(pmid))) {
//				numTruePos++;
//			} else {
//				analysis.falsePositiveList.add(pmid);
//			}
//		}

		for (Entry<Long, ReCiterCluster> entry : finalCluster.entrySet()) {
			for (ReCiterArticle reCiterArticle : entry.getValue().getArticleCluster()) {
				long pmid = reCiterArticle.getArticleId();
				StatusEnum statusEnum;
				if (articleList.contains(reCiterArticle) && goldStandardPmids.contains(pmid)) {
					analysis.getTruePositiveList().add(pmid);
					
					if (reCiterArticle.getJournal() != null && reCiterArticle.getJournal().getJournalTitle() != null) {
						if (!analysis.getTruePositiveJournalCount().containsKey(reCiterArticle.getJournal().getJournalTitle())) {
							analysis.getTruePositiveJournalCount().put(reCiterArticle.getJournal().getJournalTitle(), 1);
						} else {
							int count = analysis.getTruePositiveJournalCount().get(reCiterArticle.getJournal().getJournalTitle());
							analysis.getTruePositiveJournalCount().put(reCiterArticle.getJournal().getJournalTitle(), ++count);
						}
					}
					statusEnum = StatusEnum.TRUE_POSITIVE;
				} else if (articleList.contains(reCiterArticle) && !goldStandardPmids.contains(pmid)) {
					
					analysis.getFalsePositiveList().add(pmid);
					statusEnum = StatusEnum.FALSE_POSITIVE;
//					slf4jLogger.info("year diff [False Positive]: " + reCiterArticle.getArticleId() + ": " + reCiterClusterer.computeYearDiscrepancy(
//							reCiterArticle, reCiterClusterer.getTargetAuthor()));
					if (reCiterArticle.getJournal() != null && reCiterArticle.getJournal().getJournalTitle() != null) {
						if (!analysis.getFalsePositiveJournalCount().containsKey(reCiterArticle.getJournal().getJournalTitle())) {
							analysis.getFalsePositiveJournalCount().put(reCiterArticle.getJournal().getJournalTitle(), 1);
						} else {
							int count = analysis.getFalsePositiveJournalCount().get(reCiterArticle.getJournal().getJournalTitle());
							analysis.getFalsePositiveJournalCount().put(reCiterArticle.getJournal().getJournalTitle(), ++count);
						}
					}
					
				} else if (!articleList.contains(reCiterArticle) && goldStandardPmids.contains(pmid)) {
					analysis.getFalseNegativeList().add(pmid);
					statusEnum = StatusEnum.FALSE_NEGATIVE;
//					slf4jLogger.info("year diff [False Negative]: " + reCiterArticle.getArticleId() + ": " + reCiterClusterer.computeYearDiscrepancy(
//							reCiterArticle, reCiterClusterer.getTargetAuthor()));
					if (reCiterArticle.getJournal() != null && reCiterArticle.getJournal().getJournalTitle() != null) {
						if (!analysis.getFalseNegativeJournalCount().containsKey(reCiterArticle.getJournal().getJournalTitle())) {
							analysis.getFalseNegativeJournalCount().put(reCiterArticle.getJournal().getJournalTitle(), 1);
						} else {
							int count = analysis.getFalseNegativeJournalCount().get(reCiterArticle.getJournal().getJournalTitle());
							analysis.getFalseNegativeJournalCount().put(reCiterArticle.getJournal().getJournalTitle(), ++count);
						}
					}
					
				} else {
					analysis.getTrueNegativeList().add(pmid);
					statusEnum = StatusEnum.TRUE_NEGATIVE;
//					slf4jLogger.info("year diff [True Negative]: " + reCiterArticle.getArticleId() + ": " + reCiterClusterer.computeYearDiscrepancy(
//							reCiterArticle, reCiterClusterer.getTargetAuthor()));
					if (reCiterArticle.getJournal() != null && reCiterArticle.getJournal().getJournalTitle() != null) {
						if (!analysis.getTrueNegativeJournalCount().containsKey(reCiterArticle.getJournal().getJournalTitle())) {
							analysis.getTrueNegativeJournalCount().put(reCiterArticle.getJournal().getJournalTitle(), 1);
						} else {
							int count = analysis.getTrueNegativeJournalCount().get(reCiterArticle.getJournal().getJournalTitle());
							analysis.getTrueNegativeJournalCount().put(reCiterArticle.getJournal().getJournalTitle(), ++count);
						}
					}
				}

				boolean isClusterOriginator = false;
				long clusterOriginator = entry.getValue().getClusterOriginator();
				if (pmid == clusterOriginator) {
					isClusterOriginator = true;
				}

				boolean isArticleSelected = false;
				if (articleList.contains(reCiterArticle)) {
					isArticleSelected = true;
				}
				
				AnalysisObject analysisObject = AnalysisTranslator.translate(
						reCiterArticle, 
						statusEnum, 
						cwid, 
						reCiterClusterer.getIdentity(), 
						isClusterOriginator, 
						entry.getValue().getClusterID(), 
						entry.getValue().getArticleCluster().size(), 
						isArticleSelected);
				
				analysis.getAnalysisObjectList().add(analysisObject);
			}
		}

		analysis.setTruePos(analysis.getTruePositiveList().size());
		analysis.setTrueNeg(analysis.getTrueNegativeList().size());
		analysis.setFalseNeg(analysis.getFalseNegativeList().size());
		analysis.setFalsePos(analysis.getFalsePositiveList().size());
		analysis.setPrecision(analysis.getPrecision());
		analysis.setRecall(analysis.getRecall());
		return analysis;
	}

	public double getPrecision() {
		if (selectedClusterSize == 0) 
			return 0;
		return (double) truePos / selectedClusterSize;
	}

	public double getRecall() {
		if (goldStandardSize == 0)
			return 0;
		return (double) truePos / goldStandardSize;
	}

	public int getTruePos() {
		return truePos;
	}

	public void setTruePos(int truePos) {
		this.truePos = truePos;
	}

	public int getGoldStandardSize() {
		return goldStandardSize;
	}

	public void setGoldStandardSize(int goldStandardSize) {
		this.goldStandardSize = goldStandardSize;
	}

	public int getSelectedClusterSize() {
		return selectedClusterSize;
	}

	public void setSelectedClusterSize(int selectedClusterSize) {
		this.selectedClusterSize = selectedClusterSize;
	}

	public List<Long> getFalsePositiveList() {
		return falsePositiveList;
	}

	public void setFalsePositiveList(List<Long> falsePositiveList) {
		this.falsePositiveList = falsePositiveList;
	}

	public List<AnalysisObject> getAnalysisObjectList() {
		return analysisObjectList;
	}

	public void setAnalysisObjectList(List<AnalysisObject> analysisObjectList) {
		this.analysisObjectList = analysisObjectList;
	}

	public List<Long> getFalseNegativeList() {
		return falseNegativeList;
	}

	public void setFalseNegativeList(List<Long> falseNegativeList) {
		this.falseNegativeList = falseNegativeList;
	}

	public int getFalseNeg() {
		return falseNeg;
	}

	public void setFalseNeg(int falseNeg) {
		this.falseNeg = falseNeg;
	}

	public int getTrueNeg() {
		return trueNeg;
	}

	public void setTrueNeg(int trueNeg) {
		this.trueNeg = trueNeg;
	}

	public int getFalsePos() {
		return falsePos;
	}

	public void setFalsePos(int falsePos) {
		this.falsePos = falsePos;
	}

	public List<Long> getTruePositiveList() {
		return truePositiveList;
	}

	public void setTruePositiveList(List<Long> truePositiveList) {
		this.truePositiveList = truePositiveList;
	}

	public List<Long> getTrueNegativeList() {
		return trueNegativeList;
	}

	public void setTrueNegativeList(List<Long> trueNegativeList) {
		this.trueNegativeList = trueNegativeList;
	}

	public Map<String, Integer> getTruePositiveJournalCount() {
		return truePositiveJournalCount;
	}

	public void setTruePositiveJournalCount(Map<String, Integer> truePositiveJournalCount) {
		this.truePositiveJournalCount = truePositiveJournalCount;
	}

	public Map<String, Integer> getTrueNegativeJournalCount() {
		return trueNegativeJournalCount;
	}

	public void setTrueNegativeJournalCount(Map<String, Integer> trueNegativeJournalCount) {
		this.trueNegativeJournalCount = trueNegativeJournalCount;
	}

	public Map<String, Integer> getFalsePositiveJournalCount() {
		return falsePositiveJournalCount;
	}

	public void setFalsePositiveJournalCount(Map<String, Integer> falsePositiveJournalCount) {
		this.falsePositiveJournalCount = falsePositiveJournalCount;
	}

	public Map<String, Integer> getFalseNegativeJournalCount() {
		return falseNegativeJournalCount;
	}

	public void setFalseNegativeJournalCount(Map<String, Integer> falseNegativeJournalCount) {
		this.falseNegativeJournalCount = falseNegativeJournalCount;
	}

	public void setPrecision(double precision) {
		this.precision = precision;
	}

	public void setRecall(double recall) {
		this.recall = recall;
	}

	@Override
	public String toString() {
		return "Analysis [truePos=" + truePos + ", trueNeg=" + trueNeg + ", falseNeg=" + falseNeg + ", falsePos="
				+ falsePos + ", goldStandardSize=" + goldStandardSize + ", selectedClusterSize=" + selectedClusterSize
				+ ", truePositiveList=" + truePositiveList + ", trueNegativeList=" + trueNegativeList
				+ ", falsePositiveList=" + falsePositiveList + ", falseNegativeList=" + falseNegativeList
				+ ", truePositiveJournalCount=" + truePositiveJournalCount + ", trueNegativeJournalCount="
				+ trueNegativeJournalCount + ", falsePositiveJournalCount=" + falsePositiveJournalCount
				+ ", falseNegativeJournalCount=" + falseNegativeJournalCount + ", analysisObjectList="
				+ analysisObjectList + ", precision=" + precision + ", recall=" + recall + "]";
	}
	
	
}
