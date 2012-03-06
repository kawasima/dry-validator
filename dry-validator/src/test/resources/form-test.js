var form = new DRYValidator.Form();
form.setup("sampleForm");

Object.prototype.toString = function () {
	var props = [];
	for(key in this)
		props.push(key+":"+this[key]);
	return "{" + props.join(",") + "}"

}

for(var key in form.formItems) {
	console.log(key + "=" + form.formItems[key]);
}

