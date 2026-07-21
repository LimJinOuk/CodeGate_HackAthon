package com.example.codegate.config;

import com.example.codegate.reservation.domain.Department;
import com.example.codegate.reservation.domain.District;
import com.example.codegate.reservation.support.HospitalProfileParser;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.Set;

/**
 * Hibernate ddl-auto=update 가 안전하게 처리하지 못하는 기존 DB 스키마 보정 작업.
 *
 * <p>MySQL에서만 실행한다. H2 테스트 DB는 Hibernate create-drop 으로 충분하므로 건드리지 않는다.</p>
 */
@Component
public class DatabaseSchemaMigrationRunner implements ApplicationRunner {

    private static final String OLD_SLOT_UNIQUE_INDEX = "uk_slot_hospital_department_datetime";
    private static final String NEW_SLOT_UNIQUE_INDEX = "uk_slot_hospital_datetime";

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final HospitalProfileParser hospitalProfileParser;

    public DatabaseSchemaMigrationRunner(DataSource dataSource,
                                         JdbcTemplate jdbcTemplate,
                                         HospitalProfileParser hospitalProfileParser) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.hospitalProfileParser = hospitalProfileParser;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!isMySql()) {
            return;
        }

        migrateScheduleSlotUniqueConstraint();
        backfillHospitalDistricts();
        backfillHospitalDepartments();
        migrateReservationStatusColumns();
    }

    private boolean isMySql() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return metaData.getDatabaseProductName().toLowerCase().contains("mysql");
        }
    }

    private void migrateScheduleSlotUniqueConstraint() {
        if (!tableExists("schedule_slots")) {
            return;
        }

        Integer duplicateCount = jdbcTemplate.queryForObject("""
                select count(*) from (
                    select hospital_id, slot_date, start_time
                    from schedule_slots
                    group by hospital_id, slot_date, start_time
                    having count(*) > 1
                ) duplicated_slots
                """, Integer.class);
        if (duplicateCount != null && duplicateCount > 0) {
            throw new IllegalStateException(
                    "schedule_slots contains duplicate hospital/date/time rows. "
                            + "Resolve duplicates before applying uk_slot_hospital_datetime.");
        }

        String oldIndexName = findIndexName("schedule_slots", OLD_SLOT_UNIQUE_INDEX);
        if (oldIndexName != null) {
            jdbcTemplate.execute("alter table schedule_slots drop index " + oldIndexName);
        }
        if (findIndexName("schedule_slots", NEW_SLOT_UNIQUE_INDEX) == null) {
            jdbcTemplate.execute("""
                    alter table schedule_slots
                    add constraint uk_slot_hospital_datetime unique (hospital_id, slot_date, start_time)
                    """);
        }
    }

    private void backfillHospitalDistricts() {
        if (!tableExists("hospitals") || !columnExists("hospitals", "district")) {
            return;
        }

        List<HospitalDistrictRow> rows = jdbcTemplate.query("""
                select id, hospital_location
                from hospitals
                where district is null or district = ''
                """, (resultSet, rowNum) -> new HospitalDistrictRow(
                resultSet.getLong("id"),
                resultSet.getString("hospital_location")
        ));

        for (HospitalDistrictRow row : rows) {
            District district = hospitalProfileParser.parseDistrictText(row.hospitalLocation());
            if (district != null) {
                jdbcTemplate.update("update hospitals set district = ? where id = ?", district.name(), row.hospitalId());
            }
        }
    }

    private void backfillHospitalDepartments() {
        if (!tableExists("hospitals") || !tableExists("hospital_departments")) {
            return;
        }

        List<HospitalDepartmentRow> rows = jdbcTemplate.query("""
                select id, medical_subjects
                from hospitals
                """, (resultSet, rowNum) -> new HospitalDepartmentRow(
                resultSet.getLong("id"),
                resultSet.getString("medical_subjects")
        ));

        for (HospitalDepartmentRow row : rows) {
            Set<Department> departments = hospitalProfileParser.parseDepartmentsText(row.medicalSubjects());
            for (Department department : departments) {
                Integer exists = jdbcTemplate.queryForObject("""
                        select count(*)
                        from hospital_departments
                        where hospital_id = ? and department = ?
                        """, Integer.class, row.hospitalId(), department.name());
                if (exists == null || exists == 0) {
                    jdbcTemplate.update("""
                            insert into hospital_departments (hospital_id, department)
                            values (?, ?)
                            """, row.hospitalId(), department.name());
                }
            }
        }
    }

    private void migrateReservationStatusColumns() {
        if (!tableExists("reservations")) {
            return;
        }

        boolean hasDecidedAt = columnExists("reservations", "decided_at");
        boolean hasDecisionMessage = columnExists("reservations", "decision_message");

        if (columnExists("reservations", "approved_at") && hasDecidedAt) {
            jdbcTemplate.update("""
                    update reservations
                    set approved_at = coalesce(approved_at, decided_at)
                    where status = 'APPROVED' and decided_at is not null
                    """);
        }

        if (columnExists("reservations", "rejected_at") && hasDecidedAt) {
            jdbcTemplate.update("""
                    update reservations
                    set rejected_at = coalesce(rejected_at, decided_at)
                    where status = 'REJECTED' and decided_at is not null
                    """);
        }

        if (columnExists("reservations", "canceled_at") && hasDecidedAt) {
            jdbcTemplate.update("""
                    update reservations
                    set canceled_at = coalesce(canceled_at, decided_at)
                    where status = 'CANCELED' and decided_at is not null
                    """);
        }

        if (columnExists("reservations", "hospital_memo") && hasDecisionMessage) {
            jdbcTemplate.update("""
                    update reservations
                    set hospital_memo = coalesce(hospital_memo, decision_message)
                    where status in ('APPROVED', 'REJECTED') and decision_message is not null
                    """);
        }

        if (columnExists("reservations", "cancel_reason")) {
            String messageExpression = hasDecisionMessage
                    ? "coalesce(cancel_reason, decision_message, '사용자가 예약을 취소했습니다.')"
                    : "coalesce(cancel_reason, '사용자가 예약을 취소했습니다.')";
            jdbcTemplate.update("""
                    update reservations
                    set cancel_reason = %s
                    where status = 'CANCELED'
                    """.formatted(messageExpression));
        }

        jdbcTemplate.update("""
                update reservations
                set status = 'PATIENT_CANCELED'
                where status = 'CANCELED'
                """);

        if (columnExists("reservations", "status_changed_at")) {
            String statusChangedExpression = hasDecidedAt
                    ? "coalesce(status_changed_at, decided_at, requested_at)"
                    : "coalesce(status_changed_at, requested_at)";
            jdbcTemplate.update("""
                    update reservations
                    set status_changed_at = %s
                    where status_changed_at is null
                    """.formatted(statusChangedExpression));
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.tables
                where table_schema = database()
                  and lower(table_name) = lower(?)
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                select count(*)
                from information_schema.columns
                where table_schema = database()
                  and lower(table_name) = lower(?)
                  and lower(column_name) = lower(?)
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private String findIndexName(String tableName, String indexName) {
        List<String> names = jdbcTemplate.queryForList("""
                select distinct index_name
                from information_schema.statistics
                where table_schema = database()
                  and lower(table_name) = lower(?)
                  and lower(index_name) = lower(?)
                """, String.class, tableName, indexName);
        return names.isEmpty() ? null : names.get(0);
    }

    private record HospitalDistrictRow(Long hospitalId, String hospitalLocation) {
    }

    private record HospitalDepartmentRow(Long hospitalId, String medicalSubjects) {
    }
}
