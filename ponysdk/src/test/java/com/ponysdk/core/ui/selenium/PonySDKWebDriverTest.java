/*
 * Copyright (c) 2017 PonySDK
 *  Owners:
 *  Luciano Broussal  <luciano.broussal AT gmail.com>
 *  Mathieu Barbier   <mathieu.barbier AT gmail.com>
 *  Nicolas Ciaravola <nicolas.ciaravola.pro AT gmail.com>
 *
 *  WebSite:
 *  http://code.google.com/p/pony-sdk/
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.ponysdk.core.ui.selenium;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.ponysdk.core.model.ClientToServerModel;

@Ignore
public class PonySDKWebDriverTest {

    private static PonySDKWebDriver driver;
    private static WebDriverWait wait;

    @BeforeClass
    public static void beforeClass() {
        driver = new PonySDKWebDriver();
        wait = new WebDriverWait(driver, 30);
        driver.get("ws://localhost:8081/sample/ws?" + ClientToServerModel.TYPE_HISTORY.toStringValue() + "=");
        boolean result;
        try {
            //driver.findElement(By.name("test")).click();

            result = wait.until((ExpectedCondition<Boolean>) webDriver -> {
                System.out.println("Searching ...");
                return webDriver.findElement(By.id("label")) != null;
            });
        } catch (final Exception e) {
            e.printStackTrace();
            result = false;
        } finally {
            driver.close();
        }

        System.out.println("result final ... " + result);
    }

    @Test
    public void test() {

    }
}
