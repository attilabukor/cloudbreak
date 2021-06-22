package com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sequenceiq.flow.api.model.FlowIdentifier;

import io.swagger.annotations.ApiModel;

@ApiModel("FreeIpaUpgradeV1Response")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FreeIpaUpgradeResponse {

    private FlowIdentifier flowIdentifier;

    private ImageInfoResponse image;

    public FlowIdentifier getFlowIdentifier() {
        return flowIdentifier;
    }

    public void setFlowIdentifier(FlowIdentifier flowIdentifier) {
        this.flowIdentifier = flowIdentifier;
    }

    public ImageInfoResponse getImage() {
        return image;
    }

    public void setImage(ImageInfoResponse image) {
        this.image = image;
    }

    @Override
    public String toString() {
        return "FreeIpaUpgradeResponse{" +
                "flowIdentifier=" + flowIdentifier +
                ", image=" + image +
                '}';
    }
}
