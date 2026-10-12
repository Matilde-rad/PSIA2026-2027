package pt.ulisboa.tecnico.psia.g2.edge;

import org.jboss.resteasy.reactive.ClientWebApplicationException;

final class Upstream {

    private Upstream() {
    }

    static boolean notFound(Throwable t) {
        return t instanceof ClientWebApplicationException e && e.getResponse().getStatus() == 404;
    }
}
