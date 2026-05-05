package com.gitggal.clothesplz.repository.profile;

import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.user.User;
import java.util.Optional;

public interface ProfileRepositoryCustom {

  Optional<Profile> findByUser(User user);
}
