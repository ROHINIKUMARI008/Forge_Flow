package com.workflow.ForgeFlow.workflow;

import com.workflow.ForgeFlow.workflow.persistence.JobEntity;
import com.workflow.ForgeFlow.workflow.persistence.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Writes retry/failure state in a new transaction so a requeue exception
 * does not roll back the attempt counter.
 */
@Service
public class JobRetryRecorder {

    private final JobRepository jobRepository;

    public JobRetryRecorder(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean recordFailureAndShouldRequeue(String jobId, String error, int maxAttempts) {
        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("Job not found: " + jobId));
        job.registerFailedAttempt(error);
        if (job.getAttemptCount() >= maxAttempts) {
            job.markFailed(error);
            jobRepository.save(job);
            return false;
        }
        jobRepository.save(job);
        return true;
    }
}
