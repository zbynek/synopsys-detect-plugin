/**
 * blackduck-detect
 *
 * Copyright (c) 2020 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.jenkins.detect.substeps;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.jenkins.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.JenkinsVersionHelper;
import com.synopsys.integration.jenkins.SynopsysCredentialsHelper;
import com.synopsys.integration.jenkins.detect.extensions.global.DetectGlobalConfig;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfigBuilder;
import com.synopsys.integration.util.IntEnvironmentVariables;

import jenkins.model.GlobalConfiguration;

public class CreateDetectEnvironment {
    private final JenkinsIntLogger logger;
    private final JenkinsProxyHelper jenkinsProxyHelper;
    private final JenkinsVersionHelper jenkinsVersionHelper;
    private final SynopsysCredentialsHelper synopsysCredentialsHelper;
    private final Map<String, String> environmentVariables;

    public CreateDetectEnvironment(JenkinsIntLogger logger, JenkinsProxyHelper jenkinsProxyHelper, JenkinsVersionHelper jenkinsVersionHelper, SynopsysCredentialsHelper synopsysCredentialsHelper, Map<String, String> environmentVariables) {
        this.logger = logger;
        this.jenkinsProxyHelper = jenkinsProxyHelper;
        this.jenkinsVersionHelper = jenkinsVersionHelper;
        this.synopsysCredentialsHelper = synopsysCredentialsHelper;
        this.environmentVariables = environmentVariables;
    }

    public IntEnvironmentVariables createDetectEnvironment() {
        IntEnvironmentVariables intEnvironmentVariables = new IntEnvironmentVariables(false);
        intEnvironmentVariables.putAll(environmentVariables);
        logger.setLogLevel(intEnvironmentVariables);

        populateAllBlackDuckEnvironmentVariables(intEnvironmentVariables::put);
        populateAllPolarisEnvironmentVariables(intEnvironmentVariables::put);

        Optional<String> pluginVersion = jenkinsVersionHelper.getPluginVersion("blackduck-detect");
        if (pluginVersion.isPresent()) {
            logger.info("Running Synopsys Detect for Jenkins version: " + pluginVersion.get());
        } else {
            logger.info("Running Synopsys Detect for Jenkins");
        }

        return intEnvironmentVariables;
    }

    private void populateAllBlackDuckEnvironmentVariables(BiConsumer<String, String> environmentPutter) {
        DetectGlobalConfig detectGlobalConfig = GlobalConfiguration.all().get(DetectGlobalConfig.class);
        if (detectGlobalConfig == null) {
            return;
        }

        BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = detectGlobalConfig.getBlackDuckServerConfigBuilder(jenkinsProxyHelper, synopsysCredentialsHelper);

        blackDuckServerConfigBuilder.getProperties()
            .forEach((builderPropertyKey, propertyValue) -> acceptIfNotNull(environmentPutter, builderPropertyKey.getKey(), propertyValue));
    }

    private void populateAllPolarisEnvironmentVariables(BiConsumer<String, String> environmentPutter) {
        DetectGlobalConfig detectGlobalConfig = GlobalConfiguration.all().get(DetectGlobalConfig.class);
        if (detectGlobalConfig == null) {
            return;
        }

        PolarisServerConfigBuilder polarisServerConfigBuilder = detectGlobalConfig.getPolarisServerConfigBuilder(jenkinsProxyHelper, synopsysCredentialsHelper);

        polarisServerConfigBuilder.getProperties()
            .forEach((builderPropertyKey, propertyValue) -> acceptIfNotNull(environmentPutter, builderPropertyKey.getKey(), propertyValue));

        try {
            polarisServerConfigBuilder.build().populateEnvironmentVariables(environmentPutter);
        } catch (Exception ignored) {
            // If this doesn't work, Detect will throw an exception later on.
        }
    }

    private void acceptIfNotNull(BiConsumer<String, String> environmentPutter, String key, String value) {
        if (StringUtils.isNoneBlank(key, value)) {
            environmentPutter.accept(key, value);
        }
    }

}
