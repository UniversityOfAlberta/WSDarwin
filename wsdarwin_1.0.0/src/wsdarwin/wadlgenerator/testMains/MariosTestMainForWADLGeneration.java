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
		Runtime rt = Runtime.getRuntime();
		try {
			Process pr = rt.exec("files/wadl2java/bin/wadl2java.bat -o files/tumblr/v2/client -p com.tumplr.api files/tumblr/v2/wadl/tumblr.wadl");
			System.out.println(pr.getInputStream().read());
			System.out.println(pr.getErrorStream().read());
			System.out.println(pr.waitFor());
			System.out.println(pr.exitValue());
			zipFolder();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
			service.generateWADL(urlRequests, new ArrayList<String>(), FILENAME_DIR+VENDOR+".wadl", RESPONSE_DIR, false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*try {
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

			XMLGenerator generator = new XMLGenerator();
			
			WADLFile mergedWADL = new WADLFile(FILENAME_DIR+VENDOR+".wadl", null, new XSDFile());
			HashSet<XSDFile> grammarSet = new HashSet<XSDFile>();
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
				final String FILENAME_JSON  = id+".json";
				
		        
		        // URLConnection
				URL yahoo = new URL(urlLine);
		        URLConnection yc = yahoo.openConnection();
		        BufferedReader in = new BufferedReader(
                    new InputStreamReader(yc.getInputStream()));
		        String inputLine;
		        File jsonFile = new File(RESPONSE_DIR+"\\"+FILENAME_JSON);
				BufferedWriter out = new BufferedWriter(new FileWriter(jsonFile));

		        while ((inputLine = in.readLine()) != null) {
		        	int listIndex = inputLine.indexOf("[");
					int mapIndex = inputLine.indexOf("{");
					if(listIndex<mapIndex) {
						inputLine = inputLine.substring(listIndex);
					}
					else {
						inputLine = inputLine.substring(mapIndex);
					}
		            out.write(inputLine);
		            out.newLine();
		        }
		        in.close();
		        out.close();
		        Response2XSD xsdBuilder = new Response2XSD();
		        String methodID = "";
		        if(analyzer.getMethodID().equals("")) {
		        	methodID = analyzer.getContainingResource();
		        }
		        else {
		        	methodID = analyzer.getMethodID();
		        }
				xsdBuilder.buildXSDFromJSON(jsonFile, methodID);
				XSDFile xsdFile = xsdBuilder.getXSDFile();
				String newWADLFilename = PATH_PREFIX+"/newWADL.wadl";
		        WADLFile newWADL = new WADLFile(newWADLFilename, urlLine, xsdFile);
		        
		        grammarSet.add(xsdFile);
		        newWADL.buildWADL(grammarSet, analyzer, resourceBase, methodName, 200);
		        mergedWADL.compareToMerge(newWADL);
		        
				requestLine = testIn.readLine();
				File wadlFile = new File(newWADLFilename);
				wadlFile.delete();
				jsonFile.delete();
				
			}
			generator.createWADL(mergedWADL, resourceBase);
			testIn.close();
			
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
	}

	private static void testComparison() {
		WADLParser parser1 = new WADLParser(new File("files/tumblr/v1/wadl/tumblr.wadl"));
		WADLParser parser2 = new WADLParser(new File("files/tumblr/v2/wadl/tumblr.wadl"));
		Delta delta = parser1.getService().diff(parser2.getService());
		DeltaUtil.findMoveDeltas(delta);
		delta.printDelta(0);
	}
	
	public static void zipFolder()
    {
		String sourceFolder = "files/tumblr/v2/client/com";
		ArrayList<String> filelist = new ArrayList<String>();
    	generateFileList(new File(sourceFolder), filelist, sourceFolder);
    	zipIt("files/tumblr/v2/client/proxy.zip", filelist, sourceFolder);
    }
 
    /**
     * Zip it
     * @param zipFile output ZIP file location
     */
    public static void zipIt(String zipFile, ArrayList<String> filelist, String sourceFolder){
 
     byte[] buffer = new byte[1024];
 
     try{
 
    	FileOutputStream fos = new FileOutputStream(zipFile);
    	ZipOutputStream zos = new ZipOutputStream(fos);
 
    	System.out.println("Output to Zip : " + zipFile);
 
    	for(String file : filelist){
 
    		System.out.println("File Added : " + file);
    		ZipEntry ze= new ZipEntry(file);
        	zos.putNextEntry(ze);
 
        	FileInputStream in = 
                       new FileInputStream(sourceFolder + File.separator + file);
 
        	int len;
        	while ((len = in.read(buffer)) > 0) {
        		zos.write(buffer, 0, len);
        	}
 
        	in.close();
    	}
 
    	zos.closeEntry();
    	//remember close it
    	zos.close();
 
    	System.out.println("Done");
    }catch(IOException ex){
       ex.printStackTrace();   
    }
   }
 
    /**
     * Traverse a directory and get all files,
     * and add the file into fileList  
     * @param node file or directory
     */
    public static void generateFileList(File node, ArrayList<String> filelist, String sourceFolder){
 
    	//add file only
	if(node.isFile()){
		filelist.add(generateZipEntry(node.getPath().toString(), sourceFolder));
	}
 
	if(node.isDirectory()){
		String[] subNote = node.list();
		for(String filename : subNote){
			generateFileList(new File(node, filename), filelist, sourceFolder);
		}
	}
 
    }
 
    /**
     * Format the file path for zip
     * @param file file path
     * @return Formatted file path
     */
    private static String generateZipEntry(String file, String sourceFolder){
    	return file.substring(sourceFolder.length()+1, file.length());
    }

}
