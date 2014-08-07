package weka.classifiers.bayes.SGM;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Hashtable;
import java.util.Map;

public class TFIDF implements Serializable{
	private static final long serialVersionUID = -3376037288335722173L;
	int train_count;
	int normalized;
	float length_scale;
	float idf_lift;
	Hashtable<Integer, Float> idfs;

	public TFIDF(double length_scale, double idf_lift) {
		this.length_scale= (float) length_scale;
		this.idf_lift= (float) idf_lift;
		idfs= new Hashtable<Integer, Float>();
		normalized= 0;
		train_count= 0;
	}

	//public void add_doc() {if (normalized==0) train_count++;}

	public void add_count(Integer term) {
		//if (normalized==0) {
		Float idf= idfs.get(term);
		idf= (idf==null) ? 1: idf+1;
		idfs.put(term, idf);
		//}
	}

	public void normalize(double min_count) {
		if (normalized==1) return;
		for (Iterator<Integer> d = idfs.keySet().iterator(); d.hasNext();) {
			Integer term= d.next();
			//System.out.println(term+" "+idfs.get(term)+" "+get_idf(term));
			Float idf= (float)get_idf(term);
			if (idf<=0.0 || idfs.get(term)<=min_count) {
				//System.out.println(term+" "+idfs.get(term)+" "+idfs.get(term)/train_count);
				d.remove();
			}
			else idfs.put(term, idf);
		}
		normalized= 1;
	}

	public void length_normalize(int[] terms, float[] counts){
		if (normalized==0) {
			train_count++;
			for (int term:terms) add_count(term);
		}
		float length_norm= (float)1.0/ (float)terms.length;
		float length_norm2= (float) Math.pow(length_norm, length_scale);
		length_norm= (float) Math.pow(length_norm, 1.0-length_scale);
		int t= 0;
		for (float count: counts) counts[t++]= (float)Math.log(1.0+count * length_norm)*length_norm2;
	}

	public float[] length_normalize_wordseq(int[] terms){
		int len= terms.length, i= 0;
		float[] weights= new float[len];
		Hashtable<Integer, Integer> counts= new Hashtable<Integer, Integer>();
		for(int term:terms) {
			if (term==-1) continue;
			Integer term2= term;
			if (counts.containsKey(term2)) counts.put(term2, counts.get(term2)+1);
			else counts.put(term2, new Integer(1));
		}
		if (normalized==0) {
			//add_doc();
			for (Map.Entry<Integer, Integer> entry : counts.entrySet())
				add_count(new Integer(entry.getKey()));
		}
		int uniq_len= counts.size();
		float lnorm= (float)1.0/ (float)uniq_len;
		float lnorm2= (float) Math.pow(lnorm, length_scale);
		lnorm= (float) Math.pow(lnorm, 1.0-length_scale);

		for(int term:terms) {
			if (term==-1) {i++; continue;}
			int count= counts.get((Integer)term);
			weights[i++]= (float)Math.log(1.0+count *lnorm)*(lnorm2/count);
		}
		return weights;
	}

	public double get_idf(Integer term) {
		if (normalized==1) return idfs.containsKey(term) ? idfs.get(term): 0.0;
		float idf= idfs.containsKey(term) ? idfs.get(term): (float)0.0;
		return Math.log(Math.max(idf_lift+(double)train_count/idf, 1.0));
	}
}