package pt.ulisboa.tecnico.psia.g2.edge;

import jakarta.ws.rs.NotFoundException;

final class MovieIds {

    private MovieIds() {
    }

    // catalog ids are numbers; anything else cannot be a movie, so it is a 404 without asking upstream
    static String valid(String id) {
        if (id == null || !id.matches("[0-9]{1,9}")) {
            throw new NotFoundException();
        }
        return id;
    }
}
