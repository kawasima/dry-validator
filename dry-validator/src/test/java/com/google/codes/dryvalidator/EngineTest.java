package com.google.codes.dryvalidator;

import org.junit.Test;


public class EngineTest {
	@Test
	public void exec() {
		Engine engine = new Engine();
		try {
			engine.setUp();
			for(int i=0; i<1; i++) {
				engine.exec(getFormItem(), "あいうえおかきくけこさ4f");
			}
		} finally {
			engine.dispose();
		}
	}
	
	private FormItem getFormItem() {
		FormItem formItem = new FormItem();
		formItem.setLabel("氏名");
    	formItem.getValidations().addValidation(new Validation("required", "true"));
    	formItem.getValidations().addValidation(new Validation("maxLength", "10"));
    	formItem.getValidations().addValidation(new Validation("letterType", "Zenkaku"));
    	return formItem;
	}

}
