
$('document').ready(function(){
    initBootstrapJS_popover();
    var dl_url = tomcat_server_path + "wsdarwin_1.0.0/files/client/proxy.zip";
    console.log("dl:  " + dl_url);
    $("#downloaGenProxy").attr('onclick', "downloadGeneratedProxy(\"" + dl_url + "\")");
});

// tomcat server path/location
// var tomcat_server_path              = "http://localhost:8080/";
var tomcat_server_path            = "http://ssrg17.cs.ualberta.ca:8080/";

var server_api_url                  = tomcat_server_path + "wsdarwin_1.0.0/jaxrs/api/";

var wadl_download_str           = '';
var extended_wadl_download_str  = '';

// for java_diff: ( if true, appends text line by line, otherwise it prints it all in a tree-hierarchy way)
var parsing_html_mode           = true;
var max_wadl_files_uploaded     = 1;

// calls a (java) service so this needs to be true
var crossService                = true;

//wadl html file
var wadl_attribute_edit_mode    = true;         // DEBUG (only applicable to 'analyze', not 'compare')
var add_elements_mode           = true;         // DEBUG (only applicable to 'analyze', not 'compare')

// type confidence colors
var typeConfidence0_24          = "#c9302c";
var typeConfidence24_49         = "#F0854E";
var typeConfidence50_74         = "#f0ad4e";
var typeConfidence75_100        = "#449d44";

var DEBUG_PRINT = true;

// global vars keeping track of urls
var noURLFields                 = 0;
var noCompareURLFields          = 0;
var uppedWADLurls               = [];
var compareWADLurls             = [];

var rootNode;

var lineNumber  = 0;
var spaces      = 0;
var node_id     = 0;
var xmlDoc;

var oldRootDoc;
var newRootDoc;

