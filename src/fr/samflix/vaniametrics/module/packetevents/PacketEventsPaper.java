package fr.samflix.vaniametrics.module.packetevents;

import org.bukkit.plugin.java.JavaPlugin;

import fr.samflix.vaniametrics.api.Collector;
import fr.samflix.vaniametrics.api.VaniaMetrics;
import fr.samflix.vaniametrics.api.VaniaMetricsProvider;

/**
 * PacketEvents — le trafic réseau, que l'API du serveur ne montre pas du tout.
 *
 * <p>Son intercepteur tourne sur le fil réseau, pour CHAQUE paquet. Il n'y fait qu'un incrément atomique, mais c'est une décision qui se prend : ce jar ne s'installe que si on le veut.
 *
 * <p>SON plugin.yml DÉCLARE {@code depend: [VaniaMetrics, packetevents]} : les deux sont
 * indispensables, et le déclarer laisse Bukkit garantir l'ordre de chargement plutôt que de
 * l'espérer. Retirer ce jar retire cette intégration et RIEN D'AUTRE — c'est tout l'intérêt d'un
 * jar par intégration.
 */
public final class PacketEventsPaper extends JavaPlugin {

	private Collector collecteur;

	@Override
	public void onEnable() {
		VaniaMetrics metriques = VaniaMetricsProvider.get();
		collecteur = new PacketEventsCollector(metriques.plateforme(), metriques.config());
		metriques.enregistrer(collecteur);
	}

	@Override
	public void onDisable() {
		if (collecteur != null) {
			VaniaMetricsProvider.chercher().ifPresent(m -> m.retirer(collecteur));
		}
	}
}
