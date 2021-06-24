package com.sequenceiq.cloudbreak.cloud.gcp.group;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.api.services.compute.Compute;
import com.google.api.services.compute.Compute.InstanceGroups;
import com.google.api.services.compute.model.InstanceGroupsAddInstancesRequest;
import com.google.api.services.compute.model.InstanceGroupsListInstances;
import com.google.api.services.compute.model.Operation;
import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.gcp.GcpResourceException;
import com.sequenceiq.cloudbreak.cloud.gcp.context.GcpContext;
import com.sequenceiq.cloudbreak.cloud.gcp.service.GcpResourceNameService;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
public class GcpInstanceGroupResourceBuilderTest {

    @Mock
    private GcpStackUtil gcpStackUtil;

    @InjectMocks
    private GcpInstanceGroupResourceBuilder underTest;

    @Mock
    private Network network;

    @Mock
    private GcpContext gcpContext;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private Compute compute;

    @Mock
    private Location location;

    @Mock
    private AvailabilityZone availabilityZone;

    @Mock
    private InstanceGroups instanceGroups;

    @Mock
    private InstanceGroups.Delete instanceGroupsDelete;

    @Mock
    private Operation operation;

    @Mock
    private Security security;

    @Mock
    private Group group;

    @BeforeEach
    private void setup() {
        GcpResourceNameService resourceNameService = new GcpResourceNameService();
        ReflectionTestUtils.setField(resourceNameService, "maxResourceNameLength", 50);
        ReflectionTestUtils.setField(underTest, "resourceNameService", resourceNameService);
    }

    @Test
    public void testDeleteWhenEverythingGoesFine() throws Exception {
        CloudResource resource = new CloudResource.Builder()
                .type(ResourceType.GCP_INSTANCE_GROUP)
                .status(CommonStatus.CREATED)
                .group("master")
                .name("super")
                .instanceId("id-123")
                .params(new HashMap<>())
                .persistent(true)
                .build();

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn("id");
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone);
        when(availabilityZone.value()).thenReturn("zone");
        when(compute.instanceGroups()).thenReturn(instanceGroups);
        when(instanceGroups.delete(anyString(), anyString(), anyString())).thenReturn(instanceGroupsDelete);
        when(instanceGroupsDelete.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");

        CloudResource delete = underTest.delete(gcpContext, authenticatedContext, resource, network);

        Assert.assertEquals(ResourceType.GCP_INSTANCE_GROUP, delete.getType());
        Assert.assertEquals(CommonStatus.CREATED, delete.getStatus());
        Assert.assertEquals("super", delete.getName());
        Assert.assertEquals("master", delete.getGroup());
        Assert.assertEquals("id-123", delete.getInstanceId());
    }

    @Test
    public void testCreateWhenEverythingGoesFine() throws Exception {
        when(gcpContext.getName()).thenReturn("name");
        when(group.getName()).thenReturn("group");

        CloudResource cloudResource = underTest.create(gcpContext, authenticatedContext, group, network);

        Assertions.assertTrue(cloudResource.getName().startsWith("name-group"));
    }

    @Test
    public void testBuild() throws Exception {
        CloudResource resource = new CloudResource.Builder()
                .type(ResourceType.GCP_INSTANCE_GROUP)
                .status(CommonStatus.CREATED)
                .group("master")
                .name("super")
                .instanceId("id-123")
                .params(new HashMap<>())
                .persistent(true)
                .build();
        Compute.InstanceGroups.Insert instanceGroupsInsert = mock(Compute.InstanceGroups.Insert.class);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn("id");
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone);
        when(availabilityZone.value()).thenReturn("zone");

        when(compute.instanceGroups()).thenReturn(instanceGroups);
        when(instanceGroups.insert(anyString(), anyString(), any())).thenReturn(instanceGroupsInsert);
        when(instanceGroupsInsert.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");
        when(operation.getHttpErrorStatusCode()).thenReturn(null);

        CloudResource cloudResource = underTest.build(gcpContext, authenticatedContext, group, network, security, resource);

        Assert.assertEquals("super", cloudResource.getName());

    }

    @Test
    public void testBuildNoPermission() throws Exception {
        CloudResource resource = new CloudResource.Builder()
                .type(ResourceType.GCP_INSTANCE_GROUP)
                .status(CommonStatus.CREATED)
                .group("master")
                .name("super")
                .instanceId("id-123")
                .params(new HashMap<>())
                .persistent(true)
                .build();
        Compute.InstanceGroups.Insert instanceGroupsInsert = mock(Compute.InstanceGroups.Insert.class);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn("id");
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone);
        when(availabilityZone.value()).thenReturn("zone");

        when(compute.instanceGroups()).thenReturn(instanceGroups);
        when(instanceGroups.insert(anyString(), anyString(), any())).thenReturn(instanceGroupsInsert);
        when(instanceGroupsInsert.execute()).thenReturn(operation);
        when(operation.getHttpErrorStatusCode()).thenReturn(401);
        when(operation.getHttpErrorMessage()).thenReturn("Not Authorized");

        Assert.assertThrows("Not Authorized", GcpResourceException.class,
                () -> underTest.build(gcpContext, authenticatedContext, group, network, security, resource));
    }

    @Test
    public void testUpdate() throws Exception {
        CloudResource resource = new CloudResource.Builder()
                .type(ResourceType.GCP_INSTANCE_GROUP)
                .status(CommonStatus.CREATED)
                .group("master")
                .name("super")
                .instanceId("id-123")
                .params(new HashMap<>())
                .persistent(true)
                .build();
        CloudInstance instance1 = mock(CloudInstance.class);
        CloudInstance instance2 = mock(CloudInstance.class);
        List<CloudInstance> instances = Lists.newArrayList(instance1, instance2);
        Compute.InstanceGroups instanceGroups = mock(Compute.InstanceGroups.class);
        Compute.InstanceGroups.Insert instanceGroupsInsert = mock(Compute.InstanceGroups.Insert.class);
        Compute.InstanceGroups.ListInstances instanceGroupsListInstances = mock(Compute.InstanceGroups.ListInstances.class);
        Compute.InstanceGroups.AddInstances instanceGroupsAddInstances = mock(Compute.InstanceGroups.AddInstances.class);

        when(gcpContext.getCompute()).thenReturn(compute);
        when(gcpContext.getProjectId()).thenReturn("id");
        when(instance1.getInstanceId()).thenReturn("inst1");
        when(instance2.getInstanceId()).thenReturn("inst2");
        when(group.getInstances()).thenReturn(instances);
        when(gcpContext.getLocation()).thenReturn(location);
        when(location.getAvailabilityZone()).thenReturn(availabilityZone);
        when(availabilityZone.value()).thenReturn("zone");

        when(compute.instanceGroups()).thenReturn(instanceGroups);
        when(instanceGroups.insert(anyString(), anyString(), any())).thenReturn(instanceGroupsInsert);
        when(instanceGroupsInsert.execute()).thenReturn(operation);
        when(operation.getName()).thenReturn("name");
        when(operation.getHttpErrorStatusCode()).thenReturn(null);

        when(instanceGroups.listInstances(anyString(), anyString(), anyString(), any())).thenReturn(instanceGroupsListInstances);
        when(instanceGroupsListInstances.execute()).thenReturn(new InstanceGroupsListInstances());
        ArgumentCaptor<InstanceGroupsAddInstancesRequest> addCaptor = ArgumentCaptor.forClass(InstanceGroupsAddInstancesRequest.class);
        when(instanceGroups.addInstances(anyString(), anyString(), anyString(), addCaptor.capture())).thenReturn(instanceGroupsAddInstances);
        when(instanceGroupsAddInstances.execute()).thenReturn(operation);

        CloudResource cloudResource = underTest.build(gcpContext, authenticatedContext, group, network, security, resource);
        underTest.updateInstanceList(gcpContext, authenticatedContext, group, network, security, resource);

        Assert.assertEquals("super", cloudResource.getName());

        InstanceGroupsAddInstancesRequest addInstancesRequest = addCaptor.getValue();
        Assert.assertEquals(2, addInstancesRequest.getInstances().size());
    }

    @Test
    public void testResourceType() {
        Assert.assertTrue(underTest.resourceType().equals(ResourceType.GCP_INSTANCE_GROUP));
    }

}
