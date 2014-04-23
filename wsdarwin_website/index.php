<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<?php include_once("funcsPHP/genFuncs.php") ?>
<html>

<head>

<!-- <LINK REL="SHORTCUT ICON" HREF="http://pokemonpacific.com/images/PikachuSprite.gif" /> -->
<!-- css sheets -->
<link rel=stylesheet href="<?= $GLOBALS['baseURL']; ?>css/first.css" type="text/CSS">

<!-- scripts -->
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/generalFuncs.js"></script>
<script src="<?= $GLOBALS['baseURL']; ?>funcsJS/ObjTree.js"></script>
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

	$(document).ready(function(){
		//alert('dfdsdf');
		//$("#wadlOutput").html("#include file=\"http://localhost:8080/wsdarwin_1.0.0/twitterMerged.wadl\"");
		//var cont = "<iframe src=\"http://localhost:8080/wsdarwin_1.0.0/twitterMerged.wadl\" width=800 height=500></iframe>";
		//$("#wadlOutput").html(cont);
		//$("#wadlOutput").load("http://www.google.com/", function(){alert('Done deal');});
		//alert('dfdsdf');

		//var xotree = new XML.ObjTree();
   		var url = "http://localhost:8080/wsdarwin_1.0.0/twitterMerged.wadl";
   		var url2 = "C:/Users/mihai/eclipse_workspace/wsdarwin_1.0.0/WebContent/twitterMerged.wadl";
    	//var tree = xotree.parseHTTP( url );

		//$("#wadlOutput").append("abc " + tree.html["@lang"]);

		//var xotree = new XML.ObjTree();
		//var xmlsrc = '<span class="author">Kawasaki Yusuke</span>';
		//var tree = xotree.parseXML( xmlsrc );
		//var class = tree.span["-class"];        # attribute
		//var name  = tree.span["#text"];         # text node

		/*var cc = "<orderperson>John Smith</orderperson>"+
  "<shipto>"+
   " <name>Ola Nordmann</name>"+
    "<address>Langgt 23</address>"+
    "<city>4000 Stavanger</city>"+
    "<country>Norway</country>"+
  "</shipto>"+
  "<item>"+
    "<title>Empire Burlesque</title>"+
    "<note>Special Edition</note>"+
    "<quantity>1</quantity>"+
    "<price>10.90</price>"+
  "</item>"+
  "<item>"+
    "<title>Hide your heart</title>"+
    "<quantity>1</quantity>"+
    "<price>9.90</price>"+
  "</item>"+
"</shiporder>";*/
		//console.log( parseXml(cc) );
		console.log("Hello world..");



		//alert('1: ' + tree.span["-class"] + ", 2: " + tree.span["#text"] + ", 3: ");

	});



</script>
<?php
/*
//ini_set('display_errors', false);
//set_exception_handler('ReturnError');

$url = "http://pokemonpacific.com/twitterMerged.xml";
		$r = '';
		if ($url) {

			// fetch XML
			$c = curl_init();
			curl_setopt_array($c, array(
				CURLOPT_URL => $url,
				CURLOPT_HEADER => false,
				CURLOPT_TIMEOUT => 10,
				CURLOPT_RETURNTRANSFER => true
			));
			$r = curl_exec($c);
			curl_close($c);

		}

		function saveAttributes($src_elem, $dest_elem){
  			// saving the element's attributes
  			foreach ($src_elem->attributes AS $att){
  				$dest_elem->attrs[$att->nodeName] = $att->nodeValue;
  			}
		}

		$xmlDoc = new DOMDocument();
		$xmlDoc->loadXML( $r );
		$x = $xmlDoc->documentElement;

		$all_element = new any_element;
		$all_element->class_type = 'application';
		$all_element->attrs['xmlns'] = "http://wadl.dev.java.net/2009/02";
		$all_element->attrs['xmlns:xs'] = "http://www.w3.org/2001/XMLSchema";

		foreach ($x->childNodes AS $item){
		  	//print $item->nodeName . " = " . $item->nodeValue . "<br>";

		  	if ($item->nodeName == 'grammars'){
		  		// add code for multiple <grammars>..
		  		$grammarsElem = new any_element;
		  		$grammarsElem->class_type = $item->nodeName;	// grammars
		  		//saveAttributes($item, $grammarsElem);

		  		$schemaItem = $item->firstChild;

		  		$schemaElem = new any_element;	// can only have 1 <xs:schema> element
		  		$schemaElem->class_type = $schemaItem->nodeName;
		  		saveAttributes($schemaItem, $schemaElem);

		  		foreach ($schemaItem->childNodes AS $complex_simpleItem){
		  			$xs_celeElem = new any_element;
		  			$xs_celeElem->class_type = $complex_simpleItem->nodeName;		// <xs:complexType> or <xs:element>
		  			saveAttributes($complex_simpleItem, $xs_celeElem);

		  			foreach ($complex_simpleItem->childNodes AS $sequenceItem){
			  			$xs_sequenceElem = new any_element;
			  			$xs_sequenceElem->class_type = $sequenceItem->nodeName;		// xs:sequence
			  			saveAttributes($sequenceItem, $xs_sequenceElem);

			  			foreach ($sequenceItem->childNodes AS $xs_elementItem){
				  			$xs_elementElem = new any_element;
				  			$xs_elementElem->class_type = $xs_elementItem->nodeName;		// xs:element
				  			saveAttributes($xs_elementItem, $xs_elementElem);

				  			array_push($xs_sequenceElem->elements, $xs_elementElem);	// add <xs:element> element(s) to <xs:complexType> array
			  				//var_dump($xs_elementElem);
			  			}

			  			array_push($xs_celeElem->elements, $xs_sequenceElem);	// add <xs:element> element(s) to <xs:complexType> array
		  			}

		  			array_push($schemaElem->elements, $xs_celeElem);			// add <xs:complexType> or <xs:element> element(s) to <schema> array
		  		}

		  		array_push($grammarsElem->elements, $schemaElem);			// add <xs:complexType> or <xs:element> element(s) to <schema> array

		  		array_push($all_element->elements, $grammarsElem);

		  	} else if ($item->nodeName == 'resources'){
		  		$resources_Elem = new any_element;
		  		$resources_Elem->class_type = 'resources';
				saveAttributes($item, $resources_Elem);

				foreach ($item->childNodes AS $resourceItem){
					$resourceElem = new any_element;
					$resourceElem->class_type = $resourceItem->nodeName;							// 'resource'
					saveAttributes($resourceItem, $resourceElem);

					foreach ($resourceItem->childNodes AS $methodItem){
						$methodElem = new any_element;
						$methodElem->class_type = $methodItem->nodeName;							// 'method'
						saveAttributes($methodItem, $methodElem);

						foreach ($methodItem->childNodes AS $request_responseItem){
							$request_responseElem = new any_element;
							$request_responseElem->class_type = $request_responseItem->nodeName;	// 'request' or 'response'
							saveAttributes($request_responseItem, $request_responseElem);

							foreach ($request_responseItem->childNodes AS $paramItem){
								$paramElem = new any_element;
								$paramElem->class_type = $paramItem->nodeName;						// 'param'
								saveAttributes($paramItem, $paramElem);

								array_push($request_responseElem->elements, $paramElem);	// add <param> element(s) to <request> or <response> array
								//var_dump($paramElem);
							}


							array_push($methodElem->elements, $request_responseElem);	// add <request> or <response> element(s) to <method> array
							//var_dump($request_responseElem);
						}

						array_push($resourceElem->elements, $methodElem);	// add <method> element(s) to <resource> array
						//var_dump($methodElem);
					}

					array_push($resources_Elem->elements, $resourceElem);	// add <resource> element to <resources> array

					//var_dump($resourceElem);
				}

				array_push($all_element->elements, $resources_Elem);
		  		//var_dump($resources_Elem);
		  	}


		  }

		//var_dump($all_element);
		$elemString = '';
		print_wadl($all_element);

		$printoutString = '';
		$spaces = 0;
		//print $GLOBALS['elemString'];

		function print_wadl($myNode){
			global $elemString;
			global $spaces;

			$n = 0;
			while ($n < $spaces){
				$GLOBALS['elemString'] .= "&nbsp;";
				$n++;
			}

			$GLOBALS['elemString'] .= htmlentities("<" . $myNode->class_type);

			foreach ($myNode->attrs as $attName => $attValue){
				//print $elemString;
				$GLOBALS['elemString'] .= htmlentities(" " . $attName . "=\"" . $attValue . "\"" );
				//print $elemString;
				//var_dump($elemString);
				//print $elemString;
			}

			$GLOBALS['elemString'] .= htmlentities(">");
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

			$GLOBALS['elemString'] .= htmlentities("</" . $myNode->class_type . ">");
			$GLOBALS['elemString'] .= "</br>";
			//print $elemString;
		}

		

		//var_dump($doc);
//print_r(simplexml_import_dom($doc)->asXML());
//$abc = json_encode(new SimpleXMLElement($r));
//print_r($abc);



// //output xml in your response: 
//header('Content-Type: text/xml'); 
//echo $xml->asXML(); 


//print_r( $abc[] );
//print_r(json_encode($r));

if ($r) {
	// XML to JSON
	//echo json_encode(new SimpleXMLElement($r));
} else {
	// nothing returned?
	//ReturnError();
	echo '{"error":true}';
}

class xs_complexType {
	public $attrs = array();	// array of attributes
	public $elements = array();	// array of 'xs_element' objects
}

class xs_element {
	public $attrs = array();	// array of attributes

}

class any_element {
	public $class_type;	// resources/resource/path/method/request/param
	public $attrs = array();	// array of attributes
	public $elements = array();
}
*/
?>


</head>

<body>

<div class='bodyWrap' id='bodyWrap'>

<div class='popupDiv' id='popupDiv'>heeeeeeeeey</div>

<div class='fields_input'>
	<button onClick="addURLField()"> Add URL </button>
	<button id='analyzeBtn' onClick="analyzeBtn()"> Analyze URI </button>
	<br>

	<div id='fieldUrlDiv' class='fieldUrlDiv'>
		<script>addURLField();</script>
	</div>
	
	<!--
	<div class='areaUrlDiv'>
		<textarea class='textarea' name='textarea' id='textarea' placeholder='Enter one URL per line'></textarea>
	</div>
	-->

</div>

<div class='middleRegion'>
	<div class='requestDetails'>
		<div id='urlRequestDiv' class='urlRequestDiv'>
			Hello There. Parameter Analysis goes here.
		</div>
	</div>

	<div class='wadlDiv'>
		<div id='wadlOutput' class='wadlOutput' placeholder='WADL'>
			<p>WADL:</p>
			<?php
			//$homepage = file_get_contents("http://google.com");
			//echo $homepage;
			//$xml = simplexml_load_file('http://localhost:8080/wsdarwin_1.0.0/twitterMerged.wadl');

			//print($xml);
			$map_url = "http://localhost:8080/wsdarwin_1.0.0/twitterMerged.wadl";
			//$map_url = "http://google.com";
			//$response_xml_data = file_get_contents($map_url);

    		//if($response_xml_data){
    			//echo "<pre>";
            	//print($response_xml_data);
            	//echo "</pre>";
        	//}
			//$response_xml_data = file_get_contents($map_url);
	        //$data = simplexml_load_string($response_xml_data);
        	//echo "<pre>"; print_r($data); exit; 

        	//$xml = simplexml_load_file("http://localhost:8080/wsdarwin_1.0.0/twitterMerged.wadl"); // or simplexml_load_string()
			//$json = json_encode($xml);
			$url = "http://localhost:8080/wsdarwin_1.0.0/twitterMerged.wadl";
			/*$fileContents= file_get_contents($url);
			$fileContents = str_replace(array("\n", "\r", "\t"), '', $fileContents);
			$fileContents = trim(str_replace('"', "'", $fileContents));
			$simpleXml = simplexml_load_string($fileContents);
			$json = json_encode($simpleXml);*/

			
			//echo $json;

			?>

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
