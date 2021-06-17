package com.sequenceiq.flow.api.model.operation;

import java.util.List;
import java.util.Map;

import com.sequenceiq.flow.api.model.FlowProgressResponse;

public class OperationFlowsView {

    private final OperationType operationType;

    private final Map<String, FlowProgressResponse> flowTypeProgressMap;

    private final List<String> typeOrderList;

    public OperationFlowsView(OperationType operationType, Map<String, FlowProgressResponse> flowTypeProgressMap, List<String> typeOrderList) {
        this.operationType = operationType;
        this.flowTypeProgressMap = flowTypeProgressMap;
        this.typeOrderList = typeOrderList;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public Map<String, FlowProgressResponse> getFlowTypeProgressMap() {
        return flowTypeProgressMap;
    }

    public List<String> getTypeOrderList() {
        return typeOrderList;
    }
}
