package org.sagebionetworks.bridge.sdk.models.studies;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Objects;

import static org.sagebionetworks.bridge.sdk.utils.BridgeUtils.TO_STRING_STYLE;

/**
 * Represent a study summary object
 */
@JsonDeserialize(as=StudySummary.class)
public class StudySummary {
    private String name;
    private String identifier;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, identifier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        StudySummary other = (StudySummary) obj;
        return (Objects.equals(name, other.name)
                && Objects.equals(identifier, other.identifier));
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, TO_STRING_STYLE)
                .append("name", name)
                .append("identifier", identifier)
                .toString();
    }
}