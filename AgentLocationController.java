package com.hungerexpress.tracking;

import com.hungerexpress.agent.AgentProfile;
import com.hungerexpress.agent.AgentProfileRepository;
import com.hungerexpress.common.CurrentUser;
import com.hungerexpress.user.User;
import com.hungerexpress.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class AgentLocationController {
    
    private final UserRepository userRepo;
    private final AgentProfileRepository agentProfileRepo;
    
    // Update agent location (Agent)
    @PutMapping("/location")
    @PreAuthorize("hasRole('AGENT')")
    public ResponseEntity<LocationResponse> updateLocation(@RequestBody UpdateLocationRequest request) {
        String email = CurrentUser.email();
        if (email == null) return ResponseEntity.status(401).build();
        
        User user = userRepo.findByEmail(email).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        
        AgentProfile profile = agentProfileRepo.findByUserId(user.getId()).orElse(null);
        if (profile == null) return ResponseEntity.notFound().build();
        
        profile.setCurrentLatitude(request.latitude());
        profile.setCurrentLongitude(request.longitude());
        profile.setLastLocationUpdate(Instant.now());
        
        agentProfileRepo.save(profile);
        
        LocationResponse response = new LocationResponse(
            profile.getCurrentLatitude(),
            profile.getCurrentLongitude(),
            profile.getLastLocationUpdate()
        );
        
        return ResponseEntity.ok(response);
    }
    
    // Get agent location by ID (Customer tracking their order)
    @GetMapping("/agent/{agentId}")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ResponseEntity<LocationResponse> getAgentLocation(@PathVariable Long agentId) {
        AgentProfile profile = agentProfileRepo.findByUserId(agentId).orElse(null);
        if (profile == null) return ResponseEntity.notFound().build();
        
        // Only return location if agent is currently delivering (online)
        if (!Boolean.TRUE.equals(profile.getIsAvailable())) {
            return ResponseEntity.status(404).build();
        }
        
        LocationResponse response = new LocationResponse(
            profile.getCurrentLatitude(),
            profile.getCurrentLongitude(),
            profile.getLastLocationUpdate()
        );
        
        return ResponseEntity.ok(response);
    }
    
    // Get all active agents with locations (Admin/Map view)
    @GetMapping("/agents/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AgentLocationDto>> getActiveAgentsLocations() {
        List<AgentProfile> activeAgents = agentProfileRepo.findAll().stream()
            .filter(agent -> Boolean.TRUE.equals(agent.getIsAvailable()))
            .filter(agent -> agent.getCurrentLatitude() != null && agent.getCurrentLongitude() != null)
            .collect(Collectors.toList());
        
        List<AgentLocationDto> locations = activeAgents.stream()
            .map(agent -> {
                User user = userRepo.findById(agent.getUserId()).orElse(null);
                return new AgentLocationDto(
                    agent.getUserId(),
                    user != null ? user.getFullName() : "Unknown",
                    agent.getCurrentLatitude(),
                    agent.getCurrentLongitude(),
                    agent.getLastLocationUpdate()
                );
            })
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(locations);
    }
    
    // Get nearby agents (for order assignment)
    @GetMapping("/agents/nearby")
    @PreAuthorize("hasRole('ADMIN') or hasRole('OWNER')")
    public ResponseEntity<List<NearbyAgentDto>> getNearbyAgents(
        @RequestParam Double latitude,
        @RequestParam Double longitude,
        @RequestParam(defaultValue = "5.0") Double radiusKm
    ) {
        List<AgentProfile> availableAgents = agentProfileRepo.findAll().stream()
            .filter(agent -> Boolean.TRUE.equals(agent.getIsAvailable()))
            .filter(agent -> agent.getCurrentLatitude() != null && agent.getCurrentLongitude() != null)
            .collect(Collectors.toList());
        
        List<NearbyAgentDto> nearbyAgents = availableAgents.stream()
            .map(agent -> {
                double distance = calculateDistance(
                    latitude, longitude,
                    agent.getCurrentLatitude(), agent.getCurrentLongitude()
                );
                
                User user = userRepo.findById(agent.getUserId()).orElse(null);
                
                return new NearbyAgentDto(
                    agent.getUserId(),
                    user != null ? user.getFullName() : "Unknown",
                    agent.getCurrentLatitude(),
                    agent.getCurrentLongitude(),
                    distance,
                    agent.getVehicleType(),
                    agent.getVehicleNumber()
                );
            })
            .filter(dto -> dto.distanceKm() <= radiusKm)
            .sorted((a, b) -> Double.compare(a.distanceKm(), b.distanceKm()))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(nearbyAgents);
    }
    
    // Calculate distance between two coordinates (Haversine formula)
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371; // Radius in kilometers
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c;
    }
    
    // DTOs
    record UpdateLocationRequest(Double latitude, Double longitude) {}
    
    record LocationResponse(Double latitude, Double longitude, Instant lastUpdate) {}
    
    record AgentLocationDto(
        Long agentId,
        String agentName,
        Double latitude,
        Double longitude,
        Instant lastUpdate
    ) {}
    
    record NearbyAgentDto(
        Long agentId,
        String agentName,
        Double latitude,
        Double longitude,
        Double distanceKm,
        String vehicleType,
        String vehicleNumber
    ) {}
}
