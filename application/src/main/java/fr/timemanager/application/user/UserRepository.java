package fr.timemanager.application.user;

import fr.timemanager.domain.user.User;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port — annuaire des utilisateurs (lecture seule pour l'affichage et le périmètre). */
public interface UserRepository {

  Optional<User> findById(UUID id);

  List<User> findAllByIds(Collection<UUID> ids);
}
