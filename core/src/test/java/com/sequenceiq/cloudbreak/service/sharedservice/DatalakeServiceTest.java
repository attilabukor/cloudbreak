package com.sequenceiq.cloudbreak.service.sharedservice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.cmtemplate.utils.BlueprintUtils;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.DatalakeResources;
import com.sequenceiq.cloudbreak.service.datalake.DatalakeResourcesService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class DatalakeServiceTest {

    @Mock
    private DatalakeResourcesService datalakeResourcesService;

    @Mock
    private StackService stackService;

    @Mock
    private BlueprintUtils blueprintUtils;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private DatalakeService underTest;

    @Before
    public void setup() {
        Stack resultStack = new Stack();
        resultStack.setName("teststack");
        when(stackService.getByCrn(anyString())).thenReturn(resultStack);

        DatalakeResources resultDatalakeResource = new DatalakeResources();
        resultDatalakeResource.setName("testdl");
        when(datalakeResourcesService.findById(any())).thenReturn(Optional.of(resultDatalakeResource));
    }

    @Test
    public void testPrepareDatalakeRequestWhenDatalakeCrnIsNotNull() {
        Stack source = new Stack();
        source.setDatalakeCrn("crn");
        source.setDatalakeResourceId(1L);
        StackV4Request x = new StackV4Request();
        underTest.prepareDatalakeRequest(source, x);
        verify(stackService, times(1)).getByCrn("crn");
        verify(datalakeResourcesService, never()).findById(1L);
    }

    @Test
    public void testPrepareDatalakeRequestWhenDatalakeCrnIsNull() {
        Stack source = new Stack();
        source.setDatalakeCrn(null);
        source.setDatalakeResourceId(1L);
        StackV4Request x = new StackV4Request();
        underTest.prepareDatalakeRequest(source, x);
        verify(stackService, never()).getByCrn("crn");
        verify(datalakeResourcesService, times(1)).findById(1L);
    }

    @Test
    public void testAddSharedServiceResponseWhenDatalakeCrnIsNotNull() {
        Stack source = new Stack();
        source.setDatalakeCrn("crn");
        source.setDatalakeResourceId(1L);
        StackV4Response x = new StackV4Response();
        underTest.addSharedServiceResponse(source, x);
        verify(stackService, times(1)).getByCrn("crn");
        verify(datalakeResourcesService, never()).findById(1L);
    }

    @Test
    public void testAddSharedServiceResponseWhenDatalakeCrnIsNull() {
        Stack source = new Stack();
        source.setDatalakeCrn(null);
        source.setDatalakeResourceId(1L);
        StackV4Response x = new StackV4Response();
        underTest.addSharedServiceResponse(source, x);
        verify(stackService, never()).getByCrn("crn");
        verify(datalakeResourcesService, times(1)).findById(1L);
    }

    @Test
    public void testGetDatalakeStackByDatahubStackWhereDatalakeCrnIsNull() {
        Stack stack = new Stack();
        stack.setDatalakeCrn(null);
        stack.setDatalakeResourceId(1L);
        underTest.getDatalakeStackByDatahubStack(stack);
        verify(stackService, never()).getByCrn("crn");
        verify(datalakeResourcesService, times(1)).findById(1L);
    }

    @Test
    public void testGetDatalakeStackByDatahubStackWhereDatalakeCrnIsNotNull() {
        Stack stack = new Stack();
        stack.setDatalakeCrn("crn");
        stack.setDatalakeResourceId(1L);
        underTest.getDatalakeStackByDatahubStack(stack);
        verify(stackService, times(1)).getByCrn("crn");
        verify(datalakeResourcesService, never()).findById(1L);
    }
}
