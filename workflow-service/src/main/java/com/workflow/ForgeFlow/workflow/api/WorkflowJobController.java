package com.workflow.ForgeFlow.workflow.api;

import com.workflow.ForgeFlow.workflow.ExecutionResult;
import com.workflow.ForgeFlow.workflow.IdempotentExecutionService;
import com.workflow.ForgeFlow.workflow.JobRecord;
import com.workflow.ForgeFlow.workflow.WorkflowAiService;
import com.workflow.ForgeFlow.workflow.WorkflowExecutionService;
import com.workflow.ForgeFlow.workflow.ai.GroqCallException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflows")
public class WorkflowJobController {

    private final IdempotentExecutionService idempotentExecutionService;
    private final WorkflowExecutionService workflowExecutionService;
    private final WorkflowAiService workflowAiService;

    public WorkflowJobController(
            IdempotentExecutionService idempotentExecutionService,
            WorkflowExecutionService workflowExecutionService,
            WorkflowAiService workflowAiService) {
        this.idempotentExecutionService = idempotentExecutionService;
        this.workflowExecutionService = workflowExecutionService;
        this.workflowAiService = workflowAiService;
    }

    @PostMapping("/execute")
    @Operation(summary = "Enqueue a workflow job")
    public ResponseEntity<ExecuteWorkflowResponse> execute(
            @Valid @RequestBody ExecuteWorkflowRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        ExecutionResult result = idempotentExecutionService.execute(request, idempotencyKey);
        JobRecord job = result.job();
        String message = result.replayed()
                ? "Existing job returned for this Idempotency-Key."
                : "Job accepted. Poll GET /api/v1/workflows/jobs/{jobId} for status.";
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new ExecuteWorkflowResponse(
                job.jobId(),
                job.status(),
                message,
                result.replayed()
        ));
    }

    @PostMapping("/generate")
    @Operation(summary = "Turn a prompt into a job and enqueue it")
    public ResponseEntity<GenerateWorkflowResponse> generate(@Valid @RequestBody GenerateWorkflowRequest request) {
        if (!workflowAiService.isConfigured()) {
            throw new GroqCallException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Groq key is empty. Set GROQ_API_KEY and restart workflow-service.");
        }
        JobRecord job = workflowAiService.generateAndEnqueue(request.prompt());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new GenerateWorkflowResponse(
                job.jobId(),
                job.status(),
                job.workflowName(),
                job.payload(),
                "Created from prompt. Poll GET /api/v1/workflows/jobs/{jobId}."
        ));
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Read one job")
    public JobRecord getJob(@PathVariable String jobId) {
        return workflowExecutionService.getJob(jobId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Job not found: " + jobId));
    }
}
