package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class GatherTask extends DelayedTask {
	//@formatter:off
	public enum GatherType {
		MEAT( EnumTemplates.GAME_HOME_SHORTCUTS_MEAT, EnumTemplates.GAME_HOME_SHORTCUTS_FARM_MEAT, EnumConfigurationKey.GATHER_MEAT_LEVEL_INT), 
		WOOD( EnumTemplates.GAME_HOME_SHORTCUTS_WOOD, EnumTemplates.GAME_HOME_SHORTCUTS_FARM_WOOD, EnumConfigurationKey.GATHER_WOOD_LEVEL_INT), 
		COAL( EnumTemplates.GAME_HOME_SHORTCUTS_COAL, EnumTemplates.GAME_HOME_SHORTCUTS_FARM_COAL, EnumConfigurationKey.GATHER_COAL_LEVEL_INT), 
		IRON( EnumTemplates.GAME_HOME_SHORTCUTS_IRON, EnumTemplates.GAME_HOME_SHORTCUTS_FARM_IRON, EnumConfigurationKey.GATHER_IRON_LEVEL_INT);
		
		
		EnumTemplates template;
		EnumTemplates tile;
		EnumConfigurationKey level;
		
		GatherType(EnumTemplates enumTemplate,EnumTemplates tile, EnumConfigurationKey level) {
		this.template = enumTemplate;
		this.tile = tile;
		this.level = level;
		}
		
		public String getTemplate() {
            return template.getTemplate();
		}
		
		public String getTile() {
            return tile.getTemplate();
		}
		
		public EnumConfigurationKey getConfig() {
            return level;
        }

		
	}
	
	private DTOPoint[][] queues = {
			{new DTOPoint(10, 342),new DTOPoint(435, 407), new DTOPoint(152, 378)},
			{new DTOPoint(10, 415),new DTOPoint(435, 480), new DTOPoint(152, 451)},
			{new DTOPoint(10, 488),new DTOPoint(435, 553), new DTOPoint(152, 524)},
			{new DTOPoint(10, 561),new DTOPoint(435, 626), new DTOPoint(152, 597)},
			{new DTOPoint(10, 634),new DTOPoint(435, 699), new DTOPoint(152, 670)},
			{new DTOPoint(10, 707),new DTOPoint(435, 772), new DTOPoint(152, 743)},
			};
	
	//@formatter:on

	private final GatherType gatherType;
	private final ServScheduler servScheduler = ServScheduler.getServices();
	private final ServLogs servLogs = ServLogs.getServices();
	private final EmulatorManager emuManager = EmulatorManager.getInstance();

	public GatherTask(DTOProfiles profile, TpDailyTaskEnum tpTask, GatherType gatherType) {
		super(profile, tpTask);
		this.gatherType = gatherType;
	}

	@Override
	protected void execute() {
		DTOImageSearchResult homeResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90);
		DTOImageSearchResult worldResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720, 1280, 90);

		if (homeResult.isFound() || worldResult.isFound()) {
			if (worldResult.isFound()) {
				emuManager.tapAtPoint(EMULATOR_NUMBER, worldResult.getPoint());
				sleepTask(4000);
			}

			// verificar marchas activas
			emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(2, 550));
			sleepTask(1000);

			emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(340, 265));
			sleepTask(1000);
			servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "looking for " + gatherType);
			DTOImageSearchResult resource = emuManager.searchTemplate(EMULATOR_NUMBER, gatherType.getTemplate(), 10, 342, (425 - 10), 772, 90);

			if (resource.isFound()) {
				servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Resource found, getting remaining time");
				int index = obtenerIndice(resource.getPoint());
				if (index != -1) {
					try {
						String time = emuManager.ocrRegionText(EMULATOR_NUMBER, queues[index][2], new DTOPoint(queues[index][2].getX() + 140, queues[index][2].getY() + 19));
						this.reschedule(parseRemaining(time).plusMinutes(5));
					} catch (Exception e) {

					}
				}
				emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(110, 270));
				emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(464, 551));
			} else {
				emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(110, 270));
				emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(464, 551));
				homeResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90);
				if (homeResult.isFound()) {
					emuManager.tapAtPoint(EMULATOR_NUMBER, homeResult.getPoint());
					sleepTask(4000);
				}
				// debo mandar un escuadro a recojer recursos
				// ir a la lupa
				emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(25, 850), new DTOPoint(67, 898));
				sleepTask(2000);

				// hacer swhipe a la izquierda
				emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(678, 913), new DTOPoint(40, 913));
				sleepTask(300);
				DTOImageSearchResult tile = emuManager.searchTemplate(EMULATOR_NUMBER, gatherType.getTile(), 0, 0, 720, 1280, 90);

				if (tile.isFound()) {
					emuManager.tapAtPoint(EMULATOR_NUMBER, tile.getPoint());
					// regresar al nivel 1
					sleepTask(1000);
					emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(435, 1052), new DTOPoint(40, 1052));
					sleepTask(300);
					emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(487, 1055), new DTOPoint(487, 1055), (profile.getConfig(gatherType.getConfig(), Integer.class) - 1), 50);

					DTOImageSearchResult tick = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_SHORTCUTS_FARM_TICK.getTemplate(), 0, 0, 720, 1280, 90);
					if (!tick.isFound()) {
						emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(183, 1140));

					}

					// click Search
					emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(301, 1200), new DTOPoint(412, 1229));
					sleepTask(3000);
					// click gather

					DTOImageSearchResult gather = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_SHORTCUTS_FARM_GATHER.getTemplate(), 0, 0, 720, 1280, 90);
					if (gather.isFound()) {
						emuManager.tapAtPoint(EMULATOR_NUMBER, gather.getPoint());
						sleepTask(3000);

						// verificar el heroe y remover restantes
						emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(171, 430));
						sleepTask(1000);
						// remover restantes
						emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(560, 348));

						sleepTask(1000);
						DTOImageSearchResult remove = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.RALLY_REMOVE_HERO_BUTTON.getTemplate(), 0, 0, 720, 1280, 90);
						if (remove.isFound()) {
							emuManager.tapAtPoint(EMULATOR_NUMBER, remove.getPoint());
							sleepTask(500);
						}

						emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(362, 348));
						sleepTask(500);
						remove = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.RALLY_REMOVE_HERO_BUTTON.getTemplate(), 0, 0, 720, 1280, 90);
						if (remove.isFound()) {
							emuManager.tapAtPoint(EMULATOR_NUMBER, remove.getPoint());
							sleepTask(1000);
						}
						emuManager.tapBackButton(EMULATOR_NUMBER);
						// falta fverificar si esta el heroe adecuado
						sleepTask(500);

						// click equalize
						emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(195, 1190));
						sleepTask(500);

						// click gather
						DTOImageSearchResult gatherButton = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.RALLY_GATHER_BUTTON.getTemplate(), 0, 0, 720, 1280, 90);
						if (gatherButton.isFound()) {
							emuManager.tapAtPoint(EMULATOR_NUMBER, gatherButton.getPoint());
							sleepTask(3000);
							// verificar si ya hay un marcha en curso
							DTOImageSearchResult march = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.RALLY_GATHER_ALREADY_MARCHING.getTemplate(), 0, 0, 720, 1280, 90);
							if (march.isFound()) {
								emuManager.tapBackButton(EMULATOR_NUMBER);
								emuManager.tapBackButton(EMULATOR_NUMBER);
								reschedule(LocalDateTime.now());
								servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "March already gathering");
							} else {
								servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "March started");
								reschedule(LocalDateTime.now().plusMinutes(5));
							}
						}

					} else {
						emuManager.tapBackButton(EMULATOR_NUMBER);
						reschedule(LocalDateTime.now().plusMinutes(5));
						servLogs.appendLog(EnumTpMessageSeverity.ERROR, taskName, profile.getName(), "Tile not found");
					}
				} else {
					emuManager.tapBackButton(EMULATOR_NUMBER);
					reschedule(LocalDateTime.now().plusMinutes(5));
					servLogs.appendLog(EnumTpMessageSeverity.ERROR, taskName, profile.getName(), "resource not found");
				}
			}

		} else {
			emuManager.tapBackButton(EMULATOR_NUMBER);
			reschedule(LocalDateTime.now());

		}

	}

	public int obtenerIndice(DTOPoint punto) {
		for (int i = 0; i < queues.length; i++) {
			// Obtener los límites del rango (para asegurar el orden correcto, usamos Math.min y Math.max)
			int minX = Math.min(queues[i][0].getX(), queues[i][1].getX());
			int maxX = Math.max(queues[i][0].getX(), queues[i][1].getX());
			int minY = Math.min(queues[i][0].getY(), queues[i][1].getY());
			int maxY = Math.max(queues[i][0].getY(), queues[i][1].getY());

			// Verificar si el punto está dentro de los límites
			if (punto.getX() >= minX && punto.getX() <= maxX && punto.getY() >= minY && punto.getY() <= maxY) {
				return i; // Retorna el índice del par encontrado
			}
		}
		return -1; // Retorna -1 si el punto no se encuentra en ninguno de los rangos
	}

	public static LocalDateTime parseRemaining(String timeStr) {
		if (timeStr == null) {
			return LocalDateTime.now();
		}
		// Elimina saltos de línea y espacios extra
		timeStr = timeStr.replaceAll("\\r?\\n", "").trim();

		// Se espera el formato hh:mm:ss
		String[] parts = timeStr.split(":");
		if (parts.length != 3) {
			return LocalDateTime.now();
		}

		try {
			int hours = Integer.parseInt(parts[0].trim());
			int minutes = Integer.parseInt(parts[1].trim());
			int seconds = Integer.parseInt(parts[2].trim());

			LocalDateTime now = LocalDateTime.now();
			// Se suma el tiempo parseado al instante actual
			return now.plusHours(hours).plusMinutes(minutes).plusSeconds(seconds);
		} catch (NumberFormatException e) {
			// Si ocurre algún error durante el parseo, se retorna LocalDateTime.now()
			return LocalDateTime.now().plusMinutes(3);
		}
	}

}
