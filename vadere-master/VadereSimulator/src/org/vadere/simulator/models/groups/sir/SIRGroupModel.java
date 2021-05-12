package org.vadere.simulator.models.groups.sir;


import org.vadere.annotation.factories.models.ModelClass;
import org.vadere.simulator.models.Model;
import org.vadere.simulator.models.groups.AbstractGroupModel;
import org.vadere.simulator.models.groups.Group;
import org.vadere.simulator.models.groups.GroupSizeDeterminator;
import org.vadere.simulator.models.groups.cgm.CentroidGroup;
import org.vadere.simulator.models.potential.fields.IPotentialFieldTarget;
import org.vadere.simulator.projects.Domain;
import org.vadere.state.attributes.Attributes;
import org.vadere.simulator.models.groups.sir.SIRGroup;
import org.vadere.state.attributes.models.AttributesSIRG;
import org.vadere.state.attributes.scenario.AttributesAgent;
import org.vadere.state.scenario.DynamicElementContainer;
import org.vadere.state.scenario.Pedestrian;
import org.vadere.state.scenario.Topography;
import org.vadere.util.geometry.LinkedCellsGrid;
import org.vadere.util.geometry.shapes.VRectangle;

import java.util.*;

/**
 * Implementation of groups for a susceptible / infected / removed (SIR) model.
 */
@ModelClass
public class SIRGroupModel extends AbstractGroupModel<SIRGroup> {

    private Random random;
    private LinkedHashMap<Integer, SIRGroup> groupsById;
    private Map<Integer, LinkedList<SIRGroup>> sourceNextGroups;
    private AttributesSIRG attributesSIRG;
    private Topography topography;
    private IPotentialFieldTarget potentialFieldTarget;
    private int totalInfected = 0;
    private int updateCounter = 0;

    public SIRGroupModel() {
        this.groupsById = new LinkedHashMap<>();
        this.sourceNextGroups = new HashMap<>();
    }

    @Override
    public void initialize(List<Attributes> attributesList, Domain domain,
                           AttributesAgent attributesPedestrian, Random random) {
        this.attributesSIRG = Model.findAttributes(attributesList, AttributesSIRG.class);
        this.topography = domain.getTopography();
        this.random = random;
        this.totalInfected = 0;
    }

    @Override
    public void setPotentialFieldTarget(IPotentialFieldTarget potentialFieldTarget) {
        this.potentialFieldTarget = potentialFieldTarget;
        // update all existing groups
        for (SIRGroup group : groupsById.values()) {
            group.setPotentialFieldTarget(potentialFieldTarget);
        }
    }

    @Override
    public IPotentialFieldTarget getPotentialFieldTarget() {
        return potentialFieldTarget;
    }

    private int getFreeGroupId() {
        if (this.random.nextDouble() < this.attributesSIRG.getInfectionRate()
                || this.totalInfected < this.attributesSIRG.getInfectionsAtStart()) {
            if (!getGroupsById().containsKey(SIRType.ID_INFECTED.ordinal())) {
                SIRGroup g = getNewGroup(SIRType.ID_INFECTED.ordinal(), Integer.MAX_VALUE / 2);
                getGroupsById().put(SIRType.ID_INFECTED.ordinal(), g);
            }
            this.totalInfected += 1;
            return SIRType.ID_INFECTED.ordinal();
        } else {
            if (!getGroupsById().containsKey(SIRType.ID_SUSCEPTIBLE.ordinal())) {
                SIRGroup g = getNewGroup(SIRType.ID_SUSCEPTIBLE.ordinal(), Integer.MAX_VALUE / 2);
                getGroupsById().put(SIRType.ID_SUSCEPTIBLE.ordinal(), g);
            }
            return SIRType.ID_SUSCEPTIBLE.ordinal();
        }
    }


    @Override
    public void registerGroupSizeDeterminator(int sourceId, GroupSizeDeterminator gsD) {
        sourceNextGroups.put(sourceId, new LinkedList<>());
    }

    @Override
    public int nextGroupForSource(int sourceId) {
        return Integer.MAX_VALUE / 2;
    }

    @Override
    public SIRGroup getGroup(final Pedestrian pedestrian) {
        SIRGroup group = groupsById.get(pedestrian.getGroupIds().getFirst());
        assert group != null : "No group found for pedestrian";
        return group;
    }

    @Override
    protected void registerMember(final Pedestrian ped, final SIRGroup group) {
        groupsById.putIfAbsent(ped.getGroupIds().getFirst(), group);
    }

    @Override
    public Map<Integer, SIRGroup> getGroupsById() {
        return groupsById;
    }

    @Override
    protected SIRGroup getNewGroup(final int size) {
        return getNewGroup(getFreeGroupId(), size);
    }

    @Override
    protected SIRGroup getNewGroup(final int id, final int size) {
        if (groupsById.containsKey(id)) {
            return groupsById.get(id);
        } else {
            return new SIRGroup(id, this);
        }
    }

    private void initializeGroupsOfInitialPedestrians() {
        // get all pedestrians already in topography
        DynamicElementContainer<Pedestrian> c = topography.getPedestrianDynamicElements();

        if (c.getElements().size() > 0) {

            // TODO: fill in code to assign pedestrians in the scenario at the beginning (i.e., not created by a source)
            //  to INFECTED or SUSCEPTIBLE groups.
            ;

            //adding randomized pedestrians that will start infected

            List<Integer> infectedIDS = new ArrayList<>();
            Random myRandom = new Random();
            while (infectedIDS.size() < this.attributesSIRG.getInfectionsAtStart()) {
                int r = myRandom.nextInt(c.getElements().size());
                if (!infectedIDS.contains(r)) {
                    infectedIDS.add(r);
                }
            }

            /**
             List<Integer> infectedIDS = new ArrayList<>(c.getElements().size());
             for (int i = 0; i < n; i++){
             infectedIDS.add(i);
             }
             Collections.shuffle(infectedIDS);
             infectedIDS.subList(0, this.attributesSIRG.getInfectionsAtStart()).clear();
             **/

            //assigning the infection status
            int i = 0;
            for (Pedestrian p : c.getElements()) {
                if (infectedIDS.contains(i)) {
                    assignToGroup(p, SIRType.ID_INFECTED.ordinal());

                } else {
                    //elementRemoved(p)
                    assignToGroup(p, SIRType.ID_SUSCEPTIBLE.ordinal());
                }
                i++;
            }

            //elementRemoved(p); maybe at some point

        }
    }

    protected void assignToGroup(Pedestrian ped, int groupId) {
        SIRGroup currentGroup = getNewGroup(groupId, Integer.MAX_VALUE / 2);
        currentGroup.addMember(ped);
        ped.getGroupIds().clear();
        ped.getGroupSizes().clear();
        ped.addGroupId(currentGroup.getID(), currentGroup.getSize());
        registerMember(ped, currentGroup);
    }

    protected void assignToGroup(Pedestrian ped) {
        int groupId = getFreeGroupId();
        assignToGroup(ped, groupId);
    }


    /* DynamicElement Listeners */

    @Override
    public void elementAdded(Pedestrian pedestrian) {
        assignToGroup(pedestrian);
    }

    @Override
    public void elementRemoved(Pedestrian pedestrian) {
        Group group = groupsById.get(pedestrian.getGroupIds().getFirst());
        if (group.removeMember(pedestrian)) { // if true pedestrian was last member.
            groupsById.remove(group.getID());
        }
    }

    /* Model Interface */

    @Override
    public void preLoop(final double simTimeInSec) {
        // this.cnt = 0
        initializeGroupsOfInitialPedestrians();
        topography.addElementAddedListener(Pedestrian.class, this);
        topography.addElementRemovedListener(Pedestrian.class, this);
    }

    @Override
    public void postLoop(final double simTimeInSec) {
    }

    @Override
    public void update(final double simTimeInSec) {


        if (simTimeInSec <= updateCounter) {
            return;
        }
        updateCounter++;


        // clock check each second

        // check the positions of all pedestrians and switch groups to INFECTED (or REMOVED).
        DynamicElementContainer<Pedestrian> c = topography.getPedestrianDynamicElements();


        VRectangle vrec = new VRectangle(0, 0, this.topography.getBoundingBoxWidth(), this.topography.getBoundingBoxWidth());
        LinkedCellsGrid lcg = new LinkedCellsGrid(vrec, this.topography.getBoundingBoxWidth());

        for (Pedestrian p : c.getElements()) {
            lcg.addObject(p);

            //System.out.println(p.getPosition().x+p.getPosition().x);
        }
        for (Pedestrian p : c.getElements()) {
            if (getGroup(p).getID() == SIRType.ID_INFECTED.ordinal()) {
                List<Pedestrian> victims = lcg.getObjects(p.getPosition(), this.attributesSIRG.getInfectionMaxDistance());

                for (Pedestrian v : victims) {

                    if (getGroup(v).getID() != SIRType.ID_Recovered.ordinal() && random.nextDouble() < attributesSIRG.getInfectionRate()) {

                        //elementRemoved(p);
                        assignToGroup(v, SIRType.ID_INFECTED.ordinal());

                    }
                }

            }
        }
        for (Pedestrian p : c.getElements()) {
            if (getGroup(p).getID() == SIRType.ID_INFECTED.ordinal()) {

                if (this.random.nextDouble() < attributesSIRG.getRecoveryRate()) {
                    assignToGroup(p, SIRType.ID_Recovered.ordinal());
                }
            }
        }

        System.out.println("updateCounter " + updateCounter);
        System.out.println("simTimeinSec " + simTimeInSec);

        /**
         // For debuging

         int cnt=0;
         for (Pedestrian p : c.getElements()){
         if (getGroup(p).getID()==SIRType.ID_INFECTED.ordinal()) {
         cnt++;
         }
         }
         System.out.println(cnt);
         **/
        /**
         if (c.getElements().size() > 0) {
         for(Pedestrian p : c.getElements()) {
         // loop over neighbors and set infected if we are close
         for(Pedestrian p_neighbor : c.getElements()) {
         if(p == p_neighbor || getGroup(p_neighbor).getID() != SIRType.ID_INFECTED.ordinal())
         continue;
         double dist = p.getPosition().distance(p_neighbor.getPosition());
         if (dist < attributesSIRG.getInfectionMaxDistance() &&
         this.random.nextDouble() < attributesSIRG.getInfectionRate()) {
         SIRGroup g = getGroup(p);
         if (g.getID() == SIRType.ID_SUSCEPTIBLE.ordinal()) {
         elementRemoved(p);
         assignToGroup(p, SIRType.ID_INFECTED.ordinal()); }
         }
         }
         }
         }
         **/

    }
}
