package com.sequenceiq.cloudbreak.cloud.model;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

public class CloudCredential extends DynamicModel {

    public static final String GOV_CLOUD = "govCloud";

    private String id;

    private String name;

    private boolean verifyPermissions;

    private String accountId;

    public CloudCredential() {
    }

    public CloudCredential(String id, String name, String account) {
        this(id, name, new HashMap<>(), account, false);
    }

    public CloudCredential(String id, String name, String account, boolean verifyPermissions) {
        this(id, name, new HashMap<>(), account, verifyPermissions);
    }

    public CloudCredential(String id, String name, Map<String, Object> parameters, String acountId, boolean verifyPermissions) {
        super(parameters);
        this.id = id;
        this.name = name;
        this.verifyPermissions = verifyPermissions;
        this.accountId = acountId;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isVerifyPermissions() {
        return verifyPermissions;
    }

    public void setVerifyPermissions(boolean verifyPermissions) {
        this.verifyPermissions = verifyPermissions;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    // Must not reveal any secrets, hence not including DynamicModel.toString()!
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("CloudCredential{");
        sb.append("id='").append(id).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", verifyPermissions=").append(verifyPermissions);
        sb.append('}');
        return sb.toString();
    }

}
