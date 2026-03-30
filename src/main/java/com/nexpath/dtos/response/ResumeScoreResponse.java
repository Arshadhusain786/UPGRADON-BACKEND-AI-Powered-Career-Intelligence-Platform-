package com.nexpath.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class ResumeScoreResponse {
    private Map<String, Object> data;
}
