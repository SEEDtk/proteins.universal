package org.theseed.proteins.universal;

public class App
{
    public static void main( String[] args )
    {
        UniversalRoleProcessor runObject = new UniversalRoleProcessor();
        boolean ok = runObject.parseCommand(args);
        if (ok) {
            runObject.run();
        }
    }
}
