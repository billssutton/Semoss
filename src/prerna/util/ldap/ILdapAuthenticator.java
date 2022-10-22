package prerna.util.ldap;

import java.io.IOException;

import javax.naming.directory.DirContext;

import prerna.auth.AccessToken;

public interface ILdapAuthenticator {

	String LDAP_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
	
	String LDAP = "ldap";
	String LDAP_PREFIX = LDAP + "_";
	
	// what type of ldap process are we using
	String LDAP_TYPE = LDAP_PREFIX + "type";
	String LDAP_TYPE_SINGLE = "single";
	String LDAP_TYPE_MULTI = "multi";
	
	// the provider url
	String LDAP_PROVIDER_URL = LDAP_PREFIX + "provider_url";

	// specific to multi structure searching
	String LDAP_APPLICATION_SECURITY_PRINCIPAL = LDAP_PREFIX + "master_principal";
	String LDAP_APPLICATION_SECURITY_CREDENTIALS = LDAP_PREFIX + "master_credentials";
	
	// specific to single structure
	String LDAP_SECURITY_PRINCIPAL_TEMPLATE = LDAP_PREFIX + "principal_tempalte";
	
	// attribute names
	String LDAP_ID_KEY = LDAP_PREFIX + "key_id";
	String LDAP_NAME_KEY = LDAP_PREFIX + "key_name";
	String LDAP_EMAIL_KEY = LDAP_PREFIX + "key_email";
	String LDAP_USERNAME_KEY = LDAP_PREFIX + "key_username";

	/**
	 * Reload properties from social.properties
	 * @throws IOException
	 */
	void load() throws IOException;
	
	/**
	 * Validate the necessary ldap input is provided
	 * @throws IOException
	 */
	void validate() throws IOException;
	
	/**
	 * Authenticate the user input
	 * @param username
	 * @param password
	 * @return
	 */
	boolean authenticate(String username, String password) throws Exception;

	/**
	 * Close any existing connection
	 * @return
	 */
	boolean close();
	
	/**
	 * Grab the user attributes and construct the AccessToken
	 * @param con
	 * @return
	 */
	AccessToken generateAccessToken(DirContext con) throws Exception;
	
}
