package com.sequenceiq.freeipa.flow.freeipa.upgrade;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.image.ImageSettingsRequest;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class UpgradeEvent extends StackEvent {

    @SuppressWarnings("IllegalType")
    private final HashSet<String> instanceIds;

    private final String primareGwInstanceId;

    private final String operationId;

    private final ImageSettingsRequest imageSettingsRequest;

    private final boolean backupSet;

    @JsonCreator
    public UpgradeEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("instanceIds") HashSet<String> instanceIds,
            @JsonProperty("primareGwInstanceId") String primareGwInstanceId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("imageSettingsRequest") ImageSettingsRequest imageSettingsRequest,
            @JsonProperty("backupSet") boolean backupSet) {
        super(selector, stackId);
        this.instanceIds = instanceIds;
        this.primareGwInstanceId = primareGwInstanceId;
        this.operationId = operationId;
        this.imageSettingsRequest = imageSettingsRequest;
        this.backupSet = backupSet;
    }

    public Set<String> getInstanceIds() {
        return instanceIds;
    }

    public String getPrimareGwInstanceId() {
        return primareGwInstanceId;
    }

    public String getOperationId() {
        return operationId;
    }

    public ImageSettingsRequest getImageSettingsRequest() {
        return imageSettingsRequest;
    }

    public boolean isBackupSet() {
        return backupSet;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(UpgradeEvent.class, other,
                event -> Objects.equals(operationId, event.operationId)
                        && Objects.equals(instanceIds, event.instanceIds)
                        && Objects.equals(primareGwInstanceId, event.primareGwInstanceId)
                        && Objects.equals(imageSettingsRequest, event.imageSettingsRequest)
                        && backupSet == event.backupSet);
    }

}
