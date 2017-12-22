package org.tak.zeger.enversvalidationplugin.connection;

import javax.annotation.Nonnull;

import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.ext.oracle.OracleDataTypeFactory;
import org.dbunit.ext.postgresql.PostgresqlDataTypeFactory;
import org.tak.zeger.enversvalidationplugin.exceptions.DatabaseNotSupportedException;

public class ConnectionProviderInstance
{
	static final String ORACLE_DRIVER = "oracle.jdbc.OracleDriver";
	static final String POSTGRESQL_DRIVER = "org.postgresql.Driver";

	private final String driverClass;
	private final String connectionUrl;
	private final String username;
	private final String password;
	private final String auditTableInformationFile;
	private final IDatabaseTester databaseTester;

	private IDatabaseConnection databaseConnection;
	private DatabaseQueries databaseQueries;

	public ConnectionProviderInstance(@Nonnull String connectionUrl, @Nonnull String driverClass, @Nonnull String username, @Nonnull String password, @Nonnull String auditTableInformationFile)
	{
		this.driverClass = driverClass;
		this.connectionUrl = connectionUrl;
		this.password = password;
		this.username = username;
		this.auditTableInformationFile = auditTableInformationFile;
		databaseTester = newDatabaseTester();
	}

	@Nonnull
	private IDatabaseTester newDatabaseTester()
	{
		try
		{
			if (driverClass.equals(ORACLE_DRIVER))
			{
				// For Oracle, provide the schema name. Otherwise, metadata for ALL objects is retrieved.
				final JdbcDatabaseTester jdbcDatabaseTester = new JdbcDatabaseTester(driverClass, connectionUrl, username, password, username);
				databaseConnection = jdbcDatabaseTester.getConnection();
				getDatabaseConnection().getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new OracleDataTypeFactory());

				databaseQueries = new OracleQueries(this);
				return jdbcDatabaseTester;
			}
			else if (driverClass.equals(POSTGRESQL_DRIVER))
			{
				// For Postgresql
				final JdbcDatabaseTester jdbcDatabaseTester = new JdbcDatabaseTester(driverClass, connectionUrl, username, password);
				databaseConnection = jdbcDatabaseTester.getConnection();
				getDatabaseConnection().getConfig().setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new PostgresqlDataTypeFactory());

				databaseQueries = new PostgresQueries(this);
				return jdbcDatabaseTester;
			}
		}
		catch (Exception e)
		{
			throw new DatabaseNotSupportedException("An error occurred while trying to initialise the database connection provider, is it correctly configured? " + e.getMessage(), e);
		}

		throw new DatabaseNotSupportedException("Unable to determine database type.");
	}

	@Nonnull
	public IDatabaseConnection getDatabaseConnection()
	{
		if (databaseConnection == null)
		{
			try
			{
				databaseConnection = databaseTester.getConnection();
			}
			catch (Exception e)
			{
				throw new DatabaseNotSupportedException("Database connection could not be established.", e);
			}
		}
		return databaseConnection;
	}

	@Nonnull
	public DatabaseQueries getQueries()
	{
		if (databaseQueries == null)
		{
			newDatabaseTester();
		}

		return databaseQueries;
	}

	@Nonnull
	public String getAuditTableInformationFile()
	{
		return auditTableInformationFile;
	}

	@Override
	public String toString()
	{
		return "ConnectionProperties{" + "driverClass='" + driverClass + '\'' + ", connectionUrl='" + connectionUrl + '\'' + ", username='" + username + '\'' + '}';
	}
}