package com.workflow.ForgeFlow.workflow;

import com.workflow.ForgeFlow.workflow.ai.GeneratedWorkflow;
import com.workflow.ForgeFlow.workflow.ai.GroqWorkflowGenerator;
import com.workflow.ForgeFlow.workflow.api.ExecuteWorkflowRequest;
import org.springframework.stereotype.Service;

@Service
public class WorkflowAiService {

    private final GroqWorkflowGenerator groqWorkflowGenerator;
    private final WorkflowExecutionService workflowExecutionService;

    public WorkflowAiService(
            GroqWorkflowGenerator groqWorkflowGenerator,
            WorkflowExecutionService workflowExecutionService) {
        this.groqWorkflowGenerator = groqWorkflowGenerator;
        this.workflowExecutionService = workflowExecutionService;
    }

    public boolean isConfigured() {
        return groqWorkflowGenerator.isConfigured();
    }

    public JobRecord generateAndEnqueue(String prompt) {
        GeneratedWorkflow generated = groqWorkflowGenerator.generate(prompt);
        return workflowExecutionService.enqueue(
                new ExecuteWorkflowRequest(generated.workflowName(), generated.payload()));
    }
}
