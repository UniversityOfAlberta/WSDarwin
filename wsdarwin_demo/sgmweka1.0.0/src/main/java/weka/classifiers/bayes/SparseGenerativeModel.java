/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    NaiveBayesMultinomialText.java
 *    Copyright (C) 2012 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.bayes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.bayes.SGM.SGM;
import weka.classifiers.bayes.SGM.TFIDF;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;

/**
 * @author Antti Puurula (as12{[at]}cms{[dot]}waikato{[dot]}ac{[dot]}nz)
 *
 */
public class SparseGenerativeModel extends AbstractClassifier implements WeightedInstancesHandler {

	/** For serialization */
	private static final long serialVersionUID = 2139025532014821394L;
	/** number of unique words */
	protected int m_numAttributes;
	/** number of class values */
	protected int m_numClasses;
	/** copy of header information for use in toString method */
	protected Instances m_headerInfo;

	TFIDF tfidf;
	SGM sgm;
	protected int cond_hashsize= 10000000;
	protected double prune_count_table= -10000000;
	protected double prune_count_insert= -6.0;	
	protected double min_count= 0.0;
	protected double length_scale= 0.5;
	protected double idf_lift= 20.0;
	protected double cond_unif_weight= 0.5;
	protected double cond_bg_weight= 0.05;
	protected double cond_scale= 1.2;
	protected double prior_scale= 0.5;
	protected boolean reverse_nb= false;
	/**
	 * Returns a string describing classifier
	 * @return a description suitable for
	 * displaying in the explorer/experimenter gui
	 */
	public String globalInfo() {
		return "Generative classifier for scalable and precise text classification. " +
				"Provides several modifications for improving accuracy and uses " +
				"sparse matrix representation of conditional probabilities " +
				"for reduced time complexity inference. Both training and " +
				"classification resource uses are sublinear, depending on sparsity. Hash " +
				"tables are used for model training, inverted index with MaxScore query " +
				"optimization algorithm for classification. For preprocessing " +
				"use StringToWordVector with outputWordCounts=True. \n" +
				"Reference:\n" +
				"Puurula, A. " +
				"Scalable Text Classification with Sparse Generative Modeling. " +
				"Proceedings of the 12th Pacific Rim International Conference on Artificial Intelligence." +
				"2012.";
	}

	/**
	 * Returns default capabilities of the classifier.
	 *
	 * @return      the capabilities of this classifier
	 */
	public Capabilities getCapabilities() {
		Capabilities result = super.getCapabilities();
		result.disableAll();
		result.enable(Capability.NUMERIC_ATTRIBUTES);
		result.enable(Capability.NOMINAL_CLASS);
		result.enable(Capability.MISSING_CLASS_VALUES);
		return result;
	}

	/**
	 * Generates the classifier.
	 *
	 * @param instances   set of instances serving as training data
	 * @throws Exception  if the classifier has not been generated successfully
	 */
	public void buildClassifier(Instances instances) throws Exception {
		// can classifier handle the data?
		getCapabilities().testWithFail(instances);

		instances = new Instances(instances);
		instances.deleteWithMissingClass();
		m_headerInfo= new Instances(instances, 0);
		m_numClasses= instances.numClasses();
		m_numAttributes= instances.numAttributes();

		tfidf= new TFIDF(length_scale, idf_lift);
		sgm= new SGM();
		sgm.init_model(cond_hashsize, tfidf, 1);
		sgm.prune_count_insert= prune_count_insert;
		if (reverse_nb) sgm.reverse_nb= true;
		Instance instance;
		int terms_length;
		int[] labels= {-1};
		float[] label_weights= {1};
		Enumeration<Instance> enumInsts= (Enumeration<Instance>) instances.enumerateInstances();
		while (enumInsts.hasMoreElements()) {
			instance= (Instance)enumInsts.nextElement();
			labels[0]= (int)instance.value(instance.classIndex());
			label_weights[0]= (float)instance.weight();
			terms_length= instance.numValues();
			for(int a= 0; a<instance.numValues(); a++) if(instance.index(a) == instance.classIndex()) terms_length--;
			int n= 0;
			int[] terms= new int[terms_length];
			float[] counts= new float[terms_length];
			for(int a= 0; a<instance.numValues(); a++) {
				if(instance.index(a) != instance.classIndex()) {
					if(!instance.isMissing(a)) {
						terms[n]= instance.index(a);
						counts[n++]= (float)instance.valueSparse(a);
					}
				}
			}
			sgm.add_instance(terms, counts, labels, label_weights);
		}
		tfidf.normalize(min_count);
		if (prune_count_table>-1000000.0) sgm.prune_counts(prune_count_table, cond_hashsize);
		sgm.apply_idfs();
		if (cond_scale!=1.0) sgm.scale_conditionals(cond_scale);
		sgm.normalize_model();
		sgm.smooth_conditionals(cond_unif_weight, cond_bg_weight);
		sgm.smooth_prior(prior_scale);
		sgm.prepare_inference();
	}

	public double classifyInstance(Instance instance) throws Exception {
		int terms_length= instance.numValues();
		int n= 0;
		int[] terms= new int[terms_length];
		float[] counts= new float[terms_length];
		for(int a= 0; a<instance.numValues(); a++) if(instance.index(a) == instance.classIndex()) terms_length--;
		for(int a= 0; a<instance.numValues(); a++)
			if(instance.index(a) != instance.classIndex()) {
				if(!instance.isMissing(a)) {
					terms[n]= instance.index(a);
					counts[n++]= (float)instance.valueSparse(a);
				}
			}
		int label= sgm.inference(terms, counts).indices[0];
		return label;
	}
	

	public String pruneCountTableTipText() {return "Log-count pruning value of conditional parameters after training.";}
	public double getPruneCountTable() {return (double)prune_count_table;}
	public void setPruneCountTable(double value) {prune_count_table= (float)value;}
	
	public String pruneCountInsertTipText() {return "Log-count pruning value of conditional parameters after each update.";}
	public double getPruneCountInsert() {return (double)prune_count_insert;}
	public void setPruneCountInsert(double value) {prune_count_insert= (float)value;}

	public String minCountTipText() {return "Minimum document frequency of term after training. 0= no terms pruned.";}
	public double getMinCount() {return (double)min_count;}
	public void setMinCount(double value) {min_count= (float)value;}

	public String lengthScaleTipText() {return "Length normalization parameter. Higher values for stronger length normalization.";}
	public double getLengthScale() {return (double)length_scale;}
	public void setLengthScale(double value) {length_scale= (float)value;}

	public String idfLiftTipText() {return "IDF normalization parameter. Higher values for weaker IDF normalization.";}
	public double getIdfLift() {return (double)idf_lift;}
	public void setIdfLift(double value) {idf_lift= (float)value;}

	public String condUnifWeightTipText() {return "Uniform smoothing weight for conditionals.";}
	public double getCondUnifWeight() {return (double)cond_unif_weight;}
	public void setCondUnifWeight(double value) {cond_unif_weight= (float)value;}

	public String condBgWeightTipText() {return "Background model smoothing weight for conditionals.";}
	public double getCondBgWeight() {return (double)cond_bg_weight;}
	public void setCondBgWeight(double value) {cond_bg_weight= (float)value;}

	public String condScaleTipText() {return "Scaling of unsmoothed conditional probabilities. Similar to absolute discounting.";}
	public double getCondScale() {return (double)cond_scale;}
	public void setCondScale(double value) {cond_scale= (float)value;}

	public String priorScaleTipText() {return "Scaling of prior probabilities. Equivalent to language model scaling in HMM speech recognition.";}
	public double getPriorScale() {return (double)prior_scale;}
	public void setPriorScale(double value) {prior_scale= (float)value;}
	
	public String reverseNbTipText() {return "Use Reverse Naive Bayes normalization. ";}
	public boolean getReverseNb() {return reverse_nb;}
	public void setReverseNb(boolean value) {reverse_nb= value;}
	

	public Enumeration<Option> listOptions() {
		Vector<Option> newVector = new Vector<Option>();
		newVector.addElement(new Option(pruneCountTableTipText(), "prune_count_table", 1, "-prune_count_table <double>"));
		newVector.addElement(new Option(pruneCountInsertTipText(), "prune_count_insert", 1, "-prune_count_insert <double>"));
		newVector.addElement(new Option(minCountTipText(), "min_count", 1, "-min_count <double>"));
		newVector.addElement(new Option(lengthScaleTipText(), "length_scale", 1, "-length_scale <double>"));
		newVector.addElement(new Option(idfLiftTipText(), "idf_lift", 1, "-idf_lift <double>"));
		newVector.addElement(new Option(condUnifWeightTipText(), "cond_unif_weight", 1, "-cond_unif_weight <double>"));
		newVector.addElement(new Option(condBgWeightTipText(), "cond_bg_weight", 1, "-cond_bg_weight <double>"));
		newVector.addElement(new Option(condScaleTipText(), "cond_scale", 1, "-cond_scale <double>"));
		newVector.addElement(new Option(priorScaleTipText(), "prior_scale", 1, "-prior_scale <double>"));
		newVector.addElement(new Option(reverseNbTipText(), "reverse_nb", 0, "-reverse_nb"));
		return newVector.elements();
	}
    	
	public void setOptions(String[] options) throws Exception {
		setPruneCountTable(Double.parseDouble(Utils.getOption("prune_count_table", options)));
		setPruneCountInsert(Double.parseDouble(Utils.getOption("prune_count_insert", options)));
		setMinCount(Double.parseDouble(Utils.getOption("min_count", options)));
		setLengthScale(Double.parseDouble(Utils.getOption("length_scale", options)));
		setIdfLift(Double.parseDouble(Utils.getOption("idf_lift", options)));
		setCondUnifWeight(Double.parseDouble(Utils.getOption("cond_unif_weight", options)));
		setCondBgWeight(Double.parseDouble(Utils.getOption("cond_bg_weight", options)));
		setCondScale(Double.parseDouble(Utils.getOption("cond_scale", options)));
		setPriorScale(Double.parseDouble(Utils.getOption("prior_scale", options)));
	    if (Utils.getFlag("reverse_nb", options)) reverse_nb= true;
	}
	
	public String[] getOptions() {
		ArrayList<String> options = new ArrayList<String>();
		options.add("-prune_count_table"); options.add("" + getPruneCountTable());
		options.add("-prune_count_insert"); options.add("" + getPruneCountInsert());
		options.add("-min_count"); options.add("" + getMinCount());
		options.add("-length_scale"); options.add("" + getLengthScale());
		options.add("-idf_lift"); options.add("" + getIdfLift());
		options.add("-cond_unif_weight"); options.add("" + getCondUnifWeight());
		options.add("-cond_bg_weight"); options.add("" + getCondBgWeight());
		options.add("-cond_scale"); options.add("" + getCondScale());
		options.add("-prior_scale"); options.add("" + getPriorScale());
		if (reverse_nb) options.add("-reverse_nb");
		return options.toArray(new String[1]);
	}

	/**
	 * Returns a textual description of this classifier.
	 * 
	 * @return a textual description of this classifier.
	 */
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("sgm.model.cond_lprobs.size():" +sgm.model.cond_lprobs.size()+"\n");
		result.append("sgm.model.cond_bgs.size():" +sgm.model.cond_bgs.size()+"\n");
		result.append("sgm.model.prior_lprobs.size():" +sgm.model.prior_lprobs.size()+"\n");
		return result.toString();
	}

	/**
	 * Returns the revision string.
	 * 
	 * @return            the revision
	 */
	public String getRevision() {
		return RevisionUtils.extract("$Revision: 8034 $");
	}

	/**
	 * Main method for testing this class.
	 *
	 * @param args        the options
	 */
	public static void main(String[] args) {
		runClassifier(new SparseGenerativeModel(), args);
	}
}

