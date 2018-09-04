/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.management.service.alert;

import io.gravitee.alert.api.trigger.Trigger;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.services.healthcheck.EndpointHealthCheckService;
import io.gravitee.management.model.PrimaryOwnerEntity;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.service.alert.impl.AlertTriggerServiceImpl;
import io.gravitee.plugin.alert.AlertService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.HashSet;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class AlertTriggerServiceTest {

    @InjectMocks
    private AlertTriggerService alertTriggerService = new AlertTriggerServiceImpl();

    @Mock
    private AlertService alertService;
    @Mock
    private ConfigurableEnvironment environment;

    @Mock
    private ApiEntity api;
    @Mock
    private PrimaryOwnerEntity primaryOwner;
    @Mock
    private Proxy proxy;
    @Mock
    private EndpointGroup endpointGroup;
    @Mock
    private Endpoint endpointWithHC;
    @Mock
    private HttpEndpoint httpEndpointWithHC;
    @Mock
    private HttpEndpoint endpointWithoutHC;
    @Mock
    private EndpointHealthCheckService endpointHealthCheckService;

    @Before
    public void init() {
        when(api.getId()).thenReturn("123");
        when(api.getPrimaryOwner()).thenReturn(primaryOwner);
        when(primaryOwner.getEmail()).thenReturn("test@email.com");
        when(api.getProxy()).thenReturn(proxy);
        when(proxy.getGroups()).thenReturn(singleton(endpointGroup));
        when(endpointGroup.getName()).thenReturn("default");
        when(endpointGroup.getEndpoints()).thenReturn(new HashSet<>(asList(endpointWithHC, endpointWithoutHC, httpEndpointWithHC)));
        when(endpointWithHC.getName()).thenReturn("endpointWithHC");
        when(httpEndpointWithHC.getName()).thenReturn("httpEndpointWithHC");
        when(endpointWithoutHC.getName()).thenReturn("endpointWithoutHC");
        when(endpointWithoutHC.getName()).thenReturn("httpEndpointWithoutHC");
        when(endpointWithoutHC.getHealthCheck()).thenReturn(endpointHealthCheckService);
        when(endpointHealthCheckService.isEnabled()).thenReturn(false);
    }

    @Test
    public void shouldTriggerAPIHC() {
        alertTriggerService.triggerAPIHC(api);

        verify(alertService, times(2)).send(argThat(new ArgumentMatcher<Trigger>() {
            @Override
            public boolean matches(Object argument) {
                final Trigger trigger = (Trigger) argument;
                final String prefixCondition = "$[?(@.type == 'HC' && @.props.API == '123' && @.props['Endpoint group name'] == 'default' && @.props.['Endpoint name'] == ";
                return (prefixCondition + "'endpointWithHC')]").equals(trigger.getCondition()) ||
                        (prefixCondition + "'httpEndpointWithHC')]").equals(trigger.getCondition());
            }
        }));
    }

    @Test
    public void shouldTriggerAPIHCOnce() {
        alertTriggerService.triggerAPIHC(api);
        alertTriggerService.triggerAPIHC(api);
        verify(alertService, times(2)).send(any(Trigger.class));
    }

    @Test
    public void shouldNotTriggerAPIHCBecauseNoEmailConfigured() {
        when(primaryOwner.getEmail()).thenReturn(null);
        alertTriggerService.triggerAPIHC(api);
        verify(alertService, times(0)).send(any(Trigger.class));
    }

    @Test
    public void shouldCancelAPIHC() {
        alertTriggerService.triggerAPIHC(api);
        alertTriggerService.cancelTriggerAPIHC(api);

        verify(alertService, times(2)).send(argThat(new ArgumentMatcher<Trigger>() {
            @Override
            public boolean matches(Object argument) {
                final Trigger trigger = (Trigger) argument;
                return trigger.isCancel();
            }
        }));
    }

    @Test
    public void shouldNotCancelAPIHCBecauseNotTriggeredYet() {
        alertTriggerService.cancelTriggerAPIHC(api);
        verify(alertService, times(0)).send(any(Trigger.class));
    }
}