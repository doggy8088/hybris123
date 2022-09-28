package com.hybris.hybris123.runtime.helper;
/*
 * © 2017 SAP SE or an SAP affiliate company.
 * All rights reserved.
 * Please see http://www.sap.com/corporate-en/legal/copyright/index.epx for additional trademark information and
 * notices.
 */

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hybris.hybris123.runtime.tests.Hybris123Tests;
import com.paulhammant.ngwebdriver.ByAngular;

import io.github.bonigarcia.wdm.WebDriverManager;

public class SeleniumHelper {
	private static final Logger LOG = LoggerFactory.getLogger(SeleniumHelper.class);
	private static WebDriver dvr = null;

	private static final String yUSERNAME = "admin";
	private static final String yPASSWORD = System.getenv("INITIAL_ADMIN");

	private static final int PAUSE_MS = 2000;
	private static final int PAUSE_FOR_SERVER_START_MS  = 120000;
	private static final int PAUSE_BETWEEN_KEYS_MS = 50;
	private static final int NORMAL_WAIT_S = 10;
	private static final int LONG_WAIT_S = 240;
	private static final int BUILD_WAIT_S = 60 * 60;
	private static final int POLLING_RATE_S = 2;

	private static Wait<WebDriver> wait;
	private static Wait<WebDriver> longWait;
	private static Wait<WebDriver> buildWait;

	private static final boolean WINDOWS = System.getProperty("os.name")!=null && System.getProperty("os.name").toLowerCase().contains("windows");
	private static final boolean OSX = System.getProperty("os.name")!=null && System.getProperty("os.name").toLowerCase().contains("mac");

	// disable default constructor
	private SeleniumHelper() {}

	public static boolean canLoginToHybrisCommerce()  {
		try {
			String url;
			int version = 0;

			if ( VersionHelper.getVersion().equals("UNDEFINED")) {
				url = "https://localhost:9002/login.jsp";
			} else {
				version = Integer.parseInt(VersionHelper.getVersion().toString().substring(1));

				if (version < 2005) {
					url = "https://localhost:9002/login.jsp";
				} else {
					url = "https://localhost:9002/login.jsp";
				}
			}

			waitForConnectionToOpen(url, PAUSE_FOR_SERVER_START_MS);
		 	getDriver().get(url);

			pauseMS();
			WebElement usernameElem = findElement(By.name("j_username"));
			WebElement passwordElem = findElement(By.name("j_password"));

			clearField(usernameElem);
			usernameElem.sendKeys(yUSERNAME);
			clearField(passwordElem);
			passwordElem.sendKeys(yPASSWORD);
			pauseMS();
			passwordElem.submit();
			Assert.assertTrue(waitFor("div", "Memory overview"));

			return true;
		} catch (Exception e) {
			if (!getDriver().findElements(By.xpath("//button[text()='Initialize']")).isEmpty()) {
				// sometimes login menu does not appear on Unix
				return true;
			} else {
				StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
				String callingMethod = stackTraceElements[2].getMethodName();
				Hybris123Tests.fail(callingMethod, "Connect Exception: " + e.getMessage());

				return false;
			}
		}
	}

	public static boolean checkIsOnHybrisCommerce()  {
		try {
			String url;
			int version = 0;

			if ( VersionHelper.getVersion().equals("UNDEFINED")) {
				url = "https://localhost:9002/login.jsp";
			} else {
				version = Integer.parseInt(VersionHelper.getVersion().toString().substring(1));

				if (version < 2005) {
					url = "https://localhost:9002/login.jsp";
				} else {
					url = "https://localhost:9002/login.jsp";
				}
			}

			waitForConnectionToOpen(url, PAUSE_FOR_SERVER_START_MS);
		 	getDriver().get(url);

			pauseMS();
			WebElement usernameElem = findElement(By.name("j_username"));

			return true;
		} catch (Exception e) {
			if (!getDriver().findElements(By.xpath("//button[text()='Initialize']")).isEmpty()) {
				// sometimes login menu does not appear on Unix
				return true;
			} else {
				StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
				String callingMethod = stackTraceElements[2].getMethodName();
				Hybris123Tests.fail(callingMethod, "Connect Exception: " + e.getMessage());

				return false;
			}
		}
	}

	public static boolean loginToBackOffice(String ... language) {
		try {
			Map<String, String> logins = Map.of("Deutsch", "Anmelden");
			String sLogin = "Login";
			pauseMS(PAUSE_MS);
			// Allow time for server to start
			waitForConnectionToOpen("https://localhost:9002/backoffice", PAUSE_FOR_SERVER_START_MS);
			getDriver().get("https://localhost:9002/backoffice");
			// wait to prevent StaleElementReferenceException
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//select")));
			if (language.length == 0){
				Select s = new Select(findElement(By.xpath("//select")));
				s.selectByVisibleText("English");
			}
			if (language.length == 1){
				Select s = new Select(findElement(By.xpath("//select")));
				s.selectByVisibleText(language[0]);
				sLogin = logins.get(language[0]);
			}
			pauseMS(PAUSE_MS);

			WebElement un = findElement(By.name("j_username"));
			clearField(un, yUSERNAME + Keys.TAB);

			WebElement pwd = findElement(By.name("j_password"));
			clearField(pwd, yPASSWORD + Keys.TAB);
			pauseMS(PAUSE_MS);
			waitForThenClickButtonWithText(sLogin);
			return true;
		} catch (Exception e) {
			// debug
			LOG.error("Login to Backoffice failed.", e);
			String callingMethod =Thread.currentThread().getStackTrace()[2].getMethodName();
			Hybris123Tests.fail(callingMethod, "Connect Exception");
		}
		return false;
	}

	public static String copyCloudAdminPassword(String envName) throws IOException, UnsupportedFlavorException {
		waitForThenClick("a", "Environments");
		waitForThenClick("a", envName);
		waitForThenAndClickSpan("View Configurations");
		waitForThenAndClickSpan("hcs_admin");
		WebElement adminMenu = findElements(By.tagName("mat-list-item")).get(0);
		adminMenu.findElements(By.tagName("button")).get(0).click();
		waitForThenAndClickSpan("Copy to Clipboard");
		waitFor("span", "Success");
		WebElement closeButton = findElements(By.className("mat-dialog-container-close")).get(0);
		closeButton.click();

		String cloudPassword = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
		return cloudPassword;
	}

	public static boolean loginToCloudBackOffice(String envNumber, String password, String language) {
		try {
			pauseMS(PAUSE_MS);
			// Allow time for server to start
			waitForConnectionToOpen("https://backoffice.cqz1m-softwarea1-" + envNumber + "-public.model-t.cc.commerce.ondemand.com/backoffice/login.zul", PAUSE_FOR_SERVER_START_MS);
			getDriver().get("https://backoffice.cqz1m-softwarea1-" + envNumber + "-public.model-t.cc.commerce.ondemand.com/backoffice/login.zul");
			// wait to prevent StaleElementReferenceException
			wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//select")));
			if (language == null){
				Select s = new Select(findElement(By.xpath("//select")));
				s.selectByVisibleText("English");
			} else {
				Select s = new Select(findElement(By.xpath("//select")));
				s.selectByVisibleText(language);
			}
			pauseMS(PAUSE_MS);

			WebElement un = findElement(By.name("j_username"));
			clearField(un, yUSERNAME + Keys.TAB);

			WebElement pwd = findElement(By.name("j_password"));
			clearField(pwd, password + Keys.TAB);
			pauseMS(PAUSE_MS);
			pwd.submit();
			return true;
		} catch (Exception e) {
			// debug
			LOG.error("Login to Backoffice failed.", e);
			String callingMethod =Thread.currentThread().getStackTrace()[2].getMethodName();
			Hybris123Tests.fail(callingMethod, "Connect Exception");
		}
		return false;
	}

	public static void searchForConcertInBackoffice() {
		try {
			waitForThenDoBackofficeSearch("Concert");
		}
		catch (WebDriverException exc) {
			if (!findElements(By.name("j_username")).isEmpty()) {
				loginToBackOffice("Deutsch");
				waitForThenClickMenuItem("System");
				waitForThenClickMenuItem("Typen");
				waitForThenDoBackofficeSearch("Concert");
			}
		}
	}

	private static void waitForThenDoBackofficeSearch(String search, String xp) {
		waitUntilElement(By.xpath(xp));
		pauseMS(PAUSE_MS);
		sendKeysSlowly(findElement(By.xpath(xp)), search+Keys.RETURN);
		waitUntilElement(By.xpath("//button[contains(@class, 'yw-textsearch-searchbutton')]"));
		pauseMS(PAUSE_MS);
		scrollToThenClick( findElement(By.xpath(xp)));
	}

	public static void waitForThenDoBackofficeSearch(String search) {
		int version = 0;

		pauseMS(PAUSE_MS);
		if (VersionHelper.getVersion().equals(Version.V6000)) {
			if (search.length()==0) {// 6.0 Default search does not work; need to expand it like this
				waitForthenScrollToThenClick("//button[contains(@class, 'yw-toggle-advanced-search')]");
				pauseMS(PAUSE_MS);
				waitForthenScrollToThenClick("//button[contains(@class, 'yw-textsearch-searchbutton')]");
			}
			else
				waitForThenDoBackofficeSearch(search, "//input[contains(@class, 'yw-textsearch-searchbox')]");
		}
		else {
			if ( VersionHelper.getVersion().equals("UNDEFINED")) {
				waitForThenDoBackofficeSearch(search, "//input[contains(@class, 'z-bandbox-input')]");
			} else {
				version = Integer.parseInt(VersionHelper.getVersion().toString().substring(1));

				if (version < 2011) {
					waitForThenDoBackofficeSearch(search, "//input[contains(@class, 'z-bandbox-input')]");
				} else {
					waitForThenDoBackofficeSearch(search, "//input[contains(@class, 'z-bandbox-rightedge')]");
				}
			}
		}

		pauseMS(PAUSE_MS);
	}

	private static void waitForthenScrollToThenClick(String xpath) {
		WebElement we = waitUntilElement(By.xpath(xpath));
		scrollToThenClick(we);
	}

	private static void scrollToThenClick(WebElement e) {
		((JavascriptExecutor) getDriver()).executeScript("arguments[0].scrollIntoView(true);", e);
		pauseMS(500L);
		((JavascriptExecutor) getDriver()).executeScript("arguments[0].click();", e);
	}

	public static WebElement waitUntilElement(By by) {
		return wait.until((WebDriver webDriver) -> findElement(by));
	}

	public static List<WebElement> waitUntilElements(By by) {
		return wait.until((WebDriver webDriver) -> findElements(by));
	}

	public static void waitForThenClickOkInAlertWindow() {
		longWait.until(ExpectedConditions.alertIsPresent());
		getDriver().switchTo().alert().accept();
	}

	public static WebElement waitForConstraintsMenu(){
		try {
			wait.until((WebDriver webDriver) -> findElements(By.xpath("//span[@class='z-tree-icon']")).size() == 3);
		} catch (WebDriverException exc) {
			// 6.1 and before requires clicking an extra top level menu entry "Constraint"
			waitForthenScrollToThenClick("//span[@class='z-tree-icon']");
			wait.until((WebDriver webDriver) -> findElements(By.xpath("//span[@class='z-tree-icon']")).size() == 3);
		}
		List<WebElement> we = findElements(By.xpath("//span[@class='z-tree-icon']"));
		scrollToThenClick(we.get(1));
		return we.get(1);
	}

	public static void waitForTagXWithAttributeYWithValueZThenClick( String tag, String att, String value){
		try {
			pauseMS(PAUSE_MS);
			waitForthenScrollToThenClick("//"+tag+"[@"+att+"='"+value+"']");
		} catch(Exception e) {
			LOG.error(e.getMessage());
		}
	}

	public static WebElement findElement(By by) {
		return getDriver().findElement(by);
	}

	public static List<WebElement> findElements(By by) {
		return getDriver().findElements(by);
	}

	public static void waitForGroovyWindowThenSubmitScript(String gs) {
		// Rollback to Commit
		pauseMS(2000);
		waitForthenScrollToThenClick("//label[@for='commitCheckbox']");
		WebElement queryInput = waitUntilElement(By.xpath("//div[contains(@class, 'CodeMirror')]"));
		pauseMS(2000);

		gs = gs.replaceAll("\\n", "\\\\"+"n");

		((JavascriptExecutor) dvr).executeScript("arguments[0].CodeMirror.setValue('"+gs+"');", queryInput);
		// Note some users have noted that they need the following line instead of the previous one
		//js.executeScript("arguments[0].CodeMirror.setValue(\""+gs+"\");", queryInput);

		waitForthenScrollToThenClick("//button[@id='executeButton']");
		pauseMS(4000);

	}

	public static void waitForFlexQueryFieldThenSubmit(String fq) {
		WebElement localSel = waitUntilElement(By.id("locale1"));
		new Select(localSel).selectByVisibleText("en");

		WebElement queryInput = waitUntilElement(By.xpath("//div[contains(@class, 'CodeMirror')]"));
		JavascriptExecutor js = (JavascriptExecutor) dvr;
		js.executeScript("arguments[0].CodeMirror.setValue('"+fq+"');", queryInput);
		scrollToThenClick(findElement(By.xpath("//button[@id='buttonSubmit1']")));
		pauseMS(PAUSE_MS);
		scrollToThenClick(findElement(By.xpath("//a[@id='nav-tab-3']")));
	}

	public static boolean waitForText(String text) {
		wait.until(webDriver -> webDriver.findElement(By.tagName("body")).getText().contains(text));
		return true;
	}

	public  static boolean waitFor(String tag, String text) {
		try {
			waitUntilElement(By.xpath("//"+tag+"[text()='"+text+"']"));
			return true;
		}
		catch(Exception e) {
			if (text.equals("Import finished successfully")) {
				waitUntilElement(By.xpath("//"+tag+"[text()='Import wurde erfolgreich abgeschlossen']"));
				return true;
			}
			throw new NoSuchElementException("Text not found in  waitFor: "+text);
		}
	}

	public static boolean waitForTagContaining(String tag, String text) {
		waitUntilElement(By.xpath("//"+tag+"[contains(text(),'"+text+"')]"));
		return true;
	}

	public static boolean waitForImageWithTitleThenClick(String title) {
		waitForthenScrollToThenClick("//img[@title='"+title+"']");
		return true;
	}

	public static boolean waitForValidImage() {
		wait.until(webDriver -> !findElements(By.tagName("img")).isEmpty());
		String src = findElements(By.tagName("img")).get(0).getAttribute("src");
		return src.contains("media");
	}

	public static boolean waitForThenUpdateInputField(String from, String to) {
		WebElement e = waitUntilElement(By.xpath("//input[@value='"+from+"']"));
		clearField(e);

		sendKeysSlowly(e, to);
		e.sendKeys(Keys.RETURN);
		return true;
	}


	public static boolean waitForThenClick(String tag, String text) {
		pauseMS(PAUSE_MS);
		try {
			waitForthenScrollToThenClick("//"+tag+"[text()='"+text+"']");
		}
		catch(Exception e) {
			LOG.debug("Not found " +"//{}[text()='{}']. If 6.2 or eariler and span, will try for div.", tag, text);
		}

		Version v = VersionHelper.getVersion();
		if (tag.equals("span") && v.compareTo(Version.V6200) <= 0){
			try {
				waitForthenScrollToThenClick("//div[text()='"+text+"']");
			}
			catch(Exception e) {
				LOG.debug("Not found: //div[text()='{}']", text);
			}
		}
		return true;
	}

	public static boolean waitForValue(String tag, String text) {
		waitUntilElement(By.xpath("//"+tag+"[@value='"+text+"']"));
		return true;
	}

	public static Boolean waitForExtensionListing(String extensionName){
		return waitUntilElement(By.xpath("//td[@data-extensionname='"+extensionName+"']")) != null;
	}

	public static void accessBackofficeProducts() {
		waitForThenClickMenuItem("Catalog");
		waitForThenClickMenuItem("Products");

		// fix CI issue with product listing
		if (!findElements(By.name("j_username")).isEmpty()) {
			loginToBackOffice("English");
			waitForThenClickMenuItem("Catalog");
			waitUntilElement(By.xpath("//tr[@title='Products']"));
			waitForthenScrollToThenClick("//span[text()='Products']");
		}
	}

	public static void waitForThenClickMenuItem(String menuItem) {
		pauseMS(PAUSE_MS);
		waitUntilElement(By.xpath("//tr[@title='" + menuItem + "']"));
		pauseMS(PAUSE_MS);
		waitForthenScrollToThenClick("//span[text()='" + menuItem + "']");
		pauseMS(PAUSE_MS);
	}

	public static void waitForThenClickDotsBySpan(String text) {
		pauseMS(PAUSE_MS);
		try {
			waitFor("span",text);
		}
		catch(Exception e) { // 6.2, 6.1 expect a div rather than a span
			waitFor("div",text);
		}
		WebElement dots = findElements(By.xpath("//td[contains(@class, 'ye-actiondots')]")).get(2);
		scrollToThenClick(dots);
	}

	public static void waitForThenAndClickSpan(final String spanText) { // For 6.2 Divs versus Spans
		pauseMS(PAUSE_MS);
		try {
			waitForthenScrollToThenClick("//span[text()='" + spanText + "']");
		}
		catch(Exception e) {
			if (spanText.equals("Concert")) {// 6.2 expects div with Konzert rather than span with Concert
				waitForthenScrollToThenClick("//div[text()='Konzert']");
			}
			else {
				waitForthenScrollToThenClick("//div[text()='" + spanText + "']");
			}
		}
	}

	public static void waitForThenAndClickSpan(String spanText, String ... spanOptionalText) {
		pauseMS(PAUSE_MS);
		try {
			waitForthenScrollToThenClick("//span[text()='" + spanText + "']");
		}
		catch(Exception e){
			if (spanOptionalText!=null){
				waitForthenScrollToThenClick("//span[text()='" + spanText + "'] | //span[text()='" + spanOptionalText[0] + "'] ");
			}
		}
	}

	public static void waitForThenAndClickDiv(String divText, String ... divOptionalText) {
		pauseMS(PAUSE_MS);
		try {
			waitForthenScrollToThenClick("//div[text()='" + divText + "']");
		}
		catch(Exception e){
			if (divOptionalText!=null){
				waitForthenScrollToThenClick("//div[text()='" + divText + "'] | //div[text()='" + divOptionalText[0] + "'] ");
			}
		}
	}

	public static void waitForThenClickButtonWithText(String buttonText) {
		pauseMS(PAUSE_MS);
		waitForthenScrollToThenClick("//button[text()='" + buttonText + "']");
	}

	public static void waitForInitToComplete() {
		// on some machines the focus is on the search box and fails the test
		try {
			hideElement(By.id("searchsuggest"));
			pauseMS(PAUSE_MS * 2);
			waitForLogToInitialize();
			scrollToBottom();
			longWait.until(webDriver -> findElement(By.xpath("//a[text()='Continue...']")));
			scrollToBottom();
			scrollToThenClick(findElement(By.xpath("//a[text()='Continue...']")));
		}
		catch (WebDriverException exc) {
			reinitialise(1);
		}
	}

	private static void reinitialise(int numTries) {
		try {
			canLoginToHybrisCommerce();
			navigateTo("https://localhost:9002/platform/init");
			waitForThenClickButtonWithText("Initialize");
			waitForThenClickOkInAlertWindow();

			hideElement(By.id("searchsuggest"));
			pauseMS(PAUSE_MS * 2);
			waitForLogToInitialize();
			scrollToBottom();
			longWait.until(webDriver -> findElement(By.xpath("//a[text()='Continue...']")));
			scrollToBottom();
			scrollToThenClick(findElement(By.xpath("//a[text()='Continue...']")));
		}
		catch (WebDriverException exc) {
			if (numTries < 5) {
				reinitialise(numTries + 1);
			}
			else {
				throw exc;
			}
		}
	}

	private static void waitForLogToInitialize() {
		for (int i = 0; i < 3; i++) {
			try {
				wait.until(webDriver -> findElement(By.xpath("//*[contains(text(), 'Initialize system')]")));
				return;
			}
			catch (TimeoutException exc) {
				// repeat initialization
				if (i < 2) {
					navigateTo("https://localhost:9002/platform/init");
					waitForThenClickButtonWithText("Initialize");
					waitForThenClickOkInAlertWindow();
				}
			}
		}
		throw new TimeoutException("Initialization did not load correctly.");
	}

	private static void scrollToBottom() {
		int count = 4;
		for (int i = 0; i < count; i++) {
			 new Actions(getDriver()).sendKeys(Keys.PAGE_DOWN).perform();
			 pauseMS(100);
		 }
	}
	public static void waitForNoificationToClose() {
		pauseMS();
	}

	public static void waitForAllInputFields(int n) {
		wait.until(webDriver -> findElements(By.xpath("//input[@type='text']")).size()>=n );
		pauseMS(PAUSE_MS);
	}


	public static  void navigateTo(String url) {
		getDriver().navigate().to(url);
		pauseMS(PAUSE_MS);
	}

	public static String getTitle() {
		return getDriver().getTitle();
	}

	public static String getXMLFromPage(String page) {
		navigateTo(page);
		String content = getDriver().getPageSource();
		content = content.replaceAll("\n", "");
		return content;
	}

	public static void submitImpexScript(String impex) {
		WebElement queryInput = findElement(By.xpath("//div[contains(@class, 'CodeMirror')]"));
		JavascriptExecutor js = (JavascriptExecutor) dvr;
		impex = impex.replaceAll("\\n", "\\\\n");
		js.executeScript("arguments[0].CodeMirror.setValue('"+impex+"');", queryInput);

		try {
			waitForthenScrollToThenClick("//input[@value='Import content']");
		}
		catch (Exception e) {
			waitForthenScrollToThenClick("//input[@value='Inhalt importieren']");
		}
	}



	public static void setDriver(WebDriver wd) {
		dvr = wd;
	}

	/**
	 * Used for cleanup jobs concerning the WebDriver. Can return null, as opposed to getDriver().
	 * @return the WebDriver instance or null
	 */
	public static WebDriver peakDriver() {
		return dvr;
	}

	private static int parseString(String s) {
		try {
			return Integer.parseInt(s);
		}
		catch (Exception e) {
			return -1;
		}
	}

	public static WebDriver getDriver() {
		if (dvr != null)
			return dvr;


		LOG.debug("In getdriver");
//		WebDriverManager.chromedriver().clearPreferences();
//		WebDriverManager.chromedriver().targetPath("resources/selenium").setup();

		WebDriverManager.chromedriver().clearResolutionCache();
        WebDriverManager.chromedriver().setup();

		List<String> optionArguments = new ArrayList<>();
		// allow big enough screen for visibility of elements
		optionArguments.add("window-size=1044,784");
		optionArguments.add("--disable-gpu");
		optionArguments.add("--disable-browser-side-navigation");
		if (GraphicsEnvironment.isHeadless()) {
			optionArguments.add("--headless");
		}
        // prevent unknown WebDriver errors
        optionArguments.add("--no-sandbox");
        optionArguments.add("--disable-dev-shm-usage");
        // backend is insecure by default
        optionArguments.add("--allow-insecure-localhost");
		ChromeOptions chromeOptions = new ChromeOptions();
		chromeOptions.addArguments(optionArguments);
		chromeOptions = chromeOptions.setAcceptInsecureCerts(true);

		if (WINDOWS) {
			dvr = new ChromeDriver(chromeOptions);
		} else {
			int seleniumPort = parseString( System.getProperty("selenium.port") );
			if (seleniumPort!=-1){
				LOG.debug("Opening chromedriver on port {}...", seleniumPort);
				ChromeDriverService cds =
						new ChromeDriverService.Builder().usingDriverExecutable(
						new File( "./chromedriver"))
		                .usingPort(seleniumPort)
		                .build();

				dvr = new ChromeDriver(cds, chromeOptions);
			}
			else {
				dvr = new ChromeDriver(chromeOptions);
			}
		}

		wait = new FluentWait<WebDriver>(dvr)
				.withTimeout(Duration.ofSeconds(NORMAL_WAIT_S))
				.pollingEvery(Duration.ofSeconds(POLLING_RATE_S))
				.ignoring(NoSuchElementException.class);

		longWait = new FluentWait<WebDriver>(dvr)
				.withTimeout(Duration.ofSeconds(LONG_WAIT_S))
				.pollingEvery(Duration.ofSeconds(POLLING_RATE_S))
				.ignoring(NoSuchElementException.class);

		buildWait = new FluentWait<WebDriver>(dvr)
				.withTimeout(Duration.ofSeconds(BUILD_WAIT_S))
				.pollingEvery(Duration.ofSeconds(POLLING_RATE_S))
				.ignoring(NoSuchElementException.class);

		return dvr;
	}

	public static boolean checkTestSuiteXMLMatches(String s){
		try {
			String fileContents = FileHelper.getContents("../hybris/log/junit/TESTS-TestSuites.xml").replace("\n", "").replace("\r", "");
			boolean match = fileContents.matches(s);
			if (!match)
				LOG.error("checkTestSuiteXMLMatches failed: {}", fileContents);
			return match;
		} catch (IOException e) {
			Hybris123Tests.fail("Regex not found:" + s);
		}
		return false;
	}

	public static String callCurl(String... curl) {
		byte[] bytes = new byte[100];
	    StringBuffer response = new StringBuffer();
	    try {
			ProcessBuilder pb = new ProcessBuilder(curl);
		    Process p = pb.start();
		    InputStream is = p.getInputStream();
		    BufferedInputStream bis = new BufferedInputStream(is);
		    while (bis.read(bytes, 0, 100) != -1) {
		    	for (byte b : bytes) {
			        response.append((char)b);
			    }
		        Arrays.fill(bytes, (byte) 0);
		    }
		} catch (IOException e) {
			LOG.error(e.getLocalizedMessage());
		}
		return response.toString();
	}

	public static String getMethodName() {
		final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		int i = 2;
		while (!ste[i].getMethodName().startsWith("test") &&
				!ste[i].getMethodName().startsWith("gitRepoOk") &&
				!ste[i].getMethodName().startsWith("loginAndCheckForConcertToursExtension"))
			i++;
		return  ste[i].getMethodName();
	}

	public static void modifyABandToHaveNegativeAlbumSales() {
		waitForThenClickMenuItem("System");
		waitForThenClickMenuItem("Types");
		waitForThenDoBackofficeSearch("Band");
		waitForThenClick("span","Band");  // 6.2 expects div
		waitForImageWithTitleThenClick("Search by type");
		waitForThenDoBackofficeSearch(""); // 6.2
		waitForThenClick("span","The Quiet");// 6.2 expects div
		waitForThenUpdateInputField("1200", "-1200");
		waitForThenClick("button","Save");
	}

	public static void tryToViolateTheNewConstraint() {
		waitForThenClickMenuItem("Types");
		waitForThenDoBackofficeSearch("Band");
		waitForThenClick("span","Band");// 6.2 expects div
		waitForImageWithTitleThenClick("Search by type");
		waitForThenDoBackofficeSearch(""); // 6.2
		waitForThenClick("span","The Quiet"); // 6.2 expects div
		waitForThenUpdateInputField("1200", "-1200");
		waitForThenClick("button","Save");
		waitFor("div","You have 1 Validation Errors");
	}

	public static void tryToViolateTheNewCustomConstraint() {
		waitForThenClickMenuItem("Types");
		waitForThenDoBackofficeSearch("Band"+Keys.RETURN);
		waitForThenClick("span","Band");// 6.2 expects div
		waitForImageWithTitleThenClick("Search by type");
		waitForThenDoBackofficeSearch("");//6.2
		waitForThenClick("span","The Quiet");// 6.2 expects div
		waitForThenUpdateInputField("English choral society specialising in beautifully arranged, soothing melodies and songs", "Lorem Ipsum"+Keys.RETURN);
		waitForThenClick("button","Save");
		waitFor("div","You have 1 Validation Errors");
	}

	public static void reloadConstraints() {
		waitForThenClickMenuItem("Constraints");
		waitForTagXWithAttributeYWithValueZThenClick("img","title","Reload validation engine");
		waitForThenClick("button","Yes");
		pauseMS();
	}

	public static void pauseMS(long ... pause) {
		try {
			if (pause.length==0)
				Thread.sleep(6000);
			else
				Thread.sleep(pause[0]);
		} catch (InterruptedException e) {
			LOG.error("Thread interrupted.", e);
		}
	}


	private static void clearField(WebElement elem) {
		clearField(elem, "");
	}


	private static void sendKeysSlowly(WebElement elem, String input) {
		for (int i = 0; i < input.length(); i++) {
		    elem.sendKeys(input.charAt(i) + "");
			pauseMS(PAUSE_BETWEEN_KEYS_MS);
		}
	}
	private static void clearField(WebElement elem, String newInput) {
		elem.clear();
		pauseMS(PAUSE_BETWEEN_KEYS_MS);
		elem.sendKeys(newInput);
	}

	public static void addNewMinConstraint(String id) {
		Version v = VersionHelper.getVersion();
		if (v.compareTo(Version.V6100) <= 0) {
			addNewMinConstraint61(id);
			return;
		}

		waitForTagXWithAttributeYWithValueZThenClick("a","class","ya-create-type-selector-button z-toolbarbutton");
		waitForConstraintsMenu();
		waitForTagXWithAttributeYWithValueZThenClick("tr","title","Min constraint");
		waitForAllInputFields(20);

		WebElement idInputField = findElements(By.xpath("//span[text()='ID:']/following::input[1]")).get(0);
		clearField(idInputField, id);

		WebElement minimalValueField = findElements(By.xpath("//span[text()='Minimal value:']/following::input[1]")).get(0);
		clearField(minimalValueField, "0");

		WebElement dots = waitUntilElements(By.xpath("//span[text()='Enclosing Type:' or text()='Composed type to validate:']/following::i[1]")).get(0);
		scrollToThenClick(dots);
		pauseMS(PAUSE_MS);
		WebElement identifierField = waitUntilElements(By.xpath("//span[text()='Identifier']/following::input[2]")).get(0);
		identifierField.sendKeys("Band"+Keys.RETURN);
		waitForThenClick("span","Band");// 6.2 expects div
		waitForThenClick("button","Select (1)");
		pauseMS(PAUSE_MS);

		WebElement attributeDescField = findElements(By.xpath("//span[text()='Attribute descriptor to validate:']/following::input[1]")).get(0);
		sendKeysSlowly(attributeDescField, "album sales");
		attributeDescField.sendKeys(Keys.DOWN);

		waitForThenClick("span","Band [Band] -> album sales [albumSales]");
		scrollToBottom();
		waitForThenClick("button","Done");
		waitForNoificationToClose();

		// Add a message to the the new min constraint
		waitForThenDoBackofficeSearch(id);
		waitForThenClick("span", id);// 6.2 expects div
		waitForTagXWithAttributeYWithValueZThenClick("button","class","yw-expandCollapse z-button");
		pauseMS(PAUSE_MS);

		WebElement errorMessageField = findElements(By.xpath("//div[text()='Is used in the following constraint groups']/preceding::input[1]")).get(0);
		sendKeysSlowly(errorMessageField, "Album sales must not be negative"+Keys.RETURN);
		waitForThenClick("button","Save");
		waitForNoificationToClose();
	}

	public static void addNewMinConstraint61(String id) {
		waitForTagXWithAttributeYWithValueZThenClick("a","class","ya-create-type-selector-button z-toolbarbutton");

		waitForConstraintsMenu();

		waitForTagXWithAttributeYWithValueZThenClick("tr","title","Min constraint");
		pauseMS(1000);

		WebElement idInputField = findElements(By.xpath("//span[text()='ID:']/following::input[1]")).get(0);
		clearField(idInputField, id);

		WebElement minimalValueField = findElements(By.xpath("//span[text()='Minimal value:']/following::input[1]")).get(0);
		clearField(minimalValueField, "0");

		WebElement enclosingTypeField = findElements(By.xpath("//span[text()='Enclosing Type:']/following::input[1]")).get(0);
		enclosingTypeField.sendKeys("Band"+Keys.RETURN);

		pauseMS(1000);
		WebElement dots = findElements(By.xpath("//span[text()='Enclosing Type:']/following::i[1]")).get(0);
		scrollToThenClick(dots);

		waitForThenClick("span","Band [Band]");// 6.2 expects div
		pauseMS(PAUSE_MS);

		WebElement attributeDescField = findElements(By.xpath("//span[text()='Attribute descriptor to validate:']/following::input[1]")).get(0);
		sendKeysSlowly(attributeDescField, "album sales");
		attributeDescField.sendKeys(Keys.DOWN);

		waitForThenClick("span","Band [Band] -> album sales [albumSales]");
		scrollToBottom();
		waitForThenClick("button","Done");
		waitForNoificationToClose();

		// Add a message to the the new min constraint
		waitForThenDoBackofficeSearch(id);
		waitForThenClick("span", id);// 6.2 expects div
		waitForTagXWithAttributeYWithValueZThenClick("button", "class", "yw-expandCollapse z-button");
		pauseMS(PAUSE_MS);

		WebElement errorMessageField = findElements(By.xpath("//div[text()='Is used in the following constraint groups']/preceding::input[1]")).get(0);
		sendKeysSlowly(errorMessageField, "Album sales must not be negative"+Keys.RETURN);
		waitForThenClick("button","Save");
		waitForNoificationToClose();
	}

	public static void addNewCustomConstraint(String id) {
		Version v = VersionHelper.getVersion();
		if (v.compareTo(Version.V6100) <= 0) {
			addNewCustomConstraint61(id);
			return;
		}

		waitForTagXWithAttributeYWithValueZThenClick("a","class","ya-create-type-selector-button z-toolbarbutton");

		waitForConstraintsMenu();

		scrollToBottom();
		waitForTagXWithAttributeYWithValueZThenClick("tr","title","NotLoremIpsumConstraint");
		waitForAllInputFields(19);
		scrollToBottom();
		findElements(By.xpath("//input[@type='text']"));
		scrollToBottom();

		WebElement idInputField = waitUntilElements(By.xpath("//span[text()='ID:']/following::input[1]")).get(0);
		clearField(idInputField, id+Keys.RETURN);


		// Set Band [Bands]
		WebElement dots = waitUntilElements(By.xpath("//span[text()='Enclosing Type:' or text()='Composed type to validate:']/following::i[1]")).get(0);
		scrollToThenClick(dots);
		pauseMS(PAUSE_MS);
		WebElement identifierField = waitUntilElements(By.xpath("//span[text()='Identifier']/following::input[2]")).get(0);
		identifierField.sendKeys("Band"+Keys.RETURN);
		waitForThenClick("span","Band");// 6.2 expects div
		waitForThenClick("button","Select (1)");
		pauseMS(PAUSE_MS);

		waitForAllInputFields(19);


		dots = waitUntilElements(By.xpath("//span[text()='Attribute descriptor to validate:']/following::i[1]")).get(0);
		scrollToThenClick(dots);
		pauseMS(PAUSE_MS);
		waitForThenClick("span","history");// 6.2 expects div
		waitForThenClick("button","Select (1)");
		pauseMS(PAUSE_MS);

		waitForThenClick("button","Done");
		waitForNoificationToClose();

		// Add a message to the the custom constraint
		waitForThenDoBackofficeSearch(id);
		waitForThenClick("span", id);// 6.2 expects div
		waitForTagXWithAttributeYWithValueZThenClick("button","class","yw-expandCollapse z-button");
		WebElement errorMessageField = findElements(By.xpath("//div[text()='Is used in the following constraint groups']/preceding::input[1]")).get(0);
		sendKeysSlowly(errorMessageField, "No Lorem Ipsum");
		waitForThenClick("button","Save");
		waitForNoificationToClose();
	}

	public static void addNewCustomConstraint61(String id) {
		waitForTagXWithAttributeYWithValueZThenClick("a","class","ya-create-type-selector-button z-toolbarbutton");

		waitForConstraintsMenu();

		waitForTagXWithAttributeYWithValueZThenClick("tr","title","NotLoremIpsumConstraint");
		pauseMS(1000);

		WebElement idInputField = findElements(By.xpath("//span[text()='ID:']/following::input[1]")).get(0);
		clearField(idInputField, id);

		WebElement enclosingTypeField = findElements(By.xpath("//span[text()='Enclosing Type:']/following::input[1]")).get(0);
		enclosingTypeField.sendKeys("Band"+Keys.RETURN);
		WebElement dots = findElements(By.xpath("//span[text()='Enclosing Type:']/following::i[1]")).get(0);
		scrollToThenClick(dots);
		pauseMS(PAUSE_MS);

		waitForThenClick("span","Band [Band]");// 6.2 expects div
		pauseMS(PAUSE_MS);

		WebElement attributeDescField = findElements(By.xpath("//span[text()='Attribute descriptor to validate:']/following::input[1]")).get(0);
		sendKeysSlowly(attributeDescField, "history");
		attributeDescField.sendKeys(Keys.DOWN);

		waitForThenClick("span","Band [Band] -> history [history]");
		scrollToBottom();
		waitForThenClick("button","Done");
		waitForNoificationToClose();

		// Add a message to the the new min constraint
		waitForThenDoBackofficeSearch(id);
		waitForThenClick("span", id);// 6.2 expects div
		waitForTagXWithAttributeYWithValueZThenClick("button","class","yw-expandCollapse z-button");
		pauseMS(PAUSE_MS);

		WebElement errorMessageField = findElements(By.xpath("//div[text()='Is used in the following constraint groups']/preceding::input[1]")).get(0);
		sendKeysSlowly(errorMessageField, "No Lorem Ipsum"+Keys.RETURN);
		waitForThenClick("button","Save");
		waitForNoificationToClose();
	}


	public static void selectConstraintsPage() {
		waitForThenClickMenuItem("System");
		waitForThenClickMenuItem("Validation");
		waitForThenClickMenuItem("Constraints");
	}

	public static void deleteExistingMinConstraint(String id) {
		try{
			waitForThenClick("span", id);// 6.2 expects div
			pauseMS(PAUSE_MS);

			List<WebElement> bins = findElements(By.xpath("//img[contains(@src,'/backoffice/widgetClasspathResource/widgets/actions/deleteAction/icons/icon_action_delete_default.png')]"));
			if (bins.size()==2)
				scrollToThenClick(bins.get(1));
			else
				scrollToThenClick(bins.get(0));

			waitForThenClickButtonWithText("Yes");
			waitForNoificationToClose();
		}
		catch(Exception e){
			LOG.info(e.getMessage());
		}
	}

	public static boolean canLoginToPortal() {
		try {
			waitForConnectionToOpen("https://portal.commerce.ondemand.com/", PAUSE_FOR_SERVER_START_MS);
			getDriver().get("https://portal.commerce.ondemand.com/");
			// load time may vary with the number of environments
			pauseMS(5000);
			WebElement progressBar = findElements(By.className("mat-progress-bar")).get(0);
			longWait.until(ExpectedConditions.invisibilityOf(progressBar));
			return true;
		} catch (WebDriverException e) {
			StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
			String callingMethod = stackTraceElements[2].getMethodName();
			Hybris123Tests.fail(callingMethod, "Connect Exception: " + e.getMessage());
		}
		return false;
	}

	public static boolean canLoginToSAPHelp() {
		try {
			waitForConnectionToOpen("https://help.sap.com", PAUSE_FOR_SERVER_START_MS);
			getDriver().get("https://help.sap.com");
			// load time may vary with the number of environments
			pauseMS(5000);
			return true;
		} catch (WebDriverException e) {
			StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
			String callingMethod = stackTraceElements[2].getMethodName();
			Hybris123Tests.fail(callingMethod, "Connect Exception: " + e.getMessage());
		}
		return false;
	}

	public static void createEnvironment(String envName) {
		waitForThenClickButtonWithText("New Environment");
		WebElement envNameBox = findElements(By.id("mat-input-2")).get(0);
		sendKeysSlowly(envNameBox, envName);
		Select envTypeSelect = new Select(findElements(By.id("mat-select-3")).get(0));
		envTypeSelect.selectByVisibleText("Staging");
		Select localeSelect = new Select(findElements(By.id("mat-select-4")).get(0));
		localeSelect.selectByVisibleText("German (Germany) [de_DE]");
		Select timeZoneSelect = new Select(findElements(By.id("mat-select-5")).get(0));
		timeZoneSelect.selectByVisibleText("(GMT+02:00) Europe/Berlin");
		waitForThenClickButtonWithText("Save");
	}

	public static void addRepositoryLink(String repositoryURL) throws IOException, UnsupportedFlavorException {
		getDriver().findElement(ByAngular.cssContainingText(".mat-list-item", "Repository")).click();
		waitFor("h1", "Repository");
		findElements(By.cssSelector(".mat-select-required")).get(0).click();
		findElements(By.cssSelector(".mat-option")).get(0).click();
		WebElement inputBox = findElements(By.cssSelector(".mat-input-element")).get(0);
		clearField(inputBox);
		sendKeysSlowly(inputBox, repositoryURL);
		waitForThenClickButtonWithText("Save");
		waitForThenClick("a", "Regenerate");
		getDriver().findElement(ByAngular.buttonText("Regenerate")).click();
		waitForThenClick("a", "Copy to Clipboard");
		pasteFromClipboard("target/ccloud_public_key.txt");
	}

	private static void pasteFromClipboard(String path) throws IOException, UnsupportedFlavorException {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		DataFlavor dataFlavor = DataFlavor.stringFlavor;
        if (clipboard.isDataFlavorAvailable(dataFlavor)) {
            String publicKey = (String) clipboard.getData(dataFlavor);
            FileHelper.writeToFile(path, publicKey);
        }
	}

	public static void createBuild(String buildName, String branch) {
		pauseMS(10000);
		getDriver().findElement(ByAngular.cssContainingText(".mat-list-item-content", "Builds")).click();
		waitFor("h1", "Builds");
		getDriver().findElement(ByAngular.cssContainingText(".view-headline-link", "Create Build")).click();
		pauseMS(PAUSE_MS);
		WebElement buildNameBox = findElements(By.cssSelector(".mat-input-element")).get(0);
		clearField(buildNameBox);
		sendKeysSlowly(buildNameBox, buildName);
		WebElement branchBox = findElements(By.cssSelector(".mat-input-element")).get(1);
		clearField(branchBox);
		sendKeysSlowly(branchBox, branch);
		waitForThenClickButtonWithText("Save");
	}

	public static void deployBuild(String buildName) {
		pauseMS(10000);
		getDriver().findElement(ByAngular.cssContainingText(".mat-list-item-content", "Builds")).click();
		waitFor("h1", "Builds");
		getDriver().findElements(ByAngular.cssContainingText(".mat-button", buildName)).get(0).click();
		pauseMS(5000);
		getDriver().findElement(ByAngular.cssContainingText(".view-headline-link", "Deploy to Environment")).click();
		pauseMS(PAUSE_MS);
		longWait.until(ExpectedConditions.presenceOfElementLocated(By.className("cdk-overlay-container")));
		getDriver().findElement(By.className("mat-select-arrow")).click();
		getDriver().findElement(By.xpath("//span[contains(text(), 'concerttours')]")).click();
		waitForThenClick("a", "Deploy");
	}

	public static void waitForBuild(String buildName) {
		getDriver().findElement(ByAngular.cssContainingText(".mat-list-item-content", "Environments")).click();
		pauseMS(10000);
		getDriver().findElement(ByAngular.cssContainingText(".mat-list-item-content", "Builds")).click();
		pauseMS(PAUSE_MS);
		getDriver().findElements(ByAngular.cssContainingText(".mat-button", buildName)).get(0).click();
		pauseMS(5000);
		buildWait.until(webDriver -> webDriver.findElement(By.tagName("body")).getText().contains("Success"));
	}

	public static void waitForDeployment(String envName) {
		getDriver().findElement(ByAngular.cssContainingText(".mat-list-item-content", "Environments")).click();
		pauseMS(10000);
		waitForThenClick("a", envName);
		buildWait.until(webDriver -> webDriver.findElement(By.tagName("body")).getText().contains("Deployed"));
	}

	public static String getEnvironmentNumber(String envName) {
		waitForThenClick("a", "Environments");
		waitForThenClick("a", envName);
		String url = getDriver().getCurrentUrl();
		String[] urlComponents = url.split("/");
		return urlComponents[urlComponents.length - 1];
	}

	public static void setEnviornmentProperties(String envName) {
		String envNum = getEnvironmentNumber(envName);
		waitForThenClick("a", "View Configurations");
		waitForThenClick("a", "hcs_common");
		WebElement propertiesBox = findElements(By.id("mat-input-0")).get(0);
		sendKeysSlowly(propertiesBox, "deployment.api.endpoint=https://api.cqz1m-softwarea1-" + envNum +
				"-public.model-t.cc.commerce.ondemand.com" + System.lineSeparator());
		sendKeysSlowly(propertiesBox, "sop.post.url=https://electronics.cqz1m-softwarea1-" + envNum +
				"-public.model-t.cc.commerce.ondemand.com/acceleratorservices/sop-mock/process" + System.lineSeparator());
		sendKeysSlowly(propertiesBox, "website.electronics.http=http://electronics.cqz1m-softwarea1-" + envNum +
				"-public.model-t.cc.commerce.ondemand.com" + System.lineSeparator());
		sendKeysSlowly(propertiesBox, "website.electronics.https=https://electronics.cqz1m-softwarea1-" + envNum +
				"-public.model-t.cc.commerce.ondemand.com" + System.lineSeparator());
		waitForThenClickButtonWithText("Save");
	}

	public static void accessStorefrontEndpoint() {
		wait.until(ExpectedConditions.textToBe(By.tagName("h2"), "Endpoints"));
		String accstorefront = findElements(By.xpath("//span[contains(text(), 'accstorefront')]")).get(0).getText();
		String prefix = accstorefront.contains("http") ? "" : "https://";
		navigateTo(prefix + accstorefront + "?site=electronics");
	}

	public static void editStorefrontEndpoint(String envName) {
		waitForThenClick("a", "Environments");
		waitForThenClick("a", envName);
		waitForThenClick("a", "Storefront");
		WebElement urlBox = findElements(By.id("mat-input-5")).get(0);
		String newURL = urlBox.getAttribute("value").replace("accstorefront", "electronics");
		urlBox.clear();
		sendKeysSlowly(urlBox, newURL);
		waitForThenClickButtonWithText("Save");
	}

	private static void allowAll(String endpoint) {
		waitForthenScrollToThenClick("//span[text()='" + endpoint + "']");
		waitForthenScrollToThenClick("//mat-select[contains(@aria-label, 'Rule')]");
		pauseMS(2000);
		waitForthenScrollToThenClick("//span[contains(text(), 'Allow')]");
		waitForThenClickButtonWithText("Save");
	}

	public static void allowEndpointAccess(String envName) {
		waitForThenClick("a", "Environments");
		waitForThenClick("a", envName);
		allowAll("Storefront");
		allowAll("Backoffice");
		allowAll("API");
		allowAll("JS Storefront");
	}

	public static void setSpartacusInBackoffice() {
		waitForThenClickMenuItem("Base Commerce");
		waitForThenClickMenuItem("Base Store");
		waitForThenClick("span", "Electronics Store");
		WebElement idInputField = waitUntilElements(By.xpath("//span[text()='ID']/following::input[1]")).get(0);
		sendKeysSlowly(idInputField, "electronics-spa");
		waitForThenClick("button","Save");
	}

	public static boolean testSpartacusCheckout(String envName, String email) {
		canLoginToPortal();
		waitForThenClick("a", "Environments");
		waitForThenClick("a", envName);
		addProductToCart();
		enterPurchaseDetails(email);

		return false;
	}

	private static void addProductToCart() {
		accessStorefrontEndpoint();
		findElements(By.className("carousel__item")).get(0).findElement(By.tagName("a")).click();
		findElements(By.className("js-add-to-cart")).get(0).click();
		findElements(By.className("add-to-cart-button")).get(0).click();
		findElements(By.className("js-continue-checkout-button")).get(0).click();
	}

	private static void enterPurchaseDetails(String email) {
		WebElement emailBox = findElements(By.className("guestEmail")).get(0);
		sendKeysSlowly(emailBox, email);
		WebElement confirmationBox = findElements(By.className("confirmGuestEmail")).get(0);
		sendKeysSlowly(confirmationBox, email);
		findElements(By.className("guestCheckoutBtn")).get(0).click();

		Select countrySelect = new Select(findElements(By.name("country-iso")).get(0));
		countrySelect.selectByValue("DE");
		WebElement nameBox = findElements(By.name("firstName")).get(0);
		sendKeysSlowly(nameBox, "Purchase");
		WebElement surnameBox = findElements(By.name("lastName")).get(0);
		sendKeysSlowly(surnameBox, "Tester");
		WebElement addressBox = findElements(By.name("line1")).get(0);
		sendKeysSlowly(addressBox, "Test str. 4");
		WebElement cityBox = findElements(By.name("townCity")).get(0);
		sendKeysSlowly(cityBox, "Munich");
		WebElement postcodeBox = findElements(By.name("postcode")).get(0);
		sendKeysSlowly(postcodeBox, "80000");
		findElements(By.id("addressSubmit")).get(0).click();
		findElements(By.id("deliveryMethodSubmit")).get(0).click();

		Select cardSelect = new Select(findElements(By.name("card_cardType")).get(0));
		cardSelect.selectByValue("001");
		WebElement cardNumberBox = findElements(By.name("card_accountNumber")).get(0);
		sendKeysSlowly(cardNumberBox, "4444333322221111");
		Select monthSelect = new Select(findElements(By.name("card_expirationMonth")).get(0));
		monthSelect.selectByValue("1");
		Select yearSelect = new Select(findElements(By.name("card_expirationYear")).get(0));
		yearSelect.selectByValue("2029");
		WebElement cvcBox = findElements(By.name("card_cvNumber")).get(0);
		cvcBox.sendKeys("111");
		findElements(By.id("checkout-next")).get(0).click();
		// TODO
	}

	public static boolean canLoginToExtensionFactory() {
		try {
			waitForConnectionToOpen("https://extend.cx.cloud.sap/", PAUSE_FOR_SERVER_START_MS);
			getDriver().get("https://extend.cx.cloud.sap/");
			// load time may vary with the number of environments
			pauseMS(5000);
			return true;
		} catch (WebDriverException e) {
			StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
			String callingMethod = stackTraceElements[2].getMethodName();
			Hybris123Tests.fail(callingMethod, "Connect Exception: " + e.getMessage());
		}
		return false;
	}

	public static void createExtensionFactoryApplication(String appName) {
		waitForThenAndClickSpan("Applications");
		waitForthenScrollToThenClick("//button[contains(text(), 'Create Application')]");
		WebElement nameBox = findElements(By.name("applicationName")).get(0);
		sendKeysSlowly(nameBox, appName);
		WebElement descriptionBox = findElements(By.name("applicationDescription")).get(0);
		sendKeysSlowly(descriptionBox, "A test application for the concerttours extension");
		waitForthenScrollToThenClick("//button[contains(text(), 'Create')]");
		waitForThenClick("a", appName);
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[contains(@class, 'fd-status-label--available')]")));
	}

	public static void copyApplicationConnectorURL() {
		waitForthenScrollToThenClick("//button[contains(text(), 'Connect Application')]");
		waitForthenScrollToThenClick("//button[contains(text(), 'Copy to clipboard')]");
		waitForthenScrollToThenClick("//button[contains(text(), 'OK')]");
	}

	public static void addCertificateActionToBackoffice() {
		waitForThenClickMenuItem("System");
		waitForThenClickMenuItem("API");
		// TODO
	}

	public static boolean canLoginToEFCluster(String clusterURL, String username, String password) {
		try {
			waitForConnectionToOpen(clusterURL, PAUSE_FOR_SERVER_START_MS);
			getDriver().get(clusterURL);
			pauseMS(5000);
			WebElement usernameBox = findElements(By.name("login")).get(0);
			sendKeysSlowly(usernameBox, username);
			WebElement passwordBox = findElements(By.name("password")).get(0);
			sendKeysSlowly(passwordBox, password);
			waitForThenClickButtonWithText("Login");
			return true;
		} catch (WebDriverException e) {
			StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
			String callingMethod = stackTraceElements[2].getMethodName();
			Hybris123Tests.fail(callingMethod, "Connect Exception: " + e.getMessage());
		}
		return false;
	}

	public static void downloadKubeconfig() {
		waitForThenAndClickSpan("General Settings");
		waitForthenScrollToThenClick("//button[contains(text(), 'Download config')]");
		// TODO
	}

	public static void createNamespace(String namespace) {
		waitForThenAndClickSpan("Namespaces");
		waitForthenScrollToThenClick("//button[contains(text(), 'Create Namespace')]");
		WebElement namespaceBox = findElements(By.name("namespaceName")).get(0);
		sendKeysSlowly(namespaceBox, namespace);
		waitForthenScrollToThenClick("//button[contains(text(), 'Save')]");
	}

	public static void bindApplicationAndNamespace(String appName, String namespace) {
		waitForThenAndClickSpan("Applications");
		waitForthenScrollToThenClick("//button[contains(text(), 'Create Binding')]");
		WebElement namespaceBox = findElements(By.name("namespaceName")).get(0);
		sendKeysSlowly(namespaceBox, namespace);
		waitForthenScrollToThenClick("//button[contains(text(), 'OK')]");
	}

	public static void bindECEventsAndNamespace(String namespace) {
		waitForThenAndClickSpan("Namespaces");
		waitForThenClick("h2", namespace);
		waitForThenAndClickSpan("Catalog");
		WebElement searchBox = findElements(By.className("fd-input")).get(0);
		sendKeysSlowly(searchBox, "");
		// TODO
	}

	public static void bindECOOCAndNamespace(String namespace) {
		waitForThenAndClickSpan("Namespaces");
		waitForThenClick("h2", namespace);
		waitForThenAndClickSpan("Catalog");
		WebElement searchBox = findElements(By.className("fd-input")).get(0);
		sendKeysSlowly(searchBox, "");
		// TODO
	}

	public static boolean waitForConnectionToOpen(String url, int waitMS) {
		try {
			URL obj = new URL(url);
			HttpURLConnection conn;
			if (url.contains("https"))
				conn = (HttpsURLConnection) obj.openConnection();
			else
				conn = (HttpURLConnection) obj.openConnection();
			conn.setConnectTimeout(waitMS); // start-up can take some time
			conn.setReadTimeout(waitMS);
			conn.setRequestMethod("GET");
			// Read response
			conn.getResponseCode();
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}

	public static boolean isElementClickable(By by) {
		try {
			getDriver().findElement(by).click();
			return true;
		}
		catch (WebDriverException exc) {
			return false;
		}
	}

	/**
	 * Makes a page element invisible. Useful when an user action cannot be simulated.
	 */
	public static void hideElement(By by) {
		try {
			WebElement element = findElement(by);
			((JavascriptExecutor) dvr).executeScript("arguments[0].style.display = 'none';", element);
		}
		catch(Exception e) {}
	}

	public static void closeBrowser() {
		if (peakDriver() != null) {
			takeScreenshot("target/exit_screenshot_" + (WINDOWS ? "windows" : "unix") + ".png");
			for (StackTraceElement s : Thread.currentThread().getStackTrace())
				LOG.debug(s.toString());
			getDriver().close();
			getDriver().quit();
			setDriver(null);
		}
	}

	public static void takeScreenshot(String filepath) {
		File screenshotFile = ((TakesScreenshot)getDriver()).getScreenshotAs(OutputType.FILE);
		try {
			FileUtils.copyFile(screenshotFile, new File(filepath));
		} catch (IOException e) {
			LOG.error(e.getLocalizedMessage());
		}
	}

}
