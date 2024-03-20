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

package org.s1p.demo.spring.boot.admin.discovery;

import java.net.URI;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.kubernetes.commons.discovery.KubernetesServiceInstance;
import org.springframework.util.Assert;


public class KubernetesServiceInstanceConverter extends DefaultServiceInstanceConverter {

    @Override
    protected URI getHealthUrl(ServiceInstance instance) {
        Assert.isInstanceOf(KubernetesServiceInstance.class,
                            instance,
                            "serviceInstance must be of type KubernetesServiceInstance");
        return ((KubernetesServiceInstance) instance).getUri();
    }
}
