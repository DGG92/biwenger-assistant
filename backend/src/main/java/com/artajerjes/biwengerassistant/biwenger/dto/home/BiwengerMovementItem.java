package com.artajerjes.biwengerassistant.biwenger.dto.home;

import java.util.List;

public record BiwengerMovementItem(
                Long player,
                BiwengerMovementUser from,
                BiwengerMovementUser to,
                Long amount,
                String type,
                Integer rounds,
                List<BiwengerMovementBid> bids) {

        public BiwengerMovementItem(
                        Long player,
                        BiwengerMovementUser from,
                        BiwengerMovementUser to,
                        Long amount,
                        String type,
                        List<BiwengerMovementBid> bids) {
                this(
                                player,
                                from,
                                to,
                                amount,
                                type,
                                null,
                                bids);
        }
}