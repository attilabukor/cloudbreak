package com.sequenceiq.cloudbreak.cloud.gcp.group;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.InstanceGroups.AddInstances;
import com.google.api.services.compute.Compute.InstanceGroups.Delete;
import com.google.api.services.compute.Compute.InstanceGroups.Insert;
import com.google.api.services.compute.Compute.InstanceGroups.ListInstances;
import com.google.api.services.compute.Compute.InstanceGroups.RemoveInstances;
import com.google.api.services.compute.model.InstanceGroup;
import com.google.api.services.compute.model.InstanceGroupsAddInstancesRequest;
import com.google.api.services.compute.model.InstanceGroupsListInstances;
import com.google.api.services.compute.model.InstanceGroupsListInstancesRequest;
import com.google.api.services.compute.model.InstanceGroupsRemoveInstancesRequest;
import com.google.api.services.compute.model.InstanceReference;
import com.google.api.services.compute.model.InstanceWithNamedPorts;
import com.google.api.services.compute.model.Operation;
import com.google.common.collect.Sets;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.common.api.type.ResourceType;

/**
 * The Group Resource Builder that is responsible for the GCP API calls to manage an instance group
 * This currently only defines unmanaged instance groups
 * In GCP an instance group is a collection of Compute Instances in the same Zone and subnet
 * An unmanaged instace group is only used to be applied as a target of a load balancer backend
 * An instance shoup be added to at most 1 load balanced instance group
 * This creates a Instance Group for every group defined in a stack.
 */

public class GcpInstanceGroupResourceBuilder extends AbstractGcpGroupBuilder {

    private static final int ORDER = 1;

    @Override
    public CloudResource create(GcpContext context, AuthenticatedContext auth, Group group, Network network) {
        String resourceName = getResourceNameService().resourceName(resourceType(), context.getName(), group.getName());
        return createNamedResource(resourceType(), resourceName);
    }

    @Override
    public CloudResource build(GcpContext context,
            AuthenticatedContext auth, Group group,
            Network network, Security security, CloudResource resource) throws Exception {

        Insert insert = context.getCompute().instanceGroups().insert(context.getProjectId(),
                context.getLocation().getAvailabilityZone().value(), new InstanceGroup().setName(resource.getName()));

        return doOperationalRequest(resource, insert);
    }

    @Override
    public CloudResourceStatus update(GcpContext context, AuthenticatedContext auth, Group group, Network network, Security security, CloudResource resource) {
        return null;
    }

    /*
     * compute difference between current Group and GCP Instance Group, remove any no longer needed then add new
     */
    protected void updateInstanceList(GcpContext context, AuthenticatedContext auth,
            Group group, Network network, Security security, CloudResource resource) throws IOException {
        Compute compute = context.getCompute();
        String projectId = context.getProjectId();
        Location location = context.getLocation();

        InstanceGroupsListInstancesRequest listInstancesRequest = new InstanceGroupsListInstancesRequest();
        ListInstances listInstances = compute.instanceGroups().listInstances(projectId,
                location.getAvailabilityZone().value(), resource.getName(), listInstancesRequest);

        List<CloudInstance> instances = group.getInstances();
        Set<String> existingURLs = new HashSet<>();
        Set<String> requestedURLs = new HashSet<>();
        for (CloudInstance instance : instances) {
            requestedURLs.add(String.format("https://www.googleapis.com/compute/v1/projects/%s/zones/%s/instances/%s",
                    projectId, location.getAvailabilityZone().value(), instance.getInstanceId()));
        }
        try {
            InstanceGroupsListInstances listInstancesResponse = listInstances.execute();
            if (listInstancesResponse.getItems() != null) {
                for (InstanceWithNamedPorts item : listInstancesResponse.getItems()) {
                    existingURLs.add(item.getInstance());
                }
            }
        } catch (GoogleJsonResponseException e) {
            throw new GcpResourceException(checkException(e), resourceType(), resource.getName());
        }
        Set<String> removed = Sets.difference(existingURLs, requestedURLs);
        Set<String> added = Sets.difference(requestedURLs, existingURLs);


        if (!removed.isEmpty()) {
            List<InstanceReference> deleteReferences = new ArrayList<>();
            for (String instance : removed) {
                deleteReferences.add(new InstanceReference().setInstance(instance));
            }
            RemoveInstances removeInstances = compute.instanceGroups().removeInstances(projectId,
                    location.getAvailabilityZone().value(), resource.getName(),
                    new InstanceGroupsRemoveInstancesRequest().setInstances(deleteReferences));
            doOperationalRequest(resource, removeInstances);
        }

        if (!added.isEmpty()) {
            List<InstanceReference> addReferences = new ArrayList<>();
            for (String instance : added) {
                addReferences.add(new InstanceReference().setInstance(instance));
            }
            AddInstances addInstances = compute.instanceGroups().addInstances(projectId,
                    location.getAvailabilityZone().value(), resource.getName(),
                    new InstanceGroupsAddInstancesRequest().setInstances(addReferences));
            doOperationalRequest(resource, addInstances);
        }
    }

    @Override
    public CloudResource delete(GcpContext context, AuthenticatedContext auth, CloudResource resource, Network network) throws Exception {
        Delete delete = context.getCompute().instanceGroups().delete(context.getProjectId(),
                context.getLocation().getAvailabilityZone().value(), resource.getName());
        try {
            Operation operation = delete.execute();
            return createOperationAwareCloudResource(resource, operation);
        } catch (GoogleJsonResponseException e) {
            exceptionHandler(e, resource.getName(), resourceType());
            return null;
        }
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.GCP_INSTANCE_GROUP;
    }

    @Override
    public int order() {
        return ORDER;
    }
}
