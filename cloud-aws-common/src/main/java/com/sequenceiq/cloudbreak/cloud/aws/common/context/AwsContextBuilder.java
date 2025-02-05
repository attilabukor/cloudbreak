package com.sequenceiq.cloudbreak.cloud.aws.common.context;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder;

@Service
public class AwsContextBuilder implements ResourceContextBuilder<AwsContext> {

    private static final int PARALLEL_RESOURCE_REQUEST = 30;

    @Override
    public AwsContext contextInit(CloudContext context, AuthenticatedContext auth, Network network, List<CloudResource> resources, boolean build) {
        Location location = context.getLocation();
        AmazonEc2Client amazonEC2Client = new AuthenticatedContextView(auth).getAmazonEC2Client();
        return new AwsContext(context.getName(), amazonEC2Client, location, PARALLEL_RESOURCE_REQUEST, build);
    }

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_DEFAULT_VARIANT;
    }
}
