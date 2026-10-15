package pt.ulisboa.tecnico.psia.g2.edge;

public record Movie(String movieId, String title, Integer year, String posterUrl, String bannerUrl) {

    static Movie from(MoviesClient.CatalogMovie m) {
        return new Movie(m.id(), m.title(), m.year(), m.posterUrl(), m.bannerUrl());
    }
}
