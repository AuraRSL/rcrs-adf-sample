package adf.sample.module.complex;

import adf.agent.communication.MessageManager;
import adf.agent.communication.standard.bundle.MessageUtil;
import adf.agent.communication.standard.bundle.centralized.CommandPolice;
import adf.agent.communication.standard.bundle.centralized.MessageReport;
import adf.agent.communication.standard.bundle.information.MessagePoliceForce;
import adf.agent.develop.DevelopData;
import adf.agent.info.AgentInfo;
import adf.agent.info.ScenarioInfo;
import adf.agent.info.WorldInfo;
import adf.agent.module.ModuleManager;
import adf.agent.precompute.PrecomputeData;
import adf.component.communication.CommunicationMessage;
import adf.component.module.complex.PoliceTargetAllocator;
import rescuecore2.standard.entities.*;
import rescuecore2.worldmodel.EntityID;

import java.util.*;

import static rescuecore2.standard.entities.StandardEntityURN.*;

public class SamplePoliceTargetAllocator extends PoliceTargetAllocator {

    private Collection<EntityID> priorityAreas;
    private Collection<EntityID> targetAreas;

    private Map<EntityID, PoliceForceInfo> agentInfoMap;

    public SamplePoliceTargetAllocator(AgentInfo ai, WorldInfo wi, ScenarioInfo si, ModuleManager moduleManager, DevelopData developData) {
        super(ai, wi, si, moduleManager, developData);
        this.priorityAreas = new HashSet<>();
        this.targetAreas = new HashSet<>();
        this.agentInfoMap = new HashMap<>();
    }

    @Override
    public PoliceTargetAllocator resume(PrecomputeData precomputeData) {
        super.resume(precomputeData);
        if(this.getCountResume() >= 2) {
            return this;
        }
        for(EntityID id : this.worldInfo.getEntityIDsOfType(StandardEntityURN.POLICE_FORCE)) {
            this.agentInfoMap.put(id, new PoliceForceInfo(id));
        }
        for(StandardEntity e : this.worldInfo.getEntitiesOfType(REFUGE, BUILDING, GAS_STATION)) {
            for(EntityID id : ((Building)e).getNeighbours()) {
                StandardEntity neighbour = this.worldInfo.getEntity(id);
                if(neighbour instanceof Road) {
                    this.targetAreas.add(id);
                }
            }
        }
        for(StandardEntity e : this.worldInfo.getEntitiesOfType(REFUGE)) {
            for(EntityID id : ((Building)e).getNeighbours()) {
                StandardEntity neighbour = this.worldInfo.getEntity(id);
                if(neighbour instanceof Road) {
                    this.priorityAreas.add(id);
                }
            }
        }
        return this;
    }

    @Override
    public PoliceTargetAllocator preparate() {
        super.preparate();
        if(this.getCountPrecompute() >= 2) {
            return this;
        }
        for(EntityID id : this.worldInfo.getEntityIDsOfType(StandardEntityURN.POLICE_FORCE)) {
            this.agentInfoMap.put(id, new PoliceForceInfo(id));
        }
        for(StandardEntity e : this.worldInfo.getEntitiesOfType(REFUGE, BUILDING, GAS_STATION)) {
            for(EntityID id : ((Building)e).getNeighbours()) {
                StandardEntity neighbour = this.worldInfo.getEntity(id);
                if(neighbour instanceof Road) {
                    this.targetAreas.add(id);
                }
            }
        }
        for(StandardEntity e : this.worldInfo.getEntitiesOfType(REFUGE)) {
            for(EntityID id : ((Building)e).getNeighbours()) {
                StandardEntity neighbour = this.worldInfo.getEntity(id);
                if(neighbour instanceof Road) {
                    this.priorityAreas.add(id);
                }
            }
        }
        return this;
    }

    @Override
    public Map<EntityID, EntityID> getResult() {
        return this.convert(this.agentInfoMap);
    }

    @Override
    public PoliceTargetAllocator calc() {
        List<StandardEntity> agents = this.getActionAgents(this.agentInfoMap);
        for(EntityID target : this.priorityAreas) {
            if(agents.size() > 0) {
                StandardEntity targetEntity = this.worldInfo.getEntity(target);
                if (targetEntity != null) {
                    agents.sort(new DistanceSorter(this.worldInfo, targetEntity));
                    StandardEntity result = agents.get(0);
                    agents.remove(0);
                    PoliceForceInfo info = this.agentInfoMap.get(result.getID());
                    if(info != null) {
                        info.canNewAction = false;
                        info.target = target;
                        this.agentInfoMap.put(result.getID(), info);
                    }
                }
            }
        }
        for(EntityID target : this.targetAreas) {
            if(agents.size() > 0) {
                StandardEntity targetEntity = this.worldInfo.getEntity(target);
                if (targetEntity != null) {
                    agents.sort(new DistanceSorter(this.worldInfo, targetEntity));
                    StandardEntity result = agents.get(0);
                    agents.remove(0);
                    PoliceForceInfo info = this.agentInfoMap.get(result.getID());
                    if(info != null) {
                        info.canNewAction = false;
                        info.target = target;
                        this.agentInfoMap.put(result.getID(), info);
                    }
                }
            }
        }
        if(agents.size() > 0) {
            for(EntityID target : this.priorityAreas) {
                if(agents.size() > 0) {
                    StandardEntity targetEntity = this.worldInfo.getEntity(target);
                    if (targetEntity != null) {
                        agents.sort(new DistanceSorter(this.worldInfo, targetEntity));
                        StandardEntity result = agents.get(0);
                        agents.remove(0);
                        PoliceForceInfo info = this.agentInfoMap.get(result.getID());
                        if(info != null) {
                            info.canNewAction = false;
                            info.target = target;
                            this.agentInfoMap.put(result.getID(), info);
                        }
                    }
                }
            }
        }
        return this;
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


    @Override
    public PoliceTargetAllocator updateInfo(MessageManager messageManager) {
        super.updateInfo(messageManager);
        if(this.getCountUpdateInfo() >= 2) {
            return this;
        }

        for(CommunicationMessage message : messageManager.getReceivedMessageList(MessagePoliceForce.class)) {
            MessagePoliceForce mpf = (MessagePoliceForce) message;
            MessageUtil.reflectMessage(this.worldInfo, mpf);
            PoliceForceInfo info = this.agentInfoMap.get(mpf.getAgentID());
            if(info != null) {
                this.agentInfoMap.put(mpf.getAgentID(), this.update(info, mpf));
            }
        }
        for(CommunicationMessage message : messageManager.getReceivedMessageList(CommandPolice.class)) {
            CommandPolice command = (CommandPolice)message;
            if(command.getAction() == CommandPolice.ACTION_CLEAR && command.isBroadcast()) {
                this.priorityAreas.add(command.getTargetID());
                this.targetAreas.add(command.getTargetID());
            }
        }
        for(CommunicationMessage message : messageManager.getReceivedMessageList(MessageReport.class)) {
            MessageReport report = (MessageReport) message;
            PoliceForceInfo info = this.agentInfoMap.get(report.getSenderID());
            if(info != null && report.isDone()) {
                info.canNewAction = true;
                this.priorityAreas.remove(info.target);
                this.targetAreas.remove(info.target);
                info.target = null;
                this.agentInfoMap.put(info.agentID, info);
            }
        }
        return this;
    }

    private Map<EntityID, EntityID> convert(Map<EntityID, PoliceForceInfo> map) {
        Map<EntityID, EntityID> result = new HashMap<>();
        for(EntityID id : map.keySet()) {
            PoliceForceInfo info = map.get(id);
            if(info != null && info.target != null) {
                result.put(id, info.target);
            }
        }
        return result;
    }

    private List<StandardEntity> getActionAgents(Map<EntityID, PoliceForceInfo> map) {
        List<StandardEntity> result = new ArrayList<>();
        for(StandardEntity entity : this.worldInfo.getEntitiesOfType(StandardEntityURN.POLICE_FORCE)) {
            PoliceForceInfo info = map.get(entity.getID());
            if(info != null && info.canNewAction) {
                result.add(entity);
            }
        }
        return result;
    }

    private PoliceForceInfo update(PoliceForceInfo info, MessagePoliceForce message) {
        if(message.isBuriednessDefined() && message.getBuriedness() > 0) {
            info.canNewAction = false;
            if (info.target != null) {
                this.targetAreas.add(info.target);
                info.target = null;
            }
            return info;
        }
        if(message.getAction() == MessagePoliceForce.ACTION_REST) {
            info.canNewAction = true;
            if (info.target != null) {
                this.targetAreas.add(info.target);
                info.target = null;
            }
        } else if(message.getAction() == MessagePoliceForce.ACTION_MOVE) {
            if(message.getTargetID() != null) {
                StandardEntity entity = this.worldInfo.getEntity(message.getTargetID());
                if(entity != null && entity instanceof Area) {
                    if(info.target != null) {
                        StandardEntity targetEntity = this.worldInfo.getEntity(info.target);
                        if(targetEntity != null && targetEntity instanceof Area) {
                            if(message.getTargetID().getValue() == info.target.getValue()) {
                                info.canNewAction = false;
                            } else {
                                info.canNewAction = true;
                                this.targetAreas.add(info.target);
                                info.target = null;
                            }
                        } else {
                            info.canNewAction = true;
                            info.target = null;
                        }
                    } else {
                        info.canNewAction = true;
                    }
                } else {
                    info.canNewAction = true;
                    if(info.target != null) {
                        this.targetAreas.add(info.target);
                        info.target = null;
                    }
                }
            } else {
                info.canNewAction = true;
                if(info.target != null) {
                    this.targetAreas.add(info.target);
                    info.target = null;
                }
            }
        } else if(message.getAction() == MessagePoliceForce.ACTION_CLEAR) {
            info.canNewAction = false;
            this.priorityAreas.remove(message.getTargetID());
            this.targetAreas.remove(message.getTargetID());
        }
        return info;
    }


    private class PoliceForceInfo {
        EntityID agentID;
        EntityID target;
        boolean canNewAction;

        PoliceForceInfo(EntityID id) {
            agentID = id;
            target = null;
            canNewAction = true;
        }
    }
}