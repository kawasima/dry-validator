package com.google.codes.dryvalidator;

import com.google.codes.dryvalidator.dto.FormItem;
import com.google.codes.dryvalidator.util.JavaToJsUtil;
import net.arnx.jsonic.JSON;
import org.apache.commons.io.IOUtils;
import org.mozilla.javascript.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validation engine.
 *
 * @author kawasima
 */
public class ValidationEngine {
	private static final String DIGEST_ALGORITHM = "SHA-1";

	Script getValidatorScript;
	Script doValidateScript;
    ThreadLocal<Boolean> initialized = new ThreadLocal<Boolean>();
    ThreadLocal<ScriptableObject> global = new ThreadLocal<ScriptableObject>();

	/** compiled script cache*/
	static final ConcurrentHashMap<String, Script> scriptCache = new ConcurrentHashMap<String, Script>(
			10);

	public static class Console {
		public void log(Object obj) {
			System.out.println(Context.toString(obj));
		}
	}

	public ValidationEngine() {
	}

	public ValidationEngine setup(String customScriptPath) {
        if (isInitialized())
            return this;

		Context ctx = Context.enter();
		global.set(ctx.initStandardObjects());
        initialized.set(Boolean.TRUE);
		try {
			loadScript("com/google/codes/dryvalidator/underscore.js");
			loadScript("com/google/codes/dryvalidator/dry-validator.js");
			if (customScriptPath != null)
				loadScript(customScriptPath);

			ScriptableObject.putProperty(global.get(), "console", Context.javaToJS(
					new Console(), global.get()));
			executeScript("var executor = new DRYValidator.Executor();", global.get());
		} catch (IOException e) {
			throw new ResourceNotFoundException(e);
		} finally {
            if (ctx != null)
                Context.exit();
        }
		return this;
	}

	public ValidationEngine setup() {
		return setup(null);
	}

    public void dispose() {}

	public void register() {
        if (!isInitialized())
            setup();

        Context ctx = Context.enter();
        try {
            Scriptable local = ctx.newObject(global.get());
            local.setPrototype(global.get());
            local.setParentScope(null);
            Object wrappedValidation = Context.javaToJS("String", local);
            ScriptableObject.putProperty(local, "formItem", wrappedValidation);
            executeScript(
                    "var validation = { label: formItem.label, messageDecorator: formItem.messageDecorator };\n"
                            + "_.each(formItem.validations.validation, function(v) { validation[v.name] = v.value; });\n"
                            + "executor.addValidator(formItem.id, DRYValidator.CompositeValidator.make(validation));\n",
                    local);
        } finally {
            Context.exit();
        }
	}

	public void register(FormItem formItem) {
        if (!isInitialized())
            setup();

        Context ctx = Context.enter();
        try {
            Scriptable local = ctx.newObject(global.get());
            local.setPrototype(global.get());
            local.setParentScope(null);
            Object wrappedValidation = Context.javaToJS(formItem, local);
            ScriptableObject.putProperty(local, "formItem", wrappedValidation);
            executeScript(
                    "var validation = { label: formItem.label, messageDecorator: formItem.messageDecorator };\n"
                            + "_.each(formItem.validations.validation, function(v) { validation[v.name] = v.value; });\n"
                            + "executor.addValidator(formItem.id, DRYValidator.CompositeValidator.make(validation));\n",
                    local);
        } finally {
            Context.exit();
        }
	}

    public void register(Map json) {
        if (!isInitialized())
            setup();

        Context ctx = Context.enter();
        try {
            Scriptable local = ctx.newObject(global.get());
            local.setPrototype(global.get());
            local.setParentScope(null);
            NativeObject object = JavaToJsUtil.convert(json, ctx, local);
            ScriptableObject.putProperty(local, "validations", object);
            executeScript(
                    "_.chain(validations).pairs().each(function(pair) {" +
                            "executor.addValidator(pair[0], DRYValidator.CompositeValidator.make(pair[1]));" +
                            "});",
                    local);
        } finally {
            Context.exit();
        }
    }

    public ScriptableObject getGlobalScope() {
        return global.get();
    }
	public Map<String, List<String>> exec(Map<String, Object> formValues) {
        if (!isInitialized())
            setup();

        Context ctx = Context.enter();
        try {
            Scriptable local = ctx.newObject(global.get());
            local.setPrototype(global.get());
            local.setParentScope(null);

            NativeObject formValuesObj = NativeObject.class.cast(ctx.newObject(local));
            for (Map.Entry<String, Object> entry : formValues.entrySet()) {
                Object value = null;
                if (entry.getValue() == null || entry.getValue() instanceof String
                        || entry.getValue() instanceof Number || entry.getValue() instanceof Boolean ) {
                    value = entry.getValue();
                } else {
                    value = ctx.evaluateString(local, "(" + JSON.encode(entry.getValue()) + ")", "<JSON>", 1, null);
                }
                formValuesObj.defineProperty(
                        entry.getKey(),
                        value,
                        NativeObject.READONLY);
            }
            ScriptableObject.putProperty(local, "values", formValuesObj);
            Object obj = executeScript("executor.execute(values)", local);

            Map<String, List<String>> messages = new HashMap<String, List<String>>();
            if (obj instanceof NativeObject) {
                NativeObject nobj = NativeObject.class.cast(obj);
                nobj.entrySet();
                for (Map.Entry<Object, Object> e : nobj.entrySet()) {
                    if (e.getValue() instanceof NativeArray) {
                        NativeArray values = NativeArray.class.cast(e.getValue());
                        List<String> thisMessages = new ArrayList<String>();
                        for (int i = 0; i < values.getLength(); i++) {
                            thisMessages.add(Context.toString(values.get(i, local)));
                        }
                        messages.put(e.getKey().toString(), thisMessages);
                    }
                }
            }

            return messages;
        } finally {
            Context.exit();
        }
	}

	public List<String> exec(String id, Object value) {
        if (!isInitialized())
            setup();

        Context ctx = Context.enter();
        try {
            Scriptable local = ctx.newObject(global.get());
            local.setPrototype(global.get());
            local.setParentScope(null);
            ScriptableObject.putProperty(local, "id", Context.javaToJS(id, local));
            ScriptableObject.putProperty(local, "value", Context.javaToJS(value,
                    local));

            Object obj = executeScript("executor.validators[id].validate(value);", local);

            List<String> messages = new ArrayList<String>();
            if (obj instanceof NativeArray) {
                NativeArray array = (NativeArray) obj;
                for (int i = 0; i < array.getLength(); i++) {
                    String msg = Context.toString(array.get(i, local));
                    messages.add(msg);
                }
            }
            return messages;
        } finally {
            Context.exit();
        }
	}

	private Object executeScript(String scriptText, Scriptable scope) {
        if (!isInitialized())
            setup();

        Context ctx = Context.enter();
        try {
            String cacheKey = digest(scriptText);
            Script script = scriptCache.get(cacheKey);
            if (script == null) {
                script = ctx.compileString(scriptText, "<cmd>", 1, null);
                scriptCache.putIfAbsent(cacheKey, script);
            }
            return script.exec(ctx, scope);
        } finally {
            Context.exit();
        }
	}

	public void loadScript(String path) throws IOException {
        if (!isInitialized())
            setup();

        Context ctx = Context.enter();
        try {
            Script script = scriptCache.get(path);
            if (script == null) {
                InputStreamReader in = null;

                try {
                    in = new InputStreamReader(Thread.currentThread()
                            .getContextClassLoader().getResourceAsStream(path),
                            "UTF-8");
                    script = ctx.compileReader(in, path, 0, null);
                    scriptCache.putIfAbsent(path, script);
                } finally {
                    IOUtils.closeQuietly(in);
                }
            }
            script.exec(ctx, global.get());
        } finally {
            Context.exit();
        }
	}

	public void unregisterAll() {
        if (!isInitialized())
            setup();

        Context ctx = Context.enter();
        try {
            ctx.evaluateString(global.get(), "executor.validators = {};", "<cmd>", 1, null);
        } finally {
            Context.exit();
        }
	}

	private String digest(String input) {
		StringBuilder sb = new StringBuilder();
		try {
			MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
			md.reset();
			md.update(input.getBytes());
			byte[] digest = md.digest();
			for (int i = 0; i < digest.length; i++) {
				int d = digest[i] & 0xff;
				String hex = Integer.toHexString(d);
				if (hex.length() == 1) {
					sb.append("0");
				}
				sb.append(hex);
			}
		} catch (NoSuchAlgorithmException e) {
			// ignore
		}
		return sb.toString();
	}

    private boolean isInitialized() {
        return initialized.get() != null && initialized.get();
    }
}
