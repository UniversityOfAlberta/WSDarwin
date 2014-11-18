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
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import wsdarwin.comparison.delta.*;
import wsdarwin.model.*;
import wsdarwin.util.DeltaUtil;
import wsdarwin.util.LevenshteinDistance;
import wsdarwin.util.XMLGenerator;
import wsdarwin.wadlgenerator.RequestAnalyzer;
import wsdarwin.wadlgenerator.Response2XSD;
import wsdarwin.wadlgenerator.model.xsd.XSDComplexType;
import wsdarwin.wadlgenerator.model.xsd.XSDElement;
import wsdarwin.wadlgenerator.model.xsd.XSDFile;
import wsdarwin.wadlgenerator.model.xsd.XSDPrimitiveType;

public class WADLFile implements WADLElement {

	private String wadlFilename;
	private String requestURI;
	// private XSDFile response;

	private XSDFile schema;
	private HashMap<String, Resources> resourcesElements;
	private HashSet<MapDelta> mapDeltas;
	public ArrayList<ArrayList<String>> elemMappingArray;

	// still hard-coded see Annoki ToDo-List
	/*
	 * public static final String RESOURCES_BASE =
	 * "http://maps.googleapis.com/maps/"; public static final String
	 * RESOURCE_PATH = "api"; public static final String METHOD_ID = "geocode";
	 * public static final String METHOD_NAME = "GET";
	 */
	public static final String PARAM_STYLE = "query";
	public static final boolean PARAM_REQUIRED = true;

	/*
	 * public static final int RESPONSE_STATUS = 200; public static final String
	 * REPRESENTATION_MEDIATYPE = "application/xml"; // or JSON (with
	 * Directions)
	 */
	// TODO implement support for multiple namespaces

	public WADLFile(String filename, String requestURI, XSDFile schema) {

		this.wadlFilename = filename;
		this.requestURI = requestURI;
		this.schema = schema;
		this.resourcesElements = new HashMap<String, Resources>();
		this.mapDeltas = new HashSet<MapDelta>();
		this.elemMappingArray = new ArrayList<ArrayList<String>>();
	}

	public WADLFile(String filename) {

		this.wadlFilename = filename;
		this.resourcesElements = new HashMap<String, Resources>();
		this.mapDeltas = new HashSet<MapDelta>();
		this.schema = new XSDFile();
	}

	public ArrayList<ArrayList<String>> getElementMappings() {
		return this.elemMappingArray;
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

	public XSDFile getSchema() {
		return schema;
	}

	public void setSchema(XSDFile schema) {
		this.schema = schema;
	}

	public HashSet<MapDelta> getMapDeltas() {
		return mapDeltas;
	}

	public HashMap<String, Resources> getResourcesElements() {
		return resourcesElements;
	}

	public void addResourcesElement(String base, Resources resourcesElement) {
		this.resourcesElements.put(base, resourcesElement);
	}

	public String toString() {
		return wadlFilename;
		// return
		// "<application>/[WADLFile]: FILENAME="+wadlFilename+", #Grammars="+schema.getIncludedGrammars().size()
		// +", #resourcesElements="+resourcesElements.size();
	}

	public void buildWADL(HashSet<XSDFile> xsdFilenames,
			RequestAnalyzer analyzer, String resourceBase, String methodName,
			int status) {
		/*
		 * for(XSDFile xsd : xsdFilenames) { schema.compareToMerge(xsd); }
		 */

		// Create all WADL objects
		Resources resources = new Resources(resourceBase);
		resourcesElements.put(resourceBase, resources);

		Resource resource = new Resource(analyzer.getResourceID(analyzer
				.getResourcePath()[0]));
		resources
				.addResourceElement(
						analyzer.getResourceID(analyzer.getResourcePath()[0]),
						resource);
		Resource resourceElement = null;
		for (int i = 1; i < analyzer.getResourcePath().length; i++) {
			resourceElement = new Resource(analyzer.getResourceID(analyzer
					.getResourcePath()[i]));
			resource.addResourceElement(
					analyzer.getResourceID(analyzer.getResourcePath()[i]),
					resourceElement);
			resource = resourceElement;
		}

		if (resourceElement == null) {
			resourceElement = resource;
		}

		Method method = new Method(methodName, analyzer.getMethodID());
		resourceElement.addMethodElement(analyzer.getMethodID(), method);

		Request request = new Request();
		method.addRequestElement(request);

		Response response = new Response(status);
		method.addResponseElement(status, response);

		Representation representation = new Representation("application/"
				+ analyzer.getRepresentationMediaType(), this.schema);
		response.addRepresentationElement(XMLGenerator.TARGET_SCHEMA_NAMESPACE
				+ this.schema, representation);

		// Call URI Analyzer
		// System.out.println("============== ANALYZING PARAMS ! ===============");
		request.addAllParamElements(analyzer.analyzeParams());

		analyzer.getQueryAsMap();
	}

	public void compareToMerge(WADLFile file) {
		// HashSet<XSDFile> grammarsAdded = new HashSet<XSDFile>();
		HashSet<Resources> resourcesAdded = new HashSet<Resources>();
		HashMap<Resources, HashSet<Resource>> changedResources = new HashMap<Resources, HashSet<Resource>>();
		HashMap<XSDFile, HashSet<XSDElement>> changedGrammars = new HashMap<XSDFile, HashSet<XSDElement>>();

		if (!this.schema.getElements().isEmpty()) {
			this.schema.compareToMerge(file.getSchema());
		} else {
			for (String element : file.getSchema().getElements().keySet()) {
				this.schema.addElement(element, file.getSchema().getElements()
						.get(element));
			}
		}
		for (String base : file.getResourcesElements().keySet()) {
			if (!this.getResourcesElements().containsKey(base)) {
				resourcesAdded.add(file.getResourcesElements().get(base));
			} else {
				changedResources.put(
						this.getResourcesElements().get(base),
						this.getResourcesElements()
								.get(base)
								.compareToMerge(
										file.getResourcesElements().get(base)));
			}
		}

		mergeWADLFiles(resourcesAdded, changedResources);
	}

	private void mergeWADLFiles(HashSet<Resources> resourcesAdded,
			HashMap<Resources, HashSet<Resource>> changedResources) {
		// add grammars
		/*
		 * for(XSDFile xsd : grammarsAdded) {
		 * this.schema.put(xsd.getIdentifier(), xsd); }
		 */
		// add resources
		for (Resources resource : resourcesAdded) {
			this.resourcesElements.put(resource.getIdentifier(), resource);
		}
		// MERGE changedResources
		for (Resources resources : changedResources.keySet()) {
			resources.mergeResources(changedResources.get(resources));
		}
	}

	@Override
	public boolean equalsByName(Object o) {
		if (o instanceof WADLFile) {
			return wadlFilename.equals(((WADLFile) o).wadlFilename);
		} else {
			return false;
		}
	}

	@Override
	public boolean equalsAfterRename(Object o) {
		return true;
	}

	/*
	 * @Override public Delta compare(WSElement element) { WADLFile application
	 * = null; if(element instanceof WADLFile) { application =
	 * (WADLFile)element; } else { return null; }
	 * 
	 * Delta elementDelta = null; ArrayList<Delta> deltas = new
	 * ArrayList<Delta>();
	 * 
	 * // check for rename based on identifiers (id and name) // would be the
	 * comparison of the WADLFilenames
	 * 
	 * Grammars Grammars grammarsElementAdded = null; Grammars
	 * grammarsElementDeleted = null;
	 * 
	 * if(schema == null) { grammarsElementAdded =
	 * application.getGrammarsElements(); AddDelta delta = new AddDelta(null,
	 * grammarsElementAdded); WADLFile.addChildrenElements(grammarsElementAdded,
	 * delta); deltas.add(delta); } else if(application.getGrammarsElements() ==
	 * null) { grammarsElementDeleted = this.grammarsElements; DeleteDelta delta
	 * = new DeleteDelta(grammarsElementDeleted, null);
	 * WADLFile.deleteChildrenElements(grammarsElementDeleted, delta);
	 * deltas.add(delta); }
	 * 
	 * // recursive call of compare() method if(grammarsElementAdded == null &&
	 * grammarsElementDeleted == null) {
	 * deltas.add(this.grammarsElements.compare
	 * (application.getGrammarsElements())); }
	 * 
	 * 
	 * Resources
	 * 
	 * HashMap<String, Resources> resourcesAdded = new HashMap<String,
	 * Resources>(); HashMap<String, Resources> resourcesDeleted = new
	 * HashMap<String, Resources>(); HashMap<String, Resources>
	 * resourcesIncluded = new HashMap<String , Resources>();
	 * 
	 * for(String name : this.resourcesElements.keySet()) {
	 * resourcesIncluded.put(name, this.getResourcesElements().get(name)); }
	 * for(String name : this.resourcesElements.keySet()) {
	 * if(!application.getResourcesElements().containsKey(name)) {
	 * resourcesDeleted.put(name, this.resourcesElements.get(name));
	 * resourcesIncluded.remove(name); } } for(String name :
	 * application.resourcesElements.keySet()) {
	 * if(!this.getResourcesElements().containsKey(name)) {
	 * resourcesAdded.put(name, application.resourcesElements.get(name)); } }
	 * 
	 * // mark additions and deletions ArrayList<String> resourcesNotAdded = new
	 * ArrayList<String>(); ArrayList<String> resourcesNotDeleted = new
	 * ArrayList<String>(); for(String nameAdded : resourcesAdded.keySet()) {
	 * for(String nameDeleted : resourcesDeleted.keySet()) {
	 * 
	 * if(resourcesAdded.get(nameAdded) instanceof Resources &&
	 * resourcesDeleted.get(nameDeleted) instanceof Resources) { // check for
	 * rename (based on children)
	 * if(resourcesAdded.get(nameAdded).getResourceElements()
	 * .equals(resourcesDeleted.get(nameDeleted).getResourceElements())) {
	 * deltas
	 * .add(resourcesAdded.get(nameAdded).compare(resourcesDeleted.get(nameDeleted
	 * ))); resourcesNotAdded.add(nameAdded);
	 * resourcesNotDeleted.add(nameDeleted); } } } }
	 * 
	 * // delete all of the previously marked elements for(String notAdded :
	 * resourcesNotAdded) { resourcesAdded.remove(notAdded); } for(String
	 * notDeleted : resourcesNotDeleted) { resourcesDeleted.remove(notDeleted);
	 * }
	 * 
	 * // create Deltas for additions & deletions for(String name :
	 * resourcesAdded.keySet()) { AddDelta delta = new AddDelta(null,
	 * resourcesAdded.get(name)); addChildrenElements(resourcesAdded.get(name),
	 * delta); deltas.add(delta); } for(String name : resourcesDeleted.keySet())
	 * { DeleteDelta delta = new DeleteDelta(resourcesDeleted.get(name), null);
	 * deleteChildrenElements(resourcesDeleted.get(name), delta);
	 * deltas.add(delta); }
	 * 
	 * // recursive call of compare() method for(String name :
	 * resourcesIncluded.keySet()) {
	 * deltas.add(this.resourcesElements.get(name).
	 * compare(application.getResourcesElements().get(name))); }
	 * 
	 * if (elementDelta == null) { // create ElementDeltas if
	 * (DeltaUtil.containsOnlyMatchDeltas(deltas)) { elementDelta = new
	 * MatchDelta(this, application); } else { elementDelta = new
	 * ChangeDelta(this, application, "", null, null); } }
	 * elementDelta.addAllDeltas(deltas); elementDelta.adoptOrphanDeltas();
	 * return elementDelta; }
	 */

	/*
	 * public static void addChildrenElements(WADLElement element, AddDelta
	 * delta) { AddDelta addDelta = delta; if
	 * (!delta.getTarget().equals(element)) { addDelta = new AddDelta(null,
	 * element); delta.addDelta(addDelta); addDelta.setParent(delta); } if
	 * (element instanceof Resources) { for(Resource resource :
	 * ((Resources)element).getResourceElements().values()) {
	 * addChildrenElements(resource, addDelta); } } else if(element instanceof
	 * Resource) { for(Method method :
	 * ((Resource)element).getMethodElements().values()) {
	 * addChildrenElements(method, addDelta); } } else if(element instanceof
	 * Method) { addChildrenElements(((Method)element).getRequestElement(),
	 * addDelta); for(Response response :
	 * ((Method)element).getResponseElements().values()) {
	 * addChildrenElements(response, addDelta); } } else if(element instanceof
	 * Request) { for(Param param :
	 * ((Request)element).getParamElements().values()) {
	 * addChildrenElements(param, addDelta); } } else if(element instanceof
	 * Param) {
	 * 
	 * //if (!addDelta.getTarget().equals(element)) { for(Option option :
	 * ((Param)element).getOptions()) { addChildrenElements(option, addDelta); }
	 * AddDelta addParamDelta = new AddDelta(null, element);
	 * delta.addDelta(addParamDelta); addParamDelta.setParent(delta); //} } else
	 * if(element instanceof Option) { //if
	 * (!addDelta.getTarget().equals(element)) { AddDelta addOptionDelta = new
	 * AddDelta(null, element); delta.addDelta(addOptionDelta);
	 * addOptionDelta.setParent(delta); //} } else if(element instanceof
	 * Response) { for(Representation representation :
	 * ((Response)element).getRepresentationElements().values()) {
	 * addChildrenElements(representation, addDelta); } } else if(element
	 * instanceof Representation) { if (!addDelta.getTarget().equals(element)) {
	 * AddDelta addRepresentationDelta = new AddDelta(null, element);
	 * delta.addDelta(addRepresentationDelta);
	 * addRepresentationDelta.setParent(delta); } }
	 * 
	 * }
	 */

	/*
	 * public static void deleteChildrenElements(WADLElement element,
	 * DeleteDelta delta) { DeleteDelta deleteDelta = delta; if
	 * (!delta.getSource().equals(element)) { deleteDelta = new
	 * DeleteDelta(element, null); delta.addDelta(deleteDelta);
	 * deleteDelta.setParent(delta); } if (element instanceof Resources) {
	 * for(Resource resource :
	 * ((Resources)element).getResourceElements().values()) {
	 * deleteChildrenElements(resource, deleteDelta); } } else if(element
	 * instanceof Resource) { for(Method method :
	 * ((Resource)element).getMethodElements().values()) {
	 * deleteChildrenElements(method, deleteDelta); } } else if(element
	 * instanceof Method) {
	 * deleteChildrenElements(((Method)element).getRequestElement(),
	 * deleteDelta); for(Response response :
	 * ((Method)element).getResponseElements().values()) {
	 * deleteChildrenElements(response, deleteDelta); } } else if(element
	 * instanceof Request) { for(Param param :
	 * ((Request)element).getParamElements().values()) {
	 * deleteChildrenElements(param, deleteDelta); } } else if(element
	 * instanceof Param) { //if (!deleteDelta.getSource().equals(element)) {
	 * DeleteDelta deleteParamDelta = new DeleteDelta(element, null);
	 * delta.addDelta(deleteParamDelta); deleteParamDelta.setParent(delta); //}
	 * } else if(element instanceof Response) { for(Representation
	 * representation :
	 * ((Response)element).getRepresentationElements().values()) {
	 * deleteChildrenElements(representation, deleteDelta); } } else if(element
	 * instanceof Representation) { if
	 * (!deleteDelta.getSource().equals(element)) { DeleteDelta
	 * deleteRepresentationDelta = new DeleteDelta( element, null);
	 * delta.addDelta(deleteRepresentationDelta);
	 * deleteRepresentationDelta.setParent(delta); } }
	 * 
	 * }
	 */

	// missing the complete response part because we need to merge it with the
	// readXSD()
	public void readWADL() throws ParserConfigurationException, SAXException,
			IOException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document xmlDoc = builder.parse(new File(this.wadlFilename));

		// System.out.println("XML DOC: " + this.wadlFilename);

		// add grammars
		// NodeList grammars = xmlDoc.getElementsByTagName("grammars");
		/*
		 * this.schema = new HashMap<String, XSDFile>(); NodeList includes =
		 * grammars.item(0).getChildNodes(); for(int i=0;
		 * i<includes.getLength(); i++) {
		 * if(includes.item(i).getNodeName().equals("include")) {
		 * //this.grammarsElements
		 * .addIncludedGrammar(includes.item(i).getAttributes
		 * ().getNamedItem("href").getNodeValue());
		 * //System.out.println(this.grammarsElements.toString()); } }
		 */

		// Processing the <grammars> of the WADL
		XSDFile xsdFile = new XSDFile();
		// NodeList schemas = grammars.item(0).getChildNodes(); // list of
		// schema elements
		// System.out.println("THIS IS: " + schemas.item(0).getNodeName() +
		// ", chillen: " + schemas.item(0).getChildNodes().getLength());
		xsdFile.readXSD(xmlDoc);
		this.setSchema(xsdFile);

		/*
		 * 
		 * System.out.println("grammar elements: " + grammars.getLength());
		 * System.out.println("grammar name: " +
		 * grammars.item(0).getNodeName()); NodeList schemaNodeList =
		 * grammars.item(0).getChildNodes(); System.out.println("schema name: "
		 * + schemaNodeList.item(0).getNodeName()); NodeList schemaChildren =
		 * schemaNodeList.item(0).getChildNodes();
		 * System.out.println("schema children count: " +
		 * schemaChildren.getLength());
		 * System.out.println("schema children name: " +
		 * schemaChildren.item(0).getAttributes().toString());
		 */

		// add resourcesElements
		NodeList resourcesElements = xmlDoc.getElementsByTagName("resources");

		for (int i = 0; i < resourcesElements.getLength(); i++) {
			Resources resourcesElement = new Resources(resourcesElements
					.item(i).getAttributes().getNamedItem("base")
					.getNodeValue());
			this.resourcesElements.put(resourcesElement.getIdentifier(),
					resourcesElement);
			NodeList resourcesChildren = resourcesElements.item(i)
					.getChildNodes();
			// System.out.println("resources element to string:" +
			// resourcesElement.toString() + ", identifier: " +
			// resourcesElement.getIdentifier());
			// System.out.println("resources elements to string: " +
			// this.resourcesElements.toString());
			readResourceElements(resourcesElement, resourcesChildren);
		}
	}

	private void readResourceElements(WADLElement resourcesElement,
			NodeList resourcesChildren) {
		for (int j = 0; j < resourcesChildren.getLength(); j++) {
			if (resourcesChildren.item(j).getNodeName().equals("resource")) {
				// System.out.println("inside " +
				// resourcesChildren.item(j).getNodeName());
				Resource resource = new Resource(resourcesChildren.item(j)
						.getAttributes().getNamedItem("path").getNodeValue());
				if (resourcesElement instanceof Resources) {
					((Resources) resourcesElement).addResourceElement(
							resource.getIdentifier(), resource);
				} else if (resourcesElement instanceof Resource) {
					((Resource) resourcesElement).addResourceElement(
							resource.getIdentifier(), resource);
				}
				NodeList resourceChildren = resourcesChildren.item(j)
						.getChildNodes();
				readResourceElements(resource, resourceChildren);
				for (int k = 0; k < resourceChildren.getLength(); k++) {
					if (resourceChildren.item(k).getNodeName().equals("method")) {
						Method method = new Method(resourceChildren.item(k)
								.getAttributes().getNamedItem("name")
								.getNodeValue(), resourceChildren.item(k)
								.getAttributes().getNamedItem("id")
								.getNodeValue());
						resource.addMethodElement(method.getIdentifier(),
								method);
						NodeList methodChildren = resourceChildren.item(k)
								.getChildNodes();
						for (int l = 0; l < methodChildren.getLength(); l++) {
							if (methodChildren.item(l).getNodeName()
									.equals("request")) {
								Request request = new Request();
								method.addRequestElement(request);
								NodeList requestChildren = methodChildren.item(
										l).getChildNodes();
								for (int m = 0; m < requestChildren.getLength(); m++) {
									if (requestChildren.item(m).getNodeName()
											.equals("param")) {
										String name = "";
										String type = "";
										String style = "";
										boolean required = false;
										if (requestChildren.item(m)
												.getAttributes()
												.getNamedItem("name") != null) {
											name = requestChildren.item(m)
													.getAttributes()
													.getNamedItem("name")
													.getNodeValue();
										}
										if (requestChildren.item(m)
												.getAttributes()
												.getNamedItem("type") != null) {
											type = requestChildren.item(m)
													.getAttributes()
													.getNamedItem("type")
													.getNodeValue();
										}
										if (requestChildren.item(m)
												.getAttributes()
												.getNamedItem("style") != null) {
											style = requestChildren.item(m)
													.getAttributes()
													.getNamedItem("style")
													.getNodeValue();
										}
										if (requestChildren.item(m)
												.getAttributes()
												.getNamedItem("required") != null) {
											required = Boolean
													.parseBoolean(requestChildren
															.item(m)
															.getAttributes()
															.getNamedItem(
																	"required")
															.getNodeValue());
										}
										Param param = new Param(name, type,
												style, required);
										request.addParamElement(
												param.getIdentifier(), param);
										// System.out.println("param identifier : "
										// + param.getIdentifier());
										NodeList paramChildren = requestChildren
												.item(m).getChildNodes();
										for (int n = 0; n < paramChildren
												.getLength(); n++) {
											if (paramChildren.item(n)
													.getNodeName()
													.equals("option")) {
												param.addOption(new Option(
														paramChildren
																.item(n)
																.getAttributes()
																.getNamedItem(
																		"value")
																.getNodeValue()));
											}
										}
									}
								}
							} else if (methodChildren.item(l).getNodeName()
									.equals("response")) {
								Response response = new Response(
										Integer.parseInt(methodChildren.item(l)
												.getAttributes()
												.getNamedItem("status")
												.getNodeValue()));
								method.addResponseElement(response.getID(),
										response);
								// System.out.println("response ID : " +
								// response.getID());
							}
						}
					}
				}
			}
		}
	}

	@Override
	public boolean mapElement(WADLElement element) {
		// System.out.println("MAP ELEMENT WADLFILE");
		if (element instanceof WADLFile) {
			WADLFile file2 = (WADLFile) element;
			// this.grammarsElements.mapElement(file2.getGrammarsElements());
			HashMap<String, Resources> mapped = new HashMap<String, Resources>();
			HashMap<String, Resources> added = new HashMap<String, Resources>();
			HashMap<String, Resources> deleted = new HashMap<String, Resources>();
			mapByValue(file2);
			mapByID(file2, mapped, added, deleted);
			mapByStructure(file2, mapped, added, deleted);
		}
		return false;
	}

	public void mapByValue(WADLFile file2) {
		//MapDelta deltaParam = mapByValueParam(file2);
		MapDelta deltaElement = mapByValueResponse(file2);
		// removeDeuplicateMapDeltas();
		// System.out.println("Map by Value response " + mapDeltas.toString());
		//iteratemap();
	}

	public MapDelta mapByValueParam(WADLFile file2) {
		MapDelta delta = null;
		for (Resources resources : this.getResourcesElements().values()) {
			ArrayList<Delta> resourceDeltas = new ArrayList<Delta>();
			for (Resource resource : resources.getResourceElements().values()) {
				ArrayList<Delta> methodDeltas = new ArrayList<Delta>();
				for (Method method : resource.getMethodElements().values()) {
					ArrayList<Delta> paramDeltas = new ArrayList<Delta>();
					for (Param param : method.getRequestElement()
							.getParamElements().values()) {

						for (Resources resources2 : file2
								.getResourcesElements().values()) {
							for (Resource resource2 : resources2
									.getResourceElements().values()) {
								for (Method method2 : resource2
										.getMethodElements().values()) {
									int numOfParams1 = method
											.getRequestElement()
											.getParamElements().size();
									int numOfParams2 = method2
											.getRequestElement()
											.getParamElements().size();
									double scoreMax = 5 * numOfParams1
											* numOfParams2;

									double paramScore = 0;
									for (Param param2 : method2
											.getRequestElement()
											.getParamElements().values()) {
										param.map(param2);
										// paramScore = paramScore +
										// param.getScore();

										// compare similar double values
										/*
										 * if(param.getValue() instanceof Double
										 * && param2.getValue() instanceof
										 * Double){ Double value1 =
										 * Double.parseDouble((String)
										 * param.getValue()); Double value2 =
										 * Double.parseDouble((String)
										 * param.getValue());
										 * if(Math.abs(value1-value2)<0.001){
										 * MapDelta paramDelta = new
										 * MapDelta(param, param2);
										 * mapDeltas.add(paramDelta);
										 * paramDeltas.add(paramDelta); } }
										 * 
										 * //compare by distance example:
										 * Edmonton+AB and Aedmonton%20%AB
										 * should be the same
										 * if(param.getValue() instanceof String
										 * && param2.getValue() instanceof
										 * String){ double distance =
										 * LevenshteinDistance
										 * .getDistance((String
										 * )param.getValue(),
										 * (String)param2.getValue());
										 * System.out.println("DISTANCE: " +
										 * distance); }
										 * 
										 * if(param.getValue().equals(param2.
										 * getValue())){
										 * //System.out.println("equal: " +
										 * param.getValue() + " AND " +
										 * param2.getValue()); MapDelta
										 * paramDelta = new MapDelta(param,
										 * param2);
										 * 
										 * 
										 * mapDeltas.add(paramDelta);
										 * paramDeltas.add(paramDelta); } else {
										 * System.out.println("not equal: " +
										 * param.getValue() + " AND " +
										 * param2.getValue());
										 * 
										 * }
										 */
									}

									// compare score
									double methodScore = ((scoreMax - paramScore) / scoreMax) * 100;
									method.setScore(methodScore);
									// System.out.println("Mehtod Score = " +
									// method.getScore() + "  " +
									// method.getIdentifier() + "   " +
									// method2.getIdentifier());

									if (paramDeltas.size() != 0) {
										MapDelta methodDelta = new MapDelta(
												method, method2);
										methodDelta.addAllDeltas(paramDeltas);
										methodDeltas.add(methodDelta);
										mapDeltas.add(methodDelta);
									}
								}
								if (methodDeltas.size() != 0) {
									MapDelta resourceDelta = new MapDelta(
											resource, resource2);
									resourceDelta.addAllDeltas(methodDeltas);
									resourceDeltas.add(resourceDelta);
									mapDeltas.add(resourceDelta);
								}

							}
							if (resourceDeltas.size() != 0) {
								delta = new MapDelta(resources, resources2);
								delta.addAllDeltas(resourceDeltas);
								mapDeltas.add(delta);
							}
						}
						// System.out.println("NEW HASHMAP VALUES");
						HashMap<Param, Double> distance = param
								.getParamDistanceMap();
						double totalScore = 0;
						for (Param p : distance.keySet()) {
							// System.out.println("Param: " +
							// param.getIdentifier() + "->" + p.getIdentifier()
							// + " : " + distance.get(p));
							totalScore += distance.get(p);
						}
						// System.out.println("Total score = " + totalScore);

					}
				}
			}
		}

		if (delta != null) {
			// System.out.println("Map by Value " + delta.printDelta(0));
		}
		return delta;
	}

	private HashMap<XSDElement, Object> getResponseElements() {

		HashMap<XSDElement, Object> map = new HashMap<XSDElement, Object>();
		for (Resources resources : this.getResourcesElements().values()) {
			for (Resource resource : resources.getResourceElements().values()) {
				getResponseElementsForResource(map, resource);
			}
		}
		return map;
	}

	private void getResponseElementsForResource(
			HashMap<XSDElement, Object> map, Resource resource) {
		for (Resource r : resource.getResourceElements().values()) {
			getResponseElementsForResource(map, r);
		}
		for (Method method : resource.getMethodElements().values()) {
			for (Response response : method.getResponseElements().values()) {
				for (Representation represent : response
						.getRepresentationElements().values()) {
					getXSDElements((XSDComplexType) represent.getElement()
							.getType(), map);
				}
			}
		}
	}

	public MapDelta mapByValueResponse(WADLFile file2) {
		// System.out.println("MAPPING BY RESPONSEEEEEE");
		HashMap<XSDElement, Object> map = this.getResponseElements();
		HashMap<XSDElement, Object> map2 = file2.getResponseElements();
		for (XSDElement xsd : map.keySet()) {
			if (map.get(xsd) instanceof List) {
				List<Object> valueList = (List<Object>) map.get(xsd);
				for (Object value : valueList) {
					getXSDElementByValue(map2, value, xsd);
				}
			} else {
				getXSDElementByValue(map2, map.get(xsd), xsd);
			}
		}
		MapDelta delta = null;
		/*
		 * for(Resources resources : this.getResourcesElements().values()){
		 * ArrayList<Delta> resourceDeltas = new ArrayList<Delta>();
		 * for(Resource resource : resources.getResourceElements().values()){
		 * ArrayList<Delta> methodDeltas = new ArrayList<Delta>(); for(Method
		 * method: resource.getMethodElements().values()){ ArrayList<Delta>
		 * elementDeltas = new ArrayList<Delta>(); for(Response response :
		 * method.getResponseElements().values()){ for(Representation represent
		 * : response.getRepresentationElements().values()){ HashMap<XSDElement,
		 * Object> map = new HashMap<XSDElement, Object>();
		 * getXSDElements((XSDComplexType)represent.getElement().getType(),
		 * map); for(XSDElement xsd : map.keySet()){ //for map elements
		 * for(Resources resources2 : file2.getResourcesElements().values()){
		 * for(Resource resource2 : resources2.getResourceElements().values()){
		 * for(Method method2: resource2.getMethodElements().values()){
		 * for(Response response2 : method2.getResponseElements().values()){
		 * for(Representation represent2 :
		 * response2.getRepresentationElements().values()){ XSDElement element2
		 * = null; HashMap<XSDElement, Object> map2 = new HashMap<XSDElement,
		 * Object>();
		 * getXSDElements((XSDComplexType)represent2.getElement().getType
		 * (),map2); if (map.get(xsd) instanceof List) { List<Object> valueList
		 * = (List<Object>)map.get(xsd); for (Object value : valueList) {
		 * element2 = getXSDElementByValue(map2, value, xsd); } } else {
		 * element2 = getXSDElementByValue(map2, map.get(xsd), xsd); }
		 * if(element2 != null) { delta = new MapDelta(xsd, element2);
		 * elementDeltas.add(delta); mapDeltas.add(delta); } }
		 * if(elementDeltas.size()!=0){ MapDelta methodDelta = new
		 * MapDelta(method, method2); methodDelta.addAllDeltas(elementDeltas);
		 * methodDeltas.add(methodDelta); mapDeltas.add(methodDelta); } }
		 * if(methodDeltas.size()!=0){ MapDelta resourceDelta = new
		 * MapDelta(resource, resource2);
		 * resourceDelta.addAllDeltas(methodDeltas);
		 * resourceDeltas.add(resourceDelta); mapDeltas.add(resourceDelta); } }
		 * if(resourceDeltas.size()!=0){ delta = new MapDelta(resources,
		 * resources2); delta.addAllDeltas(resourceDeltas);
		 * mapDeltas.add(delta); }
		 * 
		 * } } HashMap<XSDElement, Double> distance =
		 * xsd.getElementDistanceMap(); double totalScore = 0.0; for(XSDElement
		 * xsdelement : distance.keySet()){
		 * totalScore+=distance.get(xsdelement); }
		 * //System.out.println("TOTAL SCORE FOR RESPONSEEEEEEEEEEEEE " +
		 * totalScore); } } }
		 * 
		 * } } } if (delta != null) {
		 * System.out.println("Map by Value response " + mapDeltas.toString());
		 * }
		 */

		return delta;

	}

	private void iteratemap() {
		boolean i = true;
		for (MapDelta mapDelta : mapDeltas) {
			// if(i){
			// System.out.println(mapDelta.toString());
			i = false;
			// }
		}
	}

	private void getXSDElements(XSDComplexType complexType,
			HashMap<XSDElement, Object> map) {
		// System.out.println();
		for (XSDElement element : complexType.getElements().values()) {
			if (element.getType() instanceof XSDPrimitiveType) {
				map.put(element, element.getValue());
			} else {
				getXSDElements((XSDComplexType) element.getType(), map);
			}
		}
	}

	private XSDElement getXSDElementByValue(HashMap<XSDElement, Object> map,
			Object value, XSDElement xsd) {
		for (XSDElement element : map.keySet()) {
			// double score=0;
			if (element.getType() instanceof XSDPrimitiveType) {
				if (element.getValue() instanceof Integer
						&& value instanceof Integer) {
					int value1 = (int) value;
					int value2 = (int) element.getValue();
					boolean inclusion = false;
					double similarity;
					double diffInclusion;
					double diff = (double) (Math.max(value1, value2) - Math
							.min(value1, value2)) / Math.max(value1, value2);
					similarity = (1 - diff) * 100;
					// element.addElementDistanceMap(element, diff);
					String values1 = Integer.toString(value1);
					String values2 = Integer.toString(value2);
					if (values1.contains(values2) || values2.contains(values1)) {
						inclusion = true;
					}
					diffInclusion = similarity;
					if (inclusion) {
						diffInclusion = (similarity + 50) / 1.5;
					}
					xsd.addElementDistanceMap(element, similarity);
					// System.out.println(element.getName() + "\t"
					// +element.getValue() + "\t" + xsd.getName() + "\t" + value
					// + "\t" + similarity + "\t" + inclusion);
				} else if (element.getValue() instanceof Double
						&& value instanceof Double) {
					double value1 = (Double) value;
					double value2 = (Double) element.getValue();
					double diff = (Math.max(value1, value2) - Math.min(value1,
							value2)) / Math.max(value1, value2);
					double similarity = (1 - diff) * 100;
					String values1 = Double.toString(value1);
					String values2 = Double.toString(value2);
					boolean inclusion = false;
					if (values1.contains(values2) || values2.contains(values1)) {
						inclusion = true;
					}
					xsd.addElementDistanceMap(element, similarity);
					// System.out.println(element.getName() + "\t"
					// +element.getValue() + "\t" + xsd.getName() + "\t" + value
					// + "\t" + similarity +"\t"+inclusion);
				}

				else if (element.getValue() instanceof Set
						&& value instanceof Double) {
					Set setelements = (Set) element.getValue();
					for (Object setelement : setelements) {
						if (setelement instanceof Double) {
							double value1 = (double) value;
							double value2 = ((Double) setelement).doubleValue();
							double diff = (Math.max(value1, value2) - Math.min(
									value1, value2)) / Math.max(value1, value2);
							double similarity = (1 - diff) * 100;
							boolean inclusion = false;
							String values1 = Double.toString(value1);
							String values2 = Double.toString(value2);
							if (values1.contains(values2)
									|| values2.contains(values1)) {
								inclusion = true;
							}
							xsd.addElementDistanceMap(element, similarity);
							// System.out.println(element.getName() + "\t"
							// +element.getValue() + "\t" + xsd.getName() + "\t"
							// + value + "\t" + similarity + "\t" + inclusion);
						} else if (setelement instanceof Integer) {
							double value1 = (double) value;
							double value2 = ((Integer) setelement)
									.doubleValue();
							double diff = (Math.max(value1, value2) - Math.min(
									value1, value2)) / Math.max(value1, value2);
							double similarity = (1 - diff) * 100;
							boolean inclusion = false;
							String values1 = Double.toString(value1);
							String values2 = Double.toString(value2);
							if (values1.contains(values2)
									|| values2.contains(values1)) {
								inclusion = true;
							}
							xsd.addElementDistanceMap(element, similarity);
							// System.out.println(element.getName() + "\t"
							// +element.getValue() + "\t" + xsd.getName() + "\t"
							// + value + "\t" + similarity + "\t" +inclusion);
						}

					}

				}
				// set string and integer
				else if (element.getValue() instanceof Set
						&& value instanceof Integer) {
					Set setelements = (Set) element.getValue();
					for (Object setelement : setelements) {
						if (setelement instanceof String) {
							int value1 = (int) value;
							long value1long = new Long(value1);
							String value2 = (String) setelement;// 2011-08-04
							value2 = value2.replaceAll("\\D+", "");
							double similarity = 0.0;
							boolean inclusion = false;
							if (value2.length() > 0) {
								long value2long = Long.parseLong(value2);
								// compute distance and similarity
								double diff = (Math.max(value1long, value2long) - Math
										.min(value1long, value2long))
										/ Math.max(value1long, value2long);
								similarity = (1 - diff) * 100;
								// inclusion
								String value1str = Long.toString(value1long);
								if (value1str.contains(value2)
										|| value2.contains(value1str)) {
									inclusion = true;
								}
							}
							xsd.addElementDistanceMap(element, similarity);
							// System.out.println(element.getName() + "\t"
							// +element.getValue() + "\t" + xsd.getName() + "\t"
							// + value + "\t" + similarity + "\t" + inclusion);
						}
					}

				} else if (element.getValue() instanceof Integer
						&& value instanceof Set) {
					Set setelements = (Set) value;
					for (Object setelement : setelements) {
						if (setelement instanceof String) {
							int value1 = (int) element.getValue();// 20110804
							long value1long = new Long(value1);
							String value2 = (String) setelement;// 2011-08-04
							value2 = value2.replaceAll("\\D+", "");
							double similarity = 0.0;
							boolean inclusion = false;
							if (value2.length() > 0) {
								long value2long = Long.parseLong(value2);
								// compute distance and similarity
								double diff = (double) (Math.max(value1long,
										value2long) - Math.min(value1long,
										value2long))
										/ (double) Math.max(value1long,
												value2long);
								similarity = (1 - diff) * 100;
								// inclusion
								String value1str = Long.toString(value1long);
								if (value1str.contains(value2)
										|| value2.contains(value1str)) {
									inclusion = true;
								}
							}
							xsd.addElementDistanceMap(element, similarity);
							// System.out.println(element.getName() + "\t"
							// +element.getValue() + "\t" + xsd.getName() + "\t"
							// + value + "\t" + similarity + "\t" + inclusion);

						}
					}

				} else if (element.getValue() instanceof Set
						&& value instanceof String) {
					Set setelements = (Set) element.getValue();
					for (Object setelement : setelements) {
						if (setelement instanceof String) {
							String value1 = (String) value;
							String value2 = (String) setelement;
							boolean inclusion = false;
							double dist = LevenshteinDistance.getDistance(
									value1, value2);
							xsd.addElementDistanceMap(element, dist);
							if (value1.contains(value2)
									|| value2.contains(value1)) {
								inclusion = true;
							}
							// element.addElementDistanceMap(xsd, similarity);
							// System.out.println(element.getName() + "\t"
							// +element.getValue() + "\t" + xsd.getName() + "\t"
							// + value + "\t" + dist + "\t" + inclusion);
						}
					}
				} else if (element.getValue() instanceof String
						&& value instanceof String) {
					String value1 = (String) value;
					String value2 = (String) element.getValue();
					boolean inclusion = false;
					double dist = LevenshteinDistance.getDistance(value1,
							value2);
					xsd.addElementDistanceMap(element, dist);
					if (value1.contains(value2) || value2.contains(value1)) {
						inclusion = true;
					}
					// element.addElementDistanceMap(xsd, similarity)
					// System.out.println(element.getName() + "\t"
					// +element.getValue() + "\t" + xsd.getName() + "\t" + value
					// + "\t" + dist + "\t" + inclusion);
				} else if (element.getValue() instanceof String
						&& value instanceof Integer) {
					String value1;
					double value2int = (int) value * 1.0;
					long value1int;
					String str = (String) element.getValue();
					value1 = str.replaceAll("\\D+", "");
					double similarity = 0.0;
					boolean inclusion = false;
					if (value1.length() > 0) {
						value1int = Long.parseLong(value1);
						// compute distance and similarity
						double diff = (Math.max(value1int, value2int) - Math
								.min(value1int, value2int))
								/ Math.max(value1int, value2int);
						similarity = (1 - diff) * 100;
						// inclusion
						String value2 = Integer.toString((int) value);
						if (value1.contains(value2) || value2.contains(value1)) {
							inclusion = true;
						}
					}
					xsd.addElementDistanceMap(element, similarity);
					// System.out.println(element.getName() + "\t"
					// +element.getValue() + "\t" + xsd.getName() + "\t" + value
					// + "\t" + similarity + "\t" + inclusion);
				} else if (element.getValue() instanceof Integer
						&& value instanceof String) {
					String value2 = (String) value;
					double value1int = (int) element.getValue() * 1.0;
					long value2int;
					String str = (String) value2;
					value2 = str.replaceAll("\\D+", "");
					double similarity = 0.0;
					boolean inclusion = false;
					if (value2.length() > 0) {

						if (Response2XSD.isLong(value2)) {
							value2int = Long.parseLong(value2);
							// compute distance and similarity
							double diff = (Math.max(value1int, value2int) - Math
									.min(value1int, value2int))
									/ Math.max(value1int, value2int);
							similarity = (1 - diff) * 100;
							// inclusion
							String value1 = Double.toString(value1int);
							if (value1.contains(value2)
									|| value2.contains(value1)) {
								inclusion = true;
							}
						}
					}
					xsd.addElementDistanceMap(element, similarity);
					// System.out.println(element.getName() + "\t"
					// +element.getValue() + "\t" + xsd.getName() + "\t" + value
					// + "\t" + similarity + "\t" + inclusion);

				}

				/*
				 * if(element.getValue().equals(value)){ return element; }
				 */
			}
		}
		double max = 0;
		XSDElement maxEl = null;
		for (XSDElement el : xsd.getElementDistanceMap().keySet()) {
			if (xsd.getElementDistanceMap().get(el) > max) {
				max = xsd.getElementDistanceMap().get(el);
				maxEl = el;
			}
		}
		if (maxEl != null) {
			// String [] elemLst = new String[4];
			ArrayList<String> elemLst = new ArrayList<String>();
			elemLst.add(maxEl.getName());
			// elemLst.add(String.valueOf(maxEl.getValue()));
			elemLst.add(xsd.getName());
			// elemLst.add(String.valueOf(value));
			elemLst.add(String.valueOf(max));
			this.elemMappingArray.add(elemLst);
			System.out.println("this stuff: " + maxEl.getName() + "\t"
					+ maxEl.getValue() + "\t" + xsd.getName() + "\t" + value
					+ "\t" + max);
		}
		return maxEl;
	}

	// Grammars do not have id so Map By Structure only for Resources
	private void mapByStructure(WADLFile file2,
			HashMap<String, Resources> mapped,
			HashMap<String, Resources> added, HashMap<String, Resources> deleted) {
		HashMap<String, Resources> notAdded = new HashMap<String, Resources>();
		HashMap<String, Resources> notDeleted = new HashMap<String, Resources>();
		for (String base : added.keySet()) {
			for (String base2 : deleted.keySet()) {
				if (added.get(base).mapElement(deleted.get(base2))) {
					notAdded.put(base, added.get(base));
					notDeleted.put(base2, deleted.get(base2));
				}
			}
		}
		for (String base : notAdded.keySet()) {
			added.remove(base);
			mapped.put(base, notAdded.get(base));
		}
		for (String base : notDeleted.keySet()) {
			deleted.remove(base);
			mapped.put(base, notDeleted.get(base));
		}

	}

	private void mapByID(WADLFile file2, HashMap<String, Resources> mapped,
			HashMap<String, Resources> added, HashMap<String, Resources> deleted) {
		for (String base : file2.resourcesElements.keySet()) {
			if (this.getResourcesElements().containsKey(base)) {
				mapped.put(base, this.resourcesElements.get(base));
			} else {
				added.put(base, file2.resourcesElements.get(base));
			}
		}

		for (String base : this.resourcesElements.keySet()) {
			if (!mapped.containsKey(base)) {
				deleted.put(base, this.resourcesElements.get(base));
			}
		}

	}

	public void serializeFile() {
		// String serFilename = wadlFilename.toString();
		String serFilename = wadlFilename.replace(".wadl", ".ser");
		try {
			ObjectOutputStream out = new ObjectOutputStream(
					new FileOutputStream(new File(serFilename)));
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
		// String serFilename = wadlFilename.toString();
		String serFilename = filename.replace(".wadl", ".ser");
		WADLFile file = null;
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(
					new File(serFilename)));
			Object object = in.readObject();
			file = (WADLFile) object;
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
