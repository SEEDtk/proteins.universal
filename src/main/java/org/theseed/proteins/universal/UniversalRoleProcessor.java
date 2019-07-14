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
import org.theseed.genomes.Genome;
import org.theseed.genomes.GenomeDirectory;
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
            // Get the universal roles.
            List<Role> universals = this.counter.universals(this.threshold);
            // Write them to the output.
            System.out.println("role_id\tdescription\tgood\tbad");
            for (Role role : universals) {
                System.out.format("%s\t%s\t%d\t%d%n", role.getId(),
                        role.getName(), this.counter.good(role),
                        this.counter.bad(role));
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

}
