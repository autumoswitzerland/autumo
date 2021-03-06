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
package ch.autumo.beetroot.handler;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Map;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.ConfigurationManager;
import ch.autumo.beetroot.DatabaseManager;
import ch.autumo.beetroot.Entity;
import ch.autumo.beetroot.SecureApplicationHolder;
import ch.autumo.beetroot.Utils;

/**
 * Default handler for 'web/html/<entity>/edit.html' templates.
 */
public class DefaultEditHandler extends BaseHandler {

	public DefaultEditHandler(String entity) {
		super(entity);
	}

	public DefaultEditHandler(String entity, String errMsg) {
		super(entity);
		this.addErrorMessage(errMsg);
	}
	
	@Override
	public HandlerResponse readData(BeetRootHTTPSession session, int id) throws Exception {
		
		// RETRY case!
		final Map<String, String> params = session.getParms();
		final String _method = params.get("_method");
		
		Connection conn = null;
		Statement stmt = null;
		ResultSet set = null; 
		
		try {
		
			if (_method != null && _method.equals("RETRY")) {
	
				conn = DatabaseManager.getInstance().getConnection();
				stmt = conn.createStatement();
				
				// we only need the result set for the column meta data
				stmt.setFetchSize(1);
				
				String stmtStr = "SELECT " + super.getColumnsForSql() + " FROM " + this.entity;
				set = stmt.executeQuery(stmtStr);
	
				for (int i = 1; i <= columns().size(); i++) {
					
					final String col[] = getColumn(i);
					htmlData += this.extractSingleInputDiv(session, params, set, col[0], col[1], i);
				}
				stmt.close();
				conn.close();
				return null;
			}
			
			// NORMAL case: first call case
			conn = DatabaseManager.getInstance().getConnection();
			stmt = conn.createStatement();
			
			String stmtStr = "SELECT id, " + super.getColumnsForSql() + " FROM " + this.entity + " WHERE id=" + id;
			set = stmt.executeQuery(stmtStr);
	
			set.next(); // one record !
			
			final Entity entity = Utils.createBean(getBeanClass(), set);
			this.prepare(session, entity);
			
			for (int i = 1; i <= columns().size(); i++) {
				
				final String col[] = getColumn(i);
				int dbIdx = i + 1; // because of additional id!
				htmlData += extractSingleInputDiv(session, set, entity, col[0], col[1], dbIdx);		
			}
		
		} finally {
			if (set != null)
				set.close();
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();
		}
		
		return null;
	}

	@Override
	public HandlerResponse updateData(BeetRootHTTPSession session, int id) throws Exception {
		
		// Unique fields test!
		final HandlerResponse status = super.uniqueTest(session, "SELECT id FROM "+getEntity()+" WHERE id!="+id+" AND ", "updating");
		if (status != null) {
			status.setId(id);
			return status;
		}

		Connection conn = null;
		Statement stmt = null;
		
		try {
		
			conn = DatabaseManager.getInstance().getConnection();
			
			// Now save edited data !
			stmt = conn.createStatement();
			
			String stmtStr = "UPDATE "+getEntity()+" SET "+this.getUpdateSetClause(session)+" WHERE id=" + id;
			stmt.executeUpdate(stmtStr);
		
		} finally {
			if (stmt != null)
				stmt.close();
			if (conn != null)
				conn.close();
		}
		
		return null;
	}
	
	/**
	 * Prepare call to to something with the entity bean if necessary.
	 * 
	 * @param entity entity bean
	 */
	public void prepare(BeetRootHTTPSession session, Entity entity) {
	}
	
	/**
	 * Extract one single input div with label and input tags from result set standing at current row.
	 * NOTE: Never call "set.next()" !
	 * 
	 * @param session HTTP session
	 * @param set result set
	 * @param entity entity bean
	 * @param columnName column name as configured in 'web/<entity>/columns.cfg'
	 * @param guiColName GUI column name as configured in 'web/<entity>/columns.cfg'
	 * @param idx SQL result set column index
	 * @return html data extract <div>...</div>
	 * @throws Exception
	 */
	private String extractSingleInputDiv(BeetRootHTTPSession session, ResultSet set, Entity entity, String columnName, String guiColName, int idx) throws Exception {

		final String val = this.formatSingleValueForGUI(session, set.getObject(idx).toString().trim(), columnName, idx, entity);
		return this.extractSingleInputDiv(session, val, set, columnName, guiColName, idx, true);
	}
	
	/**
	 * Extract one single input div with label and input tags from result set standing at current row.
	 * NOTE: Never call "set.next()" !
	 * 
	 * @param session HTTP session
	 * @param data repost data
	 * @param set result set, even when empty, data is taken from the map (retry)
	 * @param columnName column name as configured in 'web/<entity>/columns.cfg'
	 * @param guiColName GUI column name as configured in 'web/<entity>/columns.cfg'
	 * @param idx SQL result set column index
	 * @return html data extract <div>...</div>
	 * @throws Exception
	 */	
	private String extractSingleInputDiv(BeetRootHTTPSession session, Map<String, String> data, ResultSet set, String columnName, String guiColName, int idx) throws Exception {
		
		return this.extractSingleInputDiv(session, data.get(columnName), set, columnName, guiColName, idx, false);
	}
	
	private String extractSingleInputDiv(BeetRootHTTPSession session, String val, ResultSet set, String columnName, String guiColName, int idx, boolean pwFromDb) throws Exception {
		
		String result = "";
		boolean isCheck = false;
		final ResultSetMetaData rsmd = set.getMetaData();
		final String inputType = Utils.getHtmlInputType(rsmd, idx, columnName);
		final String divType = Utils.getHtmlDivType(rsmd, idx, columnName);
		if (inputType == "checkbox")
			isCheck = true;
		
		int nullable = rsmd.isNullable(idx);
		int precision = rsmd.getPrecision(idx);
		
		if (nullable == ResultSetMetaData.columnNoNulls)
			result += "<div class=\"input "+divType+" required\">\n";
		else
			result += "<div class=\"input "+divType+"\">\n";
		
		// we have to decode the password for edit, even it is obfuscted by stars
		// -> if the user presses save it would be double-encoded otherwise!
		if (pwFromDb && inputType.equals("password")) {
			val = Utils.decode(val, SecureApplicationHolder.getInstance().getSecApp());
		}
		
		result += "<label for=\""+columnName+"\">"+guiColName+"</label>\n"; 
		
		if (isCheck) {
			
			if (val.equals("true")) {
				result += "<input type=\"checkbox\" name=\"cb_"+columnName+"\" id=\"cb_"+columnName+"\" value=\"true\" checked>\n";
				result += "<input type=\"hidden\" name=\""+columnName+"\" id=\""+columnName+"\" value=\"true\" />";
			}
			else {
				result += "<input type=\"checkbox\" name=\"cb_"+columnName+"\" id=\"cb_"+columnName+"\" value=\"false\">\n";
				result += "<input type=\"hidden\" name=\""+columnName+"\" id=\""+columnName+"\" value=\"false\" />";
			}
			
			// Must !
			super.addCheckBox(session, columnName);
		} 
		
		if (!isCheck) {
		
			if (getEntity().equals("users") && columnName.toLowerCase().equals("role")) {
				
				final String roles[] = ConfigurationManager.getInstance().getAppRoles();
				result += "<select name=\""+columnName+"\" id=\""+columnName+"\">\n";
				for (int i = 0; i < roles.length; i++) {
					if (val.equals(roles[i]))
						result += "    <option value=\""+roles[i]+"\" selected>"+roles[i]+"</option>\n";
					else
						result += "    <option value=\""+roles[i]+"\">"+roles[i]+"</option>\n";
				}
				result += "</select>";
				
			} else if (this.isSelect(columnName)) {
				
				final String entries[] = this.getSelectValues(columnName);
				result += "<select name=\""+columnName+"\" id=\""+columnName+"\">\n";
				for (int i = 0; i < entries.length; i++) {
					if (val.equals(entries[i]))
						result += "    <option value=\""+entries[i]+"\" selected>"+entries[i]+"</option>\n";
					else
						result += "    <option value=\""+entries[i]+"\">"+entries[i]+"</option>\n";
				}
				result += "</select>";				
				
			} else {
			
				if (nullable == ResultSetMetaData.columnNoNulls) {
					
					result += "<input type=\""+inputType+"\" name=\""+columnName+"\" required=\"required\"\n";
					result += "    data-validity-message=\"This field cannot be left empty\" oninvalid=\"this.setCustomValidity(&#039;&#039;); if (!this.value) this.setCustomValidity(this.dataset.validityMessage)\"\n";
					result += "    oninput=\"this.setCustomValidity(&#039;&#039;)\"\n"; 
					result += "    id=\""+columnName+"\" aria-required=\"true\" value=\""+val+"\" maxlength=\""+precision+"\">\n";
					
				} else {
					
					result += "<input type=\""+inputType+"\" name=\""+columnName+"\"\n";
					result += "    id=\""+columnName+"\" value=\""+val+"\" maxlength=\""+precision+"\">\n";
				}
			}
		}
		
		
		result += "</div>\n";
		return result;
	}

	/**
	 * Is this column a HTML select field?
	 * 
	 * @param columnName column name
	 * @returntruew if so
	 */
	protected boolean isSelect(String columnName) {
		return false;
	}
	
	/**
	 * Get values for column name
	 * @param columnName column name
	 * @return select vaues
	 */
	protected String[] getSelectValues(String columnName) {
		return null;
	}

	/**
	 * Format value for GUI.
	 * 
	 * @param session HTTP session
	 * @param value value from DB 
	 * @param columnName DB column name
	 * @param dbIdx SQL result set column index
	 * @param entity whole entity bean
	 * @return formated value for given column-name or DB index 
	 */
	public String formatSingleValueForGUI(BeetRootHTTPSession session, String value, String columnName, int dbIdx, Entity entity) {
		return value;
	}
	
	@Override
	public String formatSingleValueForDB(BeetRootHTTPSession session, String val, String columnname) {
		return val;
	}
	
	@Override
	public String getResource() {
		return "web/html/:lang/"+entity+"/edit.html";
	}

	/**
	 * Get bean entity class that has been generated trough PLANT, 
	 * self-written or null (then null in extract calls too).
	 * 
	 * @return bean entity class
	 */
	public Class<?> getBeanClass() {
		return null;
	}
	
}
