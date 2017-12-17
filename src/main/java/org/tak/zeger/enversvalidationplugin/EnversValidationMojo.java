package org.tak.zeger.enversvalidationplugin;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.tak.zeger.enversvalidationplugin.annotation.Parameterized;
import org.tak.zeger.enversvalidationplugin.annotation.Validate;
import org.tak.zeger.enversvalidationplugin.annotation.ValidationType;
import org.tak.zeger.enversvalidationplugin.connection.ConnectionProviderInstance;
import org.tak.zeger.enversvalidationplugin.entities.Config;
import org.tak.zeger.enversvalidationplugin.entities.WhitelistEntry;
import org.tak.zeger.enversvalidationplugin.execution.ValidationExecutor;
import org.tak.zeger.enversvalidationplugin.utils.PropertyUtils;

@Mojo(name = "validate")
public class EnversValidationMojo extends AbstractMojo
{
	private static final String PACKAGE_TO_ALWAYS_SCAN_FOR_EXECUTORS = "org.tak.zeger.enversvalidationplugin.validate";

	/**
	 * Properties file for the connection information.
	 * Required properties:
	 * - username: db-user used to connect with the database under test.
	 * - password: db-user password used to connect with the database under test.
	 * - driver: database driver class.
	 * - url: jdbc connection string. (E.g. 'jdbc:postgresql://localhost/schemaToTest')
	 */
	@Parameter(property = "connectionPropertyFile", required = true, readonly = true)
	private File connectionPropertyFile;

	/**
	 * Used to define packages which hold user defined validators.
	 * WARNING: Untested feature.
	 *
	 * Validators within these packages will be found based on the {@link ValidationType} annotation.
	 * The actual validator methods should be annotated with the {@link Validate} annotation.
	 */
	@Parameter(property = "packageToScanForValidators", readonly = true)
	private List<String> packageToScanForValidators;

	/**
	 * Used to define validator classes/method that should be ignored.
	 * Validators can be ignored based on their unique identifier, the following cases are supported.
	 *
	 * Class level: E.g. RevisionValidator
	 * Method level: E.g. RevisionValidator.validateAllRecordsInAuditedTableHaveAValidLatestRevision
	 * Individual runs: E.g. RevisionValidator.validateAllRecordsInAuditedTableHaveAValidLatestRevision.AUDIT_TABLE_TO_IGNORE.
	 * (In case of a {@link Validate} method with data generated by a {@link Parameterized} method)
	 */
	@Parameter(property = "ignorables", readonly = true)
	private List<String> ignorables;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		final ConnectionProviderInstance connectionProvider = PropertyUtils.getConnectionProperties(connectionPropertyFile);
		final Map<String, WhitelistEntry> whiteList = PropertyUtils.getWhiteList(connectionProvider.getWhiteListPropertyFile(), connectionProvider.getQueries().getAuditTablePostFix());
		final Set<String> listOfAuditTablesInDatabase = getListOfAuditTablesInDatabase(connectionProvider);
		final Config config = new Config(packageToScanForValidators, whiteList, ignorables);
		try
		{
			packageToScanForValidators.add(PACKAGE_TO_ALWAYS_SCAN_FOR_EXECUTORS);
			final ValidationExecutor validationExecutor = new ValidationExecutor(getLog(), config, connectionProvider);
			validationExecutor.executeValidations(getLog(), listOfAuditTablesInDatabase);
		}
		catch (Exception e)
		{
			throw new MojoFailureException(e.getMessage(), e);
		}
	}

	@Nonnull
	private Set<String> getListOfAuditTablesInDatabase(@Nonnull ConnectionProviderInstance connectionProvider)
	{
		try
		{
			return connectionProvider.getQueries().getTablesByNameEndingWith(connectionProvider.getQueries().getAuditTablePostFix());
		}
		catch (Exception e)
		{
			throw new RuntimeException(e.getMessage(), e);
		}
	}
}