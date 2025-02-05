package com.sequenceiq.cloudbreak.service.stack.flow;

import static java.lang.String.format;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateValidator;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDownscaleValidatorService;
import com.sequenceiq.cloudbreak.workspace.model.User;

@Component
public class UpdateNodeCountValidator {

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackDownscaleValidatorService downscaleValidatorService;

    @Inject
    private CmTemplateValidator cmTemplateValidator;

    public void validataHostMetadataStatuses(Stack stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson) {
        if (upscaleEvent(instanceGroupAdjustmentJson.getScalingAdjustment())) {
            List<InstanceMetaData> instanceMetaDataAsList = stack.getInstanceMetaDataAsList();
            List<InstanceMetaData> unhealthyInstanceMetadataList = instanceMetaDataAsList.stream()
                    .filter(instanceMetaData -> InstanceStatus.SERVICES_UNHEALTHY.equals(instanceMetaData.getInstanceStatus()))
                    .collect(Collectors.toList());
            if (!unhealthyInstanceMetadataList.isEmpty()) {
                String notHealthyInstances = unhealthyInstanceMetadataList.stream()
                        .map(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() + ": " + instanceMetaData.getInstanceStatus())
                        .collect(Collectors.joining(","));
                throw new BadRequestException(
                        format("Upscale is not allowed because the following hosts are not healthy: %s. Please remove them first!", notHealthyInstances));
            }
        }
    }

    public void validateHostGroupAdjustment(InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson, Stack stack, Integer adjustment) {
        Optional<HostGroup> hostGroup = stack.getCluster().getHostGroups().stream()
                .filter(input -> input.getInstanceGroup().getGroupName().equals(instanceGroupAdjustmentJson.getInstanceGroup())).findFirst();
        if (!hostGroup.isPresent()) {
            throw new BadRequestException(format("Group '%s' not found or not part of Data Hub '%s'",
                    instanceGroupAdjustmentJson.getInstanceGroup(), stack.getName()));
        }
    }

    public void validateStackStatus(Stack stack) {
        if (!stack.isAvailable()) {
            throw new BadRequestException(format("Group '%s' is currently in '%s' state. Node count can only be updated if it's running.",
                    stack.getName(), stack.getStatus()));
        }
    }

    public void validateServiceRoles(Stack stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson) {
        String instanceGroup = instanceGroupAdjustmentJson.getInstanceGroup();
        Optional<HostGroup> hostGroup = stack.getCluster().getHostGroups()
                .stream()
                .filter(e -> e.getName().equals(instanceGroup))
                .findFirst();
        if (hostGroup.isPresent()) {
            User creator = stack.getCreator();
            String userCrn = creator.getUserCrn();
            String accountId = Crn.safeFromString(userCrn).getAccountId();
            cmTemplateValidator.validateHostGroupScalingRequest(
                    accountId,
                    stack.getCluster().getBlueprint(),
                    hostGroup.get(),
                    instanceGroupAdjustmentJson.getScalingAdjustment());
        }
    }

    public void validateClusterStatus(Stack stack) {
        Cluster cluster = stack.getCluster();
        if (cluster != null && !cluster.isAvailable()) {
            throw new BadRequestException(format("Data Hub '%s' is currently in '%s' state. Node count can only be updated if it's not available.",
                    cluster.getName(), cluster.getStatus()));
        }
    }

    public void validateInstanceGroup(Stack stack, String instanceGroupName) {
        InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupName);
        if (instanceGroup == null) {
            throw new BadRequestException(format("Data Hub '%s' does not have a group named '%s'.", stack.getName(), instanceGroupName));
        }
    }

    public void validateScalabilityOfInstanceGroup(Stack stack, HostGroupAdjustmentV4Request hostGroupAdjustmentV4Request) {
        InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupName(hostGroupAdjustmentV4Request.getHostGroup());
        validateGroupAdjustment(
                stack,
                hostGroupAdjustmentV4Request.getScalingAdjustment(),
                instanceGroup);
    }

    public void validateScalabilityOfInstanceGroup(Stack stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson) {
        InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupAdjustmentJson.getInstanceGroup());
        validateGroupAdjustment(
                stack,
                instanceGroupAdjustmentJson.getScalingAdjustment(),
                instanceGroup);
    }

    private void validateGroupAdjustment(Stack stack, Integer scalingAdjustment, InstanceGroup instanceGroup) {
        if (upscaleEvent(scalingAdjustment)) {
            if (nodeCountIsLowerThanMinimalNodeCountAfterTheScalingEvent(instanceGroup, scalingAdjustment)) {
                throw new BadRequestException(format("Requested scaling up is forbidden on %s Data Hub %s group because the " +
                        "the current node count is %s node the node count after the upscale action will be %s node and the minimal " +
                        "node count in the %s group is %s node. You can not go under the minimal node count.",
                        stack.getName(),
                        instanceGroup.getGroupName(),
                        instanceGroup.getNodeCount(),
                        getNodeCountAfterScaling(instanceGroup, scalingAdjustment),
                        instanceGroup.getGroupName(),
                        instanceGroup.getMinimumNodeCount()));
            }
            if (!instanceGroup.getScalabilityOption().upscalable()) {
                throw new BadRequestException(format("Requested scaling up is forbidden on %s Data Hub %s group.",
                        stack.getName(),
                        instanceGroup.getGroupName()));
            }
        } else if (downScaleEvent(scalingAdjustment)) {
            if (nodeCountIsLowerThanMinimalNodeCountAfterTheScalingEvent(instanceGroup, scalingAdjustment)) {
                throw new BadRequestException(format("Requested scaling down is forbidden on %s Data Hub %s group because the " +
                        "the current node count is %s node the node count after the downscale action will be %s node and the minimal " +
                        "node count in the %s group is %s node. You can not go under the minimal node count.",
                        stack.getName(),
                        instanceGroup.getGroupName(),
                        instanceGroup.getNodeCount(),
                        getNodeCountAfterScaling(instanceGroup, scalingAdjustment),
                        instanceGroup.getGroupName(),
                        instanceGroup.getMinimumNodeCount()));
            }
            if (!instanceGroup.getScalabilityOption().downscalable()) {
                throw new BadRequestException(format("Requested scaling down is forbidden on %s Data Hub's %s group.",
                        stack.getName(),
                        instanceGroup.getGroupName()));
            }
        }
    }

    private int getNodeCountAfterScaling(InstanceGroup instanceGroup, Integer scalingAdjustment) {
        return instanceGroup.getNodeCount() + scalingAdjustment.intValue();
    }

    private boolean nodeCountIsLowerThanMinimalNodeCountAfterTheScalingEvent(InstanceGroup instanceGroup,
        Integer scalingAdjustment) {
        int minimumNodeCount = instanceGroup.getMinimumNodeCount();
        return getNodeCountAfterScaling(instanceGroup, scalingAdjustment) < minimumNodeCount;
    }

    private boolean downScaleEvent(Integer scalingAdjustment) {
        return 0 > scalingAdjustment;
    }

    private boolean upscaleEvent(Integer scalingAdjustment) {
        return 0 < scalingAdjustment;
    }

    public void validateScalingAdjustment(InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson, Stack stack) {
        if (0 == instanceGroupAdjustmentJson.getScalingAdjustment()) {
            throw new BadRequestException(format("Requested scaling adjustment on Data Hub '%s' is 0. Nothing to do.", stack.getName()));
        }
        if (upscaleEvent(instanceGroupAdjustmentJson.getScalingAdjustment())) {
            InstanceGroup instanceGroup = stack.getInstanceGroupByInstanceGroupName(instanceGroupAdjustmentJson.getInstanceGroup());
            if (-1 * instanceGroupAdjustmentJson.getScalingAdjustment() > instanceGroup.getNodeCount()) {
                throw new BadRequestException(format("There are %s instances in '%s' group. CDP Cannot remove %s instances.",
                        instanceGroup.getNodeCount(), instanceGroup.getGroupName(),
                        -1 * instanceGroupAdjustmentJson.getScalingAdjustment()));
            }
            int removableHosts = instanceMetaDataService.findRemovableInstances(stack.getId(), instanceGroupAdjustmentJson.getInstanceGroup()).size();
            if (removableHosts < -1 * instanceGroupAdjustmentJson.getScalingAdjustment()) {
                throw new BadRequestException(
                        format("There are %s unregistered instances in '%s' group '%s' but %s were requested. Decommission nodes from the Data Hub!",
                                removableHosts, instanceGroup.getGroupName(), instanceGroupAdjustmentJson.getScalingAdjustment() * -1));
            }
        }
    }

    public void validateInstanceStatuses(Stack stack, InstanceGroupAdjustmentV4Request instanceGroupAdjustmentJson) {
        if (upscaleEvent(instanceGroupAdjustmentJson.getScalingAdjustment())) {
            List<InstanceMetaData> instanceMetaDataList =
                    stack.getInstanceMetaDataAsList().stream().filter(im -> !im.isTerminated() && !im.isRunning() && !im.isCreated())
                            .collect(Collectors.toList());
            if (!instanceMetaDataList.isEmpty()) {
                String ims = instanceMetaDataList.stream()
                        .map(im -> im.getInstanceId() != null ? im.getInstanceId() : im.getPrivateId() + ": " + im.getInstanceStatus())
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(
                        format("Upscale is not allowed because the following instances are not in running state: %s. Please remove them first!", ims));
            }
        }
    }

    public InstanceMetaData validateInstanceForDownscale(String instanceId, Stack stack) {
        InstanceMetaData metaData = instanceMetaDataService.findByStackIdAndInstanceId(stack.getId(), instanceId)
                .orElseThrow(() -> new NotFoundException(format("Metadata for instance %s has not found.", instanceId)));
        downscaleValidatorService.checkInstanceIsTheClusterManagerServerOrNot(metaData.getPublicIp(), metaData.getInstanceMetadataType());
        downscaleValidatorService.checkClusterInValidStatus(stack.getCluster());
        return metaData;
    }
}
