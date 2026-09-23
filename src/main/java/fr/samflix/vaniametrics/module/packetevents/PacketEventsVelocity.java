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
 * PacketEvents — network traffic, which the server API doesn't show at all. — Velocity side.
 *
 * <p>Same collector, different entry point. Both classes live in the SAME jar: Bukkit reads
 * plugin.yml and loads the Paper variant, Velocity reads velocity-plugin.json and loads this one.
 * Each ignores the other, which is never loaded.
 */
@Plugin(
		id = "vaniametrics-packetevents",
		name = "VaniaMetrics PacketEvents",
		version = Version.VALUE,
		description = "PacketEvents — network traffic, which the server API doesn't show at all.",
		authors = {"mc-vania"},
		dependencies = {
			@Dependency(id = "vaniametrics"),
			@Dependency(id = "packetevents")
		})
public final class PacketEventsVelocity {

	private final Logger logger;
	private Collector collector;

	@Inject
	public PacketEventsVelocity(Logger logger) {
		this.logger = logger;
	}

	@Subscribe
	public void onInit(ProxyInitializeEvent e) {
		VaniaMetrics metrics = VaniaMetricsProvider.get();
		collector = new PacketEventsCollector(metrics.platform(), metrics.config());
		metrics.register(collector);
		logger.info("packetevents collector registered");
	}

	@Subscribe
	public void onShutdown(ProxyShutdownEvent e) {
		if (collector != null) {
			VaniaMetricsProvider.find().ifPresent(m -> m.unregister(collector));
		}
	}
}
