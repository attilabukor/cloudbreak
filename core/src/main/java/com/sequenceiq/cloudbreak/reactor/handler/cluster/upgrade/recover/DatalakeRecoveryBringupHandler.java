package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.recover;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.controller.StackCreatorService;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup.DatalakeRecoveryBringupFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup.DatalakeRecoveryBringupRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.upgrade.recovery.bringup.DatalakeRecoveryBringupSuccess;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.cluster.VolumeSetManagerService;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class DatalakeRecoveryBringupHandler extends ExceptionCatcherEventHandler<DatalakeRecoveryBringupRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeRecoveryBringupHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private StackCreatorService stackCreatorService;

    @Inject
    private StackUpscaleService stackUpscaleService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private DatalakeResourcesService datalakeResourcesService;

    @Inject
    private VolumeSetManagerService volumeSetManagerService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DatalakeRecoveryBringupRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<DatalakeRecoveryBringupRequest> event) {
        return new DatalakeRecoveryBringupFailedEvent(resourceId, e, DetailedStackStatus.CLUSTER_RECOVERY_FAILED);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<DatalakeRecoveryBringupRequest> event) {
        DatalakeRecoveryBringupRequest request = event.getData();
        Selectable result;
        Long stackId = request.getResourceId();
        LOGGER.debug("Relaunching instances for stack {}", stackId);
        try {
            Stack stack = stackService.getByIdWithClusterInTransaction(stackId);
            // sort by name to avoid shuffling the different instance groups
//            List<InstanceGroup> instanceGroups = stack.getInstanceGroupsAsList();
//            Collections.sort(instanceGroups);
            List<InstanceGroup> instanceGroups = stackCreatorService.sortInstanceGroups(stack);
            for(InstanceGroup instanceGroup : instanceGroups) {
                List<CloudInstance> newInstance =
                        stackUpscaleService.buildNewInstances(stack, instanceGroup.getGroupName(), instanceGroup.getInitialNodeCount());
                instanceMetaDataService.saveInstanceAndGetUpdatedStack(stack, newInstance, true);
            }
            clusterService.updateClusterStatusByStackId(stackId, Status.REQUESTED);

            if (stack.getType() == StackType.DATALAKE) {
                datalakeResourcesService.deleteByStackId(stack.getId());
            }

//            volumeSetManagerService.updateVolumesDeleteFlag(stack, resource -> true, false);
//            Stack updatedStack = instanceMetaDataService.saveInstanceAndGetUpdatedStack(stack, newInstances, true);
//            stackCreatorService.prepareInstanceMetadata(stack);
//            measure(() -> instanceMetaDataService.saveAll(stack.getInstanceMetaDataAsList()),
//                    LOGGER, "Instance metadata saved in {} ms for stack {}", stack.getName());

            result = new DatalakeRecoveryBringupSuccess(stackId);
        } catch (Exception e) {
            LOGGER.error("Relaunching instances for stack failed", e);
            result = new DatalakeRecoveryBringupFailedEvent(stackId, e, DetailedStackStatus.CLUSTER_RECOVERY_FAILED);
        }
        return result;
    }


}
