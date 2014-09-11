package weka.classifiers.bayes.SGM;
import java.util.HashSet; 
import java.util.Hashtable; 
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.Math;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.Map;
import java.util.Map.Entry;

public class SGM implements Serializable{
	private static final long serialVersionUID = -3376037288335722173L;
	public int debug;
	private BufferedReader input_file;
	public SparseData data;
	public int num_classified;
	Hashtable<Integer, ArrayList<Integer>> inverted_index;
	int prior_max_label;
	public double prune_count_insert;
	double add_prune;
	double label_threshold;
	double cond_scale;
	int cond_hashsize;
	public SGM_Params model;

	HashSet<Integer> label_constrain;
	TFIDF tfidf;
	int binary_model;
	int knn;
	public boolean reverse_nb;
	int wordseq;
	Hashtable<IntBuffer, Integer> labels2powerset;
	Hashtable<Integer, IntBuffer> encoding2labels;
	int max_labelsize;
	Hashtable<Integer, Integer> corrects;
	int tp, fp, fn;
	double rec, prec, fscore, map, meanjaccard;

	public SGM() {
	}

	public String hello() {
		return("Hello!");
	}

	public void init_model(int cond_hashsize, int max_labelsize) throws Exception {
		//System.out.println("SGM Initializing model");
		debug= 0;
		this.cond_hashsize= cond_hashsize;
		model= new SGM_Params(cond_hashsize);
		label_constrain= null;
		binary_model= 0;
		reverse_nb= false;
		prior_max_label= -1;
		labels2powerset= null;
		encoding2labels= null;
		this.max_labelsize= max_labelsize;
	}

	public void init_model(int cond_hashsize, TFIDF tfidf, int max_labelsize) throws Exception {
		init_model(cond_hashsize, max_labelsize);
		this.tfidf= tfidf;
	}

	public void train_model(int batch_size, double prune_count_insert) throws Exception {
		this.prune_count_insert= prune_count_insert;
		if (debug>0) System.out.println("Updating model " + data.doc_count + " "+ model.train_count);
		if (data.label_weights==null) for (int w= 0; w < data.doc_count; w++) add_instance(data.terms[w], data.counts[w], data.labels[w], null);
		else for (int w= 0; w < data.doc_count; w++) add_instance(data.terms[w], data.counts[w], data.labels[w], data.label_weights[w]);
	}

	public int[] get_label_powerset(int[] labels) {
		int[] labels2= Arrays.copyOf(labels, labels.length);
		if (labels2.length>1) Arrays.sort(labels2);
		IntBuffer wrap_labels= IntBuffer.wrap(labels2);
		Integer powerset= labels2powerset.get(wrap_labels);
		if (powerset==null) {
			powerset= labels2powerset.size();
			labels2powerset.put(wrap_labels, powerset);
		}
		labels= new int[1];
		labels[0]= powerset;
		return labels;
	}

	public void add_instance(int[] terms, float[] counts, int[] labels, float[] label_weights) {
		if (tfidf!=null) tfidf.length_normalize(terms, counts);
		if (labels2powerset!=null) labels= get_label_powerset(labels);
		if (labels.length>1) Arrays.sort(labels);
		for (int label:labels) {
			Integer label2= label;
			Float lsp= model.prior_lprobs.get(label2);
			lsp= (lsp==null) ? (float) 0.0 : flogaddone(lsp);
			model.prior_lprobs.put(label2, lsp);
		}
		if (knn>0) {
			int[] labels2= Arrays.copyOf(labels, labels.length);
			IntBuffer wrap_labels= IntBuffer.wrap(labels2);
			encoding2labels.put((Integer) model.train_count, wrap_labels);
			labels= new int[1];
			labels[0]= model.train_count;
		}
		model.train_count++;
		int t= 0, j;
		for (t= 0;t<terms.length;) counts[t]= (float)Math.log(counts[t++]);
		if (label_weights!=null) for (t= 0;t<labels.length;) label_weights[t]= (float)Math.log(label_weights[t++]);
		for (t= 0; t<terms.length; t++) {
			int term= terms[t];
			//System.out.println(term+" "+counts[t]);
			//if (term==-1) continue;	
			Integer term2= new Integer(term);
			Float cond_bg= model.cond_bgs.get(term2);
			cond_bg= (cond_bg==null) ? counts[t] : (float)logsum(cond_bg, counts[t]);
			model.cond_bgs.put(term2, cond_bg);

			double prune= prune_count_insert;
			prune-= Math.log(tfidf.get_idf(term2));
			double lprob= counts[t];
			j= 0;
			for (int label:labels) {
				if (label_weights!=null) lprob+= label_weights[j++];
				add_lprob(label, term, lprob, prune);
			}
		}
		//if (model.train_count % batch_size == 0) prune_counts(prune_count_insert, cond_hashsize);
	}

	public void add_lprob(int label, int term, double lprob2, double prune) {
		CountKey p_index= new CountKey(label, term);
		Float lprob= model.cond_lprobs.get(p_index);
		if (lprob==null) {
			label= -1;
			if (model.cond_lprobs.size()==cond_hashsize) return;
			lprob= (float) lprob2;
		}
		else lprob= (float) logsum(lprob, lprob2);
		//System.out.println(label+" "+term+" "+lprob2+" "+prune); 
		if (lprob < prune) {if (label!=-1) model.cond_lprobs.remove(p_index);}
		else model.cond_lprobs.put(p_index, lprob);
	}

	public void prune_counts(double prune_count_table, int cond_hashsize) {
		this.cond_hashsize= cond_hashsize;
		if (debug>0) System.out.println("Pruning conditional hash table:"+ model.cond_lprobs.size()+ " " + prune_count_table);
		Iterator<Entry<CountKey, Float>> entries= model.cond_lprobs.entrySet().iterator();
		while (entries.hasNext()) {
			Entry<CountKey,  Float> entry= (Entry<CountKey,  Float>) entries.next();
			float lprob= (Float) entry.getValue();
			lprob+= Math.log(tfidf.get_idf(((CountKey)entry.getKey()).term));
			if (lprob <= prune_count_table) entries.remove();
		}
		if (debug>0) System.out.println("Hash table pruned:"+ model.cond_lprobs.size());
		prune_counts_size(model.cond_lprobs, cond_hashsize);
	}

	public void prune_counts_size(Hashtable<CountKey, Float> counts, int hashsize) {
		if (counts.size() > hashsize) {
			int bins= (int) Math.log(counts.size());
			int i= 0, j= 0;
			double tmp[]= new double[1+counts.size()/bins];
			for (Map.Entry<CountKey, Float> entry : counts.entrySet()) {
				if (i++%bins==0) {
					float lprob= (Float)entry.getValue();
					lprob+= Math.log(tfidf.get_idf(((CountKey)entry.getKey()).term));
					tmp[j++]= lprob;
				}
			}
			Arrays.sort(tmp);
			double prune = tmp[(counts.size() - hashsize)/bins];
			Iterator<Entry<CountKey, Float>> entries= counts.entrySet().iterator();
			while (entries.hasNext()) {
				Entry<CountKey, Float> entry= (Entry<CountKey, Float>)entries.next();
				float lprob= (Float)entry.getValue();
				lprob+= Math.log(tfidf.get_idf(((CountKey)entry.getKey()).term));
				if (lprob <= prune) entries.remove();
			}
		}
		if (debug>0) System.out.println("Hash table pruned:"+ model.cond_lprobs.size());
	}

	public void apply_idfs() throws Exception {
		for (Iterator<Integer> d = model.cond_bgs.keySet().iterator(); d.hasNext();) {
			Integer term= d.next();
			Float idf= (float)tfidf.get_idf(term);
			if (!tfidf.idfs.containsKey(term) || idf<=0.0) d.remove(); 
			else {
				Float lprob= model.cond_bgs.get(term) +(float) Math.log(idf);
				model.cond_bgs.put(term, lprob);
			}
		}
		for (Iterator<CountKey> e= model.cond_lprobs.keySet().iterator(); e.hasNext();) {
			CountKey p_index= e.next();
			Integer term= p_index.term;
			Float idf= (float)tfidf.get_idf(term);
			if (!tfidf.idfs.containsKey(term) || idf<=0.0) e.remove();
			else {
				Float lprob= (float)(model.cond_lprobs.get(p_index))+(float)Math.log(tfidf.get_idf(term));
				model.cond_lprobs.put(p_index, lprob);
			}
		}
	}

	public void scale_conditionals(double cond_scale) throws Exception {
		if (debug>0) System.out.println("Scaling conditionals");
		for (Map.Entry<CountKey, Float> entry : model.cond_lprobs.entrySet()) entry.setValue((Float)(entry.getValue()*(float)cond_scale));
	}

	public void normalize_conditionals() throws Exception {
		Hashtable<Integer, Double> norms= new Hashtable<Integer, Double>();
		for (Map.Entry<CountKey, Float> entry : model.cond_lprobs.entrySet()) {
			Integer label= ((CountKey)entry.getKey()).label;
			if (reverse_nb) label= ((CountKey)entry.getKey()).term;
			Double lsum= norms.get(label);
			if (lsum == null) lsum= -100000.0;
			lsum= logsum(lsum, entry.getValue());
			norms.put(label, lsum);
		}
		for (Map.Entry<CountKey, Float> entry : model.cond_lprobs.entrySet())
			if (reverse_nb) entry.setValue((Float)(entry.getValue()- (float)(double)norms.get(((CountKey)entry.getKey()).term)));
			else entry.setValue((Float)(entry.getValue()- (float)(double)norms.get(((CountKey)entry.getKey()).label)));
		double[] tmp_vals;
		int t= 0;
		float norm;
		if (model.cond_bgs.size()>0) {
			tmp_vals= new double[tfidf.idfs.size()];
			for (Float lprob: model.cond_bgs.values()) tmp_vals[t++]= (double) lprob;
			//norm= (float) logsum_ndoubles(tmp_vals);
			norm= (float) logsum_doubles(tmp_vals);
			for (Map.Entry<Integer, Float> entry : model.cond_bgs.entrySet()) entry.setValue((Float)(entry.getValue()- norm));
		}
	}

	public void normalize_model() throws Exception {
		normalize_conditionals();
		double[] tmp_vals;
		int t= 0;
		float norm;
		if (model.prior_lprobs!=null) {
			if(binary_model==1) norm= (float)Math.log(model.train_count);
			else {
				t= 0; tmp_vals= new double[model.prior_lprobs.size()];
				for (Float lprob: model.prior_lprobs.values()) tmp_vals[t++]= (double) lprob;
				norm= (float) logsum_doubles(tmp_vals);
			}
			for (Map.Entry<Integer, Float> entry : model.prior_lprobs.entrySet()) entry.setValue((Float)(entry.getValue()- norm));
		}
	}


	public void smooth_conditionals(double cond_unif_weight, double bg_weight) {
		bg_weight= Math.max(Math.min(0.9999999999, bg_weight), 0.000000000001);
		cond_unif_weight= Math.max(Math.min(0.9999999999-bg_weight, cond_unif_weight), 0.000000000001);
		double a4= Math.log(1.0 - cond_unif_weight - bg_weight);
		double a5= Math.log(bg_weight);
		if (reverse_nb) model.cond_uniform= Math.log(cond_unif_weight) - Math.log(model.prior_lprobs.size()); 
		else model.cond_uniform= Math.log(cond_unif_weight) - Math.log(tfidf.idfs.size());
		//System.out.println(model.cond_uniform+" "+a5+" "+bg_weight+" "+a4);
		for (Map.Entry<Integer, Float> entry : model.cond_bgs.entrySet())	
			entry.setValue((Float)(float)(logsum(a5 + entry.getValue(), model.cond_uniform)));
		for (Map.Entry<CountKey, Float> entry : model.cond_lprobs.entrySet())
			entry.setValue((Float)(float)(logsum(a4 + entry.getValue(), model.cond_bgs.get(((CountKey)entry.getKey()).term))));
	}

	public void smooth_conditionals(double cond_unif_weight) {
		cond_unif_weight= Math.max(Math.min(0.9999999999, cond_unif_weight), 0.000000000001);
		double a4= Math.log(1.0 - cond_unif_weight);
		model.cond_uniform= Math.log(cond_unif_weight) - Math.log(tfidf.idfs.size());

		for (Map.Entry<CountKey, Float> entry : model.cond_lprobs.entrySet())
			entry.setValue((Float)(float)(logsum(a4 + entry.getValue(), model.cond_uniform)));
	}

	public void smooth_prior(double prior_scale) {
		if (model.prior_lprobs!=null)
			for (Map.Entry<Integer, Float> entry : model.prior_lprobs.entrySet()) entry.setValue((Float)(float) (entry.getValue() * prior_scale));
	}

	public final int[] intbuf_add(int number, int[] buf) {
		int buf_size2= buf.length + 1;
		int[] buf2= new int[buf_size2];
		int h= 0;
		for (int j: buf) 
			if (j < number) buf2[h++]= j; 
			else break;
		buf2[h++]= number;
		for (; h < buf_size2;) buf2[h]= buf[(h++)-1];
		return buf2;
	}

	public final int[] intbuf_add2(int number, int[] buf, int[] buf2) {
		int buf_size2= buf2.length;
		int h= 0;
		for (int j: buf) 
			if (j < number) buf2[h++]= j; 
			else break;
		buf2[h++]= number;
		for (; h < buf_size2;) buf2[h]= buf[(h++)-1];
		return buf2;
	}

	public final int[] intbuf_remove(int number, int[] buf) {
		int buf_size2= buf.length - 1;
		int[] buf2= new int[buf_size2];
		int h= 0;
		for (int j:buf) if (j != number) buf2[h++]= j; else break;
		for (; h < buf_size2;) buf2[h]= buf[++h];
		return buf2;
	}

	public double logsum(double val1, double val2) {
		if (val1+20.0 < val2) return val2;
		if (val2+20.0 < val1) return val1;
		if (val1 > val2) return Math.log(Math.exp(val2 - val1) + 1.0) + val1;
		return Math.log(Math.exp(val1 - val2) + 1.0) + val2;
	}

	public double logsubstract(double val1, double val2) {
		// Note: negative values floored to 0
		if (val2+20.0 < val1) return val1;
		if (val1 > val2) return Math.log(-Math.exp(val2 - val1) + 1.0) + val1;
		return (-100000.0);
	}

	public float flogsum(float val1, float val2) {
		if (val1 > val2) return (float) Math.log(Math.exp(val2 - val1) + 1) + val1;
		else return (float) Math.log(Math.exp(val1 - val2) + 1) + val2;
	}

	public float flogaddone(float val1) {
		return (float) Math.log(Math.exp(val1) + 1.0);
	}

	public final double sum_doubles(double[] vals) {
		TreeSet<DoubleBuffer> treesort = new TreeSet<DoubleBuffer>();
		for (double val: vals) {
			double[] entry = {Math.abs(val), val};
			treesort.add(DoubleBuffer.wrap(entry));
		}             
		double sum= 0.0;
		for (Iterator<DoubleBuffer> e = treesort.descendingIterator(); e.hasNext();) sum+= e.next().get(1);	
		return sum;
		//while (treesort.size()>1){
		//   //Iterator<DoubleBuffer> e = treesort.descendingIterator();
		//    Iterator<DoubleBuffer> e = treesort.iterator();
		//    double val= e.next().get(1);
		//    e.remove();
		//    val+= e.next().get(1);
		//    e.remove();
		//    double[] entry = {Math.abs(val), val};
		//    treesort.add(DoubleBuffer.wrap(entry));
		//}
		//return treesort.first().get(1);
	}

	public final double logsum_doubles(double[] vals) {
		TreeSet<DoubleBuffer> treesort = new TreeSet<DoubleBuffer>();
		for (double val: vals) {
			double[] entry = {Math.abs(val), val};
			treesort.add(DoubleBuffer.wrap(entry));
		}
		double sum= 0.0;
		for (Iterator<DoubleBuffer> e = treesort.descendingIterator(); e.hasNext();) sum= logsum(sum, e.next().get(1));
		return sum;
	}

	public final double logsum_ndoubles(double[] vals) {
		//Note: Sorts original
		Arrays.sort(vals); //reduce double sum error
		double sum= -100000.0;
		for (double val: vals) sum= logsum(val, sum);
		return sum;
	}

	public final double sum_ndoubles2(double[] vals, int count) {
		Arrays.sort(vals, 0, count);
		double sum= 0.0;
		for (int i= 0; i<count;) sum+= vals[i++];
		return sum;
	}

	public final double sum_ndoubles(double[] vals) {
		//Note: Sorts original
		Arrays.sort(vals); //reduce double sum error
		double sum= 0.0;
		//double correct= 0.0;
		//double t, c;
		//for (double val: vals){
		//    val-= correct;
		//    t= sum + val;
		//    float tmp= (float)(t-sum); 
		//    correct= tmp- val;
		//    sum= t;
		//}

		for (double val: vals) sum+= val;
		return sum;

		//double mean= 0.0;
		//int i= 0;
		//for (double val: vals) mean+= (val - mean) / ++i;
		//return mean*i;
		//int fork= 1;
		//int length= vals.length;
		//while (fork< length) {
		//    for (int j= 0; j< length; j+= fork+fork) {
		//	if (j+fork< vals.length) vals[j]+= vals[j+fork];
		//	else {vals[j-fork-fork]+= vals[j]; length=j;}
		//	//System.out.println(j+" "+(j+fork)+" "+vals.length);
		//    }
		//   fork+=fork;
		//}
		//return vals[0];
	}

	/*
	private final boolean EQ(double d1, double d2){
		if (Math.abs(d1-d2)< 0.000000001)
			return true;
		return false;
	}

	private final boolean GE(double d1, double d2){
		if (EQ(d1, d2)) return true;
		return GT(d1, d2);
	}

	private final boolean GT(double d1, double d2){
		if (d1> d2 + 0.000000001) return true;
		return false;
	}*/

	public final SparseVector inference(int[] terms, float[] counts) {
		if (tfidf!=null) tfidf.length_normalize(terms, counts);
		CountKey p_index= new CountKey(0, 0);
		int term_count= 0, t;
		Integer term2;

		//System.out.println(terms.length);
		for (t= 0; t<terms.length; t++) {
			term2= new Integer(terms[t]);
			if (inverted_index.containsKey(term2)) {
				terms[term_count]= term2;
				counts[term_count++]= (float)(counts[t] * tfidf.get_idf(term2));
			}
		}
		if (term_count!=terms.length) terms= Arrays.copyOf(terms, term_count);

		double[] bo_lprobs= new double[term_count];
		Iterator<Integer> e;

		Hashtable<Integer, LinkedList<Integer>> update_lists= new Hashtable<Integer, LinkedList<Integer>>();	
		for (int i= 0; i< term_count; i++) {
			term2= terms[i];
			bo_lprobs[i]= (model.cond_bgs.containsKey(term2) ? model.cond_bgs.get(term2): model.cond_uniform) * counts[i];
			for (e= inverted_index.get(term2).listIterator(); e.hasNext();) {
				Integer label= e.next();
				if (label_constrain!=null && !label_constrain.contains(label)) continue;
				LinkedList<Integer> update_list= update_lists.get(label);
				if (update_list==null) {
					update_list= new LinkedList<Integer>();
					update_lists.put(label, update_list);
				}
				update_list.add((Integer)i);
			}
		}
		Hashtable<CountKey, Float> cond_lprobs= model.cond_lprobs;
		Integer label2;
		int n= 0, i;
		LinkedList<Integer> update_list;

		TreeSet<DoubleBuffer> retrieve_sort= new TreeSet<DoubleBuffer>();
		double lprob, sum_bo_lprob= sum_ndoubles(Arrays.copyOf(bo_lprobs, terms.length)), max_lprob= sum_bo_lprob+ model.prior_lprobs.get(prior_max_label), topset_lprob= max_lprob;
		Iterator<Integer> g;
		for (Map.Entry<Integer, LinkedList<Integer>> entry: update_lists.entrySet()) {
			p_index.label= label2= entry.getKey();
			update_list= entry.getValue();
			float prior_lprob= 0;
			if (model.prior_lprobs!=null) {
				if (knn==0) prior_lprob= model.prior_lprobs.get(label2);
				else prior_lprob= model.prior_lprobs.get(encoding2labels.get((Integer)label2));
			} 
			lprob= prior_lprob + sum_bo_lprob;
			for (g= update_list.listIterator(); g.hasNext();) lprob-= bo_lprobs[g.next()];
			for (g= update_list.listIterator(); g.hasNext();) {
				i= g.next();
				p_index.term= terms[i];
				lprob+= cond_lprobs.get(p_index) * counts[i];
				//System.out.println(prior_lprob+" "+sum_bo_lprob+" "+cond_lprobs.get(p_index)+" "+cond_lprobs.containsKey(p_index));
				if (lprob< topset_lprob) break;
			}
			if (lprob>= topset_lprob) {
				if (lprob>= max_lprob+label_threshold && (lprob> topset_lprob || retrieve_sort.size()<= max_labelsize)) {
					if (retrieve_sort.size()== max_labelsize) retrieve_sort.remove(retrieve_sort.descendingIterator().next());
					double[] entry2 = {lprob, label2};		
					retrieve_sort.add(DoubleBuffer.wrap(entry2));
					topset_lprob= retrieve_sort.descendingIterator().next().array()[0];
					if (lprob > max_lprob) max_lprob= lprob;
				}
			}

		}
		if (retrieve_sort.size()==0) {
			double[] entry2 = {sum_bo_lprob+ model.prior_lprobs.get(prior_max_label), prior_max_label};
			retrieve_sort.add(DoubleBuffer.wrap(entry2));
		}

		Iterator<DoubleBuffer> f= retrieve_sort.descendingIterator();
		if (knn==0 && encoding2labels!=null) {
			SparseVector results= new SparseVector(encoding2labels.get((Integer)(int)f.next().array()[1]).array());
			results.values= new float[1];
			results.values[0]= (float)max_lprob;
			return results;
		}
		int labelsize= retrieve_sort.size();
		while (f.hasNext()) if (max_lprob+label_threshold > f.next().array()[0]) f.remove();
		f= retrieve_sort.descendingIterator();
		if (knn>0) {
			Hashtable<IntBuffer, Integer> labelset_counts= new Hashtable<IntBuffer, Integer>();
			if (labelsize>knn) labelsize= knn;
			int max_count= 0;
			int max_docid= 0;
			for (n= 0; n< labelsize; n++) {
				double[] entry2= f.next().array();
				IntBuffer labelset= encoding2labels.get((Integer)(int)entry2[1]);
				Integer count= labelset_counts.get(labelset);
				if (count==null) count= 1;
				else count+= 1;
				labelset_counts.put(labelset, count);
				if (count> max_count) {max_count= count; max_docid= (int)entry2[1];}
			}
			SparseVector result= new SparseVector(encoding2labels.get(max_docid).array());
			result.values= new float[1];
			result.values[0]= (float)max_lprob;
			return result;
		} else {
			SparseVector result= new SparseVector(labelsize);
			for (n= 0; n< labelsize;) {
				double[] entry2= f.next().array();
				result.indices[n]= (int)entry2[1];
				result.values[n++]= (float)entry2[0];
			}
			return result;
		}
	}

	public void prepare_inference() {
		inverted_index= new Hashtable<Integer, ArrayList<Integer>>();
		for (Enumeration<CountKey> d= model.cond_lprobs.keys(); d.hasMoreElements();) {
			CountKey p_index= d.nextElement();
			Integer term= p_index.term;
			ArrayList<Integer> labels= inverted_index.get(term);
			if (labels == null) {
				labels= new ArrayList<Integer>(4);
				labels.add(p_index.label);
				inverted_index.put(term, labels);
			} else labels.add(p_index.label);
		}
		/*for (Enumeration<ArrayList<Integer>> d = inverted_index.elements(); d.hasMoreElements();) {
	  System.out.println(d.nextElement().size());
	  }*/
		if (labels2powerset!=null) {
			encoding2labels= new Hashtable<Integer, IntBuffer>(labels2powerset.size());
			for (Enumeration<IntBuffer> d= labels2powerset.keys(); d.hasMoreElements();) {
				IntBuffer labels= d.nextElement();
				Integer powerset= labels2powerset.get(labels);
				encoding2labels.put(powerset, labels);
				labels2powerset.remove(labels);
			}
		}
		if (model.prior_lprobs!=null) {
			float max_lprob= -10000000;
			for (Map.Entry<Integer, Float> entry : model.prior_lprobs.entrySet()) if (entry.getValue()> max_lprob) {
				max_lprob= entry.getValue(); 
				prior_max_label= entry.getKey();
			}
		}
	}

	public void retrieve_data(double label_threshold, PrintWriter resultsf) throws Exception {
		this.label_threshold= label_threshold;
		int[] labels;
		for (int w= 0; w < data.doc_count; w++) {
			labels= data.labels[w];
			SparseVector retrieve_results= inference(data.terms[w], data.counts[w]);
			if (resultsf == null) {
				update_evaluation_results(retrieve_results.indices, labels, 1);
			} else {
				String results = "";
				for (int n = 0; n < retrieve_results.indices.length; n++)
					results += retrieve_results.indices[n] + " ";
				resultsf.println(results.trim());
			}
		}
	}

	public void use_powerset() {labels2powerset= new Hashtable<IntBuffer, Integer>();}

	public void use_knn(int knn) {
		encoding2labels= new Hashtable<Integer, IntBuffer>();
		this.knn= knn;
	}

	public void prepare_evaluation() {
		corrects= new Hashtable<Integer, Integer>();
		if (prior_max_label==-1) {
			prior_max_label= model.cond_lprobs.keys().nextElement().label;
			if (encoding2labels!=null) prior_max_label= encoding2labels.get(prior_max_label).get(0);
		}
		num_classified= tp= fp= fn= 0;
		rec= prec= fscore= map= 0;
	}

	private void update_evaluation_results(int[] labels, int[] ref_labels, int print_results) {
		String ref= num_classified + " Ref:"+Arrays.toString(ref_labels), res= num_classified + " Res:"+Arrays.toString(labels);
		HashSet<Integer> ref_labels2= new HashSet<Integer>(ref_labels.length);
		for (int label:ref_labels) ref_labels2.add((Integer)label); 
		int tp2= 0, fp2= 0;
		double ap= 0.0;
		num_classified++;
		for (int label:labels) {
			if (ref_labels2.contains((Integer)label)) {
				tp++;
				tp2++;
				ap+= ((double)tp2/(tp2 + fp2)) / labels.length;
			} else {
				fp++; 
				fp2++;
				ap+= ((double)tp2/(tp2 + fp2)) / labels.length;
			}
		}
		double jaccard= (double)tp2/(labels.length+ref_labels.length-tp2);
		meanjaccard+= (jaccard-meanjaccard)/num_classified;
		fn+= ref_labels.length - tp2;
		rec= (double) tp / (tp + fn);
		prec= (double) tp / (tp + fp);
		fscore= (2.0 * rec * prec) / (rec + prec);
		map+= (ap-map)/num_classified;
		if ((rec == 0 && prec == 0) || (tp + fp==0) || (tp + fn==0)) fscore= 0;
		System.out.println(res);
		System.out.println(ref + "      TP:" + tp + " FN:" + fn + " FP:" + fp + " meanJaccard:"+meanjaccard+" miFscore:" + fscore + " MAP:" + map);
	}

	public void print_evaluation_summary() {
		String res= "";
		/*for (Enumeration<Integer> e = corrects.keys(); e.hasMoreElements();) {
	    Integer in = e.nextElement();
	    res+= in + ":" + corrects.get(in) + " ";
	    }*/
		System.out.println("Results: meanJaccard:"+meanjaccard+" miFscore:" +fscore+ " MAP:" +map+ "  " +res);
		//System.out.println("Fscore: " +fscore+ "  " +res);
	}



	public void classify_data(PrintWriter resultsf) throws Exception {
		for (int w= 0; w < data.doc_count; w++) {
			SparseVector classify_results= inference(data.terms[w], data.counts[w]);
			int[] labels= classify_results.indices;
			if (resultsf == null) {
				update_evaluation_results(labels, data.labels[w], 1);
			} else {
				String results = "";
				for (int n = 0; n < labels.length; n++)
					results += labels[n] + " ";
				resultsf.println(results.trim());
			}
		}
	}

	public void open_stream(String data_file, int docs, boolean use_label_weights) throws Exception {
		if (debug>0) System.out.println("SGM opening data stream: " + data_file);
		input_file= new BufferedReader(new FileReader(data_file));
		data= new SparseData(docs, use_label_weights);
		//if (data==null || docs!=data.doc_count) data= new SparseData(-1, docs, -1);
	}

	public void close_stream() throws Exception {
		input_file.close();
	}

	public int get_features(int docs) throws Exception {
		int w= 0;
		w= read_libsvm_stream(docs);
		return w;
	}

	public int read_libsvm_stream(int docs) throws Exception {
		String l;
		String[] splits, s;
		int[] labels, terms;
		float[] counts, label_weights= null;
		int w= 0;
		for (; w < docs; w++) {
			if ((l = input_file.readLine()) == null) break;
			int term_c= 0, i= 0;//, length= 0;
			for (char c: l.toCharArray()) if (c==':') term_c++;
			splits= l.split(" ");
			//System.out.println(splits.length+" "+term_c);
			int label_c= splits.length - term_c;
			data.labels[w]= labels= new int[label_c];
			data.terms[w]= terms= new int[term_c];
			data.counts[w]= counts= new float[term_c];
			if (data.use_label_weights) data.label_weights[w]= label_weights= new float[label_c];
			for (; i < label_c; i++) {
				s= splits[i].split(",")[0].split(";");
				labels[i]= Integer.decode(s[0]);
				if (s.length>1 && data.use_label_weights) label_weights[i]= new Float(s[1]);
			}
			for (; i < splits.length;) {
				//System.out.println(splits[i]);
				s= splits[i].split(":");
				Integer term= Integer.decode(s[0]);
				//tfidf.add_count(term);
				terms[i - label_c]= term;
				counts[i++ - label_c]= (float)Integer.decode(s[1]);
			}
			//tfidf.add_doc();
			//tfidf.length_normalize(counts, terms.length);
		}
		if (w != docs) data.doc_count = w;
		return w;
	}

	public void save_model(String model_name) throws Exception {
		PrintWriter model_file = new PrintWriter(new FileWriter(model_name));
		model_file.println("train_count: " + model.train_count);
		model_file.println("cond_uniform: " + model.cond_uniform);
		if (model.prior_lprobs!=null) model_file.println("prior_lprobs: " + model.prior_lprobs.size());
		else model_file.println("prior_lprobs: 0");
		if (labels2powerset!=null) model_file.println("labels2powerset: " + labels2powerset.size());
		else model_file.println("labels2powerset: 0");
		model_file.println("tf_idf.normalized: "+ tfidf.normalized);
		model_file.println("idfs: " + tfidf.idfs.size());
		model_file.println("cond_bgs: " + model.cond_bgs.size());
		model_file.println("lprobs: " + model.cond_lprobs.size());

		if (model.prior_lprobs!=null)
			for (Map.Entry<Integer, Float> entry : model.prior_lprobs.entrySet()) model_file.println(entry.getKey()+" "+entry.getValue());
		if (labels2powerset!=null) 
			for (Enumeration<IntBuffer> d = labels2powerset.keys(); d.hasMoreElements();) {
				IntBuffer in = d.nextElement();
				int[] labelset= in.array();
				String tmp= "";
				for (int i= 0; i < labelset.length; i++) tmp+= labelset[i] + " ";
				model_file.println(tmp + labels2powerset.get(in));
			}
		for (Enumeration<Integer> d = tfidf.idfs.keys(); d.hasMoreElements();) {
			Integer in = d.nextElement();
			model_file.println(in + " " + tfidf.idfs.get(in));
		}
		for (Enumeration<Integer> d = model.cond_bgs.keys(); d.hasMoreElements();) {
			Integer in = d.nextElement();
			model_file.println(in + " " + model.cond_bgs.get(in));
		}
		for (Enumeration<CountKey> d = model.cond_lprobs.keys(); d.hasMoreElements();) {
			CountKey in = d.nextElement();
			model_file.println(in.label + " " + in.term + " " + model.cond_lprobs.get(in));
		}
		model_file.close();
	}

	public void load_model(String model_name) throws Exception {
		model= new SGM_Params(10000000);
		BufferedReader input= new BufferedReader(new FileReader(model_name));
		model.train_count= tfidf.train_count= new Integer(input.readLine().split(" ")[1]);
		model.cond_uniform= new Double(input.readLine().split(" ")[1]);
		int prior_lprobs= new Integer(input.readLine().split(" ")[1]);
		int labels2powersets= new Integer(input.readLine().split(" ")[1]);
		tfidf.normalized= new Integer(input.readLine().split(" ")[1]);
		int idfs= new Integer(input.readLine().split(" ")[1]);
		int cond_bgs= new Integer(input.readLine().split(" ")[1]);
		int lprobs= new Integer(input.readLine().split(" ")[1]);
		while (prior_lprobs > 0) {
			String[] s= input.readLine().split(" ");
			Integer label= new Integer(s[0]);
			Float lprob = new Float(s[1]);
			model.prior_lprobs.put(label, lprob);
			prior_lprobs-= 1;
		}
		while (labels2powersets > 0) {
			String[] s= input.readLine().split(" ");
			int[] labelset = new int[s.length - 1];
			IntBuffer wrap_labelset = IntBuffer.wrap(labelset);
			for (int i = 0; i < labelset.length; i++) labelset[i] = new Integer(s[i]);
			Integer label= new Integer(s[s.length - 1]);
			labels2powerset.put(wrap_labelset, label);
			labels2powersets-= 1;
		}
		while (idfs > 0) {
			String[] s= input.readLine().split(" ");
			Integer label= new Integer(s[0]);
			Float idf= new Float(s[1]);
			tfidf.idfs.put(label, idf);
			idfs-= 1;
		}
		while (cond_bgs > 0) {
			String[] s= input.readLine().split(" ");
			Integer label= new Integer(s[0]);
			Float smooth= new Float(s[1]);
			model.cond_bgs.put(label, smooth);
			cond_bgs-= 1;
		}
		while (lprobs > 0) {
			String[] s= input.readLine().split(" ");
			Float lprob= new Float(s[2]);
			CountKey p_index= new CountKey(new Integer(s[0]), new Integer(s[1]));
			model.cond_lprobs.put(p_index, lprob);
			lprobs-= 1;
		}
		input.close();
	}

}
