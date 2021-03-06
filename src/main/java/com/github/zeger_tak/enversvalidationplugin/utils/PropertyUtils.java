package com.github.zeger_tak.enversvalidationplugin.utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.github.zeger_tak.enversvalidationplugin.entities.AuditTableInformation;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.tak.zeger.enversvalidationplugin.configuration.AuditTableInformationType;
import org.tak.zeger.enversvalidationplugin.configuration.ConfigurationFile;
import org.tak.zeger.enversvalidationplugin.configuration.ObjectFactory;

public final class PropertyUtils
{
	private PropertyUtils()
	{
	}

	@Nonnull
	public static Map<String, AuditTableInformation> getAuditTableInformationMap(@Nonnull String fileName, @Nonnull String auditTablePostFix) throws MojoFailureException
	{
		final File file = new File(fileName);
		try
		{
			final JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
			final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			final ConfigurationFile configurationFile = (ConfigurationFile) unmarshaller.unmarshal(file);
			return createAuditTableInformationMap(configurationFile, auditTablePostFix);
		}
		catch (JAXBException | RuntimeException e)
		{
			throw new MojoFailureException("Unable to retrieve audit table information, errormessage: " + e.getMessage(), e);
		}
	}

	@Nonnull
	private static Map<String, AuditTableInformation> createAuditTableInformationMap(@Nonnull ConfigurationFile auditTableInformationFile, @Nonnull String auditTablePostFix) throws MojoFailureException
	{
		final Map<String, AuditTableInformationType> auditTableInformationTypes = convertToMap(auditTableInformationFile.getAuditTableInformation());
		final Map<String, AuditTableInformation> auditTableInformationMap = new HashMap<>();

		for (AuditTableInformationType auditTableInformationType : auditTableInformationTypes.values())
		{
			final String auditTableName = auditTableInformationType.getAuditTableName();
			final String contentTableName = parseContentTableName(auditTableInformationType, auditTablePostFix);
			auditTableInformationMap.putIfAbsent(auditTableName, new AuditTableInformation(auditTableName, contentTableName, new HashSet<>(auditTableInformationType.getColumnNamePresentInContentTableButNotInAuditTable())));
			final AuditTableInformation auditTableInformation = auditTableInformationMap.get(auditTableName);

			final String auditTableParentName = auditTableInformationType.getAuditTableParentName();
			if (StringUtils.isNotBlank(auditTableParentName))
			{
				final AuditTableInformationType parentAuditTableInformationType = auditTableInformationTypes.get(auditTableParentName);
				if (parentAuditTableInformationType == null)
				{
					throw new MojoFailureException("Unable to construct the audit table information tree as " + auditTableInformationType + " has a parent audit table for which no " + AuditTableInformationType.class.getSimpleName() + " was configured.");
				}

				auditTableInformationMap.putIfAbsent(parentAuditTableInformationType.getAuditTableName(), new AuditTableInformation(parentAuditTableInformationType.getAuditTableName(), parseContentTableName(parentAuditTableInformationType, auditTablePostFix), new HashSet<>(parentAuditTableInformationType.getColumnNamePresentInContentTableButNotInAuditTable())));
				final AuditTableInformation parentAuditTableInformation = auditTableInformationMap.get(auditTableParentName);
				auditTableInformation.setAuditTableParent(parentAuditTableInformation);
			}
		}
		return auditTableInformationMap;
	}

	@Nonnull
	private static String parseContentTableName(@Nonnull AuditTableInformationType auditTableInformationType, @Nonnull String auditTablePostFix)
	{
		return StringUtils.isBlank(auditTableInformationType.getContentTableName()) ? auditTableInformationType.getAuditTableName().replaceAll(auditTablePostFix, "") : auditTableInformationType.getContentTableName();
	}

	@Nonnull
	public static Properties getPropertiesFromFile(@Nonnull File file) throws MojoFailureException
	{
		Properties connectionPropertiesInFile = new Properties();

		try
		{
			connectionPropertiesInFile.load(new FileReader(file));
		}
		catch (IOException e)
		{
			throw new MojoFailureException(e.getMessage(), e);
		}
		return connectionPropertiesInFile;
	}

	@Nonnull
	private static Map<String, AuditTableInformationType> convertToMap(@Nonnull List<AuditTableInformationType> list)
	{
		return list.stream().collect(Collectors.toMap(AuditTableInformationType::getAuditTableName, Function.identity()));
	}
}