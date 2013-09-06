package net.unit8.validator.dry;

import net.unit8.validator.dry.dto.FormItem;
import net.unit8.validator.dry.util.JavaToJsUtil;
import net.arnx.jsonic.JSON;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.mozilla.javascript.*;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validation engine.
 *
 * @author kawasima
 */
public class ValidationEngine {
	private static final String DIGEST_ALGORITHM = "SHA-1";

    private final ThreadLocal<Boolean> initialized = new ThreadLocal<Boolean>();
    private Scriptable engineScope;

    private static final ScriptableObject global;
    /** compiled script cache*/
    private static final ConcurrentHashMap<String, Script> scriptCache = new ConcurrentHashMap<String, Script>(
            10);

    private static NativeObject i18nResources;
    static {
        try {
            Context ctx = Context.enter();
            global = ctx.initStandardObjects();
            ScriptableObject.putProperty(global, "console", Context.javaToJS(
                    new Console(), global));
            loadScript("net/unit8/validator/dry/underscore.js");
            loadScript("net/unit8/validator/dry/i18next-1.6.3.js");
            loadScript("net/unit8/validator/dry/dry-validator.js");
        } finally {
            Context.exit();
        }
    }

	public static class Console {
        public static String inspect(Object obj) {
            if (obj instanceof NativeObject) {
                NativeObject nObj = (NativeObject)obj;
                List<String> properties = new ArrayList<String>(nObj.getAllIds().length);
                for (Object id : nObj.getAllIds()) {
                    properties.add(id + ": " + inspect(nObj.get(id)));
                }
                return "{" + StringUtils.join(properties, ",") + "}";
            } else if (obj instanceof NativeArray) {
                NativeArray arr = (NativeArray) obj;
                List<String> elements = new ArrayList<String>(arr.size());
                for (int i=0; i < arr.size(); i++) {
                    elements.add(inspect(arr.get(i)));
                }
                return "[" + StringUtils.join(elements, ",") + "]";
            } else if (obj instanceof Undefined) {
                return "undefined";
            } else {
                return Context.toString(obj);
            }
        }

		public void log(Object obj) {
            System.err.println(inspect(obj));
		}
	}

	public ValidationEngine() {
        try {
            Context ctx = Context.enter();
            engineScope = ctx.newObject(global);
            engineScope.setPrototype(global);
            engineScope.setParentScope(global);
        } finally {
            Context.exit();
        }
	}

	public ValidationEngine setup(String customScriptPath) {
        if (customScriptPath != null)
            loadScript(customScriptPath);

        executeScript("var executor = new DRYValidator.Executor();", engineScope);
		return this;
	}

	public ValidationEngine setup() {
		return setup(null);
	}

    public void setI18n(String i18nFilePath) {
        InputStream in = null;
        try {
            Context ctx = Context.enter();
            in = new FileInputStream(new File(i18nFilePath));
            Map json = JSON.decode(in, (new LinkedHashMap<String, Object>()).getClass().getGenericSuperclass());
            NativeObject messages = JavaToJsUtil.convert(json, ctx, global);
        } catch (IOException e) {
            throw new ResourceNotFoundException(e);
        } finally {
            IOUtils.closeQuietly(in);
            Context.exit();
        }

    }

    public void dispose() {}

	public void register() {
        Context ctx = Context.enter();
        try {
            Object wrappedValidation = Context.javaToJS("String", engineScope);
            ScriptableObject.putProperty(engineScope, "formItem", wrappedValidation);
            executeScript(
                    "var validation = { label: formItem.label, messageDecorator: formItem.messageDecorator };\n"
                            + "_.each(formItem.validations.validation, function(v) { validation[v.name] = v.value; });\n"
                            + "executor.addValidator(formItem.id, DRYValidator.CompositeValidator.make(validation));\n",
                    engineScope);
        } finally {
            Context.exit();
        }
	}

	public void register(FormItem formItem) {
        try {
            Context ctx = Context.enter();
            Object wrappedValidation = Context.javaToJS(formItem, engineScope);
            ScriptableObject.putProperty(engineScope, "formItem", wrappedValidation);
            executeScript(
                    "var validation = { label: formItem.label, messageDecorator: formItem.messageDecorator };\n"
                            + "_.each(formItem.validations.validation, function(v) { validation[v.name] = v.value; });\n"
                            + "executor.addValidator(formItem.id, DRYValidator.CompositeValidator.make(validation));\n",
                    engineScope);
        } finally {
            Context.exit();
        }
	}

    public void register(Map json) {
        try {
            Context ctx = Context.enter();
            NativeObject object = JavaToJsUtil.convert(json, ctx, engineScope);
            ScriptableObject.putProperty(engineScope, "validations", object);
            ScriptableObject.putProperty(engineScope, "validators", ctx.newObject(engineScope));
            executeScript(
                    "executor.validators = validators;_.chain(validations).pairs().each(function(pair) {" +
                            "executor.addValidator(pair[0], DRYValidator.CompositeValidator.make(pair[1]));" +
                            "});",
                    engineScope);
        } finally {
            Context.exit();
        }
    }

    public Scriptable getGlobalScope() {
        return global;
    }
	public Map<String, List<String>> exec(Map<String, Object> formValues) {
        try {
            Context ctx = Context.enter();
            Scriptable local = ctx.newObject(engineScope);
            local.setPrototype(engineScope);
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
        Context ctx = Context.enter();
        try {
            Scriptable local = ctx.newObject(engineScope);
            local.setPrototype(engineScope);
            local.setParentScope(null);

            ScriptableObject.putProperty(local, "id", Context.javaToJS(id, local));
            ScriptableObject.putProperty(local, "value", Context.javaToJS(value, local));

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
        try {
            Context ctx = Context.enter();
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

	public static void loadScript(String path) throws ResourceNotFoundException {
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
                } catch (IOException e) {
                    throw new ResourceNotFoundException(e);
                } finally {
                    IOUtils.closeQuietly(in);
                }
            }
            script.exec(ctx, global);
        } finally {
            Context.exit();
        }
	}

	public void unregisterAll() {
        Context ctx = Context.enter();
        try {
            ctx.evaluateString(engineScope, "executor.validators = {};", "<cmd>", 1, null);
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
            for (byte aDigest : digest) {
                int d = aDigest & 0xff;
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
