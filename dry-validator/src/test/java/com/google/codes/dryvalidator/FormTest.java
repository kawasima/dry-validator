package com.google.codes.dryvalidator;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

public class FormTest {
	Context ctx;
	ScriptableObject global;
	@Before
	public void setUp() {
		ctx = Context.enter();
		global = ctx.initStandardObjects();
	}
	@Test
	public void test() throws IOException {
		loadScript("com/google/codes/dryvalidator/joose.js");
		loadScript("com/google/codes/dryvalidator/dry-validator.js");
		ctx.evaluateString(global, "", "<cmd>", 1, null);
	}

	@After
	public void tearDown() {
		Context.exit();
	}

	private void loadScript(String path) throws IOException {
		Reader in = null;
		try {
			in = new InputStreamReader(FileUtils.openInputStream(
					new File("src/main/resources/" + path)));
			ctx.evaluateReader(global, in, path, 1, null);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}
}
