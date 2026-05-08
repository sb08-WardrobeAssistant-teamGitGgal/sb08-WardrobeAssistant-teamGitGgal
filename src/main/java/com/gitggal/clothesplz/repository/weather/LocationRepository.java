package com.gitggal.clothesplz.repository.weather;

import com.gitggal.clothesplz.entity.weather.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {

    Optional<Location> findByGridXAndGridY(Integer gridX, Integer gridY);

    @Query("SELECT l FROM Location l WHERE " +
            "l.latitude BETWEEN :lat - 0.0001 AND :lat + 0.0001 AND " +
            "l.longitude BETWEEN :lon - 0.0001 AND :lon + 0.0001")
    Optional<Location> findByApproximateLatitudeAndLongitude(
            @Param("lat") double latitude,
            @Param("lon") double longitude
    );
    List<Location> findByLocationNamesContaining(String locationName);
}
