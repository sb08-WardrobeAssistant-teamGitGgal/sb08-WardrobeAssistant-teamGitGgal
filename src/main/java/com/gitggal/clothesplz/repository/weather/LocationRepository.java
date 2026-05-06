package com.gitggal.clothesplz.repository.weather;

import com.gitggal.clothesplz.entity.weather.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {

    Optional<Location> findByGridXAndGridY(Integer gridX, Integer gridY);

    Optional<Location> findByLatitudeAndLongitude(double latitude, double longitude);

    Optional<Location> findByLocationNamesContaining(String locationName);
}
