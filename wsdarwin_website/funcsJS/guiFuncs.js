
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

		// show/hide output divs
		$("#analyzeOutput").css("display", "block");
		$("#right_wadl_output_compare").css("display", "none");
		$("#left_wadl_output_compare").css("display", "none");
		$("#wadlOutput_compare").css("display", "none");

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

		// show/hide output divs
		$("#analyzeOutput").css("display", "none");
		$("#right_wadl_output_compare").css("display", "block");
		$("#left_wadl_output_compare").css("display", "block");
		$("#wadlOutput_compare").css("display", "block");

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
	//console.log("Adding an url field for part A");
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
	//console.log("Adding an url field for part B");
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

function removeAllURLFields(side){
	var container_div;
	if (side == "A"){
		container_div = "fieldUrlDiv";
	} else if (side == "B"){
		container_div = "fieldCompareUrlDiv";
	}
	console.log("container div is " + container_div);
	while ($("#" + container_div).children().filter("div").length > 1){
		console.log("removing index: " + $("#" + container_div).children().filter("div").length - 1);
		removeURLField($("#" + container_div).children().filter("div").length - 1);
	}
	//reassignIDs();
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

	noURLFields = idcount-1;
}

function get_input_urls(inputNo){
	var urlArray = [];
	// JSON urls to analyze
	$('input.urlInputClass' + inputNo).each(function(index) {
		urlArray.push( $('#urlInput' + inputNo + '_'+index).val() );
	});

	return urlArray;
}

// Deprecated:
function showCompareOptions(){
	$("#showCompareBtn").html("- Hide Compare Tool");
	$("#showCompareBtn").attr('onclick', "hideCompareOptions()");
	$("#compareDiv").css('height', "inherit");
}

// Deprecated:
function hideCompareOptions(){
	$("#showCompareBtn").html("+ Show Compare Tool");
	$("#showCompareBtn").attr('onclick', "showCompareOptions()");
	$("#compareDiv").css('height', "23px");
}

// Deprecated:
function showOptions() {
	$("#showOptionsBtn").html("- Hide Options");
	$("#showOptionsBtn").attr('onclick', "hideOptions()");
	$("#optionsDiv").css('height', "inherit");
}

// Deprecated:
function hideOptions() {
	$("#showOptionsBtn").html("+ Show Options");
	$("#showOptionsBtn").attr('onclick', "showOptions()");
	$("#optionsDiv").css('height', "23px");
}