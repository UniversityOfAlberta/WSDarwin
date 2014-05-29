<?php



if ( isset($_FILES) ) {
    $filename = basename($_FILES['file']['name']);
    //$error = true;
    //console.log("server windir: " + $_SERVER['WINDIR'] );

    // Only upload if on my home win dev machine
    //if ( isset($_SERVER['WINDIR']) ) {
    //var_dump($_FILES.length);
    $path = "wsdarwin/".$filename;
    //$error = !move_uploaded_file($_FILES['uppedFile']['tmp_name'], $path);

    $destination_path = getcwd().DIRECTORY_SEPARATOR;
	$destination_path = "/home1/pokemon/public_html/wsdarwin/uploads/";
	
	//for (int i = 0; i < $_FILES.length)
	//var_dump($_POST['uppedFiles']);
    //$fileArray = $_POST['uppedFiles'];
    //var_dump($destination_path);
	//echo "file length: " + count($_FILES) . "\n";
	$filepaths = array();
	for ($i = 0; $i < count($_FILES); $i++){
	  //echo "file name: " . $_FILES["file".$i]['name'] . "\n";
	  $file_name = basename( $_FILES["file".$i]["name"] );
	  $target_path = $destination_path . $file_name;
	  move_uploaded_file($_FILES['file'.$i]['tmp_name'], $target_path);
	  $file_link = "http://pokemonpacific.com/wsdarwin/uploads/" . $file_name;
	  array_push($filepaths, $file_link);
	}

    
    /*if (move_uploaded_file($_FILES['file']['tmp_name'], $path)){
    	echo "success";
    } else {
    	echo "fail";
    }*/


    $rsp = array(
        //'error' => $error, // Used in JS
        //'filename' => $filename,
        //'filepath' => '/wsdarwin/' . $filename, // Web accessible
        'filepaths' => $filepaths,
        'response' => 'success'
    );
    echo json_encode($rsp);
    exit;

} else {
	echo var_dump($_FILES);
	echo var_dump($_POST);
	//echo "Error ..";
	exit;
}

?>