package com.artajerjes.biwengerassistant.offer.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OfferResponse(
                Long id,
                Long biwengerOfferId,
                Long amount,
                String status,
                String type,
                Long fromManagerId,
                String fromManagerName,
                Long toManagerId,
                String toManagerName,
                LocalDateTime createdAt,
                LocalDateTime expiresAt,
                List<OfferPlayerResponse> requestedPlayers) {
}