<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<?php include_once("funcsPHP/genFuncs.php") ?>
<html>

<head>

<!-- scripts -->
<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>
<script src="//netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js"></script>

<!-- Bootstrap css sheets 3.2.0 -->
<link href="css/bootstrap.min.css" rel="stylesheet">
<link href="css/bootstrap.css" rel="stylesheet">

<!-- css sheets -->
<link rel="stylesheet" href="<?= $GLOBALS['baseURL']; ?>css/diffview.css" type="text/CSS">
<!-- Bootstrap css sheets 3.1.1 
<link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css">
<link rel="stylesheet" href="//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap-theme.min.css">
-->
<link rel="stylesheet" href="<?= $GLOBALS['baseURL']; ?>css/first.css" type="text/CSS">


<!-- <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.9.0/jquery.min.js"></script> -->

<!-- User-Defined js -->
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/setup.js"></script>
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/utilityFuncs.js"></script>
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/guiFuncs.js"></script>
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/compareFuncs.js"></script>
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/crossServiceComparisonFuncs.js"></script>
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/generalFuncs.js"></script>
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/testFuncs.js"></script>

<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/ObjTree.js"></script>
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/jsdiff.js"></script>
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/jsdifflib-master/difflib.js"></script>
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/jsdifflib-master/difflib.js"></script>
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/jsdifflib-master/diffview.js"></script>

</head>

<body>
	<div class='bodyWrap' id='bodyWrap'>

	<div class="menuTabs">
		<!-- Nav tabs -->
		<ul class="nav nav-tabs">
		  <li class="active" name='analysisViewName' id='analysisView'><a href="javascript:activateView('analysisView'); ">Analysis</a></li>
		  <li id='comparisonView' name='compareViewName'><a href="javascript:activateView('comparisonView')">Comparison</a></li>
		  <li id='crossServiceComparisonView' name='crossServiceViewName'><a href="javascript:activateView('crossServiceComparisonView')">Cross Service Comparison</a></li>
		  <li id='settingsView'><a href="javascript:activateView('settingsView')">Settings</a></li>
		</ul>

		<!-- Tab panes -->
		<div class="tab-content">
		  <div class="tab-pane active" id="analysis">

		  	<!-------------------------------->

		  	<!-- compareA goes here: -->
		  	<?php printCompareA(); ?>

			<!-- compareB goes here: -->
			<?php printCompareB(); ?>

			<!-- Analyze the input(s) -->
			<div id='analyzeSubmitBtn' class="submitBtnDiv">
				<button type="button" id='analyzeBtn' onClick="analyzeBtn()" style="width: 84px;" class="btn btn-success">Generate WADL</button>
				<!-- <button type="button" onClick="runSampleTest('analyze')" 
					 class="btn btn-default" style="margin-left: 7px;">Run Sample Test</button> -->
				<!-- Run Sample Test(s) -->
				<div class="btn-group">
				  <button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">
				    Run Sample Test <span class="caret"></span>
				  </button>
				  <ul class="dropdown-menu" role="menu">
				    <li><a href="#" onClick="runSampleAnalysis(1)"> 1 link (Github) </a></li>
				    <li><a href="#" onClick="runSampleAnalysis(2)"> 3 links (Github)</a></li>
				    <li><a href="#" onClick="runSampleAnalysis(3)"> 4 links (Facebook)</a></li>
				    <li><a href="#" onClick="runSampleAnalysis(4)"> 1 links (Google Maps)</a></li>
				  	<li><a href="#" onClick="runSampleAnalysis(4)"> 1 links (Openweathermap Maps)</a></li>
				  </ul>
				</div>
				<!-- Download WADL button -->
				<div class="btn-group">
				  <button type="button" class="btn btn-warning dropdown-toggle" data-toggle="dropdown">
				    Download <span class="caret"></span>
				  </button>
				  <ul class="dropdown-menu" role="menu">
				    <li><a href="#" onClick="downloadWADL()">WADL</a></li>
				    <li><a href="#" onClick="downloadWADL(true)">Extended WADL</a></li>
				  </ul>
				</div>
			</div>

			<!-- Compare the input(s) -->
			<div id='compareSubmitBtn' class="compareSubmitBtnDiv">
				<button type="button" id='compareBtn' onClick="compareBtn()" style="width: 84px;" class="btn btn-primary">Compare</button>
				<button type="button" onClick="runSampleTest('compare')" class="btn btn-default" style="margin-left: 7px;">Run Sample Test</button>
			</div>
			
			<!-- Cross-Service Comparison of the input(s) -->
			<div id='crossServiceCompareSubmitBtn' class="compareSubmitBtnDiv">
				<button type="button" id='crossServiceCompareBtn' onClick="crossServiceCompareBtn()" style="width: 180px;" class="btn btn-info">Cross-Service Compare</button>
				<button type="button" onClick="runSampleTest('crossServiceCompare')" class="btn btn-default" style="margin-left: 7px;">Run Sample Test</button>
				
			</div>
			
			<!------------------------------ -->

		  </div>
		  <div class="tab-pane" id="comparison"></div>
		  <div class="tab-pane" id="crossServiceComparison"></div>
		  <div class="tab-pane" id="settings"></div>
		</div>
	</div>

	<div class='wadlDiv'>
		<!-- <div id='wadlOutput' class='wadlOutput' placeholder='WADL'>
		</div> -->
		<!-- for the analyze service -->
		<div id='analyzeOutput' class='wadlOutput' placeHolder='WADL'></div>
		
		<!-- for the compare service -->
		<div id='wadlOutput_compare' class='wadlOutput' style="display:none;"></div>
		<div id='right_wadl_output_compare' class='halfwadlOutput'></div>
		<div id='left_wadl_output_compare' class='halfwadlOutput'></div>

		<!-- For cross-service comparison -->
		<div id='leftInfoMenu' class='leftInfoMenu'>
			<div id="rightHalfDiv" class="halfDiv list-group">
			  	<!--
			  	<a href="#" class="list-group-item active"> Cras justo odio </a>
				<a href="#" class="list-group-item">Dapibus ac facilisis in</a>
				<a href="#" class="list-group-item">Morbi leo risus</a>
				<a href="#" class="list-group-item">Porta ac consectetur ac</a>
				<a href="#" class="list-group-item">Vestibulum at eros</a>
				-->
			</div>
			<div id="leftHalfDiv" class="halfDiv list-group">
			</div>

		</div>
	</div>

	</div>
</body>
</html>
		<script>
			function onLoad() {
		    	$('#fileInput').click();
			}

			function onLoadCompare(){
				$('#fileInputCompare').click();
			}

			function handleFiles(files) {
			    var file 		= files[0];
			    var reader 		= new FileReader();
			    reader.onload 	= onFileReadComplete;
			    reader.readAsText(file);
			}
			  
			function onFileReadComplete(event) { 
			  // Do something fun with your file contents.
			}
		</script>

		<script>
		document.getElementById('fileInput').addEventListener('change', function(e) {
		//function uploadAFile()
		    //var file = this.files[0];
		    //console.log("files length: " + this.files.length);
		    var xhr = new XMLHttpRequest();
		    //xhr.file = file; // not necessary if you create scopes like this
		    xhr.addEventListener('progress', function(e) {
		        var done = e.position || e.loaded, total = e.totalSize || e.total;
		        console.log('xhr progress: ' + (Math.floor(done/total*1000)/10) + '%');
		    }, false);
		    if ( xhr.upload ) {
		        xhr.upload.onprogress = function(e) {
		            var done = e.position || e.loaded, total = e.totalSize || e.total;
		            console.log('xhr.upload progress: ' + done + ' / ' + total + ' = ' + (Math.floor(done/total*1000)/10) + '%');
		        };
		    }
		    xhr.onreadystatechange = function(e) {
		        if ( 4 == this.readyState ) {
		            console.log(['xhr upload complete', e]);
		            console.log("e is " + e.responseText );
		        }
		    };

			xhr.onreadystatechange = function() {
			    if(xhr.readyState == 4 && xhr.status == 200) {
			        alert(xhr.responseText);
			        var resp = JSON.parse(xhr.responseText);
			        console.log("links: " + resp['filepaths'] + ", count: " + resp['filepaths'].length );
			    	for (var i = 0; i < resp['filepaths'].length; i++){
			    		if (uppedWADLurls.length < max_wadl_files_uploaded) {
							uppedWADLurls.push(resp['filepaths'][i]);
				    		var displayWADLbtn = "<button style=\"margin-left: 25px;\" onClick=\"check()\" type=\"button\" class=\"btn btn-primary btn-xs\">Display WADL</button>";
				    		var deleteWADLbtn  = "<button style=\"margin-left: 10px;\" onClick=\"emptyUppedWADLs()\" type=\"button\" class=\"btn btn-danger btn-xs\">X</button>";
				    		$("#innerUploadWADLcontent").append("<div style=\"margin-bottom: 5px;\"><a href='" + resp['filepaths'][i] + "'>" + "File " + i + "</a>" + displayWADLbtn + deleteWADLbtn + "</div>");
			    		} else {
			    			alert("A maximum number (1) of WADL files has been reached");
			    		}	
			    	}
			    }
			}
			$("#uploadWADLcontent").show();

		    xhr.open('post', "/wsdarwin/funcsPHP/uploadFile.php", true);
		    //alert("file is " + file);
		    //var filesArray = [];
		    var formData = new FormData();
		    for (var i = 0; i < this.files.length; i++){
		    	//filesArray.push(this.files[i]);
		    	formData.append("file"+i, this.files[i]);
		    	console.log("ABC: " + this.files[i].filepaths);
		    }
			//formData.append("uppedFiles", this.files);
		    xhr.send(formData);
		//}
		}, false);

		document.getElementById('fileInputCompare').addEventListener('change', function(e) {
		    var xhr = new XMLHttpRequest();
		    //xhr.file = file; // not necessary if you create scopes like this
		    xhr.addEventListener('progress', function(e) {
		        var done = e.position || e.loaded, total = e.totalSize || e.total;
		        console.log('xhr progress: ' + (Math.floor(done/total*1000)/10) + '%');
		    }, false);
		    if ( xhr.upload ) {
		        xhr.upload.onprogress = function(e) {
		            var done = e.position || e.loaded, total = e.totalSize || e.total;
		            console.log('xhr.upload progress: ' + done + ' / ' + total + ' = ' + (Math.floor(done/total*1000)/10) + '%');
		        };
		    }
		    xhr.onreadystatechange = function(e) {
		        if ( 4 == this.readyState ) {
		            console.log(['xhr upload complete', e]);
		            console.log("e is " + e.responseText );
		        }
		    };

			xhr.onreadystatechange = function() {
			    if(xhr.readyState == 4 && xhr.status == 200) {
			        alert(xhr.responseText);
			        var resp = JSON.parse(xhr.responseText);
			        console.log("links: " + resp['filepaths'] + ", count: " + resp['filepaths'].length );
			    	for (var i = 0; i < resp['filepaths'].length; i++){
			    		if (compareWADLurls.length < max_wadl_files_uploaded) {
							compareWADLurls.push(resp['filepaths'][i]);
				    		var displayWADLbtn = "<button style=\"margin-left: 25px;\" onClick=\"check()\" type=\"button\" class=\"btn btn-primary btn-xs\">Display WADL</button>";
				    		var deleteWADLbtn  = "<button style=\"margin-left: 10px;\" onClick=\"emptyCompareUppedWADLs()\" type=\"button\" class=\"btn btn-danger btn-xs\">X</button>";
				    		$("#innerUploadWADLcontentCompare").append("<div style=\"margin-bottom: 5px;\"><a href='" + resp['filepaths'][i] + "'>" + "File " + i + "</a>" + displayWADLbtn + deleteWADLbtn + "</div>");
			    		} else {
			    			alert("A maximum number (1) of WADL files has been reached");
			    		}
			    		
			    	}


			    }
			}
			$("#uploadWADLcontentCompare").show();

		    xhr.open('post', "/wsdarwin/funcsPHP/uploadFile.php", true);
		    //alert("file is " + file);
		    //var filesArray = [];
		    var formData = new FormData();
		    for (var i = 0; i < this.files.length; i++){
		    	//filesArray.push(this.files[i]);
		    	formData.append("file"+i, this.files[i]);
		    	console.log("ABC: " + this.files[i].filepaths);
		    }
			//formData.append("uppedFiles", this.files);
		    xhr.send(formData);
		//}
		}, false);

function check(){
	console.log("size of uppedWADLurls is " + uppedWADLurls.length);
}

function emptyUppedWADLs(){
	uppedWADLurls = [];
	hideUploadWADLContent();
	console.log("size of uppedWADLurls is " + uppedWADLurls.length);
}

function emptyCompareUppedWADLs(){
	uppedWADLurls = [];
	hideUploadCompareWADLContent();
	console.log("size of uppedWADLurls is " + uppedWADLurls.length);
}

function hideUploadWADLContent(){
	$("#uploadWADLcontent").hide();
	$("#innerUploadWADLcontent").html("");
}

function hideUploadCompareWADLContent(){
	$("#uploadWADLcontentCompare").hide();
	$("#innerUploadWADLcontentCompare").html("");
}

function hideUploadWADLContentCompare(){
	$("#uploadWADLcontent").hide();
	$("#innerUploadWADLcontent").html("");
}
</script>

<?php

function printCompareB(){

	echo
		"<div id=\"compareB\" class=\"compareB\">
		  	<!-- Adds an input URL for Analysis -->
		  	<div class=\"addURLbtn\">
		  		<div style=\"display: inline-block; vertical-align: top;\">
					<button type=\"button\" onClick=\"addCompareURLField()\" class=\"btn btn-default\">Add URL</button>
				</div>
				<div id=\"uploadAWADLB\" class=\"uploadAWADL\">
					<button id='filesCompare' type=\"button\" onClick=\"onLoadCompare()\" class=\"btn btn-default\">Upload a WADL File</button>
					<!--	FOR MULTIPLE FILES TO BE UPLOADED 	-->
						<!-- <input style='height: 0; width: 0; visibility: hidden' id='fileInput' onchange=\"handleFiles(this.files)\" type=\"file\" multiple> -->
					
					<!--	FOR ONLY A SINGLE FILE TO BE UPLOADED 	-->
					<input style='height: 0; width: 0; visibility: hidden' id='fileInputCompare' onchange=\"handleFiles(this.files)\" type=\"file\">
					<!-- <input type=\"submit\"> -->
				</div>
			</div>

			<!-- holds all the url inputs for Compare -->
			<div id='fieldCompareUrlDiv' class='fieldCompareUrlDiv'>
				<script type=\"text/javascript\"> addCompareURLField(); </script>
			</div>
			
			<div class='compareOptions'>
				<input type='radio' class='diffTypeJava' name='diffType' id='diff_type_0' checked><label class='labelo' for='diff_type_0'>java</label></input>
				<input type='radio' class='diffTypeText' name='diffType' id='diff_type_1'><label class='labelo' for='diff_type_1'>text</label></input>
			</div>
			
			<div class=\"uploadWADLcontentCompare\" id=\"uploadWADLcontentCompare\">
				<div style=\"color: gray\">Uploaded WADL File(s) [Max 1 file can be uploaded] </div>
				<div class=\"innerUploadWADLcontentCompare\" id=\"innerUploadWADLcontentCompare\"></div>
			</div>

		</div>";
}

function printCompareA(){
	echo
		"<div id=\"compareA\" class=\"compareA\">

		  	<!-- Adds an input URL for Analysis -->
		  	<div class=\"addURLbtn\">
		  		<div style=\"display: inline-block; vertical-align: top;\">
					<button type=\"button\" onClick=\"addURLField()\" class=\"btn btn-default\">Add URL</button>
				</div>	
				<div id=\"uploadAWADLA\" class=\"uploadAWADL\" style=\"width: 148px;\">
					<button id='files' type=\"button\" onClick=\"onLoad()\" class=\"btn btn-default\">Upload a WADL File</button>
					<!--	FOR MULTIPLE FILES TO BE UPLOADED 	-->
 					<!-- <input style='height: 0; width: 0; visibility: hidden' id='fileInput' onchange=\"handleFiles(this.files)\" type=\"file\" multiple> -->
					
					<!--	FOR ONLY A SINGLE FILE TO BE UPLOADED 	-->
					<input style='height: 0; width: 0; visibility: hidden' id='fileInput' onchange=\"handleFiles(this.files)\" type=\"file\">
					<!-- <input type=\"submit\"> -->
				</div>
				<div id=\"helpPopup\" class=\"helpPopup\">
					<a href=\"#\" tabindex=\"0\" class=\"btn btn-danger\" data-toggle=\"popover\" data-trigger=\"focus\" title=\"Help\" data-content=\"And here's some amazing content. It's very engaging. Right?\">Help</a>
				</div>
			</div>
			<!-- holds all the url inputs for Analysis -->
			<div id='fieldUrlDiv' class='fieldUrlDiv'>
				<script type=\"text/javascript\"> addURLField(); </script>
			</div>
			
			<div class=\"uploadWADLcontent\" id=\"uploadWADLcontent\">
				<div style=\"color: gray\">Uploaded WADL File(s) [Max 1 file can be uploaded] </div>
				<div class=\"innerUploadWADLcontent\" id=\"innerUploadWADLcontent\"></div>
			</div>

		</div>";
}

?>

<?php


/*
			<!------------------->
			<!-- compareB DIV: -->

			<div class="compareB">
			  	<!-- Adds an input URL for Analysis -->
			  	<div class="addURLbtn">
			  		<div style="display: inline-block; vertical-align: top;">
						<button type="button" onClick="addCompareURLField()" class="btn btn-default">Add URL</button>
					</div>
					<div id="uploadAWADL" class="uploadAWADL">
						<button id='filesCompare' type="button" onClick="onLoadCompare()" class="btn btn-default">Upload a WADL File</button>
						<!--	FOR MULTIPLE FILES TO BE UPLOADED 	-->
	 					<!-- <input style='height: 0; width: 0; visibility: hidden' id='fileInput' onchange="handleFiles(this.files)" type="file" multiple> -->
						
						<!--	FOR ONLY A SINGLE FILE TO BE UPLOADED 	-->
						<input style='height: 0; width: 0; visibility: hidden' id='fileInputCompare' onchange="handleFiles(this.files)" type="file">
						<!-- <input type="submit"> -->
					</div>
				</div>

				<!-- holds all the url inputs for Compare //
				<div id='fieldCompareUrlDiv' class='fieldCompareUrlDiv'>
					
				</div>

				<div class="uploadWADLcontentCompare" id="uploadWADLcontentCompare">
					<div style="color: gray">Uploaded WADL File(s) [Max 1 file can be uploaded] </div>
					<div class="innerUploadWADLcontentCompare" id="innerUploadWADLcontentCompare"></div>
				</div>

			</div>

			<!------------------->
			<!-- compareA DIV: -->

		  	<div class="compareA">

			  	<!-- Adds an input URL for Analysis -->
			  	<div class="addURLbtn">
			  		<div style="display: inline-block; vertical-align: top;">
						<button type="button" onClick="addURLField()" class="btn btn-default">Add URL</button>
					</div>
					<div id="uploadAWADL" class="uploadAWADL">
						<button id='files' type="button" onClick="onLoad()" class="btn btn-default">Upload a WADL File</button>
						<!--	FOR MULTIPLE FILES TO BE UPLOADED 	-->
	 					<!-- <input style='height: 0; width: 0; visibility: hidden' id='fileInput' onchange="handleFiles(this.files)" type="file" multiple> -->
						
						<!--	FOR ONLY A SINGLE FILE TO BE UPLOADED 	-->
						<input style='height: 0; width: 0; visibility: hidden' id='fileInput' onchange="handleFiles(this.files)" type="file">
						<!-- <input type="submit"> -->
					</div>
				</div>
				<!-- holds all the url inputs for Analysis -->
				<div id='fieldUrlDiv' class='fieldUrlDiv'>
					<script type="text/javascript"> addURLField(); </script>
				</div>
				
				<div class="uploadWADLcontent" id="uploadWADLcontent">
					<div style="color: gray">Uploaded WADL File(s) [Max 1 file can be uploaded] </div>
					<div class="innerUploadWADLcontent" id="innerUploadWADLcontent"></div>
				</div>

			</div>

*/
?>