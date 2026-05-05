package com.gitggal.clothesplz.repository.profile;

import com.gitggal.clothesplz.entity.profile.Profile;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID>, ProfileRepositoryCustom {

}
