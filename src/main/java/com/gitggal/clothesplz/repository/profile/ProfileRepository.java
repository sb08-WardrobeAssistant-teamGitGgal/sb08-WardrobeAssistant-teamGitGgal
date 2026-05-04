package com.gitggal.clothesplz.repository.profile;

import com.gitggal.clothesplz.entity.profile.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProfileRepository extends JpaRepository<Profile, UUID> {
}
