package com.artajerjes.biwengerassistant.auth;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.artajerjes.biwengerassistant.auth.dto.CreateAssistantUserRequest;
import com.artajerjes.biwengerassistant.auth.dto.AvailableManagerResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AssistantUserService assistantUserService;

    public AdminUserController(
            AssistantUserService assistantUserService) {
        this.assistantUserService = assistantUserService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void createUser(
            @Valid @RequestBody CreateAssistantUserRequest request) {

        assistantUserService.create(
                request.username(),
                request.password(),
                AssistantRole.USER,
                request.managerId());
    }

    @GetMapping("/available-managers")
    public List<AvailableManagerResponse> getAvailableManagers() {
        return assistantUserService.findAvailableManagers();
    }
}