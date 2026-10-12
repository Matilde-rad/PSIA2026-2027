package pt.ulisboa.tecnico.psia.g2.users;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// Watchlists and watched movies (user_movies), and the activity lists read from them.
@ApplicationScoped
public class WatchlistRepository {

    // One row per user and movie, so an activity is the latest state of that pair.
    private static final String ACTIVITY = """
            select u.id, u.username, p.display_name, m.movie_id, m.status, m.updated_at
            from user_movies m
            join users u on u.id = m.user_id
            left join profiles p on p.user_id = u.id
            """;

    @Inject
    EntityManager em;

    @SuppressWarnings("unchecked")
    public List<MovieEntry> list(UUID me) {
        List<Object[]> rows = em.createNativeQuery("""
                select movie_id, status, updated_at from user_movies
                where user_id = :me
                order by updated_at desc, movie_id
                """).setParameter("me", me).getResultList();
        return rows.stream().map(WatchlistRepository::entry).toList();
    }

    @SuppressWarnings("unchecked")
    public Optional<MovieEntry> find(UUID me, String movieId) {
        List<Object[]> rows = em.createNativeQuery("""
                select movie_id, status, updated_at from user_movies
                where user_id = :me and movie_id = :movie
                """).setParameter("me", me).setParameter("movie", movieId).getResultList();
        return rows.stream().map(WatchlistRepository::entry).findFirst();
    }

    // adds the movie or changes its status; either way it becomes the user's latest activity
    public void save(UUID me, String movieId, String status) {
        em.createNativeQuery("""
                insert into user_movies (user_id, movie_id, status) values (:me, :movie, :status)
                on conflict (user_id, movie_id) do update set status = excluded.status, updated_at = now()
                """).setParameter("me", me).setParameter("movie", movieId).setParameter("status", status)
                .executeUpdate();
    }

    public void remove(UUID me, String movieId) {
        em.createNativeQuery("delete from user_movies where user_id = :me and movie_id = :movie")
                .setParameter("me", me).setParameter("movie", movieId).executeUpdate();
    }

    // what the people the caller follows did, newest first
    public List<Activity> feed(UUID me, int limit) {
        return activities(ACTIVITY + """
                join follows f on f.followee_id = m.user_id
                where f.follower_id = :me
                order by m.updated_at desc, m.movie_id
                limit :limit
                """, Map.of("me", me, "limit", limit));
    }

    public List<Activity> of(UUID user, int limit) {
        return activities(ACTIVITY + """
                where m.user_id = :user
                order by m.updated_at desc, m.movie_id
                limit :limit
                """, Map.of("user", user, "limit", limit));
    }

    // every watched movie of every user, with the verdict
    @SuppressWarnings("unchecked")
    public List<WatchedMovie> watched() {
        List<Object[]> rows = em.createNativeQuery("""
                select user_id, movie_id, status, updated_at from user_movies
                where status <> 'to_watch'
                order by user_id, updated_at desc
                """).getResultList();
        return rows.stream()
                .map(r -> new WatchedMovie(r[0].toString(), (String) r[1], "liked".equals(r[2]), instant(r[3])))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<Activity> activities(String sql, Map<String, Object> params) {
        Query query = em.createNativeQuery(sql);
        params.forEach(query::setParameter);
        return ((List<Object[]>) query.getResultList()).stream()
                .map(r -> new Activity(r[0].toString(), (String) r[1], (String) r[2], (String) r[3], (String) r[4],
                        instant(r[5])))
                .toList();
    }

    private static MovieEntry entry(Object[] row) {
        return new MovieEntry((String) row[0], (String) row[1], instant(row[2]));
    }

    // a native query hands a timestamptz back as whichever of these the driver and Hibernate agree on
    private static Instant instant(Object value) {
        return switch (value) {
            case Instant i -> i;
            case OffsetDateTime o -> o.toInstant();
            case Timestamp t -> t.toInstant();
            default -> throw new IllegalStateException("Unexpected timestamp type " + value.getClass());
        };
    }
}
