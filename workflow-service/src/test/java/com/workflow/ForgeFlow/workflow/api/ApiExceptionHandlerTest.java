package com.workflow.ForgeFlow.workflow.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void missingJobUsesTheSharedErrorShape() {
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest("GET", "/api/v1/workflows/jobs/missing"));

        var response = handler.handleApi(new ApiException(HttpStatus.NOT_FOUND, "Job not found: missing"), request);

        assertEquals(404, response.getStatusCode().value());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("Job not found: missing", response.getBody().message());
        assertEquals("/api/v1/workflows/jobs/missing", response.getBody().path());
    }
}
