<?php
    $fp = "http://pokemonpacific.com/wsdarwin/funcsPHP/wadlFile.wadl";
    if (file_exists($fp)) {
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
?>