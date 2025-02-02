/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.context.config;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.DefaultBootstrapContext;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConfigDataEnvironmentPostProcessor}.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Nguyen Bao Sach
 */
@ExtendWith(MockitoExtension.class)
class ConfigDataEnvironmentPostProcessorTests {

	private final StandardEnvironment environment = new StandardEnvironment();

	private final SpringApplication application = new SpringApplication();

	@Mock
	private ConfigDataEnvironment configDataEnvironment;

	@Spy
	private ConfigDataEnvironmentPostProcessor postProcessor = new ConfigDataEnvironmentPostProcessor(Supplier::get,
			new DefaultBootstrapContext());

	@Captor
	private ArgumentCaptor<Set<String>> additionalProfilesCaptor;

	@Captor
	private ArgumentCaptor<ResourceLoader> resourceLoaderCaptor;

	@Test
	void postProcessEnvironmentWhenNoLoaderCreatesDefaultLoaderInstance() {
		willReturn(this.configDataEnvironment).given(this.postProcessor).getConfigDataEnvironment(any(), any(), any());
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		then(this.postProcessor).should().getConfigDataEnvironment(any(), this.resourceLoaderCaptor.capture(), any());
		then(this.configDataEnvironment).should().processAndApply();
		assertThat(this.resourceLoaderCaptor.getValue()).isInstanceOf(DefaultResourceLoader.class);
	}

	@Test
	void postProcessEnvironmentWhenCustomLoaderUsesSpecifiedLoaderInstance() {
		ResourceLoader resourceLoader = mock(ResourceLoader.class);
		this.application.setResourceLoader(resourceLoader);
		willReturn(this.configDataEnvironment).given(this.postProcessor).getConfigDataEnvironment(any(), any(), any());
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		then(this.postProcessor).should().getConfigDataEnvironment(any(), this.resourceLoaderCaptor.capture(), any());
		then(this.configDataEnvironment).should().processAndApply();
		assertThat(this.resourceLoaderCaptor.getValue()).isSameAs(resourceLoader);
	}

	@Test
	void postProcessEnvironmentWhenHasAdditionalProfilesOnSpringApplicationUsesAdditionalProfiles() {
		this.application.setAdditionalProfiles("dev");
		willReturn(this.configDataEnvironment).given(this.postProcessor).getConfigDataEnvironment(any(), any(), any());
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		then(this.postProcessor).should().getConfigDataEnvironment(any(), any(),
				this.additionalProfilesCaptor.capture());
		then(this.configDataEnvironment).should().processAndApply();
		assertThat(this.additionalProfilesCaptor.getValue()).containsExactly("dev");
	}

	@Test
	void postProcessEnvironmentWhenNoActiveProfiles() {
		willReturn(this.configDataEnvironment).given(this.postProcessor).getConfigDataEnvironment(any(), any(), any());
		this.postProcessor.postProcessEnvironment(this.environment, this.application);
		then(this.postProcessor).should().getConfigDataEnvironment(any(), this.resourceLoaderCaptor.capture(), any());
		then(this.configDataEnvironment).should().processAndApply();
		assertThat(this.environment.getActiveProfiles()).isEmpty();
	}

	@Test
	void applyToAppliesPostProcessing() {
		int before = this.environment.getPropertySources().size();
		TestConfigDataEnvironmentUpdateListener listener = new TestConfigDataEnvironmentUpdateListener();
		ConfigDataEnvironmentPostProcessor.applyTo(this.environment, null, null, Collections.singleton("dev"),
				listener);
		assertThat(this.environment.getPropertySources().size()).isGreaterThan(before);
		assertThat(this.environment.getActiveProfiles()).containsExactly("dev");
		assertThat(listener.getAddedPropertySources()).isNotEmpty();
		assertThat(listener.getProfiles().getActive()).containsExactly("dev");
		assertThat(listener.getAddedPropertySources().stream().anyMatch((added) -> hasDevProfile(added.getResource())))
				.isTrue();
	}

	private boolean hasDevProfile(ConfigDataResource resource) {
		return (resource instanceof StandardConfigDataResource standardResource)
				&& "dev".equals(standardResource.getProfile());
	}

}
