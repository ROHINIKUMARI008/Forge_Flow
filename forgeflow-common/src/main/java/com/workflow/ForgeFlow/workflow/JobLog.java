package com.workflow.ForgeFlow.workflow;

import org.slf4j.MDC;

public final class JobLog {

    public static final String JOB_ID = "jobId";

    private JobLog() {
    }

    public static void run(String jobId, Runnable action) {
        MDC.put(JOB_ID, jobId);
        try {
            action.run();
        } finally {
            MDC.remove(JOB_ID);
        }
    }
}
