package com.skillswap.dao;

import com.skillswap.model.Report;
import com.skillswap.model.enums.ReportStatus;

import java.util.List;

/**
 * Data access interface for moderation reports.
 */
public interface ReportDao {

    long insert(Report r);

    List<Report> findByStatus(ReportStatus s, int limit, int offset);

    void resolve(long id, long adminId, ReportStatus s);

    int countByStatus(ReportStatus s);
}
