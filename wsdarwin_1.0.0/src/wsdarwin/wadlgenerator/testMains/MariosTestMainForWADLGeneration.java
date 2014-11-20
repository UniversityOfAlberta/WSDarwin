package wsdarwin.wadlgenerator.testMains;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import wsdarwin.comparison.delta.Delta;
import wsdarwin.comparison.delta.MapDelta;
import wsdarwin.parsers.WADLParser;
import wsdarwin.service.WSDarwinService;
import wsdarwin.util.DeltaUtil;
import wsdarwin.util.XMLGenerator;
import wsdarwin.wadlgenerator.RequestAnalyzer;
import wsdarwin.wadlgenerator.Response2XSD;
import wsdarwin.wadlgenerator.model.WADLFile;
import wsdarwin.wadlgenerator.model.xsd.XSDFile;

public class MariosTestMainForWADLGeneration {

	/**
	 * @param args
	 */
	public static final Boolean DEBUG = true;
	
	private static final String VENDOR = "tumblr";
	private static final String PATH_PREFIX = "files/"+VENDOR+"/v2";
	
	private static final String FILENAME_DIR = PATH_PREFIX+"/wadl/";
	private static final String XSD_DIR = PATH_PREFIX+"/xsd/";
	private static final String RESPONSE_DIR = PATH_PREFIX+"/responses/";

	public static void main(String[] args) {
		double time = System.currentTimeMillis();
		//testGeneration();
		//testComparison();
		//testMapping();
		testGenerateClient();
		System.out.println(System.currentTimeMillis()-time);

	}
	
	private static void testMapping() {
		WSDarwinService service  = new WSDarwinService();
		ArrayList<String> urlRequests = new ArrayList<String>();
		urlRequests.add("http://peacecorps.tumblr.com/api/read/json");
		WADLFile file1 = service.generateWADL(urlRequests, new ArrayList<String>(), FILENAME_DIR+VENDOR+"1.wadl", RESPONSE_DIR, false);
		
		urlRequests = new ArrayList<String>();
		urlRequests.add("http://api.tumblr.com/v2/blog/peacecorps.tumblr.com/posts?api_key=nfCjygcnaZH3nDTf09XsPum2IQkqMO1H3wEjmYpnSy7ltOntGH");
		WADLFile file2 = service.generateWADL(urlRequests, new ArrayList<String>(), FILENAME_DIR+VENDOR+"2.wadl", RESPONSE_DIR, false);
		
		file1.mapByValue(file2);
		for(ArrayList<String> mappings : file1.getElementMappings()) {
			System.out.println("Source: \t"+mappings.get(0)+"\t Target: \t"+mappings.get(1)+"\t Distance: \t"+mappings.get(2));
		}
	}
	
	private static void testGenerateClient() {
		WSDarwinService service = new WSDarwinService();
		String resourcesBase="api.tumblr.com/";
		resourcesBase = resourcesBase.replace("/", "");
		
		String[] folders = resourcesBase.split("\\.");
		String packageName = folders[folders.length-1];
		for(int i=folders.length-2; i>=0; i--) {
			packageName+="."+folders[i];
		}
		service.generateClientProxy(FILENAME_DIR+VENDOR+".wadl", packageName, "files/wadl2java/bin/wadl2java.bat", "files/", "files/");
		
	}

	private static void testGeneration() {
		
		try {
			WSDarwinService service  = new WSDarwinService();
			ArrayList<String> urlRequests = new ArrayList<String>();
			BufferedReader in = new BufferedReader(new FileReader(new File(PATH_PREFIX+"/request.txt")));
			String line = in.readLine();
			while(line != null) {
				urlRequests.add(line);
				line = in.readLine();
			}
			in.close();
			WADLFile file = service.generateWADL(urlRequests, new ArrayList<String>(), FILENAME_DIR+VENDOR+".wadl", RESPONSE_DIR, false);
			String resourcesBase="";
			for(String base : file.getResourcesElements().keySet()) {
				resourcesBase = base;
			}
			resourcesBase = resourcesBase.replace("/", "");
			
			String[] folders = resourcesBase.split("\\.");
			String packageName = "";
			String sourceZipFolder = packageName = folders[folders.length-1];
			for(int i=folders.length-2; i>=0; i--) {
				packageName+="."+folders[i];
			}
			System.out.println(packageName);
			System.out.println(sourceZipFolder);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void testComparison() {
		WADLParser parser1 = new WADLParser(new File("files/tumblr/v1/wadl/tumblr.wadl"));
		WADLParser parser2 = new WADLParser(new File("files/tumblr/v2/wadl/tumblr.wadl"));
		Delta delta = parser1.getService().diff(parser2.getService());
		DeltaUtil.findMoveDeltas(delta);
		delta.printDelta(0);
	}

}
