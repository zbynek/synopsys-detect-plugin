/*
 * blackduck-detect
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.detect;

import java.io.IOException;
import java.util.List;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.extensions.DetectDownloadStrategy;
import com.synopsys.integration.jenkins.detect.service.DetectArgumentService;
import com.synopsys.integration.jenkins.detect.service.DetectEnvironmentService;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectExecutionStrategy;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectStrategyService;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.util.IntEnvironmentVariables;
import com.synopsys.integration.util.OperatingSystemType;

public class DetectRunner {
    public static final String ASTERISKS = "******************************************************************************";

    private final DetectEnvironmentService detectEnvironmentService;
    private final JenkinsRemotingService remotingService;
    private final DetectStrategyService detectStrategyService;
    private final DetectArgumentService detectArgumentService;
    private final JenkinsIntLogger logger;

    public DetectRunner(
        DetectEnvironmentService detectEnvironmentService,
        JenkinsRemotingService remotingService,
        DetectStrategyService detectStrategyService,
        DetectArgumentService detectArgumentService,
        JenkinsIntLogger logger
    ) {
        this.detectEnvironmentService = detectEnvironmentService;
        this.remotingService = remotingService;
        this.detectStrategyService = detectStrategyService;
        this.detectArgumentService = detectArgumentService;
        this.logger = logger;
    }

    public int runDetect(String remoteJdkHome, String detectArgumentString, DetectDownloadStrategy detectDownloadStrategy)
        throws IOException, InterruptedException, IntegrationException {
        IntEnvironmentVariables intEnvironmentVariables = detectEnvironmentService.createDetectEnvironment();
        OperatingSystemType operatingSystemType = remotingService.getRemoteOperatingSystemType();
        DetectExecutionStrategy detectExecutionStrategy = detectStrategyService.getExecutionStrategy(
            intEnvironmentVariables,
            operatingSystemType,
            remoteJdkHome,
            detectDownloadStrategy
        );

        List<String> initialArguments = remotingService.call(detectExecutionStrategy.getSetupCallable());

        List<String> detectCommands = detectArgumentService.getDetectArguments(
            intEnvironmentVariables,
            detectExecutionStrategy.getArgumentEscaper(),
            initialArguments,
            detectArgumentString
        );

        logger.info(ASTERISKS);
        logger.info("START OF DETECT");
        logger.info(ASTERISKS);

        int detectRun = remotingService.launch(intEnvironmentVariables, detectCommands);

        logger.info(ASTERISKS);
        logger.info("END OF DETECT");
        logger.info(ASTERISKS);

        return detectRun;
    }
}
