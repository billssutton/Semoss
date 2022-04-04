package prerna.io.connector.secrets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import prerna.io.connector.secrets.hashicorp.vault.HashiCorpVaultUtil;
import prerna.util.Constants;
import prerna.util.DIHelper;
import prerna.util.Utility;

public class SecretsFactory {

	private static final Logger logger = LogManager.getLogger(SecretsFactory.class);
	
	private SecretsFactory() {
		
	}
	
	public static ISecrets getSecretConnector() {
		if(!Utility.isSecretsStoreEnabled()) {
			return null;
		}
		
		String storeType = DIHelper.getInstance().getProperty(Constants.SECRET_STORE_TYPE);
		if(storeType.equalsIgnoreCase(ISecrets.HASHICORP_VAULT)) {
			return HashiCorpVaultUtil.getInstance();
		} else {
			logger.warn("Secret store is enabled but could not find type for input = '" + storeType + "'");
			return null;
		}
	}
	
}
