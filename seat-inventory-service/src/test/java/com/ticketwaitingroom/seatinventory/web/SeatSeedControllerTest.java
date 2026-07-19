package com.ticketwaitingroom.seatinventory.web;

import com.ticketwaitingroom.seatinventory.inventory.SeatSeedService;
import com.ticketwaitingroom.seatinventory.inventory.SeatSeedService.SeedResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class SeatSeedControllerTest {

    private final SeatSeedService seatSeedService = mock(SeatSeedService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new SeatSeedController(seatSeedService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void returns201WithCreatedAndSkippedCounts() throws Exception {
        when(seatSeedService.seedSeats("evt-1", List.of("A-1", "A-2", "A-3")))
                .thenReturn(new SeedResult(2, 1));

        mockMvc.perform(post("/admin/events/{eventId}/seats", "evt-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"seatIds\":[\"A-1\",\"A-2\",\"A-3\"]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value("evt-1"))
                .andExpect(jsonPath("$.created").value(2))
                .andExpect(jsonPath("$.skipped").value(1));
    }
}
