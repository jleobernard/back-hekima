package com.leo.hekima.repository;

import com.leo.hekima.model.RefreshTokenModel;
import com.leo.hekima.model.UserAccount;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshTokenModel, Long> {

    @Query("""
              select u.* from user_account u
              left join refresh_token t on t.user_id = u.id
              WHERE t.token = :token
           """)
    Mono<UserAccount> findUserByRefreshToken(@Param("token") final String token);

    Mono<Void> deleteByToken(String token);
}
