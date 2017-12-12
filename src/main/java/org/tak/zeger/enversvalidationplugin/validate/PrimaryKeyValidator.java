package org.tak.zeger.enversvalidationplugin.validate;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.dbunit.dataset.DataSetException;
import org.tak.zeger.enversvalidationplugin.annotation.ConnectionProvider;
import org.tak.zeger.enversvalidationplugin.annotation.Parameterized;
import org.tak.zeger.enversvalidationplugin.annotation.TargetPhase;
import org.tak.zeger.enversvalidationplugin.annotation.Validate;
import org.tak.zeger.enversvalidationplugin.annotation.ValidationType;
import org.tak.zeger.enversvalidationplugin.annotation.WhiteList;
import org.tak.zeger.enversvalidationplugin.connection.ConnectionProviderInstance;
import org.tak.zeger.enversvalidationplugin.connection.DatabaseQueries;
import org.tak.zeger.enversvalidationplugin.exceptions.ValidationException;

/**
 * The goal of this validator is described in its {@link Validate} methods:
 */
@ValidationType(TargetPhase.CONSTRAINTS)
public class PrimaryKeyValidator
{
	private ConnectionProviderInstance connectionProvider;
	private final String auditTableName;
	private final String auditedTableName;
	private final List<String> primaryIdentifierColumnNamesAuditTable;
	private final List<String> primaryIdentifierColumnNamesAuditedTable;

	public PrimaryKeyValidator(@ConnectionProvider ConnectionProviderInstance connectionProvider, @Nonnull String auditTableName, @Nonnull String auditedTableName, @Nonnull List<String> primaryIdentifierColumnNamesAuditTable, @Nonnull List<String> primaryIdentifierColumnNamesAuditedTable)
	{
		this.connectionProvider = connectionProvider;
		this.auditTableName = auditTableName;
		this.auditedTableName = auditedTableName;
		this.primaryIdentifierColumnNamesAuditTable = primaryIdentifierColumnNamesAuditTable;
		this.primaryIdentifierColumnNamesAuditedTable = primaryIdentifierColumnNamesAuditedTable;
	}

	@Parameterized(name = "{index}: auditTableName: {1}", uniqueIdentifier = "{1}")
	public static List<Object[]> generateTestData(@Nonnull @ConnectionProvider ConnectionProviderInstance connectionProvider, @Nonnull @WhiteList Map<String, String> whiteList) throws SQLException, DataSetException
	{
		final DatabaseQueries databaseQueries = connectionProvider.getQueries();

		final List<Object[]> testData = new ArrayList<>();
		for (Map.Entry<String, String> whiteListEntry : whiteList.entrySet())
		{
			final List<String> primaryIdentifierColumnNamesAuditTable = databaseQueries.getPrimaryKeyColumnNames(whiteListEntry.getKey());
			final List<String> primaryIdentifierColumnNamesAuditedTable = databaseQueries.getPrimaryKeyColumnNames(whiteListEntry.getValue());

			testData.add(new Object[] { connectionProvider, whiteListEntry.getKey(), whiteListEntry.getValue(), primaryIdentifierColumnNamesAuditTable, primaryIdentifierColumnNamesAuditedTable, });
		}

		return testData;
	}

	/**
	 * Validates that the primary of the audit table is valid.
	 * A valid primary key for an audit table consists of:
	 * - The columns matching the primary key columns of the content table.
	 * - The column holding a foreign key to the revision table.
	 * - The column holding the revision type value (Add/Update/Remove)
	 */
	@Validate
	public void validateAuditTableHasAValidPrimaryKey()
	{
		if (primaryIdentifierColumnNamesAuditTable.isEmpty())
		{
			throw new ValidationException("Audit table " + auditTableName + " has no primary key.");
		}

		final Set<String> expectedAuditTablePrimaryKeyColumnNames = new HashSet<>(primaryIdentifierColumnNamesAuditedTable);
		final String revisionTableIdentifierColumnName = connectionProvider.getQueries().getRevisionTableIdentifierColumnName();
		expectedAuditTablePrimaryKeyColumnNames.add(revisionTableIdentifierColumnName);

		expectedAuditTablePrimaryKeyColumnNames.removeAll(primaryIdentifierColumnNamesAuditTable);

		if (!expectedAuditTablePrimaryKeyColumnNames.isEmpty())
		{
			throw new ValidationException(
					//@formatter:off
					"Audit table " + auditTableName + " has a primary key that is not compromised of the primary key columns of the table to audit ["
					+ auditedTableName + "] + [" + revisionTableIdentifierColumnName +
					"] the following columns are missing: " + expectedAuditTablePrimaryKeyColumnNames
					//@formatter:on
			);
		}

		final Set<String> actualPrimaryKeyColumnsAuditTable = new HashSet<>(primaryIdentifierColumnNamesAuditTable);
		actualPrimaryKeyColumnsAuditTable.remove(revisionTableIdentifierColumnName);
		actualPrimaryKeyColumnsAuditTable.removeAll(primaryIdentifierColumnNamesAuditedTable);

		if (!actualPrimaryKeyColumnsAuditTable.isEmpty())
		{
			throw new ValidationException("The primary key of audit table " + auditTableName + " is comprised of more columns than expected, the following columns were not expected: " + actualPrimaryKeyColumnsAuditTable + " this error may also be thrown if the table to audit has no primary key.");
		}
	}
}