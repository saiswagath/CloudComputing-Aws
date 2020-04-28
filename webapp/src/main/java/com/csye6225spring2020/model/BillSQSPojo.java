package com.csye6225spring2020.model;

import java.util.List;
import java.util.UUID;

public class BillSQSPojo {

    private String email;
    private List<UUID> dueBillIds;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<UUID> getDueBillIds() {
        return dueBillIds;
    }

    public void setDueBillIds(List<UUID> dueBillIds) {
        this.dueBillIds = dueBillIds;
    }

    @Override
    public String toString() {
        return "BillSQSPojo{" +
                "email='" + email + '\'' +
                ", dueBillIds=" + dueBillIds +
                '}';
    }
}
