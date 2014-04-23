<?php

$localhostON = 4;

if ($localhostON == 0){
        $baseURL = "http://" . $_SERVER['HTTP_HOST'] . "/";				// 000webhost:  blackmarket5.hostei.com
        $basehref = "<base href='//blackmarket5.hostei.com' \/>";
} else if ($localhostON == 1){
        $baseURL = "http://localhost/abc/";	// localhost mihai
        $basehref = "<base href=\"http://localhost/abc/\" target='_blank' />";
} else if ($localhostON == 2){
        //$baseURL = "http://cs410-06.cs.ualberta.ca/";
        //$baseURL = "http://cs410.cs.ualberta.ca:41061/";
        $baseURL = "/";
        $basehref = "<base href=\"//cs410.cs.ualberta.ca:41061\" />";
} else if ($localhostON == 3){
        $baseURL = "http://96.52.101.188/pokemonpacific/";	// localhost mihai
        $basehref = "<base href=\"http://96.52.101.188/pokemonpacific/\" />";
} else if ($localhostON == 4){
        $baseURL = "http://pokemonpacific.com/wsdarwin/";	// localhost mihai
        $basehref = "<base href=\"http://pokemonpacific/wsdarwin/\" />";

}

function addURLtextfield(){
        echo "<div class='textfieldWrap'>";
        echo "<input type='text' name='urlTextfield' class='urlTextfield'></input>";
        echo "<button>submit url</button>";
        echo "<button>check url</button>";
        echo "</div>";
}


?>