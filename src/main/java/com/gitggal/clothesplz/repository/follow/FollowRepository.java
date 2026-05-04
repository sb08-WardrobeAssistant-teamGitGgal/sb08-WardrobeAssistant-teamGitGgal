package com.gitggal.clothesplz.repository.follow;

import com.gitggal.clothesplz.entity.follow.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {
}
