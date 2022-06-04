package prerna.project.impl;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.mail.Session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import prerna.util.AssetUtility;
import prerna.util.Constants;
import prerna.util.SocialPropertiesProcessor;

public class ProjectProperties {
	
	private static final Logger logger = LogManager.getLogger(ProjectProperties.class);
	private static final String ADMIN_DIRECTORY = ".admin";
	
	private String projectDirString = null;
	private File adminDir = null;
	private File socialProp = null;
	private SocialPropertiesProcessor processor = null;

	public ProjectProperties(String projectName, String projectId) {
		this.projectDirString = AssetUtility.getProjectAssetFolder(projectName, projectId);
		this.adminDir = new File(projectDirString + "/" + ADMIN_DIRECTORY);
		if(!this.adminDir.exists() || !this.adminDir.isDirectory()) {
			this.adminDir.mkdirs();
		}
		
		String socialPropertiesFileLoc = this.adminDir.getAbsolutePath() + "/" + Constants.SOCIAL_PROPERTIES_FILENAME;
		this.socialProp = new File(socialPropertiesFileLoc);
		if(!this.socialProp.exists() || !this.socialProp.isFile()) {
			try {
				this.socialProp.createNewFile();
			} catch (IOException e) {
				logger.error(Constants.STACKTRACE, e);
			}
		}
		this.processor = new SocialPropertiesProcessor(socialPropertiesFileLoc);
	}
	
	public void updateProviderProperties(String provider, Map<String, String> mods) {
		this.processor.updateProviderProperties(provider, mods);
	}
	
	public void updateAllProperties(Map<String, String> mods) {
		this.processor.updateAllProperties(mods);
	}
	
	public Map<String, Boolean> getLoginsAllowed() {
		return this.processor.getLoginsAllowed();
	}
	
	public String getProperty(String key) {
		return this.processor.getProperty(key);
	}
	
	public Object get(Object key) {
		return this.processor.get(key);
	}
	
	public boolean containsKey(String key) {
		return this.processor.containsKey(key);
	}
	
	public Set<String> stringPropertyNames() {
		return this.processor.stringPropertyNames();
	}
	
	public Map<String, String[]> getSamlAttributeNames() {
		return this.processor.getSamlAttributeNames();
	}
	
	public boolean emailEnabled() {
		return this.processor.emailEnabled();
	}
	
	public String getSmtpSender() {
		return this.processor.getSmtpSender();
	}
	
	public Session getEmailSession() {
		return this.processor.getEmailSession();
	}
	
	public Map<String, String> getEmailStaticProps() {
		return this.processor.getEmailStaticProps();
	}
	
	public void reloadProps() {
		this.processor.reloadProps();
	}
	
}
