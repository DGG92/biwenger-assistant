package com.artajerjes.biwengerassistant.offer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.manager.ManagerRepository;

@SpringBootTest
@Transactional
class OfferRepositoryTest {

    @Autowired
    private OfferRepository offerRepository;

    @Autowired
    private LeagueRepository leagueRepository;

    @Autowired
    private ManagerRepository managerRepository;

    @Test
    void shouldAllowSameBiwengerOfferIdForDifferentOwners() {

        League league = leagueRepository.save(
                new League(
                        "Liga ofertas",
                        "offer-league-1"));

        Manager firstManager = managerRepository.save(
                createManager(
                        1001L,
                        "Manager A",
                        1,
                        league));

        Manager secondManager = managerRepository.save(
                createManager(
                        1002L,
                        "Manager B",
                        2,
                        league));

        Offer firstOffer = offerRepository.saveAndFlush(
                createOffer(
                        123456L,
                        firstManager,
                        league));

        Offer secondOffer = offerRepository.saveAndFlush(
                createOffer(
                        123456L,
                        secondManager,
                        league));

        assertThat(firstOffer.getId()).isNotNull();
        assertThat(secondOffer.getId()).isNotNull();
        assertThat(firstOffer.getBiwengerOfferId())
                .isEqualTo(secondOffer.getBiwengerOfferId());
        assertThat(firstOffer.getOwnerManager().getId())
                .isNotEqualTo(secondOffer.getOwnerManager().getId());
    }

    @Test
    void shouldRejectSameBiwengerOfferIdForSameOwner() {

        League league = leagueRepository.save(
                new League(
                        "Liga ofertas duplicadas",
                        "offer-league-2"));

        Manager manager = managerRepository.save(
                createManager(
                        2001L,
                        "Manager",
                        1,
                        league));

        offerRepository.saveAndFlush(
                createOffer(
                        654321L,
                        manager,
                        league));

        assertThatThrownBy(() -> offerRepository.saveAndFlush(
                createOffer(
                        654321L,
                        manager,
                        league)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Offer createOffer(
            Long biwengerOfferId,
            Manager ownerManager,
            League league) {

        LocalDateTime now = LocalDateTime.now();

        return new Offer(
                biwengerOfferId,
                1_000_000L,
                "waiting",
                "purchase",
                ownerManager,
                null,
                ownerManager,
                now,
                now.plusDays(1),
                List.of(),
                league);
    }

    private Manager createManager(
            Long biwengerManagerId,
            String name,
            Integer position,
            League league) {

        return new Manager(
                biwengerManagerId,
                name,
                null,
                0,
                0,
                0L,
                0L,
                position,
                "manager",
                league);
    }
}