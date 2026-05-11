package com.gitggal.clothesplz.repository.profile.impl;

import com.gitggal.clothesplz.entity.profile.Profile;
import com.gitggal.clothesplz.entity.profile.QProfile;
import com.gitggal.clothesplz.entity.user.QUser;
import com.gitggal.clothesplz.entity.user.User;
import com.gitggal.clothesplz.repository.profile.ProfileRepositoryCustom;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProfileRepositoryImpl implements ProfileRepositoryCustom {

  private final JPAQueryFactory jpaQueryFactory;

  private final QProfile p = QProfile.profile;
  private final QUser u = QUser.user;

  @Override
  public Optional<Profile> findByUser(User user) {
    return Optional.ofNullable(
        jpaQueryFactory
            .selectFrom(p)
            .join(p.user, u).fetchJoin()
            .where(p.user.eq(user))
            .fetchOne()
    );
  }
}
