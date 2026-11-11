package org.openphc.cce.insights.domain.repository;

import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.impl.DSL;
import org.openphc.cce.insights.domain.entity.IntelligenceAlert;
import org.openphc.cce.insights.domain.entity.IntelligenceDelivery;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.openphc.cce.insights.jooq.Tables.INTELLIGENCE_DELIVERIES;
import static org.openphc.cce.insights.jooq.Tables.INTELLIGENCE_EVENT_LOGS;

@Repository
public class IntelligenceDeliveryRepositoryImpl
        extends AbstractClickHouseRepository<IntelligenceDelivery, UUID>
        implements IntelligenceDeliveryRepository {

    public IntelligenceDeliveryRepositoryImpl(DSLContext dsl) {
        super(dsl);
    }

    @Override
    protected String getTableName() {
        return INTELLIGENCE_DELIVERIES.getName();
    }

    @Override
    protected IntelligenceDelivery fromRecord(Record r) {
        return IntelligenceDelivery.builder()
                .id(r.get(INTELLIGENCE_DELIVERIES.ID.getName(), UUID.class))
                .intelligenceEventId(parseUUID(r.get(INTELLIGENCE_DELIVERIES.INTELLIGENCE_EVENT_ID.getName(), String.class)))
                .actionDefinitionId(parseUUID(r.get(INTELLIGENCE_DELIVERIES.ACTION_DEFINITION_ID.getName(), String.class)))
                .destinationAdaptorMappingId(parseUUID(r.get(INTELLIGENCE_DELIVERIES.DESTINATION_ADAPTOR_MAPPING_ID.getName(), String.class)))
                .actionType(r.get(INTELLIGENCE_DELIVERIES.ACTION_TYPE.getName(), String.class))
                .status(r.get(INTELLIGENCE_DELIVERIES.STATUS.getName(), String.class))
                .subject(r.get(INTELLIGENCE_DELIVERIES.SUBJECT.getName(), String.class))
                .protocolCanonical(r.get(INTELLIGENCE_DELIVERIES.PROTOCOL_CANONICAL.getName(), String.class))
                .actionId(r.get(INTELLIGENCE_DELIVERIES.ACTION_ID.getName(), String.class))
                .severity(r.get(INTELLIGENCE_DELIVERIES.SEVERITY.getName(), String.class))
                .destination(r.get(INTELLIGENCE_DELIVERIES.DESTINATION.getName(), String.class))
                .attemptCount(r.get(INTELLIGENCE_DELIVERIES.ATTEMPT_COUNT.getName(), Integer.class))
                .createdAt(recordDateTime(r, INTELLIGENCE_DELIVERIES.CREATED_AT.getName()))
                .updatedAt(recordDateTime(r, INTELLIGENCE_DELIVERIES.UPDATED_AT.getName()))
                .deliveredAt(recordDateTime(r, INTELLIGENCE_DELIVERIES.DELIVERED_AT.getName()))
                .build();
    }

    private String table() {
        return INTELLIGENCE_DELIVERIES.getName() + finalClause();
    }

    @Override
    public List<Object[]> countByStatus() {
        return dsl.select(
                    DSL.field(INTELLIGENCE_DELIVERIES.STATUS.getName()),
                    DSL.field("count()", Long.class))
                  .from(DSL.table(DSL.sql(table())))
                  .groupBy(DSL.field(INTELLIGENCE_DELIVERIES.STATUS.getName()))
                  .fetch()
                  .map(r -> new Object[]{r.get(0, String.class), r.get(1, Long.class)});
    }

    @Override
    public List<Object[]> countByActionType() {
        return dsl.select(
                    DSL.field(INTELLIGENCE_DELIVERIES.ACTION_TYPE.getName()),
                    DSL.field("count()", Long.class))
                  .from(DSL.table(DSL.sql(table())))
                  .groupBy(DSL.field(INTELLIGENCE_DELIVERIES.ACTION_TYPE.getName()))
                  .fetch()
                  .map(r -> new Object[]{r.get(0, String.class), r.get(1, Long.class)});
    }

    @Override
    public List<Object[]> countBySeverity() {
        return dsl.select(
                    DSL.field(INTELLIGENCE_DELIVERIES.SEVERITY.getName()),
                    DSL.field("count()", Long.class))
                  .from(DSL.table(DSL.sql(table())))
                  .groupBy(DSL.field(INTELLIGENCE_DELIVERIES.SEVERITY.getName()))
                  .fetch()
                  .map(r -> new Object[]{r.get(0, String.class), r.get(1, Long.class)});
    }

    @Override
    public List<Object[]> countByDestination() {
        return dsl.select(
                    DSL.field(INTELLIGENCE_DELIVERIES.DESTINATION.getName()),
                    DSL.field("count()", Long.class).as("cnt"))
                  .from(DSL.table(DSL.sql(table())))
                  .groupBy(DSL.field(INTELLIGENCE_DELIVERIES.DESTINATION.getName()))
                  .orderBy(DSL.field("cnt").desc())
                  .fetch()
                  .map(r -> new Object[]{r.get(0, String.class), r.get(1, Long.class)});
    }

    @Override
    public long countDelivered() {
        Long r = dsl.select(DSL.field("count()", Long.class))
                    .from(DSL.table(DSL.sql(table())))
                    .where(DSL.field(INTELLIGENCE_DELIVERIES.STATUS.getName()).eq("DELIVERED"))
                    .fetchOne(0, Long.class);
        return r != null ? r : 0L;
    }

    @Override
    public long countFailed() {
        Long r = dsl.select(DSL.field("count()", Long.class))
                    .from(DSL.table(DSL.sql(table())))
                    .where(DSL.field(INTELLIGENCE_DELIVERIES.STATUS.getName()).eq("FAILED"))
                    .fetchOne(0, Long.class);
        return r != null ? r : 0L;
    }

    @Override
    public Double avgDeliveryLatencySeconds() {
        return dsl.select(DSL.field(
                        "avg(toFloat64(dateDiff('millisecond', " +
                        INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + ", " +
                        INTELLIGENCE_DELIVERIES.DELIVERED_AT.getName() + "))) / 1000.0",
                        Double.class))
                  .from(DSL.table(DSL.sql(table())))
                  .where(DSL.field(INTELLIGENCE_DELIVERIES.STATUS.getName()).eq("DELIVERED"))
                  .fetchOne(0, Double.class);
    }

    @Override
    public long countFiltered(OffsetDateTime startDate, OffsetDateTime endDate) {
        Long r = dsl.select(DSL.field("count()", Long.class))
                    .from(DSL.table(DSL.sql(table())))
                    .where(DSL.condition(
                            INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + " >= parseDateTime64BestEffort(?)",
                            dtStart(startDate)))
                    .and(DSL.condition(
                            INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + " <= parseDateTime64BestEffort(?)",
                            dtEnd(endDate)))
                    .fetchOne(0, Long.class);
        return r != null ? r : 0L;
    }

    @Override
    public long countDeliveredFiltered(OffsetDateTime startDate, OffsetDateTime endDate) {
        Long r = dsl.select(DSL.field("count()", Long.class))
                    .from(DSL.table(DSL.sql(table())))
                    .where(DSL.field(INTELLIGENCE_DELIVERIES.STATUS.getName()).eq("DELIVERED"))
                    .and(DSL.condition(
                            INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + " >= parseDateTime64BestEffort(?)",
                            dtStart(startDate)))
                    .and(DSL.condition(
                            INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + " <= parseDateTime64BestEffort(?)",
                            dtEnd(endDate)))
                    .fetchOne(0, Long.class);
        return r != null ? r : 0L;
    }

    @Override
    public long countFailedFiltered(OffsetDateTime startDate, OffsetDateTime endDate) {
        Long r = dsl.select(DSL.field("count()", Long.class))
                    .from(DSL.table(DSL.sql(table())))
                    .where(DSL.field(INTELLIGENCE_DELIVERIES.STATUS.getName()).eq("FAILED"))
                    .and(DSL.condition(
                            INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + " >= parseDateTime64BestEffort(?)",
                            dtStart(startDate)))
                    .and(DSL.condition(
                            INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + " <= parseDateTime64BestEffort(?)",
                            dtEnd(endDate)))
                    .fetchOne(0, Long.class);
        return r != null ? r : 0L;
    }

    @Override
    public Double avgDeliveryLatencySecondsFiltered(OffsetDateTime startDate, OffsetDateTime endDate) {
        return dsl.select(DSL.field(
                        "avg(toFloat64(dateDiff('millisecond', " +
                        INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + ", " +
                        INTELLIGENCE_DELIVERIES.DELIVERED_AT.getName() + "))) / 1000.0",
                        Double.class))
                  .from(DSL.table(DSL.sql(table())))
                  .where(DSL.field(INTELLIGENCE_DELIVERIES.STATUS.getName()).eq("DELIVERED"))
                  .and(DSL.condition(
                          INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + " >= parseDateTime64BestEffort(?)",
                          dtStart(startDate)))
                  .and(DSL.condition(
                          INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + " <= parseDateTime64BestEffort(?)",
                          dtEnd(endDate)))
                  .fetchOne(0, Double.class);
    }

    @Override
    public List<Object[]> countByStatusFiltered(OffsetDateTime startDate, OffsetDateTime endDate) {
        return dsl.select(
                    DSL.field(INTELLIGENCE_DELIVERIES.STATUS.getName()),
                    DSL.field("count()", Long.class))
                  .from(DSL.table(DSL.sql(table())))
                  .where(DSL.condition(
                          INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + " >= parseDateTime64BestEffort(?)",
                          dtStart(startDate)))
                  .and(DSL.condition(
                          INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + " <= parseDateTime64BestEffort(?)",
                          dtEnd(endDate)))
                  .groupBy(DSL.field(INTELLIGENCE_DELIVERIES.STATUS.getName()))
                  .fetch()
                  .map(r -> new Object[]{r.get(0, String.class), r.get(1, Long.class)});
    }

    @Override
    public List<Object[]> countByActionTypeFiltered(OffsetDateTime startDate, OffsetDateTime endDate) {
        return dsl.select(
                    DSL.field(INTELLIGENCE_DELIVERIES.ACTION_TYPE.getName()),
                    DSL.field("count()", Long.class))
                  .from(DSL.table(DSL.sql(table())))
                  .where(DSL.condition(
                          INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + " >= parseDateTime64BestEffort(?)",
                          dtStart(startDate)))
                  .and(DSL.condition(
                          INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + " <= parseDateTime64BestEffort(?)",
                          dtEnd(endDate)))
                  .groupBy(DSL.field(INTELLIGENCE_DELIVERIES.ACTION_TYPE.getName()))
                  .fetch()
                  .map(r -> new Object[]{r.get(0, String.class), r.get(1, Long.class)});
    }

    @Override
    public List<Object[]> countBySeverityFiltered(OffsetDateTime startDate, OffsetDateTime endDate) {
        return dsl.select(
                    DSL.field(INTELLIGENCE_DELIVERIES.SEVERITY.getName()),
                    DSL.field("count()", Long.class))
                  .from(DSL.table(DSL.sql(table())))
                  .where(DSL.condition(
                          INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + " >= parseDateTime64BestEffort(?)",
                          dtStart(startDate)))
                  .and(DSL.condition(
                          INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + " <= parseDateTime64BestEffort(?)",
                          dtEnd(endDate)))
                  .groupBy(DSL.field(INTELLIGENCE_DELIVERIES.SEVERITY.getName()))
                  .fetch()
                  .map(r -> new Object[]{r.get(0, String.class), r.get(1, Long.class)});
    }

    @Override
    public List<Object[]> countByDestinationFiltered(OffsetDateTime startDate, OffsetDateTime endDate) {
        return dsl.select(
                    DSL.field(INTELLIGENCE_DELIVERIES.DESTINATION.getName()),
                    DSL.field("count()", Long.class).as("cnt"))
                  .from(DSL.table(DSL.sql(table())))
                  .where(DSL.condition(
                          INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + " >= parseDateTime64BestEffort(?)",
                          dtStart(startDate)))
                  .and(DSL.condition(
                          INTELLIGENCE_DELIVERIES.CREATED_AT.getName() + " <= parseDateTime64BestEffort(?)",
                          dtEnd(endDate)))
                  .groupBy(DSL.field(INTELLIGENCE_DELIVERIES.DESTINATION.getName()))
                  .orderBy(DSL.field("cnt").desc())
                  .fetch()
                  .map(r -> new Object[]{r.get(0, String.class), r.get(1, Long.class)});
    }

    /**
     * intelligence_deliveries joined to intelligence_event_logs on the delivery's event, restricted to
     * the given protocol instances. The event log carries the protocol instance, the intelligence
     * action that fired and the trigger reason; deliveries carry neither. FINAL (when enabled) applies
     * to both sides, so a delivery's latest status is read.
     */
    @Override
    public List<IntelligenceAlert> findAlertsByProtocolInstanceIdIn(List<UUID> protocolInstanceIds) {
        if (protocolInstanceIds == null || protocolInstanceIds.isEmpty()) return List.of();
        List<String> ids = protocolInstanceIds.stream().map(UUID::toString).toList();
        String e = INTELLIGENCE_EVENT_LOGS.getName();
        var events = DSL.table(DSL.sql(
                "(SELECT " + INTELLIGENCE_EVENT_LOGS.ID.getName() + " AS event_id, "
                + INTELLIGENCE_EVENT_LOGS.PROTOCOL_INSTANCE_ID.getName() + ", "
                + INTELLIGENCE_EVENT_LOGS.STEP_INSTANCE_ID.getName() + ", "
                + INTELLIGENCE_EVENT_LOGS.STEP_ACTION_ID.getName() + ", "
                + INTELLIGENCE_EVENT_LOGS.TRIGGER_REASON.getName() + ", "
                + INTELLIGENCE_EVENT_LOGS.SLA_STATUS.getName()
                + " FROM " + e + finalClause()
                + " WHERE " + INTELLIGENCE_EVENT_LOGS.PROTOCOL_INSTANCE_ID.getName() + " IN ({0})) ev",
                DSL.list(ids.stream().map(DSL::val).toList())));
        // Explicit columns: http_status_code and error_message are MATERIALIZED, which SELECT * leaves out.
        List<SelectFieldOrAsterisk> columns = new ArrayList<>();
        for (var f : List.of(INTELLIGENCE_DELIVERIES.ID, INTELLIGENCE_DELIVERIES.INTELLIGENCE_EVENT_ID,
                INTELLIGENCE_DELIVERIES.ACTION_ID, INTELLIGENCE_DELIVERIES.DESTINATION,
                INTELLIGENCE_DELIVERIES.ACTION_TYPE, INTELLIGENCE_DELIVERIES.SEVERITY,
                INTELLIGENCE_DELIVERIES.STATUS, INTELLIGENCE_DELIVERIES.ATTEMPT_COUNT,
                INTELLIGENCE_DELIVERIES.HTTP_STATUS_CODE, INTELLIGENCE_DELIVERIES.ERROR_MESSAGE,
                INTELLIGENCE_DELIVERIES.CREATED_AT, INTELLIGENCE_DELIVERIES.DELIVERED_AT)) {
            columns.add(DSL.field(DSL.name("d", f.getName())).as(f.getName()));
        }
        for (var f : List.of(INTELLIGENCE_EVENT_LOGS.PROTOCOL_INSTANCE_ID, INTELLIGENCE_EVENT_LOGS.STEP_INSTANCE_ID,
                INTELLIGENCE_EVENT_LOGS.STEP_ACTION_ID, INTELLIGENCE_EVENT_LOGS.TRIGGER_REASON,
                INTELLIGENCE_EVENT_LOGS.SLA_STATUS)) {
            columns.add(DSL.field(DSL.name("ev", f.getName())).as(f.getName()));
        }
        return dsl.select(columns)
                  .from(DSL.table(DSL.sql(INTELLIGENCE_DELIVERIES.getName() + " d" + finalClause())))
                  .join(events)
                  .on(DSL.condition("ev.event_id = d." + INTELLIGENCE_DELIVERIES.INTELLIGENCE_EVENT_ID.getName()))
                  .orderBy(DSL.field("d." + INTELLIGENCE_DELIVERIES.CREATED_AT.getName()).desc())
                  .fetch()
                  .map(this::toAlert);
    }

    private IntelligenceAlert toAlert(Record r) {
        return IntelligenceAlert.builder()
                .deliveryId(parseUUID(r.get(INTELLIGENCE_DELIVERIES.ID.getName(), String.class)))
                .intelligenceEventId(parseUUID(r.get(INTELLIGENCE_DELIVERIES.INTELLIGENCE_EVENT_ID.getName(), String.class)))
                .protocolInstanceId(parseUUID(r.get(INTELLIGENCE_EVENT_LOGS.PROTOCOL_INSTANCE_ID.getName(), String.class)))
                .stepInstanceId(parseUUID(r.get(INTELLIGENCE_EVENT_LOGS.STEP_INSTANCE_ID.getName(), String.class)))
                .actionId(r.get(INTELLIGENCE_DELIVERIES.ACTION_ID.getName(), String.class))
                .alertActionId(r.get(INTELLIGENCE_EVENT_LOGS.STEP_ACTION_ID.getName(), String.class))
                .triggerReason(r.get(INTELLIGENCE_EVENT_LOGS.TRIGGER_REASON.getName(), String.class))
                .slaStatus(r.get(INTELLIGENCE_EVENT_LOGS.SLA_STATUS.getName(), String.class))
                .destination(r.get(INTELLIGENCE_DELIVERIES.DESTINATION.getName(), String.class))
                .actionType(r.get(INTELLIGENCE_DELIVERIES.ACTION_TYPE.getName(), String.class))
                .severity(r.get(INTELLIGENCE_DELIVERIES.SEVERITY.getName(), String.class))
                .status(r.get(INTELLIGENCE_DELIVERIES.STATUS.getName(), String.class))
                .attemptCount(r.get(INTELLIGENCE_DELIVERIES.ATTEMPT_COUNT.getName(), Integer.class))
                .httpStatusCode(r.get(INTELLIGENCE_DELIVERIES.HTTP_STATUS_CODE.getName(), Integer.class))
                .errorMessage(r.get(INTELLIGENCE_DELIVERIES.ERROR_MESSAGE.getName(), String.class))
                .createdAt(recordDateTime(r, INTELLIGENCE_DELIVERIES.CREATED_AT.getName()))
                .deliveredAt(recordDateTime(r, INTELLIGENCE_DELIVERIES.DELIVERED_AT.getName()))
                .build();
    }
}
