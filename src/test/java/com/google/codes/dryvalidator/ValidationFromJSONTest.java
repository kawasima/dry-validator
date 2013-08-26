package com.google.codes.dryvalidator;

import junit.framework.Assert;
import net.arnx.jsonic.JSON;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ValidationFromJSONTest {
	protected static ValidationEngine validationEngine;
	@BeforeClass
	public static void initializeValidationEngine() {
		validationEngine = new ValidationEngine();
	}

	@AfterClass
	public static void disposeValidationEngine() {
		validationEngine.dispose();
	}

	private void register() throws IOException {
		String json = FileUtils.readFileToString(new File("src/test/resources/validate.json"), "UTF-8");
        Context context = Context.enter();
        try {
            ScriptableObject scope = validationEngine.getGlobalScope();
            context.evaluateString(scope, "var defs = ("+ json +");" +
                    "_.each(_.pairs(defs), function(pair) { executor.addValidator(pair[0], DRYValidator.CompositeValidator.make(pair[1])) });", "<cmd>", 1, null);
        } finally {
            Context.exit();
        }
	}

	@Test
    public void test() throws IOException, InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        for (int i=0; i<10; i++) {
            executorService.execute(new Runnable() {
                public void run() {
                    try {
                        validationEngine.setup();
                        for(int j=0; j<5; j++)
                            _test();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

	private void _test() throws IOException {
		register();
		List<String> messages = validationEngine.exec("familyName", "A1- ");

        int childNameLength = RandomUtils.nextInt(9) + 1;
        String childName = RandomStringUtils.randomAlphabetic(childNameLength);

		@SuppressWarnings("rawtypes")
		Map formValues = JSON.decode("{"
				+ "\"familyName\": \"01234567890\","
				+ "\"children\": ["
				+   "{\"name\": \"" + childName + "\"}"
				+ "]}");

		@SuppressWarnings("unchecked")
		Map<String, List<String>> messages2 = validationEngine.exec(formValues);
		Assert.assertNotNull(messages2.get("children[0].name"));
        if (childNameLength > 5) {
            Assert.assertEquals(1, messages2.get("children[0].name").size());
            Assert.assertTrue("繰り返しのメッセージ", messages2.get("children[0].name").get(0).startsWith("1人目"));
        } else {
            Assert.assertEquals(0, messages2.get("children[0].name").size());

        }
		validationEngine.unregisterAll();
	}
}
