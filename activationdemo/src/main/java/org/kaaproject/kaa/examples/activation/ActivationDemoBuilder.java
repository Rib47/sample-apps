/**
 *  Copyright 2014-2016 CyberVision, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.kaaproject.kaa.examples.activation;

import java.util.List;

import org.kaaproject.kaa.common.dto.*;
import org.kaaproject.kaa.common.dto.ctl.CTLSchemaInfoDto;
import org.kaaproject.kaa.common.dto.ctl.CTLSchemaScopeDto;
import org.kaaproject.kaa.examples.common.AbstractDemoBuilder;
import org.kaaproject.kaa.examples.common.KaaDemoBuilder;
import org.kaaproject.kaa.server.common.admin.AdminClient;
import org.kaaproject.kaa.server.common.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@KaaDemoBuilder
public class ActivationDemoBuilder extends AbstractDemoBuilder{

    private static final Logger logger = LoggerFactory.getLogger(ActivationDemoBuilder.class);

    public ActivationDemoBuilder() {
        super("demo/activation");
    }

    @Override
    protected void buildDemoApplicationImpl(AdminClient client) throws Exception {

        logger.info("Loading 'Activation demo application' data...");

        loginTenantAdmin(client);

        ApplicationDto activationApplication = new ApplicationDto();
        activationApplication.setName("Activation demo");
        activationApplication = client.editApplication(activationApplication);

        sdkProfileDto.setApplicationId(activationApplication.getId());
        sdkProfileDto.setApplicationToken(activationApplication.getApplicationToken());
        sdkProfileDto.setNotificationSchemaVersion(1);
        sdkProfileDto.setProfileSchemaVersion(0);
        sdkProfileDto.setLogSchemaVersion(1);

        loginTenantDeveloper(client);

        ConfigurationSchemaDto configurationSchema = new ConfigurationSchemaDto();
        configurationSchema.setApplicationId(activationApplication.getId());
        configurationSchema.setName("Activation configuration schema");
        configurationSchema.setDescription("Configuration schema describing active and inactive devices used by city guide application");
        configurationSchema = client.createConfigurationSchema(configurationSchema, getResourcePath("configuration-schema.avsc"));
        sdkProfileDto.setConfigurationSchemaVersion(configurationSchema.getVersion());

        EndpointGroupDto baseEndpointGroup = null;
        List<EndpointGroupDto> endpointGroups = client.getEndpointGroups(activationApplication.getId());
        if (endpointGroups.size() == 1 && endpointGroups.get(0).getWeight() == 0) {
            baseEndpointGroup = endpointGroups.get(0);
        }
        if (baseEndpointGroup == null) {
            throw new RuntimeException("Can't get default endpoint group for activation application!");
        }

        CTLSchemaInfoDto serverProfileCtlSchema = client.saveCTLSchema(getResourceAsString("server_profile_schema.avsc"), CTLSchemaScopeDto.SERVER_PROFILE_SCHEMA, activationApplication.getId());

        ServerProfileSchemaDto serverProfileSchema = new ServerProfileSchemaDto();
        serverProfileSchema.setApplicationId(activationApplication.getId());
        serverProfileSchema.setName("Activation server profile schema");
        serverProfileSchema.setDescription("Server profile schema describing activation application profile");
        serverProfileSchema.setCtlSchemaId(serverProfileCtlSchema.getId());
        serverProfileSchema = client.saveServerProfileSchema(serverProfileSchema);

        ConfigurationDto baseConfiguration = new ConfigurationDto();
        baseConfiguration.setApplicationId(activationApplication.getId());
        baseConfiguration.setEndpointGroupId(baseEndpointGroup.getId());
        baseConfiguration.setSchemaId(configurationSchema.getId());
        baseConfiguration.setSchemaVersion(configurationSchema.getVersion());
        baseConfiguration.setDescription("Base activation configuration");
        baseConfiguration.setBody(FileUtils.readResource(getResourcePath("all_devices_conf.json")));
        baseConfiguration.setStatus(UpdateStatus.INACTIVE);
        baseConfiguration = client.editConfiguration(baseConfiguration);
        client.activateConfiguration(baseConfiguration.getId());

        // Active device group

        EndpointGroupDto activeEndpointGroup = new EndpointGroupDto();
        activeEndpointGroup.setApplicationId(activationApplication.getId());
        activeEndpointGroup.setName("Active device group");
        activeEndpointGroup.setDescription("Active device endpoint group");
        activeEndpointGroup.setWeight(1);

        activeEndpointGroup = client.editEndpointGroup(activeEndpointGroup);

        ConfigurationDto activeConfiguration = new ConfigurationDto();
        activeConfiguration.setApplicationId(activationApplication.getId());
        activeConfiguration.setEndpointGroupId(activeEndpointGroup.getId());
        activeConfiguration.setSchemaId(configurationSchema.getId());
        activeConfiguration.setSchemaVersion(configurationSchema.getVersion());
        activeConfiguration.setDescription("Active devices configuration");
        activeConfiguration.setBody(FileUtils.readResource(getResourcePath("active_device_conf.json")));
        activeConfiguration.setStatus(UpdateStatus.INACTIVE);
        activeConfiguration = client.editConfiguration(activeConfiguration);
        client.activateConfiguration(activeConfiguration.getId());

        ProfileFilterDto activeProfileFilter = new ProfileFilterDto();
        activeProfileFilter.setApplicationId(activationApplication.getId());
        activeProfileFilter.setEndpointGroupId(activeEndpointGroup.getId());
        activeProfileFilter.setServerProfileSchemaId(serverProfileSchema.getId());
        activeProfileFilter.setServerProfileSchemaVersion(serverProfileSchema.getVersion());
        activeProfileFilter.setDescription("Profile filter for active devices");
        activeProfileFilter.setBody(FileUtils.readResource(getResourcePath("active_device_filter.json")));
        activeProfileFilter.setStatus(UpdateStatus.INACTIVE);
        activeProfileFilter = client.editProfileFilter(activeProfileFilter);
        client.activateProfileFilter(activeProfileFilter.getId());

        // Inactive device group

        EndpointGroupDto inactiveEndpointGroup = new EndpointGroupDto();
        inactiveEndpointGroup.setApplicationId(activationApplication.getId());
        inactiveEndpointGroup.setName("Inactive device group");
        inactiveEndpointGroup.setDescription("Inactive device endpoint group");
        inactiveEndpointGroup.setWeight(2);

        inactiveEndpointGroup = client.editEndpointGroup(inactiveEndpointGroup);

        ConfigurationDto inactiveConfiguration = new ConfigurationDto();
        inactiveConfiguration.setApplicationId(activationApplication.getId());
        inactiveConfiguration.setEndpointGroupId(inactiveEndpointGroup.getId());
        inactiveConfiguration.setSchemaId(configurationSchema.getId());
        inactiveConfiguration.setSchemaVersion(configurationSchema.getVersion());
        inactiveConfiguration.setDescription("Inactive devices configuration");
        inactiveConfiguration.setBody(FileUtils.readResource(getResourcePath("inactive_device_conf.json")));
        inactiveConfiguration.setStatus(UpdateStatus.INACTIVE);
        inactiveConfiguration = client.editConfiguration(inactiveConfiguration);
        client.activateConfiguration(inactiveConfiguration.getId());

        ProfileFilterDto inactiveProfileFilter = new ProfileFilterDto();
        inactiveProfileFilter.setApplicationId(activationApplication.getId());
        inactiveProfileFilter.setEndpointGroupId(inactiveEndpointGroup.getId());
        inactiveProfileFilter.setServerProfileSchemaId(serverProfileSchema.getId());
        inactiveProfileFilter.setServerProfileSchemaVersion(serverProfileSchema.getVersion());
        inactiveProfileFilter.setDescription("Profile filter for inactive devices");
        inactiveProfileFilter.setBody(FileUtils.readResource(getResourcePath("inactive_device_filter.json")));
        inactiveProfileFilter.setStatus(UpdateStatus.INACTIVE);
        inactiveProfileFilter = client.editProfileFilter(inactiveProfileFilter);
        client.activateProfileFilter(inactiveProfileFilter.getId());

        logger.info("Finished loading 'Activation demo application' data...");
    }
}
