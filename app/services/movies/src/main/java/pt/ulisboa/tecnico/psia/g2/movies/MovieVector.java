package pt.ulisboa.tecnico.psia.g2.movies;

// A movie's embedding, for services that compute with it (recommendations steers its search with them).
public record MovieVector(String id, float[] embedding) {
}
