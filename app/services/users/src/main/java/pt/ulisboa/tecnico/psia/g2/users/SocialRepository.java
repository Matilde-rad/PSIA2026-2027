package pt.ulisboa.tecnico.psia.g2.users;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

// Follows, and the lists of people built on them. Native SQL: the overlap between two profiles
// is computed on their jsonb lists inside Postgres.
@ApplicationScoped
public class SocialRepository {

    private static final TypeReference<List<String>> STRINGS = new TypeReference<>() {
    };

    // One row per person; :me is the caller. Each list below appends its own joins and filter.
    private static final String PEOPLE = """
            select u.id, u.username, p.display_name,
                   cast(p.favorite_genres as text), cast(p.favorite_directors as text),
                   cast(p.favorite_movies as text),
                   exists (select 1 from follows f where f.follower_id = :me and f.followee_id = u.id)
            from users u
            left join profiles p on p.user_id = u.id
            """;

    // How many entries of p's list are also in mine, ignoring case.
    private static final String SHARED = """
            (select count(*) from jsonb_array_elements_text(p.%1$s) theirs
              where lower(theirs) in (select lower(mine) from jsonb_array_elements_text(me.%1$s) mine))
            """;

    // A favorite film in common says more about taste than a genre in common, so it counts double.
    private static final String SCORE = SHARED.formatted("favorite_genres")
            + " + 2 * " + SHARED.formatted("favorite_movies")
            + " + " + SHARED.formatted("favorite_directors");

    @Inject
    EntityManager em;

    @Inject
    ObjectMapper mapper;

    // People the caller does not follow yet whose profile overlaps with theirs, closest first.
    // Empty when the caller has no profile.
    public List<Person> suggestions(UUID me, int limit) {
        return people(me, PEOPLE + """
                join profiles me on me.user_id = :me
                where u.id <> :me
                  and not exists (select 1 from follows f where f.follower_id = :me and f.followee_id = u.id)
                  and (%1$s) > 0
                order by (%1$s) desc, u.username
                limit :limit
                """.formatted(SCORE), Map.of("limit", limit));
    }

    // People whose username or display name contains the text, the caller excluded.
    public List<Person> search(UUID me, String text, int limit) {
        String pattern = "%" + text.toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return people(me, PEOPLE + """
                where u.id <> :me and (u.username like :pattern or lower(p.display_name) like :pattern)
                order by u.username
                limit :limit
                """, Map.of("pattern", pattern, "limit", limit));
    }

    public List<Person> followers(UUID me, UUID target, int limit) {
        return people(me, PEOPLE + """
                join follows fo on fo.follower_id = u.id
                where fo.followee_id = :target
                order by fo.created_at desc, u.username
                limit :limit
                """, Map.of("target", target, "limit", limit));
    }

    public List<Person> following(UUID me, UUID target, int limit) {
        return people(me, PEOPLE + """
                join follows fo on fo.followee_id = u.id
                where fo.follower_id = :target
                order by fo.created_at desc, u.username
                limit :limit
                """, Map.of("target", target, "limit", limit));
    }

    public SocialStats stats(UUID me, UUID target) {
        Object[] row = (Object[]) em.createNativeQuery("""
                select (select count(*) from follows where followee_id = :target),
                       (select count(*) from follows where follower_id = :target),
                       exists (select 1 from follows where follower_id = :me and followee_id = :target)
                """).setParameter("me", me).setParameter("target", target).getSingleResult();
        return new SocialStats(((Number) row[0]).longValue(), ((Number) row[1]).longValue(), (Boolean) row[2]);
    }

    // following someone twice is not an error
    public void follow(UUID me, UUID target) {
        em.createNativeQuery("""
                insert into follows (follower_id, followee_id) values (:me, :target)
                on conflict do nothing
                """).setParameter("me", me).setParameter("target", target).executeUpdate();
    }

    public void unfollow(UUID me, UUID target) {
        em.createNativeQuery("delete from follows where follower_id = :me and followee_id = :target")
                .setParameter("me", me).setParameter("target", target).executeUpdate();
    }

    @SuppressWarnings("unchecked")
    private List<Person> people(UUID me, String sql, Map<String, Object> params) {
        Query query = em.createNativeQuery(sql).setParameter("me", me);
        params.forEach(query::setParameter);
        Profile mine = Profile.findById(me);
        return ((List<Object[]>) query.getResultList()).stream().map(row -> person(row, mine)).toList();
    }

    private Person person(Object[] row, Profile mine) {
        return new Person(row[0].toString(), (String) row[1], (String) row[2], (Boolean) row[6],
                shared(row[3], mine == null ? null : mine.favoriteGenres),
                shared(row[4], mine == null ? null : mine.favoriteDirectors),
                shared(row[5], mine == null ? null : mine.favoriteMovies));
    }

    // their entries that are also in mine, ignoring case, written their way
    private List<String> shared(Object theirJson, List<String> mine) {
        if (theirJson == null || mine == null || mine.isEmpty()) {
            return List.of();
        }
        Set<String> mineLower = mine.stream().map(v -> v.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        try {
            return mapper.readValue((String) theirJson, STRINGS).stream()
                    .filter(v -> mineLower.contains(v.toLowerCase(Locale.ROOT)))
                    .toList();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not read a profile list", e);
        }
    }
}
