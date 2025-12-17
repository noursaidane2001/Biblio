package com.biblio.services;

import com.biblio.dao.UserLogDAO;
import com.biblio.entities.User;
import com.biblio.entities.UserLog;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserLogService {
    private final UserLogDAO userLogDAO;

    public UserLogService(UserLogDAO userLogDAO) {
        this.userLogDAO = userLogDAO;
    }

    @Transactional
    public void log(User utilisateur, String action, String message, String level) {
        UserLog log = UserLog.builder()
                .utilisateur(utilisateur)
                .action(action)
                .message(message)
                .level(level != null ? level : "INFO")
                .build();
        userLogDAO.save(log);
    }

    public List<Map<String, Object>> getRecentLogs(int limit, Long userId) {
        int size = Math.max(1, Math.min(limit, 200));
        Pageable pageable = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<UserLog> logs = userId != null
                ? userLogDAO.findByUtilisateur_IdOrderByCreatedAtDesc(userId, pageable).getContent()
                : userLogDAO.findAllByOrderByCreatedAtDesc(pageable).getContent();
        return logs.stream().map(this::toMap).collect(Collectors.toList());
    }

    private Map<String, Object> toMap(UserLog log) {
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", log.getCreatedAt());
        map.put("level", log.getLevel());
        map.put("message", log.getMessage());
        map.put("action", log.getAction());
        map.put("user", log.getUtilisateur() != null ? log.getUtilisateur().getEmail() : null);
        map.put("userId", log.getUtilisateur() != null ? log.getUtilisateur().getId() : null);
        map.put("role", log.getUtilisateur() != null ? log.getUtilisateur().getRole().name() : null);
        map.put("roleDisplay", log.getUtilisateur() != null ? log.getUtilisateur().getRole().getDisplayName() : null);
        map.put("bibliotheque", log.getUtilisateur() != null && log.getUtilisateur().getBibliotheque() != null
                ? log.getUtilisateur().getBibliotheque().getNom()
                : null);
        return map;
    }
}
