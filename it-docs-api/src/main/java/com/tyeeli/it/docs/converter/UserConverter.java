package com.tyeeli.it.docs.converter;

import com.tyeeli.it.docs.dto.UserDTO;
import com.tyeeli.it.docs.repository.entity.User;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户 实体 → DTO（passwordHash 永不外传）
 */
public final class UserConverter {

    private UserConverter() {
    }

    public static UserDTO toDTO(User u) {
        if (u == null) {
            return null;
        }
        UserDTO v = new UserDTO();
        v.setId(u.getId());
        v.setUsername(u.getUsername());
        v.setDisplayName(u.getDisplayName());
        v.setAuthSource(u.getAuthSource());
        v.setRole(u.getRole());
        v.setSysScope(splitScope(u.getSysScope()));
        v.setEmail(u.getEmail());
        v.setStatus(u.getStatus());
        v.setMustChangePwd(u.getMustChangePwd() != null && u.getMustChangePwd() == 1);
        v.setLastLoginAt(u.getLastLoginAt());
        v.setCreatedAt(u.getCreatedAt());
        v.setTempPassword(u.getTempPassword());
        return v;
    }

    public static List<UserDTO> toDTOList(List<User> list) {
        List<UserDTO> out = new ArrayList<UserDTO>();
        if (list != null) {
            for (User u : list) {
                out.add(toDTO(u));
            }
        }
        return out;
    }

    /** sysScope 逗号串 → 列表（空 = 全部系统） */
    public static List<String> splitScope(String sysScope) {
        List<String> out = new ArrayList<String>();
        if (sysScope != null) {
            for (String s : sysScope.split(",")) {
                if (s != null && !s.trim().isEmpty()) {
                    out.add(s.trim());
                }
            }
        }
        return out;
    }

    /** 列表 → 逗号串（去空去重，最多 20 项）；空列表 = null（全部系统） */
    public static String joinScope(List<String> systems) {
        if (systems == null) {
            return null;
        }
        List<String> uniq = new ArrayList<String>();
        for (String s : systems) {
            if (s == null) {
                continue;
            }
            String t = s.trim();
            if (!t.isEmpty() && !uniq.contains(t)) {
                uniq.add(t);
            }
        }
        if (uniq.isEmpty()) {
            return null;
        }
        if (uniq.size() > 20) {
            throw new IllegalArgumentException("可见系统最多 20 个");
        }
        StringBuilder sb = new StringBuilder();
        for (String s : uniq) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(s);
        }
        return sb.toString();
    }
}
