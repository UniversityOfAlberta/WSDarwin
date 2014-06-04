package wsdarwin.wadlgenerator.testMains;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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

import javax.xml.parsers.ParserConfigurationException;

import wsdarwin.comparison.delta.Delta;
import wsdarwin.comparison.delta.MapDelta;
import wsdarwin.parsers.WADLParser;
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
	
	private static final String VENDOR = "github";
	private static final String PATH_PREFIX = "files/icsm2014/"+VENDOR;
	
	private static final String FILENAME_DIR = PATH_PREFIX+"/wadl/";
	private static final String XSD_DIR = PATH_PREFIX+"/xsd/";
	private static final String RESPONSE_DIR = PATH_PREFIX+"/responses/";

	public static void main(String[] args) {
		testGeneration();
		//testComparison();

	}
	
	private static void testMapping(WADLFile file1, WADLFile file2) {
		file1.mapElement(file2);
		for(MapDelta delta : file1.getMapDeltas()) {
			delta.printDelta(0);
		}
	}

	private static void testGeneration() {
		try {
			ArrayList<String> requests = new ArrayList<String>();
			ArrayList<String> uris = new ArrayList<String>();
			HashMap<String, XSDFile> responses = new HashMap<String, XSDFile>();
			
			BufferedReader testIn = new BufferedReader(new FileReader(new File(PATH_PREFIX+"/request.txt")));
			String line = testIn.readLine();
			
			while(line != null) {
				requests.add(line);
				String[] tokens = line.split(" ");
				uris.add(tokens[2]);
				line = testIn.readLine();
			}
			
			RequestAnalyzer analyzer = new RequestAnalyzer();
			String resourceBase = analyzer.batchRequestAnalysis(uris);
			for(String methodName : analyzer.getMethodNamesFromBatch(uris)) {
				responses.put(methodName, new XSDFile());
			}
			String xsdFilename = null;

			XMLGenerator generator = new XMLGenerator();
	        System.out.println("[Interface retrieval]");
			
			// create empty merged WADL file
			WADLFile mergedWADL = new WADLFile(FILENAME_DIR+VENDOR+"Merged.wadl", null, null);
			if(DEBUG) System.out.println("** Merged WADLFile: "+mergedWADL.getIdentifier()+" **");
			HashSet<XSDFile> grammarSet = new HashSet<XSDFile>();
			// Looping over test queries
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
				final String FILENAME_WADL = "WADLresponse"+id+".wadl";
				final String FILENAME_XSD  = "response"+id+".xsd";
				
		        System.out.println(" Request #"+id+"");
		        
		        // URLConnection
				URL yahoo = new URL(urlLine);
		        URLConnection yc = yahoo.openConnection();
		        BufferedReader in = new BufferedReader(
                    new InputStreamReader(yc.getInputStream()));
		        String inputLine;
		        BufferedWriter out = new BufferedWriter(new FileWriter(new File(RESPONSE_DIR+FILENAME_XML)));

		        while ((inputLine = in.readLine()) != null) {
		            out.write(inputLine);
		            out.newLine();
		        }
		        in.close();
		        out.close();
		        Response2XSD xsdBuilder = new Response2XSD();
	        
				xsdBuilder.buildXSDFromJSON(RESPONSE_DIR+FILENAME_XML, analyzer.getMethodID());
				XSDFile xsdFile = xsdBuilder.getXSDFile();
				XSDFile mergedXSDFile = responses.get(analyzer.getMethodID());
				
				mergedXSDFile.compareToMerge(xsdFile);
				
		        WADLFile newWADL = new WADLFile(FILENAME_DIR+FILENAME_WADL, urlLine, mergedXSDFile.getResponseElement());
		        
		        grammarSet.add(xsdFile);		// TODO later change to Identifier + XSDElement
		        newWADL.buildWADL(grammarSet, analyzer, resourceBase, methodName, 200);
		        generator.createWADL(newWADL);

		        testMapping(mergedWADL, newWADL);
		        // Call diff&merge methods from sub-objects 
		        mergedWADL.compareToMerge(newWADL);
		        
				requestLine = testIn.readLine();
				
				
			}
			// write merged WADL file only once
			generator.createWADL(mergedWADL);
			//mergedWADL.serializeFile();
			/*for(String methodID : responses.keySet()) {
				generator.createXSD(responses.get(methodID));
			}*/
			testIn.close();
			
			/*System.out.println("");
			System.out.println("[Comparison]");
			Delta delta = mergedWADL.compare(mergedWADL);
			delta.printDelta(0);*/			
			
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
	}

	private static void testComparison() {
		WADLParser parser1 = new WADLParser(new File(FILENAME_DIR+"WADLResponse001.wadl"));
		//WADLParser parser2 = new WADLParser(new File(FILENAME_DIR+"WADLResponse002.wadl"));
		Delta delta = parser1.getService().diff(parser1.getService());
		delta.printDelta(0);
	}

}
