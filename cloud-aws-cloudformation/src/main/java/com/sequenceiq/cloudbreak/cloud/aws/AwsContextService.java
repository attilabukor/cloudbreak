package com.sequenceiq.cloudbreak.cloud.aws;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AwsContextService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsContextService.class);

    public void addInstancesToContext(List<CloudResource> instances, ResourceBuilderContext context, List<Group> groups) {
        groups.forEach(group -> {
            List<Long> ids = group.getInstances().stream()
                    .filter(instance -> Objects.isNull(instance.getInstanceId()))
                    .map(CloudInstance::getTemplate)
                    .map(InstanceTemplate::getPrivateId)
                    .collect(Collectors.toList());
            List<CloudResource> groupInstances = instances.stream().filter(inst -> inst.getGroup().equals(group.getName())).collect(Collectors.toList());
            if (ids.size() > groupInstances.size()) {
                String message = String.format("Not found enough instances in %s group, expected %s, got %s. " +
                                "Please check the instances on your cloud provider for further details.", group.getName(), ids.size(),
                        groupInstances.size());
                throw new CloudConnectorException(message);
            }
            for (int i = 0; i < ids.size(); i++) {
                context.addComputeResources(ids.get(i), List.of(groupInstances.get(i)));
            }
        });
    }

    public void addResourcesToContext(List<CloudResource> resources, ResourceBuilderContext context, List<Group> groups) {
        groups.forEach(group -> {
            List<Long> ids = group.getInstances().stream()
                    .filter(instance -> Objects.isNull(instance.getInstanceId()))
                    .map(CloudInstance::getTemplate).map(InstanceTemplate::getPrivateId).collect(Collectors.toList());
            List<CloudResource> groupInstances = getResourcesOfTypeInGroup(resources, group, ResourceType.AWS_INSTANCE);
            List<CloudResource> groupVolumeSets = getResourcesOfTypeInGroup(resources, group, ResourceType.AWS_VOLUMESET);
            for (int i = 0; i < ids.size(); i++) {
                if (i < groupInstances.size()) {
                    Long privateId = ids.get(i);
                    CloudResource instanceResource = groupInstances.get(i);
                    if (i > groupVolumeSets.size() - 1) {
                        context.addComputeResources(privateId, List.of(instanceResource));
                    } else {
                        CloudResource volumesetResource = groupVolumeSets.get(i);
                        LOGGER.debug("Adding instance and volume set to context under private id: {}. "
                                + "Instance: {}, Volume Set: {}", privateId, instanceResource, volumesetResource);
                        context.addComputeResources(privateId, List.of(instanceResource, volumesetResource));
                    }
                }
            }
        });
    }

    private List<CloudResource> getResourcesOfTypeInGroup(List<CloudResource> resources, Group group, ResourceType awsInstance) {
        return resources.stream()
                .filter(cloudResource -> awsInstance.equals(cloudResource.getType()))
                .filter(inst -> inst.getGroup().equals(group.getName())).collect(Collectors.toList());
    }

}
