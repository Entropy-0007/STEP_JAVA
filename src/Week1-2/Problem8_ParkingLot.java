import java.time.*;
import java.util.*;

/**
 * Problem 8: Parking Lot Management with Open Addressing
 * Concepts: Open addressing (linear probing), collision resolution, custom hash functions,
 *           load factor management, billing system
 */
public class Problem8_ParkingLot {

    private static final double HOURLY_RATE = 5.0;
    private static final int EMPTY = 0;
    private static final int OCCUPIED = 1;
    private static final int DELETED = 2; // Tombstone for linear probing

    // Parking Spot record
    private static class ParkingSpot {
        int status;        // EMPTY, OCCUPIED, DELETED
        String licensePlate;
        LocalDateTime entryTime;
        int spotNumber;

        ParkingSpot(int spotNumber) {
            this.spotNumber = spotNumber;
            this.status = EMPTY;
        }
    }

    private final int capacity;
    private final ParkingSpot[] spots;
    private int occupiedCount = 0;

    // License plate → spot number (for O(1) exit lookup)
    private final HashMap<String, Integer> plateToSpot = new HashMap<>();

    // Statistics
    private int totalProbes = 0;
    private int totalParkings = 0;
    private final Map<Integer, Integer> hourlyOccupancy = new TreeMap<>();

    public Problem8_ParkingLot(int capacity) {
        this.capacity = capacity;
        this.spots = new ParkingSpot[capacity];
        for (int i = 0; i < capacity; i++) {
            spots[i] = new ParkingSpot(i + 1); // spot numbers 1-indexed
        }
    }

    // Custom hash function: maps license plate to a preferred spot
    private int hashLicensePlate(String plate) {
        int hash = 0;
        for (char c : plate.toCharArray()) {
            hash = (hash * 31 + c) % capacity;
        }
        return Math.abs(hash);
    }

    // Park a vehicle using linear probing — O(1) amortized
    public String parkVehicle(String licensePlate) {
        if (occupiedCount >= capacity) return "Parking lot is FULL";
        if (plateToSpot.containsKey(licensePlate))
            return licensePlate + " is already parked at spot #" + plateToSpot.get(licensePlate);

        int preferred = hashLicensePlate(licensePlate);
        int index = preferred;
        int probes = 0;
        StringBuilder probeLog = new StringBuilder();

        while (spots[index].status == OCCUPIED) {
            probeLog.append("... Spot #").append(spots[index].spotNumber).append(" occupied ");
            index = (index + 1) % capacity;
            probes++;
            if (probes >= capacity) return "ERROR: Lot full (concurrent issue)";
        }

        spots[index].status = OCCUPIED;
        spots[index].licensePlate = licensePlate;
        spots[index].entryTime = LocalDateTime.now();

        plateToSpot.put(licensePlate, index);
        occupiedCount++;

        totalProbes += probes;
        totalParkings++;
        int hour = LocalDateTime.now().getHour();
        hourlyOccupancy.merge(hour, occupiedCount, Math::max);

        String probeStr = probeLog.length() > 0 ? probeLog.toString() : "";
        String result = String.format("parkVehicle(\"%s\") → %sAssigned spot #%d (%d probe%s)",
                licensePlate, probeStr, spots[index].spotNumber, probes, probes == 1 ? "" : "s");
        System.out.println(result);
        return result;
    }

    // Vehicle exits — calculate fee
    public String exitVehicle(String licensePlate) {
        Integer index = plateToSpot.get(licensePlate);
        if (index == null) return licensePlate + " not found in parking lot";

        ParkingSpot spot = spots[index];
        LocalDateTime exitTime = LocalDateTime.now();
        Duration duration = Duration.between(spot.entryTime, exitTime);

        long minutes = duration.toMinutes();
        // Minimum 15 minutes billing
        long billableMinutes = Math.max(15, minutes);
        double fee = (billableMinutes / 60.0) * HOURLY_RATE;

        // Mark as DELETED (tombstone) — allows future linear probing to work correctly
        spot.status = DELETED;
        spot.licensePlate = null;
        spot.entryTime = null;

        plateToSpot.remove(licensePlate);
        occupiedCount--;

        String result = String.format(
                "exitVehicle(\"%s\") → Spot #%d freed, Duration: %dh %dm, Fee: $%.2f",
                licensePlate, spot.spotNumber,
                duration.toHours(), duration.toMinutesPart(), fee);
        System.out.println(result);
        return result;
    }

    // Find nearest available spot to entrance (spot #1)
    public int findNearestAvailableSpot() {
        for (int i = 0; i < capacity; i++) {
            if (spots[i].status != OCCUPIED) return spots[i].spotNumber;
        }
        return -1;
    }

    // Load factor check — rehash (expand) if > 0.7
    public double getLoadFactor() {
        return (double) occupiedCount / capacity;
    }

    public void getStatistics() {
        double avgProbes = totalParkings == 0 ? 0 : (double) totalProbes / totalParkings;
        int peakHour = hourlyOccupancy.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);

        System.out.printf("getStatistics() → Occupancy: %.0f%%, Avg Probes: %.1f, Peak Hour: %d:00-%d:00%n",
                getLoadFactor() * 100, avgProbes,
                peakHour < 0 ? 0 : peakHour,
                peakHour < 0 ? 1 : peakHour + 1);
        System.out.println("  Total Parked: " + totalParkings + ", Current: " + occupiedCount + "/" + capacity);
        System.out.println("  Load Factor: " + String.format("%.2f", getLoadFactor()));
        System.out.println("  Nearest available spot: " + findNearestAvailableSpot());
    }

    public static void main(String[] args) {
        System.out.println("=== Problem 8: Parking Lot Management with Open Addressing ===\n");

        Problem8_ParkingLot lot = new Problem8_ParkingLot(500);

        // Sample input from the problem
        lot.parkVehicle("ABC-1234");
        lot.parkVehicle("ABC-1235");
        lot.parkVehicle("XYZ-9999");

        lot.exitVehicle("ABC-1234");

        // Demonstrate load factor
        System.out.println("\nParking 200 more vehicles...");
        for (int i = 0; i < 200; i++) {
            lot.parkVehicle("CAR-" + String.format("%04d", i));
        }

        lot.getStatistics();

        // Test collision scenario
        System.out.println("\n--- Collision Test (small lot, 5 spots) ---");
        Problem8_ParkingLot smallLot = new Problem8_ParkingLot(5);
        smallLot.parkVehicle("AAA-001");
        smallLot.parkVehicle("AAA-002");
        smallLot.parkVehicle("AAA-003");
        smallLot.getStatistics();
    }
}
