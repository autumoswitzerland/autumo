/**
 * Generated by PLANT - beetRoot CRUD Generator.
 */
package planted.beetroot.handler.properties;

import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.handler.DefaultEditHandler;

/**
 * Properties edit handler. 
 */
public class PropertiesEditHandler extends DefaultEditHandler {
	
	public PropertiesEditHandler(String entity) {
		super(entity);
	}

	public PropertiesEditHandler(String entity, String errMsg) {
		super(entity, errMsg);
	}
	
	@Override
	public Class<?> getRedirectHandler() {
		return PropertiesIndexHandler.class;
	}

	@Override
	public boolean hasAccess(Session userSession) {
		return userSession.getUserRole().equalsIgnoreCase("administrator");
	}
	
}
