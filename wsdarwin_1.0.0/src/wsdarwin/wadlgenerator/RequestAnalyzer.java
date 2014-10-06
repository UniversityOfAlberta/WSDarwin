package wsdarwin.wadlgenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import wsdarwin.wadlgenerator.model.Param;
import wsdarwin.wadlgenerator.model.WADLFile;

public class RequestAnalyzer extends Uri {

	private Map<String, Object> queryMap;
	private List<String> pathComponents;
	private HashMap<String, HashSet<String>> variableBases;
	private HashMap<String, HashSet<String>> variableResources;
	private String[] queryParameters;
	private String[] queryValues;
	private String resourceBase;
	
	public RequestAnalyzer(String uriString) {
		super(uriString);
		resourceBase = getAuthority();
		pathComponents = Arrays.asList(getPathComponentsAfterBase(resourceBase));
		this.variableBases = new HashMap<String, HashSet<String>>();
		this.variableResources = new HashMap<String, HashSet<String>>();
		queryMap = new TreeMap<String, Object>();		
	    queryParameters = getQueryParameters();
	    queryValues = getQueryValues();

		
		// use get() / put() to work on the Map<>
	    
		// later on use the Map<> to diff and merger the differences between multiple Requests        
	}
	
	public RequestAnalyzer() {
		this.variableBases = new HashMap<String, HashSet<String>>();
		this.variableResources = new HashMap<String, HashSet<String>>();
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
		ArrayList<Uri> uris = new ArrayList<Uri>();
		for(String request : requests) {
			Uri uri = new Uri();
			uri.resolve(request);
			uris.add(uri);
		}
		resourceBase = uris.get(0).getAuthority();
		HashSet<String> bases = new HashSet<String>();
		for(Uri uri : uris) {
			bases.add(uri.getAuthority());
		}
		if(bases.size()>1) {
			variableBases.put("{baseID}", bases);
			resourceBase = "{baseID}";
		}
		getVariableResourceIDs(uris);
		/*String path = "";
		String[] tokens = uris.get(0).getPathComponents();
		boolean endOfBase = false;
		for(int i=0; i<tokens.length; i++) {
			path+="/"+tokens[i];
			for(Uri uri2 : uris) {
				if(!uri2.getPath().startsWith(path.substring(1))) {
					path = path.substring(path.lastIndexOf("/"));
					endOfBase = true;
					break;
				}
			}
			if (endOfBase) {
				break;
			}
		}
		resourceBase += path;*/
		
		/*HashMap<String,Integer> pathTokenFrequencies = new HashMap<String,Integer>();
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
		}*/
		return resourceBase;
	}
	
	private void getVariableResourceIDs(ArrayList<Uri> uris) {
		
		Uri maxUri = null;
		int maxLength = 0;
		for(Uri uri : uris) {
			if(uri.getPathComponents().length>=maxLength) {
				maxUri = uri;
				maxLength = uri.getPathComponents().length;
			}
		}
		ArrayList<Boolean[]> pathDiff = new ArrayList<Boolean[]>();
		for(Uri uri2 : uris) {
			int length = 0;
			if(maxUri.getPathComponents().length<=uri2.getPathComponents().length) {
				length = maxUri.getPathComponents().length;
			}
			else {
				length = uri2.getPathComponents().length;
			}
			Boolean[] diff = new Boolean[length];
			for(int i=0; i<length; i++) {
				diff[i] = maxUri.getPathComponents()[i].equals(uri2.getPathComponents()[i]);
			}
			pathDiff.add(diff);
		}
		
		HashSet<Integer> falseIndices = new HashSet<Integer>();
		for (Boolean[] diff : pathDiff) {
			for (int i = 0; i < diff.length; i++) {
				if (diff[i].equals(false)) {
					falseIndices.add(i);
				}
			}
		}
		int falseCounter = 0;
		for (Integer falseIndex : falseIndices) {
			if(falseIndex+1 == maxUri.getPathComponents().length) {
				break;
			}
			String variableResourceID = "{resource" + falseIndex+"}";
			int trueCounter = 0;
			boolean commonPrefix = false;
			int prefixCounter = 0;

			int size = pathDiff.size();
			for (int i = 0; i < falseIndex; i++) {
				if (!falseIndices.contains(i)) {
					for (Boolean[] diff : pathDiff) {
						if (diff[i].equals(true)) {
							trueCounter++;
						}
					}
					if (trueCounter == size) {
						prefixCounter++;
					}
					trueCounter = 0;
				}
				else {
					prefixCounter++;
				}
			}
			if(prefixCounter == falseIndex) {
				commonPrefix = true;
			}
			HashSet<String> commonPathComponents = new HashSet<String>();
			boolean partialCommonSuffix = false;
			if (commonPrefix) {
				int lastIndexCounter = 0;
				for (Uri uri : uris) {
					String[] path = uri.getPathComponents();
					if(path.length<falseIndex+2) {
						lastIndexCounter++;
					}
					else if (commonPathComponents.contains(path[falseIndex + 1])) {
						partialCommonSuffix = true;
						break;
					}
					else {
						commonPathComponents.add(path[falseIndex + 1]);
					}
				}
				if(lastIndexCounter == uris.size()) {
					partialCommonSuffix = true;
				}
			}
			if (partialCommonSuffix) {
				HashSet<String> paths = new HashSet<String>();
				for (Uri uri : uris) {
					String[] path = uri.getPathComponents();
					if(path.length>falseIndex) {
						paths.add(path[falseIndex]);
					}
				}
				variableResources.put(variableResourceID, paths);
			}
			falseCounter++;
		}
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
		return getPathComponents();
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
		String mediaType = "";
        List<String> pathTokens = Arrays.asList(getPathComponentsAfterBase(resourceBase));
		if (pathTokens.contains("json") || pathTokens.contains("JSON")
				|| pathTokens.contains("Json")) {
			mediaType = "json";
		} else if (pathTokens.contains("xml") || pathTokens.contains("XML")
				|| pathTokens.contains("Xml")) {
			mediaType = "xml";
		}
        return mediaType;
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

	public String getResourceID(String path) {
		for(String id : variableResources.keySet()) {
			if(variableResources.get(id).contains(path)) {
				return id;
			}
		}
		return path;
	}
		
}
