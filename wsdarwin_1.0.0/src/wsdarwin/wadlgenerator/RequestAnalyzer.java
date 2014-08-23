package wsdarwin.wadlgenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import wsdarwin.wadlgenerator.model.Param;
import wsdarwin.wadlgenerator.model.WADLFile;

public class RequestAnalyzer extends Uri {

	private Map<String, Object> queryMap;
	private String[] queryParameters;
	private String[] queryValues;
	private String resourceBase;
	
	public RequestAnalyzer(String uriString) {
		super(uriString);
		
		queryMap = new TreeMap<String, Object>();		
	    queryParameters = getQueryParameters();
	    queryValues = getQueryValues();

		
		// use get() / put() to work on the Map<>
	    
		// later on use the Map<> to diff and merger the differences between multiple Requests        
	}
	
	public RequestAnalyzer() {
		
	}
	
	public void resetUriString(String uriString) {
		boolean emptyBase = false;
		if(this.getPathComponentsAfterBase(resourceBase).length == 0) {
			emptyBase = true;
		}
		resolve(uriString);
		if(emptyBase) {
			resourceBase = getAuthority();
		}
		queryMap = new TreeMap<String, Object>();		
	    queryParameters = getQueryParameters();
	    queryValues = getQueryValues();
	}
	
	public String batchRequestAnalysis(ArrayList<String> requests) {
		resolve(requests.get(0));
		String base = getAuthority();
		HashMap<String,Integer> pathTokenFrequencies = new HashMap<String,Integer>();
		for(String request : requests) {
			resolve(request);
			String[] tokens = this.getPathComponents();
			for(String path : tokens) {
				if(pathTokenFrequencies.containsKey(path)) {
					pathTokenFrequencies.put(path, pathTokenFrequencies.get(path)+1);
				}
				else {
					pathTokenFrequencies.put(path,1);
				}
			}
		}
		for(String pathToken : pathTokenFrequencies.keySet()) {
			if(pathTokenFrequencies.get(pathToken) == requests.size()) {
				base += pathToken+"/";
			}
		}
		if(this.getPathComponentsAfterBase(base).length == 0) {
			base = getAuthority();
		}
		resourceBase = base;
		return base;
	}
	
	public HashSet<String> getMethodNamesFromBatch(ArrayList<String> requests) {
		HashSet<String> methodNames = new HashSet<String>();
		for(String request : requests) {
			resolve(request);
			methodNames.add(getMethodID());
		}
		return methodNames;
	}
	
	// implement pattern matching from Response2XSD.
	
	public HashMap<String, Param> analyzeParams() {		
		HashMap<String, Param> paramElements = new HashMap<String, Param>();
		Param param;
		//System.out.println("in-parameter analysis. length: " + queryParameters.length);
		for (int i = 0; i < queryParameters.length; i++) {
			String type;
			Object valueObject;
			
			//System.out.println("query values[i] :" + queryValues[i] );
			
			if(queryValues[i].equals("")) {
				valueObject = queryValues[i];
				type = "string";
			}
			else if(Pattern.matches("^[-+]?\\d*$", queryValues[i]) && queryValues[i].length()<=10) {
				valueObject = Integer.parseInt(queryValues[i]);
				type = "int";
			}
			else if(Pattern.matches("^[-+]?\\d*$", queryValues[i]) && queryValues[i].length()<=19) {
				valueObject = Long.parseLong(queryValues[i]);
				type = "long";
			}
			else if(Pattern.matches("^[-+]?[0-9]+[.]?[0-9]*([eE][-+]?[0-9]+)?$", queryValues[i])) {
				valueObject = Double.parseDouble(queryValues[i]);
				type = "double";
			}
			else if(Pattern.matches("true|false", queryValues[i])) {
				valueObject = Boolean.parseBoolean(queryValues[i]);
				type = "boolean";
			}
			/*else if(Pattern.matches("^(\\d{4})-(\\d{2})-(\\d{2})[T]?(\\d{2}):(\\d{2}):(\\d{2})[Z]?$", queryValues[i])) {
				valueObject = queryValues[i];
				type = "dateTime";
			}
			else if(Pattern.matches("^(\\d{4})-(\\d{2})-(\\d{2}) (\\d{2}):(\\d{2}):(\\d{2})$", queryValues[i])) {
				valueObject = queryValues[i];
				type = "date";
			}*/
			/*else if(queryValues[i].equals(queryValues[i].toUpperCase()) &&
					containsLetter(queryValues[i])) {
				 four conditions: isString && isUpperCase && containsLetter && distribution.exists 
				 *   1) due to pattern matching 2+3) checked here 4) will be checked after parsing in merging process 
				valueObject = queryValues[i];
				type = "enumeration";
			}*/
			else {
				valueObject = queryValues[i];
				type = "string";
			}
			
			System.out.println("RequestAnalyzer.java: ELEM TYPPE-> > " + type + " for: " + queryValues[i]);
			
			param = new Param(queryParameters[i], type, valueObject, WADLFile.PARAM_STYLE, WADLFile.PARAM_REQUIRED);
			paramElements.put(queryParameters[i], param);
		}
		
		return paramElements;
	}

	
	public boolean containsLetter(String str) {
		Pattern p = Pattern.compile("[a-zA-Z]+[a-zA-Z]+");
		Matcher m = p.matcher(str);
			
		return m.find();
	}

	
	public Map<String, Object> getQueryAsMap() {
	
		for (int i = 0; i < queryParameters.length; i++) {
			
			if(queryValues[i].equals("")) {
				queryMap.put(queryParameters[i], queryValues[i]);
			}
			else if(Pattern.matches("^[-+]?\\d*$", queryValues[i]) && queryValues[i].length()<=10) {
				queryMap.put(queryParameters[i], Integer.parseInt(queryValues[i]));
			}
			else if(Pattern.matches("^[-+]?\\d*$", queryValues[i]) && queryValues[i].length()<=19) {
				queryMap.put(queryParameters[i], Long.parseLong(queryValues[i]));
			}
			else if(Pattern.matches("^[-+]?[0-9]+[.]?[0-9]*([eE][-+]?[0-9]+)?$", queryValues[i])) {
				queryMap.put(queryParameters[i], Double.parseDouble(queryValues[i]));
			}
			else if(Pattern.matches("true|false", queryValues[i])) {
				queryMap.put(queryParameters[i], Boolean.parseBoolean(queryValues[i]));
			}
			else {
				queryMap.put(queryParameters[i], queryValues[i]);
			}
			
		}
		return queryMap;
	}
	
	public String[] getResourcePath() {
		return getPathComponentsAfterBase(resourceBase);
		/*String[] pathTokens = getPathComponentsAfterBase(resourceBase);
        String finalPath = "";
        for(int i=0; i<pathTokens.length-1; i++) {
        	finalPath += "/"+pathTokens[i];
        }
        return finalPath;*/
	}
	
	public String getMethodID() {
        String[] pathTokens = getPathComponentsAfterBase(resourceBase);
        String[] methodTokens = pathTokens[pathTokens.length-1].split("\\.");
        return methodTokens[0];
	}
	
	public String getRepresentationMediaType() {
        //String[] pathTokens = getPathComponentsAfterBase(resourceBase);
        //System.out.println("path tokens length: " + pathTokens.length );
        //String[] methodTokens = pathTokens[pathTokens.length-1].split("\\.");
        //System.out.println("method tokens length: " + methodTokens[0] + ", " + methodTokens[1]);
        return "json";
	}
	
	
	/**
	 * Returns all analyzed data back for debugging purposes. Each string consists of:<br /> 
	 * scheme + authority + path + query (+ fragment)
	 * @return String with all elements
	 */
	public String toString() {
        return "URI analysis: scheme="+getScheme()
        		+", authority="+getAuthority()+", path="+getPath()
        		+", query="+getQuery()+", fragment="+getFragment();
	}
		
}
