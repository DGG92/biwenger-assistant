package com.artajerjes.biwengerassistant.biwenger.dto.user;

import java.util.List;

public record BiwengerUserLineup(
                String type,
                BiwengerLineupPlayerRef captain,
                BiwengerLineupPlayerRef striker,
                BiwengerLineupPlayerRef coach,
                Long date,
                List<Long> playersID,
                List<Long> reservesID,
                List<BiwengerLineupReserve> reserves) {
}