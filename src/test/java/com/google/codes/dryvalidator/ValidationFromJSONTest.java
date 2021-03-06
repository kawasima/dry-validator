package com.google.codes.dryvalidator;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import net.arnx.jsonic.JSON;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class ValidationFromJSONTest {
	protected static ValidationEngine validationEngine;
	@BeforeClass
	public static void initailizeValidationEngine() {
		validationEngine = new ValidationEngine();
		validationEngine.setup();
	}

	@AfterClass
	public static void disposeValidationEngine() {
		validationEngine.dispose();
	}

	private void register() throws IOException {
		String json = FileUtils.readFileToString(new File("src/test/resources/validate.json"), "UTF-8");
		Context context = validationEngine.getContext();
		ScriptableObject scope = validationEngine.getGlobalScope();
		context.evaluateString(scope, "var defs = ("+ json +");" +
				"_.each(_.pairs(defs), function(pair) { executor.addValidator(pair[0], DRYValidator.CompositeValidator.make(pair[1])) });", "<cmd>", 1, null);
	}

	@Test
	public void test() throws IOException {
		register();
		List<String> messages = validationEngine.exec("familyName", "A1- ");
		System.out.println(messages);

		@SuppressWarnings("rawtypes")
		Map formValues = JSON.decode("{"
				+ "\"familyName\": \"01234567890\","
				+ "\"children\": ["
				+   "{\"name\": \"\"}"
				+ "]}");

		@SuppressWarnings("unchecked")
		Map<String, List<String>> messages2 = validationEngine.exec(formValues);
		Assert.assertNotNull(messages2.get("children[0].name"));
		Assert.assertEquals(1, messages2.get("children[0].name").size());
        System.err.println(messages2.get("children[0].name").get(0));
		Assert.assertTrue("繰り返しのメッセージ", messages2.get("children[0].name").get(0).startsWith("1人目"));
		validationEngine.unregisterAll();
	}
}
