function runSampleAnalysis(testNumber){
	if (testNumber == 1){
		removeAllURLFields("A");
		$('#urlInputA' + '_'+"0").val("https://api.github.com/users");
		analyzeBtn();
	} else if (testNumber == 2){
		removeAllURLFields("A");
		$('#urlInputA' + '_'+"0").val("https://api.github.com/users/penguinsource");
		addURLField();
		$('#urlInputA' + '_'+"1").val("https://api.github.com/users/fokaefs");
		addURLField();
		$('#urlInputA' + '_'+"2").val("https://api.github.com/users/lorencs");
		analyzeBtn();
	} else if (testNumber == 3){
		removeAllURLFields("A");
		$('#urlInputA' + '_'+"0").val("https://graph.facebook.com/oprescu3");
		addURLField();
		$('#urlInputA' + '_'+"1").val("https://graph.facebook.com/jack");
		addURLField();
		$('#urlInputA' + '_'+"2").val("https://graph.facebook.com/seinfeld");
		addURLField();
		$('#urlInputA' + '_'+"3").val("https://graph.facebook.com/arresteddevelopment");
		analyzeBtn();
	} else if (testNumber == 4){
		removeAllURLFields("A");
		$('#urlInputA' + '_'+"0").val("http://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=HeyThere");
		analyzeBtn();
	} else if (testNumber == 5){
		removeAllURLFields("A");
		$('#urlInputA' + '_'+"0").val("http://api.openweathermap.org/data/2.1/find/city?lat=55&lon=37&cnt=10");
		analyzeBtn();
	}
}

function runSampleTest(process){
	console.debug("running sample test");
	if (process === 'analyze'){
		$('#urlInputA' + '_'+"0").val("https://api.github.com/users");
		analyzeBtn();
	} else if (process === 'compare'){
		$('#urlInputA' + '_'+"0").val("https://api.github.com/users/penguinsource/repos");
		$('#urlInputB' + '_'+"0").val("https://api.github.com/users/fokaefs/repos");
		compareBtn();
	} else if (process === 'crossServiceCompare'){
		$('#urlInputA' + '_'+"0").val("https://graph.facebook.com/oprescu3");
		$('#urlInputB' + '_'+"0").val("https://graph.facebook.com/jack");
		crossServiceCompareBtn();
	}
}
