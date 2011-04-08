package com.google.codes.dryvalidator;

import java.util.List;

import junit.framework.Assert;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.ie.InternetExplorerDriver;

public class BrowserVerify {
	@BeforeClass
	public static void ウェブサーバを起動する() throws Exception{
		Server server = new Server();
		SelectChannelConnector connector = new SelectChannelConnector();
		connector.setPort(8888);
		server.addConnector(connector);

		ResourceHandler handler = new ResourceHandler();
		handler.setDirectoriesListed(true);
		handler.setResourceBase(".");

		server.setHandler(handler);
		server.start();
	}

	@Test
	public void ブラウザテスト() {
		WebDriver driver = new InternetExplorerDriver();
		driver.get("http://127.0.0.1:8888/src/test/resources/validation.html");
		driver.findElement(By.name("familyName")).sendKeys("長い文字");
		driver.findElement(By.name("firstName")).sendKeys("長い文字");
		driver.findElement(By.id("doValidate")).click();
		List<WebElement> messages = driver.findElement(By.id("messageArea")).findElements(By.tagName("li"));
		Assert.assertEquals(1, messages.size());
		Assert.assertEquals("特別なチェック", messages.get(0).getText());

		driver.findElement(By.name("familyName")).clear();
		driver.findElement(By.name("familyName")).sendKeys("hahaha");
		driver.findElement(By.id("doValidate")).click();
		messages = driver.findElement(By.id("messageArea")).findElements(By.tagName("li"));
		Assert.assertEquals(2, messages.size());
		Assert.assertEquals("氏名は全角文字で入力してください。", messages.get(0).getText());
		driver.quit();
	}
}
