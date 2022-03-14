package com.sequenceiq.datalake.flow.refresh.event;

import com.sequenceiq.datalake.flow.SdxEvent;

public class DatahubRefreshStartEvent extends SdxEvent {
    public DatahubRefreshStartEvent(Long sdxId, String userId) {
        super(sdxId, userId);
    }
}
