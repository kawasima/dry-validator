package com.google.codes.dryvalidator;

import java.util.List;
import java.util.UUID;

import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

import com.google.codes.dryvalidator.dto.FormItem;
import com.google.codes.dryvalidator.dto.Validation;


public class ValidationEngineTest {
	@Test
	public void exec() {
		ValidationEngine validationEngine = new ValidationEngine();
		try {
			validationEngine.setup();
			for(int i=0; i<1; i++) {
				validationEngine.register(getFormItem());
				List<String> messages = validationEngine.exec("NAME", "A1- ");
				System.out.println(messages);
				validationEngine.unregisterAll();
			}
		} finally {
			validationEngine.dispose();
		}
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
	public void characterClassを動かす() {
		ValidationEngine validationEngine = new ValidationEngine();
		try {
			validationEngine.setup();
			Context ctx = validationEngine.getContext();
			ScriptableObject scope = validationEngine.getGlobalScope();

			Object obj = ctx.evaluateString(scope,
				"DRYValidator.CharacterClass.enable(['Hiragana', 'Katakana', 'Unknown']);"
				+ "console.log('enable='+DRYValidator.CharacterClass.enable());"
					, "<cmd>", 1, null);
		} finally {
			validationEngine.dispose();
		}
	}
}
