package pt.ulisboa.tecnico.psia.g2.users;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/test/boom")
public class TestFailureResource {

    @GET
    public String boom() {
        throw new IllegalStateException("secret sql detail");
    }
}
