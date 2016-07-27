package org.cheetahplatform.web.servlet;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.cheetahplatform.web.dao.StudyDao;
import org.cheetahplatform.web.dao.SubjectDao;
import org.cheetahplatform.web.dto.CreateSubjectRequest;
import org.cheetahplatform.web.dto.CreateSubjectResponse;
import org.cheetahplatform.web.dto.StudyDto;

public class FileUploadServlet extends AbstractCheetahServlet {

	private static class FileUploadRespone {
		private String message;
		private List<CreateSubjectResponse> subjectList;

		public FileUploadRespone(List<CreateSubjectResponse> subjectList) {
			this.message = null;
			this.subjectList = subjectList;
		}

		public FileUploadRespone(String message) {
			this.message = message;
			this.subjectList = null;
		}

		public String getMessage() {
			return message;
		}

		public List<CreateSubjectResponse> getSubjectList() {
			return subjectList;
		}

	}

	private static final long serialVersionUID = -3488611226776498100L;

	@Override
	protected void doPostWithDatabaseConnection(Connection connection, HttpServletRequest req, HttpServletResponse resp) throws Exception {
		ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
		List<FileItem> file = null;
		List<CreateSubjectRequest> listOfNewSubjects = new ArrayList<>();
		List<StudyDto> studies = new StudyDao().getStudies(connection);
		SubjectDao subjectDao = new SubjectDao();
		List<CreateSubjectResponse> subjectResponseList = new ArrayList<>();

		try {
			file = upload.parseRequest(req);
		} catch (FileUploadException e) {
			throw new ServletException(e);
		}
		if (file == null || file.isEmpty()) {
			return;
		}
		FileItem fileItem = file.get(0);
		InputStream input = fileItem.getInputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(input));
		try {
			makeCreateSubjectReequests(listOfNewSubjects, studies, reader);
		} catch (Exception e) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			writeJson(resp, new FileUploadRespone(e.getMessage()));
			return;
		}
		for (CreateSubjectRequest createSubjecRequest : listOfNewSubjects) {
			if (subjectDao.subjectExists(connection, createSubjecRequest.getEmail())) {
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				writeJson(resp, new FileUploadRespone("Duplicate entry\n" + createSubjecRequest.toString() + " already exists."));
				return;
			}
		}
		for (CreateSubjectRequest createSubjecRequest : listOfNewSubjects) {
			CreateSubjectResponse createSubject = subjectDao.createSubject(connection, createSubjecRequest);
			subjectResponseList.add(createSubject);

		}

		resp.setStatus(HttpServletResponse.SC_OK);
		writeJson(resp, new FileUploadRespone(subjectResponseList));

	}

	private void makeCreateSubjectReequests(List<CreateSubjectRequest> listOfNewSubjects, List<StudyDto> studies, BufferedReader reader)
			throws Exception {
		String line = reader.readLine();
		if (!line.trim().equals("email;study;subjectId;comment")) {
			throw new Exception("Heading of File is not correct.\nShould be:email;subjectId;studyId;comment ");
		}
		line = reader.readLine();
		while (line != null) {
			Long studyId = null;

			if (!(line.trim().equals(""))) {
				String[] array = line.split(";");
				if (array.length != 4) {
					throw new Exception("There is an error in line " + line);
				}
				for (StudyDto study : studies) {
					if (study.getName().equals(array[1])) {
						studyId = study.getId();
						break;
					}
				}
				if (studyId == null) {
					throw new Exception("Study \"" + array[1] + "\" not found");
				}
				CreateSubjectRequest createSubjectRequest = new CreateSubjectRequest(false);
				createSubjectRequest.setEmail(array[0]);
				createSubjectRequest.setStudyId(studyId);
				createSubjectRequest.setSubjectId(array[2]);
				createSubjectRequest.setComment(array[3]);
				listOfNewSubjects.add(createSubjectRequest);
			} else {
				throw new Exception("File has empty Lines.");
			}
			line = reader.readLine();
		}
	}

}
