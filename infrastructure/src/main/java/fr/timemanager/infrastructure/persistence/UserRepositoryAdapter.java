package fr.timemanager.infrastructure.persistence;

import fr.timemanager.domain.user.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserRepositoryAdapter implements fr.timemanager.application.user.UserRepository {

  private final UserRepository jpaRepository;

  public UserRepositoryAdapter(UserRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public Optional<User> findById(UUID id) {
    return jpaRepository.findById(id.toString()).map(UserEntity::toDomain);
  }

  @Override
  public List<User> findAllByIds(Collection<UUID> ids) {
    List<String> keys = ids.stream().map(UUID::toString).toList();
    return jpaRepository.findAllById(keys).stream().map(UserEntity::toDomain).toList();
  }
}
