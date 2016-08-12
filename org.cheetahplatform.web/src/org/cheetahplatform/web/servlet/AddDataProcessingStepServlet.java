package org.cheetahplatform.web.servlet;

import java.sql.Connection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cheetahplatform.web.dao.DataProcessingStepDao;

public class AddDataProcessingStepServlet extends AbstractCheetahServlet {
	static class AddDataProcessingStepRequest {
		private long dataProcessingId;
		private String name;
		private String type;
		private String configuration;

		public String getConfiguration() {
			return configuration;
		}

		public long getDataProcessingId() {
			return dataProcessingId;
		}

		public String getName() {
			return name;
		}

		public String getType() {
			return type;
		}

		public void setConfiguration(String configuration) {
			this.configuration = configuration;
		}

		public void setDataProcessingId(long dataProcessingId) {
			this.dataProcessingId = dataProcessingId;
		}

		public void setName(String name) {
			this.name = name;
		}

		public void setType(String type) {
			this.type = type;
		}

	}

	private static final long serialVersionUID = 8629880128490562538L;

	@Override
	protected void doPostWithDatabaseConnection(Connection connection, HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		AddDataProcessingStepRequest addRequest = readJson(request, AddDataProcessingStepRequest.class);
		DataProcessingStepDao dao = new DataProcessingStepDao();
		dao.insert(connection, addRequest.getDataProcessingId(), addRequest.getType(), addRequest.getName(), addRequest.getConfiguration());
	}

}
