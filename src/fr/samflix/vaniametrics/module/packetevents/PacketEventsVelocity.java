package fr.samflix.vaniametrics.module.packetevents;

import org.slf4j.Logger;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;

import fr.samflix.vaniametrics.api.Collector;
import fr.samflix.vaniametrics.api.VaniaMetrics;
import fr.samflix.vaniametrics.api.VaniaMetricsProvider;
import fr.samflix.vaniametrics.api.Version;

/**
 * PacketEvents — le trafic réseau, que l'API du serveur ne montre pas du tout. — côté Velocity.
 *
 * <p>Même collecteur, autre point d'entrée. Les deux classes cohabitent dans le MÊME jar : Bukkit
 * lit plugin.yml et charge la variante Paper, Velocity lit velocity-plugin.json et charge
 * celle-ci. Chacun ignore l'autre, qui n'est jamais chargée.
 */
@Plugin(
		id = "vaniametrics-packetevents",
		name = "VaniaMetrics PacketEvents",
		version = Version.VALEUR,
		description = "PacketEvents — le trafic réseau, que l'API du serveur ne montre pas du tout.",
		authors = {"mc-vania"},
		dependencies = {
			@Dependency(id = "vaniametrics"),
			@Dependency(id = "packetevents")
		})
public final class PacketEventsVelocity {

	private final Logger journal;
	private Collector collecteur;

	@Inject
	public PacketEventsVelocity(Logger journal) {
		this.journal = journal;
	}

	@Subscribe
	public void onInit(ProxyInitializeEvent e) {
		VaniaMetrics metriques = VaniaMetricsProvider.get();
		collecteur = new PacketEventsCollector(metriques.plateforme(), metriques.config());
		metriques.enregistrer(collecteur);
		journal.info("collecteur packetevents enregistré");
	}

	@Subscribe
	public void onShutdown(ProxyShutdownEvent e) {
		if (collecteur != null) {
			VaniaMetricsProvider.chercher().ifPresent(m -> m.retirer(collecteur));
		}
	}
}
