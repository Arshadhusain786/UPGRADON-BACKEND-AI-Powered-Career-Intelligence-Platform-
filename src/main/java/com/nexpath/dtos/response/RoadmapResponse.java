package com.nexpath.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class RoadmapResponse {
    private Map<String, Object> data;
}