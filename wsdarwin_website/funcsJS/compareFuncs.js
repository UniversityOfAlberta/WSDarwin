var wadl_download_str			= '';
var extended_wadl_download_str	= '';

function text_diff_JS(viewingType, process_mode) {
    // get the baseText and newText values from the two textboxes, and split them into lines
    var base = difflib.stringAsLines( firstWADL );
    var newtxt = difflib.stringAsLines( secondWADL );

    // create a SequenceMatcher instance that diffs the two sets of lines
    var sm = new difflib.SequenceMatcher(base, newtxt);

    // get the opcodes from the SequenceMatcher instance
    // opcodes is a list of 3-tuples describing what changes should be made to the base text
    // in order to yield the new text
    var opcodes = sm.get_opcodes();
    var diffoutputdiv = $("wadlOutput");
    while (diffoutputdiv.firstChild) diffoutputdiv.removeChild(diffoutputdiv.firstChild);
    //var contextSize = $("contextSize").value;
    //contextSize = contextSize ? contextSize : null;

    // build the diff view and add it to the current DOM
    var diffed_string = diffview.buildView({
	        baseTextLines: base,
	        newTextLines: newtxt,
	        opcodes: opcodes,
	        // set the display titles for each resource
	        baseTextName: "Base Text",
	        newTextName: "New Text",
	        //contextSize: null,
	        //viewType: $("comparisonDiffType").checked ? 1 : 0
	        viewType: viewingType
    	});
    //);

    if (process_mode == "compare"){
        $("#wadlOutput_compare").show();
        $("#left_wadl_output_compare").hide();
        $("#right_wadl_output_compare").hide();

        $("#wadlOutput_compare").html( diffed_string );
    } else if (process_mode == "crossServiceCompare"){

    }


}
