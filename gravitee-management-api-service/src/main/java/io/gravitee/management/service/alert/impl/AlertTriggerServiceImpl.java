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
package io.gravitee.management.service.alert.impl;

import io.gravitee.alert.api.service.AlertTrigger;
import io.gravitee.alert.api.trigger.Trigger;
import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventListener;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.service.ApiService;
import io.gravitee.management.service.alert.AlertTriggerService;
import io.gravitee.management.service.event.ApiEvent;
import io.gravitee.notifier.api.Notification;
import io.gravitee.plugin.alert.AlertService;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

import static io.gravitee.management.service.event.ApiEvent.UPDATE;
import static java.lang.String.format;
import static java.util.Collections.singletonList;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class AlertTriggerServiceImpl implements EventListener<ApiEvent, ApiEntity>, AlertTrigger, AlertTriggerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlertTriggerServiceImpl.class);
    private static final String HC_CONDITION_FORMAT = "$[?(@.type == 'HC' && @.props.API == '%s'" +
            " && @.props['Endpoint group name'] == '%s' && @.props.['Endpoint name'] == '%s')]";

    private Set<String> hcIds = new HashSet<>();

    @Autowired
    private AlertService alertService;
    @Autowired
    private EventManager eventManager;
    @Autowired
    private ApiService apiService;
    @Autowired
    private ConfigurableEnvironment environment;
    @Value("${notifiers.email.host}")
    private String host;
    @Value("${notifiers.email.port}")
    private String port;
    @Value("${notifiers.email.username}")
    private String username;
    @Value("${notifiers.email.password}")
    private String password;
    @Value("${notifiers.email.from}")
    private String defaultFrom;
    @Value("${notifiers.email.starttls.enabled:false}")
    private boolean startTLSEnabled;
    @Value("${notifiers.email.ssl.trustAll:false}")
    private boolean sslTrustAll;
    @Value("${notifiers.email.ssl.keyStore:#{null}}")
    private String sslKeyStore;
    @Value("${notifiers.email.ssl.keyStorePassword:#{null}}")
    private String sslKeyStorePassword;

    @Value("${alerts.default.enabled:false}")
    private boolean alertEnabled;

    @PostConstruct
    public void init() {
        if (alertEnabled) {
            eventManager.subscribeForEvents(this, ApiEvent.class);
        }
    }

    @Override
    public void onEvent(final Event<ApiEvent, ApiEntity> event) {
        if (UPDATE.equals(event.type())) {
            triggerAPIHC(event.content());
        }
    }

    @Override
    public void triggerAll() {
        hcIds.clear();

        final Set<ApiEntity> apis = apiService.findAll();
        for (final ApiEntity api : apis) {
            triggerAPIHC(api);
        }
    }


    @Override
    public void triggerAPIHC(final ApiEntity api) {
        api.getProxy().getGroups().stream()
                .filter(endpointGroup -> endpointGroup.getEndpoints() != null)
                .forEach(endpointGroup -> endpointGroup.getEndpoints().forEach(endpoint -> {
                    final boolean enabled;
                    if (endpoint instanceof HttpEndpoint && ((HttpEndpoint) endpoint).getHealthCheck() != null) {
                        enabled = ((HttpEndpoint) endpoint).getHealthCheck().isEnabled();
                    } else {
                        enabled = true;
                    }

                    if (enabled) {
                        final String hcId = getHCId(api, endpointGroup, endpoint);
                        if (!hcIds.contains(hcId)) {
                            final String apiOwnerEmail = api.getPrimaryOwner().getEmail();

                            if (apiOwnerEmail == null) {
                                LOGGER.warn("Alert cannot be sent cause the API owner {} has no configured email",
                                        api.getPrimaryOwner().getDisplayName());
                            } else {
                                final Trigger trigger = new Trigger();
                                trigger.setId(hcId);
                                trigger.setName("HC status transition alerts");
                                String portalUrl = environment.getProperty("portalURL");
                                if (portalUrl != null && portalUrl.endsWith("/")) {
                                    portalUrl = portalUrl.substring(0, portalUrl.length() - 1);
                                }
                                trigger.setViewDetailsUrl(portalUrl + format("/#!/management/apis/%s/healthcheck/", api.getId()));
                                trigger.setCondition(format(HC_CONDITION_FORMAT, api.getId(), endpointGroup.getName(),
                                        endpoint.getName()));

                                final Notification notification = new Notification();
                                notification.setType("email");
                                notification.setDestination(apiOwnerEmail);

                                final JsonObject jsonConfiguration = new JsonObject();
                                jsonConfiguration.put("from", defaultFrom);
                                jsonConfiguration.put("host", host);
                                jsonConfiguration.put("port", port);
                                jsonConfiguration.put("username", username);
                                jsonConfiguration.put("password", password);
                                jsonConfiguration.put("startTLSEnabled", startTLSEnabled);
                                jsonConfiguration.put("sslTrustAll", sslTrustAll);
                                jsonConfiguration.put("sslKeyStore", sslKeyStore);
                                jsonConfiguration.put("sslKeyStorePassword", sslKeyStorePassword);

                                notification.setJsonConfiguration(jsonConfiguration.toString());
                                trigger.setNotifications(singletonList(notification));

                                alertService.send(trigger);
                                hcIds.add(hcId);
                            }
                        }
                    }
                })
                );
    }

    @Override
    public void cancelTriggerAPIHC(final ApiEntity api) {
        api.getProxy().getGroups().stream()
                .filter(endpointGroup -> endpointGroup.getEndpoints() != null)
                .forEach(endpointGroup -> endpointGroup.getEndpoints().forEach(endpoint -> {
                    final String hcId = getHCId(api, endpointGroup, endpoint);
                    if (hcIds.contains(hcId)) {
                        LOGGER.info("Sending trigger cancel message...");
                        final Trigger trigger = new Trigger();
                        trigger.setId(hcId);
                        trigger.setCancel(true);
                        alertService.send(trigger);
                        hcIds.remove(hcId);
                        LOGGER.info("Message trigger cancel successfully sent!");
                    }
                }));
    }

    private String getHCId(ApiEntity api, EndpointGroup endpointGroup, Endpoint endpoint) {
        final String hcId = "HC-" + api.getId() + '-' + endpointGroup.getName() + '-' + endpoint.getName();
        return hcId.replaceAll(" ", "_");
    }
}