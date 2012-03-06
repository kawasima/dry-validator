var form = new DRYValidator.Form();
form.setup("sampleForm");
for(var key in form.formItems) {
	console.log(key);
}