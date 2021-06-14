package com.sequenceiq.cloudbreak.cloud.template.context;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;

public class VolumeMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(VolumeMatcher.class);

    private VolumeMatcher() {
    }

    public static void addVolumeResourcesToContext(List<CloudInstance> instances, List<CloudResource> groupInstances, List<CloudResource> groupVolumeSets,
            ResourceBuilderContext context) {
        List<CloudResource> groupVolumeSetsWithFQDN = groupVolumeSets.stream()
                .filter(groupVolumeSet -> groupVolumeSet.getParameter("attributes", VolumeSetAttributes.class).getDiscoveryFQDN() != null)
                .collect(Collectors.toList());
        LOGGER.info("Volume sets with FQDN: " + groupVolumeSetsWithFQDN);
        List<CloudResource> groupVolumeSetsWithoutFQDN = groupVolumeSets.stream()
                .filter(groupVolumeSet -> groupVolumeSet.getParameter("attributes", VolumeSetAttributes.class).getDiscoveryFQDN() == null)
                .collect(Collectors.toList());
        LOGGER.info("Volume sets without FQDN: " + groupVolumeSetsWithoutFQDN);
        for (int i = 0; i < instances.size(); i++) {
            if (i < groupInstances.size()) {
                CloudInstance cloudInstance = instances.get(i);
                CloudResource instanceResource = groupInstances.get(i);
                String fqdn = cloudInstance.getParameter(CloudInstance.FQDN, String.class);
                LOGGER.info("FQDN for {}: {} ", cloudInstance, fqdn);
                List<CloudResource> computeResource;
                if (fqdn != null) {
                    Optional<CloudResource> volumeSetForFQDN = groupVolumeSetsWithFQDN.stream()
                            .filter(groupVolumeSet -> fqdn.equals(groupVolumeSet.getParameter("attributes", VolumeSetAttributes.class).getDiscoveryFQDN()))
                            .findFirst();
                    volumeSetForFQDN.ifPresent(cloudResource -> LOGGER.info("Volume set for this fqdn: {}", cloudResource));
                    computeResource = volumeSetForFQDN.map(cloudResource -> List.of(instanceResource, cloudResource))
                            .orElseGet(() -> getComputeResourceFromVolumesWithoutFQDN(groupVolumeSetsWithoutFQDN, instanceResource));
                } else {
                    computeResource = getComputeResourceFromVolumesWithoutFQDN(groupVolumeSetsWithoutFQDN, instanceResource);
                }
                context.addComputeResources(cloudInstance.getTemplate().getPrivateId(), computeResource);
            }
        }
    }

    private static List<CloudResource> getComputeResourceFromVolumesWithoutFQDN(List<CloudResource> groupVolumeSetsWithoutFQDN,
            CloudResource instanceResource) {
        if (groupVolumeSetsWithoutFQDN.isEmpty()) {
            LOGGER.info("There is no volume without fqdn to attach so we will not attach anything for this instance: {}", instanceResource);
            return List.of(instanceResource);
        } else {
            CloudResource groupVolumeSet = groupVolumeSetsWithoutFQDN.remove(0);
            LOGGER.info("There is a volume without fqdn so we will attach {} to this instance: {}", groupVolumeSet, instanceResource);
            return List.of(instanceResource, groupVolumeSet);
        }
    }

}
