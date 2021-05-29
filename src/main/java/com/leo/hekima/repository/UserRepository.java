package com.leo.hekima.repository;

import com.leo.hekima.model.UserAccount;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<UserAccount, Long> {

    Mono<UserAccount> findByEmail(final String email);

    Mono<UserAccount> findByUri(final String uri);
}
