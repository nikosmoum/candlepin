/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.gutterball.liquibase;

import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;



// TODO:
// Migrate common stuff to the CP commons

/**
 * The ComplianceStatusProductIdMigrationTask upgrade task adds IDs to the existing rows in the
 * gb_*compprod_snap tables.
 */
public class ComplianceStatusProductIdMigrationTask {

    private Database database;
    private JdbcConnection connection;
    private CustomTaskLogger logger;

    private Map<String, PreparedStatement> preparedStatements;


    public ComplianceStatusProductIdMigrationTask(Database database) {
        this(database, new SystemOutLogger());
    }

    public ComplianceStatusProductIdMigrationTask(Database database, CustomTaskLogger logger) {
        if (database == null) {
            throw new IllegalArgumentException("database is null");
        }

        if (logger == null) {
            throw new IllegalArgumentException("logger is null");
        }

        if (!(database.getConnection() instanceof JdbcConnection)) {
            throw new RuntimeException("database connection is not a JDBC connection");
        }

        this.database = database;
        this.connection = (JdbcConnection) database.getConnection();
        this.logger = logger;

        this.preparedStatements = new HashMap<String, PreparedStatement>();
    }

    /**
     * Prepares the specified SQL statement and returns a PreparedStatement instance initialized and
     * configured with the given arguments.
     *
     * @param sql
     *  The SQL statement to prepare
     *
     * @param argv
     *  A collection of parameters into the prepared statement
     *
     * @return
     *  A PreparedStatement instance for the given SQL
     */
    protected PreparedStatement prepareStatement(String sql, Object... argv)
        throws DatabaseException, SQLException {

        PreparedStatement statement = this.preparedStatements.get(sql);
        if (statement == null) {
            statement = this.connection.prepareStatement(sql);
            this.preparedStatements.put(sql, statement);
        }

        statement.clearParameters();

        for (int i = 0; i < argv.length; ++i) {
            if (argv[i] != null) {
                statement.setObject(i + 1, argv[i]);
            }
            else {
                // Impl note:
                // Oracle has trouble with setNull. See the comments on this SO question for details:
                // http://stackoverflow.com/questions/11793483/setobject-method-of-preparedstatement
                statement.setNull(i + 1, Types.VARCHAR);
            }
        }

        return statement;
    }

    /**
     * Executes the given SQL query.
     *
     * @param sql
     *  The SQL to execute. The given SQL may be parameterized.
     *
     * @param argv
     *  The arguments to use when executing the given query.
     *
     * @return
     *  A ResultSet instance representing the result of the query.
     */
    protected ResultSet executeQuery(String sql, Object... argv) throws DatabaseException, SQLException {
        PreparedStatement statement = this.prepareStatement(sql, argv);
        return statement.executeQuery();
    }

    /**
     * Executes the given SQL update/insert.
     *
     * @param sql
     *  The SQL to execute. The given SQL may be parameterized.
     *
     * @param argv
     *  The arguments to use when executing the given update.
     *
     * @return
     *  The number of rows affected by the update.
     */
    protected int executeUpdate(String sql, Object... argv) throws DatabaseException, SQLException {
        PreparedStatement statement = this.prepareStatement(sql, argv);
        return statement.executeUpdate();
    }

    /**
     * Generates a 32-character UUID to use with object creation/migration.
     * <p/>
     * The UUID is generated by creating a "standard" UUID and removing the hyphens. The UUID may be
     * standardized by reinserting the hyphens later, if necessary.
     *
     * @return
     *  a 32-character UUID
     */
    protected String generateUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private int addIdsToTable(String table) throws DatabaseException, SQLException {
        int updated = 0;

        // TODO:
        // Perhaps it'd be better to create a function here? Not sure if the cross-platform
        // requirement would cause problems.

        ResultSet rows = this.executeQuery(
            String.format("SELECT comp_status_id, product_id FROM %s", table)
        );

        while (rows.next()) {
            this.executeUpdate(
                String.format("UPDATE %s SET id=? WHERE comp_status_id=? AND product_id=?", table),
                this.generateUUID(), rows.getString(1), rows.getString(2)
            );

            ++updated;
        }

        rows.close();

        return updated;
    }


    /**
     * Executes this maintenance task.
     *
     * @throws DatabaseException
     *  if an error occurs while performing a database operation
     *
     * @throws SQLException
     *  if an error occurs while executing an SQL statement
     */
    public void execute() throws DatabaseException, SQLException {

        // Store the connection's auto commit setting, so we may temporarily clobber it.
        boolean autocommit = this.connection.getAutoCommit();
        this.connection.setAutoCommit(false);

        this.addIdsToTable("gb_compprod_snap");
        this.addIdsToTable("gb_partcompprod_snap");
        this.addIdsToTable("gb_noncompprod_snap");

        // Commit & restore original autocommit state
        this.connection.commit();
        this.connection.setAutoCommit(autocommit);
    }
}
