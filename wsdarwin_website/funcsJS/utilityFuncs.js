


function downloadWADL(extended){
	if (DEBUG_PRINT){console.log("downloading wadl file. Extended? " + extended);}
    
    var dl_url = "/wsdarwin/funcsPHP/downloadWADLfile.php";
    var wadl_to_download = (extended) ? extended_wadl_download_str : wadl_download_str;
    $.ajax({
    	url: dl_url,
        type: "POST",
        data: { wadlString: wadl_to_download },
        dataType: "html",
        success: function(response) {
        	console.log("success?? response: " + response);
        	onclick="this.target='_blank';"
        	window.location = dl_url;
        },
        error: function(xhr, status, error) {
		  console.log("Error acccesing downloadWADLfile.");
		}

    });
}

function downloadGeneratedProxy(dl_url){
    onclick="this.target='_blank';"
    window.location = dl_url;
}


function var_dump(obj) {
    var out = '';
    for (var i in obj) {
        out += i + ": " + obj[i] + "\n";
    }
    console.log(out);
}

function htmlentities(str) {
    return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

// NOT USED ( class declarations )
function wadl_doc(){

	this.xmlDoc = null;

	this.hello = function(){
		alert('Hello World ' + this.xmlDoc);
	}

	this.setXmlDoc = function(arg){
		this.xmlDoc = arg;
	}

	this.getXmlDoc = function(){
		return this.xmlDoc;
	}

}

// initializing bootstrap js tooltips
// For performance reasons, the Tooltip and Popover data-apis are
// opt-in, meaning you must initialize them yourself.
function initBootstrapJS_tooltips(){
	jQuery('[data-toggle=tooltip]').tooltip();
}

function initBootstrapJS_popover(){
	console.log('init');
	jQuery('a[data-toggle=popover]').popover();
}
