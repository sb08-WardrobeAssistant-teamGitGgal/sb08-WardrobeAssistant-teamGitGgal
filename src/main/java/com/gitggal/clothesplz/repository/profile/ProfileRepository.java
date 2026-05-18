package com.gitggal.clothesplz.repository.profile;

import com.gitggal.clothesplz.entity.profile.Profile;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProfileRepository extends JpaRepository<Profile, UUID>, ProfileRepositoryCustom {

    @EntityGraph(attributePaths = "user")
    List<Profile> findByGridXAndGridY(Integer gridX, Integer gridY);
}
