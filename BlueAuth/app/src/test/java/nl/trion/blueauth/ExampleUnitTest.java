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
        assertTrue(BlueAuthDevice.fromString("username;hostname;hostmac").equals(new BlueAuthDevice("username", "hostname", "hostmac")));
    }

    @Test
    public void blueAuthDevice_toString() throws Exception {
        assertTrue((new BlueAuthDevice("username", "hostname", "hostmac")).toString().equals("username;hostname;hostmac"));
    }

    @Test
    public void rsaSignVerify() throws Exception {
        BlueAuthDevice bad = BlueAuthDevice.fromString("aa;bb;cc");
        SecureRandom random = new SecureRandom();
        byte[] challenge = new byte[20];
        random.nextBytes(challenge);

        byte[] response = AuthListActivity.sign(bad, challenge);
        assertTrue(AuthListActivity.verify(bad, response, challenge));
    }
}