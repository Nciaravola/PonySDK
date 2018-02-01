/*
 * Copyright (c) 2011 PonySDK
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

package com.ponysdk.core.server.application;

import com.ponysdk.core.ui.main.EntryPoint;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ApplicationConfiguration {

    public static final String APPLICATION_ID = "ponysdk.application.id";
    public static final String APPLICATION_NAME = "ponysdk.application.name";
    public static final String APPLICATION_DESCRIPTION = "ponysdk.application.description";
    public static final String APPLICATION_CONTEXT_NAME = "ponysdk.application.context.name";
    public static final String STYLESHEETS = "ponysdk.application.stylesheets";
    public static final String JAVASCRIPTS = "ponysdk.application.javascripts";
    public static final String POINTCLASS = "ponysdk.entry.point.class";

    private String applicationID;
    private String applicationName;
    private String applicationDescription;
    private String applicationContextName = "sample";
    private long heartBeatPeriod = 5000;// ms
    private TimeUnit heartBeatPeriodTimeUnit = TimeUnit.MILLISECONDS;

    private int sessionTimeout = 15; // minutes

    private String communicationErrorFunction;

    private Set<String> meta;
    private Map<String, String> style;
    private Set<String> javascript;

    private Class<? extends EntryPoint> entryPointClass;

    private String clientConfigFile;

    private boolean debugMode;

    public ApplicationConfiguration() {
        applicationID = System.getProperty(APPLICATION_ID);
        applicationName = System.getProperty(APPLICATION_NAME);
        applicationDescription = System.getProperty(APPLICATION_DESCRIPTION);
    }

    public String getApplicationID() {
        return applicationID;
    }

    public void setApplicationID(final String applicationID) {
        this.applicationID = applicationID;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    public String getApplicationDescription() {
        return applicationDescription;
    }

    public void setApplicationDescription(final String applicationDescription) {
        this.applicationDescription = applicationDescription;
    }

    public String getApplicationContextName() {
        return applicationContextName;
    }

    public void setApplicationContextName(final String applicationContextName) {
        this.applicationContextName = applicationContextName;
    }

    public void setHeartBeatPeriod(final long heartBeatPeriod, final TimeUnit heartBeatPeriodTimeUnit) {
        this.heartBeatPeriod = heartBeatPeriod;
        this.heartBeatPeriodTimeUnit = heartBeatPeriodTimeUnit;
    }

    public long getHeartBeatPeriod() {
        return heartBeatPeriod;
    }

    public void setHeartBeatPeriod(final long heartBeatPeriod) {
        this.heartBeatPeriod = heartBeatPeriod;
    }

    public TimeUnit getHeartBeatPeriodTimeUnit() {
        return heartBeatPeriodTimeUnit;
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(final int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public Set<String> getJavascript() {
        return javascript;
    }

    public void setJavascript(final Set<String> javascript) {
        this.javascript = javascript;
    }

    public Map<String, String> getStyle() {
        return style;
    }

    public void setStyle(final Map<String, String> style) {
        this.style = style;
    }

    public Set<String> getMeta() {
        return meta;
    }

    public void setMeta(final Set<String> meta) {
        this.meta = meta;
    }

    public String getCommunicationErrorFunction() {
        return communicationErrorFunction;
    }

    public void setCommunicationErrorFunction(final String communicationErrorFunction) {
        this.communicationErrorFunction = communicationErrorFunction;
    }

    public Class<? extends EntryPoint> getEntryPointClass() {
        return entryPointClass;
    }

    public void setEntryPointClass(final Class<? extends EntryPoint> entryPointClass) {
        this.entryPointClass = entryPointClass;
    }

    public String getClientConfigFile() {
        return clientConfigFile;
    }

    public void setClientConfigFile(final String clientConfigFile) {
        this.clientConfigFile = clientConfigFile;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(final boolean debugMode) {
        this.debugMode = debugMode;
    }

    @Override
    public String toString() {
        return "ApplicationConfiguration [heartBeatPeriod=" + heartBeatPeriod + " " + heartBeatPeriodTimeUnit + "]";
    }

}