package com.google.codes.dryvalidator;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.mozilla.javascript.Script;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class Engine {
	Context ctx;
	ScriptableObject scope;
	Script getValidatorScript;

	
	public void setUp() {
        ContextFactory contextFactory = ContextFactory.getGlobal();
        ctx = contextFactory.enterContext();
    	scope = ctx.initStandardObjects();
    	try {
    		loadScript("joose.js");
    		loadScript("pf_validator.js");
            ScriptableObject.putProperty(scope, "console", Context.javaToJS(System.out,scope));
    	} catch(IOException e) {
    		
    	}
	}
	
	public void exec(FormItem formItem, String value) {
        Scriptable local = ctx.newObject(scope);
        Object wrappedValidation = Context.javaToJS(formItem , local);
        ScriptableObject.putProperty(local, "formItem", wrappedValidation);
        ScriptableObject.putProperty(local, "value", Context.javaToJS(value, local));
        Object obj = getValidator().exec(ctx, local);
        
        List<String> messages = new ArrayList<String>();
        if(obj instanceof NativeArray) {
        	NativeArray array = (NativeArray)obj;
        	for(int i=0; i<array.getLength(); i++) {
        		String msg = Context.toString(array.get(i, local));
        		messages.add(msg);
        	}
        }
        System.out.println(messages);
	}

	private Script getValidator() {
		if(getValidatorScript == null) {
			getValidatorScript = ctx.compileString(
				"var validation = {label: formItem.label};\n"
        		+ "Joose.A.each(formItem.validations.validation, function(v) { validation[v.name] = v.value; });\n"
        		+ "var cv = PF.CompositeValidator.make(validation);\n"
        		+ "cv.validate(value);"
			, "<cmd>", 1, null);
		}
		return getValidatorScript;
	}
	

	public void loadScript(String path) throws IOException {
        InputStreamReader in = null;

        try {
                in = new InputStreamReader(Thread.currentThread().getContextClassLoader().getResourceAsStream(path));
                ctx.evaluateReader(scope, in, path, 0, null);
        } finally {
                IOUtils.closeQuietly(in);
        }
    }
	public void dispose() {
		if(ctx != null) {
			Context.exit();
		}
	}
}
