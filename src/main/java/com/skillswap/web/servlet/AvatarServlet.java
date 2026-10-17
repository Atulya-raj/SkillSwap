package com.skillswap.web.servlet;

import com.skillswap.dao.UserDao;
import com.skillswap.dao.jdbc.JdbcUserDao;
import com.skillswap.io.FileStorage;
import com.skillswap.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Streams avatar image files from disk with correct MIME types and caching headers.
 */
@WebServlet("/avatar")
public class AvatarServlet extends HttpServlet {

    private static final String DEFAULT_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">
              <circle cx="50" cy="50" r="48" fill="#2D2A5B" />
              <circle cx="50" cy="40" r="18" fill="#F4A300" />
              <path d="M 22 82 A 28 26 0 0 1 78 82 Z" fill="#F4A300" />
            </svg>
            """;

    private final UserDao userDao;
    private final FileStorage fileStorage;

    public AvatarServlet() {
        this(new JdbcUserDao(), FileStorage.getInstance());
    }

    public AvatarServlet(UserDao userDao, FileStorage fileStorage) {
        this.userDao = userDao;
        this.fileStorage = fileStorage;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idParam = request.getParameter("id");
        String fileParam = request.getParameter("file");

        String avatarFilename = null;

        if (idParam != null && !idParam.trim().isEmpty()) {
            try {
                long userId = Long.parseLong(idParam.trim());
                Optional<User> userOpt = userDao.findById(userId);
                if (userOpt.isPresent()) {
                    avatarFilename = userOpt.get().getAvatarPath();
                }
            } catch (NumberFormatException ignored) {
            }
        } else if (fileParam != null && !fileParam.trim().isEmpty()) {
            avatarFilename = fileParam.trim();
        }

        File avatarFile = fileStorage.getAvatarFile(avatarFilename);
        if (avatarFile != null && avatarFile.exists()) {
            String lower = avatarFile.getName().toLowerCase();
            String contentType = "image/jpeg";
            if (lower.endsWith(".png")) {
                contentType = "image/png";
            }

            response.setContentType(contentType);
            response.setContentLengthLong(avatarFile.length());
            response.setHeader("Cache-Control", "public, max-age=86400");

            try (FileInputStream fis = new FileInputStream(avatarFile);
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = fis.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        } else {
            // Serve default SVG avatar
            response.setContentType("image/svg+xml");
            response.setHeader("Cache-Control", "public, max-age=86400");
            byte[] svgBytes = DEFAULT_SVG.getBytes(StandardCharsets.UTF_8);
            response.setContentLength(svgBytes.length);
            try (OutputStream out = response.getOutputStream()) {
                out.write(svgBytes);
            }
        }
    }
}
