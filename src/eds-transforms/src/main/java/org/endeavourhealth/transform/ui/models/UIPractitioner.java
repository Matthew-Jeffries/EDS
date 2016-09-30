package org.endeavourhealth.transform.ui.models;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UIPractitioner {
    private UIHumanName name;
    private Boolean active;

    public UIHumanName getName() {
        return name;
    }

    public UIPractitioner setName(UIHumanName name) {
        this.name = name;
        return this;
    }

    public Boolean getActive() {
        return active;
    }

    public UIPractitioner setActive(Boolean active) {
        this.active = active;
        return this;
    }
}
