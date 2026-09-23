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
 * Le compteur de paquets.
 *
 * <p>LE TRAVAIL SE FAIT DANS L'ÉCOUTEUR, PAS DANS LE RELEVÉ : chaque paquet incrémente un
 * compteur, et le scrape ne fait que lire. C'est l'inverse des autres collecteurs, et c'est la
 * seule façon tenable de mesurer quelque chose qui arrive dix mille fois par seconde.
 *
 * <p>LA TAILLE DES PAQUETS N'EST PAS PUBLIÉE, et ce n'est pas un oubli : l'API de PacketEvents
 * n'expose aucune longueur d'octets sur ses événements — vérifié sur {@code ProtocolPacketEvent}
 * et {@code PacketReceiveEvent}. La mesurer voudrait dire lire le {@code ByteBuf} de Netty par
 * réflexion, sur le fil réseau, pour chaque paquet. Le jeu n'en vaut pas la chandelle.
 */
public final class PacketEventsCollector implements Collector {

	private final Platform plateforme;
	private final Set<String> typesSuivis;

	private Counter paquets;
	private Gauge parVersion;
	private PacketListenerAbstract ecouteur;

	public PacketEventsCollector(Platform plateforme, Config config) {
		this.plateforme = plateforme;
		// Vide par défaut : on compte le total et rien d'autre tant que personne n'a dit quels
		// types l'intéressent. Une liste blanche vide vaut mieux que quatre cents séries.
		String liste = config.texte("collector.packets.types", "");
		this.typesSuivis = liste.isBlank()
				? Set.of()
				: new HashSet<>(Arrays.asList(liste.toLowerCase(Locale.ROOT).split("\\s*,\\s*")));
	}

	@Override
	public String nom() {
		return "packets";
	}

	@Override
	public String origine() {
		return "packetevents";
	}

	@Override
	public void declarer(MetricRegistry r) {
		paquets = r.counter("network_packets_total",
				"Paquets traversant le serveur. direction = in|out. packet_type est borné par "
						+ "collector.packets.types ; tout le reste tombe dans « other ».",
				"direction", "packet_type");
		parVersion = r.gauge("network_players_by_protocol",
				"Joueurs par version de protocole. PacketEvents est le seul à la connaître : "
						+ "org.bukkit.entity.Player ne l'expose pas.",
				"protocol", "version_name");

		ecouteur = new PacketListenerAbstract(PacketListenerPriority.MONITOR) {
			@Override
			public void onPacketReceive(PacketReceiveEvent e) {
				paquets.inc("in", type(e.getPacketType().getName()));
			}

			@Override
			public void onPacketSend(PacketSendEvent e) {
				paquets.inc("out", type(e.getPacketType().getName()));
			}
		};
		PacketEvents.getAPI().getEventManager().registerListener(ecouteur);
		plateforme.info("collecteur packets — écouteur branché, "
				+ (typesSuivis.isEmpty() ? "total seul" : typesSuivis.size() + " type(s) suivi(s)"));
	}

	@Override
	public void relever(MetricRegistry r) {
		parVersion.clear();
		Map<String, Integer> compte = new HashMap<>();
		Map<String, String> noms = new HashMap<>();
		for (User u : PacketEvents.getAPI().getProtocolManager().getUsers()) {
			ClientVersion v = u.getClientVersion();
			if (v == null) {
				continue;
			}
			String protocole = String.valueOf(v.getProtocolVersion());
			compte.merge(protocole, 1, Integer::sum);
			noms.put(protocole, v.name().toLowerCase(Locale.ROOT));
		}
		compte.forEach((p, n) -> parVersion.set(n, p, noms.getOrDefault(p, "unknown")));
	}

	@Override
	public void fermer() {
		if (ecouteur != null) {
			PacketEvents.getAPI().getEventManager().unregisterListener(ecouteur);
		}
	}

	private String type(String nom) {
		if (typesSuivis.isEmpty()) {
			return "all";
		}
		String n = nom.toLowerCase(Locale.ROOT);
		return typesSuivis.contains(n) ? n : "other";
	}
}
