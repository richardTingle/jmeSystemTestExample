package jmesystemtestexample;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.system.AppSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The test driver allows for controlled interaction between the thread running the application and the thread
 * running the test
 */
public class TestDriver extends BaseAppState{

    private static Executor executor = Executors.newSingleThreadExecutor();

    private CopyOnWriteArrayList<InAppEvent> actOnTickQueue = new CopyOnWriteArrayList<>();

    @Override
    public void update(float tpf){
        super.update(tpf);

        actOnTickQueue.forEach(event -> {
            event.timeTillRun-=tpf;
            if (event.timeTillRun<0){
                actOnTickQueue.remove(event);
                event.runnable.accept((App)getApplication());
                synchronized(event.waitObject){
                    event.waitObject.notify();
                }
            }
        });

    }

    /**
     * Passes a piece of code to run within the game's update thread.
     *
     * Blocks the calling thread until that tick has occurred
     */
    public void actOnTick(Consumer<App> runnable){
        getOnTick(app -> {
            runnable.accept(app);
            return null;
        } );
    }

    public <T> T getOnTick(Function<App, T> obtainFunction){
        List<T> mutable = new ArrayList<>();

        Object waitObject = new Object();
        actOnTickQueue.add(new InAppEvent((app) -> mutable.add(obtainFunction.apply(app)), 0, waitObject));
        synchronized(waitObject){
            try{
                waitObject.wait();
            } catch(InterruptedException e){
                throw new RuntimeException(e);
            }
        }

        return mutable.get(0);
    }

    public void waitFor(double timeToWait){
        Object waitObject = new Object();
        actOnTickQueue.add(new InAppEvent((app) -> {}, timeToWait, waitObject));
        synchronized(waitObject){
            try{
                waitObject.wait();
            } catch(InterruptedException e){
                throw new RuntimeException(e);
            }
        }
    }


    @Override protected void initialize(Application app){}

    @Override protected void cleanup(Application app){}

    @Override protected void onEnable(){}

    @Override protected void onDisable(){}

    /**
     * Boots up the application on a seperate thread (blocks until that happens) and returns a test driver that can be
     * used to safely interact with it
     */
    public static TestDriver bootAppForTest(){
        TestDriver testDriver = new TestDriver();

        App app = new App(testDriver);
        AppSettings appSettings = new AppSettings(true);
        appSettings.setFrameRate(60);
        app.setSettings(appSettings);
        app.setShowSettings(false);

        executor.execute(() -> app.start());

        while( !app.hasBeenInitialised){
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return testDriver;
    }

    private static class InAppEvent{
        Consumer<App> runnable;
        double timeTillRun;
        final Object waitObject;

        public InAppEvent(Consumer<App> runnable, double timeTillRun, Object waitObject){
            this.runnable = runnable;
            this.timeTillRun = timeTillRun;
            this.waitObject = waitObject;
        }
    }
}
