package com.artajerjes.biwengerassistant.credential;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.artajerjes.biwengerassistant.credential.dto.BiwengerCredentialStatusResponse;
import com.artajerjes.biwengerassistant.credential.dto.SaveBiwengerCredentialRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profile/biwenger")
public class BiwengerCredentialController {

    private final BiwengerCredentialService biwengerCredentialService;

    public BiwengerCredentialController(
            BiwengerCredentialService biwengerCredentialService) {

        this.biwengerCredentialService = biwengerCredentialService;
    }

    @GetMapping
    public BiwengerCredentialStatusResponse getStatus() {

        return biwengerCredentialService
                .getCurrentCredentialStatus();
    }

    @PutMapping
    public BiwengerCredentialStatusResponse save(
            @Valid @RequestBody SaveBiwengerCredentialRequest request) {

        return biwengerCredentialService
                .saveCurrentCredential(request.token());
    }
}