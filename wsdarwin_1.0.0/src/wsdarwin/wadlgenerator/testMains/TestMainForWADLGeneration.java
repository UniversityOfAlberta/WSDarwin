package wsdarwin.wadlgenerator.testMains;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import wsdarwin.comparison.delta.Delta;
import wsdarwin.parsers.WADLParser;
import wsdarwin.util.DeltaUtil;
import wsdarwin.util.XMLGenerator;
import wsdarwin.wadlgenerator.RequestAnalyzer;
import wsdarwin.wadlgenerator.Response2XSD;
import wsdarwin.wadlgenerator.model.WADLFile;
import wsdarwin.wadlgenerator.model.xsd.XSDFile;

import java.net.*;
import java.io.*;

@Path(value="/api")
public class TestMainForWADLGeneration {

	@Context private HttpServletRequest servletRequest;
	//@Context private HttpServletContext servletContext;
	
	public static final Boolean DEBUG = true;
	
	private static final String VENDOR = "twitter";
	// -------------------------------------------------------------------------
	// java app directories
	private static final String PATH_PREFIX = "files/icsm2014/"+VENDOR;
	private static final String FILENAME_DIR = PATH_PREFIX+"/wadl/";
	//private static final String XSD_DIR = PATH_PREFIX+"/xsd/";
	private static final String RESPONSE_DIR = PATH_PREFIX+"/responses/";
	
	// -------------------------------------------------------------------------
	// web service directories
	// eclipse's tomcat path
	//private static final String PATH_PREFIX_TWO = "C:/Users/mihai/eclipse_workspace/wsdarwin_1.0.0/WebContent/files/icsm2014/"+VENDOR;
	// tomcat path
	private static final String PATH_PREFIX_TWO = "C:/Users/mihai/tomcat_server/webapps/wsdarwin_1.0.0/files/icsm2014/"+VENDOR;
	private static final String PATH_PREFIX_FILES = "C:/Users/mihai/tomcat_server/webapps/wsdarwin_1.0.0/files/";
	//private static final String HOST_PATH = "localhost:8080/wsdarwin_1.0.0/";
	
	//private static final String PATH_PREFIX_TWO = HOST_PATH + "files/icsm2014/"+VENDOR;
	private static final String FILENAME_DIR_TWO = PATH_PREFIX_TWO+"/wadl/";
	private static final String RESPONSE_DIR_TWO = PATH_PREFIX_TWO+"/responses/";
	private static final String UPLOADED_WADLS = PATH_PREFIX_TWO+"/uploadedWADL/";
	
	private static final String LOCALHOST_FILES_PATH = "http://localhost:8080/wsdarwin_1.0.0/files/";
	private static final String LOCALHOST_WADL_PATH = "http://localhost:8080/wsdarwin_1.0.0/files/icsm2014/twitter/wadl/";
	//private static final String LOCALHOST_WADL_PATH = PATH_PREFIX_TWO + "http://localhost:8080/wsdarwin_1.0.0/files/icsm2014/twitter/wadl/";
	
	static WADLFile wadl_one;
	static WADLFile wadl_two;
	
	public String randomString(String chars, int length) {
		  Random rand = new Random();
		  StringBuilder buf = new StringBuilder();
		  for (int i=0; i<length; i++) {
		    buf.append(chars.charAt(rand.nextInt(chars.length())));
		  }
		  return buf.toString();
	}
	
	@GET
	@Path("/analyze")
    public String singleURL(@QueryParam("newURLs") String newURLs, @QueryParam("newUppedFiles") String newUppedFiles, @QueryParam("sessionid") String sessionid, @QueryParam("type") String call_type, @QueryParam("compareURLs") String compareURLs, @QueryParam("compareWADLfiles") String compareWADLfiles) throws Exception {
		//System.out.println("get aram is " + jsonObj );
		
		//Gson gson = new Gson();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		ArrayList<String> analyze_URLs = gson.fromJson(newURLs, ArrayList.class);
		ArrayList<String> compare_URLs = gson.fromJson(compareURLs, ArrayList.class);
		ArrayList<String> analyzed_WADLurls = gson.fromJson(newUppedFiles, ArrayList.class);
		ArrayList<String> compare_WADLurls = gson.fromJson(compareWADLfiles, ArrayList.class);
		
		WADLFile helloworld = null;
		
		if (helloworld == null){
			System.out.println("helloworld is null !");
		}
		
		String analysis_wadl_merged_path_url = getWadl(analyze_URLs, analyzed_WADLurls, "analyzeURLS");
		String compare_wadl_merged_path_url = "";
		
		Delta delta = null;
		if ( ( (compare_URLs != null) || (compare_WADLurls != null) ) && ( (compare_WADLurls.size() > 0) || (compare_URLs.size() > 0) ) ){
			compare_wadl_merged_path_url = getWadl(compare_URLs, compare_WADLurls, "compareURLS");
			
			System.out.println("analysis path: " + analysis_wadl_merged_path_url);
			System.out.println("compare path: " + compare_wadl_merged_path_url);
			
			// parse the 2 wadl's into a wsdarwin.model and diff them
			WADLParser parser1 = new WADLParser(new File(FILENAME_DIR_TWO + "twitterMerged.wadl"));
			WADLParser parser2 = new WADLParser(new File(FILENAME_DIR_TWO + "twitterMerged2.wadl"));
			
			delta = parser1.getService().diff(parser2.getService());
			
			//DeltaUtil.findMoveDeltas(delta);
			delta.printDelta(0);
			
			System.out.println("Diff finished");
			System.out.println("Finished!!");
		}
		
		ArrayList<String> returnArray = new ArrayList<String>();
		
		// if a session id is set, return it; if a session id is not set, create a new one
		String sid = new String("session_");	//session id
		if (sessionid.equals("")){
			String lettersToRandomize = "qwertyuioplkjhgfdsazxcvbnm";
			sid += randomString(lettersToRandomize ,11);
			returnArray.add(sid);
		} else {
			returnArray.add(sessionid);
		}
		//System.out.println("the session id: " + returnArray.get(0));
		
		returnArray.add(analysis_wadl_merged_path_url);
		returnArray.add(compare_wadl_merged_path_url);
		System.out.println("call type is: " + call_type);
		if (call_type.equals("compare")){
			System.out.println("creating xml comparison file");
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			DOMImplementation domImpl = builder.getDOMImplementation();
			Document xmldoc = domImpl.createDocument("localXMLdelta", "deltas", null);
			
			delta.createXMLElement(xmldoc, xmldoc.getDocumentElement());
			//String comparison_file_path = 
			writeXML(domImpl, xmldoc, PATH_PREFIX_FILES + "deltaComparison.xml");
			System.out.println("done creating xml comparison file");
			//String deltaString = gson.toJson(delta);
			returnArray.add(LOCALHOST_FILES_PATH + "deltaComparison.xml");
	    }
		
		
		
		String ret = gson.toJson(returnArray);
		
		
		//System.out.println( "gson.. " + abc );
		//for (int k = 0; k < urls.size(); k++){
			//System.out.println("gson.. " + urls.get(k));
		//}
		
		/*File myFile = new File("C:/Users/mihai/jsonWADL.txt");
        myFile.createNewFile();
        FileOutputStream fOut = new FileOutputStream(myFile);
        OutputStreamWriter myOutWriter =new OutputStreamWriter(fOut);
        myOutWriter.append(jsonRet);
        myOutWriter.close();
        fOut.close();*/
		
		//JSONObject pilot = new JSONObject();
		//pilot.put( "firstName", "John");
        //pilot.put( "lastName", "Adams");
        //System.out.println("pilot : " + pilot);

		/*if (jsonObj == null){
			System.out.println("URL PATH IS NOT DEFINED");
		}
		
		
	*/
		 
		 // to diff 2 files..
		/*System.out.println("Diff started");
*/
		
		/*
		Gson gsonObj = new Gson();
		getWadl(jsonObj);*/
		
		
		return ret;
	}	
	
	private void writeXML(DOMImplementation domImpl, Document xmldoc,
			String filename) throws FileNotFoundException {
			DOMImplementationLS ls = (DOMImplementationLS) domImpl;
	        LSSerializer lss = ls.createLSSerializer();
	        LSOutput lso = ls.createLSOutput();
	        lso.setByteStream(new FileOutputStream(new File(filename)));
	        
	        lss.write(xmldoc, lso);
	        
	        //Element root = xmldoc.get
	        //System.out.println("root name ? " + getStringFromDocument(xmldoc) );
	        //String jsonRet = gson.toJson( getStringFromDocument(xmldoc) );
	        //System.out.println("THE FINAL XML: " + xmldoc.toString() );
	}
	
	/**
	 * @param args
	 */
	public static String getWadl(ArrayList<String> url_list, ArrayList<String> analyzedWADLurls, String call_type) {
		try {
			ArrayList<String> requests = new ArrayList<String>();
			ArrayList<String> uris = new ArrayList<String>();
			HashMap<String, XSDFile> responses = new HashMap<String, XSDFile>();
			
			// Read request file BY FILE PATH:
			//BufferedReader testIn = new BufferedReader(new FileReader(new File(PATH_PREFIX_TWO+"/request.txt")));
			
			// Read request file BY URL:
			//URL requestURL = new URL(PATH_PREFIX_TWO+"/request.txt");
			//BufferedReader testIn = new BufferedReader(new InputStreamReader(requestURL.openStream()));
			
			//String line = testIn.readLine();
			
			ArrayList<String> links = new ArrayList<String>();
			ArrayList<String> wadl_paths = new ArrayList<String>();
			WADLFile ret = null;
			
			//links.add(new String("005 GET https://api.github.com/users/mralexgray/repos"));
			//links.add(new String("001 GET http://api.openweathermap.org/data/2.1/find/city?lat=55&lon=37&cnt=10"));
			
			// ===============================================================================================================================
			// some api calls that return json
			// https://api.github.com/users/penguinsource/repos
			// https://api.github.com/users/mralexgray/repos
			// https://api.github.com/users/fokaefs/repos
			// https://api.github.com/users/mojombo
			// https://api.github.com/users/willcodeforfoo
			// 
			// https://graph.facebook.com/oprescu3
			// http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=HeyThere
			// http://api.openweathermap.org/data/2.1/find/city?lat=55&lon=37&cnt=10
			// doesnt work .. http://www.google.com/calendar	/feeds/developer-calendar@google.com/public/full?alt=json
			// http://en.wikipedia.org/w/api.php?format=json&action=query&titles=Main%20Page&prop=revisions&rvprop=content&format=json
			// http://www.biomedcentral.com/webapi/1.0/latest_articles.json
			// http://api.artsholland.com/rest/production.json?per_page=5&page=4
			// http://www.a3ultimate.com/API/Options/json/33916;3613214975,33916;3613214975,33916;3613214975,33916;3613214975
			// ===============================================================================================================================
			
			for (int i = 0; i < url_list.size(); i++){
				if ( "".equals( url_list.get(i) ) ){
					//System.out.println("11 ITS NULL");
				} else {
					String proper_url = new String( i + " GET " + url_list.get(i) + "" );
					links.add(proper_url);
					//System.out.println(" --> " + proper_url);
				}
			}			
			
			Iterator<String> it = links.iterator();
			
			while(it.hasNext()){
			    String line = it.next();
				requests.add(line);
				String[] tokens = line.split(" ");
				uris.add(tokens[2]);
			}
			
			/*
			while(line != null) {
				requests.add(line);
				String[] tokens = line.split(" ");
				uris.add(tokens[2]);
				line = testIn.readLine();
			}*/
			RequestAnalyzer analyzer = new RequestAnalyzer();
			String resourceBase = "";
			if ( (uris != null) && (uris.size() > 0)){
				resourceBase = analyzer.batchRequestAnalysis(uris);
				
				for(String methodName : analyzer.getMethodNamesFromBatch(uris)) {
					responses.put(methodName, new XSDFile());
				}
			}

			String xsdFilename = null;

			XMLGenerator generator = new XMLGenerator();
	        //System.out.println("[Interface retrieval]");
			
			// create empty merged WADL file
			System.out.println("THE CALL_TYPE IS: !!! " + call_type);
	        String mergedFileName = "";
	        if (call_type.equals("analyzeURLS")){
	        	mergedFileName = "Merged.wadl";
	        } else if (call_type.equals("compareURLS")){
	        	mergedFileName = "Merged2.wadl";
	        }
			WADLFile mergedWADL = new WADLFile(FILENAME_DIR_TWO+VENDOR+mergedFileName, null, null);
			if(DEBUG) System.out.println("** Merged WADLFile: "+mergedWADL.getIdentifier()+" **");
			HashSet<XSDFile> grammarSet = new HashSet<XSDFile>();
			
			// Looping over test queries
			for(String requestLine : requests) {
				//System.out.println(" THE REQUEST LINE: " + requestLine);
				String[] tokens = requestLine.split(" ");
				String id = "";
				String methodName = "";
				String urlLine = "";
				if (tokens.length>1) {
					id = tokens[0];
					methodName = tokens[1];
					urlLine = tokens[2];
				}
				analyzer.resetUriString(urlLine);
				final String FILENAME_XML  = VENDOR+id+".json";
				final String FILENAME_WADL = "late_WADLresponse"+id+".wadl";
				final String FILENAME_XSD  = "response"+id+".xsd";
				
		        //System.out.println(" Request #"+id+"");
		        
		        // URLConnection
				URL yahoo = new URL(urlLine);
		        URLConnection yc = yahoo.openConnection();
		        BufferedReader in = new BufferedReader(
                new InputStreamReader(yc.getInputStream()));
		        String inputLine;

		        //BufferedWriter out = new BufferedWriter(new FileWriter(new File(RESPONSE_DIR+FILENAME_XML)));
		        BufferedWriter out = new BufferedWriter(new FileWriter(new File(RESPONSE_DIR_TWO+FILENAME_XML)));

		        while ((inputLine = in.readLine()) != null) {
		            out.write(inputLine);
		            out.newLine();
		        }
		        in.close();
		        out.close();
		        
		        Response2XSD xsdBuilder = new Response2XSD();
		        
				//xsdBuilder.buildXSDFromJSON(RESPONSE_DIR+FILENAME_XML, analyzer.getMethodID());
		        xsdBuilder.buildXSDFromJSON(RESPONSE_DIR_TWO+FILENAME_XML, analyzer.getMethodID());
		        
		        XSDFile xsdFile = xsdBuilder.getXSDFile();
				XSDFile mergedXSDFile = responses.get(analyzer.getMethodID());
				
				//xsdFilename = mergedXSDFile.getFilename();
				//generator.createXSD(xsdFile);
					
				mergedXSDFile.compareToMerge(xsdFile);
				
		        //WADLFile newWADL = new WADLFile(FILENAME_DIR+FILENAME_WADL, urlLine, xsdBuilder.convertFromXSD(mergedXSDFile.getResponseType()));
				WADLFile newWADL = new WADLFile(FILENAME_DIR_TWO +FILENAME_WADL, urlLine, xsdBuilder.convertFromXSD(mergedXSDFile.getResponseType()));
				grammarSet.add(xsdFile);		// TODO later change to Identifier + XSDElement
				newWADL.buildWADL(grammarSet, analyzer, resourceBase, methodName, 200);
		        generator.createWADL(newWADL);
		        
		        // Call diff&merge methods from sub-objects 
		        mergedWADL.compareToMerge(newWADL);
		        
		        
		        //System.out.println(" wadl file: " + LOCALHOST_WADL_PATH+FILENAME_WADL);
		        // Add the newly generated wadl's path to the list wadl_paths
		        wadl_paths.add(LOCALHOST_WADL_PATH+FILENAME_WADL);
		        
				//requestLine = testIn.readLine();
				//requestLine = it.next();
				//System.out.println("-- > " + requestLine);
				
			}
			
			
			// merging uploaded wadl files
	        /*WADLFile uppedWADL = new WADLFile("C:/Users/mihai/tomcat_server/webapps/wsdarwin_1.0.0/files/icsm2014/twitter/uploadedWADL/file0.wadl");
	        WADLFile uppedWADL2 = new WADLFile("C:/Users/mihai/tomcat_server/webapps/wsdarwin_1.0.0/files/icsm2014/twitter/uploadedWADL/file1.wadl");
	        try {
				//System.out.println("<<< reading wadl >>>");
				uppedWADL.readWADL();
				uppedWADL2.readWADL();
				//System.out.println("<<< finished reading wadl >>>");
			} catch (SAXException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	        // Call diff&merge methods from sub-objects
	        mergedWADL.compareToMerge(uppedWADL);
	        mergedWADL.compareToMerge(uppedWADL2);*/
			if ( (analyzedWADLurls != null) && (analyzedWADLurls.size() > 0)){
				//
				//System.out.println("SIZE OF ANALYZED WADL URLS: " + analyzedWADLurls.size());
				//System.out.println("SIZE OF ANALYZED WADL URLS: " + analyzedWADLurls.get(0));
				for (int i = 0; i < analyzedWADLurls.size(); i++){
					System.out.println("ADDING ANALYZED URL: " + analyzedWADLurls.get(i));
			        // URLConnection
					URL yahoo = new URL(analyzedWADLurls.get(i));
			        URLConnection yc = yahoo.openConnection();
			        BufferedReader in = new BufferedReader(
		            new InputStreamReader(yc.getInputStream()));
			        String inputLine;
			        
			        //BufferedWriter out = new BufferedWriter(new FileWriter(new File(RESPONSE_DIR+FILENAME_XML)));
			        String uppedWADLpath = UPLOADED_WADLS+"file"+i+".wadl";
			        BufferedWriter out = new BufferedWriter(new FileWriter(new File(uppedWADLpath)));
		
			        while ((inputLine = in.readLine()) != null) {
			            out.write(inputLine);
			            out.newLine();
			        }
			        
			        in.close();
			        out.close();
			        
			        WADLFile uppedWADL = new WADLFile(uppedWADLpath);
					try {
						//System.out.println("<<< reading wadl >>>");
						uppedWADL.readWADL();
						//System.out.println("<<< finished reading wadl >>>");
					} catch (SAXException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					System.out.println("-> Merging wadl with path: " + uppedWADLpath);
			        // Call diff&merge methods from sub-objects
			        mergedWADL.compareToMerge(uppedWADL);
			        System.out.println("FILE MERGED !");
				}
			}
			
			//
			
			//InputStream inputStream = TestMainForWADLGeneration.class.getResourceAsStream("files/icsm2014/twitter/wadl/WADLResponse001.wadl");
			/*
			File randomWADLfile	= new File("C:/Users/mihai/tomcat_server/webapps/wsdarwin_1.0.0/files/icsm2014/twitter/wadl/twitterMerged.wadl");
			File randomWADLfile2= new File("C:/Users/mihai/tomcat_server/webapps/wsdarwin_1.0.0/files/icsm2014/twitter/wadl/imdbMerged.wadl");
			WADLParser parser3 = new WADLParser(randomWADLfile);
			//WADLParser parser1 = new WADLParser(new File("files/icsm2014/twitter/wadl/WADLResponse001.wadl"));
			System.out.println("=====> parser1:" + parser3);
			if (randomWADLfile != null){
				System.out.println("File opened");
			}
			if (parser3 != null){
				System.out.println("WADLParser initiated");
			}
			
			System.out.println("Diff started");
			WADLParser parser1 = new WADLParser(randomWADLfile);
			WADLParser parser2 = new WADLParser(randomWADLfile2);

			Delta delta = parser1.getService().diff(parser2.getService());
			
			System.out.println("<<< reading delta >>>");
			DeltaUtil.findMoveDeltas(delta);
			delta.printDelta(0);
			System.out.println("<<< finished reading delta >>>");
			*/
			

			
			/*
			WADLFile deserialized = WADLFile.deserializeFile("C:/Users/mihai/tomcat_server/webapps/wsdarwin_1.0.0/files/icsm2014/twitter/wadl/hey.ser");
			if (deserialized != null){
				System.out.println("WADLFile deserialized");
			}*/
			//}
			

			/*if (call_type.equals("compare")){
				// READING MERGED WADL
				System.out.println("<<< reading end merged wadl >>>");
				
				WADLFile no = new WADLFile("C:/Users/mihai/tomcat_server/webapps/wsdarwin_1.0.0/files/icsm2014/twitter/wadl/twitterMerged3.wadl");
				try {
					System.out.println("<<< reading wadl >>>");
					no.readWADL();
					System.out.println("<<< finished reading wadl >>>");
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println("End of run !");
				
				
		        // Call diff&merge methods from sub-objects 
		        mergedWADL.compareToMerge(no);
			}*/
			
			// write merged WADL file only once
			generator.createWADL(mergedWADL);
			
			//
			/*for(String methodID : responses.keySet()) {
				generator.createXSD(responses.get(methodID));
			}*/
			
			// if BufferedReader is used (for URL and local file requests)
			//testIn.close();
			
			/*System.out.println("");
			System.out.println("[Comparison]");
			Delta delta = mergedWADL.compare(mergedWADL);
			delta.printDelta(0);*/			
			
			String MERGED_WADL_PATH = LOCALHOST_WADL_PATH+VENDOR+"Merged.wadl";
			String MERGED_WADL_PATH_TWO = LOCALHOST_WADL_PATH+VENDOR+"Merged2.wadl";
	        
			// Add the merged wadl's path to the list wadl_paths
	        wadl_paths.add(LOCALHOST_WADL_PATH+VENDOR+"Merged.wadl");
	        
			//return wadl_paths;
			//return mergedWADL;
	        
	        // returning a DOM Document
			//Document xml_doc_merged = generator.createWADL(mergedWADL);
			//return xml_doc_merged;
	        //mergedWADL.serializeFile();
	        if (call_type.equals("analyzeURLS")){
	        	wadl_one = mergedWADL;
	        	return MERGED_WADL_PATH;
	        } else if (call_type.equals("compareURLS")){
	        	wadl_two = mergedWADL;
	        	return MERGED_WADL_PATH_TWO;
	        }
	        return "";
			
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		return null;
	}

}
