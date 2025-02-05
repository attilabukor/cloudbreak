package com.sequenceiq.environment.environment.validation.securitygroup.gcp;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.gcp.util.GcpStackUtil;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroup;
import com.sequenceiq.cloudbreak.cloud.model.CloudSecurityGroups;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.CdpResourceType;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentValidationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.validation.securitygroup.EnvironmentSecurityGroupValidator;
import com.sequenceiq.environment.platformresource.PlatformParameterService;
import com.sequenceiq.environment.platformresource.PlatformResourceRequest;

@Component
public class GcpEnvironmentSecurityGroupValidator implements EnvironmentSecurityGroupValidator {

    private PlatformParameterService platformParameterService;

    public GcpEnvironmentSecurityGroupValidator(PlatformParameterService platformParameterService) {
        this.platformParameterService = platformParameterService;
    }

    @Override
    public void validate(EnvironmentValidationDto environmentValidationDto, ValidationResult.ValidationResultBuilder resultBuilder) {
        EnvironmentDto environmentDto = environmentValidationDto.getEnvironmentDto();
        SecurityAccessDto securityAccessDto = environmentDto.getSecurityAccess();
        if (securityAccessDto != null) {
            if (onlyOneSecurityGroupIdDefined(securityAccessDto)) {
                resultBuilder.error(securityGroupIdsMustBePresent());
            } else if (isSecurityGroupIdDefined(securityAccessDto)) {
                if (!Strings.isNullOrEmpty(securityAccessDto.getDefaultSecurityGroupId())) {
                    validateSecurityGroup(environmentDto, resultBuilder, securityAccessDto.getDefaultSecurityGroupId());
                }
                if (!Strings.isNullOrEmpty(securityAccessDto.getSecurityGroupIdForKnox())) {
                    validateSecurityGroup(environmentDto, resultBuilder, securityAccessDto.getSecurityGroupIdForKnox());
                }
            }
        }
    }

    private void validateSecurityGroup(EnvironmentDto environmentDto, ValidationResult.ValidationResultBuilder resultBuilder, String securityGroupId) {
        Region region = environmentDto.getRegions().iterator().next();
        PlatformResourceRequest request = platformParameterService.getPlatformResourceRequest(
                environmentDto.getAccountId(),
                environmentDto.getCredential().getName(),
                null,
                region.getName(),
                getCloudPlatform().name(),
                null,
                CdpResourceType.DEFAULT);

        Map<String, String> filters = new HashMap<>();
        if (!Strings.isNullOrEmpty(environmentDto.getNetwork().getGcp().getSharedProjectId())) {
            filters.put(GcpStackUtil.SHARED_PROJECT_ID, environmentDto.getNetwork().getGcp().getSharedProjectId());
        }
        request.setFilters(filters);

        CloudSecurityGroups securityGroups = platformParameterService.getSecurityGroups(request);

        boolean securityGroupFoundInRegion = false;
        if (Objects.nonNull(securityGroups.getCloudSecurityGroupsResponses())
                && Objects.nonNull(securityGroups.getCloudSecurityGroupsResponses().get(region.getName()))) {
            for (CloudSecurityGroup cloudSecurityGroup : securityGroups.getCloudSecurityGroupsResponses().get(region.getName())) {
                String groupId = cloudSecurityGroup.getGroupId();
                if (groupId.equalsIgnoreCase(securityGroupId)) {
                    securityGroupFoundInRegion = true;
                    break;
                }
            }
        }
        if (!securityGroupFoundInRegion) {
            resultBuilder.error(securityGroupNotInTheSameRegion(securityGroupId, region.getName()));
        }
    }

    @Override
    public String securityGroupNotInTheSameRegion(String securityGroupId, String region) {
        return String.format("The '%s' security group must exists on Google Cloud side that you defined in the request!", securityGroupId);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return GCP;
    }

}
