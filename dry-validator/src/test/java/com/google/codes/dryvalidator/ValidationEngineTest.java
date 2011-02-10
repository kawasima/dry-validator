package com.google.codes.dryvalidator;

import java.util.List;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ScriptableObject;

import com.google.codes.dryvalidator.dto.FormItem;
import com.google.codes.dryvalidator.dto.Validation;


public class ValidationEngineTest {
	static ValidationEngine validationEngine;
	
	@BeforeClass
	public static void validationEngineを初期化する() {
		validationEngine = new ValidationEngine();
		validationEngine.setup();
	}

	@AfterClass
	public static void validationEngineを破棄する() {
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
	public void exec() {
		validationEngine.register(getFormItem());
		List<String> messages = validationEngine.exec("NAME", "A1- ");
		System.out.println(messages);
		validationEngine.unregisterAll();
	}

	@Test
	public void requiredのテスト() {
		FormItem formItem = new FormItem();
		formItem.setId("NAME");
		formItem.setLabel("氏名");
    	formItem.getValidations().addValidation(new Validation("required", "false"));
		validationEngine.register(formItem);
		Assert.assertTrue(validationEngine.exec("NAME", "").isEmpty());
		Assert.assertTrue(validationEngine.exec("NAME", null).isEmpty());
		Assert.assertTrue(validationEngine.exec("NAME", "abc").isEmpty());
		validationEngine.unregisterAll();
		
		formItem.getValidations().clear();
    	formItem.getValidations().addValidation(new Validation("required", "true"));
		validationEngine.register(formItem);
		Assert.assertTrue(!validationEngine.exec("NAME", "").isEmpty());
		Assert.assertTrue(!validationEngine.exec("NAME", null).isEmpty());
		Assert.assertTrue(validationEngine.exec("NAME", "abc").isEmpty());
		validationEngine.unregisterAll();
		

	}

	@Test
	public void maxLengthのテスト() {
		FormItem formItem = new FormItem();
		formItem.setId("NAME");
		formItem.setLabel("氏名");
    	formItem.getValidations().addValidation(new Validation("maxLength", "10"));
		validationEngine.register(formItem);
		// 未入力の場合はチェックしない
		Assert.assertTrue(validationEngine.exec("NAME", "").isEmpty());
		Assert.assertTrue(validationEngine.exec("NAME", null).isEmpty());
		
		//10文字以内はOK
		Assert.assertTrue(validationEngine.exec("NAME", "abc").isEmpty());
		
		// 10文字超えると駄目
		Assert.assertTrue(validationEngine.exec("NAME",  "0123456789").isEmpty());
		Assert.assertTrue(!validationEngine.exec("NAME", "01234567891").isEmpty());

		// 全角でも文字で数える
		Assert.assertTrue(validationEngine.exec("NAME",  "０１２３４５６７８９").isEmpty());
		Assert.assertTrue(!validationEngine.exec("NAME", "０１２３４５６７８９１").isEmpty());

		validationEngine.unregisterAll();
	}
	
	
	public void characterClassを動かす() {
		Context ctx = validationEngine.getContext();
		ScriptableObject scope = validationEngine.getGlobalScope();

		Object obj = ctx.evaluateString(scope,
			"DRYValidator.CharacterClass.enable(['Hiragana', 'Katakana', 'Unknown']);"
			+ "console.log('enable='+DRYValidator.CharacterClass.enable());"
				, "<cmd>", 1, null);
	}
	
	@Test
	public void selectionの動き() {
		FormItem formItem = new FormItem();
		formItem.setId("NAME");
		formItem.setLabel("氏名");
		
    	formItem.getValidations().addValidation(new Validation("Selection", "{min:1}"));
		validationEngine.register(formItem);
		// 未入力の場合はチェックしない
		Assert.assertFalse(validationEngine.exec("NAME", new String[]{""}).isEmpty());
		Assert.assertTrue(validationEngine.exec("NAME", new String[]{"asdf"}).isEmpty());
		Assert.assertFalse(validationEngine.exec("NAME", "").isEmpty());
		Assert.assertTrue(validationEngine.exec("NAME", "hoge").isEmpty());
		validationEngine.unregisterAll();
		
    	formItem.getValidations().addValidation(new Validation("Selection", "{max:2}"));
		validationEngine.register(formItem);
		Assert.assertTrue(validationEngine.exec("NAME", "hoge").isEmpty());
		Assert.assertFalse(validationEngine.exec("NAME", new String[]{"a","b","c"}).isEmpty());
		validationEngine.unregisterAll();
	}
}

