package net.unit8.validator.dry;

import junit.framework.Assert;
import net.arnx.jsonic.JSON;
import net.unit8.validator.dry.ValidationEngine;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ValidationFromJSONTest {
	protected static ValidationEngine validationEngine;
	@BeforeClass
	public static void initializeValidationEngine() {
	}

	@AfterClass
	public static void disposeValidationEngine() {
		validationEngine.dispose();
	}

	@Test
    public void test() throws IOException, InterruptedException {
        validationEngine = new ValidationEngine().setup();
        String json = FileUtils.readFileToString(new File("src/test/resources/validate.json"), "UTF-8");
        validationEngine.register((Map)JSON.decode(json));

        ExecutorService executorService = Executors.newFixedThreadPool(100);
        for (int i=0; i<10000; i++) {
            executorService.execute(new Runnable() {
                public void run() {
                    try {
                        _test();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        executorService.awaitTermination(10, TimeUnit.SECONDS);
        validationEngine.unregisterAll();
    }

	private void _test() throws IOException {
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
        if (childNameLength > 5) {
            Assert.assertEquals(1, messages2.get("children[0].name").size());
            Assert.assertTrue("繰り返しのメッセージ", messages2.get("children[0].name").get(0).startsWith("1人目"));
        } else {
            Assert.assertNull(messages2.get("children[0].name"));

        }
	}
}
