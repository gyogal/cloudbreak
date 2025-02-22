package com.sequenceiq.cloudbreak.cloud.service;

import java.util.List;
import java.util.Optional;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

public interface ResourceRetriever {

    Optional<CloudResource> findByResourceReferenceAndStatusAndType(String resourceReference, CommonStatus status, ResourceType resourceType);

    default Optional<CloudResource> findByResourceReferenceAndStatusAndTypeAndStack(String resourceReference, CommonStatus status, ResourceType resourceType,
            Long stackId) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default Optional<CloudResource> findByStatusAndTypeAndStack(CommonStatus status, ResourceType resourceType, Long stackId) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default List<CloudResource> findAllByStatusAndTypeAndStackAndInstanceGroup(CommonStatus status, ResourceType resourceType, Long stackId,
            String instanceGroup) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }

    default List<CloudResource> findAllByStatusAndTypeAndStack(CommonStatus status, ResourceType resourceType, Long stackId) {
        throw new UnsupportedOperationException("Interface not implemented.");
    }
}