package com.artajerjes.biwengerassistant.auth;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AssistantUserDetailsService implements UserDetailsService {

    private final AssistantUserRepository assistantUserRepository;

    public AssistantUserDetailsService(
            AssistantUserRepository assistantUserRepository) {
        this.assistantUserRepository = assistantUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        AssistantUser assistantUser = assistantUserRepository
                .findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Assistant user not found: " + username));

        return User.builder()
                .username(assistantUser.getUsername())
                .password(assistantUser.getPasswordHash())
                .roles(assistantUser.getRole().name())
                .disabled(!assistantUser.isEnabled())
                .build();
    }
}