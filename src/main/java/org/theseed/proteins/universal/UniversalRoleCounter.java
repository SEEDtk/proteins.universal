/**
 *
 */
package org.theseed.proteins.universal;

import java.util.ArrayList;
import java.util.List;

import org.theseed.counters.CountMap;
import org.theseed.counters.QualityCountMap;
import org.theseed.genomes.Feature;
import org.theseed.genomes.Genome;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;

/**
 * This object counts universal roles in genomes.  A role that occurs once gets a good count,
 * and a role that occurs more than once gets a bad count.  The object itself tracks the total
 * number of genomes processed.
 *
 * @author Bruce Parrello
 *
 */
public class UniversalRoleCounter extends QualityCountMap<Role> {


    //FIELDS
    /** total number of genomes processed */
    private int genomeCount;
    /** map of useful roles */
    private RoleMap usefulRoles;
    /** count map used for genome role profiles */
    private CountMap<Role> profiler;

    /**
     * Create a new universal role counter for a specified set of roles.
     *
     * @param usefulRoles	role map used to identify the roles of interest
     */
    public UniversalRoleCounter(RoleMap usefulRoles) {
        this.usefulRoles = usefulRoles;
        this.genomeCount = 0;
        this.profiler = new CountMap<Role>();
    }

    /**
     * Count all the roles in the specified genome.
     *
     * @param genome	genome of interest
     */
    public void count(Genome genome) {
        // Create the count map so it only contains role occurrences in the new genome.
        profiler.clear();
        // Get all the features in the genome.
        for (Feature feat : genome.getFeatures()) {
            // Get this feature's roles.
            for (Role role : feat.getUsefulRoles(this.usefulRoles)) {
                profiler.count(role);
            }
        }
        // Loop through the counts, updating our master count map.
        for (CountMap<Role>.Count count : this.profiler.counts()) {
            switch (count.getCount()) {
            case 0:
                // do nothing
                break;
            case 1:
                // singly-occurring, good
                this.setGood(count.getKey());
                break;
            default:
                // multiply-occurring, bad
                this.setBad(count.getKey());
            }
        }
        // Denote we counted this genome.
        this.genomeCount++;
    }

    /**
     * @return the number of genomes counted
     */
    public Object getCounted() {
        return this.genomeCount;
    }

    /**
     * @return a list of the roles that are singly-occurring in the specified fraction of
     * 		   the genomes, sorted from best to worst
     *
     * @param threshold		lowest acceptable fraction score
     */
    public List<Role> universals(double threshold) {
        ArrayList<Role> retVal = new ArrayList<Role>(this.size());
        // Compute the minimum acceptable good count.
        double minD = (threshold * this.genomeCount);
        double minF = Math.floor(minD);
        int min = (minD == minF ? (int) minF - 1 : (int) minF);
        for (Role role : this.bestKeys()) {
            if (this.good(role) > min) retVal.add(role);
        }
        return retVal;
    }

    /**
     * @return the fraction of times a role has a good occurrence
     *
     * @param role	role of interest
     */
    public double score(Role role) {
        return ((double) this.good(role)) / this.genomeCount;
    }





}
