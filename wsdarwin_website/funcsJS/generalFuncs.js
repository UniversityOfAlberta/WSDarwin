//import "ObjTree.js";


var noURLFields = 0;

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

function analyzeBtn(){
    //grab html values and stringify into JSON
	/*var item = {};
    item.id = id;
	item.user_owner = user_owner;*/

	var obj = [];

	$("#wadlOutput").html('');

	$('input.urlInput').each(function(index) {
	    // set div id to array value
	    //$('div').attr('id', "singleURLdiv_" + idcount);
	    //idcount++;
	    //alert("id: " + $('#urlInput_'+index).attr('id') + ", val: " + $('#urlInput_'+index).val() + " index:" + index );
	    //alert('index is' + index);
		obj.push( $('#urlInput_'+index).val() );
		//parseInputURLs( $('#urlInput_'+index).val() );
	});

	for (var i = 0; i < obj.length; i++){
		//alert('i: ' + i + ", val: " + obj[i]);
	}
	
	var jsonStr = JSON.stringify(obj);

    $.ajax({
    	url: "http://localhost:8080/wsdarwin_1.0.0/jaxrs/api/analyze",
        type: "GET",
        data: { urls: jsonStr },
        //parameters: { url:  },
        dataType: "html",
        crossDomain: true,	// sending ajax call to a jsp servlet, thus needing to enable this
        //data: { buyerid: buyerid_arg, txnid: txn_id},
        success: function(response) {
        	//alert('resp: ' + response);
        	//var jsonObj = JSON.parse(response);
        	//alert("WTF ");
			$("#wadlOutput").append("<a href=\"" + response + "\">" + response + "</a> <br>");
			$("#wadlOutput").append( getWADL(response) );
        },
        error: function(xhr, status, error) {
          alert("status: " + status + ", xhr: " + xhr.responseText + ", error: " + error) ;
		  //var err = eval("(" + xhr.responseText + ")");
		  //alert(err.Message);
		}

    });


	/*var xotree = new XML.ObjTree();
    var url = "http://localhost:8080/wsdarwin_1.0.0/twitterMerged.wadl";
    var tree = xotree.parseHTTP( url );

	$("#wadlOutput").html(yy);*/
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

function hideStuff(){
	alert('Hiding ?');

}

function analyzeSingleURL(id){
	var urlToAnalyze = $("#urlInput_" + id).val();
	//alert('its value is ' + urlToAnalyze);
    $.ajax({
    	url: "http://localhost:8080/wsdarwin_1.0.0/jaxrs/api/analyze",
        type: "GET",
        data: { url: urlToAnalyze },
        dataType: "html",
        crossDomain: true,	// sending ajax call to a jsp servlet, thus needing to enable this
        //data: { buyerid: buyerid_arg, txnid: txn_id},
        success: function(response) {
			//alert("cart saved !resp:" + response);
			//JSON.decode();
			
        },
        error: function(xhr, status, error) {
          alert("status: " + status + ", xhr: " + xhr.responseText + ", error: " + error) ;
		  //var err = eval("(" + xhr.responseText + ")");
		  //alert(err.Message);
		}
    });
}

// analyzing a text area ( this func should be used for batch url input)
function parseInputURLs(url){
	//$("#urlRequestDiv").text("");
	
	//$('input.urlInput').each(function(index) {

	//}


}

function parseURL(url) {
    var a =  document.createElement('a');
    a.href = url;
    return {
        source: url,
        protocol: a.protocol.replace(':',''),
        host: a.hostname,
        port: a.port,
        query: a.search,
        params: (function(){
            var ret = {},
                seg = a.search.replace(/^\?/,'').split('&'),
                len = seg.length, i = 0, s;
            for (;i<len;i++) {
                if (!seg[i]) { continue; }
                s = seg[i].split('=');
                ret[s[0]] = s[1];
            }
            return ret;
        })(),
        file: (a.pathname.match(/\/([^\/?#]+)$/i) || [,''])[1],
        hash: a.hash.replace('#',''),
        path: a.pathname.replace(/^([^\/])/,'/$1'),
        relative: (a.href.match(/tps?:\/\/[^\/]+(.+)/) || [,''])[1],
        segments: a.pathname.replace(/^\//,'').split('/')
    };
}


function loadXMLString(txt) 
{
if (window.DOMParser)
  {
  parser=new DOMParser();
  xmlDoc=parser.parseFromString(txt,"text/xml");
  }
else // code for IE
  {
  xmlDoc=new ActiveXObject("Microsoft.XMLDOM");
  xmlDoc.async=false;
  xmlDoc.loadXML(txt); 
  }
return xmlDoc;
}

function loadXMLDoc(filename)
{
if (window.XMLHttpRequest)
  {
  xhttp=new XMLHttpRequest();
  }
else // code for IE5 and IE6
  {
  xhttp=new ActiveXObject("Microsoft.XMLHTTP");
  }
xhttp.open("GET",filename,false);
xhttp.send();
return xhttp.responseXML;
}

function any_element(){
	this.class_type = new String();
	this.attrs = new Array();
	this.elementsArray = new Array();
}

function htmlentities(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}


var elemString = '';
var spaces = 0;
var rootNode;
var node_id = 0;
var xmlDoc;

function getWADL(merged_url_path){

	var application = new any_element();

	console.log('name: ' + application.elementsArray.length);

	xmlDoc = loadXMLDoc(merged_url_path);
	//console.log(xmlDoc);
	rootNode=xmlDoc.documentElement;
	var appElem = new any_element();
	appElem.classType = rootNode.nodeName;

	console.log('root node name: ' + xmlDoc.documentElement.attributes[0].name);
	console.log('root node name: ' + xmlDoc.documentElement.attributes[0].value);
	console.log('root node name: ' + xmlDoc.documentElement.attributes.length);

	// nodeName, attributes, childNodes

	var finalPrint = '';

	init_elements(rootNode);
	setup_wadl_print();
}

//var retElem;

function removeNode(mynodeid){
	var foundNode = find_id(rootNode, mynodeid);
	//console.log('found node name: ' + foundNode.nodeName + ', parent: ' + foundNode.parentNode.nodeName);
	foundNode.parentNode.removeChild(foundNode);
	setup_wadl_print();
}

function hideNodesChildren(mynodeid){
	var foundNode = find_id(rootNode, mynodeid);

	foundNode.minimized = true;
	setup_wadl_print();
}

function showNodesChildren(mynodeid){
	var foundNode = find_id(rootNode, mynodeid);
	foundNode.minimized = false;
	setup_wadl_print();
}

function addResource(mynodeid){
	var foundNode = find_id(rootNode, mynodeid);
	var newNode=xmlDoc.createElement("resource");
	newNode.setAttribute("path","");
	foundNode.appendChild(newNode);
	//document.getElementById("popupDiv").style.visibility="visible";

	setup_wadl_print();
}

function addMethod(mynodeid){
	var foundNode = find_id(rootNode, mynodeid);
	var methodNode=xmlDoc.createElement("method");
	//methodNode.setAttribute("id","");
	methodNode.setAttribute("name","");

	foundNode.appendChild(methodNode);
	//document.getElementById("popupDiv").style.visibility="visible";

	setup_wadl_print();
}

function addParam(mynodeid){
	var foundNode = find_id(rootNode, mynodeid);
	var newNode=xmlDoc.createElement("param");
	newNode.setAttribute("path","");
	foundNode.appendChild(newNode);
	//document.getElementById("popupDiv").style.visibility="visible";

	setup_wadl_print();
}

function addRequest(mynodeid){
	var foundNode = find_id(rootNode, mynodeid);
	var newNode=xmlDoc.createElement("request");
	foundNode.appendChild(newNode);
	//document.getElementById("popupDiv").style.visibility="visible";

	setup_wadl_print();
}

function addResponse(mynodeid){
	var foundNode = find_id(rootNode, mynodeid);
	var newNode=xmlDoc.createElement("response");
	newNode.setAttribute("status","");
	foundNode.appendChild(newNode);
	//document.getElementById("popupDiv").style.visibility="visible";

	setup_wadl_print();
}

function addRepresentation(mynodeid){
	var foundNode = find_id(rootNode, mynodeid);
	var newNode=xmlDoc.createElement("representation");
	newNode.setAttribute("element","");
	newNode.setAttribute("mediaType","");
	foundNode.appendChild(newNode);

	setup_wadl_print();
}

function addFault(mynodeid){
	var foundNode = find_id(rootNode, mynodeid);
	var newNode=xmlDoc.createElement("fault");
	newNode.setAttribute("status","");
	newNode.setAttribute("mediaType","");
	foundNode.appendChild(newNode);

	setup_wadl_print();
}

function find_id(mynode, sid){
	if (mynode.my_id == sid){
		window.foundNode = mynode;
	}
	for (var i = 0; i < mynode.childNodes.length; i++){
		console.log('FINDING NODES: ' + mynode.childNodes[i].nodeName + ", id: " + mynode.childNodes[i].my_id);
		if (mynode.childNodes[i].my_id == sid){
			window.foundNode = mynode.childNodes[i];		// window.foundNode global
			console.log('! name is ' + window.foundNode.nodeName);
			return window.foundNode;
		}
		find_id(mynode.childNodes[i], sid);
	}
	return window.foundNode;
}

function save_wadl_to_file(){

}

function setup_wadl_print(){
	elemString = '';
	node_id = 0;
	spaces = 0;
	print_wadl(rootNode);
	$("#wadlOutput").html(elemString);
}

function init_elements(myNode){
	myNode.minimized = false;
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

function addSpaces() {
	var n = 0;
	while (n < spaces){
		console.log('SUP !');
		elemString += "&nbsp;";
		n++;
	}
	//spaces -= 4;
	console.log('spaces in func: ' + spaces);
	//elemString += "AAAAAAAAAAAAAAAAA";
}

var attribute_edit_mode = true;
var last_element = false;

function print_wadl(myNode){

	var n = 0;
	// spacing between each node and it's child nodes
	while (n < spaces){
		elemString += "&nbsp;";
		n++;
	}


	// set a unique id for each node
	myNode.my_id = node_id;
	node_id++;

	console.log('NODE AND ID: ' + myNode.nodeName + ", " + myNode.my_id);
	
	// if 'myNode' has any child nodes, then it can be expanded/minimized; otherwise, it can't
	if (myNode.childNodes.length > 0){
		// if the node is not minimized, add an onClick 'hideNode' action; else, add an onClick 'showNode' action
		if (myNode.minimized){
			elemString += "<button onClick=\"showNodesChildren('" + myNode.my_id + "')\">+</button>";			// remove element button
		} else {
			elemString += "<button onClick=\"hideNodesChildren('" + myNode.my_id + "')\">-</button>";			// remove element button
		}
	}

	elemString += "<span id='" + myNode.my_id + "'>";
	elemString += "<font color='#008080'>" + htmlentities("<" + myNode.nodeName) + "</font>";

	// print myNode's attributes
	for (var i = 0; i < myNode.attributes.length; i++){
		if (attribute_edit_mode){
			elemString += "<font color='#7B277C'>" + htmlentities(" " + myNode.attributes[i].name ) + "</font>" + "=" + "<font color='#4152A3'><i>" + "\"" + "<input type='text' value=\"" + htmlentities( myNode.attributes[i].value ) + "\"></input>" + "\"" + "</i></font>";
		} else {
			elemString += "<font color='#7B277C'>" + htmlentities(" " + myNode.attributes[i].name ) + "</font>" + "=" + "<font color='#4152A3'><i>" + htmlentities("\"" + myNode.attributes[i].value + "\"" ) + "</i></font>";
		}
	}

	elemString += "<font color='#008080'>" + htmlentities(">") + "</font>";
	elemString += "<button onmouseout=\"unhighlight('" + myNode.my_id + "');\" onmouseover=\"highlightDiv('" + myNode.my_id + "');\" onClick=\"removeNode('" + myNode.my_id + "')\">X</button>";	// remove element button
	elemString += "</br>";

	// only print myNode's children nodes if myNode is not minimized
	if (!myNode.minimized){
		// print myNode's children nodes
		for (var i = 0; i < myNode.childNodes.length; i++){
			if ( (i+1) == myNode.childNodes.length){
				last_element = true;
			}
			spaces += 4;
			print_wadl(myNode.childNodes[i]);
		}
	}

	if (myNode.nodeName == 'resources'){
		addSpaces();
		elemString += "&nbsp;&nbsp;&nbsp;&nbsp;";
		elemString += "<button onClick=\"addResource('" + myNode.my_id + "')\">Add Resource</button>";		// remove element button
		elemString += "</br>";
		
		addSpaces();
		elemString += "&nbsp;&nbsp;&nbsp;&nbsp;";
		elemString += "<button onClick=\"addMethod('" + myNode.my_id + "')\">Add Method</button>";			// remove element button
		elemString += "</br>";

		addSpaces();
		elemString += "&nbsp;&nbsp;&nbsp;&nbsp;";
		elemString += "<button onClick=\"addParam('" + myNode.my_id + "')\">Add Param</button>";			// remove element button
		elemString += "</br>";
	} else if (myNode.nodeName == 'resource'){
		addSpaces();
		elemString += "&nbsp;&nbsp;&nbsp;&nbsp;";
		elemString += "<button onClick=\"addResource('" + myNode.my_id + "')\">Add Resource</button>";		// remove element button
		elemString += "</br>";
		
		addSpaces();
		elemString += "&nbsp;&nbsp;&nbsp;&nbsp;";
		elemString += "<button onClick=\"addMethod('" + myNode.my_id + "')\">Add Method</button>";			// remove element button
		elemString += "</br>";
		
		addSpaces();
		elemString += "&nbsp;&nbsp;&nbsp;&nbsp;";
		elemString += "<button onClick=\"addParam('" + myNode.my_id + "')\">Add Param</button>";			// remove element button
		elemString += "</br>";
		//elemString += "</br>";
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
			addSpaces();
			elemString += "&nbsp;&nbsp;&nbsp;&nbsp;";
			elemString += "<button onClick=\"addRequest('" + myNode.my_id + "')\">Add Request</button>";		// remove element button
			elemString += "</br>";
		}
		if (!hasResponse){
			addSpaces();
			elemString += "&nbsp;&nbsp;&nbsp;&nbsp;";
			elemString += "<button onClick=\"addResponse('" + myNode.my_id + "')\">Add Response</button>";		// remove element button
			elemString += "</br>";
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
			addSpaces();
			elemString += "&nbsp;&nbsp;&nbsp;&nbsp;";
			elemString += "<button onClick=\"addRepresentation('" + myNode.my_id + "')\">Add Representation</button>";		// remove element button
			elemString += "</br>";
		}

		addSpaces();
		elemString += "&nbsp;&nbsp;&nbsp;&nbsp;";
		elemString += "<button onClick=\"addParam('" + myNode.my_id + "')\">Add Param</button>";		// remove element button
		elemString += "</br>";
	} else if (myNode.nodeName == 'response'){
		// a response element can only have one representation element
		var hasRepresentation = false;
		for (var j = 0; j < myNode.childNodes.length; j++){
			if (myNode.childNodes[j].nodeName == 'representation'){
				hasRepresentation = true;
			}
		}

		if (!hasRepresentation){
			addSpaces();
			elemString += "&nbsp;&nbsp;&nbsp;&nbsp;";
			elemString += "<button onClick=\"addRepresentation('" + myNode.my_id + "')\">Add Representation</button>";		// remove element button
			elemString += "</br>";
		}

		addSpaces();
		elemString += "&nbsp;&nbsp;&nbsp;&nbsp;";
		elemString += "<button onClick=\"addParam('" + myNode.my_id + "')\">Add Param</button>";		// remove element button
		elemString += "</br>";

		addSpaces();
		elemString += "&nbsp;&nbsp;&nbsp;&nbsp;";
		elemString += "<button onClick=\"addFault('" + myNode.my_id + "')\">Add Fault</button>";		// remove element button
		elemString += "</br>";
	}

	// print the end element of myNode
	addSpaces();
	elemString += "<font color='#008080'>" + htmlentities("</" + myNode.nodeName + ">") + "</font>";
	elemString += "</span>";
	elemString += "</br>";

	// decrease spaces as the recursion is going down in the element hierarchy
	spaces -= 4;
}

//function insertNode(){
	// 
//}



/*
function print_wadl($myNode){
	global $elemString;
	global $spaces;

	//$colorTagStart = "<font color='#3E94C0'>";
	//$colorTagEnd = "<font color='#3E94C0'>";

	$n = 0;
	while ($n < $spaces){
		$GLOBALS['elemString'] .= "&nbsp;";
		$n++;
	}

	//$no_childNodes = 0;
	//if ($myNode->elements){
	//	$no_childNodes++;
	//}
	if ($myNode->elements){
		$GLOBALS['elemString'] .= "<button onClick='hideStuff()'>" . '1' . "</button><font color='#008080'>" . htmlentities("<" . $myNode->class_type) . "</font>";
	} else {
		$GLOBALS['elemString'] .= "<font color='#008080'>" . htmlentities("<" . $myNode->class_type) . "</font>";
	}


	foreach ($myNode->attrs as $attName => $attValue){
		//print $elemString;
		$GLOBALS['elemString'] .= "<font color='#7B277C'>" . htmlentities(" " . $attName ) . "</font>" . "=" . "<font color='#4152A3'><i>" . htmlentities("\"" . $attValue . "\"" ) . "</i></font>";
		//print $elemString;
		//var_dump($elemString);
		//print $elemString;
	}

	$GLOBALS['elemString'] .= "<font color='#008080'>" . htmlentities(">") . "</font>";
	$GLOBALS['elemString'] .= "</br>";

	foreach ($myNode->elements AS $nodeElem){
		$spaces += 4;
		print_wadl($nodeElem);
	}

	$n = 0;
	while ($n < $spaces){
		$GLOBALS['elemString'] .= "&nbsp;";
		$n++;
	}
	$spaces -= 4;

	$GLOBALS['elemString'] .= "<font color='#008080'>" . htmlentities("</" . $myNode->class_type . ">") . "</font>";
	$GLOBALS['elemString'] .= "</br>";
	//print $elemString;
}*/






/*
// old version -> calls getWadlService, which then returns a String with the xml data
function getWADL(merged_url_path){
    /*$.ajax({
    	url: "http://localhost/abc/funcsPHP/getWadlService.php",
        type: "GET",
        data: { mergedUrlPath: merged_url_path },
        //parameters: { url:  },
        //dataType: "html",
        //data: { buyerid: buyerid_arg, txnid: txn_id},
        success: function(response) {
        	//alert('resp: ' + response);
        	console.log('obj is  ' + response);
        	console.log("===========================================");
        	var jsonObj = JSON.parse(response);
        	console.dir('obj is  ' + jsonObj['class_type']);
        	console.dir('obj is  ' + jsonObj['attrs']);
        	console.dir('obj is  ' + jsonObj['elements']);


        	var xmlDoc=loadXMLString(response);

			// documentElement always represents the root node
			var x=xmlDoc.documentElement.childNodes;

			for (i=0;i<x.length;i++)
			  {
			  console.log(x[i].nodeName);
			  console.log(": ");
			  console.log(x[i].childNodes[0].nodeValue);
			  console.log("<br>");
			  }
        	console.log('xml doc: ' + xmlDoc);
        	console.log('xml doc: ' + xmlDoc.childNodes);

			//alert("web service resp: " + response);
			$("#wadlOutput").append( response );
        },
        error: function(xhr, status, error) {
          alert("ERROR status: " + status + ", xhr: " + xhr.responseText + ", error: " + error) ;
		  //var err = eval("(" + xhr.responseText + ")");
		  //alert(err.Message);
		}
    });
}


*/

/*


parsing every line of a textarea element

	var lines = $('#textarea').val().split('\n');
	for(var i = 0;i < lines.length;i++){
	    //code here using lines[i] which will give you each line
	    alert('url ' + i + ": " + lines[i]);
	    urlObj = parseURL(lines[i]);
	    var cont = "Url #" + i + ":\n";
	    $('#urlRequestDiv').append(cont);
	}


*/



