import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Duration;

public final class ElapsedTimerTest {
    private ElapsedTimerTest() {
    }

    public static void main(String[] args) throws Exception {
        Class<?> clockType =
            Class.forName("com.autoclicker.ProgrammableAutoClicker$ElapsedClock");
        Constructor<?> constructor = clockType.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object clock = constructor.newInstance();

        Method start = clockType.getDeclaredMethod("start");
        start.setAccessible(true);
        Method finish = clockType.getDeclaredMethod("finish");
        finish.setAccessible(true);
        Method elapsed = clockType.getDeclaredMethod("elapsed");
        elapsed.setAccessible(true);

        start.invoke(clock);
        Thread.sleep(100);
        finish.invoke(clock);
        long stoppedElapsed = ((Duration) elapsed.invoke(clock)).toMillis();
        Thread.sleep(500);
        long laterElapsed = ((Duration) elapsed.invoke(clock)).toMillis();

        if (stoppedElapsed != laterElapsed) {
            throw new AssertionError(
                "Timer advanced after stop: "
                    + stoppedElapsed
                    + " ms -> "
                    + laterElapsed
                    + " ms"
            );
        }

        System.out.println(
            "PASS: elapsed timer remained frozen at "
                + stoppedElapsed
                + " ms after a 500 ms wait"
        );
    }
}
