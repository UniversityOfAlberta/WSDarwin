package wsdarwin.wadlgenerator.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import wsdarwin.comparison.delta.*;
import wsdarwin.model.*;
import wsdarwin.util.DeltaUtil;
import wsdarwin.util.XMLGenerator;
import wsdarwin.wadlgenerator.RequestAnalyzer;
import wsdarwin.wadlgenerator.model.xsd.XSDFile;

public class WADLFile implements WADLElement {

	private String wadlFilename;
	private String requestURI;
	private ComplexType response;

	private Grammars grammarsElements;
	private HashMap<String, Resources> resourcesElements;

	// still hard-coded see Annoki ToDo-List
	/*public static final String 	RESOURCES_BASE = "http://maps.googleapis.com/maps/";
	public static final String 	RESOURCE_PATH = "api";
	public static final String 	METHOD_ID = "geocode";
	public static final String 	METHOD_NAME = "GET";
	*/
	public static final String 	PARAM_STYLE = "query";
	public static final boolean PARAM_REQUIRED = true;	
	/*public static final int 	RESPONSE_STATUS = 200;	
	public static final String	REPRESENTATION_MEDIATYPE = "application/xml";  // or JSON (with Directions)

	*/
//	TODO implement support for multiple namespaces

	public WADLFile(String filename,
			String requestURI, ComplexType response) {

		this.wadlFilename = filename;
		this.requestURI = requestURI;
		this.response = response;
		this.grammarsElements = new Grammars();			
		this.resourcesElements = new HashMap<String, Resources>();
	}

	public WADLFile(String filename) {

		this.wadlFilename = filename;
		this.grammarsElements = new Grammars();			
		this.resourcesElements = new HashMap<String, Resources>();
	}

	public String getIdentifier() {
		return wadlFilename;
	}

	public void setIdentifier(String filename) {
		this.wadlFilename = filename;
	}

	public String getRequestURI() {
		return requestURI;
	}

	public void setRequestURI(String requestURI) {
		this.requestURI = requestURI;
	}

	public Grammars getGrammarsElements() {
		return grammarsElements;
	}

	public void addGrammarsElement(XSDFile schema) {
		grammarsElements.addIncludedGrammar(schema);
	}

	public HashMap<String, Resources> getResourcesElements() {
		return resourcesElements;
	}

	public void addResourcesElement(String base, Resources resourcesElement) {
		this.resourcesElements.put(base, resourcesElement);
	}

	public String toString() {
		return wadlFilename;
		//		return "<application>/[WADLFile]: FILENAME="+wadlFilename+", #Grammars="+grammarsElements.getIncludedGrammars().size()
		//				+", #resourcesElements="+resourcesElements.size();
	}

	public void buildWADL(HashSet<XSDFile> xsdFilenames, RequestAnalyzer analyzer, String resourceBase, String methodName, int status) {
        grammarsElements.addAllIncludedGrammars(xsdFilenames);
        
        // Create all WADL objects
        Resources resources = new Resources(resourceBase);
        resourcesElements.put(resourceBase, resources);
        
        Resource resource = new Resource(analyzer.getResourcePath());
        resources.addResourceElement(analyzer.getResourcePath(), resource);
        
        Method method = new Method(methodName, analyzer.getMethodID());
        resource.addMethodElement(analyzer.getMethodID(), method);
        
        Request request = new Request();
        method.addRequestElement(request);
        
        Response response = new Response(status);
        method.addResponseElement(status, response);
        
        Representation representation = new Representation("application/"+analyzer.getRepresentationMediaType(), this.response);
        response.addRepresentationElement(XMLGenerator.TARGET_SCHEMA_NAMESPACE+this.response, representation);
        
        // Call URI Analyzer
        //System.out.println("============== ANALYZING PARAMS ! ===============");
        request.addAllParamElements(analyzer.analyzeParams());
        
        analyzer.getQueryAsMap();
       	}
	
	public void compareToMerge(WADLFile file) {
		Grammars grammarsAdded = new Grammars();
		HashSet<Resources> resourcesAdded = new HashSet<Resources>();
		HashMap<Resources, HashSet<Resource>> changedResources = new HashMap<Resources, HashSet<Resource>>();

		Grammars grammars = file.getGrammarsElements(); 
		for(XSDFile key : grammars.getIncludedGrammars()) {
			if(!this.getGrammarsElements().getIncludedGrammars().contains(key)) {
				grammarsAdded.addIncludedGrammar(key);
			}
		}	

		for(String base : file.getResourcesElements().keySet()) {
			if(!this.getResourcesElements().containsKey(base)) {
				resourcesAdded.add(file.getResourcesElements().get(base));
			} else {
				changedResources.put(this.getResourcesElements().get(base), this.getResourcesElements().get(base).compareToMerge(file.getResourcesElements().get(base)));
			}
		}

		mergeWADLFiles(grammarsAdded, resourcesAdded, changedResources);						
	}

	private void mergeWADLFiles(Grammars grammarsAdded,
			HashSet<Resources> resourcesAdded,
			HashMap<Resources, HashSet<Resource>> changedResources) {
		// add grammars
		this.grammarsElements.addAllIncludedGrammars(grammarsAdded.getIncludedGrammars());
		// add resources
		for(Resources resource : resourcesAdded) {
			this.resourcesElements.put(resource.getIdentifier(), resource);
		}
		// MERGE changedResources
		for(Resources resources : changedResources.keySet()) {
			resources.mergeResources(changedResources.get(resources));
		}
	}

	@Override
	public boolean equalsByName(Object o) {
		if(o instanceof WADLFile) {
			return wadlFilename.equals(((WADLFile)o).wadlFilename);
		}
		else {
			return false;
		}
	}

	@Override
	public boolean equalsAfterRename(Object o) {
		return true;
	}

	/*@Override
	public Delta compare(WSElement element) {
		WADLFile application = null;
		if(element instanceof WADLFile) {
			application = (WADLFile)element;
		} else {
			return null;
		}

		Delta elementDelta = null;
		ArrayList<Delta> deltas = new ArrayList<Delta>();

		// check for rename based on identifiers (id and name)
		// would be the comparison of the WADLFilenames

		 Grammars 
				Grammars grammarsElementAdded = null;
		Grammars grammarsElementDeleted = null;

		if(grammarsElements == null) {
			grammarsElementAdded = application.getGrammarsElements();
			AddDelta delta = new AddDelta(null, grammarsElementAdded);
			WADLFile.addChildrenElements(grammarsElementAdded, delta);
			deltas.add(delta);
		}
		else if(application.getGrammarsElements() == null) {
			grammarsElementDeleted = this.grammarsElements;
			DeleteDelta delta = new DeleteDelta(grammarsElementDeleted, null);
			WADLFile.deleteChildrenElements(grammarsElementDeleted, delta);
			deltas.add(delta);
		}

		// recursive call of compare() method
		if(grammarsElementAdded == null && grammarsElementDeleted == null) {
			deltas.add(this.grammarsElements.compare(application.getGrammarsElements()));
		}


		 Resources 

		HashMap<String, Resources> resourcesAdded = new HashMap<String, Resources>();
		HashMap<String, Resources> resourcesDeleted = new HashMap<String, Resources>();
		HashMap<String, Resources> resourcesIncluded = new HashMap<String , Resources>();

		for(String name : this.resourcesElements.keySet()) {
			resourcesIncluded.put(name, this.getResourcesElements().get(name));
		}
		for(String name : this.resourcesElements.keySet()) {
			if(!application.getResourcesElements().containsKey(name)) {
				resourcesDeleted.put(name, this.resourcesElements.get(name));
				resourcesIncluded.remove(name);
			}
		}
		for(String name : application.resourcesElements.keySet()) {
			if(!this.getResourcesElements().containsKey(name)) {
				resourcesAdded.put(name, application.resourcesElements.get(name));
			}
		}

		// mark additions and deletions
		ArrayList<String> resourcesNotAdded = new ArrayList<String>();
		ArrayList<String> resourcesNotDeleted = new ArrayList<String>();		
		for(String nameAdded : resourcesAdded.keySet()) {
			for(String nameDeleted : resourcesDeleted.keySet()) {

				if(resourcesAdded.get(nameAdded) instanceof Resources
						&& resourcesDeleted.get(nameDeleted) instanceof Resources) {
					// check for rename (based on children)
					if(resourcesAdded.get(nameAdded).getResourceElements()
							.equals(resourcesDeleted.get(nameDeleted).getResourceElements())) {
						deltas.add(resourcesAdded.get(nameAdded).compare(resourcesDeleted.get(nameDeleted)));
						resourcesNotAdded.add(nameAdded);
						resourcesNotDeleted.add(nameDeleted);
					}
				}
			}
		}

		// delete all of the previously marked elements
		for(String notAdded : resourcesNotAdded) {
			resourcesAdded.remove(notAdded);
		}
		for(String notDeleted : resourcesNotDeleted) {
			resourcesDeleted.remove(notDeleted);
		}

		// create Deltas for additions & deletions
		for(String name : resourcesAdded.keySet()) {
			AddDelta delta = new AddDelta(null, resourcesAdded.get(name));
			addChildrenElements(resourcesAdded.get(name), delta);
			deltas.add(delta);
		}
		for(String name : resourcesDeleted.keySet()) {
			DeleteDelta delta = new DeleteDelta(resourcesDeleted.get(name), null);
			deleteChildrenElements(resourcesDeleted.get(name), delta);
			deltas.add(delta);
		}

		// recursive call of compare() method
		for(String name : resourcesIncluded.keySet()) {
			deltas.add(this.resourcesElements.get(name).compare(application.getResourcesElements().get(name)));
		}

		if (elementDelta == null) {
			// create ElementDeltas
			if (DeltaUtil.containsOnlyMatchDeltas(deltas)) {
				elementDelta = new MatchDelta(this, application);
			} else {
				elementDelta = new ChangeDelta(this, application, "", null,
						null);
			}
		}
		elementDelta.addAllDeltas(deltas);
		elementDelta.adoptOrphanDeltas();
		return elementDelta;
	}*/



	/*public static void addChildrenElements(WADLElement element, AddDelta delta) {
		AddDelta addDelta = delta;
		if (!delta.getTarget().equals(element)) {
			addDelta = new AddDelta(null, element);
			delta.addDelta(addDelta);
			addDelta.setParent(delta);
		}
		if (element instanceof Resources) {
			for(Resource resource : ((Resources)element).getResourceElements().values()) {
				addChildrenElements(resource, addDelta);
			}
		}
		else if(element instanceof Resource) {
			for(Method method : ((Resource)element).getMethodElements().values()) {
				addChildrenElements(method, addDelta);
			}
		}
		else if(element instanceof Method) {
			addChildrenElements(((Method)element).getRequestElement(), addDelta);
			for(Response response : ((Method)element).getResponseElements().values()) {
				addChildrenElements(response, addDelta);
			}
		}
		else if(element instanceof Request) {
			for(Param param : ((Request)element).getParamElements().values()) {
				addChildrenElements(param, addDelta);
			}
		}
		else if(element instanceof Param) {
			
			//if (!addDelta.getTarget().equals(element)) {
				for(Option option : ((Param)element).getOptions()) {
							addChildrenElements(option, addDelta);
						}
				AddDelta addParamDelta = new AddDelta(null, element);
				delta.addDelta(addParamDelta);
				addParamDelta.setParent(delta);
			//}
		}
		else if(element instanceof Option) {
			//if (!addDelta.getTarget().equals(element)) {
				AddDelta addOptionDelta = new AddDelta(null, element);
				delta.addDelta(addOptionDelta);
				addOptionDelta.setParent(delta);
			//}
		}
		else if(element instanceof Response) {
			for(Representation representation : ((Response)element).getRepresentationElements().values()) {
				addChildrenElements(representation, addDelta);
			}
		}
		else if(element instanceof Representation) {
			if (!addDelta.getTarget().equals(element)) {
				AddDelta addRepresentationDelta = new AddDelta(null, element);
				delta.addDelta(addRepresentationDelta);
				addRepresentationDelta.setParent(delta);
			}
		}

	}*/

	/*public static void deleteChildrenElements(WADLElement element, DeleteDelta delta) {
		DeleteDelta deleteDelta = delta;
		if (!delta.getSource().equals(element)) {
			deleteDelta = new DeleteDelta(element, null);
			delta.addDelta(deleteDelta);
			deleteDelta.setParent(delta);
		}
		if (element instanceof Resources) {
			for(Resource resource : ((Resources)element).getResourceElements().values()) {
				deleteChildrenElements(resource, deleteDelta);
			}
		}
		else if(element instanceof Resource) {
			for(Method method : ((Resource)element).getMethodElements().values()) {
				deleteChildrenElements(method, deleteDelta);
			}
		}
		else if(element instanceof Method) {
			deleteChildrenElements(((Method)element).getRequestElement(), deleteDelta);
			for(Response response : ((Method)element).getResponseElements().values()) {
				deleteChildrenElements(response, deleteDelta);
			}
		}
		else if(element instanceof Request) {
			for(Param param : ((Request)element).getParamElements().values()) {
				deleteChildrenElements(param, deleteDelta);
			}
		}
		else if(element instanceof Param) {
			//if (!deleteDelta.getSource().equals(element)) {
				DeleteDelta deleteParamDelta = new DeleteDelta(element, null);
				delta.addDelta(deleteParamDelta);
				deleteParamDelta.setParent(delta);
			//}
		}
		else if(element instanceof Response) {
			for(Representation representation : ((Response)element).getRepresentationElements().values()) {
				deleteChildrenElements(representation, deleteDelta);
			}
		}
		else if(element instanceof Representation) {
			if (!deleteDelta.getSource().equals(element)) {
				DeleteDelta deleteRepresentationDelta = new DeleteDelta(
						element, null);
				delta.addDelta(deleteRepresentationDelta);
				deleteRepresentationDelta.setParent(delta);
			}
		}

	}*/

	//missing the complete response part because we need to merge it with the readXSD()
	public void readWADL() throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document xmlDoc = builder.parse(new File(this.wadlFilename));
		
		//System.out.println("XML DOC: " + this.wadlFilename);
		
		//add grammars
		NodeList grammars = xmlDoc.getElementsByTagName("grammars");
		this.grammarsElements = new Grammars();
		NodeList includes = grammars.item(0).getChildNodes();
		for(int i=0; i<includes.getLength(); i++) {
			if(includes.item(i).getNodeName().equals("include")) {
				//this.grammarsElements.addIncludedGrammar(includes.item(i).getAttributes().getNamedItem("href").getNodeValue());
				//System.out.println(this.grammarsElements.toString());
			}
		}
		
		// Processing the <grammars> of the WADL
		XSDFile xsdFile = new XSDFile();
		//NodeList schemas = grammars.item(0).getChildNodes();	// list of schema elements
		//System.out.println("THIS IS: " + schemas.item(0).getNodeName() + ", chillen: " + schemas.item(0).getChildNodes().getLength());
		xsdFile.readXSD(xmlDoc);
		this.addGrammarsElement(xsdFile);
		
		
		/*
		
		System.out.println("grammar elements: " + grammars.getLength());
		System.out.println("grammar name: " + grammars.item(0).getNodeName());
		NodeList schemaNodeList = grammars.item(0).getChildNodes();
		System.out.println("schema name: " + schemaNodeList.item(0).getNodeName());
		NodeList schemaChildren = schemaNodeList.item(0).getChildNodes();
		System.out.println("schema children count: " + schemaChildren.getLength());
		System.out.println("schema children name: " + schemaChildren.item(0).getAttributes().toString());
		
		*/
		
		//add resourcesElements
		NodeList resourcesElements = xmlDoc.getElementsByTagName("resources");
		
		for(int i=0; i<resourcesElements.getLength(); i++) {
			Resources resourcesElement = new Resources(resourcesElements.item(i).getAttributes().getNamedItem("base").getNodeValue());
			this.resourcesElements.put(resourcesElement.getIdentifier(), resourcesElement);
			NodeList resourcesChildren = resourcesElements.item(i).getChildNodes();
			//System.out.println("resources element to string:" + resourcesElement.toString() + ", identifier: " + resourcesElement.getIdentifier());
			//System.out.println("resources elements to string: " + this.resourcesElements.toString());
			for(int j=0; j<resourcesChildren.getLength(); j++) {
				if(resourcesChildren.item(j).getNodeName().equals("resource")) {
					//System.out.println("inside " + resourcesChildren.item(j).getNodeName());
					Resource resource = new Resource(resourcesChildren.item(j).getAttributes().getNamedItem("path").getNodeValue());
					resourcesElement.addResourceElement(resource.getIdentifier(), resource);
					NodeList resourceChildren = resourcesChildren.item(j).getChildNodes();
					for(int k=0; k<resourceChildren.getLength(); k++) {
						if(resourceChildren.item(k).getNodeName().equals("method")) {
							Method method = new Method(resourceChildren.item(k).getAttributes().getNamedItem("name").getNodeValue(), resourceChildren.item(k).getAttributes().getNamedItem("id").getNodeValue());
							resource.addMethodElement(method.getIdentifier(), method);
							NodeList methodChildren = resourceChildren.item(k).getChildNodes();
							for(int l=0; l<methodChildren.getLength(); l++) {
								if(methodChildren.item(l).getNodeName().equals("request")) {
									Request request = new Request();
									method.addRequestElement(request);
									NodeList requestChildren = methodChildren.item(l).getChildNodes();
									for(int m=0; m<requestChildren.getLength(); m++) {
										if(requestChildren.item(m).getNodeName().equals("param")) {
											String name = "";
											String type = "";
											String style = "";
											boolean required = false;
											if(requestChildren.item(m).getAttributes().getNamedItem("name") != null) {
												name = requestChildren.item(m).getAttributes().getNamedItem("name").getNodeValue();
											}
											if(requestChildren.item(m).getAttributes().getNamedItem("type") != null) {
												type = requestChildren.item(m).getAttributes().getNamedItem("type").getNodeValue();
											}
											if(requestChildren.item(m).getAttributes().getNamedItem("style") != null) {
												style = requestChildren.item(m).getAttributes().getNamedItem("style").getNodeValue();
											}
											if(requestChildren.item(m).getAttributes().getNamedItem("required") != null) {
												required = Boolean.parseBoolean(requestChildren.item(m).getAttributes().getNamedItem("required").getNodeValue());
											}
											Param param = new Param(name, type, style, required);
											request.addParamElement(param.getIdentifier(), param);
											//System.out.println("param identifier : " + param.getIdentifier());
											NodeList paramChildren = requestChildren.item(m).getChildNodes();
											for(int n=0; n<paramChildren.getLength(); n++) {
												if(paramChildren.item(n).getNodeName().equals("option")) {
													param.addOption(new Option(paramChildren.item(n).getAttributes().getNamedItem("value").getNodeValue()));
												}
											}
										}
									}
								} else if(methodChildren.item(l).getNodeName().equals("response")) {
									Response response = new Response(Integer.parseInt(methodChildren.item(l).getAttributes().getNamedItem("status").getNodeValue()));
									method.addResponseElement(response.getID(), response);
									//System.out.println("response ID : " + response.getID());
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public boolean mapElement(WADLElement element) {
		//System.out.println("MAP ELEMENT WADLFILE");
		if(element instanceof WADLFile){
			WADLFile file2 = (WADLFile)element;
			this.grammarsElements.mapElement(file2.getGrammarsElements());
			HashMap<String, Resources> mapped = new HashMap<String, Resources>();
			HashMap<String, Resources> added = new HashMap<String, Resources>();
			HashMap<String, Resources> deleted = new HashMap<String, Resources>();

			mapByID(file2, mapped, added, deleted);
			mapByStructure(file2, mapped, added, deleted);
		}
		return false;
	}
	
	//Grammars do not have id so Map By Structure only for Resources
	private void mapByStructure(WADLFile file2, HashMap<String, Resources> mapped, HashMap<String, Resources> added, HashMap<String, Resources> deleted) {
		HashMap<String, Resources> notAdded = new HashMap<String, Resources>();
		HashMap<String, Resources> notDeleted = new HashMap<String, Resources>();
		for(String base : added.keySet()){
			for(String base2 : deleted.keySet()){
				if(added.get(base).mapElement(deleted.get(base2))) {
					notAdded.put(base, added.get(base));
					notDeleted.put(base2, deleted.get(base2));
				}
			}
		}
		for(String base : notAdded.keySet()) {
			added.remove(base);
			mapped.put(base, notAdded.get(base));
		}
		for(String base : notDeleted.keySet()) {
			deleted.remove(base);
			mapped.put(base, notDeleted.get(base));
		}
		
		
	}

	private void mapByID(WADLFile file2, HashMap<String, Resources> mapped, HashMap<String, Resources> added, HashMap<String, Resources> deleted) {
		for(String base : file2.resourcesElements.keySet()){
			if(this.getResourcesElements().containsKey(base)){
				mapped.put(base,this.resourcesElements.get(base));
			}else {
				added.put(base,file2.resourcesElements.get(base));
			}
		}

		for(String base : this.resourcesElements.keySet()){
			if(!mapped.containsKey(base)){
				deleted.put(base,this.resourcesElements.get(base));
			}
		}
		
	}
	
	public void serializeFile() {
		//String serFilename = wadlFilename.toString();
		String serFilename = wadlFilename.replace(".wadl", ".ser");
		try {
			ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(serFilename)));
			out.writeObject(this);
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static WADLFile deserializeFile(String filename) {
		//String serFilename = wadlFilename.toString();
		String serFilename = filename.replace(".wadl", ".ser");
		WADLFile file = null;
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(new File(serFilename)));
			Object object = in.readObject();
			file = (WADLFile)object;
			in.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return file;
	}
}
