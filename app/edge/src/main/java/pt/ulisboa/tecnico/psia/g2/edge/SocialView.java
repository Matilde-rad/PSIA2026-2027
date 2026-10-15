package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

public record SocialView(boolean hasProfile, List<PersonView> suggestions) {
}
