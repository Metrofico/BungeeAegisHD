package net.md_5.bungee.protocol;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ErrorStream
{

    public void error(String message)
    {
        System.err.print( message );
    }

    public void init()
    {
    } // Just to initialize the class
}
