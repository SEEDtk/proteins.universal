/**
 *
 */
package org.theseed.proteins.universal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.theseed.counters.CountMap;
import org.theseed.counters.QualityCountMap;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
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
     * Create a blank universal role counter.
     */
    private UniversalRoleCounter() {
        this.usefulRoles = new RoleMap();
        this.genomeCount = 0;
        this.profiler = new CountMap<Role>();
    }

    /**
     * Save a universal role counter to a file.
     * @throws IOException
     */
    public void save(File outFile) throws IOException {
        FileOutputStream fileStream = new FileOutputStream(outFile);
        DataOutputStream outStream = new DataOutputStream(fileStream);
        // Write out the genome count.
        outStream.writeInt(this.genomeCount);
        // Loop through the roles.  For each one, write the ID, name, and count.
        Collection<Role> roles = this.usefulRoles.values();
        outStream.writeInt(roles.size());
        for (Role role : roles) {
            outStream.writeUTF(role.getId());
            outStream.writeUTF(role.getName());
            outStream.writeInt(this.good(role));
            outStream.writeInt(this.bad(role));
        }
        // Close the stream.
        outStream.close();
    }

    /**
     * Load a universal role counter from a file.
     * @throws IOException
     */
    public static UniversalRoleCounter load(File inFile) throws IOException {
        FileInputStream fileStream = new FileInputStream(inFile);
        DataInputStream inStream = new DataInputStream(fileStream);
        // Create the return object.
        UniversalRoleCounter retVal = new UniversalRoleCounter();
        // Read in the genome count.
        retVal.genomeCount = inStream.readInt();
        // Read in the role count.
        int roleCount = inStream.readInt();
        // Loop through the roles.
        for (int i = 0; i < roleCount; i++) {
            // Create the role.
            String roleId = inStream.readUTF();
            String roleName = inStream.readUTF();
            Role role = new Role(roleId, roleName);
            retVal.usefulRoles.register(role);
            // Get the counts.
            int good = inStream.readInt();
            int bad = inStream.readInt();
            if (good > 0)
                retVal.setGood(role, good);
            if (bad > 0)
                retVal.setBad(role, bad);
        }
        // All done.
        inStream.close();
        return retVal;
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
    public int getCounted() {
        return this.genomeCount;
    }

    /**
     * @return the role with the specified ID, or NULL if no such role exists
     *
     * @param roleId	ID of the desired role
     */
    public Role getRole(String roleId) {
        return this.usefulRoles.get(roleId);
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
