package com.paymentsystemproject.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

class WebMvcConfigTest {

	@Test
	@DisplayName("WebMvcConfig는 WebMvcConfigurer를 구현한다")
	void webMvcConfig_success() {
		WebMvcConfig webMvcConfig = new WebMvcConfig();

		assertThat(webMvcConfig).isInstanceOf(WebMvcConfigurer.class);
	}
}
