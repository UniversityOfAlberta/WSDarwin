<?php

$server_host = 5;

if ($server_host == 0){
        $baseURL = "http://" . $_SERVER['HTTP_HOST'] . "/";				// 000webhost:  blackmarket5.hostei.com
        $basehref = "<base href='//blackmarket5.hostei.com' \/>";
} else if ($server_host == 1){
        $baseURL = "http://localhost/abc/";	// localhost mihai
        $basehref = "<base href=\"http://localhost/abc/\" target='_blank' />";
} else if ($server_host == 2){
        //$baseURL = "http://cs410-06.cs.ualberta.ca/";
        //$baseURL = "http://cs410.cs.ualberta.ca:41061/";
        $baseURL = "/";
        $basehref = "<base href=\"//cs410.cs.ualberta.ca:41061\" />";
} else if ($server_host == 3){
        $baseURL = "http://96.52.101.188/pokemonpacific/";	// localhost mihai
        $basehref = "<base href=\"http://96.52.101.188/pokemonpacific/\" />";
} else if ($server_host == 4){
        $baseURL = "http://pokemonpacific.com/wsdarwin/";	// localhost mihai
        $basehref = "<base href=\"http://pokemonpacific/wsdarwin/\" />";
} else if ($server_host == 5){
        $baseURL = "http://ssrg17.cs.ualberta.ca/wsdarwin/";       // localhost mihai
        $basehref = "<base href=\"http://ssrg17.cs.ualberta.ca/wsdarwin/\" />";
}

function addURLtextfield(){
        echo "<div class='textfieldWrap'>";
        echo "<input type='text' name='urlTextfield' class='urlTextfield'></input>";
        echo "<button>submit url</button>";
        echo "<button>check url</button>";
        echo "</div>";
}


?>