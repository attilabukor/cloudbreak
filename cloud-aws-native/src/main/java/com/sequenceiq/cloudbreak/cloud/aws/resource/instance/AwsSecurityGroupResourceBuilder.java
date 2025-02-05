package com.sequenceiq.cloudbreak.cloud.aws.resource.instance;

import static com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util.SecurityGroupBuilderUtil.SECURITY_GROUP_NAME;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.aws.AwsNativeModel;
import com.sequenceiq.cloudbreak.cloud.aws.AwsNativeModelBuilder;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsModelService;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.ModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util.SecurityGroupBuilderUtil;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsCloudStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.common.api.type.ResourceType;

@Component
public class AwsSecurityGroupResourceBuilder extends AbstractAwsNativeComputeBuilder {

    private static final Logger LOGGER = getLogger(AwsSecurityGroupResourceBuilder.class);

    @Inject
    private AwsModelService awsModelService;

    @Inject
    private AwsNativeModelBuilder awsNativeModelBuilder;

    @Inject
    private SecurityGroupBuilderUtil securityGroupBuilderUtil;

    @Override
    public List<CloudResource> create(AwsContext context, long privateId, AuthenticatedContext auth, Group group, Image image) {
        CloudContext cloudContext = auth.getCloudContext();
        String securityGroupName = getResourceNameService().resourceName(resourceType(), context.getName(), group.getName(), cloudContext.getId());
        return Collections.singletonList(createNamedResource(resourceType(), securityGroupName));
    }

    @Override
    public List<CloudResource> build(AwsContext context, long privateId, AuthenticatedContext ac, Group group, List<CloudResource> buildableResource,
            CloudStack cloudStack) throws Exception {
        AmazonEc2Client amazonEc2Client = context.getAmazonEc2Client();
        AwsCloudStackView awsCloudStackView = new AwsCloudStackView(cloudStack);
        ModelContext modelContext = awsModelService.buildDefaultModelContext(ac, cloudStack, null);
        AwsNativeModel awsNativeModel = awsNativeModelBuilder.build(modelContext);
        Map<String, String> resources = securityGroupBuilderUtil.createSecurityGroupIfNeed(awsCloudStackView, group, amazonEc2Client, ac.getCloudContext(),
                awsNativeModel);
        resources.forEach(context::putParameter);
        return Collections.singletonList(createNamedResource(resourceType(), resources.get(SECURITY_GROUP_NAME)));
    }

    @Override
    public CloudResource delete(AwsContext context, AuthenticatedContext auth, CloudResource resource) throws Exception {
        return null;
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.AWS_SECURITY_GROUP;
    }

    @Override
    public int order() {
        return 0;
    }
}
