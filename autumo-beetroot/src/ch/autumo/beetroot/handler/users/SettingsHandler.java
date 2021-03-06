/**
 * Copyright 2022 autumo GmbH, Michael Gasche.
 * All Rights Reserved.
 * 
 * NOTICE: All information contained herein is, and remains
 * the property of autumo GmbH The intellectual and technical
 * concepts contained herein are proprietary to autumo GmbH
 * and are protected by trade secret or copyright law.
 * 
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from autumo GmbH.
 * 
 */
package ch.autumo.beetroot.handler.users;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.Utils;
import ch.autumo.beetroot.handler.BaseHandler;
import ch.autumo.beetroot.handler.HandlerResponse;

/**
 * Settings handler.
 */
public class SettingsHandler extends BaseHandler {

	public SettingsHandler(String entity) {
		this.entity = entity;
	}
	
	@Override
	public HandlerResponse readData(BeetRootHTTPSession session, int id) throws Exception {
		
		final Session userSession = session.getUserSession();
		boolean somethingChanged = false;
	
		String key = null;
		
		// theme
		key = session.getParms().get("theme");
		if (key != null && key.length() != 0) {
			if (!key.equals(userSession.getUserSetting("theme"))) {
				userSession.addOrUpdateUserSetting("theme", key);
				somethingChanged = true;
			}
		}
		
		if (somethingChanged)
			Utils.storeUserSettings(userSession);
		
		return null;
	}
	
	@Override
	protected boolean isNoContentResponse() {
		// This site doesn't generate an output
		return true;
	}

	@Override
	public String getResource() {
		// since no output is created, no resource is necessary
		return null;
	}

}
