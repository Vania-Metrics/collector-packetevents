package fr.samflix.vaniametrics.module.packetevents;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;

import fr.samflix.vaniametrics.api.Collector;
import fr.samflix.vaniametrics.api.Config;
import fr.samflix.vaniametrics.api.Counter;
import fr.samflix.vaniametrics.api.Gauge;
import fr.samflix.vaniametrics.api.MetricRegistry;
import fr.samflix.vaniametrics.api.Platform;

/**
 * The packet counter.
 *
 * <p>The work happens in the listener, not in collect: every packet increments a counter, and the
 * scrape only reads. This is the reverse of the other collectors, and it's the only tenable way
 * to measure something that happens ten thousand times a second.
 *
 * <p>Packet size is not published, and that's not an oversight: PacketEvents' API exposes no byte
 * length on its events — checked on {@code ProtocolPacketEvent} and {@code PacketReceiveEvent}.
 * Measuring it would mean reading Netty's {@code ByteBuf} by reflection, on the network thread,
 * for every packet. Not worth it.
 */
public final class PacketEventsCollector implements Collector {

	private final Platform platform;
	private final Set<String> trackedTypes;

	private Counter packets;
	private Gauge byVersion;
	private PacketListenerAbstract listener;

	public PacketEventsCollector(Platform platform, Config config) {
		this.platform = platform;
		// Empty by default: count the total and nothing else until someone says which types
		// they care about. An empty whitelist beats four hundred series.
		String list = config.getString("collector.packets.types", "");
		this.trackedTypes = list.isBlank()
				? Set.of()
				: new HashSet<>(Arrays.asList(list.toLowerCase(Locale.ROOT).split("\\s*,\\s*")));
	}

	@Override
	public String name() {
		return "packets";
	}

	@Override
	public String source() {
		return "packetevents";
	}

	@Override
	public void declare(MetricRegistry r) {
		packets = r.counter("network_packets_total",
				"Packets crossing the server. direction = in|out. packet_type is bounded by "
						+ "collector.packets.types; everything else falls into \"other\".",
				"direction", "packet_type");
		byVersion = r.gauge("network_players_by_protocol",
				"Players by protocol version. PacketEvents is the only one that knows it: "
						+ "org.bukkit.entity.Player doesn't expose it.",
				"protocol", "version_name");

		listener = new PacketListenerAbstract(PacketListenerPriority.MONITOR) {
			@Override
			public void onPacketReceive(PacketReceiveEvent e) {
				packets.inc("in", type(e.getPacketType().getName()));
			}

			@Override
			public void onPacketSend(PacketSendEvent e) {
				packets.inc("out", type(e.getPacketType().getName()));
			}
		};
		PacketEvents.getAPI().getEventManager().registerListener(listener);
		platform.info("packets collector — listener attached, "
				+ (trackedTypes.isEmpty() ? "total only" : trackedTypes.size() + " type(s) tracked"));
	}

	@Override
	public void collect(MetricRegistry r) {
		byVersion.clear();
		Map<String, Integer> count = new HashMap<>();
		Map<String, String> names = new HashMap<>();
		for (User u : PacketEvents.getAPI().getProtocolManager().getUsers()) {
			ClientVersion v = u.getClientVersion();
			if (v == null) {
				continue;
			}
			String protocol = String.valueOf(v.getProtocolVersion());
			count.merge(protocol, 1, Integer::sum);
			names.put(protocol, v.name().toLowerCase(Locale.ROOT));
		}
		count.forEach((p, n) -> byVersion.set(n, p, names.getOrDefault(p, "unknown")));
	}

	@Override
	public void close() {
		if (listener != null) {
			PacketEvents.getAPI().getEventManager().unregisterListener(listener);
		}
	}

	private String type(String name) {
		if (trackedTypes.isEmpty()) {
			return "all";
		}
		String n = name.toLowerCase(Locale.ROOT);
		return trackedTypes.contains(n) ? n : "other";
	}
}
