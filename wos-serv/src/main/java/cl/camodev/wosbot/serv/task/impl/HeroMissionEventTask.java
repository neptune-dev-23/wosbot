package cl.camodev.wosbot.serv.task.impl;

import cl.camodev.wosbot.almac.entity.DailyTask;
import cl.camodev.wosbot.almac.repo.DailyTaskRepository;
import cl.camodev.wosbot.almac.repo.IDailyTaskRepository;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class HeroMissionEventTask extends DelayedTask {

    public HeroMissionEventTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
        super(profile, tpTask);
    }

    @Override
    protected void execute() {
        ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(),
                "Executing Hero's Mission task");
    }

}
