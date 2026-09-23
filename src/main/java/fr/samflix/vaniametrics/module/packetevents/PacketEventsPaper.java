package fr.samflix.vaniametrics.module.packetevents;

import org.bukkit.plugin.java.JavaPlugin;

import fr.samflix.vaniametrics.api.Collector;
import fr.samflix.vaniametrics.api.VaniaMetrics;
import fr.samflix.vaniametrics.api.VaniaMetricsProvider;

/**
 * PacketEvents — network traffic, which the server API doesn't show at all.
 *
 * <p>Its interceptor runs on the network thread, for every packet. It only does an atomic
 * increment there, but that's a decision worth making: this jar is only installed if wanted.
 *
 * <p>Its plugin.yml declares {@code depend: [VaniaMetrics, packetevents]}: both are required, and
 * declaring it lets Bukkit guarantee load order instead of hoping for it. Removing this jar
 * removes this integration and nothing else — that's the whole point of one jar per integration.
 */
public final class PacketEventsPaper extends JavaPlugin {

	private Collector collector;

	@Override
	public void onEnable() {
		VaniaMetrics metrics = VaniaMetricsProvider.get();
		collector = new PacketEventsCollector(metrics.platform(), metrics.config());
		metrics.register(collector);
	}

	@Override
	public void onDisable() {
		if (collector != null) {
			VaniaMetricsProvider.find().ifPresent(m -> m.unregister(collector));
		}
	}
}
