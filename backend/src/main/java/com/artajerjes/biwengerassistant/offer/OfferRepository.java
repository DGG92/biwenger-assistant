package com.artajerjes.biwengerassistant.offer;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    Optional<Offer> findByBiwengerOfferId(Long biwengerOfferId);

    List<Offer> findAllByLeague_Id(Long leagueId);

    void deleteAllByLeague_Id(Long leagueId);
}