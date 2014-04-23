package wsdarwin.wadlgenerator.testMains;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.xml.parsers.ParserConfigurationException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
	//private static final String HOST_PATH = "localhost:8080/wsdarwin_1.0.0/";
	
	//private static final String PATH_PREFIX_TWO = HOST_PATH + "files/icsm2014/"+VENDOR;
	private static final String FILENAME_DIR_TWO = PATH_PREFIX_TWO+"/wadl/";
	private static final String RESPONSE_DIR_TWO = PATH_PREFIX_TWO+"/responses/";
	
	private static final String LOCALHOST_WADL_PATH = "http://localhost:8080/wsdarwin_1.0.0/files/icsm2014/twitter/wadl/";
	//private static final String LOCALHOST_WADL_PATH = PATH_PREFIX_TWO + "http://localhost:8080/wsdarwin_1.0.0/files/icsm2014/twitter/wadl/";
	
	@GET
	@Path("/analyze")
    public String singleURL(@QueryParam("urls") String jsonObj) throws Exception {
		//System.out.println("get aram is " + jsonObj );
		
		//Gson gson = new Gson();
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		ArrayList<String> urls = gson.fromJson(jsonObj, ArrayList.class);
		
		//ArrayList<String> wadlPaths = getWadl(urls);
		//WADLFile abc = getWadl(urls);
		String mergedPath = getWadl(urls);
		//String jsonRet = gson.toJson(abc);
		
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
		
		
		
		Gson gsonObj = new Gson();
		getWadl(jsonObj);*/
		
		return mergedPath;
		
		//return "The url you provided: " + jsonObj;
	}	
	
	/**
	 * @param args
	 */
	public static String getWadl(ArrayList<String> url_list) {
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
			
			// some api calls that return json
			// https://api.github.com/users/penguinsource/repos
			// https://api.github.com/users/mralexgray/repos
			// https://api.github.com/users/fokaefs/repos
			// https://graph.facebook.com/oprescu3
			// http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=HeyThere
			// http://api.openweathermap.org/data/2.1/find/city?lat=55&lon=37&cnt=10
			// http://www.google.com/calendar/feeds/developer-calendar@google.com/public/full?alt=json
			// http://en.wikipedia.org/w/api.php?format=json&action=query&titles=Main%20Page&prop=revisions&rvprop=content&format=json
			// http://www.biomedcentral.com/webapi/1.0/latest_articles.json
			// http://api.artsholland.com/rest/production.json?per_page=5&page=4
			// http://www.a3ultimate.com/API/Options/json/33916;3613214975,33916;3613214975,33916;3613214975,33916;3613214975
			
			System.out.println("==========================================");
			
			for (int i = 0; i < url_list.size(); i++){
				if ( "".equals( url_list.get(i) ) ){
					System.out.println("11 ITS NULL");
				} else {
					String proper_url = new String( i + " GET " + url_list.get(i) + "" );
					links.add(proper_url);
					System.out.println(" --> " + proper_url);
				}
			}
			
			Iterator<String> it = links.iterator();
			System.out.println("starting: ");
			
			while(it.hasNext()){
			    String line = it.next();
			    System.out.println("link: " + line);
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
			String resourceBase = analyzer.batchRequestAnalysis(uris);
			for(String methodName : analyzer.getMethodNamesFromBatch(uris)) {
				responses.put(methodName, new XSDFile());
			}
			String xsdFilename = null;

			XMLGenerator generator = new XMLGenerator();
	        System.out.println("[Interface retrieval]");
			
			// create empty merged WADL file
			WADLFile mergedWADL = new WADLFile(FILENAME_DIR_TWO+VENDOR+"Merged.wadl", null, null);
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
				
		        System.out.println(" Request #"+id+"");
		        
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
			
			// write merged WADL file only once
			generator.createWADL(mergedWADL);
			
			System.out.println("End of run !");
			//mergedWADL.serializeFile();
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
	        // Add the merged wadl's path to the list wadl_paths
	        wadl_paths.add(LOCALHOST_WADL_PATH+VENDOR+"Merged.wadl");
			//return wadl_paths;
			//return mergedWADL;
	        
	        // returning a DOM Document
			//Document xml_doc_merged = generator.createWADL(mergedWADL);
			//return xml_doc_merged;
	        
	        return MERGED_WADL_PATH;
			
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
