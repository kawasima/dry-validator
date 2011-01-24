package com.google.codes.dryvalidator;

import java.util.UUID;

import org.junit.Test;


public class EngineTest {
	@Test
	public void exec() {
		Engine engine = new Engine();
		try {
			engine.setUp();
			engine.register(getFormItem());
			for(int i=0; i<10000; i++) {
				engine.exec("NAME", UUID.randomUUID().toString());
				
			}
		} finally {
			engine.dispose();
		}
	}
	
	private FormItem getFormItem() {
		FormItem formItem = new FormItem();
		formItem.setId("NAME");
		formItem.setLabel("氏名");
    	formItem.getValidations().addValidation(new Validation("required", "true"));
    	formItem.getValidations().addValidation(new Validation("maxLength", "10"));
    	formItem.getValidations().addValidation(new Validation("letterType", "Zenkaku"));
    	return formItem;
	}

}
