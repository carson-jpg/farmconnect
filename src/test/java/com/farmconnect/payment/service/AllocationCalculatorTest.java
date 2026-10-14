package com.farmconnect.payment.service;

import com.farmconnect.payment.service.AllocationCalculator.FarmerWeight;
import com.farmconnect.payment.service.AllocationCalculator.Split;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class AllocationCalculatorTest {
    @Test void tenThousandShillings() {
        Split s = AllocationCalculator.split(10_000, 500, 500, List.of(new FarmerWeight(1, 1)));
        assertEquals(500, s.platformCents());
        assertEquals(500, s.countyCents());
        assertEquals(9_000, s.farmers().get(0).amountCents());
    }

    @Test void twoFarmersShareThePool() {
        Split s = AllocationCalculator.split(10_000, 500, 500, List.of(new FarmerWeight(1, 600), new FarmerWeight(2, 400)));
        assertEquals(5_400, s.farmers().get(0).amountCents());
        assertEquals(3_600, s.farmers().get(1).amountCents());
    }

    @Test void halfUpRounding() { // 5% of 10.50 = 0.525 -> 0.53
        assertEquals(53, AllocationCalculator.split(1_050, 500, 500, List.of(new FarmerWeight(1, 1))).platformCents());
    }

    @Test void alwaysSumsToTheSaleExactly() {
        Random r = new Random(7);
        for (int i = 0; i < 50_000; i++) {
            long sale = 1 + r.nextInt(50_000_000);
            List<FarmerWeight> w = new ArrayList<>();
            for (int j = 0, n = 1 + r.nextInt(5); j < n; j++) w.add(new FarmerWeight(j, 1 + r.nextInt(100_000)));
            Split s = AllocationCalculator.split(sale, 500, 500, w);
            assertEquals(sale, s.total());
            s.farmers().forEach(f -> assertTrue(f.amountCents() >= 0));
        }
    }

    @Test void rejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> AllocationCalculator.split(0, 500, 500, List.of(new FarmerWeight(1, 1))));
        assertThrows(IllegalArgumentException.class, () -> AllocationCalculator.split(100, 6_000, 6_000, List.of(new FarmerWeight(1, 1))));
        assertThrows(IllegalArgumentException.class, () -> AllocationCalculator.split(100, 500, 500, List.of()));
    }
}
