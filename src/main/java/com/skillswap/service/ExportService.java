package com.skillswap.service;

import com.skillswap.dao.SessionRequestDao;
import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcSessionRequestDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.io.CsvExporter;
import com.skillswap.model.SessionRequest;
import com.skillswap.model.User;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Service orchestrating CSV exports of user session histories and administrative user rosters.
 */
public class ExportService {

    private final SessionRequestDao sessionRequestDao;
    private final UserDao userDao;

    /**
     * Default constructor with JDBC DAOs.
     */
    public ExportService() {
        this(new JdbcSessionRequestDao(), new JdbcUserDao());
    }

    /**
     * Constructs ExportService with injected DAOs.
     *
     * @param sessionRequestDao session request DAO
     * @param userDao user DAO
     */
    public ExportService(SessionRequestDao sessionRequestDao, UserDao userDao) {
        this.sessionRequestDao = sessionRequestDao;
        this.userDao = userDao;
    }

    /**
     * Exports a user's complete session history to CSV with UTF-8 BOM.
     *
     * @param userId user ID
     * @param out output stream target
     * @throws IOException on I/O error
     */
    public void exportSessionHistory(long userId, OutputStream out) throws IOException {
        List<SessionRequest> requests = sessionRequestDao.findAllForUser(userId);
        CsvExporter<SessionRequest> exporter = CsvExporter.forSessionHistory(userId);
        exporter.export(requests, out);
    }

    /**
     * Exports all users in the system to CSV for administrative records.
     *
     * @param out output stream target
     * @throws IOException on I/O error
     */
    public void exportUsers(OutputStream out) throws IOException {
        List<User> users = userDao.findAll(10000, 0);
        CsvExporter<User> exporter = CsvExporter.forUsers();
        exporter.export(users, out);
    }
}
