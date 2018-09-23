/*
 * Copyright 2014-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.s1p.demo.spring.boot.admin.config;


import org.s1p.demo.spring.boot.admin.discovery.DefaultServiceInstanceConverter;
import org.s1p.demo.spring.boot.admin.discovery.InstanceDiscoveryListener;
import org.s1p.demo.spring.boot.admin.discovery.ServiceInstanceConverter;
import de.codecentric.boot.admin.server.config.AdminServerAutoConfiguration;
import de.codecentric.boot.admin.server.config.AdminServerMarkerConfiguration;
import de.codecentric.boot.admin.server.config.AdminServerProperties;
import de.codecentric.boot.admin.server.domain.entities.InstanceRepository;
import de.codecentric.boot.admin.server.services.InstanceRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@ConditionalOnSingleCandidate(DiscoveryClient.class)
@ConditionalOnBean(AdminServerMarkerConfiguration.Marker.class)
@ConditionalOnProperty(prefix = "spring.boot.admin.discovery", name = "enabled", matchIfMissing = true)
@AutoConfigureAfter(value = AdminServerAutoConfiguration.class, name = {
    "org.springframework.cloud.kubernetes.discovery.KubernetesDiscoveryClientAutoConfiguration",
    "org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration"})
public class AdminServerDiscoveryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConfigurationProperties(prefix = "spring.boot.admin.discovery")
    public InstanceDiscoveryListener instanceDiscoveryListener(ServiceInstanceConverter serviceInstanceConverter,
                                                               DiscoveryClient discoveryClient,
                                                               InstanceRegistry registry,
                                                               InstanceRepository repository) {
        InstanceDiscoveryListener listener = new InstanceDiscoveryListener(discoveryClient, registry, repository);
        listener.setConverter(serviceInstanceConverter);
        return listener;
    }

//    @Configuration
//    @ConditionalOnMissingBean({ServiceInstanceConverter.class})
//    @ConditionalOnBean(KubernetesClient.class)
//    public static class KubernetesConverterConfiguration {
//        @Bean
//        @Primary
//        @ConfigurationProperties(prefix = "spring.boot.admin.discovery.converter")
//        public KubernetesServiceInstanceConverter serviceInstanceConverter() {
//            return new KubernetesServiceInstanceConverter();
//        }
//    }

    @Bean
    @ConditionalOnMissingBean({ServiceInstanceConverter.class})
    @ConfigurationProperties(prefix = "spring.boot.admin.discovery.converter")
    public DefaultServiceInstanceConverter serviceInstanceConverter() {
        return new DefaultServiceInstanceConverter();
    }

    @Profile("secure")
    @Configuration
    public static class SecuritySecureConfig extends WebSecurityConfigurerAdapter {
        private final String adminContextPath;

        public SecuritySecureConfig(AdminServerProperties adminServerProperties) {
            this.adminContextPath = adminServerProperties.getContextPath();
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
            successHandler.setTargetUrlParameter("redirectTo");
            successHandler.setDefaultTargetUrl(adminContextPath + "/");

            http.authorizeRequests()
                    .antMatchers(adminContextPath + "/assets/**").permitAll() // <1>
                    .antMatchers(adminContextPath + "/login").permitAll()
                    .antMatchers(adminContextPath + "/actuator/**").permitAll()
                    .anyRequest().authenticated() // <2>
                    .and()
                    .formLogin().loginPage(adminContextPath + "/login").successHandler(successHandler).and() // <3>
                    .logout().logoutUrl(adminContextPath + "/logout").and()
                    .httpBasic().and() // <4>
                    .csrf()
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())  // <5>
                    .ignoringAntMatchers(
                            adminContextPath + "/instances",   // <6>
                            adminContextPath + "/actuator/**"  // <7>
                    );
        }
    }

    @Profile("insecure")
    @Configuration
    public static class SecurityPermitAllConfig extends WebSecurityConfigurerAdapter {
        private final String adminContextPath;

        public SecurityPermitAllConfig(AdminServerProperties adminServerProperties) {
            this.adminContextPath = adminServerProperties.getContextPath();
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests()
                    .anyRequest()
                    .permitAll()
                    .and()
                    .csrf()
                    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringAntMatchers(adminContextPath + "/instances", adminContextPath + "/actuator/**");
        }
    }
}
