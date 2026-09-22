package com.artajerjes.biwengerassistant.offer;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    Optional<Offer> findByBiwengerOfferIdAndOwnerManager_Id(
            Long biwengerOfferId,
            Long ownerManagerId);

    List<Offer> findAllByLeague_IdAndOwnerManager_Id(
            Long leagueId,
            Long ownerManagerId);
}