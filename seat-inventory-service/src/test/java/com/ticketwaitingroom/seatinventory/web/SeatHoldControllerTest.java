package com.ticketwaitingroom.seatinventory.web;

import com.ticketwaitingroom.common.exceptions.SeatUnavailableException;
import com.ticketwaitingroom.seatinventory.inventory.SeatHold;
import com.ticketwaitingroom.seatinventory.inventory.SeatHoldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SeatHoldControllerTest {

    private final SeatHoldService seatHoldService = mock(SeatHoldService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new SeatHoldController(seatHoldService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void returns201WithHoldOnSuccess() throws Exception {
        when(seatHoldService.holdSeat(eq("evt-1"), eq("A-12")))
                .thenReturn(new SeatHold("evt-1", "A-12", "hold-abc", Instant.parse("2026-07-05T12:00:00Z")));

        mockMvc.perform(post("/events/{eventId}/seats/{seatId}/hold", "evt-1", "A-12"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.seatId").value("A-12"))
                .andExpect(jsonPath("$.holdId").value("hold-abc"));
    }

    @Test
    void returns409WhenSeatUnavailable() throws Exception {
        when(seatHoldService.holdSeat(eq("evt-1"), eq("A-12")))
                .thenThrow(new SeatUnavailableException("Seat A-12 of event evt-1 is not available to hold"));

        mockMvc.perform(post("/events/{eventId}/seats/{seatId}/hold", "evt-1", "A-12"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Seat unavailable"));
    }
}
