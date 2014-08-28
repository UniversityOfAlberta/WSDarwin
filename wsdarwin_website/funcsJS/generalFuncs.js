//import "ObjTree.js";
//import "jsdiff.js";

// tomcat server path/location
var tomcat_server_path				= "http://localhost:8080/";
//var tomcat_server_path			= "http://ssrg17.cs.ualberta.ca:8080/";

var server_api_url 					= tomcat_server_path + "wsdarwin_1.0.0/jaxrs/api/";

// for java_diff: ( if true, appends text line by line, otherwise it prints it all in a tree-hierarchy way)
var parsing_html_mode 			= true;
var max_wadl_files_uploaded 	= 1;
var sideToWriteTo 				= "";
var crossService 				= true;

var noURLFields 				= 0;
var noCompareURLFields 			= 0;
var uppedWADLurls 				= [];
var compareWADLurls 			= [];

//wadl html file
var wadl_attribute_edit_mode 	= true;	// DEBUG (only applicable to 'analyze', not 'compare')
var add_elements_mode 			= true;			// DEBUG (only applicable to 'analyze', not 'compare')

var session_id 					= '';	// initially set in the java app
var html_wadl_string 			= '';
var strapped_html_wadl_string 	= '';
var xml_wadl_string 			= '';

// type confidence colors
var typeConfidence0_24 		= "#c9302c";
var typeConfidence24_49 	= "#F0854E";
var typeConfidence50_74 	= "#f0ad4e";
var typeConfidence75_100 	= "#449d44";

var oldCompare 	= '';
var newCompare 	= '';
var rightSideID = "b";
var leftSideID 	= "a";

var lineNumber 	= 0;
var spaces 		= 0;
var node_id 	= 0;
var rootNode;
var xmlDoc;

var oldRootDoc;
var newRootDoc;

var enumConvertedArray = new Array();

var firstWADL = "";
var secondWADL = "";

// highlight colors
var crossServiceMappingsHighlightColor = "#cdfeff";

// class declarations
function wadl_doc(){

	this.xmlDoc = null;

	this.hello = function(){
		alert('Hello World ' + this.xmlDoc);
	}

	this.setXmlDoc = function(arg){
		this.xmlDoc = arg;
	}

	this.getXmlDoc = function(){
		return this.xmlDoc;
	}

}

var analyzeObj = new wadl_doc();

// User-action functions ---------------------------- 

function activateView(viewid){
	$("#" + 'analysisView').removeClass("active");
	$("#" + 'comparisonView').removeClass("active");
	$("#" + 'crossServiceComparisonView').removeClass("active");
	$("#" + 'settingsView').removeClass("active");
	$("#" + viewid).addClass("active");

	if (viewid == "analysisView"){
		window.location.hash = "analysisViewName";
		//$("#compareA").show();
		$("#compareB").hide();
		$(".compareOptions").hide();
		$("#analyzeSubmitBtn").show();
		$("#compareSubmitBtn").hide();
		$("#crossServiceCompareSubmitBtn").hide();

		$("#files").text("Upload a WADL file");
		$("#filesCompare").text("Upload a WADL file");

		$("#uploadAWADLA").css("visibility", "");
		$("#uploadAWADLB").css("visibility", "");
	} else if (viewid == "comparisonView"){
		window.location.hash = "compareViewName";
		$(".compareOptions").show();
		$("#compareB").show();
		$("#analyzeSubmitBtn").hide();
		$("#crossServiceCompareSubmitBtn").hide();
		$("#compareSubmitBtn").show();

		$("#files").text("Upload a WADL file");
		$("#filesCompare").text("Upload a WADL file");
		$("#uploadAWADLA").css("visibility", "");
		$("#uploadAWADLB").css("visibility", "");

	} else if (viewid == "crossServiceComparisonView"){
		window.location.hash = "crossServiceViewName";

		$(".compareOptions").hide();

		$("#compareB").show();
		$("#analyzeSubmitBtn").hide();	// need to be changed to upload .ser files for now
		$("#compareSubmitBtn").hide();	// ..
		$("#files").text("Upload a (.wsmeta) file");
		$("#filesCompare").text("Upload a (.wsmeta) file");

		$("#uploadAWADLA").css("visibility", "");
		$("#uploadAWADLB").css("visibility", "");


		$("#crossServiceCompareSubmitBtn").show();
	}
}

function addURLField(){

	var fullDiv = "	<div class='singleUrlDiv' id='singleURLdiv_" + noURLFields + "'>"+
				  "	<select class='urlSelectBtn' id='requestType1'>"+
				  "	<option value='get' selected>GET</option>"+
						"<option value='post'>POST</option>"+
						"<option value='put'>PUT</option>"+
						"<option value='delete'>DELETE</option>"+
						"<option value='head'>HEAD</option>"+
					"</select>"+
					//"<input type='text' name='lname' id='urlInput_" + noURLFields + "' class='urlInput'>"+
					"<input type=\"text\" name='lname' id='urlInputA_" + noURLFields + "' style='width: 70%; display: inline-block;' class=\"form-control urlInputClassA\" placeholder=\"Text input\">" +
					//"<button id='analyzeSingleURL1' class='analyzeSingleURL' onClick=\"analyzeSingleURL('" + noURLFields + "')\"> Analyze </button>"+
					"<button style='margin-left: 10px; vertical-align: top;' onClick=\"removeURLField('" + noURLFields + "')\" type=\"button\" class=\"btn btn-danger\">X</button>" +
					//"<button class='singleUrlRemoveBtn' onClick=\"removeURLField('" + noURLFields + "')\">Remove</button>"+
					"</div>";
	$('#fieldUrlDiv').append(fullDiv);
	noURLFields++;
}

function addCompareURLField(){
	var fullDiv = "	<div class='singleUrlDiv' id='singleCompareUrlDiv_" + noCompareURLFields + "'>"+
				  "	<select class='urlSelectBtn' id='requestType1'>"+
				  "	<option value='get' selected>GET</option>"+
						"<option value='post'>POST</option>"+
						"<option value='put'>PUT</option>"+
						"<option value='delete'>DELETE</option>"+
						"<option value='head'>HEAD</option>"+
					"</select>"+
					"<input type=\"text\" name='comparelname' id='urlInputB_" + noCompareURLFields + "' style='width: 70%; display: inline-block;' class=\"form-control urlInputClassB\" placeholder=\"Text input\">" +
					"<button style='margin-left: 10px; vertical-align: top;' onClick=\"removeCompareURLField('" + noCompareURLFields + "')\" type=\"button\" class=\"btn btn-danger\">X</button>" +
					"</div>";
	$('#fieldCompareUrlDiv').append(fullDiv);
	noCompareURLFields++;
}

function get_input_urls(inputNo){
	var urlArray = [];
	// JSON urls to analyze
	$('input.urlInputClass' + inputNo).each(function(index) {
		urlArray.push( $('#urlInput' + inputNo + '_'+index).val() );
	});
	return urlArray;
}

var crossMappings = new Array();

function crossServiceCompareBtn(){
	runAnalysis("crossServiceCompare");
}

function compareBtn(){
	runAnalysis("compare");
}

function analyzeBtn(){
	runAnalysis("analyze");
}

function runAnalysis(process_mode){
	// reset html elements
	$("#left_wadl_output").hide();
	$("#right_wadl_output").hide();
	$("#wadlOutput").show();
	$("#wadlOutput").html('');

	var api_call_url;
	var ajaxData;

	if (process_mode === "analyze"){
		// input urls
		var analyzeDataJSON = JSON.stringify(get_input_urls('A'));

		// WADL files uploaded..
		var analyzed_wadls_URLs = [];

		for (var i = 0; i < uppedWADLurls.length; i++){
			analyzed_wadls_URLs.push(uppedWADLurls[i]);
		}
		var jsonWadlURLs = JSON.stringify(analyzed_wadls_URLs);

		api_call_url = server_api_url + "analyze";
		ajaxData = { newURLs: analyzeDataJSON, newUppedFiles: jsonWadlURLs, sessionid: session_id };
	} else if ( (process_mode === 'compare') || (process_mode === 'crossServiceCompare') ){
		// input urls
		var analyzeDataJSON = JSON.stringify(get_input_urls('A'));
		var compareDataJSON = JSON.stringify(get_input_urls('B'));

		// WADL files uploaded..
		var analyzed_wadls_URLs = [];
		var compare_wadls_URLs = [];

		for (var i = 0; i < uppedWADLurls.length; i++){
			analyzed_wadls_URLs.push(uppedWADLurls[i]);
		}
		
		for (var i = 0; i < compareWADLurls.length; i++){
			compare_wadls_URLs.push(compareWADLurls[i]);
		}
		
		var jsonWadlURLs 		= JSON.stringify(analyzed_wadls_URLs);
		var jsonCompareWadlURLs = JSON.stringify(compare_wadls_URLs);

		api_call_url = server_api_url + process_mode;
		ajaxData = {newURLs: analyzeDataJSON, newUppedFiles: jsonWadlURLs, sessionid: session_id, compareURLs: compareDataJSON, compareWADLfiles: jsonCompareWadlURLs};
	}
	
	console.log("--------AJAX data:-------- ");
	console.debug("api call: " 	+ api_call_url);
	console.debug("ajax data: " + ajaxData);
	console.log("---------------- ");
    
    $.ajax({
    	url: api_call_url,
        type: "GET",
        data: ajaxData,
        dataType: "html",
        crossDomain: true,				// sending ajax call to a jsp servlet, thus needing to enable this
        success: function(response) {
        	console.debug("response is " + response);
        	var jsonObj = JSON.parse(response);

        	session_id = jsonObj[0];
        	var analysis_merged_wadl_url_path = jsonObj[1];
			var compare_merged_wadl_url_path = jsonObj[2];
			var delta_comparison_url = jsonObj[3];
			console.log(delta_comparison_url);

			console.log("merged #1_: " + analysis_merged_wadl_url_path);
			console.log("merged #2_: " + compare_merged_wadl_url_path);
			console.log("Compare or Analysis? :'" + process_mode + "'");

			console.debug("jsonobj4 is " + jsonObj[4]);

			if (process_mode == "compare"){
				console.log("diffing the wadl's");
				add_elements_mode = false;

				getWADL(analysis_merged_wadl_url_path);
				firstWADL = xml_wadl_string;
				getWADL(compare_merged_wadl_url_path);
				secondWADL = xml_wadl_string;

				compareXMLDoc = loadXMLDoc(analysis_merged_wadl_url_path);
				oldRootDoc = compareXMLDoc.documentElement;
				compareXMLDoc = loadXMLDoc(compare_merged_wadl_url_path);
				newRootDoc = compareXMLDoc.documentElement;
				init_node(oldRootDoc, "_a_");
				init_node(newRootDoc, "_b_");

				if ( $('.diffTypeText:checked').val() ) {
					//text_diff_JS(1);		
					sideBySideDiff();												// text comparison diff
				} else if ( $('.diffTypeJava:checked').val() ) {
					java_diff(delta_comparison_url, oldRootDoc, newRootDoc, process_mode);	// java comparison diff
				}
				
				//saveWADLtoFile();
			} else if (process_mode == "analyze"){
				add_elements_mode = true;
				getWADL(analysis_merged_wadl_url_path);

				xmlDocTest = loadXMLDoc(analysis_merged_wadl_url_path);
				//smNode=xmlDocTest.documentElement;
				console.debug('! ! ! !analyze xml doc: ' + xmlDocTest);
				
				analyzeObj.setXmlDoc(xmlDocTest);

				//saveWADLtoFile();
			} else if (process_mode == "crossServiceCompare"){

				console.log("diffing the wadl's");
				add_elements_mode = false;

				getWADL(analysis_merged_wadl_url_path);
				firstWADL = xml_wadl_string;
				getWADL(compare_merged_wadl_url_path);
				secondWADL = xml_wadl_string;

				compareXMLDoc = loadXMLDoc(analysis_merged_wadl_url_path);
				oldRootDoc = compareXMLDoc.documentElement;
				compareXMLDoc = loadXMLDoc(compare_merged_wadl_url_path);
				newRootDoc = compareXMLDoc.documentElement;
				init_node(oldRootDoc, "_a_");
				init_node(newRootDoc, "_b_");

				console.debug("delta comparison url is: " + delta_comparison_url);
				java_diff(delta_comparison_url, oldRootDoc, newRootDoc, process_mode);	// java comparison diff
				//text_diff_JS(1);														// text comparison diff				
				//saveWADLtoFile();

	        	if (jsonObj[4] != null){

	        		//var listOfMappings = jsonObj[4];
	        		var n = 0;
	        		var arrayInd = 0;
	        		var listMappings = JSON.parse(jsonObj[4]);
	        		console.debug("list of mappings: " + listMappings);
	        		var mappingRow = listMappings[n];
	        		var elementsAnalyzed = new Array();

					crossMappings = [];
					var csind = 0;
	        		while(mappingRow != null){

	        			var leftElem = mappingRow[0];
	        			var rightElem = mappingRow[1];
	        			var score = mappingRow[2];

	        			var leftElemArray = mappingRow[0].split(":=");
	        			var rightElemArray = mappingRow[1].split(":=");

	        			var leftElemName = leftElemArray[0];
	        			var leftElemType = leftElemArray[1];
	        			var rightElemName = rightElemArray[0];
	        			var rightElemType = rightElemArray[1];

	        			if ( $.inArray(leftElem, elementsAnalyzed) > -1 ){
	        				// leftElem[0] has already been analyzed
	        				var ind = $.inArray(leftElem, elementsAnalyzed);
	              			var mapObject = new Object();
	        				mapObject.elemName = rightElem;
	        				mapObject.score = score;
	        				mapObject.elemId = "csr_" + csind;
	        				csind++;
	        				crossMappings[ind][1].push(mapObject);
	        			} else {
	        				crossMappings[arrayInd] = new Array(2);
	        				var leftObject = new Object();
	        				leftObject.elemName = leftElem;
	        				leftObject.elemId = "csl_" + csind;
	        				csind++;
	        				crossMappings[arrayInd][0] = leftObject;
	        				crossMappings[arrayInd][1] = new Array();
	        				var mapObject = new Object();
	        				mapObject.elemName = rightElem;
	        				mapObject.score = score;
	        				mapObject.elemId = "csr_" + csind;
	        				csind++;
	        				crossMappings[arrayInd][1].push(mapObject);

	        				elementsAnalyzed[arrayInd] = leftElem;
	        				arrayInd++;
	        			}
	        			n++;
	        			mappingRow = listMappings[n];
	        		}

	        		var emptyBtn 		= "<a href='#' class='list-group-item' style='visibility: hidden'>empty</a>";
	        		var emptyDivPadding = "<div style='width: 100%; height: 10px;'></div>";

	        		$("#leftHalfDiv").html("");
	        		$("#rightHalfDiv").html("");

	        		console.log('start of crossMappings print');
	        		console.debug(crossMappings);
	        		console.log('end of crossMappings print');

	        		for (var i = 0; i < crossMappings.length; i++){
	        			console.log("left elem: " + crossMappings[i][0] + " and it's connections: ");
	        			//var leftEname 	= crossMappings[i][0].split(":=")[0];
	        			var leftEname 	= crossMappings[i][0].elemName.split(":=")[0];
	        			var leftBtnId 	= crossMappings[i][0].elemId;
	        			var leftBtn 	= "<a href='javascript:void(0)' id='" + leftBtnId + "' onClick='highlightLeftCrossServiceMappings(" + i + ")' class='list-group-item'>" + leftEname + "</a>";
	        			$("#leftHalfDiv").append(leftBtn);
	        			for (var k = 0; k < crossMappings[i][1].length; k++){
	        				console.log(" ------> " + crossMappings[i][1][k].elemName);
	        				//var leftEname = crossMappings[i][0].split(":=")[0];
	        				var rightEname 	 = crossMappings[i][1][k].elemName.split(":=")[0];
	        				var rightBtnId 	 = crossMappings[i][1][k].elemId;
	        				var mappingScore = crossMappings[i][1][k].score;

	        				var highlightColor = '';
							if ( (mappingScore > 0) && (mappingScore <= 24) ){
								highlightColor = "red";		// red
							} else if ( (mappingScore > 24) && (mappingScore <= 49) ){
								highlightColor = "orange";	// orange
							} else if ( (mappingScore > 49) && (mappingScore <= 74) ){
								highlightColor = "yellow";	// yellow
							} else if ( (mappingScore > 74) && (mappingScore <= 100) ){
								highlightColor = "green";	// green
							}

	        				//var leftBtn 	= "<a href='#' class='list-group-item' onClick='highlightCrossServiceMappings(\"" + leftEname + "\", \"" + rightEname + "\")'>" + crossMappings[i][0] + "  [" + crossMappings[i][1][k].score + "]   " + crossMappings[i][1][k].elemName + "</a>";
	        				var rightBtn 	= "<a href='javascript:void(0)' id='" + rightBtnId + "' onClick='highlightRightCrossServiceMappings(" + i + ", " + k + ")' class='list-group-item' style=\"background-color: " + highlightColor + "\" >" + rightEname + "</a>";
	        				
	        				if (k != 0){
	        					$("#leftHalfDiv").append(emptyBtn);
	        				}
	        				$("#rightHalfDiv").append(rightBtn);
	        				//$("#rightHalfDiv").append("<div><button onClick='highlightCrossServiceMappings(\"" + leftEname + "\", \"" + rightEname + "\")'>" + crossMappings[i][0] + "  [" + crossMappings[i][1][k].score + "]   " + crossMappings[i][1][k].elemName + "</button></div>");
	        			}
	        			$("#leftHalfDiv").append("</br>");
	        			$("#rightHalfDiv").append("</br>");
	        		}
	        	} else {
	        		// NULL
	        	}

			}



        },
        error: function(xhr, status, error) {
		  alert("Error accessing app. Please try again !");
		}

    });
}

function showCompareOptions(){
	$("#showCompareBtn").html("- Hide Compare Tool");
	$("#showCompareBtn").attr('onclick', "hideCompareOptions()");
	$("#compareDiv").css('height', "inherit");
}

function hideCompareOptions(){
	$("#showCompareBtn").html("+ Show Compare Tool");
	$("#showCompareBtn").attr('onclick', "showCompareOptions()");
	$("#compareDiv").css('height', "23px");
}

function showOptions() {
	$("#showOptionsBtn").html("- Hide Options");
	$("#showOptionsBtn").attr('onclick', "hideOptions()");
	$("#optionsDiv").css('height', "inherit");
}

function hideOptions() {
	$("#showOptionsBtn").html("+ Show Options");
	$("#showOptionsBtn").attr('onclick', "showOptions()");
	$("#optionsDiv").css('height', "23px");
}

// Utility functions ---------------------------- 

function var_dump(obj) {
    var out = '';
    for (var i in obj) {
        out += i + ": " + obj[i] + "\n";
    }
    console.log(out);
}

function htmlentities(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function sideBySideDiff() {
	text_diff_JS(0);
}

function inlineDiff() {
	text_diff_JS(1);
}

function text_diff_JS(viewingType) {
    // get the baseText and newText values from the two textboxes, and split them into lines

    var base = difflib.stringAsLines( firstWADL );
    var newtxt = difflib.stringAsLines( secondWADL );

    // create a SequenceMatcher instance that diffs the two sets of lines
    var sm = new difflib.SequenceMatcher(base, newtxt);

    // get the opcodes from the SequenceMatcher instance
    // opcodes is a list of 3-tuples describing what changes should be made to the base text
    // in order to yield the new text
    var opcodes = sm.get_opcodes();
    var diffoutputdiv = $("wadlOutput");
    while (diffoutputdiv.firstChild) diffoutputdiv.removeChild(diffoutputdiv.firstChild);
    //var contextSize = $("contextSize").value;
    //contextSize = contextSize ? contextSize : null;

    // build the diff view and add it to the current DOM
    var diffed_string = diffview.buildView({
	        baseTextLines: base,
	        newTextLines: newtxt,
	        opcodes: opcodes,
	        // set the display titles for each resource
	        baseTextName: "Base Text",
	        newTextName: "New Text",
	        //contextSize: null,
	        //viewType: $("comparisonDiffType").checked ? 1 : 0
	        viewType: viewingType
    	});
    //);
	$("#wadlOutput").show();
	$("#left_wadl_output").hide();
	$("#right_wadl_output").hide();

    $("#wadlOutput").html( diffed_string );

}

function saveWADLtoFile(){
    console.log("saving wadl file");
    $.ajax({
    	url: "/wsdarwin/funcsPHP/downloadWADLfile.php",
        type: "POST",
        data: { wadlString: xml_wadl_string },
        dataType: "html",
        success: function(response) {
        	console.log("success?? response: " + response);
        	onclick="this.target='_blank';"
        },
        error: function(xhr, status, error) {
		  console.log("Error acccesing downloadWADLfile.");
		}

    });
}

function downloadWADL(){
	saveWADLtoFile();
	window.open("http://pokemonpacific.com/wsdarwin/funcsPHP/wadlFile.wadl", '_blank', 'download');
}

function removeURLField(id){
	console.debug("length is " + $("#fieldUrlDiv").children().filter("div").length );
	//console.log("length is " + $("div.fieldUrlDiv").length );
	if ($("#fieldUrlDiv").children().filter("div").length > 1){
		$('#singleURLdiv_'+id).remove();
		reassignIDs();
	}
}

function removeCompareURLField(id){
	if ($("#fieldCompareUrlDiv").children().filter("div").length > 1){
		$('#singleCompareUrlDiv_'+id).remove();
		reassignIDs();
	}	
}

function reassignIDs(){
	var idcount = 0;
	// loop through all divs
	$('div.singleUrlDiv').each(function(index) {
	    // set div id to array value

	    $('#singleUrlDiv_' + index).attr('id', "singleURLdiv_" + index);
	   	//console.debug(this);
	    idcount++;

	});

	noURLFields = idcount;
}

function removeNode(mynodeid){
	var foundNode = find_node(rootNode, mynodeid);
	console.log('found node name: ' + foundNode.nodeName + ', parent: ' + foundNode.parentNode.nodeName);
	foundNode.parentNode.removeChild(foundNode);
	setup_wadl_print();
}

function hideNodesChildren(mynodeid){
	var foundNode = find_node(rootNode, mynodeid);
	foundNode.minimized = true;
	setup_wadl_print();
}

function showNodesChildren(mynodeid){
	var foundNode = find_node(rootNode, mynodeid);
	foundNode.minimized = false;
	setup_wadl_print();
}

function addResource(mynodeid){
	var foundNode = find_node(rootNode, mynodeid);
	var newNode=xmlDoc.createElement("resource");
	newNode.setAttribute("path","");
	foundNode.appendChild(newNode);
	//document.getElementById("popupDiv").style.visibility="visible";
	setup_wadl_print();
}

function addMethod(mynodeid){
	var foundNode = find_node(rootNode, mynodeid);
	var methodNode=xmlDoc.createElement("method");
	methodNode.setAttribute("name","");

	foundNode.appendChild(methodNode);

	setup_wadl_print();
}

function addParam(mynodeid){
	var foundNode = find_node(rootNode, mynodeid);
	var newNode=xmlDoc.createElement("param");
	newNode.setAttribute("path","");
	foundNode.appendChild(newNode);

	setup_wadl_print();
}

function addRequest(mynodeid){
	var foundNode = find_node(rootNode, mynodeid);
	var newNode=xmlDoc.createElement("request");
	foundNode.appendChild(newNode);

	setup_wadl_print();
}

function addResponse(mynodeid){
	var foundNode = find_node(rootNode, mynodeid);
	var newNode=xmlDoc.createElement("response");
	newNode.setAttribute("status","");
	foundNode.appendChild(newNode);

	setup_wadl_print();
}

function addRepresentation(mynodeid){
	var foundNode = find_node(rootNode, mynodeid);
	var newNode=xmlDoc.createElement("representation");
	newNode.setAttribute("element","");
	newNode.setAttribute("mediaType","");
	foundNode.appendChild(newNode);

	setup_wadl_print();
}

function addFault(mynodeid){
	var foundNode = find_node(rootNode, mynodeid);
	var newNode=xmlDoc.createElement("fault");
	newNode.setAttribute("status","");
	newNode.setAttribute("mediaType","");
	foundNode.appendChild(newNode);

	setup_wadl_print();
}

function updateAttribute(mynodeid, nodeAttrName, newAttrValue){
	var foundNode = find_node(rootNode, mynodeid);
	for (var i = 0; i < foundNode.attributes.length; i++){
		if (foundNode.attributes[i].nodeName == nodeAttrName){
			foundNode.attributes[i].value = newAttrValue;
			console.log("updated node with id: " + foundNode.my_id + ", nodeName: " + foundNode.attributes[i].nodeName + ",value of attr:" + foundNode.attributes[i].value);
		}
	}
	setup_wadl_print();
}

function find_node(mynode, sid){
	if (mynode.my_id == sid){
		window.foundNode = mynode;
	}
	for (var i = 0; i < mynode.childNodes.length; i++){
		if (mynode.childNodes[i].my_id == sid){
			window.foundNode = mynode.childNodes[i];		// window.foundNode global
			console.log('[find_node] found node name is ' + window.foundNode.nodeName);
			return window.foundNode;
		}
		find_node(mynode.childNodes[i], sid);
	}
	return window.foundNode;
}

var foundNode2 = {};
// this function is used only once, and has been made in order to be able to also save an 
// element's position where it was found, if applicable;

// TO DO: fix 'find_node' ( the implementation below does not work properly either)
function find_node_2(mynode, sid){
	if (mynode.my_id == sid){
		foundNode2['node'] 		= mynode;
		foundNode2['position'] 	= -1;	//	Not applicable
	}
	for (var i = 0; i < mynode.childNodes.length; i++){
		if (mynode.childNodes[i].my_id == sid){
			foundNode2['node'] 		= mynode.childNodes[i];
			foundNode2['position'] 	= i;
			console.log('[find_node_2] found node name is ' + window.foundNode2['node'].nodeName);
			return foundNode2;
		}
		find_node_2(mynode.childNodes[i], sid);
	}
	return foundNode2;
}

function init_node(myNode, idSubname){
	node_id = 0;
	init_element_minimize_property(myNode);
	init_element_ids(myNode, idSubname);
}

function init_element_minimize_property(myNode){
	myNode.minimized = false;
	for (var i = 0; i < myNode.childNodes.length; i++){
		init_element_minimize_property(myNode.childNodes[i]);
	}
}

function init_element_ids(myNode, idSubname){
	myNode.my_id = node_id + idSubname;
	node_id++;
	for (var i = 0; i < myNode.childNodes.length; i++){
		init_element_ids(myNode.childNodes[i], idSubname);
	}
}

function highlightRemovingDiv(divid){
	//$("#"+divid).css('background-color', 'red');
	$("#"+divid).css('text-decoration', 'line-through');
}

function unhighlightRemovingDiv(divid){
	//$("#"+divid).css('background-color', '');
	$("#"+divid).css('text-decoration', '');
}

function setup_wadl_print_free(){
	html_wadl_string = '';
	strapped_html_wadl_string = '';
	xml_wadl_string = '';
	spaces = 0;
	parse_wadl_html(rootNode);
	//parse_wadl_html_two(rootNode);
	$("#wadlOutput").html(xml_wadl_string);
}

function addSpaces(){
	var n = 0;
	while (n < spaces){
		html_wadl_string += "&nbsp;";
		//strapped_html_wadl_string is using relative margin-left
		xml_wadl_string += " ";
		n++;
	}
}

function addSpacesTwo(){
	var n = 0;
	while (n < spaces){
		html_wadl_string += "&nbsp;";
		//strapped_html_wadl_string += " ";
		n++;
	}
}

function java_diff(deltas_path, oldDocNode, newDocNode, process_type){
	console.debug("JAVA DIFF: " + deltas_path);
	xmlDoc = loadXMLDoc(deltas_path);
	mainNode=xmlDoc.documentElement;

	console.log(" --- JAVA DIFFING --- ");
	//console.log("name: " + mainNode.nodeName + ", attrs length: " + mainNode.childNodes.length + ", attribute's value: " + mainNode.attributes.getNamedItem("xmlns").value);
	//console.log(xmlDoc);

	$("#left_wadl_output").html("");
	$("#right_wadl_output").html("");

	strapped_html_wadl_string = '';
	sideToWriteTo = "left";
	padding_left = 0;
	level = 1;
	lineNumber = 0;
	parse_wadl_html(oldDocNode);

	var oldText = strapped_html_wadl_string;
	strapped_html_wadl_string = '';
	sideToWriteTo = "right";
	padding_left = 0;
	level = 1;
	lineNumber = 0;
	parse_wadl_html(newDocNode);

	var newText = strapped_html_wadl_string;
	strapped_html_wadl_string = '';

	$("#wadlOutput").hide();
	$("#left_wadl_output").show();
	$("#right_wadl_output").show();

	if (parsing_html_mode){

	} else {
		$("#left_wadl_output").html(oldText);
		$("#right_wadl_output").html(newText);
	}

	if (process_type === "crossServiceCompare"){
		console.log("cross servicing here..");
		$(".halfwadlOutput").css("width", "40%");

	} else if (process_type === "compare") {
		highlightComparedWADLs(mainNode, oldDocNode, newDocNode);
	}
}

function highlightComparedWADLs(mainNode, oldDocNode, newDocNode){
	for (var i = 0; i < mainNode.childNodes.length; i++){
		iserviceNode = mainNode.childNodes[i];
		if (iserviceNode.nodeName == "MatchDelta"){
			//console.log("Skipping iservice:" + iserviceNode.nodeName);
			continue;
		}

		for (var k = 0; k < iserviceNode.childNodes.length; k++){
			interfaceNode = iserviceNode.childNodes[k];
			if (interfaceNode.nodeName == "MatchDelta"){
				continue;
			} else if (interfaceNode.nodeName == "ChangeDelta"){
				console.log("=============== changed interface ================");
				if (interfaceNode.attributes.getNamedItem("changedAttribute") != null){
					if (interfaceNode.attributes.getNamedItem("changedAttribute").value === "address"){
						var oldSplits = interfaceNode.attributes.getNamedItem("oldValue").value.split("/");
						var newSplits = interfaceNode.attributes.getNamedItem("newValue").value.split("/");

						highlightChangedResource(newSplits, oldDocNode, "ChangeDelta");
						highlightChangedResource(oldSplits, newDocNode, "ChangeDelta");
					}
				}
			} else if (interfaceNode.nodeName == "AddDelta"){
				console.log(" AddDelta--> the source is " + interfaceNode.attributes.getNamedItem("target").value);
				//var targetSplits = interfaceNode.attributes.getNamedItem("target").value.split("\\");
				var targetVal = interfaceNode.attributes.getNamedItem("target").value;
				//highlightResourceNodeDiv(targetSplits[targetSplits.length - 1], newDocNode, 'AddDelta');
				highlightResourceNodeDiv(targetVal, newDocNode, 'AddDelta');
			} else if (interfaceNode.nodeName == "DeleteDelta"){
				
				//console.log(" DeleteDelta--> the source is " + interfaceNode.attributes.getNamedItem("source").value);
				//var sourceSplits = interfaceNode.attributes.getNamedItem("source").value.split("\\");
				var sourceVal = interfaceNode.attributes.getNamedItem("source").value;
				highlightResourceNodeDiv(sourceVal, oldDocNode, 'DeleteDelta');
				//highlightResourceNodeDiv(sourceVal, oldDocNode, 'Space');
			}
			
			for (var j = 0; j < interfaceNode.childNodes.length; j++){ // 2
				operationNode = interfaceNode.childNodes[j];
				if (operationNode.nodeName == "MatchDelta"){
					continue;
				} else if (operationNode.nodeName == "ChangeDelta"){
					if (operationNode.attributes.getNamedItem("changedAttribute") != null){
						if (operationNode.attributes.getNamedItem("changedAttribute").value === "WHAT SHOULD GO HERE !*****"){ // <-------****
							// *****
						}
					}
				} else if (operationNode.nodeName == "AddDelta"){
					var targetVal = operationNode.attributes.getNamedItem("target").value;
					highlightMethodNodeDiv(targetVal, newDocNode, 'AddDelta');
				} else if (operationNode.nodeName == "DeleteDelta"){
					var sourceVal = operationNode.attributes.getNamedItem("source").value;
					highlightMethodNodeDiv(sourceVal, oldDocNode, 'DeleteDelta');
				}
				
				for (var m = 0; m < operationNode.childNodes.length; m++){
					xsNode = operationNode.childNodes[m];
					

					if (xsNode.nodeName == "MatchDelta"){
						//console.log("Skipping operation:" + xsNode.nodeName);
						continue;
					} else if (xsNode.nodeName == "ChangeDelta"){
						if (xsNode.attributes.getNamedItem("changedAttribute") != null){
							if (xsNode.attributes.getNamedItem("changedAttribute").value === "WHAT SHOULD GO HERE !*****"){ // <-------****
								// -- no cases encountered
							}
						}
					} else if (xsNode.nodeName == "AddDelta"){
						// request:  operationNode.attributes.getNamedItem('target').value + "RequestType"
						// response: operationNode.attributes.getNamedItem('target').value + "ResponseType"
						var targetVal = xsNode.attributes.getNamedItem("target").value;
						var targetSplits = targetVal.split(":");
						highlightCTypeNodeDiv(targetSplits[0], newDocNode, 'AddDelta');
					} else if (xsNode.nodeName == "DeleteDelta"){
						// request:  operationNode.attributes.getNamedItem('source').value + "RequestType"
						// response: operationNode.attributes.getNamedItem('source').value + "ResponseType"
						var sourceVal = xsNode.attributes.getNamedItem("source").value;
						var sourceSplits = sourceVal.split(":");
						highlightCTypeNodeDiv(sourceSplits[0], oldDocNode, 'DeleteDelta');


					}

					for (var p = 0; p < xsNode.childNodes.length; p++){
						var xs_twoNode = xsNode.childNodes[p];
						if (xs_twoNode.nodeName == "MatchDelta"){
							continue;
						} else if (xs_twoNode.nodeName == "ChangeDelta"){
							// *** add change delta
						} else if (xs_twoNode.nodeName == "AddDelta"){
							//var targetVal = xsNode.getNamedItem("target").value;
							var targetSplits = xsNode.attributes.getNamedItem("target").value.split(":");
							var targetSplitsInside = xs_twoNode.attributes.getNamedItem("target").value.split(":");
							highlightXSElemInsideCTypeNodeDiv(targetSplits[0], targetSplitsInside[0], newDocNode, 'AddDelta');
						} else if (xs_twoNode.nodeName == "DeleteDelta"){
							//var sourceVal = xsNode.getNamedItem("source").value;
							var sourceSplits = xsNode.attributes.getNamedItem("source").value.split(":");
							var sourceSplitsInside = xs_twoNode.attributes.getNamedItem("source").value.split(":");
							highlightXSElemInsideCTypeNodeDiv(sourceSplits[0], sourceSplitsInside[0], oldDocNode, 'DeleteDelta');
						}
					}

				}
			} // 2

		}
	}
}

var highlightGreen = "#A2D293";
var highlightRed = "#ED9899";
var highlightYellow = "#FFDC87";
//$("#"+xsNode.my_id+"line").css("background-color", highlightColor);

function highlightChangedResource(splits, xml_doc, type){
	var highlightColor = '';
	if (type == "AddDelta"){
		highlightColor = highlightGreen;
	} else if (type == "DeleteDelta"){
		highlightColor = highlightRed;
	} else if (type == "ChangeDelta"){
		highlightColor = highlightYellow;
	}

	if ( (xml_doc.childNodes[1].nodeName != null) && (xml_doc.childNodes[1].nodeName == "resources") ){
		var resourcesNode = xml_doc.childNodes[1];
		console.log("resources node base: " + resourcesNode.attributes.getNamedItem('base').value );
		// go through each resource and if 
		for (var i = 0; i < resourcesNode.childNodes.length; i++) {
			var resNode = resourcesNode.childNodes[i];
			var ind = 1;	// splits[0] is the base, so start at 1
			while (resNode.nodeName == "resource"){
				//console.log("splits[ind] is " + splits[ind] + ", path value is " + resNode.attributes.getNamedItem('path').value );
				if ( !(splits[ind] === resNode.attributes.getNamedItem('path').value)) {
					
					if (parsing_html_mode){
						$("."+resNode.my_id + "line").css("background-color", highlightColor);
						/*$("."+newNode.my_id + "line").each(function(){
							this.css("background-color", highlightColor);
						});*/
					} else {
						$("#"+resNode.my_id+"line").css("background-color", highlightColor);
					}
				}
				ind++;
				resNode = resNode.childNodes[0];
			}
		}
	}
}

function contains(list, obj) {
    for (var i = 0; i < list.length; i++) {
        if (list[i] === obj) {
            return true;
        }
    }
    return false;
}

function reassignLineNumbers(side){
	if (side == leftSideID){
		lineNumber = 1;
		$("#left_wadl_output").children().each(function(){
			$(this).attr("data-lineNumber", lineNumber+"_a");
			lineNumber++;
		});
	} else if (side == rightSideID){
		lineNumber = 1;
		$("#right_wadl_output").children().each(function(){
			$(this).attr("data-lineNumber", lineNumber+"_b");
			lineNumber++;
		});
	}
	
}

function addGraySpace(startAtLineIndex, noLines, side){
	console.log("startat line index is " + startAtLineIndex + ", no lines: " + noLines + ", side: " + side);
	//startAtLineIndex--;
	var emptyDivs = "";
	var n = 0;
	while(n < noLines){
		emptyDivs += "<div style='background-color: gray'>.</div>";
		n++;
	}
	if (side == leftSideID){
		//$("#left_wadl_output")
	} else if (side == rightSideID){

	}
	
	//var abc = startAtLineIndex + "_" + side;
	console.log("!!!! startAtLineIndex is " +  startAtLineIndex);
	var elemToInsertAfter = $("*[data-lineNumber=" + startAtLineIndex + "]");
	$(emptyDivs).insertAfter( elemToInsertAfter );
	console.log("insert on side: " + side + " after elem with id: " + elemToInsertAfter.attr("id") );
}
// pass (method_id, "resources node")
function highlightResourceRec(resource_id, node, highlightColor){
	for (var i = 0; i < node.childNodes.length; i++){
		var newNode = node.childNodes[i];
		if (newNode.nodeName == "resource"){
			var areEqual = newNode.attributes.getNamedItem("path").value.toLowerCase() === resource_id.toLowerCase();
			//if (resource_Node.attributes.getNamedItem("path").value == resourcePath.toLowerCase() ){
			if (areEqual){

				if (parsing_html_mode){
					var endElemClass = newNode.my_id + "line_end";
					var elemNode = $("."+newNode.my_id + "line");
					var noLines = 1;
					// go thru each div until this id is again met (but with line_end) and highlight all divs
					while(elemNode.attr("class") != endElemClass ){
						noLines++;
						elemNode.css("background-color", highlightColor);
						elemNode = elemNode.next();
					}
					$("." + endElemClass).css("background-color", highlightColor);

					if (newNode.my_id.split("_")[1] == rightSideID){
						var newAttrLineNumber = elemNode.attr("data-lineNumber").split("_")[0] + "_" + leftSideID;
						console.log("adding gray space to: " + newAttrLineNumber);
						//addGraySpace(newAttrLineNumber, noLines, leftSideID);
						reassignLineNumbers(rightSideID);
					} else if (newNode.my_id.split("_")[1] == leftSideID) {
						var newAttrLineNumber = elemNode.attr("data-lineNumber").split("_")[0] + "_" + rightSideID;
						console.log("adding gray space to: " + newAttrLineNumber);
						//addGraySpace(newAttrLineNumber, noLines, rightSideID);
						reassignLineNumbers(leftSideID);
					}

					//console.log("the id is " + newNode.my_id);
					//var side = newNode.my_id.split("_");
					//console.log("and : " + side[1]);

				} else {
					$("#"+newNode.my_id).css("background-color", highlightColor);
				}
			} else {
				highlightResourceRec(resource_id, newNode, highlightColor);
			}
		}
	}
}

function highlightResourceNodeDiv(resource_path, xml_doc, type){
	var highlightColor = '';
	if (type == "AddDelta"){
		highlightColor = highlightGreen;
	} else if (type == "DeleteDelta"){
		highlightColor = highlightRed;
	} else if (type == "ChangeDelta"){
		highlightColor = highlightYellow;
	}
	console.log(" [highlightResourceNodeDiv] -- > '" + resource_path + "'");
	if ( (xml_doc.childNodes[1].nodeName != null) && (xml_doc.childNodes[1].nodeName == "resources") ){
		var resourcesNode = xml_doc.childNodes[1];
		highlightResourceRec(resource_path, resourcesNode, highlightColor);
	}
}

function highlightMethodRec(method_id, node, highlightColor){
	for (var i = 0; i < node.childNodes.length; i++){
		var newNode = node.childNodes[i];
		if (newNode.nodeName == "resource"){
			highlightMethodRec(method_id, newNode, highlightColor);
		} else if (newNode.nodeName == "method"){
			if (newNode.attributes.getNamedItem("id").value === method_id){
				if (parsing_html_mode){
					var elemNode = $("."+newNode.my_id + "line");
					var endElemClass = newNode.my_id + "line_end";
					var noLines = 0;
					while(elemNode.attr("class") != endElemClass ){
						noLines++;
						elemNode.css("background-color", highlightColor);
						elemNode = elemNode.next();
					}
					$("." + endElemClass).css("background-color", highlightColor);
				} else {
					$("#"+newNode.my_id).css("background-color", highlightColor);
				}
			}
		}
	}
}

function highlightMethodNodeDiv(method_id, xml_doc, type){
	var highlightColor = '';
	if (type == "AddDelta"){
		highlightColor = highlightGreen;
	} else if (type == "DeleteDelta"){
		highlightColor = highlightRed;
	} else if (type == "ChangeDelta"){
		highlightColor = highlightYellow;
	}

	// if there is a 'resources' node
	if ( (xml_doc.childNodes[1].nodeName != null) && (xml_doc.childNodes[1].nodeName == "resources") ){
		var resourcesNode = xml_doc.childNodes[1];
		highlightMethodRec(method_id, resourcesNode, highlightColor);
	}
}

function highlightCTypeRec(ctype_id, node, type){
	for (var i = 0; i < node.childNodes.length; i++){
		var newNode = node.childNodes[i];
		if (newNode.nodeName == "resource"){
			highlightMethodRec(method_id, newNode, highlightColor);
		} else if (newNode.nodeName == "method") {
			highlightMethodRec(method_id, newNode, highlightColor);
		} else if (newNode.nodeName == "method"){
			if (newNode.attributes.getNamedItem("id").value === method_id){
				if (parsing_html_mode){
					var elemNode = $("."+newNode.my_id + "line");
					var endElemClass = newNode.my_id + "line_end";
					while(elemNode.attr("class") != endElemClass ){
						elemNode.css("background-color", highlightColor);
						elemNode = elemNode.next();
					}
					$("." + endElemClass).css("background-color", highlightColor);


				} else {
					$("#"+newNode.my_id).css("background-color", highlightColor);
				}
			}
		}
	}
}

function highlightCTypeGrammarsRec(ctype_id, node, highlightColor){
	for (var i = 0; i < node.childNodes.length; i++){
		var newNode = node.childNodes[i];
		if (newNode.nodeName == "xs:schema"){
			highlightCTypeGrammarsRec(ctype_id, newNode, highlightColor);
		} else if (newNode.nodeName == "xs:complexType"){
			if (newNode.attributes.getNamedItem("name").value == ctype_id){
				if (parsing_html_mode){
					var elemNode = $("."+newNode.my_id + "line");
					var endElemClass = newNode.my_id + "line_end";
					while(elemNode.attr("class") != endElemClass ){
						elemNode.css("background-color", highlightColor);
						elemNode = elemNode.next();
					}
					$("." + endElemClass).css("background-color", highlightColor);
				} else {
					$("#"+newNode.my_id).css("background-color", highlightColor);
				}
			}
		} else if (newNode.nodeName == "xs:element"){
			var typeSplits = newNode.attributes.getNamedItem("type").value.split(":");
			if (typeSplits[1] == ctype_id){
				if (parsing_html_mode){
					var elemNode = $("."+newNode.my_id + "line");
					var endElemClass = newNode.my_id + "line_end";
					while(elemNode.attr("class") != endElemClass ){
						elemNode.css("background-color", highlightColor);
						elemNode = elemNode.next();
					}
					$("." + endElemClass).css("background-color", highlightColor);
				} else {
					$("#"+newNode.my_id).css("background-color", highlightColor);
				}
			}
		}
	}
}

function highlightCTypeNodeDiv(ctype_id, xml_doc, type){
	var highlightColor = '';
	if (type == "AddDelta"){
		highlightColor = highlightGreen;
	} else if (type == "DeleteDelta"){
		highlightColor = highlightRed;
	} else if (type == "ChangeDelta"){
		highlightColor = highlightYellow;
	}

	if ( (xml_doc.childNodes[1].nodeName != null) && (xml_doc.childNodes[1].nodeName == "resources") ){
		var resourcesNode = xml_doc.childNodes[1];
		//highlightCTypeRec(ctype_id, resourcesNode, highlightColor);

		for (var i = 0; i < resourcesNode.childNodes.length; i++){
			var resource_Node = resourcesNode.childNodes[i];

			for (var j = 0; j < resource_Node.childNodes.length; j++){
				var methodNode = resource_Node.childNodes[i];
				for (var m = 0; m < methodNode.childNodes.length; m++){
					var ctypeNode = methodNode.childNodes[m];
					// ********************************************************************************* not implemented:
					//if (ctypeNode.attributes.getNamedItem("id").value == ctype_id){
					//	$("#"+ctypeNode.my_id).css("background-color", highlightColor);
					//}
				}
			}
		}
	}

	if ( (xml_doc.childNodes[0].nodeName != null) && (xml_doc.childNodes[0].nodeName == "grammars") ){
		var grammarsNode = xml_doc.childNodes[0];
		highlightCTypeGrammarsRec(ctype_id, grammarsNode, highlightColor);
	}
}

function highlightXSElemInsideCTypeRec(complexTypeName, xselementName, node, highlightColor ){
	for (var i = 0; i < node.childNodes.length; i++){
		var newNode = node.childNodes[i];
		if (newNode.nodeName == "xs:schema"){
			highlightXSElemInsideCTypeRec(complexTypeName, xselementName, newNode, highlightColor);
		} else if (newNode.nodeName == "xs:complexType"){
			if (newNode.attributes.getNamedItem('name').value == complexTypeName){
				highlightXSElemInsideCTypeRec(complexTypeName, xselementName, newNode, highlightColor);
			}
		} else if (newNode.nodeName == "xs:sequence"){
			highlightXSElemInsideCTypeRec(complexTypeName, xselementName, newNode, highlightColor);
		} else if (newNode.nodeName == "xs:element"){
			if (newNode.attributes.getNamedItem('name').value == xselementName){
				if (parsing_html_mode){
					var elemNode = $("."+newNode.my_id + "line");
					var endElemClass = newNode.my_id + "line_end";
					while(elemNode.attr("class") != endElemClass ){
						elemNode.css("background-color", highlightColor);
						elemNode = elemNode.next();
					}
					$("." + endElemClass).css("background-color", highlightColor);
				} else {
					$("#"+newNode.my_id).css("background-color", highlightColor);
				}
			}
		}
	}
}

function highlightXSElemInsideCTypeNodeDiv(complexTypeName, xselementName, xml_doc, type){
	var highlightColor = '';
	if (type == "AddDelta"){
		highlightColor = highlightGreen;
	} else if (type == "DeleteDelta"){
		highlightColor = highlightRed;
	} else if (type == "ChangeDelta"){
		highlightColor = highlightYellow;
	}

	if ( (xml_doc.childNodes[0].nodeName != null) && (xml_doc.childNodes[0].nodeName == "grammars") ){
		var grammarsNode = xml_doc.childNodes[0];
		highlightXSElemInsideCTypeRec(complexTypeName, xselementName, grammarsNode, highlightColor);
	}
}

function highlightRightCrossServiceMappings(leftElemIndex, rightElemIndex){
	var highlightColor = "";
	var isActive = $("#" + crossMappings[leftElemIndex][1][rightElemIndex].elemId).hasClass('active');
	if (!isActive){	
		highlightColor = crossServiceMappingsHighlightColor;
		/*if ( (mappingScore > 0) && (mappingScore <= 24) ){
			highlightColor = "yellow";
		} else if ( (mappingScore > 24) && (mappingScore <= 49) ){
			highlightColor = "yellow";
		} else if ( (mappingScore > 49) && (mappingScore <= 74) ){
			highlightColor = "yellow";
		} else if ( (mappingScore > 74) && (mappingScore <= 100) ){
			highlightColor = "yellow";
		}*/
	}
	$("#" + crossMappings[leftElemIndex][1][rightElemIndex].elemId).toggleClass('active');
	var leftElemName 	= crossMappings[leftElemIndex][0].elemName.split(":=")[0];
	var rightElemName 	= crossMappings[leftElemIndex][1][rightElemIndex].elemName.split(":=")[0];

	// toggle highlight of element 'rightElemName' on the right side
	var grammarsNew = newRootDoc.childNodes[0];
	highlightXSElementWithName(rightElemName, grammarsNew, highlightColor);
}

// uses global var 'crossMappings'
function highlightLeftCrossServiceMappings(leftElemIndex){
	var isActive = $("#" + crossMappings[leftElemIndex][0].elemId).hasClass('active');
	var highlightColor = "";
	if (!isActive){
		highlightColor = crossServiceMappingsHighlightColor;
	}
	
	var leftElemName = crossMappings[leftElemIndex][0].elemName.split(":=")[0];
	$("#" + crossMappings[leftElemIndex][0].elemId).toggleClass('active');

	for (var i = 0; i < crossMappings[leftElemIndex][1].length; i++){
		var rightElemName = crossMappings[leftElemIndex][1][i].elemName.split(":=")[0];
		if (!isActive){
			$("#" + crossMappings[leftElemIndex][1][i].elemId).addClass('active');
		} else {
			$("#" + crossMappings[leftElemIndex][1][i].elemId).removeClass('active');
		}

		highlightCrossServiceMappings(leftElemName, rightElemName, highlightColor);
	}
}

function highlightCrossServiceMappings(leftElemName, rightElemName, highlightColor){
	console.log("l: " + leftElemName + " r: " + rightElemName);
	
	var grammarsOld = oldRootDoc.childNodes[0];
	var grammarsNew = newRootDoc.childNodes[0];
	highlightXSElementWithName(leftElemName, grammarsOld, highlightColor);
	highlightXSElementWithName(rightElemName, grammarsNew, highlightColor);
}

function highlightXSElementWithName(xselementName, node, highlightColor ){
	for (var i = 0; i < node.childNodes.length; i++){
		var newNode = node.childNodes[i];
		if (newNode.nodeName == "xs:schema"){
			highlightXSElementWithName(xselementName, newNode, highlightColor);
		} else if (newNode.nodeName == "xs:complexType"){
			highlightXSElementWithName(xselementName, newNode, highlightColor);
		} else if (newNode.nodeName == "xs:sequence"){
			highlightXSElementWithName(xselementName, newNode, highlightColor);
		} else if (newNode.nodeName == "xs:element"){
			if (newNode.attributes.getNamedItem('name').value == xselementName){
				$("#" + newNode.my_id + "line").css("background-color", highlightColor);
			}
		}
	}
}

function getWADL(wadl_url_path){
	xmlDoc = loadXMLDoc(wadl_url_path);
	rootNode=xmlDoc.documentElement;
	// nodeName, attributes, childNodes

	setup_wadl_print();
	//setup_wadl_print_free();
}

function loadXMLDoc(filename){
	if (window.XMLHttpRequest){
		xhttp=new XMLHttpRequest();
	} else // code for IE5 and IE6
	{
		xhttp=new ActiveXObject("Microsoft.XMLHTTP");
	}
	xhttp.open("GET",filename,false);
	xhttp.send();
	return xhttp.responseXML;
}

// setup and print the wadl document ( in html form)
function setup_wadl_print(){
	html_wadl_string = '';
	strapped_html_wadl_string = '';
	xml_wadl_string = '';
	spaces = 0;
	init_element_ids(rootNode, "");

	parse_wadl_html(rootNode);

	$("#wadlOutput").html(html_wadl_string);
	initBootstrapJS_tooltips();
}

// initializing bootstrap js tooltips
// For performance reasons, the Tooltip and Popover data-apis are
// opt-in, meaning you must initialize them yourself.
function initBootstrapJS_tooltips(){
	jQuery('[data-toggle=tooltip]').tooltip();
}

var margin_left = 0;
var padding_left = 0;
var level = 1;

function appendToHTML(side, str, elemClassName){
	$("#" + sideToWriteTo + "_wadl_output").append(str);
	lineNumber++;
	if (side == "left"){
		$("." + elemClassName).attr("data-lineNumber", lineNumber + "_a");
	} else if (side == "right"){
		$("." + elemClassName).attr("data-lineNumber", lineNumber + "_b");
	}	
}

var spanElem = "";
// parses the wadl/xml document recursively and print it out
function parse_wadl_html(myNode){
	addSpaces();

	// TO DO: optimize these prints, they are everywhere and seem too random;
	// ALSO: the names are not specific enough, such as html_wadl_string, spanElem.. there needs to be a logic name for each
	// ALSO: rename functions ( such as parse_wadl_html ); create separate classes that parse/print wadls
	// etc..
	// the cross service comparison highlighting part is a big module itself, so it should be separated
	// ALSO: separate the normal comparison from the cross service comparison; the latter depends on the first.

	// if 'myNode' has any child nodes, then it can be expanded/minimized
	printMinimizeMaximizeBtn(myNode);

	html_wadl_string += "<span id='" + myNode.my_id + "'>";
	html_wadl_string += "<font color='#008080'>" + htmlentities("<" + myNode.nodeName) + "</font>";

	strapped_html_wadl_string += "<div id='" + myNode.my_id + "_all'>";
	var marginMinus = -level*10;
	var marginLeftCalc = 10;

	padding_left = level * 13;

	strapped_html_wadl_string += "<div style=\"margin-left: 10px;\" id='" + myNode.my_id + "'>";
	strapped_html_wadl_string += "<span id='" + myNode.my_id + "line'>";
	strapped_html_wadl_string += "<font color='#008080'>" + htmlentities("<" + myNode.nodeName) + "</font>";

	spanElem = "";
	var elemClassName = myNode.my_id + "line";

	spanElem += "<div data-lineNumber=\"" + lineNumber + "\" style=\"padding-left: " + padding_left + "px;\" id='" + myNode.my_id + "line' class='" + elemClassName + "' >";
	spanElem += "<font color='#008080'>" + htmlentities("<" + myNode.nodeName) + "</font>";
	xml_wadl_string += "<" + myNode.nodeName;
	
	printAttributes(myNode);

	xml_wadl_string += ">";
	xml_wadl_string += "\n";

	strapped_html_wadl_string += "<font color='#008080'>" + htmlentities(">") + "</font>";
	strapped_html_wadl_string += "</span>";

	spanElem += "<font color='#008080'>" + htmlentities(">") + "</font>";
	spanElem += "</div>";
	appendToHTML("left", spanElem, elemClassName);

	var removeNodeBtn = "<button style=\"margin-left: 10px;\" type=\"button\"" +
						"class=\"btn btn-danger btn-xs\" onmouseout=\"unhighlightRemovingDiv('" + myNode.my_id + "');\""+ 
						"onmouseover=\"highlightRemovingDiv('" + myNode.my_id + "');\"" +
						"onClick=\"removeNode('" + myNode.my_id + "')\">X</button>";

	html_wadl_string += "<font color='#008080'>" + htmlentities(">") + "</font>";
	html_wadl_string += removeNodeBtn;

	if (myNode.nodeName === "xs:element"){
		var hintTypeBtnTooltip;
		if (myNode.convertedToEnumType){
			hintTypeBtnTooltip = 	"<button type='button' class=\"convertToEnum btn btn-default btn-xs\" data-toggle='tooltip' " + 
									"onClick='reverseEnumToNormalTypeHandler(" + myNode.my_id + ", this)' data-placement='right'" +
									" title='Reset to its original type'>Reset Type</button>";
		} else {
			hintTypeBtnTooltip =	"<button type='button' class=\"convertToEnum btn btn-default btn-xs\" data-toggle='tooltip' " + 
									"onClick='createEnumSimpleTypeHandler(" + myNode.my_id + ", this)' data-placement='right'" +
									" title='Click to convert to an Enumeration type'>Enum?</button>";
		}
		
		var typeConfidenceMap = calculateTypeConfidence(myNode);
		var typeConfStr  	  = "";
		//var chosenKey;
		var max_percent		  = 0;
		var typeConfidenceColor;
		for (var key in typeConfidenceMap){
			if (typeConfidenceMap.hasOwnProperty(key)){
				typeConfStr += key + "  " + typeConfidenceMap[key] + "%";
				if (typeConfidenceMap[key] > max_percent){
					max_percent = typeConfidenceMap[key];
					//chosenKey 	= key;
					if ( (max_percent > 0) && (max_percent <= 24) ){
						typeConfidenceColor = typeConfidence0_24;
					} else if ( (max_percent > 24) && (max_percent <= 49) ){
						typeConfidenceColor = typeConfidence24_49;
					} else if ( (max_percent > 49) && (max_percent <= 74) ){
						typeConfidenceColor = typeConfidence50_74;
					} else if ( (max_percent > 74) && (max_percent <= 100) ){
						typeConfidenceColor = typeConfidence75_100;
					}
				}
			}
		}

		// have to check if myNode has childNode 'typeFrequency' ( if it doesn't, don't display );
		// calculate typefrequency from it !
		var typeConfidenceBtnTooltip = "<button style='margin-left: 5px; width: 15px; height: 15px; " + 
									"background-color: " + typeConfidenceColor + "; " +
									"border-radius: 15px;' type='button' class=\"btn btn-xs " + typeConfidenceColor + "\" " +
									"data-toggle='tooltip' data-placement='right'" +
									" title='" + typeConfStr + "'></button>";

		html_wadl_string += hintTypeBtnTooltip;
		html_wadl_string += typeConfidenceBtnTooltip;
	}

	html_wadl_string += "</br>";
	
	printChildren(myNode);

	printAddElementButtons(myNode);

	elemClassName = myNode.my_id + "line_end";
	spanElem = "";
	spanElem += "<div data-lineNumber=\"" + '15' + "\" style=\"padding-left: " + padding_left + "px\" id='" + myNode.my_id + "line_end' class='" + elemClassName + "'>";
	spanElem += "<font color='#008080'>" + htmlentities("</" + myNode.nodeName + ">") + "</font>";
	spanElem += "</div>";

	appendToHTML("left", spanElem, elemClassName);

	// print the end element of myNode
	addSpaces();
	level--;
	padding_left = level * 13;
	
	xml_wadl_string += "</" + myNode.nodeName + ">";
	xml_wadl_string += "\n";
	
	strapped_html_wadl_string += "<span id='" + myNode.my_id + "line'>";
	strapped_html_wadl_string += "<font color='#008080'>" + htmlentities("</" + myNode.nodeName + ">") + "</font>";
	strapped_html_wadl_string += "</span>";
	strapped_html_wadl_string += "</div>";
	strapped_html_wadl_string += "</div>";

	html_wadl_string += "<font color='#008080'>" + htmlentities("</" + myNode.nodeName + ">") + "</font>";
	html_wadl_string += "</span>";
	html_wadl_string += "</br>";
	
	// decrease spaces as the recursion is going down in the element hierarchy
	spaces -= 4;
}

function calculateTypeConfidence(myNode){
	var totalFrequency = 0;
	var typeConfidenceArray = {};
	
	var typeFrequenciesNode = getChildNodeNamed(myNode, 'typeFrequencies');
	if (typeFrequenciesNode){
		if (typeFrequenciesNode.childNodes.length > 0){
			// calculating the sum of the frequencies / TO DO: will get this value from the <typeFrequencies> after it's added there
			for (var i = 0; i < typeFrequenciesNode.childNodes.length; i++){
				totalFrequency += typeFrequenciesNode.childNodes[i].attributes.getNamedItem('frequency').value;
			}
			for (var i = 0; i < typeFrequenciesNode.childNodes.length; i++){
				var typeValue  	 = typeFrequenciesNode.childNodes[i].attributes.getNamedItem('type').value;
				var freqValue	 = typeFrequenciesNode.childNodes[i].attributes.getNamedItem('frequency').value;
				var percentValue = (freqValue / totalFrequency) * 100;
				typeConfidenceArray[typeValue] = percentValue;
				totalFrequency += typeFrequenciesNode.childNodes[i].attributes.getNamedItem('frequency').value;
			}
		}
	} else {
		// this (xs:element) node most likely has no child nodes
	}
	return typeConfidenceArray;
}

// utility function
// gets a child node named 'childNodeName' node given a node 'myNode'
function getChildNodeNamed(myNode, childNodeName){
	for (var i = 0; i < myNode.childNodes.length; i++){
		if (myNode.childNodes[i].nodeName == childNodeName){
			return myNode.childNodes[i];
		}
	}
}

// expand/minimize buttons for showing/hiding a node's children
function printMinimizeMaximizeBtn(myNode){
	// if 'myNode' has any child nodes, then it can be expanded/minimized; otherwise, it can't
	// SPECIAL CASE: don't print any child of xs:element
	if ( (myNode.childNodes.length > 0) && (myNode.nodeName !== "xs:element") ){
		// if the node is not minimized, add an onClick 'hideNode' action; else, add an onClick 'showNode' action
		if (myNode.minimized){
			// remove element button
			var showNodesChildrenBtn = 	"<button type=\"button\" class=\"btn btn-default btn-xs\" " + 
										"onClick=\"showNodesChildren('" + myNode.my_id + "')\">+</button>";
			html_wadl_string += showNodesChildrenBtn;
			//html_wadl_string += "<button onClick=\"showNodesChildren('" + myNode.my_id + "')\">+</button>";			
		} else {
			// remove element button
			var hideNodesChildrenBtn = 	"<button type=\"button\" class=\"btn btn-default btn-xs\" " +
										"onClick=\"hideNodesChildren('" + myNode.my_id + "')\">-</button>";
			html_wadl_string += hideNodesChildrenBtn;
			//html_wadl_string += "<button onClick=\"hideNodesChildren('" + myNode.my_id + "')\">-</button>";			// remove element button
		}
	}
}

var reverseEnumToNormalTypeHandler = function(myNode_id, btn){
	var foundNode = find_node(rootNode, myNode_id);

	// useful for determining the type of onclick eventlistener is defined for node 'foundNode'
	foundNode.convertedToEnumType = false;

	for (var i = 0; i < enumConvertedArray.length; i++){
		// console.debug("enumConvertedArray: " + enumConvertedArray[i].old_xselement.attributes.getNamedItem('type').value);
		if (foundNode == enumConvertedArray[i].new_xselement){
			var parentOfFoundNode = foundNode.parentNode;

			// Option #1 start: inserting element at the end of the list
			// parentOfFoundNode.appendChild(enumConvertedArray[i].old_xselement);
			// Option #1 end

			// Option #2 start: inserting element at the same position where it was originally
			var indexToInsertAt = enumConvertedArray[i].elem_position;
			for (var j = 0; j < parentOfFoundNode.childNodes.length; j++){
				if (indexToInsertAt == j){
					parentOfFoundNode.insertBefore(enumConvertedArray[i].old_xselement, parentOfFoundNode.childNodes[j]);
				}
			}
			// Option #2 end
			parentOfFoundNode.removeChild(foundNode);
			var schemaNode = rootNode.childNodes[0].childNodes[0];
			schemaNode.removeChild(enumConvertedArray[i].new_simpletype);
			enumConvertedArray.splice(i, 1);
		}
	}

	setup_wadl_print();
}

// other possible name: createSimpleTypeForXSElement
var createEnumSimpleTypeHandler = function(myNode_id, btn){
	console.debug("creating simple type ! node name: " + myNode_id);
	//var foundNode  = find_node(rootNode, myNode_id);
	var foundNode2 = find_node_2(rootNode, myNode_id);
	
	console.debug("node name value: " + foundNode2['node'].attributes[0].value + " === " + 
				   foundNode2['node'].attributes.getNamedItem('name').value );
	var attribute_name_val = foundNode2['node'].attributes.getNamedItem('name').value;
	var attribute_type_val = foundNode2['node'].attributes.getNamedItem('type').value;
	var oldNode = foundNode2['node'].cloneNode(true);

	// changes the attribute 'type' of the node
	foundNode2['node'].attributes.getNamedItem("type").value = "tns:" + attribute_name_val + "Type";
	// useful for determining the type of onclick eventlistener is defined for node 'foundNode2['node']'
	foundNode2['node'].convertedToEnumType = true;

	// creating the xs:simpleType element
	var simpleTypeNode = xmlDoc.createElement("xs:simpleType");
	var nameAttr = xmlDoc.createAttribute("name");
	nameAttr.value = attribute_name_val + "Type";
	simpleTypeNode.setAttributeNode(nameAttr);

	var restrictionTypeNode = xmlDoc.createElement("xs:restriction");
	var baseAttr = xmlDoc.createAttribute("base");
	baseAttr.value = attribute_type_val;
	restrictionTypeNode.setAttributeNode(baseAttr);
	
	for (var i = 0; i < foundNode2['node'].childNodes.length; i++){
		// if the found element has a childNode 'valueFrequencies'
		if (foundNode2['node'].childNodes[i].nodeName === "valueFrequencies"){
			var valueFrequenciesNode = foundNode2['node'].childNodes[i];
			for (var j = 0; j < valueFrequenciesNode.childNodes.length; j++){
				var valFreqNode = valueFrequenciesNode.childNodes[j];
				var enumerationNode = xmlDoc.createElement("xs:enumeration");
				var enumValueAttr = xmlDoc.createAttribute("value");
				enumValueAttr.value = valFreqNode.attributes.getNamedItem('value').value;
				enumerationNode.setAttributeNode(enumValueAttr);
				enumerationNode.appendChild(valFreqNode);
				restrictionTypeNode.appendChild(enumerationNode);
			}
			foundNode2['node'].removeChild(valueFrequenciesNode);
		}
		// if the found element has a childNode 'typeFrequencies'
		if (foundNode2['node'].childNodes[i].nodeName === "typeFrequencies"){
			var typeFrequenciesNode = foundNode2['node'].childNodes[i];
			for (var j = 0; j < typeFrequenciesNode.childNodes.length; j++){
				var typeFreqNode = typeFrequenciesNode.childNodes[j];
				restrictionTypeNode.appendChild(typeFreqNode);
			}
			foundNode2['node'].removeChild(typeFrequenciesNode);
		}
	}

	simpleTypeNode.appendChild(restrictionTypeNode);
	
	var schemaNode = rootNode.childNodes[0].childNodes[0];
	schemaNode.appendChild(simpleTypeNode);

	// keep track of the elements that have been converted from their original 'type' to
	// the enum type.
	var obj = {};
	obj.elem_position	= foundNode2['position'];
	obj.old_xselement 	= oldNode;
	obj.new_xselement 	= foundNode2['node'];
	obj.new_simpletype 	= simpleTypeNode;
	enumConvertedArray.push(obj);

	setup_wadl_print();
}

function printChildren(myNode){
	// only print myNode's children nodes if myNode is not minimized
	// SPECIAL CASE: don't print any child of xs:element
	//if ( (!myNode.minimized) ){
	if ( (!myNode.minimized) && (myNode.nodeName !== "xs:element") ){
		
		// print myNode's children nodes
		for (var i = 0; i < myNode.childNodes.length; i++){
			spaces += 4;
			margin_left += 10;
			level++;
			parse_wadl_html(myNode.childNodes[i]);
		}
	}
}

function printAttributes(myNode){
	// print myNode's attributes
	for (var i = 0; i < myNode.attributes.length; i++){
		// SPECIAL CASE: don't print attribute 'hasVariableID' 
		if (myNode.attributes[i].name !== "hasVariableID"){
			if (wadl_attribute_edit_mode){
				html_wadl_string += 		 "<font color='#7B277C'>" + htmlentities(" " + myNode.attributes[i].name ) + "</font>" + "=" + "<font color='#4152A3'><i>" + "\"" + "<input type='text' onchange=\"updateAttribute('" + myNode.my_id + "', '" + myNode.attributes[i].nodeName + "', this.value );\" value=\"" + htmlentities( myNode.attributes[i].value ) + "\"></input>" + "\"" + "</i></font>";
				strapped_html_wadl_string += "<font color='#7B277C'>" + htmlentities(" " + myNode.attributes[i].name ) + "</font>" + "=" + "<font color='#4152A3'><i>" + "\"" + htmlentities( myNode.attributes[i].value ) + "\"" + "</i></font>";
				spanElem += "<font color='#7B277C'>" + htmlentities(" " + myNode.attributes[i].name ) + "</font>" + "=" + "<font color='#4152A3'><i>" + "\"" + htmlentities( myNode.attributes[i].value ) + "\"" + "</i></font>";
			} else {
				html_wadl_string += 		 "<font color='#7B277C'>" + htmlentities(" " + myNode.attributes[i].name ) + "</font>" + "=" + "<font color='#4152A3'><i>" + htmlentities("\"" + myNode.attributes[i].value + "\"" ) + "</i></font>";
				strapped_html_wadl_string += "<font color='#7B277C'>" + htmlentities(" " + myNode.attributes[i].name ) + "</font>" + "=" + "<font color='#4152A3'><i>" + htmlentities("\"" + myNode.attributes[i].value + "\"" ) + "</i></font>";
				spanElem += "<font color='#7B277C'>" + htmlentities(" " + myNode.attributes[i].name ) + "</font>" + "=" + "<font color='#4152A3'><i>" + htmlentities("\"" + myNode.attributes[i].value + "\"" ) + "</i></font>";
			}
			xml_wadl_string += " " + myNode.attributes[i].name + "=" + "\"" + myNode.attributes[i].value + "\"";
		}
	}
}

function printAddElementButtons(myNode){

	var addBtnsSize = "xs";			// 'xs' for extra small, 'sm' for small
	var addBtnsType = "default";	// 'default' for white, ..

	if (myNode.nodeName == 'resources'){
		addSpacesTwo();
		var addResourceBtn = "<button type=\"button\" class=\"btn btn-" + addBtnsType + " btn-" + addBtnsSize + "\" onClick=\"addResource('" + myNode.my_id + "')\">Add Resource</button>";
		html_wadl_string += addResourceBtn;
		html_wadl_string += "</br>";
		
		addSpacesTwo();
		var addMethodBtn = "<button type=\"button\" class=\"btn btn-" + addBtnsType + " btn-" + addBtnsSize + "\" onClick=\"addMethod('" + myNode.my_id + "')\">Add Method</button>";
		html_wadl_string += addMethodBtn;
		html_wadl_string += "</br>";

		addSpacesTwo();
		var addParamBtn = "<button type=\"button\" class=\"btn btn-" + addBtnsType + " btn-" + addBtnsSize + "\" onClick=\"addParam('" + myNode.my_id + "')\">Add Param</button>";
		html_wadl_string += addParamBtn;
		html_wadl_string += "</br>";
	} else if (myNode.nodeName == 'resource'){
		addSpacesTwo();
		var addResourceBtn = "<button type=\"button\" class=\"btn btn-" + addBtnsType + " btn-" + addBtnsSize + "\" onClick=\"addResource('" + myNode.my_id + "')\">Add Resource</button>";
		html_wadl_string += addResourceBtn;
		html_wadl_string += "</br>";
		
		addSpacesTwo();
		var addMethodBtn = "<button type=\"button\" class=\"btn btn-" + addBtnsType + " btn-" + addBtnsSize + "\" onClick=\"addMethod('" + myNode.my_id + "')\">Add Method</button>";
		html_wadl_string += addMethodBtn;
		html_wadl_string += "</br>";
		
		addSpacesTwo();
		var addParamBtn = "<button type=\"button\" class=\"btn btn-" + addBtnsType + " btn-" + addBtnsSize + "\" onClick=\"addParam('" + myNode.my_id + "')\">Add Param</button>";
		html_wadl_string += addParamBtn;
		html_wadl_string += "</br>";
	} else if (myNode.nodeName == 'method'){
		// a method element can only have one request and one response element
		var hasRequest = false;
		var hasResponse = false;
		for (var j = 0; j < myNode.childNodes.length; j++){
			if (myNode.childNodes[j].nodeName == 'request'){
				hasRequest = true;
			} else if (myNode.childNodes[j].nodeName == 'response'){
				hasResponse = true;
			}
		}
		
		if (!hasRequest){
			addSpacesTwo();
			var addRequestBtn = "<button type=\"button\" class=\"btn btn-" + addBtnsType + " btn-" + addBtnsSize + "\" onClick=\"addRequest('" + myNode.my_id + "')\">Add Request</button>";
			html_wadl_string += addRequestBtn;
			html_wadl_string += "</br>";
		}

		if (!hasResponse){
			addSpacesTwo();
			var addResponseBtn = "<button type=\"button\" class=\"btn btn-" + addBtnsType + " btn-" + addBtnsSize + "\" onClick=\"addResponse('" + myNode.my_id + "')\">Add Response</button>";
			html_wadl_string += addResponseBtn;
			html_wadl_string += "</br>";
		}
	} else if (myNode.nodeName == 'request'){
		// a request element can only have one representation element
		var hasRepresentation = false;
		for (var j = 0; j < myNode.childNodes.length; j++){
			if (myNode.childNodes[j].nodeName == 'representation'){
				hasRepresentation = true;
			}
		}

		if (!hasRepresentation){
			addSpacesTwo();
			var addRepresentationBtn = "<button type=\"button\" class=\"btn btn-" + addBtnsType + " btn-" + addBtnsSize + "\" onClick=\"addRepresentation('" + myNode.my_id + "')\">Add Representation</button>";
			html_wadl_string += addRepresentationBtn;
			html_wadl_string += "</br>";
		}

		addSpacesTwo();
		var addParamBtn = "<button type=\"button\" class=\"btn btn-" + addBtnsType + " btn-" + addBtnsSize + "\" onClick=\"addParam('" + myNode.my_id + "')\">Add Param</button>";
		html_wadl_string += addParamBtn;
		html_wadl_string += "</br>";
	} else if (myNode.nodeName == 'response'){
		// a response element can only have one representation element
		var hasRepresentation = false;
		for (var j = 0; j < myNode.childNodes.length; j++){
			if (myNode.childNodes[j].nodeName == 'representation'){
				hasRepresentation = true;
			}
		}

		if (!hasRepresentation){
			addSpacesTwo();
			var addRepresentationBtn = "<button type=\"button\" class=\"btn btn-" + addBtnsType + " btn-" + addBtnsSize + "\" onClick=\"addRepresentation('" + myNode.my_id + "')\">Add Representation</button>";
			html_wadl_string += addRepresentationBtn;
			html_wadl_string += "</br>";
		}

		addSpacesTwo();
		var addParamBtn = "<button type=\"button\" class=\"btn btn-" + addBtnsType + " btn-" + addBtnsSize + "\" onClick=\"addParam('" + myNode.my_id + "')\">Add Param</button>";
		html_wadl_string += addParamBtn;
		html_wadl_string += "</br>";

		addSpacesTwo();
		var addFaultBtn = "<button type=\"button\" class=\"btn btn-" + addBtnsType + " btn-" + addBtnsSize + "\" onClick=\"addFault('" + myNode.my_id + "')\">Add Fault</button>";
		html_wadl_string += addFaultBtn;
		html_wadl_string += "</br>";
	}
}

function runSampleTest(process){
	console.debug("running sample test");
	if (process === 'analyze'){
		$('#urlInputA' + '_'+"0").val("https://api.github.com/users");
		analyzeBtn();
	} else if (process === 'compare'){
		$('#urlInputA' + '_'+"0").val("https://api.github.com/users/penguinsource/repos");
		$('#urlInputB' + '_'+"0").val("https://api.github.com/users/fokaefs/repos");
		compareBtn();
	} else if (process === 'crossServiceCompare'){
		$('#urlInputA' + '_'+"0").val("https://graph.facebook.com/oprescu3");
		$('#urlInputB' + '_'+"0").val("https://graph.facebook.com/jack");
		crossServiceCompareBtn();
	}
}