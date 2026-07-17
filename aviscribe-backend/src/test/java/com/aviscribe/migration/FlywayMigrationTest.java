package com.aviscribe.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class FlywayMigrationTest {

    private static final String URL =
            "jdbc:h2:mem:flyway-migration;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

    @Test
    void initialMigrationCreatesExpectedSchema() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(URL, "sa", "")
                .locations("classpath:db/migration")
                .load();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);

        try (Connection connection = DriverManager.getConnection(URL, "sa", "");
                Statement statement = connection.createStatement()) {
            assertThat(tableExists(statement, "t_user")).isTrue();
            assertThat(tableExists(statement, "t_task")).isTrue();
            assertThat(successfulMigrationCount(statement)).isEqualTo(1);
        }
    }

    private boolean tableExists(Statement statement, String tableName) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = '" + tableName + "'")) {
            resultSet.next();
            return resultSet.getInt(1) == 1;
        }
    }

    private int successfulMigrationCount(Statement statement) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(
                "SELECT COUNT(*) FROM flyway_schema_history "
                        + "WHERE version = '1' AND type = 'SQL' AND success = TRUE")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }
}
