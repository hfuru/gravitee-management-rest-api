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
package io.gravitee.management.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.management.model.alert.AlertEntity;
import io.gravitee.management.model.notification.GenericNotificationConfigEntity;
import io.gravitee.management.model.notification.NotificationConfigType;
import io.gravitee.management.model.notification.PortalNotificationConfigEntity;
import io.gravitee.management.rest.security.Permission;
import io.gravitee.management.rest.security.Permissions;
import io.gravitee.management.service.AlertService;
import io.gravitee.management.service.GenericNotificationConfigService;
import io.gravitee.management.service.PortalNotificationConfigService;
import io.gravitee.management.service.alert.AlertTriggerService;
import io.gravitee.management.service.exceptions.ForbiddenAccessException;
import io.gravitee.repository.management.model.Alert;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;

import javax.ws.rs.*;
import javax.ws.rs.DELETE;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.gravitee.management.model.permissions.RolePermission.API_ALERT;
import static io.gravitee.management.model.permissions.RolePermission.API_NOTIFICATION;
import static io.gravitee.management.model.permissions.RolePermissionAction.*;
import static io.gravitee.repository.management.model.Alert.AlertReferenceType.API;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Api(tags = {"API", "Alerts"})
public class ApiAlertsResource extends AbstractResource {

    @Autowired
    private AlertService alertService;

    @GET
    @ApiOperation(value = "List configured alerts of a given API")
    @Produces(MediaType.APPLICATION_JSON)
    @Permissions({
            @Permission(value = API_ALERT, acls = READ)
    })
    public List<AlertEntity> list(@PathParam("api") String api) {
        return alertService.list(API, api);
    }
}
