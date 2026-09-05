package com.artajerjes.biwengerassistant.auth;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.manager.ManagerRepository;

@SpringBootTest
@Transactional
class AssistantUserRepositoryTest {

    @Autowired
    private AssistantUserRepository assistantUserRepository;

    @Autowired
    private LeagueRepository leagueRepository;

    @Autowired
    private ManagerRepository managerRepository;

    @Test
    void shouldFindUserByUsernameIgnoringCase() {
        AssistantUser user = new AssistantUser(
                "diego",
                "hashed-password",
                AssistantRole.ADMIN,
                null);

        assistantUserRepository.save(user);

        Optional<AssistantUser> result = assistantUserRepository.findByUsernameIgnoreCase("DIEGO");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("diego");
        assertThat(result.get().getRole()).isEqualTo(AssistantRole.ADMIN);
        assertThat(result.get().isEnabled()).isTrue();
    }

    @Test
    void shouldDetectExistingUsernameIgnoringCase() {
        AssistantUser user = new AssistantUser(
                "manager1",
                "hashed-password",
                AssistantRole.USER,
                null);

        assistantUserRepository.save(user);

        assertThat(
                assistantUserRepository.existsByUsernameIgnoreCase("MANAGER1"))
                .isTrue();
    }

    @Test
    void shouldAssociateUserWithManager() {
        League league = leagueRepository.save(
                new League("Liga amigos", "12345"));

        Manager manager = managerRepository.save(
                new Manager(
                        999L,
                        "Diego",
                        null,
                        100,
                        20,
                        50_000_000L,
                        100_000L,
                        1,
                        "manager",
                        league));

        AssistantUser user = new AssistantUser(
                "diego",
                "hashed-password",
                AssistantRole.ADMIN,
                manager);

        AssistantUser saved = assistantUserRepository.save(user);

        assertThat(saved.getManager()).isNotNull();
        assertThat(saved.getManager().getId()).isEqualTo(manager.getId());
        assertThat(saved.getManager().getLeague().getId())
                .isEqualTo(league.getId());
    }
}