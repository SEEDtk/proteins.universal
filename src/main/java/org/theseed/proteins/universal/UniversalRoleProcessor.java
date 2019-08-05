/**
 *
 */
package org.theseed.proteins.universal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.theseed.genome.Genome;
import org.theseed.genome.GenomeDirectory;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;

/**
 * This class loads a genome directory and outputs the universal roles.  The positional
 * oarameters are the names of directories of GTOs to process.  The command-line options
 * are as follows.
 *
 * -R			name of a file containing the useful roles; the file is tab-delimited,
 * 				each record containing a role ID and a role name
 * -t			the minimum fraction of role occurrences required for a role to be
 * 				considered universal (default 0.90)
 * -v			write progress messages to STDERR
 * -o			name of a file to which the counts should be saved, for future comparisons
 *
 * --compare	name of a saved count file to which our results should be compared; a universal
 * 				role in our results should be universal in this file as well
 *
 * The standard output will contain a report listing the universal roles by ID and
 * name, along with number of single occurrences and the number of genomes having
 * multiple occurrences.
 *
 * @author Bruce Parrello
 *
 */
public class UniversalRoleProcessor {

    // FIELDS
    /** structure for counting the universal roles */
    UniversalRoleCounter counter;

    // COMMAND LINE

    /** help option */
    @Option(name="-h", aliases={"--help"}, help=true)
    private boolean help;

    /** TRUE if we want progress messages */
    @Option(name="-v", aliases={"--verbose", "--debug"}, usage="display progress on STDERR")
    private boolean debug;

    /** minimum fraction for a role to be considered universal */
    @Option(name="-t", aliases={"--minFraction", "--min", "--threshold"}, metaVar="0.8",
            usage="minimum fraction of genomes in which a universal role must occur singly")
    private double threshold;

    /** file containing the useful roles */
    @Option(name="-R", aliases={"--roles", "--roleFile"}, metaVar="roleFile",
            usage="file of useful roles (create only)", required=true)
    private File roleFile;

    /** save file */
    @Option(name="-o", aliases={"--save"}, metaVar="uniRoles.ser",
            usage="file to which to save results")
    private File saveFile;

    /** compare file */
    @Option(name="--compare", metaVar="uniRoles.ser", usage="saved file for comparison")
    private File compareFile;

    /** input genome directories */
    @Argument(index=0, metaVar="genomeDir1 genomeDir2 ...", multiValued=true,
            usage="directory of input genomes")
    private List<File> genomeDirs;


    /**
     * Parse command-line options to specify the parameters of this object.
     *
     * @param args	an array of the command-line parameters and options
     *
     * @return TRUE if successful, FALSE if the parameters are invalid
     */
    public boolean parseCommand(String[] args) {
        boolean retVal = false;
        // Set the defaults.
        this.threshold = 0.90;
        this.debug = false;
        this.roleFile = null;
        this.saveFile = null;
        this.compareFile = null;
        this.genomeDirs = new ArrayList<File>();
        // Parse the command line.
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
            if (this.help) {
                parser.printUsage(System.err);
            } else {
                // Get the role map.
                RoleMap usefulRoles = RoleMap.load(this.roleFile);
                // Insure the genome directories are valid.
                for (File genomeDir : genomeDirs) {
                    if (! genomeDir.isDirectory()) {
                        throw new FileNotFoundException(genomeDir.getPath() + " is not a valid directory.");
                    }
                }
                // Insure the compare file exists.
                if (this.compareFile != null && ! this.compareFile.exists()) {
                    throw new FileNotFoundException(compareFile.getPath() + " not found on disk.");
                }
                // Create the counter.
                this.counter = new UniversalRoleCounter(usefulRoles);
                // We made it this far, we can run the application.
                retVal = true;
            }
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            // For parameter errors, we display the command usage.
            parser.printUsage(System.err);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return retVal;
    }

    /**
     * Run through the genome directories, counting universals, then write the
     * results.
     */
    public void run() {
        try {
            // Loop through the genome directories.
            for (File genomeDirFile : this.genomeDirs) {
                if (debug) System.err.println("Processing " + genomeDirFile + ".");
                GenomeDirectory genomeDir = new GenomeDirectory(genomeDirFile.getPath());
                for (Genome genome : genomeDir) {
                    if (debug) System.err.println("Parsing " + genome + ".");
                    this.counter.count(genome);
                }
            }
            // Save the counters if desired.
            if (this.saveFile != null) {
                if (debug) System.err.println("Saving results to " + this.saveFile + ".");
                this.counter.save(this.saveFile);
            }
            // Get the universal roles.  These are our output.
            List<Role> universals = this.counter.universals(this.threshold);
            // Write the headers.
            System.out.print("role_id\tdescription\tgood\tbad");
            // Do we have a compare file? If so, load it and we have extra headings.
            UniversalRoleCounter comparator = null;
            int totalCount = 0;
            int failureCount = 0;
            if (this.compareFile != null) {
                if (debug) System.err.println("Loading comparison data from " + this.compareFile + ".");
                comparator = UniversalRoleCounter.load(compareFile);
                System.out.println("\ttest_pct\ttest_count\terror");
            } else {
                System.out.println();
            }
            // Write them to the output.
            for (Role role : universals) {
                totalCount++;
                System.out.format("%s\t%s\t%d\t%d", role.getId(),
                        role.getName(), this.counter.good(role),
                        this.counter.bad(role));
                if (comparator != null) {
                    // Here we are comparing.
                    String errorFlag = "";
                    double score = comparator.score(role);
                    if (score < this.threshold) {
                        failureCount++;
                        errorFlag = "Y";
                    }
                    System.out.format("\t%4.2g\t%d\t%s%n", score, comparator.good(role), errorFlag);
                } else {
                    System.out.println();
                }
            }
            if (debug) {
                System.err.println(totalCount + " universal roles found.");
                if (comparator != null) {
                    System.err.println("Failure count is " + failureCount + ".");
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

}
