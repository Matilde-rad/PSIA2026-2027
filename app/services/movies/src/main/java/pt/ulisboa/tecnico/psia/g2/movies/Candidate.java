package pt.ulisboa.tecnico.psia.g2.movies;

import java.util.List;

public record Candidate(String id, String title, Integer year, List<String> genres, int ratingCount,
        String posterUrl, String bannerUrl) {
}
