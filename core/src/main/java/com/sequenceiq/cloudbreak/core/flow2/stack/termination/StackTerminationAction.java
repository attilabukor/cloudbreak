package com.sequenceiq.cloudbreak.core.flow2.stack.termination;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.resource.TerminateStackRequest;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.CcmKeyDeregisterSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;

@Component("StackTerminationAction")
public class StackTerminationAction extends AbstractStackTerminationAction<CcmKeyDeregisterSuccess> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackTerminationAction.class);

    public StackTerminationAction() {
        super(CcmKeyDeregisterSuccess.class);
    }

    @Override
    protected void doExecute(StackTerminationContext context, CcmKeyDeregisterSuccess payload, Map<Object, Object> variables) {
        TerminateStackRequest<?> terminateRequest = createRequest(context);
        sendEvent(context, terminateRequest.selector(), terminateRequest);
    }

    @Override
    protected TerminateStackRequest<?> createRequest(StackTerminationContext context) {
        List<CloudResource> cloudResources = context.getCloudResources();
        if (context.getTerminationType() == TerminationType.RECOVERY) {
            LOGGER.debug("Recovery is in progress, skipping volume-set deletion!");
            cloudResources.removeIf(cloudResource -> cloudResource.getType() == context.getStack().getDiskResourceType());
        }
        return new TerminateStackRequest<>(context.getCloudContext(), context.getCloudStack(), context.getCloudCredential(), cloudResources);
    }
}
