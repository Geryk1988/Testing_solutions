package com.ubs.dqs.idq_gsnowintg.model;

import lombok.Builder;
import lombok.Data;

/**
 * Represents the 10 extracted fields written to the consumer output file.
 */
@Data
@Builder
public class IncidentRecord {

    /** 1. Incident number */
    private String number;

    /** 2. Service impacted (display value) */
    private String serviceOffering;

    /** 3. Short description */
    private String shortDescription;

    /** 4. Message timestamp — formatted as dd-MM-yyyy'T'HH:mm:ss.SSSZ */
    private String formattedTimestamp;

    /** 5. State / status display value */
    private String state;

    /** 6. Created date — formatted as yyyy-MM-dd HH:mm:ss */
    private String formattedCreatedOn;

    /** 7. Resolved / end date — formatted as yyyy-MM-dd HH:mm:ss */
    private String formattedClosedAt;

    /** 8. Assigned-to display name */
    private String assignedTo;

    /** 9. Assigned-to username */
    private String assignedToUserName;

    /** 10. Assignment group display value */
    private String assignmentGroup;

    /**
     * Produces the hash-delimited row written to the consumer output file,
     * matching the original String.join("$", ...) output exactly.
     */
    public String toHashRow() {
        return String.join("$",
            nullSafe(number),
            nullSafe(serviceOffering),
            nullSafe(shortDescription),
            nullSafe(formattedTimestamp),
            nullSafe(state),
            nullSafe(formattedCreatedOn),
            nullSafe(formattedClosedAt),
            nullSafe(assignedTo),
            nullSafe(assignedToUserName),
            nullSafe(assignmentGroup)
        );
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
