package com.google.codes.dryvalidator;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.google.codes.dryvalidator.dto.FormItem;

public class ValidationEngine {
	Context ctx;
	ScriptableObject global;
	Script getValidatorScript;
	Script doValidateScript;

	public static class Console {
		public void log(Object obj) {
			System.out.println(Context.toString(obj));
		}
	}
	public ValidationEngine setup() {
		ContextFactory contextFactory = ContextFactory.getGlobal();
		ctx = contextFactory.enterContext();
		global = ctx.initStandardObjects();
		try {
			loadScript("com/google/codes/dryvalidator/joose.js");
			loadScript("com/google/codes/dryvalidator/dry-validator.js");
			ScriptableObject.putProperty(global, "console", Context.javaToJS(
					new Console(), global));
			ctx.evaluateString(global, "var validators = {};", "<cmd>", 1, null);
		} catch (IOException e) {
			throw new ResourceNotFoundException(e);
		}
		return this;
	}
	
	public Context getContext() {
		return this.ctx;
	}
	
	public ScriptableObject getGlobalScope() {
		return this.global;
	}
	public void register(FormItem formItem) {
		Scriptable local = ctx.newObject(global);
		local.setPrototype(global);
		local.setParentScope(null);
		Object wrappedValidation = Context.javaToJS(formItem, local);
		ScriptableObject.putProperty(local, "formItem", wrappedValidation);
		getValidator().exec(ctx, local);
	}

	public Map<String, List<String>> exec(Map<String, Object> formValues) {
		Map<String, List<String>> messages = new HashMap<String, List<String>>();
		for(Map.Entry<String, Object> e : formValues.entrySet()) {
			List<String> messagesByItem = exec(e.getKey(), e.getValue());
			if(messagesByItem != null && !messagesByItem.isEmpty())
				messages.put(e.getKey(), messagesByItem);
		}
		return messages;
	}

	public List<String> exec(String id, Object value) {
		Scriptable local = ctx.newObject(global);
		local.setPrototype(global);
		local.setParentScope(null);
		ScriptableObject.putProperty(local, "id", Context.javaToJS(id,
				local));
		ScriptableObject.putProperty(local, "value", Context.javaToJS(value,
				local));

		Object obj = doValidate().exec(ctx, local);

		List<String> messages = new ArrayList<String>();
		if (obj instanceof NativeArray) {
			NativeArray array = (NativeArray) obj;
			for (int i = 0; i < array.getLength(); i++) {
				String msg = Context.toString(array.get(i, local));
				messages.add(msg);
			}
		}
		return messages;
	}

	private Script doValidate() {
		if(doValidateScript == null) {
			doValidateScript = ctx.compileString("validators[id].validate(value);",
					"<cmd>", 1, null);
		}
		return doValidateScript;
	}
	private Script getValidator() {
		if (getValidatorScript == null) {
			getValidatorScript = ctx.compileString(
				"var validation = {label: formItem.label};\n"
				+ "Joose.A.each(formItem.validations.validation, function(v) { validation[v.name] = v.value; });\n"
				+ "validators[formItem.id] = DRYValidator.CompositeValidator.make(validation);\n"
				, "<cmd>", 1, null);
		}
		return getValidatorScript;
	}

	public void loadScript(String path) throws IOException {
		InputStreamReader in = null;

		try {
			in = new InputStreamReader(Thread.currentThread()
					.getContextClassLoader().getResourceAsStream(path), "UTF-8");
			ctx.evaluateReader(global, in, path, 0, null);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	public void dispose() {
		if (ctx != null) {
			Context.exit();
		}
	}

	public void unregisterAll() {
		ctx.evaluateString(global, "validators = {};", "<cmd>", 1, null);
	}

}
