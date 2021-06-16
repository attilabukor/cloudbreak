package com.sequenceiq.cloudbreak.service.upgrade.sync;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.upgrade.UpgradeImageInfoFactory;

@Service
public class CmVersionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CmVersionService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterApiConnectors apiConnectors;

    @Inject
    private UpgradeImageInfoFactory upgradeImageInfoFactory;

    /**
     * 1. Will query the CM for the current parcel versions
     * 2. Using the CM versions, will look for a matching component among the provided candidateProducts
     * A product is matching if the name and version both match.
     * @param stackId The id of the stack whose CM CB is checking
     * @param candidateProducts A list of ClouderaManagerProducts - parcels.
     * @return
     */
    public List<ClouderaManagerProduct> getInstalledProducts(long stackId, List<ClouderaManagerProduct> candidateProducts) {
        List<ParcelInfo> installedParcels = getParcels(stackId);
        return installedParcels.stream()
                .map(ip -> {
                    List<ClouderaManagerProduct> matchingProducts = candidateProducts.stream()
                            .filter(cp -> ip.getName().equals(cp.getName()))
                            .filter(cp -> ip.getVersion().equals(cp.getVersion()))
                            .collect(Collectors.toList());
                    return matchingProducts.stream().findFirst();
                })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Will query all parcels (CDH and non-CDH as well) from the CM server.Received format:
     * CDH -> 7.2.7-1.cdh7.2.7.p7.12569826
     *
     * TODO: the call is blocking for some time. Check if it is possible to block for shorter period of time.
     * TODO: might go to its own class
     *
     * @param stackId The id of the stack of which CM cloudbreak should query
     * @return
     */
    private List<ParcelInfo> getParcels(long stackId) {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        Map<String, String> installedParcels = apiConnectors.getConnector(stack).gatherInstalledParcels(stack.getName());
        LOGGER.debug("found parcels: " + installedParcels);
        return installedParcels.entrySet().stream()
                .map(es -> new ParcelInfo(es.getKey(), es.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * TODO: might go to its own class
     */
    private static class ParcelInfo {
        private String name;
        private String version;

        public ParcelInfo(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }
    }
}
// TODO change usage: readParcels => readParcelsWithHttpInfo