package com.google.codes.dryvalidator;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.Field;
import org.apache.commons.validator.Form;
import org.apache.commons.validator.ValidatorResources;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import com.google.codes.dryvalidator.dto.FormItem;
import com.google.codes.dryvalidator.dto.Validation;

public class ValidationFromStruts {
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

	@Test
	public void test() throws IOException, SAXException {
		InputStream in = this.getClass().getResourceAsStream("/validation.xml");
		ValidatorResources resources = new ValidatorResources(in);
		Form form = resources.getForm(Locale.JAPANESE, "MemberForm");
		for(Field field : (List<Field>)form.getFields()) {
			FormItem formItem = new FormItem();
			formItem.setId(field.getProperty());
			formItem.setLabel(field.getKey());
			for(String depend : (List<String>)field.getDependencyList()) {
				String value = null;
				if(StringUtils.equals(depend, "required")) {
					value="true";
				} else {
					value=field.getVarValue(depend);
				}
				if(StringUtils.equals(depend, "maxlength")) {
					depend = "maxLength";
				}
				Validation validation = new Validation(depend, value);
				formItem.getValidations().addValidation(validation);
			}
			System.out.println(formItem);
			validationEngine.register(formItem);
		}

		List<String> messages = validationEngine.exec("familyName", "0123456789");
		System.out.println(messages);
		validationEngine.unregisterAll();

	}
}
