/**
 * Copyright (c) 2022, autumo Ltd. Switzerland, Michael Gasche
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */
package ch.autumo.beetroot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Config manager.
 */
public class ConfigurationManager {

	protected final static Logger LOG = LoggerFactory.getLogger(ConfigurationManager.class.getName());
	
	private static ConfigurationManager manager = null;
	private static String rootPath = null;

	private static boolean isInitialized = false;
	
	private ServletContext servletContext = null;
	
	private Properties generalProps = null;
	private boolean csrf = true;
	
	static {
    	
    	rootPath = System.getProperty("ROOTPATH");
    	
    	if (rootPath == null || rootPath.length() == 0)
    		rootPath = "." + Utils.FILE_SEPARATOR;
    	
    	if (!rootPath.endsWith(Utils.FILE_SEPARATOR))
    		rootPath += Utils.FILE_SEPARATOR;
    }
	
	private ConfigurationManager() {
	}
	
	/**
	 * Get config manager.
	 * 
	 * @return manager
	 */
	public static ConfigurationManager getInstance() {
		
		if (manager == null) {
			manager = new ConfigurationManager();
		}
		return manager;
	}

	/**
	 * Throws a runtime exception if this config manager 
	 * hasn't been initialized yet!
	 */
	public static void isInitialized() {
		if (manager == null)
			throw new RuntimeException("Configuration manager must be first initialized before this code!");
	}
	
	/**
	 * Initialize with absolute path.
	 * 
	 * @param absolutePath absolute path
	 * @param servletContext true, if it runs in a servlet context
	 * @throws Exception
	 */
	public void initializeWithFullPath(String absolutePath, ServletContext servletContext) throws Exception {
		
		this.initializeInt(absolutePath);
		
		this.servletContext = servletContext;
	}
	
	/**
	 * Initialize with path 'ROOTPATH/<given-path-and-file>'.
	 * 
	 * @param relativePath relative path
	 * @throws Exception
	 */
	public void initialize(String relativePath) throws Exception {
		this.initializeInt(rootPath + relativePath);
	}
	
	/**
	 * Initialize with standard config path 'ROOTPATH/cfg/beetroot.cfg'.
	 * 
	 * @throws Exception
	 */
	public void initialize() throws Exception {
		this.initializeInt(rootPath + Constants.CONFIG_PATH + Constants.GENERAL_SRV_CFG_FILE);
	}
	
	/**
	 * Customized initialization with specific config file.
	 * 
	 * @param configFilePath full path to specific config file
	 * @throws Exception
	 */
	public synchronized void initializeInt(String configFilePath) throws Exception {
		
		if (isInitialized) {
			
    		LOG.warn("WARNING: Initialisation of configuration manager is called more than once!");
    		return;
		}
		
		if (servletContext == null) {
		
	    	if (rootPath == null || rootPath.length() == 0) {
	    		
	    		LOG.error("Specified '-DROOTPATH' is non-existant! Check starting script of java process.");
				throw new Exception("Specified '-DROOTPATH' is non-existant! Check starting script of java process.");
	    	}
		    	
			// check root path
	    	if (!rootPath.endsWith(Utils.FILE_SEPARATOR))
	    		rootPath += Utils.FILE_SEPARATOR;
		    
			final File dir = new File(rootPath);
			if (!dir.exists() || !dir.isDirectory()) {
				
				LOG.error("Specified '-DROOTPATH' is invalid! Check starting script of java process.");
				throw new Exception("Specified '-DROOTPATH' is non-existant! Check starting script of java process.");
			}		
		}
		
		// read general config
		generalProps = new Properties();
		// read general config
		final String file = configFilePath;
		try {
			
			generalProps.load(new FileInputStream(file));
			
		} catch (IOException e) {
			
			LOG.error("Couldn't read general server configuration '" + file + "' !", e);
			throw new Exception("Couldn't read general server configuration '" + file + "' !");
		}
		
		isInitialized = true;
	}
	
	/**
	 * Set if CSRF should be used.
	 * 
	 * @param csrf true if CSRF should be used
	 */
	public void setCsrf(boolean csrf) {
		this.csrf = csrf;
	}
	
	/**
	 * Use CSRF?
	 * @return true true if CSRF should be used
	 */
	public boolean useCsrf() {
		return this.csrf;
	}
	
	/**
	 * Get app root path.
	 * 
	 * @return root path
	 */
	
	public String getRootPath() {
		return rootPath;
	}
	
	/**
	 * Get a string value.
	 * 
	 * @param key key
	 * @return value
	 * @throws Exception
	 */
	public String getString(String key) {
		
		String v = generalProps.getProperty(key);
		if (v != null)
			v = v.trim();

		if (v == null)
			LOG.trace("Value for key '"+key+"' doesn't exist in beetroot configuration!");
		
		return v;
	}
	
	/**
	 * Get integer value.
	 * 
	 * @param key key
	 * @return value
	 * @throws Exception
	 */
	public int getInt(String key) {
		
		String v = generalProps.getProperty(key);
		
		if (v == null) {
			LOG.trace("Value for key '"+key+"' doesn't exist in beetroot configuration!");
			return -1;
		}
		
		return Integer.valueOf(v);
	}
	
	/**
	 * Get yes (true) or no (false), if the configuration is messed up false
	 * is returned.
	 * 
	 * @param key key
	 * @return true or false
	 */
	public boolean getYesOrNo(String key) {
		
		String val = generalProps.getProperty(key);
		
		if (val == null || val.length() == 0) {
			
			if (val == null)
				LOG.warn("Value for yes/no key '"+key+"' doesn't exist in beetroot configuration!");
			
			return false;
		}
		
		val = val.trim();
		
		if (val.toLowerCase().equals(Constants.YES))
			return true;
		else
			return false;
	}	
	
	/**
	 * Decode encrypted value, if it is encrypted by ifacex standards!
	 * See 'encoder.sh'.
	 * 
	 * @param key key
	 * @param app ifacex secure application
	 * @return encrypted value
	 * @throws Exception
	 */
	public String getDecodedString(String key, SecureApplication app) throws Exception {
		
		String v = generalProps.getProperty(key);
		if (v != null)
			v = v.trim();
		
		return Utils.decode(v, app);
	}
	
	/**
	 * Get web app roles.
	 * @return web app roles
	 */
	public String[] getAppRoles() {

		return getSepValues("web_roles");
	}

	/**
	 * Get comma-separated values, e.g. 'a,b,c'.
	 * @param key key
	 * @return values
	 */
	public String[] getSepValues(String key) {
		
		String v1 = generalProps.getProperty(key);
		
		if (v1 == null || v1.length() == 0) {
			
			LOG.warn("There are no separated values (or fields configured) for key '" + key + "' !");
			return new String[0];
		}
		
		String v2[] = v1.split(",");
		String res[] = new String[v2.length];
		for (int j = 0; j < v2.length; j++) {
			res[j] = v2[j].trim();
		}
		return res;
	}

	/**
	 * Get allowed mime types.
	 * @param key mime type key
	 * @return allowed mime types
	 */
	public List<String> getMimeTypes(String key) {
		
		final String mimes = generalProps.getProperty(key);
		if (mimes == null || mimes.trim().length() == 0) {
			LOG.warn("There are no mime types for key '" + key + "' ! This will create errors...");
			return Collections.emptyList();
		}
		final String arr[] = mimes.trim().split(" ");
		/**
		for (int i = 0; i < arr.length; i++) {
			System.err.println(key+": "+arr[i]);
		}*/
		return Arrays.asList(arr);
	}
	
	/**
	 * Get servlet context
	 * @return servlet context or null
	 */
	public ServletContext getServletContext() {
		return servletContext;
	}
	
}
