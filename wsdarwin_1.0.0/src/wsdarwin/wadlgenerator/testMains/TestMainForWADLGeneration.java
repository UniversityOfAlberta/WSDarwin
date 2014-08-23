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
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Pattern;

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
import wsdarwin.comparison.delta.MapDelta;
import wsdarwin.model.PrimitiveType;
import wsdarwin.parsers.WADLParser;
import wsdarwin.util.DeltaUtil;
import wsdarwin.util.XMLGenerator;
import wsdarwin.wadlgenerator.RequestAnalyzer;
import wsdarwin.wadlgenerator.Response2XSD;
import wsdarwin.wadlgenerator.model.WADLFile;
import wsdarwin.wadlgenerator.model.xsd.XSDComplexType;
import wsdarwin.wadlgenerator.model.xsd.XSDElement;
import wsdarwin.wadlgenerator.model.xsd.XSDFile;
import wsdarwin.wadlgenerator.model.xsd.XSDIType;

import java.net.*;
import java.io.*;

@Path(value="/api")
public class TestMainForWADLGeneration {
	
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
	// private static final String PATH_PREFIX_TWO = "C:/Users/mihai/eclipse_workspace/wsdarwin_1.0.0/WebContent/files/icsm2014/"+VENDOR;
	// tomcat path
	// 1:
	private static final String PATH_PREFIX_TWO = "C:/Users/mihai/tomcat_server/webapps/wsdarwin_1.0.0/files/icsm2014/"+VENDOR;
	
	// 2:
	//private static final String PATH_PREFIX_TWO = "/var/lib/tomcat7/webapps/wsdarwin_1.0.0/files/icsm2014/"+VENDOR;
	
	
	
	//private static final String PATH_PREFIX_TWO = "http://ssrg17.cs.ualberta.ca:8080/wsdarwin_1.0.0/files/icsm2014/"+VENDOR;
	
	//private static final String HOST_PATH = "localhost:8080/wsdarwin_1.0.0/";
	//private static final String PATH_PREFIX_TWO = HOST_PATH + "files/icsm2014/"+VENDOR;
	private static final String FILENAME_DIR_TWO = PATH_PREFIX_TWO+"/wadl/";
	//private static final String WADL_PATH = PATH_PREFIX_FILES;
	private static final String RESPONSE_DIR_TWO = PATH_PREFIX_TWO+"/responses/";
	private static final String UPLOADED_WADLS = PATH_PREFIX_TWO+"/uploadedWADL/";
	
	// 1:
	 private static final String PATH_PREFIX_FILES = "C:/Users/mihai/tomcat_server/webapps/wsdarwin_1.0.0/files/";
	 private static final String LOCALHOST_FILES_PATH = "http://localhost:8080/wsdarwin_1.0.0/files/";
	
	// 2:
	//private static final String PATH_PREFIX_FILES = "/var/lib/tomcat7/webapps/wsdarwin_1.0.0/files/";
	//private static final String LOCALHOST_FILES_PATH = "http://ssrg17.cs.ualberta.ca:8080/wsdarwin_1.0.0/files/";
	
	
	//private static final String LOCALHOST_WADL_PATH = "http://localhost:8080/wsdarwin_1.0.0/files/";
	//private static final String LOCALHOST_WADL_PATH = "http://localhost:8080/wsdarwin_1.0.0/files/icsm2014/twitter/wadl/";
	
	//private static final String LOCALHOST_WADL_PATH = PATH_PREFIX_TWO + "http://localhost:8080/wsdarwin_1.0.0/files/icsm2014/twitter/wadl/";
	
	static WADLFile wadl_one;
	static WADLFile wadl_two;
	static WADLFile testA = null;
	static WADLFile testB = null;
	
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
    public String analyze(
    		@QueryParam("newURLs") String newURLs, 
    		@QueryParam("newUppedFiles") String newUppedFiles,
    		@QueryParam("sessionid") String sessionid) throws Exception {
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		ArrayList<String> analyze_URLs = gson.fromJson(newURLs, ArrayList.class);
		ArrayList<String> analyze_WADLurls = gson.fromJson(newUppedFiles, ArrayList.class);
		ArrayList<String> returnArray = new ArrayList<String>();
		System.out.println(" -- > session id: " + sessionid);
		String session_id = getSessionID(sessionid);
		returnArray.add(session_id);
		
		String filenameA = PATH_PREFIX_FILES + "wadlA" + session_id + ".wadl";
		String localhostFilenameA = LOCALHOST_FILES_PATH + "wadlA" + session_id + ".wadl";
		System.out.println("filename A: " + filenameA );
		String analysis_wadl_merged_path_url = getWadl(analyze_URLs, analyze_WADLurls, "analyzeURLS", session_id, filenameA, localhostFilenameA);
		
		returnArray.add(analysis_wadl_merged_path_url);
		String ret = gson.toJson(returnArray);
		
		String regex = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
		if (Pattern.matches(regex, "http://google.com")){
			System.out.println("BLAH ONE !");
		}
		if (Pattern.matches(regex, "https://avatars.githubusercontent.com/u/100?v=2")){
			System.out.println("BLAH TWO !");
		}
		return ret;
		
	}
	
	
	@GET
	@Path("/compare")
    public String compare(
    		@QueryParam("newURLs") String newURLs, 
    		@QueryParam("newUppedFiles") String newUppedFiles,
    		@QueryParam("sessionid") String sessionid, 
    		@QueryParam("compareURLs") String compareURLs, 
    		@QueryParam("compareWADLfiles") String compareWADLfiles) throws Exception {
		
		System.out.println("COMPARE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		ArrayList<String> analyze_URLs = gson.fromJson(newURLs, ArrayList.class);
		ArrayList<String> compare_URLs = gson.fromJson(compareURLs, ArrayList.class);
		ArrayList<String> analyze_WADLurls = gson.fromJson(newUppedFiles, ArrayList.class);
		ArrayList<String> compare_WADLurls = gson.fromJson(compareWADLfiles, ArrayList.class);
		ArrayList<String> returnArray = new ArrayList<String>();
		
		String session_id = getSessionID(sessionid);
		returnArray.add(session_id);
		
		String filenameA = PATH_PREFIX_FILES + "wadlA" + session_id + ".wadl";
		String localhostFilenameA = LOCALHOST_FILES_PATH + "wadlA" + session_id + ".wadl";
		System.out.println("filename A: " + filenameA );
		String analysis_wadl_merged_path_url = getWadl(analyze_URLs, analyze_WADLurls, "analyzeURLS", session_id, filenameA, localhostFilenameA);
		String compare_wadl_merged_path_url = "";
		
		Delta delta = null;
		if ( ( (compare_URLs != null) || (compare_WADLurls != null) ) && ( (compare_WADLurls.size() > 0) || (compare_URLs.size() > 0) ) ){
			String filenameB = PATH_PREFIX_FILES + "wadlB" + session_id + ".wadl";
			String localhostFilenameB = LOCALHOST_FILES_PATH + "wadlB" + session_id + ".wadl";
			compare_wadl_merged_path_url = getWadl(compare_URLs, compare_WADLurls, "compareURLS", session_id, filenameB, localhostFilenameB);
			System.out.println("");
			System.out.println("analysis path: " + analysis_wadl_merged_path_url);
			System.out.println("compare path: " + compare_wadl_merged_path_url);
			
			// parse the 2 wadl's into a wsdarwin.model and diff them
			WADLParser parser1 = new WADLParser(new File(filenameA));
			WADLParser parser2 = new WADLParser(new File(filenameB));
			
			delta = parser1.getService().diff(parser2.getService());
						
			//DeltaUtil.findMoveDeltas(delta);
			delta.printDelta(0);
			//System.out.println("Diff finished");
		}
		
		returnArray.add(analysis_wadl_merged_path_url);
		returnArray.add(compare_wadl_merged_path_url);
		
		System.out.println("creating xml comparison file");
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		DOMImplementation domImpl = builder.getDOMImplementation();
		Document xmldoc = domImpl.createDocument("localXMLdelta", "deltas", null);
		
		delta.createXMLElement(xmldoc, xmldoc.getDocumentElement());
		//String comparison_file_path = 
		writeXML(domImpl, xmldoc, PATH_PREFIX_FILES + "/deltaComparison.xml");
		System.out.println("done creating xml comparison file");
		//String deltaString = gson.toJson(delta);
		returnArray.add(LOCALHOST_FILES_PATH + "deltaComparison.xml");
		
		String ret = gson.toJson(returnArray);		
		return ret;
	}
	
	@GET
	@Path("/crossServiceCompare")
    public String crossServiceCompare(
    		@QueryParam("newURLs") String newURLs, 
    		@QueryParam("newUppedFiles") String newUppedFiles,
    		@QueryParam("sessionid") String sessionid, 
    		@QueryParam("compareURLs") String compareURLs, 
    		@QueryParam("compareWADLfiles") String compareWADLfiles) throws Exception {
		
		System.out.println("CROSS SERVICE COMPARE !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		ArrayList<String> analyze_URLs = gson.fromJson(newURLs, ArrayList.class);
		ArrayList<String> compare_URLs = gson.fromJson(compareURLs, ArrayList.class);
		ArrayList<String> analyze_WADLurls = gson.fromJson(newUppedFiles, ArrayList.class);
		ArrayList<String> compare_WADLurls = gson.fromJson(compareWADLfiles, ArrayList.class);
		ArrayList<String> returnArray = new ArrayList<String>();
		System.out.println(" -- > session id: " + sessionid);
		String session_id = getSessionID(sessionid);
		returnArray.add(session_id);
		//MergedWADL_A_ + session_id
		
		String filenameA = PATH_PREFIX_FILES + "wadlA" + session_id + ".wadl";
		String localhostFilenameA = LOCALHOST_FILES_PATH + "wadlA" + session_id + ".wadl";
		System.out.println("filename A: " + filenameA );
		String analysis_wadl_merged_path_url = getWadl(analyze_URLs, analyze_WADLurls, "analyzeURLS", session_id, filenameA, localhostFilenameA);
		String compare_wadl_merged_path_url = "";
		
		Delta delta = null;
		if ( ( (compare_URLs != null) || (compare_WADLurls != null) ) && ( (compare_WADLurls.size() > 0) || (compare_URLs.size() > 0) ) ){
			String filenameB = PATH_PREFIX_FILES + "wadlB" + session_id + ".wadl";
			String localhostFilenameB = LOCALHOST_FILES_PATH + "wadlB" + session_id + ".wadl";
			compare_wadl_merged_path_url = getWadl(compare_URLs, compare_WADLurls, "compareURLS", session_id, filenameB, localhostFilenameB);
			System.out.println("");
			System.out.println("analysis path: " + analysis_wadl_merged_path_url);
			System.out.println("compare path: " + compare_wadl_merged_path_url);
			
			// parse the 2 wadl's into a wsdarwin.model and diff them
			WADLParser parser1 = new WADLParser(new File(filenameA));
			WADLParser parser2 = new WADLParser(new File(filenameB));
			
			delta = parser1.getService().diff(parser2.getService());
						
			//DeltaUtil.findMoveDeltas(delta);
			delta.printDelta(0);
			//System.out.println("Diff finished");
		}
		
		returnArray.add(analysis_wadl_merged_path_url);
		returnArray.add(compare_wadl_merged_path_url);

		//if (call_type.equals("compare")){
			System.out.println("creating xml comparison file");
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			DOMImplementation domImpl = builder.getDOMImplementation();
			Document xmldoc = domImpl.createDocument("localXMLdelta", "deltas", null);
			
			delta.createXMLElement(xmldoc, xmldoc.getDocumentElement());
			//String comparison_file_path = 
			writeXML(domImpl, xmldoc, PATH_PREFIX_FILES + "/deltaComparison.xml");
			System.out.println("done creating xml comparison file");
			//String deltaString = gson.toJson(delta);
			returnArray.add(LOCALHOST_FILES_PATH + "deltaComparison.xml");
	    //}
		
		System.out.println(" = = = = = = ");
		System.out.println("wadl files A and B exist  ? " + testA.getIdentifier() + " AND " + testB.getIdentifier() );
		
		testA.mapElement(testB);
		HashSet<MapDelta> bset = testB.getMapDeltas();
		HashSet<MapDelta> aset = testA.getMapDeltas();
		//for (int i = 0; i < abc.size(); i++){
		//	System.out.println("<><><> " + aset );
		//	System.out.println("<><><> " + bset );
		//}
		
		
		System.out.println("------------------------");
		System.out.println("Cross Service printDelta:");
		delta.printDelta(0);
		System.out.println("------------------------");
		//returnArray.add(LOCALHOST_FILES_PATH + "deltaComparison.xml");
		System.out.println("SUUUP: " + testA.getElementMappings());
		System.out.println("ABECEDAR: " + gson.toJson(testA.getElementMappings()));
		
		returnArray.add( gson.toJson( testA.getElementMappings() ) );
		
		String ret = gson.toJson(returnArray);		
		return ret;
	}
	
	/*
	@GET
	@Path("/analyze2")
    public String singleURL(@QueryParam("newURLs") String newURLs, @QueryParam("newUppedFiles") String newUppedFiles,
    		@QueryParam("sessionid") String sessionid, @QueryParam("type") String call_type,
    		@QueryParam("compareURLs") String compareURLs, @QueryParam("compareWADLfiles") String compareWADLfiles) throws Exception {
		
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		ArrayList<String> analyze_URLs = gson.fromJson(newURLs, ArrayList.class);
		ArrayList<String> compare_URLs = gson.fromJson(compareURLs, ArrayList.class);
		ArrayList<String> analyze_WADLurls = gson.fromJson(newUppedFiles, ArrayList.class);
		ArrayList<String> compare_WADLurls = gson.fromJson(compareWADLfiles, ArrayList.class);
		ArrayList<String> returnArray = new ArrayList<String>();
		System.out.println(" -- > session id: " + sessionid);
		String session_id = getSessionID(sessionid);
		returnArray.add(session_id);
		//MergedWADL_A_ + session_id
		
		String filenameA = PATH_PREFIX_FILES + "wadlA" + session_id + ".wadl";
		String localhostFilenameA = LOCALHOST_FILES_PATH + "wadlA" + session_id + ".wadl";
		System.out.println("filename A: " + filenameA );
		String analysis_wadl_merged_path_url = getWadl(analyze_URLs, analyze_WADLurls, "analyzeURLS", session_id, filenameA, localhostFilenameA);
		String compare_wadl_merged_path_url = "";
		
		Delta delta = null;
		if ( ( (compare_URLs != null) || (compare_WADLurls != null) ) && ( (compare_WADLurls.size() > 0) || (compare_URLs.size() > 0) ) ){
			String filenameB = PATH_PREFIX_FILES + "wadlB" + session_id + ".wadl";
			String localhostFilenameB = LOCALHOST_FILES_PATH + "wadlB" + session_id + ".wadl";
			compare_wadl_merged_path_url = getWadl(compare_URLs, compare_WADLurls, "compareURLS", session_id, filenameB, localhostFilenameB);
			System.out.println("");
			System.out.println("analysis path: " + analysis_wadl_merged_path_url);
			System.out.println("compare path: " + compare_wadl_merged_path_url);
			
			// parse the 2 wadl's into a wsdarwin.model and diff them
			WADLParser parser1 = new WADLParser(new File(filenameA));
			WADLParser parser2 = new WADLParser(new File(filenameB));
			
			delta = parser1.getService().diff(parser2.getService());
						
			//DeltaUtil.findMoveDeltas(delta);
			delta.printDelta(0);
			//System.out.println("Diff finished");
		}
		
		returnArray.add(analysis_wadl_merged_path_url);
		returnArray.add(compare_wadl_merged_path_url);

		//if (call_type.equals("compare")){
			System.out.println("creating xml comparison file");
			
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			DocumentBuilder builder = factory.newDocumentBuilder();
			DOMImplementation domImpl = builder.getDOMImplementation();
			Document xmldoc = domImpl.createDocument("localXMLdelta", "deltas", null);
			
			delta.createXMLElement(xmldoc, xmldoc.getDocumentElement());
			//String comparison_file_path = 
			writeXML(domImpl, xmldoc, PATH_PREFIX_FILES + "/deltaComparison.xml");
			System.out.println("done creating xml comparison file");
			//String deltaString = gson.toJson(delta);
			returnArray.add(LOCALHOST_FILES_PATH + "deltaComparison.xml");
	    //}
		
		System.out.println(" = = = = = = ");
		System.out.println("wadl files A and B exist  ? " + testA.getIdentifier() + " AND " + testB.getIdentifier() );
		
		testA.mapElement(testB);
		HashSet<MapDelta> bset = testB.getMapDeltas();
		HashSet<MapDelta> aset = testA.getMapDeltas();
		//for (int i = 0; i < abc.size(); i++){
			System.out.println("<><><> " + aset );
			System.out.println("<><><> " + bset );
		//}
		
		
		System.out.println("------------------------");
		System.out.println("Cross Service printDelta:");
		delta.printDelta(0);
		System.out.println("------------------------");
		//returnArray.add(LOCALHOST_FILES_PATH + "deltaComparison.xml");
		System.out.println("SUUUP: " + testA.getElementMappings());
		System.out.println("ABECEDAR: " + gson.toJson(testA.getElementMappings()));
		
		returnArray.add( gson.toJson( testA.getElementMappings() ) );
		
		String ret = gson.toJson(returnArray);		
		return ret;
	}
	 */
	
	// if a session id is set, return it; if a session id is not set, create a new one
	private String getSessionID(String sessionid) {
		String session_id = new String("_");	//session id
		if (sessionid.equals("")){
			// #1 : random session numbers
			//String lettersToRandomize = "qwertyuioplkjhgfdsazxcvbnm";
			//session_id += randomString(lettersToRandomize ,11);
			
			// #2 : static session 
			session_id += "static";
			//returnArray.add(session_id);
			System.out.println("creating new session id: " + session_id);
		} else {
			//returnArray.add(sessionid);
			session_id = sessionid;
			System.out.println("returning same session id: " + session_id);
		}
		return session_id;
	}	
	
	private void writeXML(DOMImplementation domImpl, Document xmldoc,
			String filename) throws FileNotFoundException {
			DOMImplementationLS ls = (DOMImplementationLS) domImpl;
	        LSSerializer lss = ls.createLSSerializer();
	        LSOutput lso = ls.createLSOutput();
	        lso.setByteStream(new FileOutputStream(new File(filename)));
	        
	        lss.write(xmldoc, lso);
	}
	
	public static WADLFile getAnalyzeWADLfile(ArrayList<String> analyze_WADLurls) throws ParserConfigurationException, IOException {
		for (int i = 0; i < analyze_WADLurls.size(); i++){
			try {
				
				System.out.println("ADDING ANALYZED URL: " + analyze_WADLurls.get(i));
		        // URLConnection
				URL yahoo = new URL(analyze_WADLurls.get(i));
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
				
		        return uppedWADL;
			} catch (MalformedURLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		return null;		
	}
	
	public static void getURIs(ArrayList<String> url_list, ArrayList<String> uriListReference, ArrayList<String> requestsListReference){
		ArrayList<String> apiLinks = new ArrayList<String>();
		
		for (int i = 0; i < url_list.size(); i++){
			if ( "".equals( url_list.get(i) ) ){
				// its null
			} else {
				String proper_url = new String( i + " GET " + url_list.get(i) + "" );
				apiLinks.add(proper_url);
			}
		}
		
		// Sample JSON links ( add json link here for debug )
		//apiLinks.add(new String("005 GET https://api.github.com/users/mralexgray/repos"));
		//apiLinks.add(new String("001 GET http://api.openweathermap.org/data/2.1/find/city?lat=55&lon=37&cnt=10"));
		
		Iterator<String> it = apiLinks.iterator();
		while(it.hasNext()){
		    String line = it.next();
		    requestsListReference.add(line);
			String[] tokens = line.split(" ");
			uriListReference.add(tokens[2]);
		}
	}
	
	private static String analyzeURIs(ArrayList<String> uris,
			HashMap<String, XSDFile> responses, RequestAnalyzer analyzer) {
		String resourceBase = "";
		if ( (uris != null) && (uris.size() > 0)){
			resourceBase = analyzer.batchRequestAnalysis(uris);
			
			for(String methodName : analyzer.getMethodNamesFromBatch(uris)) {
				responses.put(methodName, new XSDFile());
			}
		}
		return resourceBase;
	}
	
	private static void processRequests(ArrayList<String> requests,
			HashMap<String, XSDFile> responses, RequestAnalyzer analyzer,
			String resourceBase, XMLGenerator generator, WADLFile mergedWADL,
			HashSet<XSDFile> grammarSet, String mergedWADLFileName) throws MalformedURLException,
			IOException, ParserConfigurationException {
		
		HashMap<String, HashMap<String, Integer>> valueFrequencies = new HashMap<String, HashMap<String, Integer>>();
		
		for(String requestLine : requests) {
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
			final String FILENAME_WADL = "NEW_late_WADLresponse"+id+".wadl";
			final String FILENAME_XSD  = "NEW_response"+id+".xsd";
			System.out.println("-->the response name is " + FILENAME_XSD);
			System.out.println("-->the filename of the wadl file is " + FILENAME_WADL);
			System.out.println("-->the filename of the xml file is " + FILENAME_XML);
			
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
			WADLFile newWADL = new WADLFile(FILENAME_DIR_TWO +FILENAME_WADL, urlLine, mergedXSDFile.getResponseElement());
			grammarSet.add(xsdFile);		// TODO later change to Identifier + XSDElement
			newWADL.buildWADL(grammarSet, analyzer, resourceBase, methodName, 200);
		    generator.createWADL(newWADL);
		    
		    // Call diff&merge methods from sub-objects 
		    mergedWADL.compareToMerge(newWADL);
		    
			// calculating the value frequency of all XSDElements
		    HashMap<String, XSDIType> xsdtypes = xsdFile.getTypes();
		    System.out.println("============== START of printing all elements of xsdfile ================ ");
		    for (String typeString: xsdtypes.keySet()){
		    	getXSDElementsValueFrequencies(xsdtypes.get(typeString), valueFrequencies);
		    }
		    System.out.println("============== END of printing all elements of xsdfile ================ ");
		    
		    // Add the newly generated wadl's path to the list wadl_paths
		    //wadl_paths.add(LOCALHOST_FILES_PATH+FILENAME_WADL);
			
		}
		System.out.println("VALUE FREQUENCY TABLE: " + valueFrequencies);
	}
	
	/*private static String getWADLMergedFilename(String call_type) {
		String mergedFileName = "";
		if (call_type.equals("analyzeURLS")){
			mergedFileName = "mergedFileA.wadl";
		} else if (call_type.equals("compareURLS")){
			mergedFileName = "mergedFileB.wadl";
		}
		return mergedFileName;
	}*/
	
	// NAME ??????????? getXSDElementsValueFrequencies
	public static void getXSDElementsValueFrequencies(Object elem, HashMap<String, HashMap<String, Integer>> valueFrequencies){
		if (elem instanceof XSDComplexType){
			XSDComplexType xscomplex = (XSDComplexType) elem;
			//System.out.println("Complex Element [name] " + xscomplex.getName() + " [elements]: " + xscomplex.getElements());
			HashMap<String, XSDElement> elemList = xscomplex.getElements();
			for (String elemKey: elemList.keySet()){
				getXSDElementsValueFrequencies(elemList.get(elemKey), valueFrequencies);
			}
		} else if (elem instanceof XSDElement){
			XSDElement xsdelem = (XSDElement) elem;
			System.out.println("[name] " + xsdelem.getName() + " [type] " + xsdelem.getType() 
						+ " [value] " + xsdelem.getValue());
			
			// if XSDElement xsdelem's name exists in valueFrequencies
			if (valueFrequencies.containsKey(xsdelem.getName())){
				System.out.println("KEY " + xsdelem.getName() + " already exists !!!!!!!!!!" );
				HashMap<String, Integer> type_value_map = valueFrequencies.get(xsdelem.getName());
				System.out.println("BLAH BAH  ! '" + xsdelem.getType() + "' and '" + xsdelem.getType().toString() + "'" );
				// if XSDElement xsdelem's type also exists (checks that the element also has the same type)
				if (type_value_map.containsKey(xsdelem.getType().toString())){
					// increase frequency by 1
					Integer currentFrequency = type_value_map.get(xsdelem.getType().toString());
					type_value_map.put(xsdelem.getType().toString(), (currentFrequency+1));
				}
			} else {	// add element to the valueFrequencies list
				HashMap<String, Integer> type_value_map = new HashMap<String, Integer>();
				type_value_map.put(xsdelem.getType().toString(), 1);
				valueFrequencies.put(xsdelem.getName(), type_value_map);
			}
		}
	}
	
	/**
	 * @param args
	 */
	public static String getWadl(ArrayList<String> url_list, ArrayList<String> analyze_WADLurls, String call_type, String session_id, String mergedWADLFileName, String localhostFilePath) {
		try {
			ArrayList<String> requests = new ArrayList<String>();
			ArrayList<String> uris = new ArrayList<String>();
			HashMap<String, XSDFile> responses = new HashMap<String, XSDFile>();
			
			//ArrayList<String> wadl_paths = new ArrayList<String>();
			
			// gets 'uris' and 'requests', given 'uris'
			getURIs(url_list, uris, requests);
			
			RequestAnalyzer analyzer = new RequestAnalyzer();
			String resourceBase = analyzeURIs(uris, responses, analyzer);

			XMLGenerator generator = new XMLGenerator();
	        //System.out.println("[Interface retrieval]");
			
	        //String mergedWADLFileName = getWADLMergedFilename(call_type);
	        
			// (#1) If you want to use unique session ids:
			/*WADLFile mergedWADL;
			File f = new File(mergedWADLFileName);
			if(f.exists() && !f.isDirectory()) {
				System.out.println("A WADL FILE WITH filename " + mergedWADLFileName + " already exists. Reading it ! " );
				mergedWADL = new WADLFile(mergedWADLFileName);
				try {
					System.out.println("<<< reading wadl >>>");
					mergedWADL.readWADL();
					System.out.println("<<< finished reading wadl >>>");
				} catch (SAXException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				System.out.println("CREATING WADL FILE WITH filename " + mergedWADLFileName);
				mergedWADL = new WADLFile(mergedWADLFileName, null, null);
			}*/
			
			// (#2) No unique session ids ( no caching ) / overwrites the same files every time
	        // create empty merged WADL file
			WADLFile mergedWADL = new WADLFile(mergedWADLFileName, null, null);
			if(DEBUG) System.out.println("** Merged WADLFile: "+mergedWADL.getIdentifier()+" **");
			HashSet<XSDFile> grammarSet = new HashSet<XSDFile>();
			
			// Looping over test queries
			processRequests(requests, responses, analyzer, resourceBase,
					generator, mergedWADL, grammarSet, mergedWADLFileName);
			
			// merge uploaded wadl file(s) (if any / can only merge one uploaded wadl file right now)
			if ( (analyze_WADLurls != null) && (analyze_WADLurls.size() > 0)){
				WADLFile uppedWADL = getAnalyzeWADLfile(analyze_WADLurls);
		        // Call diff&merge methods from sub-objects
		        mergedWADL.compareToMerge(uppedWADL);
		        System.out.println("FILE MERGED !");
			}
			
			// write merged WADL file only once
			generator.createWADL(mergedWADL);
			
			// Add the merged wadl's path to the list wadl_paths
	        //wadl_paths.add(LOCALHOST_FILES_PATH+VENDOR+"Merged.wadl");
	        
	        //String MERGED_WADL_PATH = LOCALHOST_FILES_PATH+VENDOR+mergedWADLFileName;
	        if (call_type.equals("analyzeURLS")){
	        	wadl_one = mergedWADL;
	        	testA = mergedWADL;
	        	return localhostFilePath;
	        } else if (call_type.equals("compareURLS")){
	        	wadl_two = mergedWADL;
	        	testB = mergedWADL;
	        	return localhostFilePath;
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

// ===============================================================================================================================
// some api calls that return json
// https://api.github.com/users/penguinsource/repos
// https://api.github.com/users/mralexgray/repos
// https://api.github.com/users/fokaefs/repos
// https://api.github.com/users/mojombo
//
// 
// https://api.github.com/repos/penguinsource/pokemon_pac
//
// TYPES dateTime, anyURI, email example:  
// 		https://api.github.com/users/willcodeforfoo
//
//
//
//
//
//
//
// 
// 
// API calls with only one resource:
//
// https://api.github.com/users
// https://api.github.com/repositories
// https://graph.facebook.com/seinfeld
// https://graph.facebook.com/arresteddevelopment
//
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




// ********************************* junk code to review/use
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
//********************************* junk code to review/use
