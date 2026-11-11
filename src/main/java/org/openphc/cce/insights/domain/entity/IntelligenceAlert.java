package org.openphc.cce.insights.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * One intelligence delivery joined to the intelligence event that caused it: what was sent, to which
 * destination, why (trigger reason), and whether it arrived.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntelligenceAlert {

    private UUID deliveryId;
    private UUID intelligenceEventId;
    private UUID protocolInstanceId;
    private UUID stepInstanceId;
    /** The protocol step the alert belongs to. */
    private String actionId;
    /** The intelligence action (nested under the step) that fired. */
    private String alertActionId;
    /** Why it fired, e.g. completion or overdue. */
    private String triggerReason;
    private String slaStatus;
    private String destination;
    /** FHIR resource type sent, e.g. ServiceRequest or CommunicationRequest. */
    private String actionType;
    private String severity;
    /** Delivery status, e.g. PENDING, DELIVERED or FAILED. */
    private String status;
    private int attemptCount;
    private Integer httpStatusCode;
    private String errorMessage;
    private OffsetDateTime createdAt;
    private OffsetDateTime deliveredAt;
}
