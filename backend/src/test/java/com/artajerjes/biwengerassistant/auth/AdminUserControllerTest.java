package com.artajerjes.biwengerassistant.auth;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.artajerjes.biwengerassistant.auth.dto.CreateAssistantUserRequest;

class AdminUserControllerTest {

    @Test
    void createUserShouldAlwaysCreateRegularUser() {

        AssistantUserService assistantUserService = mock(AssistantUserService.class);

        AdminUserController controller = new AdminUserController(assistantUserService);

        CreateAssistantUserRequest request = new CreateAssistantUserRequest(
                "usuario-prueba",
                "password123",
                7L);

        controller.createUser(request);

        verify(assistantUserService).create(
                "usuario-prueba",
                "password123",
                AssistantRole.USER,
                7L);
    }
}