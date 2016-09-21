package adf.sample.control;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.information.MessagePoliceForce;
import adf.agent.communication.standard.bundle.information.MessageRoad;
import adf.agent.communication.standard.bundle.topdown.CommandPolice;
import adf.agent.communication.standard.bundle.topdown.MessageReport;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.control.ControlPolice;
import rescuecore2.standard.entities.Blockade;
import rescuecore2.standard.entities.Road;
import rescuecore2.standard.entities.StandardEntity;
import rescuecore2.standard.entities.StandardEntityURN;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

public class SampleControlPolice extends ControlPolice {

    private Map<EntityID, EntityID> agentTaskMap;
    private Set<EntityID> request;

    @Override
    public void initialize(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, MessageManager messageManager, DevelopData developData) {
        this.agentTaskMap = new HashMap<>();
        this.request = new HashSet<>();
    }

    @Override
    public void think(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, MessageManager messageManager, DevelopData developData) {
        this.updateAgentTaskInfo(worldInfo, messageManager);
        this.collectTask(messageManager);
        this.sendCommand(worldInfo, messageManager);
    }

    private void sendCommand(WorldInfo worldInfo, MessageManager messageManager) {
        if(this.request.isEmpty()) {
            return;
        }
        List<StandardEntity> agents = new ArrayList<>(worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE));
        for(EntityID id : this.agentTaskMap.keySet()) {
            agents.remove(worldInfo.getEntity(id));
        }
        for(EntityID id : this.request) {
            if(agents.isEmpty()) {
                return;
            }
            StandardEntity entity = worldInfo.getEntity(id);
            if(entity instanceof Road) {
                agents.sort(new DistanceSorter(worldInfo, entity));
                EntityID agentID = agents.get(0).getID();
                messageManager.addMessage(new CommandPolice(
                        true,
                        agentID,
                        entity.getID(),
                        CommandPolice.ACTION_CLEAR
                ));
                this.agentTaskMap.put(agentID, entity.getID());
                agents.remove(0);
            } else if(entity.getStandardURN() == StandardEntityURN.BLOCKADE) {
                entity = worldInfo.getEntity(((Blockade)entity).getPosition());
                agents.sort(new DistanceSorter(worldInfo, entity));
                EntityID agentID = agents.get(0).getID();
                messageManager.addMessage(new CommandPolice(
                        true,
                        agentID,
                        entity.getID(),
                        CommandPolice.ACTION_CLEAR
                ));
                this.agentTaskMap.put(agentID, entity.getID());
                agents.remove(0);
            }
        }

    }

    private void updateAgentTaskInfo(WorldInfo worldInfo, MessageManager messageManager) {
        Collection<EntityID> agentIDs = worldInfo.getEntityIDsOfType(StandardEntityURN.POLICE_FORCE);
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessagePoliceForce.class)) {
            MessagePoliceForce mpf = (MessagePoliceForce) message;
            if (mpf.getSenderID().getValue() == mpf.getAgentID().getValue()) {
                EntityID target = this.agentTaskMap.get(mpf.getAgentID());
                if(target != null) {
                    if(target.getValue() == mpf.getPosition().getValue()) {
                        StandardEntity entity = worldInfo.getEntity(target);
                        if(entity instanceof Road) {
                            Road road = (Road)entity;
                            if(road.isBlockadesDefined()) {
                                if(road.getBlockades().isEmpty()) {
                                    this.agentTaskMap.remove(mpf.getAgentID());
                                }
                            } else {
                                this.agentTaskMap.remove(mpf.getAgentID());
                            }
                        } else {
                            this.agentTaskMap.remove(mpf.getAgentID());
                        }
                    }
                }
            }
        }
        for (CommunicationMessage message : messageManager.getReceivedMessageList(MessageReport.class)) {
            MessageReport report = (MessageReport)message;
            if(agentIDs.contains(report.getSenderID())) {
                if (report.isDone()) {
                    this.agentTaskMap.remove(report.getSenderID());
                } else {
                    EntityID target = this.agentTaskMap.get(report.getSenderID());
                    if(target != null) {
                        this.request.add(target);
                    }
                    this.agentTaskMap.remove(report.getSenderID());
                }
            }
        }
    }

    private void collectTask(MessageManager messageManager) {
        for(CommunicationMessage message : messageManager.getReceivedMessageList()) {
            Class<? extends CommunicationMessage> messageClass = message.getClass();
            if(messageClass == CommandPolice.class) {
                CommandPolice command = (CommandPolice)message;
                if(command.getAction() == CommandPolice.ACTION_CLEAR) {
                    this.request.add(command.getTargetID());
                }
            } else if(messageClass == MessageRoad.class) {
                MessageRoad mr = (MessageRoad)message;
                if (mr.isPassable()) {
                    this.request.remove(mr.getRoadID());
                } else {
                    if(mr.isBlockadeDefined()) {
                        this.request.add(mr.getRoadID());
                    }
                }
            }
        }
    }

    @Override
    public void resume(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, PrecomputeData precomputeInfo, DevelopData developData) {

    }

    @Override
    public void preparate(AgentInfo agentInfo, WorldInfo worldInfo, ScenarioInfo scenarioInfo, ModuleManager moduleManager, DevelopData developData) {

    }

    private class DistanceSorter implements Comparator<StandardEntity> {
        private StandardEntity reference;
        private WorldInfo worldInfo;

        DistanceSorter(WorldInfo wi, StandardEntity reference) {
            this.reference = reference;
            this.worldInfo = wi;
        }

        public int compare(StandardEntity a, StandardEntity b) {
            int d1 = this.worldInfo.getDistance(this.reference, a);
            int d2 = this.worldInfo.getDistance(this.reference, b);
            return d1 - d2;
        }
    }
}
