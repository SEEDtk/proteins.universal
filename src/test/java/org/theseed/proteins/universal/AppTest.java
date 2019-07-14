package org.theseed.proteins.universal;

import junit.framework.Test;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;

import org.theseed.genomes.Contig;
import org.theseed.genomes.Feature;
import org.theseed.genomes.Genome;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;

/**
 * Unit test for simple App.
 */
public class AppTest
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Dummy test
     */
    public void testCounter() {
        Genome fakeGenome = new Genome("12345.6", "Bacillus praestrigiae Narnia", "Bacteria", 11);
        fakeGenome.addContig(new Contig("con1", "agct", 11));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.1",  "Role 1", "con1", "+",  100,  300));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.2",  "Role 2", "con1", "-",  100,  400));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.3",  "Role 3", "con1", "+",  200,  500));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.4",  "Role 4", "con1", "-", 1000, 1200));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.5",  "Role 5", "con1", "+", 1010, 1300));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.6",  "Role 6 / Role 1", "con1", "-", 3300, 4000));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.7",  "Role 2 # comment", "con1", "-", 5000, 5100));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.9",  "Role 1", "con1", "+", 5250, 5400));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.10", "Role 2", "con1", "-", 5401, 5450));
        RoleMap roleMap = new RoleMap();
        roleMap.register("Role 1", "Role 2", "Role 3", "Role 4", "Role 5", "Role A", "Role B");
        Role role1 = roleMap.get("Role1n1");
        Role role2 = roleMap.get("Role2n1");
        Role role3 = roleMap.get("Role3n1");
        Role role4 = roleMap.get("Role4n1");
        Role role5 = roleMap.get("Role5n1");
        Role roleA = roleMap.get("RoleA");
        UniversalRoleCounter newCounter = new UniversalRoleCounter(roleMap);
        newCounter.count(fakeGenome);
        assertThat("Wrong genome count.", newCounter.getCounted(), equalTo(1));
        List<Role> goodRoles = newCounter.universals(0.60);
        assertThat("Wrong roles returned.", goodRoles, contains(role3, role4, role5));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.8",  "Role 3", "con1", "+", 6200, 6500));
        newCounter.count(fakeGenome);
        goodRoles = newCounter.universals(0.60);
        assertThat("Wrong roles returned after second count.", goodRoles, contains(role4, role5));
        assertThat("Wrong good count for role 1.", newCounter.good(role1), equalTo(2));
        assertThat("Wrong bad count for role 1.", newCounter.bad(role1), equalTo(0));
        assertThat("Wrong score for role 2.", newCounter.score(role1), closeTo(1.0, 0.001));
        assertThat("Wrong good count for role 2.", newCounter.good(role2), equalTo(0));
        assertThat("Wrong bad count for role 2.", newCounter.bad(role2), equalTo(2));
        assertThat("Wrong score for role 2.", newCounter.score(role2), closeTo(0.0, 0.001));
        assertThat("Wrong good count for role 3.", newCounter.good(role3), equalTo(1));
        assertThat("Wrong bad count for role 3.", newCounter.bad(role3), equalTo(1));
        assertThat("Wrong score for role 3.", newCounter.score(role3), closeTo(0.5, 0.001));
        fakeGenome.addFeature(new Feature("fig|12345.6.peg.8",  "Role 4", "con1", "+", 7200, 7500));
        goodRoles = newCounter.universals(0.60);
        assertThat("Wrong roles returned after third count.", goodRoles, contains(role5, role4));
        assertThat("Wrong good count for role 4.", newCounter.good(role4), equalTo(2));
        assertThat("Wrong bad count for role 4.", newCounter.bad(role4), equalTo(1));
        assertThat("Wrong score for role 4.", newCounter.score(role4), closeTo(0.667, 0.001));
        assertThat("Wrong good count for role A.", newCounter.good(roleA), equalTo(0));
        assertThat("Wrong bad count for role A.", newCounter.bad(roleA), equalTo(0));
        assertThat("Wrong score for role A.", newCounter.score(roleA), closeTo(0, 0.001));
    }

}