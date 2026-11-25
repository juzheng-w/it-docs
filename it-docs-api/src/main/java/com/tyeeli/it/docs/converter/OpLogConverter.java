package com.tyeeli.it.docs.converter;

import com.tyeeli.it.docs.dto.OpLogDTO;
import com.tyeeli.it.docs.repository.entity.OpLog;

import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志 实体 → DTO
 */
public final class OpLogConverter {

    private OpLogConverter() {
    }

    public static OpLogDTO toDTO(OpLog e) {
        if (e == null) {
            return null;
        }
        OpLogDTO v = new OpLogDTO();
        v.setId(e.getId());
        v.setOpType(e.getOpType());
        v.setDocId(e.getDocId());
        v.setOperator(e.getOperator());
        v.setBeforeJson(e.getBeforeJson());
        v.setAfterJson(e.getAfterJson());
        v.setOpTime(e.getOpTime());
        return v;
    }

    public static List<OpLogDTO> toDTOList(List<OpLog> list) {
        List<OpLogDTO> out = new ArrayList<OpLogDTO>();
        if (list != null) {
            for (OpLog e : list) {
                out.add(toDTO(e));
            }
        }
        return out;
    }
}
