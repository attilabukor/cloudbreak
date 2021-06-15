package com.sequenceiq.cloudbreak.service.sharedservice;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@ExtendWith({MockitoExtension.class})
public class DatalakeServiceTest {

    @Mock
    private StackService stackService;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private DatalakeService underTest;

    @BeforeEach
    public void setup() {
        Stack resultStack = new Stack();
        resultStack.setName("teststack");
        lenient().when(stackService.getByCrn(anyString())).thenReturn(resultStack);
    }

    @Test
    public void testPrepareDatalakeRequestWhenDatalakeCrnIsNotNull() {
        Stack source = new Stack();
        source.setDatalakeCrn("crn");
        StackV4Request x = new StackV4Request();
        underTest.prepareDatalakeRequest(source, x);
        verify(stackService, times(1)).getByCrn("crn");
    }

    @Test
    public void testPrepareDatalakeRequestWhenDatalakeCrnIsNull() {
        Stack source = new Stack();
        source.setDatalakeCrn(null);
        StackV4Request x = new StackV4Request();
        underTest.prepareDatalakeRequest(source, x);
        verify(stackService, never()).getByCrn("crn");
    }

    @Test
    public void testAddSharedServiceResponseWhenDatalakeCrnIsNotNull() {
        Stack source = new Stack();
        source.setDatalakeCrn("crn");
        StackV4Response x = new StackV4Response();
        underTest.addSharedServiceResponse(source, x);
        verify(stackService, times(1)).getByCrn("crn");
    }

    @Test
    public void testAddSharedServiceResponseWhenDatalakeCrnIsNull() {
        Stack source = new Stack();
        source.setDatalakeCrn(null);
        StackV4Response x = new StackV4Response();
        underTest.addSharedServiceResponse(source, x);
        verify(stackService, never()).getByCrn("crn");
    }

    @Test
    public void testGetDatalakeStackByDatahubStackWhereDatalakeCrnIsNull() {
        Stack stack = new Stack();
        stack.setDatalakeCrn(null);
        underTest.getDatalakeStackByDatahubStack(stack);
        verify(stackService, never()).getByCrn("crn");
    }

    @Test
    public void testGetDatalakeStackByDatahubStackWhereDatalakeCrnIsNotNull() {
        Stack stack = new Stack();
        stack.setDatalakeCrn("crn");
        underTest.getDatalakeStackByDatahubStack(stack);
        verify(stackService, times(1)).getByCrn("crn");
    }
}
