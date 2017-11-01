package nl.trion.blueauth;

import org.junit.Test;

import java.security.SecureRandom;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void blueAuthDevice_fromString() throws Exception {
        assertTrue(BlueAuthDevice.fromString("hostmac;username").equals(new BlueAuthDevice("hostmac", "username")));
    }

    @Test
    public void blueAuthDevice_toString() throws Exception {
        assertTrue((new BlueAuthDevice("hostmac", "username")).toString().equals("hostmac;username"));
    }
}