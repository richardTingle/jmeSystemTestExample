/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package jmesystemtestexample;

import com.jme3.math.Vector3f;
import org.junit.Test;
import static org.junit.Assert.*;

public class AppTest {

    @Test(timeout = 30000)
    public void exampleSystemTest() {
        TestDriver testDriver = TestDriver.bootAppForTest();

        //mutate the app, giving the box a velocity
        testDriver.actOnTick( app -> app.setBoxVelocity(new Vector3f(0,0.1f,0)));
        //wait for the box to move
        testDriver.waitFor(5);

        //check it's where we expect it to be
        float boxHeight = testDriver.getOnTick(app -> (float)app.getBoxPosition().y);
        assertEquals(0.5f,boxHeight, 0.1f );

    }
}
