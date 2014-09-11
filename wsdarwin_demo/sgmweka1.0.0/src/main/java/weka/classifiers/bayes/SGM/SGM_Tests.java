package weka.classifiers.bayes.SGM;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Hashtable;

public class SGM_Tests {
	public static void main(String[] args) throws Exception{
		long start_time= System.currentTimeMillis();
		// Defaults:
		String model_use= "classification";
		int normalize_model= 1;
		int binary_model= 0;
		int powerset_model= 0;
		int knn= 0;
		boolean reverse_nb= false;
		boolean use_label_weights= false;

		int batch_size= 1000000;
		int cond_hashsize= 10000000;
		double prune_count_table= -6.0;
		double prune_count_insert= -2.5;		
		double min_count= 0.0;
		double length_scale= 0.5;
		double idf_lift= 0.0;
		double label_threshold= 0.0;
		int max_labelsize= 1;
		double cond_unif_weight= 0.01;
		double cond_bg_weight= 0.0;
		double cond_scale= 1.0;
		double prior_scale= 0.5;

		String workdir= System.getProperty("user.dir");
		String train_file= "";
		String test_file= "";
		String save_model= "";
		String load_model= "";
		String results_file= "";
		//PrintWriter logf= new PrintWriter(new FileWriter(log_file));

		Date date= new Date(start_time);
		SimpleDateFormat ft= new SimpleDateFormat ("E yyyy.MM.dd 'at' hh:mm:ss a zzz");
		System.out.println("time:"+ft.format(date));
		String argtmp=" "; for (int i= 0;i < args.length; i++) argtmp+= args[i]+" ";
		if (args.length>0) {
			Hashtable<String, String> arguments= new Hashtable<String, String>();
			String[] args2= argtmp.split(" -");
			for (int i= 0;i < args2.length; i++) {
				String[] tmp2= args2[i].split(" "); 
				//if (tmp2.length!= 2) continue;
				if (tmp2.length!= 2) {String[] tmp3= {tmp2[0], ""}; tmp2= tmp3;}
				arguments.put(tmp2[0], tmp2[1].replace('\\', ' '));
			}
			if (arguments.containsKey("model_use")) model_use= arguments.get("model_use");
			if (arguments.containsKey("normalize_model")) normalize_model= Integer.parseInt(arguments.get("normalize_model"));
			if (arguments.containsKey("workdir")) workdir= arguments.get("workdir");
			if (arguments.containsKey("train_file")) train_file= arguments.get("train_file");
			if (arguments.containsKey("test_file")) test_file= arguments.get("test_file");
			if (arguments.containsKey("load_model")) load_model= arguments.get("load_model");
			if (arguments.containsKey("save_model")) save_model= arguments.get("save_model");
			if (arguments.containsKey("results_file")) results_file= arguments.get("results_file");
			if (arguments.containsKey("batch_size")) batch_size= (Integer)(int)Double.parseDouble(arguments.get("batch_size"));
			if (arguments.containsKey("cond_hashsize")) cond_hashsize= (Integer)(int)Double.parseDouble(arguments.get("cond_hashsize"));
			if (arguments.containsKey("prune_count_table")) prune_count_table= Double.parseDouble(arguments.get("prune_count_table"));
			if (arguments.containsKey("prune_count_insert")) prune_count_insert= Double.parseDouble(arguments.get("prune_count_insert"));
			if (arguments.containsKey("min_count")) min_count= Double.parseDouble(arguments.get("min_count"));
			if (arguments.containsKey("length_scale")) length_scale= Double.parseDouble(arguments.get("length_scale"));
			if (arguments.containsKey("idf_lift")) idf_lift= Double.parseDouble(arguments.get("idf_lift"));
			if (arguments.containsKey("cond_unif_weight")) cond_unif_weight= Double.parseDouble(arguments.get("cond_unif_weight"));
			if (arguments.containsKey("cond_bg_weight")) cond_bg_weight= Double.parseDouble(arguments.get("cond_bg_weight"));
			if (arguments.containsKey("cond_scale")) cond_scale= Double.parseDouble(arguments.get("cond_scale"));
			if (arguments.containsKey("prior_scale")) prior_scale= Double.parseDouble(arguments.get("prior_scale"));
			if (arguments.containsKey("label_threshold")) label_threshold= Double.parseDouble(arguments.get("label_threshold"));
			if (arguments.containsKey("max_labelsize")) max_labelsize= (Integer)(int)Double.parseDouble(arguments.get("max_labelsize"));
			if (arguments.containsKey("binary_model")) binary_model= Integer.parseInt(arguments.get("binary_model"));
			if (arguments.containsKey("powerset_model")) powerset_model= Integer.parseInt(arguments.get("powerset_model"));
			if (arguments.containsKey("knn")) knn= (Integer)(int)Double.parseDouble(arguments.get("knn"));
			if (arguments.containsKey("reverse_nb")) reverse_nb= true;
			if (arguments.containsKey("use_label_weights")) use_label_weights= true;
		}
		workdir= (workdir+"/").replace("//", "/");
		if (!train_file.startsWith("/")) train_file= (workdir + "/"+ train_file).replace("//", "/");
		if (!test_file.startsWith("/")) test_file= (workdir + "/"+ test_file).replace("//", "/");
		if (!results_file.startsWith("/")) results_file= (workdir + "/"+ results_file).replace("//", "/");
		//if (!model_file.startsWith("/")) model_file= (workdir + "/"+ model_file).replace("//", "/");
		if (!load_model.startsWith("/")) load_model= (workdir + "/"+ load_model).replace("//", "/");
		if (!save_model.startsWith("/")) save_model= (workdir + "/"+ save_model).replace("//", "/");

		PrintWriter resultsf= null;
		if (!results_file.equals(workdir)) resultsf= new PrintWriter(new FileWriter(results_file));
		//logf.println("args:"+argtmp);
		//logf.close();
		//if (true) return;

		TFIDF tfidf= new TFIDF(length_scale, idf_lift);
		SGM sgm= new SGM();
		sgm.debug= 1;
		sgm.init_model(cond_hashsize, tfidf, max_labelsize);
		if (model_use.equals("retrieval") && binary_model!=1 &&knn<1) sgm.model.prior_lprobs= null;
		if (binary_model==1) sgm.binary_model= 1;
		if (powerset_model==1) sgm.use_powerset();
		if (knn>0) sgm.use_knn(knn);
		if (reverse_nb) sgm.reverse_nb= true;
		if (!load_model.endsWith("/")) sgm.load_model(load_model);
		else if (!train_file.endsWith("/")) {
			int incr= 40000;
			int max_read= incr * 1000000000;
			int total= 0;
			int read;
			sgm.open_stream(train_file, incr, use_label_weights);
			while ((read= sgm.get_features(incr))!=0) {
				total+= read;
				System.out.println("Reading data. Time:"+(System.currentTimeMillis()-start_time)+" Read:"+total);
				sgm.train_model(batch_size, prune_count_insert);
				if (total>= max_read || sgm.model.cond_lprobs.size()==0) break;
			}
			sgm.close_stream();
			System.out.println("Model trained. Time:"+(System.currentTimeMillis()-start_time));
		}
		if (sgm.model.cond_lprobs.size()==0) return;
		if (normalize_model==1) {
			System.out.println("Normalizing model. sgm.model.cond_lprobs.size:"+  sgm.model.cond_lprobs.size());
			tfidf.normalize(min_count);
			if (prune_count_table>-1000000.0) sgm.prune_counts(prune_count_table, cond_hashsize);
			sgm.apply_idfs();
			System.out.println("sgm.model.cond_lprobs.size:"+  sgm.model.cond_lprobs.size());
			if (cond_scale!=1.0) sgm.scale_conditionals(cond_scale);
			sgm.normalize_model();
			sgm.smooth_conditionals(cond_unif_weight, cond_bg_weight);
			sgm.smooth_prior(prior_scale);
			System.out.println("sgm.model.cond_lprobs.size():"+sgm.model.cond_lprobs.size());
			System.out.println("Model normalized. Time:"+(System.currentTimeMillis()-start_time));
		}
		if (!save_model.endsWith("/")) sgm.save_model(save_model);
		sgm.prepare_inference();
		if (!test_file.endsWith("/")) {
			sgm.prepare_evaluation();
			System.out.println("Evaluating: "+test_file+". Time:"+(System.currentTimeMillis()-start_time));
			int incr= 40000;
			int total= 0;
			int read;
			sgm.open_stream(test_file, incr, use_label_weights);
			while ((read= sgm.get_features(incr))!=0) {
				total+= read;
				System.out.println("Reading data. Time:"+(System.currentTimeMillis()-start_time));
				if (model_use.equals("retrieval"))
					sgm.retrieve_data(label_threshold, resultsf);
				if (model_use.equals("classification"))
					sgm.classify_data(resultsf);
				if (read!=incr) break;
			}
			sgm.close_stream();
			if (resultsf!=null) resultsf.close();
			//logf.close();

			System.out.println("Done. Time:"+(System.currentTimeMillis()-start_time)+" "+total);
			sgm.print_evaluation_summary();
		}
	}
}

