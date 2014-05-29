<?php


if (isset($_GET['mergedUrlPath'])){
	//$wadl =  $_GET['mergedUrlPath'];
	
	// headers to tell that result is JSON
	//header('Content-type: application/json');

	//echo json_encode(array('wadlString' => "BLAH"));
	//return;
	echo json_encode( getWADLString( $_GET['mergedUrlPath'] ) );
	return;

} else {
	echo "No URL sent !";
	return json_encode(array('error' => 'No URL sent !'));
}

function getWADLString($mergedPath){

	$url = $mergedPath;
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

	if ($r) {
		// XML to JSON
		//echo json_encode(new SimpleXMLElement($r));
	} else {
		// nothing returned?
		//ReturnError();
		echo '{"error":true}';
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
	  		$resources_Elem->class_type = $item->nodeName;										// 'resources'
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

	$elemString = '';

	return $all_element;
	/*print_wadl($all_element);

	$printoutString = '';
	$spaces = 0;
	return $GLOBALS['elemString'];*/

}

function saveAttributes($src_elem, $dest_elem){
		// saving the element's attributes
		foreach ($src_elem->attributes AS $att){
			$dest_elem->attrs[$att->nodeName] = $att->nodeValue;
		}
}

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

?>