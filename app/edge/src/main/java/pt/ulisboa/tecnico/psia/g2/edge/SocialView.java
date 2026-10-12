package pt.ulisboa.tecnico.psia.g2.edge;

import java.util.List;

// hasProfile false means the suggestions are empty because there is nothing to match on yet
public record SocialView(boolean hasProfile, List<PersonView> suggestions) {
}
