package com.sequenceiq.cloudbreak.structuredevent.event;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.sequenceiq.cloudbreak.event.ResourceEvent;

public interface CloudbreakEventService {
    String DATAHUB_RESOURCE_TYPE = "datahub";

    String DATALAKE_RESOURCE_TYPE = "datalake";

    String ENVIRONMENT_RESOURCE_TYPE = "environment";

    String FREEIPA_RESOURCE_TYPE = "freeipa";

    String KERBEROS_RESOURCE_TYPE = "kerberos";

    String LDAP_RESOURCE_TYPE = "ldap";

    String CREDENTIAL_RESOURCE_TYPE = "credential";

    @Deprecated
    String LEGACY_RESOURCE_TYPE = "stacks";

    String CONSUMPTION_RESOURCE_TYPE = "consumption";

    void fireCloudbreakEvent(Long entityId, String eventType, ResourceEvent resourceEvent);

    void fireCloudbreakEvent(Long entityId, String eventType, ResourceEvent resourceEvent, Collection<String> eventMessageArgs);

    void fireCloudbreakInstanceGroupEvent(Long stackId, String eventType, String instanceGroupName, ResourceEvent resourceEvent,
            Collection<String> eventMessageArgs);

    List<StructuredNotificationEvent> cloudbreakEvents(Long workspaceId, Long since);

    List<StructuredNotificationEvent> cloudbreakEventsForStack(Long stackId);

    Page<StructuredNotificationEvent> cloudbreakEventsForStack(Long stackId, String stackType, Pageable pageable);
}
