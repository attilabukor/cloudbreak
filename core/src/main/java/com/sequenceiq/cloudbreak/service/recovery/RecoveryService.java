package com.sequenceiq.cloudbreak.service.recovery;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.DELETE_COMPLETED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.DATALAKE_RECOVERY_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.STACK_DELETE_COMPLETED;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackResult;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationContext;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateRetryParams;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.flow.TerminationService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Service
public class RecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecoveryService.class);

    private static final int RECOVER_OPERATION_RETRY_COUNT = 10;

    private static final String SDX_RECOVER = "postgresql.disaster_recovery.recover";

    @Inject
    private TerminationService terminationService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private StackUpdater stackUpdater;



    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    public void finishTeardown(StackTerminationContext context, TerminateStackResult payload) {
        LOGGER.debug("Recovery tear down result: {}", payload);
        Stack stack = context.getStack();
        terminationService.finalizeRecoveryTeardown(stack.getId());
        flowMessageService.fireEventAndLog(stack.getId(), DELETE_COMPLETED.name(), STACK_DELETE_COMPLETED);
//        clusterService.updateClusterStatusByStackId(stack.getId(), DELETE_COMPLETED);
//        metricService.incrementMetricCounter(MetricType.STACK_TERMINATION_SUCCESSFUL, stack);
    }

    public void handleRecoveryError(Long stackId, Exception errorDetails) {
        DetailedStackStatus status;
        String stackUpdateMessage = "Recovery failed: " + errorDetails.getMessage();
        status = DetailedStackStatus.CLUSTER_RECOVERY_FAILED;
        stackUpdater.updateStackStatus(stackId, status, stackUpdateMessage);
        LOGGER.info("Error during stack recovery flow: ", errorDetails);

        flowMessageService.fireEventAndLog(stackId, status.name(), DATALAKE_RECOVERY_FAILED, stackUpdateMessage);
    }

    public void runRecoverState(Stack stack) throws Exception {
        OrchestratorStateParams stateParams = createRecoverStateParams(stack);
        hostOrchestrator.runOrchestratorState(stateParams);
    }

    private OrchestratorStateParams createRecoverStateParams(Stack stack) {
        Cluster cluster = stack.getCluster();
        Set<Node> nodes = stackUtil.collectReachableNodes(stack);
        OrchestratorStateParams stateParams = new OrchestratorStateParams();
        stateParams.setState(RecoveryService.SDX_RECOVER);
        stateParams.setPrimaryGatewayConfig(gatewayConfigService.getGatewayConfig(stack, stack.getPrimaryGatewayInstance(), stack.getCluster().hasGateway()));
        stateParams.setTargetHostNames(nodes.stream().map(Node::getHostname).collect(Collectors.toSet()));
        stateParams.setAllNodes(nodes);
        stateParams.setExitCriteriaModel(ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stack.getId(), cluster.getId()));
        stateParams.setStateParams(createRecoverParams());
        OrchestratorStateRetryParams retryParams = new OrchestratorStateRetryParams();
        retryParams.setMaxRetry(RECOVER_OPERATION_RETRY_COUNT);
        stateParams.setStateRetryParams(retryParams);
        return stateParams;
    }

    private Map<String, Object> createRecoverParams() {
//        Map<String, String> sshParams = new HashMap<>();
//        sshParams.put("user", user);
//        sshParams.put("comment", authKeysComment);
//        if (keyPair != null) {
//            sshParams.put("publickey", PkiUtil.convertOpenSshPublicKey(keyPair.getPublic()));
//        }
//        return Map.of("tmpssh", sshParams);
        return Map.of();
    }

}
