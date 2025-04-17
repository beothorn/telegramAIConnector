package com.github.beothorn.telegramAIConnector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;

public class SimpleLoggerAdvisor implements CallAroundAdvisor {

	private static final Logger logger = LoggerFactory.getLogger(SimpleLoggerAdvisor.class);

	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {

		logger.debug("BEFORE: {}", advisedRequest);

		AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);

		logger.debug("AFTER: {}", advisedResponse);

		return advisedResponse;
	}
}