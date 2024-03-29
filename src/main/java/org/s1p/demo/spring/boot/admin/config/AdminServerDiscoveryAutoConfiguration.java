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
import org.s1p.demo.spring.boot.admin.discovery.InstanceDiscoveryListener;
import org.s1p.demo.spring.boot.admin.discovery.KubernetesServiceInstanceConverter;
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
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@ConditionalOnSingleCandidate(DiscoveryClient.class)
@ConditionalOnBean(AdminServerMarkerConfiguration.Marker.class)
@ConditionalOnProperty(prefix = "spring.boot.admin.discovery", name = "enabled", matchIfMissing = true)
@AutoConfigureAfter(value = AdminServerAutoConfiguration.class, name = {
        "org.springframework.cloud.kubernetes.discovery.KubernetesDiscoveryClientBlockingAutoConfiguration",
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

    @Configuration
    @ConditionalOnMissingBean({ServiceInstanceConverter.class})
    //@ConditionalOnBean(KubernetesClient.class)
    public static class KubernetesConverterConfiguration {
        @Bean
        @ConfigurationProperties(prefix = "spring.boot.admin.discovery.converter")
        public KubernetesServiceInstanceConverter serviceInstanceConverter() {
            return new KubernetesServiceInstanceConverter();
        }
    }

    @Profile("secure")
    @Configuration
    public static class SecuritySecureConfig {
        private final String adminContextPath;

        public SecuritySecureConfig(AdminServerProperties adminServerProperties) {
            this.adminContextPath = adminServerProperties.getContextPath();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
            successHandler.setTargetUrlParameter("redirectTo");
            successHandler.setDefaultTargetUrl(adminContextPath + "/");

            http.authorizeHttpRequests(authorizeRequests -> {
                        authorizeRequests.requestMatchers(adminContextPath + "/assets/**").permitAll() // <1>
                                .requestMatchers(adminContextPath + "/login").permitAll()
                                .requestMatchers(adminContextPath + "/actuator/**").permitAll()
                                .requestMatchers(adminContextPath + "/instances").permitAll()
                                .anyRequest().authenticated(); // <2>
                    }).formLogin(httpSecurityFormLoginConfigurer ->
                            httpSecurityFormLoginConfigurer
                                    .loginPage(adminContextPath + "/login")
                                    .successHandler(successHandler))
                    .logout(httpSecurityLogoutConfigurer -> httpSecurityLogoutConfigurer.logoutUrl(adminContextPath + "/logout"))  // <3>
                    .httpBasic(Customizer.withDefaults()) // <4>
                    .csrf(httpSecurityCsrfConfigurer -> httpSecurityCsrfConfigurer.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));   //<5>
            return http.build();

        }

        /*@Bean
        public WebSecurityCustomizer webSecurityCustomizer() {
            return (web) -> web.ignoring().requestMatchers(adminContextPath + "/instances", adminContextPath + "/actuator/**");
        }*/


        @Profile("insecure")
        @Configuration
        public static class SecurityPermitAllConfig {
            private final String adminContextPath;

            public SecurityPermitAllConfig(AdminServerProperties adminServerProperties) {
                this.adminContextPath = adminServerProperties.getContextPath();
            }

            @Bean
            public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http.authorizeHttpRequests(authorizeRequest -> authorizeRequest.anyRequest().permitAll())
                        .csrf(httpSecurityCsrfConfigurer -> httpSecurityCsrfConfigurer.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()));
                return http.build();
            }

            /*@Bean
            public WebSecurityCustomizer webSecurityCustomizer() {
                return (web) -> web.ignoring().requestMatchers(adminContextPath + "/instances", adminContextPath + "/actuator/**");
            }*/
        }


    }
}
