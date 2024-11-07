package no.nibio.vips.weather.proxy.parsers;

public class OutOfAreaException extends Exception{
    public OutOfAreaException(String msg)
    {
        super(msg);
    }
}
