package com.workflow.ForgeFlow.workflow.api;

import com.workflow.ForgeFlow.workflow.JobRecord;
import com.workflow.ForgeFlow.workflow.WorkflowAiService;
import com.workflow.ForgeFlow.workflow.WorkflowExecutionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowJobController {

    private final WorkflowExecutionService workflowExecutionService;
    private final WorkflowAiService workflowAiService;

    public WorkflowJobController(
            WorkflowExecutionService workflowExecutionService,
            WorkflowAiService workflowAiService) {
        this.workflowExecutionService = workflowExecutionService;
        this.workflowAiService = workflowAiService;
    }

    @PostMapping("/execute")
    public ResponseEntity<ExecuteWorkflowResponse> execute(@Valid @RequestBody ExecuteWorkflowRequest request) {
        JobRecord job = workflowExecutionService.enqueue(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ExecuteWorkflowResponse(
                job.jobId(),
                job.status(),
                "Job accepted. Poll GET /api/v1/workflows/jobs/{jobId} for status."
        ));
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@Valid @RequestBody GenerateWorkflowRequest request) {
        if (!workflowAiService.isConfigured()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                    "error",
                    "Groq key is empty. In application.properties set forgeflow.ai.groq.api-key=gsk_your_key (no quotes), save, restart spring-boot:run."
            ));
        }
        try {
            JobRecord job = workflowAiService.generateAndEnqueue(request.prompt());
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(new GenerateWorkflowResponse(
                    job.jobId(),
                    job.status(),
                    job.workflowName(),
                    job.payload(),
                    "Created from prompt. Poll GET /api/v1/workflows/jobs/{jobId}."
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "error", ex.getMessage() == null ? "AI call failed" : ex.getMessage()
            ));
        }
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<JobRecord> getJob(@PathVariable String jobId) {
        return workflowExecutionService.getJob(jobId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
