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
    
    


    echo "doney !";

//$file = "";


} else {
	
    $destination_path = "/var/www/html/wsdarwin/uploads/wadl_FILE.wadl";
    //$fp = fopen($destination_path, 'w');
    $fp = $destination_path;
    if (file_exists($fp)) {
        echo "wtf";
        header('Content-Description: File Transfer');
        header('Content-Type: application/octet-stream');
        header('Content-Disposition: attachment; filename='.basename($fp));
        header('Content-Transfer-Encoding: binary');
        header('Expires: 0');
        header('Cache-Control: must-revalidate');
        header('Pragma: public');
        header('Content-Length: ' . filesize($fp));
        ob_clean();
        flush();
        readfile($fp);
        exit;
    }
    echo "Error ..";
    fclose($fp);
	exit;
}

?>