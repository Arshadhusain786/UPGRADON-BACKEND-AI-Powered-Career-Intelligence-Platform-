package com.nexpath.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class SkillGapResponse {
    private Map<String, Object> data;
}
