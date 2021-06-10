package com.sequenceiq.cloudbreak.service.cluster;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;

@Service
public class VolumeSetManagerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRepairService.class);

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    public void updateVolumesDeleteFlag(Stack stack, Predicate<Resource> resourceFilter, boolean deleteVolumes) {
        List<Resource> volumes = resourceService.findByStackIdAndType(stack.getId(), stack.getDiskResourceType());
        volumes = volumes.stream()
                .filter(resourceFilter)
                .map(volumeSet -> updateDeleteVolumesFlag(deleteVolumes, volumeSet))
                .collect(toList());
        List<String> volumeNames = volumes.stream().map(Resource::getResourceName).collect(toList());
        LOGGER.info("Update delete volume flag on {} to {}", volumeNames, deleteVolumes);
        resourceService.saveAll(volumes);
    }

    private Resource updateDeleteVolumesFlag(boolean deleteVolumes, Resource volumeSet) {
        Optional<VolumeSetAttributes> attributes = resourceAttributeUtil.getTypedAttributes(volumeSet, VolumeSetAttributes.class);
        attributes.ifPresent(volumeSetAttributes -> {
            volumeSetAttributes.setDeleteOnTermination(deleteVolumes);
            resourceAttributeUtil.setTypedAttributes(volumeSet, volumeSetAttributes);
        });
        return volumeSet;
    }
}
