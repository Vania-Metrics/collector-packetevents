package fr.samflix.vaniametrics.module.packetevents;

import net.md_5.bungee.api.plugin.Plugin;

import fr.samflix.vaniametrics.api.Collector;
import fr.samflix.vaniametrics.api.VaniaMetrics;
import fr.samflix.vaniametrics.api.VaniaMetricsProvider;

/**
 * PacketEvents — network traffic, which the server API doesn't show at all. — BungeeCord and Waterfall side.
 *
 * <p>Same collector, a third entry point in the same jar: BungeeCord reads bungee.yml before
 * plugin.yml, which is Bukkit's, and loads this class; Velocity reads velocity-plugin.json.
 * {@code depends} in bungee.yml makes BungeeCord enable the core first.
 */
public final class PacketEventsBungee extends Plugin {

	private Collector collector;

	@Override
	public void onEnable() {
		VaniaMetrics metrics = VaniaMetricsProvider.get();
		collector = new PacketEventsCollector(metrics.platform(), metrics.config());
		metrics.register(collector);
		getLogger().info("packetevents collector registered");
	}

	@Override
	public void onDisable() {
		if (collector != null) {
			VaniaMetricsProvider.find().ifPresent(m -> m.unregister(collector));
		}
	}
}
