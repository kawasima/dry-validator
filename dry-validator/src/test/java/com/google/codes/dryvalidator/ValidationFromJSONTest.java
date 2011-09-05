package com.google.codes.dryvalidator;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.codes.dryvalidator.dto.FormItem;
import com.google.codes.dryvalidator.dto.Validation;

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
	
	private FormItem getFormItem() {
		FormItem formItem = new FormItem();
		formItem.setId("NAME");
		formItem.setLabel("氏名");
    	formItem.getValidations().addValidation(new Validation("required", "true"));
    	formItem.getValidations().addValidation(new Validation("maxLength", "10"));
    	formItem.getValidations().addValidation(new Validation("letterType", "Alnum+Punct"));
    	return formItem;
	}	

	@Test
	public void test() {
		validationEngine.register(getFormItem());
		List<String> messages = validationEngine.exec("NAME", "A1- ");
		System.out.println(messages);
		validationEngine.unregisterAll();
	}
}
