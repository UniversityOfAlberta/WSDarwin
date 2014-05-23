//import "ObjTree.js";
//import "jsdiff.js";

var noURLFields = 0;
var noCompareURLFields = 0;
var uppedWADLurls = [];
var compareWADLurls = [];

//wadl html file
var wadl_attribute_edit_mode = true;	// DEBUG (only applicable to 'analyze', not 'compare')
var add_elements_mode = true;			// DEBUG (only applicable to 'analyze', not 'compare')

var session_id = '';	// initially set in the java app
var html_wadl_string = '';
var strapped_html_wadl_string = '';
var xml_wadl_string = '';

var oldCompare = '';
var newCompare = '';

var spaces = 0;
var rootNode;
var node_id = 0;
var xmlDoc;

function addURLField(){
	var fullDiv = "	<div class='singleUrlDiv' id='singleURLdiv_" + noURLFields + "'>"+
				  "	<select id='requestType1'>"+
				  "	<option value='get' selected>GET</option>"+
						"<option value='post'>POST</option>"+
						"<option value='put'>PUT</option>"+
						"<option value='delete'>DELETE</option>"+
						"<option value='head'>HEAD</option>"+
					"</select>"+
					"<input type='text' name='lname' id='urlInput_" + noURLFields + "' class='urlInput'>"+
					//"<button id='analyzeSingleURL1' class='analyzeSingleURL' onClick=\"analyzeSingleURL('" + noURLFields + "')\"> Analyze </button>"+
					"<button onClick=\"removeURLField('" + noURLFields + "')\">Remove</button>"+
					"</div>";
	$('#fieldUrlDiv').append(fullDiv);
	noURLFields++;
}

function addCompareURLField(){
	//alert("ss");
	var fullDiv = "	<div class='singleUrlDiv' id='singleCompareUrlDiv" + noCompareURLFields + "'>"+
				  "	<select id='requestType1'>"+
				  "	<option value='get' selected>GET</option>"+
						"<option value='post'>POST</option>"+
						"<option value='put'>PUT</option>"+
						"<option value='delete'>DELETE</option>"+
						"<option value='head'>HEAD</option>"+
					"</select>"+
					"<input type='text' name='comparelname' id='urlCompareInput_" + noCompareURLFields + "' class='urlInput2'>"+
					//"<button id='analyzeSingleURL1' class='analyzeSingleURL' onClick=\"analyzeSingleURL('" + noCompareURLFields + "')\"> Analyze </button>"+
					"<button onClick=\"removeURLField('" + noCompareURLFields + "')\">Remove</button>"+
					"</div>";
	$('#compareDiv').append(fullDiv);
	noCompareURLFields++;
}

function var_dump(obj) {
    var out = '';
    for (var i in obj) {
        out += i + ": " + obj[i] + "\n";
    }
    console.log(out);
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

var firstWADL = "";
var secondWADL = "";

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
    //diffoutputdiv.appendChild(
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

    //downloadWADL();
}

function saveWADLtoFile(){
    console.log("saving wadl file");
    $.ajax({
    	url: "/wsdarwin/funcsPHP/downloadWADLfile.php",
        type: "POST",
        data: { wadlString: xml_wadl_string },
        dataType: "html",
        success: function(response) {
        	//console.log("RESPONSE: " + response);
        	//$("#wadlOutput").append("<a href=\"http://pokemonpacific.com/wsdarwin/funcsPHP/wadlFile.wadl\">Download the wadl file</a>");
        },
        error: function(xhr, status, error) {
		  console.log("Error acccesing downloadWADLfile.");
		}

    });
}

function downloadWADL(){
	saveWADLtoFile();
	window.open("http://pokemonpacific.com/wsdarwin/funcsPHP/wadlFile.wadl", '_blank', 'download');
	//window.location = "http://pokemonpacific.com/wsdarwin/funcsPHP/wadlFile.wadl";
}

function removeURLField(id){
	$('#singleURLdiv_'+id).remove();
	//reassignIDs();
}

/*function reassignIDs(){
	var idcount = 0;
	// loop through all divs
	$('div.singleUrlDiv').each(function(index) {
	    // set div id to array value
	    $('div').attr('id', "singleURLdiv_" + idcount);
	    idcount++;

	});

	noURLFields = idcount;
}*/

// utility funcs
function htmlentities(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

function removeNode(mynodeid){
	var foundNode = find_node(rootNode, mynodeid);
	//console.log('found node name: ' + foundNode.nodeName + ', parent: ' + foundNode.parentNode.nodeName);
	foundNode.parentNode.removeChild(foundNode);
	setup_wadl_print();
}

function hideNodesChildren(mynodeid){
	console.log("HIIIIIIIIIIDING");
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
	//methodNode.setAttribute("id","");
	methodNode.setAttribute("name","");

	foundNode.appendChild(methodNode);
	//document.getElementById("popupDiv").style.visibility="visible";

	setup_wadl_print();
}

function addParam(mynodeid){
	var foundNode = find_node(rootNode, mynodeid);
	var newNode=xmlDoc.createElement("param");
	newNode.setAttribute("path","");
	foundNode.appendChild(newNode);
	//document.getElementById("popupDiv").style.visibility="visible";

	setup_wadl_print();
}

function addRequest(mynodeid){
	var foundNode = find_node(rootNode, mynodeid);
	var newNode=xmlDoc.createElement("request");
	foundNode.appendChild(newNode);
	//document.getElementById("popupDiv").style.visibility="visible";

	setup_wadl_print();
}

function addResponse(mynodeid){
	var foundNode = find_node(rootNode, mynodeid);
	var newNode=xmlDoc.createElement("response");
	newNode.setAttribute("status","");
	foundNode.appendChild(newNode);
	//document.getElementById("popupDiv").style.visibility="visible";

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
	//alert('node attribute: ' + nodeAttrName + ", value: " + newAttrValue );
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
		//console.log('FINDING NODES: ' + mynode.childNodes[i].nodeName + ", id: " + mynode.childNodes[i].my_id);
		if (mynode.childNodes[i].my_id == sid){
			window.foundNode = mynode.childNodes[i];		// window.foundNode global
			console.log('! name is ' + window.foundNode.nodeName);
			return window.foundNode;
		}
		find_node(mynode.childNodes[i], sid);
	}
	return window.foundNode;
}

function init_node(myNode){
	//node_id = 0;
	init_elements(myNode);
}

function init_elements(myNode){
	myNode.minimized = false;
	myNode.my_id = node_id;
	node_id++;
	for (var i = 0; i < myNode.childNodes.length; i++){
		init_elements(myNode.childNodes[i]);
	}
}

function highlightDiv(divid){
	//alert('hello world !');
	//$("#"+divid).css('background-color', 'red');
	$("#"+divid).css('text-decoration', 'line-through');
}

function unhighlight(divid){
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

function compareBtn(){
	analyze("compare");
}

function java_diff(deltas_path, oldDocNode, newDocNode){
	xmlDoc = loadXMLDoc(deltas_path);
	mainNode=xmlDoc.documentElement;

		console.log("old doc: " + oldDocNode.childNodes[1].childNodes[0].attributes.getNamedItem("path").value);
	console.log("new doc: " + newDocNode.childNodes[1].childNodes[0].attributes.getNamedItem("path").value);

	console.log(" --- JAVA DIFFING --- ");
	//console.log("name: " + mainNode.nodeName + ", attrs length: " + mainNode.childNodes.length + ", attribute's value: " + mainNode.attributes.getNamedItem("xmlns").value);
	//console.log(xmlDoc);

	strapped_html_wadl_string = '';
	console.log("old doc node: ");
	parse_wadl_html(oldDocNode);
	var oldText = strapped_html_wadl_string;
	strapped_html_wadl_string = '';
	console.log("new doc node: ");
	parse_wadl_html(newDocNode);
	var newText = strapped_html_wadl_string;
	strapped_html_wadl_string = '';

	$("#wadlOutput").hide();
	$("#left_wadl_output").show();
	$("#right_wadl_output").show();

	$("#left_wadl_output").html(oldText);
	$("#right_wadl_output").html(newText);

	for (var i = 0; i < mainNode.childNodes.length; i++){
		iserviceNode = mainNode.childNodes[i];
		if (iserviceNode.nodeName == "MatchDelta"){
			console.log("Skipping iservice:" + iserviceNode.nodeName);
			continue;
		}
		console.log("iservice: " + mainNode.childNodes[i].nodeName);
		console.log("node: " + mainNode.childNodes[i]);
		//console.log("type: " + mainNode.childNodes[i].attributes.getNamedItem("type").value);
		for (var k = 0; k < iserviceNode.childNodes.length; k++){
			interfaceNode = iserviceNode.childNodes[k];
			if (interfaceNode.nodeName == "MatchDelta"){
				console.log("Skipping interface:" + interfaceNode.nodeName);
				continue;
			} else if (interfaceNode.nodeName == "ChangeDelta"){
				console.log("Change Delta interface:" + interfaceNode.nodeName);
				if (interfaceNode.attributes.getNamedItem("changedAttribute") != null){
					if (interfaceNode.attributes.getNamedItem("changedAttribute").value === "address"){
						//var newAddress = interfaceNode.attributes.getNamedItem("newValue").value;
						//var oldAddress = interfaceNode.attributes.getNamedItem("oldValue").value;
						var sourceSplits = interfaceNode.attributes.getNamedItem("source").value.split("\\");
						var targetSplits = interfaceNode.attributes.getNamedItem("target").value.split("\\");
						//console.log("splits: " + sourceSplits);
						//console.log("splits length: " + sourceSplits.length);
						//console.log("last of splits: " + sourceSplits[splits.length - 1]);
						console.log("old doc node:");
						highlightResourceNodeDiv(sourceSplits[sourceSplits.length - 1], oldDocNode, 'ChangeDelta');
						console.log("new doc node:");
						highlightResourceNodeDiv(targetSplits[targetSplits.length - 1], newDocNode, 'ChangeDelta');
						// code to add changed address for a resource
					}
				}
			} else if (interfaceNode.nodeName == "AddDelta"){
				var targetSplits = interfaceNode.attributes.getNamedItem("target").value.split("\\");
				highlightResourceNodeDiv(targetSplits[targetSplits.length - 1], newDocNode, 'AddDelta');
			} else if (interfaceNode.nodeName == "DeleteDelta"){
				var sourceSplits = interfaceNode.attributes.getNamedItem("source").value.split("\\");
				highlightResourceNodeDiv(sourceSplits[sourceSplits.length - 1], oldDocNode, 'DeleteDelta');
			}


			for (var j = 0; j < interfaceNode.childNodes.length; j++){
				operationNode = interfaceNode.childNodes[j];
				if (operationNode.nodeName == "MatchDelta"){
					console.log("Skipping operation:" + operationNode.nodeName);
					continue;
				} else if (operationNode.nodeName == "ChangeDelta"){
					if (operationNode.attributes.getNamedItem("changedAttribute") != null){
						if (operationNode.attributes.getNamedItem("changedAttribute").value === "WHAT SHOULD GO HERE !*****"){ // <-------****
							//var newAddress = interfaceNode.attributes.getNamedItem("newValue").value;
							//var oldAddress = interfaceNode.attributes.getNamedItem("oldValue").value;
							//var sourceSplits = interfaceNode.attributes.getNamedItem("source").value.split("\\");
							//var targetSplits = interfaceNode.attributes.getNamedItem("target").value.split("\\");
							//console.log("splits: " + sourceSplits);
							//console.log("splits length: " + sourceSplits.length);
							//console.log("last of splits: " + sourceSplits[splits.length - 1]);
							//var source = interfaceNode.attributes.getNamedItem("target").value;
							//highlightMethodNodeDiv(targetVal, newDocNode, 'AddDelta');
							// code to add changed address for a resource
						}
					}
				} else if (operationNode.nodeName == "AddDelta"){

					var targetVal = operationNode.attributes.getNamedItem("target").value;
					console.log("hello thereee !!!!!!!!!!!!!!!!! " + targetVal);
					highlightMethodNodeDiv(targetVal, newDocNode, 'AddDelta');
				} else if (operationNode.nodeName == "DeleteDelta"){
					var sourceVal = operationNode.attributes.getNamedItem("source").value;
					highlightMethodNodeDiv(sourceVal, oldDocNode, 'DeleteDelta');
				}
				//console.log("operation node source: " + operationNode.attributes.getNamedItem("source").value);
			}
		}
	}

	console.log(" --- END OF JAVA DIFFING --- ");
}

function highlightResourceNodeDiv(resourcePath, xml_doc, type){
	var highlightColor = '';
	if (type == "AddDelta"){
		highlightColor = "green";
	} else if (type == "DeleteDelta"){
		highlightColor = "red";
	} else if (type == "ChangeDelta"){
		highlightColor = "yellow";
	}

	if ( (xml_doc.childNodes[1].nodeName != null) && (xml_doc.childNodes[1].nodeName == "resources") ){
		var resourcesNode = xml_doc.childNodes[1];
		for (var i = 0; i < resourcesNode.childNodes.length; i++){
			var resource_Node = resourcesNode.childNodes[i];
			if (resource_Node.attributes.getNamedItem("path").value == resourcePath){
				$("#"+resource_Node.my_id).css("background-color", highlightColor);
			}
		}
	}
}

function highlightMethodNodeDiv(method_id, xml_doc, type){
	var highlightColor = '';
	if (type == "AddDelta"){
		highlightColor = "green";
	} else if (type == "DeleteDelta"){
		highlightColor = "red";
	} else if (type == "ChangeDelta"){
		highlightColor = "yellow";
	}

	if ( (xml_doc.childNodes[1].nodeName != null) && (xml_doc.childNodes[1].nodeName == "resources") ){
		var resourcesNode = xml_doc.childNodes[1];
		for (var i = 0; i < resourcesNode.childNodes.length; i++){
			var resource_Node = resourcesNode.childNodes[i];
			for (var j = 0; j < resource_Node.childNodes.length; j++){
				var methodNode = resource_Node.childNodes[i];
				console.log("Method node id val is: " + methodNode.attributes.getNamedItem("id").value);
				if (methodNode.attributes.getNamedItem("id").value == method_id){
					$("#"+methodNode.my_id).css("background-color", highlightColor);
				}
			}
		}
	}
}

//function changeResource(oldValue, newValue){	// Resource === Interface in WSMeta Model

//}
function analyze(process_mode){
	// reset
	$("#left_wadl_output").hide();
	$("#right_wadl_output").hide();
	$("#wadlOutput").show();
	$("#wadlOutput").html('');

	var analyze_urls_array = [];
	var compare_urls_array = [];

	// JSON urls to analyze
	$('input.urlInput').each(function(index) {
		analyze_urls_array.push( $('#urlInput_'+index).val() );
	});
	// JSON urls to compare
	if (process_mode === 'compare'){
		$('input.urlInput2').each(function(index) {
			compare_urls_array.push( $('#urlCompareInput_'+index).val() );
			//console.log("compare urls: " + $('#urlCompareInput_'+index).val() );
		});
	}
	
	var analyze_json = JSON.stringify(analyze_urls_array);
	var compare_json = JSON.stringify(compare_urls_array);

	// WADL files uploaded..
	var analyzed_wadls_URLs = [];
	var compare_wadls_URLs = [];

	for (var i = 0; i < uppedWADLurls.length; i++){
		analyzed_wadls_URLs.push(uppedWADLurls[i]);
		//console.log("link " + i + ": " + uppedWADLurls[i]);
	}

	// WADL files to compare
	if (process_mode === 'compare'){
		for (var i = 0; i < compareWADLurls.length; i++){
			compare_wadls_URLs.push(compareWADLurls[i]);
			//console.log("link " + i + ": " + compareWADLurls[i]);
		}
	}

	//analyzed_wadls_URLs.push("http://localhost:8080/wsdarwin_1.0.0/files/icsm2014/twitter/wadl/WADLresponse020.wadl");
	var jsonWadlURLs = JSON.stringify(analyzed_wadls_URLs);
	var jsonCompareWadlURLs = JSON.stringify(compare_wadls_URLs);

    $.ajax({
    	url: "http://localhost:8080/wsdarwin_1.0.0/jaxrs/api/analyze",
        type: "GET",
        data: { newURLs: analyze_json, newUppedFiles: jsonWadlURLs, sessionid: session_id, type: process_mode, compareURLs: compare_json, compareWADLfiles: jsonCompareWadlURLs },
        //parameters: { url:  },
        dataType: "html",
        crossDomain: true,	// sending ajax call to a jsp servlet, thus needing to enable this
        //data: { buyerid: buyerid_arg, txnid: txn_id},
        success: function(response) {
        	//alert('resp: ' + response);
        	var jsonObj = JSON.parse(response);

        	session_id = jsonObj[0];
        	var analysis_merged_wadl_url_path = jsonObj[1];
			var compare_merged_wadl_url_path = jsonObj[2];
			var delta_comparison_url = jsonObj[3];
			console.log("delta comparisons url: ");
			console.log(delta_comparison_url);

			console.log("merged #1_: " + analysis_merged_wadl_url_path);
			console.log("merged #2_: " + compare_merged_wadl_url_path);
			console.log("process_mode is " + process_mode);

			if (process_mode == "compare"){
				console.log("diffing the wadl's");
				add_elements_mode = false;

				getWADL(analysis_merged_wadl_url_path);
				firstWADL = xml_wadl_string;
				getWADL(compare_merged_wadl_url_path);
				secondWADL = xml_wadl_string;

				compareXMLDoc = loadXMLDoc(analysis_merged_wadl_url_path);
				var oldRootDoc = compareXMLDoc.documentElement;
				compareXMLDoc = loadXMLDoc(compare_merged_wadl_url_path);
				var newRootDoc = compareXMLDoc.documentElement;
				init_node(oldRootDoc);
				init_node(newRootDoc);

				java_diff(delta_comparison_url, oldRootDoc, newRootDoc);	// java comparison diff
				//text_diff_JS(1);											// text comparison diff
				
				saveWADLtoFile();
			} else if (process_mode == "analyze"){
				add_elements_mode = true;
				getWADL(analysis_merged_wadl_url_path);
				saveWADLtoFile();
			}
        },
        error: function(xhr, status, error) {
          //alert("status: " + status + ", xhr: " + xhr.responseText + ", error: " + error) ;
		  alert("Error accessing app. Please try again !");
		  //var err = eval("(" + xhr.responseText + ")");
		  //alert(err.Message);
		}

    });
}

function getWADL(wadl_url_path){
	xmlDoc = loadXMLDoc(wadl_url_path);
	rootNode=xmlDoc.documentElement;
	// nodeName, attributes, childNodes

	init_elements(rootNode);
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
	parse_wadl_html(rootNode);
	//parse_wadl_html_two(rootNode);
	$("#wadlOutput").html(html_wadl_string);
	//$("#wadlOutput").html(strapped_html_wadl_string);
}

// parses the wadl/xml document recursively and print it out
function parse_wadl_html(myNode){

	//var n = 0;
	addSpaces();
	
	// if 'myNode' has any child nodes, then it can be expanded/minimized; otherwise, it can't
	if (myNode.childNodes.length > 0){
		// if the node is not minimized, add an onClick 'hideNode' action; else, add an onClick 'showNode' action
		if (myNode.minimized){
			html_wadl_string += "<button onClick=\"showNodesChildren('" + myNode.my_id + "')\">+</button>";			// remove element button
		} else {
			html_wadl_string += "<button onClick=\"hideNodesChildren('" + myNode.my_id + "')\">-</button>";			// remove element button
		}
	}

	html_wadl_string += "<span id='" + myNode.my_id + "'>";
	html_wadl_string += "<font color='#008080'>" + htmlentities("<" + myNode.nodeName) + "</font>";

	strapped_html_wadl_string += "<div style=\"background-color: white; padding-left: 10px;\" id='" + myNode.my_id + "'>";
	strapped_html_wadl_string += "<font color='#008080'>" + htmlentities("<" + myNode.nodeName) + "</font>";
	//$("#"+myNode.my_id).css("margin-left") += '3px';
	//$("#"+myNode.my_id).css("margin-left", "35px");

	xml_wadl_string += "<" + myNode.nodeName;

	// print myNode's attributes
	for (var i = 0; i < myNode.attributes.length; i++){
		if (wadl_attribute_edit_mode){
			html_wadl_string += 		 "<font color='#7B277C'>" + htmlentities(" " + myNode.attributes[i].name ) + "</font>" + "=" + "<font color='#4152A3'><i>" + "\"" + "<input type='text' onchange=\"updateAttribute('" + myNode.my_id + "', '" + myNode.attributes[i].nodeName + "', this.value );\" value=\"" + htmlentities( myNode.attributes[i].value ) + "\"></input>" + "\"" + "</i></font>";
			strapped_html_wadl_string += "<font color='#7B277C'>" + htmlentities(" " + myNode.attributes[i].name ) + "</font>" + "=" + "<font color='#4152A3'><i>" + "\"" + htmlentities( myNode.attributes[i].value ) + "\"" + "</i></font>";
		} else {
			html_wadl_string += 		 "<font color='#7B277C'>" + htmlentities(" " + myNode.attributes[i].name ) + "</font>" + "=" + "<font color='#4152A3'><i>" + htmlentities("\"" + myNode.attributes[i].value + "\"" ) + "</i></font>";
			strapped_html_wadl_string += "<font color='#7B277C'>" + htmlentities(" " + myNode.attributes[i].name ) + "</font>" + "=" + "<font color='#4152A3'><i>" + htmlentities("\"" + myNode.attributes[i].value + "\"" ) + "</i></font>";
		}
		xml_wadl_string += " " + myNode.attributes[i].name + "=" + "\"" + myNode.attributes[i].value + "\"";
	}

	xml_wadl_string += ">";
	xml_wadl_string += "\n";
	//xml_wadl_string += "</br>";

	strapped_html_wadl_string += "<font color='#008080'>" + htmlentities(">") + "</font>";
	

	html_wadl_string += "<font color='#008080'>" + htmlentities(">") + "</font>";
	html_wadl_string += "<button onmouseout=\"unhighlight('" + myNode.my_id + "');\" onmouseover=\"highlightDiv('" + myNode.my_id + "');\" onClick=\"removeNode('" + myNode.my_id + "')\">X</button>";	// remove element button
	html_wadl_string += "</br>";
	
	// only print myNode's children nodes if myNode is not minimized
	if (!myNode.minimized){
		// print myNode's children nodes
		for (var i = 0; i < myNode.childNodes.length; i++){
			spaces += 4;
			parse_wadl_html(myNode.childNodes[i]);
		}
	}

	console.log("SUP 6");
	// printing the add 'element(s)' buttons
	if (myNode.nodeName == 'resources'){
		addSpacesTwo();
		html_wadl_string += "<button onClick=\"addResource('" + myNode.my_id + "')\">Add Resource</button>";		// remove element button
		html_wadl_string += "</br>";
		
		addSpacesTwo();
		html_wadl_string += "<button onClick=\"addMethod('" + myNode.my_id + "')\">Add Method</button>";			// remove element button
		html_wadl_string += "</br>";

		addSpacesTwo();
		html_wadl_string += "<button onClick=\"addParam('" + myNode.my_id + "')\">Add Param</button>";			// remove element button
		html_wadl_string += "</br>";
	} else if (myNode.nodeName == 'resource'){
		//console.log("RES PATH: " + myNode.attributes.getNamedItem("path").value);
		addSpacesTwo();
		html_wadl_string += "<button onClick=\"addResource('" + myNode.my_id + "')\">Add Resource</button>";		// remove element button
		html_wadl_string += "</br>";
		
		addSpacesTwo();
		html_wadl_string += "<button onClick=\"addMethod('" + myNode.my_id + "')\">Add Method</button>";			// remove element button
		html_wadl_string += "</br>";
		
		addSpacesTwo();
		html_wadl_string += "<button onClick=\"addParam('" + myNode.my_id + "')\">Add Param</button>";			// remove element button
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
			html_wadl_string += "<button onClick=\"addRequest('" + myNode.my_id + "')\">Add Request</button>";		// remove element button
			html_wadl_string += "</br>";
		}

		if (!hasResponse){
			addSpacesTwo();
			html_wadl_string += "<button onClick=\"addResponse('" + myNode.my_id + "')\">Add Response</button>";		// remove element button
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
			html_wadl_string += "<button onClick=\"addRepresentation('" + myNode.my_id + "')\">Add Representation</button>";		// remove element button
			html_wadl_string += "</br>";
		}

		addSpacesTwo();
		html_wadl_string += "<button onClick=\"addParam('" + myNode.my_id + "')\">Add Param</button>";		// remove element button
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
			html_wadl_string += "<button onClick=\"addRepresentation('" + myNode.my_id + "')\">Add Representation</button>";		// remove element button
			html_wadl_string += "</br>";
		}

		addSpacesTwo();
		html_wadl_string += "<button onClick=\"addParam('" + myNode.my_id + "')\">Add Param</button>";		// remove element button
		html_wadl_string += "</br>";

		addSpacesTwo();
		html_wadl_string += "<button onClick=\"addFault('" + myNode.my_id + "')\">Add Fault</button>";		// remove element button
		html_wadl_string += "</br>";
	}

	// print the end element of myNode
	addSpaces();

	xml_wadl_string += "</" + myNode.nodeName + ">";
	xml_wadl_string += "\n";
	//xml_wadl_string += "</br>";
	strapped_html_wadl_string += "<font color='#008080'>" + htmlentities("</" + myNode.nodeName + ">") + "</font>";
	strapped_html_wadl_string += "</div>";

	html_wadl_string += "<font color='#008080'>" + htmlentities("</" + myNode.nodeName + ">") + "</font>";
	html_wadl_string += "</span>";
	html_wadl_string += "</br>";

	// decrease spaces as the recursion is going down in the element hierarchy
	spaces -= 4;
}

/*
function parse_wadl_html_two(myNode){

	addSpaces();

	html_wadl_string += "<span id='" + myNode.my_id + "'>";
	html_wadl_string += "<font color='#008080'>" + htmlentities("<" + myNode.nodeName) + "</font>";
	
	strapped_html_wadl_string += "<" + myNode.nodeName;

	// print myNode's attributes
	for (var i = 0; i < myNode.attributes.length; i++){
		html_wadl_string += "<font color='#7B277C'>" + htmlentities(" " + myNode.attributes[i].name ) + "</font>" + "=" + "<font color='#4152A3'><i>" + htmlentities("\"" + myNode.attributes[i].value + "\"" ) + "</i></font>";
		strapped_html_wadl_string += " " + myNode.attributes[i].name + "=" + "\"" + myNode.attributes[i].value + "\"";
	}

	strapped_html_wadl_string += ">";
	strapped_html_wadl_string += "\n";
	html_wadl_string += "<font color='#008080'>" + htmlentities(">") + "</font>";
	html_wadl_string += "</br>";

	// print myNode's children nodes
	for (var i = 0; i < myNode.childNodes.length; i++){
		spaces += 4;
		parse_wadl_html_two(myNode.childNodes[i]);
	}

	// print the end element of myNode
	addSpaces();
	strapped_html_wadl_string += "</" + myNode.nodeName + ">";
	strapped_html_wadl_string += "\n";
	html_wadl_string += "<font color='#008080'>" + htmlentities("</" + myNode.nodeName + ">") + "</font>";
	html_wadl_string += "</span>";
	html_wadl_string += "</br>";

	// decrease spaces as the recursion is going down in the element hierarchy
	spaces -= 4;
}

function save_wadl_to_file(){
	html_wadl_string = '';
	strapped_html_wadl_string = '';
	spaces = 0;
	parse_wadl_html_two(rootNode);
	$('#popupBig').html(html_wadl_string);
	$('#popupBig').append("</br><button onClick=\"document.getElementById('outerpopup').style.visibility='hidden';\">Close</button>");

	document.getElementById("outerpopup").style.visibility="visible";
}

*/