<?php



if ( isset($_POST['wadlString']) ) {
    $destination_path = "/home1/pokemon/public_html/wsdarwin/uploads/"; // use it later.
    $destination_path = "/var/www/html/wsdarwin/uploads/wadl_FILE.wadl";
    if (! ($fp = fopen($destination_path, 'w')) ){
        echo "Error opening file";
    }
    $toWrite = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" . $_POST['wadlString'];
    fwrite($fp, $toWrite);
    //fwrite($fp, '23');
    fclose($fp);
    echo "done !";

//$file = "";


} else {
	echo "Error ..";
	exit;
}

?>