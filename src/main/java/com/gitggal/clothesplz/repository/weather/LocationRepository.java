package com.gitggal.clothesplz.repository.weather;

import com.gitggal.clothesplz.entity.weather.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {
}
