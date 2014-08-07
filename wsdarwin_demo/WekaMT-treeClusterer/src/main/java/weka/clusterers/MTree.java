package weka.clusterers;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import weka.classifiers.rules.DecisionTableHashKey;
import weka.core.Attribute;
//import weka.clusterers.SimpleKMeans.KMeansClusterTask;
import weka.core.Capabilities;
import weka.core.DenseInstance;
import weka.core.DistanceFunction;
import weka.core.EuclideanDistance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.Capabilities.Capability;
import weka.filters.unsupervised.attribute.Center;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
/**
 * <!-- globalinfo-start --> Cluster data using the MTree algorithm
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * * 
 * <pre>
 * -N &lt;num&gt;
 *  number of clusters.
 *  (default 100).
 * </pre>
 * 
 * 
 * <pre>
 * -M
 *  Replace missing values with mean/mode.
 * </pre>
 * 
 * 
 * <pre>
 * -A &lt;classname and options&gt;
 *  Distance function to be used for instance comparison
 *  (default weka.core.EuclidianDistance)
 * </pre>
 * 
 * <!-- options-end -->
 */ 

class Node implements Serializable {

	/**
	 * A Node represents a cluster
	 */
	private static final long serialVersionUID = 3838507051790773553L;
	public int nrKeys = 0;
	public boolean isLeaf = true;
	public ArrayList <Double> radix; 
	public ArrayList<Node> routes; 
	public ArrayList<Instance> instances;
	public Instance parent;


	public Node(int size, Instances train)
	{
		radix = new ArrayList<Double>(size);  
		routes = new ArrayList<Node>(size);   //the children
		instances = new ArrayList<Instance>(size); 
		nrKeys = 0;
		isLeaf = true;

		for (int i = 0; i < size; i++) {
			this.radix.add(new Double(0.0));
			this.routes.add(new Node());
			Instance newInst = new DenseInstance(train.numAttributes());
			newInst.setDataset(train);
			this.instances.add(newInst);
		}
	}

	public Node() {
		radix = new ArrayList<Double>();  
		routes = new ArrayList<Node>();   
		instances = new ArrayList<Instance>();
		nrKeys = 0;
		isLeaf = true;
	}

}

class InstanceRadix implements Serializable
{
	private static final long serialVersionUID = 3838507051790773554L;
	public Instance center;
	public double radix;
	public boolean isPlaceHolder = false;
}

class PriorityQueueElement
{
	private static final long serialVersionUID = 3838507051790773555L;
	Node node; // a node from the M-tree
	Instance parent; // the parent object of the node in the tree
	double dmin; // the minimum distance from which an object from this node's children can be found from Q
}

class SplitOutput implements Serializable{

	private static final long serialVersionUID = -8395584129162721591L;

	public ArrayList<Node> clusters = new ArrayList<Node>();
	public ArrayList<Instance> centers = new ArrayList<Instance>();
	public ArrayList <Double> radix = new ArrayList<Double>();
}

class MTreeBean implements Serializable{

	/**
	 * The class containing the root node
	 */
	private static final long serialVersionUID = 3498776313092608804L;
	public Node root;


	public MTreeBean()
	{
		root = new Node();
	}	
}

/**
 * Clustering algorithm based on MTree data structure.
 */
public class MTree extends RandomizableClusterer implements
NumberOfClustersRequestable, WeightedInstancesHandler,
TechnicalInformationHandler {
	int index2 = 0;	
	private static Instances train;
	protected DistanceFunction m_DistanceFunction;
	private DecimalFormat Format = new DecimalFormat("#0.00");
	long stopTime = 0;
	long startTime = 0;
	MTreeBean mTree = new MTreeBean();

	/** for serialization. */
	static final long serialVersionUID = -3235809600124455376L;

	/**
	 * replace missing values in training instances.
	 */
	private ReplaceMissingValues m_ReplaceMissingFilter;

	/**
	 * number of clusters to generate.
	 */
	private int m_NumClusters = 100;

	/**
	 * holds the cluster centroids.
	 */
	private Instances m_ClusterCentroids;

	/**
	 * Replace missing values globally?
	 */
	private boolean m_dontReplaceMissing = true;

	/**
	 * The number of instances in each cluster.
	 */
	private int[] m_ClusterSizes;// = new int[10000];

	/**
	 * Holds the squared errors for all clusters.
	 */
	private double[] m_squaredErrors;

	/**
	 * Preserve order of instances.
	 */
	private boolean m_PreserveOrder = true;

	/**
	 * Assignments obtained.
	 */
	protected int[] m_Assignments = null;

	/** whether to use fast calculation of distances (using a cut-off). */
	protected boolean m_FastDistanceCalc = false;

	protected int m_executionSlots = 1;

	/** For parallel execution mode */
	protected transient ExecutorService m_executorPool;

	public int numberOfIterations = 0;
	public int INFINITY = 10000000;
	public ArrayList<Instance> rangeQueryInstances = new ArrayList<Instance>();

	public MTree() { 

		super();

		m_SeedDefault = 10;
		setSeed(m_SeedDefault);
	}


	/**
	 * Returns a string describing this clusterer
	 * 
	 * @return a description of the evaluator suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String globalInfo() {
		return "Cluster data using the MTree algorithm. Can use either "
				+ " the Euclidean distance (default) or the Manhattan distance."
				+ " If the Manhattan distance is used, then centroids are computed "
				+ " as the component-wise median rather than mean." +
				"\n article{DBLP:journals/informaticaSI/MihaescuB12" +
				"author    = {Marian Cristian Mihaescu Dumitru Dan Burdescu}" +
				"title     = {Using M Tree Data Structure as Unsupervised Classification Method}," +
				"journal   = {Informatica (Slovenia)}," +
				"volume    = {36}," +
				"number    = {2}," +
				"year      = {2012}," +
				"pages     = {153-160}," +
				"ee        = {http://www.informatica.si//PDF//36-2/05_Mihaescu\\%20-\\%20Using\\%20M\\%20Tree\\%20Data\\%20Structure\\%20as\\%20Unsupervised\\%20Classification\\%20Method.pdf}," +
				"bibsource = {DBLP, http://dblp.uni-trier.de}}";
	}

	/**
	 *
	 * @return      the capabilities of this clusterer
	 */
	public Capabilities getCapabilities() {

		Capabilities result = super.getCapabilities();
		result.disableAll();
		result.enable(Capability.NO_CLASS);

		// attributes
		result.enable(Capability.NUMERIC_ATTRIBUTES);

		return result;
	}

	/**
	 * Returns an enumeration describing the available options.
	 * 
	 * @return an enumeration of all the available options.
	 */
	@Override
	public Enumeration listOptions() {
		Vector result = new Vector();

		result.addElement(new Option("\tnumber of clusters.\n" + "\t(default 100).",
				"N", 1, "-N <num>"));

		result.add(new Option("\tDistance function to use.\n"
				+ "\t(default: weka.core.EuclideanDistance)", "A", 1,
				"-A <classname and options>"));

		Enumeration en = super.listOptions();
		while (en.hasMoreElements()) {
			result.addElement(en.nextElement());
		}

		return result.elements();
	}

	public void mTreeInsert(MTreeBean tree, Instance inst) {
		/**
		 * we will perform the split for the root node if the root is a Leaf and
		 * EM algorithm decides to split the node in at least 2 other nodes.
		 * The number of nodes in which we can split the root must be less or equal
		 * than MaxRootSize  
		 */
		if(tree.root.isLeaf && tree.root.nrKeys > 0 && tree.root.instances.size() > 0) {

			ClusterEvaluation eval = emClustering(tree.root);
			if(IsClusterEvaluationValid(eval)) {
				int nrClustersObtained = eval.getNumClusters();
				if(nrClustersObtained > 1) //we must split the node
				{
					Node newRoot = new Node(m_NumClusters, train);

					newRoot.nrKeys = 0;
					newRoot.isLeaf = false;

					SplitOutput splitOutput;
					int numberOfClustersAfterSplit = eval.getNumClusters();

					if(numberOfClustersAfterSplit > m_NumClusters) {
						numberOfClustersAfterSplit = m_NumClusters;
						splitOutput = splitNode(tree.root, numberOfClustersAfterSplit);
					}
					else
						splitOutput = splitNode(tree.root, eval);

					for(int index = 0; index < splitOutput.clusters.size(); index++)				
					{
						newRoot.routes.add(index, splitOutput.clusters.get(index));
						newRoot.instances.add(index, splitOutput.centers.get(index));
						newRoot.radix.add(index, splitOutput.radix.get(index));

						newRoot.nrKeys++;
					}

					tree.root = newRoot;
				}
			}
		}
		m_Node_Insert_Nonfull(tree.root, inst);	
	}

	public void m_Node_Insert_Nonfull(Node node, Instance inst) {

		SplitOutput splitOut;
		while (!node.isLeaf){
			int i,idx = 0;
			Double d = Double.MAX_VALUE;

			if(node.nrKeys > 0) {

				Double min = m_DistanceFunction.distance(inst, node.instances.get(0));

				for (i = 0; i < node.nrKeys; i++) {
					if (node.instances.get(i) == null)
						node.instances.set(i, new DenseInstance(train.numAttributes()));

					Instance indexInstance = node.instances.get(i);
					try
					{
						d = m_DistanceFunction.distance(inst, indexInstance);

						if (d < min){
							min = d;
							idx = i;
						}
					}
					catch(ArrayIndexOutOfBoundsException e)
					{
						i++;
					}
				}
			}    

			if  (node.nrKeys < m_NumClusters && node.nrKeys > 0 && node.instances.size() > 0) { 
				ClusterEvaluation eval = emClustering(node.routes.get(idx));
				if(IsClusterEvaluationValid(eval)) {
					int nrClustersObtained = eval.getNumClusters();
					if( (node.routes.get(idx).nrKeys > 0) && (nrClustersObtained > 1) ) {

						if(nrClustersObtained + node.nrKeys - 1 <= m_NumClusters)
							splitOut = splitNode(node.routes.get(idx), eval);
						else
							splitOut = splitNode(node.routes.get(idx), m_NumClusters - node.nrKeys + 1); //minim 2 clustere.

						for(int index = 0; index < splitOut.clusters.size(); index++)				
							if(index == 0)
							{
								node.routes.set(idx, splitOut.clusters.get(index));
								node.instances.set(idx, splitOut.centers.get(index));
								node.radix.set(idx, splitOut.radix.get(index));
							}
							else
							{
								node.routes.add(idx + index, splitOut.clusters.get(index));
								node.instances.add(idx + index, splitOut.centers.get(index));
								node.radix.add(idx + index, splitOut.radix.get(index));
								node.nrKeys++;
							}
					}
				}
			}
			node = node.routes.get(idx);
		}

		node.instances.add(node.nrKeys, inst);
		node.nrKeys++;
	}

	public boolean IsClusterEvaluationValid(ClusterEvaluation eval)
	{
		if(eval.getNumClusters() > 0)
		{
			double[] assignments = eval.getClusterAssignments();
			for(int i = 0; i < assignments.length - 1; i++)
				if(assignments[i] != assignments[i+1])
					return true;
		}
		return false;
	}
	private Instances arrayInstanceToInstances(ArrayList<Instance> array) {

		//initialize the instances using first 2 elements of train because
		//Instances doesen't have a more convenient constructor in this situation

		Instances instancesForSplit =  new Instances(train, 0, 0);
		for(int i = 0; i < array.size(); i++)
			instancesForSplit.add(array.get(i));

		return instancesForSplit;
	}

	private ArrayList<Node> getClustersFromEM(double[] clusterAssignments, Node parentNode, int nrClusters) {

		ArrayList<Node> clusters = new ArrayList<Node>(nrClusters);

		for(int i = 0; i < nrClusters; i++)
			clusters.add(new Node());

		for(int i = 0; i < parentNode.nrKeys; i++) {

			int index = (int)clusterAssignments[i];
			clusters.get(index).instances.add(parentNode.instances.get(i));

			if (!parentNode.isLeaf)
				clusters.get(index).routes.add(parentNode.routes.get(i));

			clusters.get(index).nrKeys++;
		}

		for(int i = 0; i < clusters.size(); i++)
		{
			if(clusters.get(i).nrKeys < 1)
			{
				clusters.remove(i);
			}
		}
		return clusters;
	}

	/**
	 * This method evaluates a node of the MTree using
	 * EM algorithm to decide if it can be split or not,
	 * and in the first case which clusters will result
	 * from the split operation. 
	 * 
	 */
	private ClusterEvaluation emClustering(Node node) {
		numberOfIterations++;

		// create new cluster and evaluator objects
		ClusterEvaluation eval = new ClusterEvaluation();
		EM clusterer = new EM();                                

		//obtain the instances as an Instances object from the array of Instances.
		//this operation is needed for the buildClusterer method
		Instances nodeInstances = arrayInstanceToInstances(node.instances);

		try {
			if(nodeInstances.numInstances() > 0)
			{
				clusterer.setNumClusters(-1);
				clusterer.buildClusterer(nodeInstances);
				eval.setClusterer(clusterer); // the cluster to evaluate
				eval.evaluateClusterer(nodeInstances);
			}
		}                       
		catch(java.lang.IllegalArgumentException e2)
		{
			e2.printStackTrace();
		} 
		catch (Exception e) {
			e.printStackTrace();
		}
		return eval;
	}

	public SplitOutput splitNode(Node node, ClusterEvaluation eval) {
		SplitOutput splitOutput = new SplitOutput();
		try{
			splitOutput.clusters = getClustersFromEM(eval.getClusterAssignments(), node, eval.getNumClusters());

			if(splitOutput.clusters.size() > 0)
			{
				for(int i = 0; i < splitOutput.clusters.size(); i++) 
				{
					InstanceRadix instanceRadix = chooseCenter(splitOutput.clusters.get(i));
					splitOutput.centers.add(instanceRadix.center);
					splitOutput.radix.add(instanceRadix.radix);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}                                 

		return splitOutput;
	}

	public SplitOutput splitNode(Node node,  int nrOfClusters) {

		ClusterEvaluation eval = new ClusterEvaluation();
		EM clusterer = new EM();                                
		SplitOutput splitOutput = new SplitOutput();
		//obtain the instances as an Instances object from the array of Instances.
		//this operation is needed for the buildClusterer method
		Instances nodeInstances = arrayInstanceToInstances(node.instances);
		try{
			numberOfIterations++;
			clusterer.setNumClusters(nrOfClusters);
			clusterer.buildClusterer(nodeInstances);

			eval.setClusterer(clusterer); // the cluster to evaluate
			eval.evaluateClusterer(nodeInstances);

			splitOutput.clusters = getClustersFromEM(eval.getClusterAssignments(), node, eval.getNumClusters());

			for(int i = 0; i < splitOutput.clusters.size(); i++) {
				InstanceRadix instanceRadix = chooseCenter(splitOutput.clusters.get(i));
				splitOutput.centers.add(instanceRadix.center);
				splitOutput.radix.add(instanceRadix.radix);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}                                 
		return splitOutput;
	}

	public InstanceRadix chooseCenter(Node node) {
		ArrayList<Double> radix = new ArrayList<Double>();

		for (int i = 0; i < node.instances.size(); i++) 
			radix.add(0.0);

		//the biggest distance to the others for each instance 
		for (int i = 0; i < node.instances.size(); i++){
			for (int j = 0; j < node.instances.size(); j++){
				if (m_DistanceFunction.distance(node.instances.get(i),node.instances.get(j)) > radix.get(i))
					radix.set(i, m_DistanceFunction.distance(node.instances.get(i),node.instances.get(j))) ;
			}
		}

		int indexMinRadix = 0;

		if(radix.size() > 0)
		{
			//minimum from the computed distances
			Double minRadix = radix.get(0);

			for (int i = 0; i < node.nrKeys; i++){
				if (radix.get(i) < minRadix) {
					minRadix = radix.get(i);
					indexMinRadix = i;
				}
			}
		}

		InstanceRadix instanceRadix = new InstanceRadix();
		instanceRadix.center = node.instances.get(indexMinRadix);
		instanceRadix.radix = radix.get(indexMinRadix);

		return instanceRadix;
	}	

	public void m_Tree_Display(MTreeBean tree, BufferedWriter bw) {
		try {						
			m_Node_Print(tree.root,0, bw);
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}				
	}

	public void m_Node_Print(Node node, int level, BufferedWriter writingFile) throws IOException{
		for (int i=0; i < node.nrKeys; i++){
			for (int j=0; j < level; j++){
				writingFile.write("  ");
			}
		}
	}

	public double getSumOfSquaredErrors(MTreeBean mTree) {
		double sse = 0;
		double distance;

		int index1 = 0;
		for(int k = 0; k < mTree.root.nrKeys; k++)
		{
			Instance centroid = mTree.root.instances.get(k);
			if(mTree.root.routes.size() > 0)
			{
				Node node = mTree.root.routes.get(k);

				for(int i = 0; i < node.nrKeys; i++) {
					distance = m_DistanceFunction.distance(centroid, node.instances.get(i));
					if (m_DistanceFunction instanceof EuclideanDistance) 
					{
						sse += (distance * distance);
						index1++;
					}

				}
			}
		}
		return sse;
	}

	/**
	 * 
	 * @return the cluster centroids
	 */
	public Instances getClusterCentroids() {
		return m_ClusterCentroids;
	}

	@Override
	public void buildClusterer(Instances data) throws Exception {

		mTree = new MTreeBean();
		// can clusterer handle the data?
		getCapabilities().testWithFail(data);
		m_ClusterCentroids = new Instances(data, 0 , 1);
		m_ClusterCentroids.delete();
		numberOfIterations = 0;
		this.train = data;
		m_DistanceFunction = new EuclideanDistance();
		m_DistanceFunction.setInstances(train);

		startTime = System.currentTimeMillis();

		System.out.println("Insert start");
		for (int i = 0; i < train.numInstances(); i++){
			mTreeInsert(mTree, train.instance(i));
		}

		System.out.println("Insert end");

		int[] clusterAssignments = new int[data.numInstances()];
		m_squaredErrors = new double[mTree.root.instances.size()];
		// calculate errors

		for (int i = 0; i < data.numInstances(); i++) {
			clusterAssignments[i] = clusterProcessedInstance(data.instance(i), true, true);
		}
		m_ClusterSizes = new int[mTree.root.nrKeys];

		for(int i = 0; i <  mTree.root.nrKeys; i++) 
		{
			m_ClusterCentroids.add(mTree.root.instances.get(i));
			if(mTree.root.routes.size() > 0)
			{
				m_ClusterSizes[i] = (mTree.root.routes.get(i).nrKeys);
			}
		}

		if (m_PreserveOrder)
			m_Assignments = clusterAssignments;

		m_NumClusters = mTree.root.nrKeys;
		stopTime = System.currentTimeMillis();
	}

	/**
	 * return a string describing this clusterer.
	 * 
	 * @return a description of the clusterer as a string
	 */
	@Override
	public String toString() {
		StringBuffer temp = new StringBuffer();
		temp.append("Nr clusters: " + mTree.root.nrKeys + "\n");
		temp.append("Key:");
		m_ClusterSizes = new int[mTree.root.nrKeys];
		for(int i = 0; i <  mTree.root.nrKeys; i++) {
			//m_ClusterCentroids.add(mTree.root.instances.get(i));//set(i, mTree.root.instances.get(i));
			if(mTree.root.routes.size() > 0) {
				m_ClusterSizes[i] = (mTree.root.routes.get(i).nrKeys);

				temp.append(mTree.root.instances.get(i) + " "); //mTree.root.routes.get(i).nrKeys + " ");
			}
		}
		if (m_ClusterCentroids == null) {
			return "No clusterer built yet!";
		}

		temp.append("\nMTrees \n======\n");
		temp.append("\nNumber of instances: " + train.numInstances());
		if (!m_FastDistanceCalc) {
			temp.append("\n");
			if (m_DistanceFunction instanceof EuclideanDistance) {
				temp.append("Within cluster sum of squared errors: "
						+ Utils.sum(m_squaredErrors));
			} else {
				temp.append("Sum of within cluster distances: "
						+ Utils.sum(m_squaredErrors));
			}
		}

		//temp.append("\n Number of iterations:" + numberOfIterations);

		return temp.toString();
	}

	/**
	 * Main method for executing this class.
	 * 
	 * @param args use -h to list all parameters
	 */

	public static void main(String[] args) {
		runClusterer(new MTree(), args);
	}


	@Override
	public TechnicalInformation getTechnicalInformation() {
		TechnicalInformation technicalInformation = new  TechnicalInformation(TechnicalInformation.Type.ARTICLE);
		technicalInformation.setValue(TechnicalInformation.Field.AUTHOR, "Marian Cristian Mihaescu and Dumitru Dan Burdescu");
		technicalInformation.setValue(TechnicalInformation.Field.TITLE, "Using M Tree Data Structure as Unsupervised Classification Method");
		technicalInformation.setValue(TechnicalInformation.Field.VOLUME, "36");
		technicalInformation.setValue(TechnicalInformation.Field.NUMBER, "2");
		technicalInformation.setValue(TechnicalInformation.Field.YEAR, "2012");
		technicalInformation.setValue(TechnicalInformation.Field.PAGES, "153-160");
		technicalInformation.setValue(TechnicalInformation.Field.JOURNAL, "Informatica (Slovenia)");

		return technicalInformation;
	}


	/**
	 * Start the pool of execution threads
	 */
	protected void startExecutorPool() {
		if (m_executorPool != null) {
			m_executorPool.shutdownNow();
		}

		m_executorPool = Executors.newFixedThreadPool(m_executionSlots);
	}

	/**
	 * Classifies a given instance.
	 * 
	 * @param instance the instance to be assigned to a cluster
	 * @return the number of the assigned cluster as an integer if the class is
	 *         enumerated, otherwise the predicted value
	 * @throws Exception if instance could not be classified successfully
	 */
	@Override
	public int clusterInstance(Instance instance) {
		Instance inst = null;
		if (!m_dontReplaceMissing) {
			m_ReplaceMissingFilter.input(instance);
			m_ReplaceMissingFilter.batchFinished();
			inst = m_ReplaceMissingFilter.output();
		} else {
			inst = instance;
		}

		return clusterProcessedInstance(inst, false, true);
	}

	/**
	 * clusters an instance that has been through the filters.
	 * 
	 * @param instance the instance to assign a cluster to
	 * @param updateErrors if true, update the within clusters sum of errors
	 * @param useFastDistCalc whether to use the fast distance calculation or not
	 * @return a cluster number
	 */
	private int clusterProcessedInstance(Instance instance, boolean updateErrors,
			boolean useFastDistCalc) {

		double minDist = Integer.MAX_VALUE;
		int bestCluster = 0;

		for (int i = 0; i <  mTree.root.nrKeys && mTree.root.routes.size() > i; i++) {

			if(mTree.root.routes.get(i).instances.contains(instance)) {
				double dist;
				if (useFastDistCalc)
					dist = m_DistanceFunction.distance(instance,
							mTree.root.instances.get(i));
				else
					dist = m_DistanceFunction.distance(instance,
							mTree.root.instances.get(i));
				minDist = dist;
				bestCluster = i;

				break;
			}
			else
			{
				double dist;
				if (useFastDistCalc)
					dist = m_DistanceFunction.distance(instance,
							mTree.root.instances.get(i), minDist);
				else
					dist = m_DistanceFunction.distance(instance,
							mTree.root.instances.get(i));
				if (dist < minDist) {
					minDist = dist;
					bestCluster = i;

				}
			}
		}
		if (updateErrors) {
			if (m_DistanceFunction instanceof EuclideanDistance) {
				// Euclidean distance to Squared Euclidean distance
				minDist *= minDist;
			}
			index2++;
			m_squaredErrors[bestCluster] += minDist;
		}

		return bestCluster;
	}

	public ArrayList<Instance> RangeQuery(int parentClusterNumber, Instance Q, double r)
	{
		rangeQueryInstances.clear();
		Node node = getRoot();
		Instance parent = node.instances.get(parentClusterNumber);
		return RecursiveRangeQuery(parent, node, Q, r);
	}
	/**RangeQuery: searches all the instances that are within a given distance from a given instance */
	public ArrayList<Instance> RecursiveRangeQuery(Instance parent, Node node, Instance Q, double r)
	{
		DistanceFunction distanceFunction = new EuclideanDistance();
		distanceFunction.setInstances(train);
		int i;
		/*if the current node is not a leaf, check to see if you can go downward*/

		if(!node.isLeaf)
		{
			for(i = 0; i < node.nrKeys; i++)
				//if( Math.abs(m_DistanceFunction.distance(Q, node.instances.get(i)) - m_DistanceFunction.distance(parent , node.instances.get(i))) <= r + node.radix.get(i))
				if(distanceFunction.distance(Q, node.instances.get(i)) <= r + node.radix.get(i))
					RecursiveRangeQuery(node.instances.get(i), node.routes.get(i), Q, r);
		}

		else/*else search close objects*/
		{
			for(i = 0; i < node.nrKeys; i++)
				if(Math.abs(distanceFunction.distance(Q, parent) - distanceFunction.distance(parent, node.instances.get(i))) <= r)
					if(distanceFunction.distance(Q, node.instances.get(i)) <= r)
					{
						rangeQueryInstances.add(node.instances.get(i));
					}
		}

		return rangeQueryInstances;
	}

	public double twoFeaturesDistance(Instance a, Instance b)
	{
		double dif1 = (a.value(0) - b.value(0)) * (a.value(0) - b.value(0));
		double dif2 = (a.value(1) - b.value(1)) * (a.value(1) - b.value(1));

		double result = Math.sqrt(dif1 + dif2);

		return result;
	}

	/**RangeQuery: searches all the instances that are within a given distance from a given instance */
	public ArrayList<Instance> CustomRangeQuery(Instance parent, Node node, Instance Q, double r)
	{
		int i;
		/*if the current node is not a leaf, check to see if you can go downward*/

		if(!node.isLeaf)
		{
			for(i = 0; i < node.nrKeys; i++)
				//if( Math.abs(m_DistanceFunction.distance(Q, node.instances.get(i)) - m_DistanceFunction.distance(parent , node.instances.get(i))) <= r + node.radix.get(i))
				if(twoFeaturesDistance(Q, node.instances.get(i)) <= r + node.radix.get(i))
					RecursiveRangeQuery(node.instances.get(i), node.routes.get(i), Q, r);
		}

		else/*else search close objects*/
		{
			for(i = 0; i < node.nrKeys; i++)
				if(Math.abs(twoFeaturesDistance(Q, parent) - twoFeaturesDistance(parent, node.instances.get(i))) <= r)
					if(twoFeaturesDistance(Q, node.instances.get(i)) <= r)
					{
						rangeQueryInstances.add(node.instances.get(i));
					}
		}

		return rangeQueryInstances;
	}
	
	/**Comparator anonymous class implementation used for ordering the KNN Priority Queue*/
	public static Comparator<PriorityQueueElement> distanceComparator = new Comparator<PriorityQueueElement>(){

		@Override
		public int compare(PriorityQueueElement pq1, PriorityQueueElement pq2) {
			if (pq1.dmin < pq2.dmin) return -1;
			if (pq1.dmin > pq2.dmin) return 1;
			return 0;
		}
	};	

	/**Update the KNN Queue deleting the elements with the distance from the given instance greater than the computed one*/
	private void updateQueue(Queue<PriorityQueueElement> kNNPriorityQueue, double distance)
	{
		for(PriorityQueueElement node : kNNPriorityQueue)
		{
			if(node.dmin > distance)
				kNNPriorityQueue.remove(node);
		}
	}

	/**kNN: finds the first k elements from the tree that are the closest
	 *to a given object Q; the points are stored in array NNArrayElements*/
	public ArrayList<Instance> kNN(Instance Q, int k)
	{

		Node root = this.getRoot();
		Queue<PriorityQueueElement> kNNPriorityQueue = new PriorityQueue(k, distanceComparator);
		PriorityQueueElement node, nextNode;

		/* add the root as the first node of PR */
		node = new PriorityQueueElement();
		node.node = root;
		node.parent = null;
		node.dmin = 0;
		kNNPriorityQueue.add(node);

		ArrayList<InstanceRadix> NNArrayElements = new ArrayList<InstanceRadix>(k);
		/* initialise the NN array */
		for(int i = 0; i < k; i++)
		{
			InstanceRadix ir = new InstanceRadix();
			ir.radix = INFINITY;
			NNArrayElements.add(ir);
		}

		/* find the neighbors */
		while(!kNNPriorityQueue.isEmpty())
		{
			nextNode = kNNPriorityQueue.poll();
			if(nextNode.parent == null)
				kNNNodeSearch(null/*nextNode.parent*/, nextNode.node, Q, k, kNNPriorityQueue, NNArrayElements);
			else
				kNNNodeSearch(nextNode.parent, nextNode.node, Q, k, kNNPriorityQueue, NNArrayElements);
		}

		ArrayList<Instance> nnInstances = new ArrayList<Instance>();
		for(int i = 0; i < NNArrayElements.size(); i++)
		{
			nnInstances.add(NNArrayElements.get(i).center);
		}
		return nnInstances;
	}

	/**kNNNodeSearch: finds the first k elements from the tree that are the closest to a given object Q;
	 the points are stored in array NNArrayElements*/
	void kNNNodeSearch(Instance parent, Node root, Instance Q, int k, Queue<PriorityQueueElement> priorityQueue, ArrayList<InstanceRadix> NNArrayElements)
	{
		PriorityQueueElement node = new PriorityQueueElement();
		double dmin, dmax;

		int i;

		/* for internal nodes */
		if(!root.isLeaf)
		{
			for(i = 0; i < root.nrKeys; i++)
				if((parent == null) ||(parent != null && Math.abs(m_DistanceFunction.distance(parent, Q) - m_DistanceFunction.distance(root.instances.get(i), parent)) <= root.radix.get(i) + NNArrayElements.get(NNArrayElements.size() - 1).radix))
				{
					//distance between Q and the closest point of the cluster
					dmin = Math.max(m_DistanceFunction.distance(root.instances.get(i), Q) - root.radix.get(i), 0);
					if(dmin < NNArrayElements.get(NNArrayElements.size() -1).radix)
					{
						/* insert active sub-trees in PR*/
						node.node = root.routes.get(i);
						node.parent = root.instances.get(i);
						node.dmin = dmin;
						priorityQueue.add(node);

						//distance between Q and the farest point in the cluster 
						dmax = m_DistanceFunction.distance(root.instances.get(i), Q) + root.radix.get(i);

						/* update the NN array */
						if(dmax < NNArrayElements.get(NNArrayElements.size() - 1).radix)
						{
							InstanceRadix e = new InstanceRadix();
							e.radix = dmax;
							e.center = root.instances.get(i);
							e.isPlaceHolder = true;
							/* insert e in the array;
			   				   e is only a placeholder for
			   				   its descendants from leaf nodes;
							 */

							NNUpdate(NNArrayElements,k,e,parent);
							/* update the queue */
							updateQueue(priorityQueue, NNArrayElements.get(NNArrayElements.size() - 1).radix);
						}
					}
				}
		}
		/* for leaf nodes */
		else
		{
			for(i = 0; i < root.nrKeys; i++)
				if(Math.abs(m_DistanceFunction.distance(parent, Q) - m_DistanceFunction.distance(root.instances.get(i), parent)) < NNArrayElements.get(NNArrayElements.size() - 1).radix)
				if(m_DistanceFunction.distance(root.instances.get(i), Q) < NNArrayElements.get(NNArrayElements.size() - 1).radix)
				{
					InstanceRadix e = new InstanceRadix();
					e.radix = m_DistanceFunction.distance(root.instances.get(i), Q);
					e.center = root.instances.get(i);
					/* insert the actual points in the array and remove placeholders*/
					NNUpdate(NNArrayElements,k,e,parent);
					/* update the queue */
					updateQueue(priorityQueue,NNArrayElements.get(NNArrayElements.size() - 1).radix);
				}
		}
	}


	/**NNUpdate: insert the found element, e, whose parent is parent, in the NN array*/
	void NNUpdate(ArrayList<InstanceRadix> NNArrayElements , int k, InstanceRadix e, Instance parent)
	{
		int i,j;
		i=0;

		/* find the correct position of e */
		while(i < NNArrayElements.size() - 1 && NNArrayElements.get(i).radix < e.radix)
			i++;

		boolean found =  false;
		for(int l = 0;  l < NNArrayElements.size(); l++)
		{
			if(NNArrayElements.get(l).hashCode() == e.hashCode())
				found = true;
			if(e.radix == NNArrayElements.get(l).radix)
				found = true;
		}
		if(i != NNArrayElements.size() - 1 && !NNArrayElements.contains(e) && !found)
		{
			/* shift the elements after e with
			   one position to the right
			 */
			NNArrayElements.add(i, e);
			NNArrayElements.remove(NNArrayElements.size() - 1);

		}
		i=0;
		/* if there were a placeholder for e (one ancestor)*/
		while(i != NNArrayElements.size() - 1)
		{
			if(NNArrayElements.get(i).center == parent && NNArrayElements.get(i).isPlaceHolder)
				break;
			i++;
		}

		/* remove it */
		if(i != NNArrayElements.size() - 1)
		{
			NNArrayElements.remove(i);
		}	
	}

	/**
	 * Gets the assignments for each instance.
	 * 
	 * @return Array of indexes of the centroid assigned to each instance
	 * @throws Exception if order of instances wasn't preserved or no assignments
	 *           were made
	 */
	public int[] getAssignments() throws Exception {
		if (!m_PreserveOrder) {
			throw new Exception(
					"The assignments are only available when order of instances is preserved (-O)");
		}
		if (m_Assignments == null) {
			throw new Exception("No assignments made.");
		}
		return m_Assignments;
	}


	/**
	 * set the number of clusters to generate.
	 * 
	 * @param n the number of clusters to generate
	 * @throws Exception if number of clusters is negative
	 */
	@Override
	public void setNumClusters(int n) throws Exception {
		if (n <= 0) {
			throw new Exception("Number of clusters must be > 0");
		}
		m_NumClusters = n;
	}

	/**
	 * gets the number of clusters to generate.
	 * 
	 * @return the number of clusters to generate
	 */
	public int getNumClusters() {
		return m_NumClusters;
	}

	/**
	 * Gets the squared error for all clusters.
	 * 
	 * @return the squared error, NaN if fast distance calculation is used
	 * @see #m_FastDistanceCalc
	 */
	public double getSquaredError() {
		if (m_FastDistanceCalc)
			return Double.NaN;
		else
			return Utils.sum(m_squaredErrors);
	}

	/**
	 * Gets the root of the tree.
	 * @return the root
	 */
	public Node getRoot()
	{
		return mTree.root;
	}

	@Override
	public int numberOfClusters() throws Exception {
		return m_NumClusters;
	}

	/**
	 * Returns the tip text for this property
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String numClustersTipText() {
		return "set number of clusters";
	}
	/**
	 * Returns the tip text for this property.
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String distanceFunctionTipText() {
		return "The distance function to use for instances comparison "
				+ "(default: weka.core.EuclideanDistance). ";
	}

	/**
	 * Returns the tip text for this property
	 * 
	 * @return tip text for this property suitable for displaying in the
	 *         explorer/experimenter gui
	 */
	public String dontReplaceMissingValuesTipText() {
		return "Replace missing values globally with mean/mode.";
	}

}

