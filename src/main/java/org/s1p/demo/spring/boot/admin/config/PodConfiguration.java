package org.s1p.demo.spring.boot.admin.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import org.springframework.cloud.kubernetes.commons.PodUtils;
import org.springframework.cloud.kubernetes.fabric8.Fabric8PodUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PodConfiguration {

    @Bean
    public PodUtils podUtils(KubernetesClient client){
        return new Fabric8PodUtils(client);
    }
}
