package com.tyeeli.it.docs.converter;

import com.tyeeli.it.docs.dto.AclDTO;
import com.tyeeli.it.docs.dto.AclItemRequest;
import com.tyeeli.it.docs.dto.SpaceDTO;
import com.tyeeli.it.docs.repository.entity.Space;
import com.tyeeli.it.docs.repository.entity.SpaceAcl;

import java.util.ArrayList;
import java.util.List;

/**
 * 空间 / 授权 实体 ↔ DTO/DTO
 */
public final class SpaceConverter {

    private SpaceConverter() {
    }

    public static SpaceDTO toDTO(Space s) {
        if (s == null) {
            return null;
        }
        SpaceDTO v = new SpaceDTO();
        v.setId(s.getId());
        v.setSpaceName(s.getSpaceName());
        v.setSpaceCode(s.getSpaceCode());
        v.setSpaceType(s.getSpaceType());
        v.setVisibility(s.getVisibility());
        v.setDescription(s.getDescription());
        v.setOwner(s.getOwner());
        v.setStatus(s.getStatus());
        v.setCreatedAt(s.getCreatedAt());
        return v;
    }

    public static List<SpaceDTO> toDTOList(List<Space> list) {
        List<SpaceDTO> out = new ArrayList<SpaceDTO>();
        if (list != null) {
            for (Space s : list) {
                out.add(toDTO(s));
            }
        }
        return out;
    }

    public static AclDTO toDTO(SpaceAcl a) {
        if (a == null) {
            return null;
        }
        AclDTO v = new AclDTO();
        v.setId(a.getId());
        v.setSpaceId(a.getSpaceId());
        v.setSubjectType(a.getSubjectType());
        v.setSubjectName(a.getSubjectName());
        v.setRole(a.getRole());
        v.setCreatedBy(a.getCreatedBy());
        v.setCreatedAt(a.getCreatedAt());
        return v;
    }

    public static List<AclDTO> toAclDTOList(List<SpaceAcl> list) {
        List<AclDTO> out = new ArrayList<AclDTO>();
        if (list != null) {
            for (SpaceAcl a : list) {
                out.add(toDTO(a));
            }
        }
        return out;
    }

    public static SpaceAcl toEntity(Long spaceId, AclItemRequest req, String operator) {
        SpaceAcl a = new SpaceAcl();
        a.setSpaceId(spaceId);
        a.setSubjectType(req.getSubjectType().trim());
        a.setSubjectName(req.getSubjectName().trim());
        a.setRole(req.getRole().trim());
        a.setCreatedBy(operator);
        return a;
    }
}
