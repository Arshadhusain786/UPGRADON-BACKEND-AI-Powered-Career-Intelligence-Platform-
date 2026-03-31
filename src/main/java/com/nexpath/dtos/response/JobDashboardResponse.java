package com.nexpath.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JobDashboardResponse {
    private long totalPostsCreated;
    private long totalConnectionsReceived;
    private long totalConnectionsSent;
    private long pendingRequests;
    private long acceptedConnections;
    private long freeConnectionsRemaining;
    private int lockedCredits;
    private int totalCredits;
}
