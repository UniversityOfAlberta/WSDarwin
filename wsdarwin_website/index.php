<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<?php include_once("funcsPHP/genFuncs.php") ?>
<html>

<head>
<!-- css sheets -->
<link rel=stylesheet href="<?= $GLOBALS['baseURL']; ?>css/first.css" type="text/CSS">
<link rel=stylesheet href="<?= $GLOBALS['baseURL']; ?>css/diffview.css" type="text/CSS">
<!-- scripts -->
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/generalFuncs.js"></script>
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/ObjTree.js"></script>
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/jsdiff.js"></script>
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/jsdifflib-master/difflib.js"></script>
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/jsdifflib-master/difflib.js"></script>
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/jsdifflib-master/diffview.js"></script>

<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>
<!-- <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.9.0/jquery.min.js"></script> -->


<script>

function parseXml(xml) {
   var dom = null;
   if (window.DOMParser) {
      try { 
         dom = (new DOMParser()).parseFromString(xml, "text/xml"); 
      } 
      catch (e) { dom = null; }
   }
   else if (window.ActiveXObject) {
      try {
         dom = new ActiveXObject('Microsoft.XMLDOM');
         dom.async = false;
         if (!dom.loadXML(xml)) // parse error ..

            window.alert(dom.parseError.reason + dom.parseError.srcText);
      } 
      catch (e) { dom = null; }
   }
   else
      alert("cannot parse xml string!");
   return dom;
}
</script>

</head>

<body>

<div class='outerpopup' id='outerpopup'>
	<div class='popupBig' id="popupBig"></div>
</div>

<div class='bodyWrap' id='bodyWrap'>

	<div class='popupDiv' id='popupDiv'>heeeeeeeeey</div>

	<div class='fields_input'>
		<button onClick="addURLField()"> Add URL </button>

		Select images: <input id='files' type="file" multiple>
		<input type="submit">

		<script>
		document.getElementById('files').addEventListener('change', function(e) {
		    //var file = this.files[0];
		    console.log("files length: " + this.files.length);
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
			    		uppedWADLurls.push(resp['filepaths'][i]);
			    	}

			    }
			}

		    xhr.open('post', "/wsdarwin/funcsPHP/uploadFile.php", true);
		    //alert("file is " + file);
		    //var filesArray = [];
		    var formData = new FormData();
		    for (var i = 0; i < this.files.length; i++){
		    	//filesArray.push(this.files[i]);
		    	formData.append("file"+i, this.files[i]);
		    }
			//formData.append("uppedFiles", this.files);
		    xhr.send(formData);

		}, false);
		</script>

		<div id='fieldUrlDiv' class='fieldUrlDiv'>
			<script type="text/javascript"> addURLField(); </script>
		</div>

	</div>
	<div class='middleRegion'>

	<div id='compareDiv' class='compareDiv'>
		<button class='showCompareBtn' id='showCompareBtn' onClick="showCompareOptions()">+ Show Compare Tool</button>
		<div id='compareInsideDiv' class='compareInsideDiv'>
			<button onClick="addCompareURLField()"> Add URL </button>
			<span>
			Upload a WADL File<input id='filesCompare' type='file' placeholder='Upload'></input>
			</span>

			<script>
				document.getElementById('filesCompare').addEventListener('change', function(e) {
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
					    		compareWADLurls.push(resp['filepaths'][i]);
					    	}
					    	//console.log("URLS added: " + uppedWADLurls);
					    }
					}

				    xhr.open('post', "/wsdarwin/funcsPHP/uploadFile.php", true);

				    var formData = new FormData();
				    for (var i = 0; i < this.files.length; i++){
				    	//filesArray.push(this.files[i]);
				    	formData.append("file"+i, this.files[i]);
				    }
					//formData.append("uppedFiles", this.files);
				    xhr.send(formData);

				}, false);
			</script>

		</div>
	</div>

	<div id='optionsDiv' class='optionsDiv'>
		<button class='showOptionsBtn' id='showOptionsBtn' onClick="showOptions()">+ Show Options</button>
		<div class='optionsInsideDiv'>
			<div>Comparison View: </div>
			<div>Parameters: </div>
			<span>Comparison View: </span>
			<!--
			<span>Side By Side Diff</span><input type='radio' id='sideBySideDiff' onClick='diffUsingJS()'></input>
			<span>Inline Diff</span><input type='radio' id='inlineDiff' onClick='diffUsingJS()'></input>
			-->
			<input type="radio" onClick="inlineDiff()" name="comparisonDiffType" value="true" id="diff_yes" />
			<label for="diff_yes">Inline Diff</label>
			<input type="radio" onClick="sideBySideDiff()" name="comparisonDiffType" value="false" id="diff_no" />
			<label for="diff_no">Side by Side Diff</label>

		</div>
	</div>

	<button class='analyzeBtn' id='analyzeBtn' onClick="analyze('analyze')"> Analyze URI </button>		
	<button class='compareBtn' id='compareBtn' onClick="compareBtn()"> Compare </button>
	<button class='crossServiceCompareBtn' id='crossServiceCompareBtn' onClick="crossServiceCompareBtn()"> Cross-Service Comparison </button>
	<!--<button class='saveWADLbtn' id='saveWADLbtn' onClick='save_wadl_to_file()'>Save WADL File</button>-->
	<button class='saveWADLbtn' id='saveWADLbtn' onClick='downloadWADL()'>Save WADL File</button>

	<div class='wadlDiv'>
		<div id='wadlOutput' class='wadlOutput' placeholder='WADL'>
			<p>No WADL Output</p>
		</div>
		<div id='right_wadl_output' class='halfwadlOutput'></div>
		<div id='left_wadl_output' class='halfwadlOutput'></div>
		<div id='leftInfoMenu' class='leftInfoMenu'>
			<div id="rightHalfDiv" class="halfDiv">
				<div>hey</div>
				<div>two</div>
			</div>
			<div id="leftHalfDiv" class="halfDiv">
				<div>second</div>
				<div>three</span>
			</div>
			<!--<select size="3" height="100px" name="selectionField" multiple="yes" > 
			  <option value="CA" >California -- CA </option>
			  <option value="CO" >Colorado -- CO</option>
			  <option value="CN" >Connecticut -- CN</option>
			  <option value="CN" >Connecticut -- CN</option>
			  <option value="CN" >Connecticut -- CN</option>
			  <option value="CN" >asdfsd -- CN</option>
			</select>
			<button>hello world</button>-->
		</div>
	</div>

</div>

</div>

<?php 
if (isset($GLOBALS['error_message'])){
	echo "err:" . $GLOBALS['error_message'];
}
?>

</body>
</html>
