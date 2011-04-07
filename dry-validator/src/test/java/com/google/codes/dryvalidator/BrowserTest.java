package com.google.codes.dryvalidator;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.ie.InternetExplorerDriver;

public class BrowserTest {
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
		driver.findElement(By.id("familyName")).sendKeys("長い文字");
		driver.findElement(By.id("firstName")).sendKeys("長い文字");
		WebElement element = driver.findElement(By.id("doValidate"));
		element.click();
		driver.quit();
	}
}
