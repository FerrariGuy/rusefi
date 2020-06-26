package com.rusefi;


import com.opensr5.Logger;
import com.rusefi.binaryprotocol.BinaryProtocol;
import com.rusefi.config.generated.Fields;
import com.rusefi.core.MessagesCentral;
import com.rusefi.core.Sensor;
import com.rusefi.core.SensorCentral;
import com.rusefi.io.CommandQueue;
import com.rusefi.io.LinkManager;
import com.rusefi.waves.EngineChart;
import com.rusefi.waves.EngineReport;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.rusefi.IoUtil.*;
import static com.rusefi.IoUtil.getEnableCommand;
import static com.rusefi.TestingUtils.*;
import static com.rusefi.config.generated.Fields.ET_CITROEN_TU3JP;
import static com.rusefi.config.generated.Fields.MOCK_MAF_COMMAND;
import static com.rusefi.waves.EngineReport.isCloseEnough;

/**
 * rusEfi firmware simulator functional test suite
 * <p/>
 * java -cp rusefi_console.jar com.rusefi.AutoTest
 *
 * @author Andrey Belomutskiy
 * 3/5/14
 */
public class AutoTest {
    public static final int COMPLEX_COMMAND_RETRY = 10000;
    static int currentEngineType;
    private static String criticalError;

    private final LinkManager linkManager;
    private CommandQueue commandQueue;

    public AutoTest(LinkManager linkManager, CommandQueue commandQueue) {
        this.linkManager = linkManager;
        this.commandQueue = commandQueue;
    }

    void mainTestBody() throws Exception {
        MessagesCentral.getInstance().addListener(new MessagesCentral.MessageListener() {
            @Override
            public void onMessage(Class clazz, String message) {
                if (message.startsWith(Fields.CRITICAL_PREFIX))
                    criticalError = message;
            }
        });


        BinaryProtocol bp = linkManager.getCurrentStreamState();
        // let's make sure 'burn' command works since sometimes it does not
        bp.burn(Logger.CONSOLE);

        sendCommand(getDisableCommand(Fields.CMD_TRIGGER_HW_INPUT));
        sendCommand(getEnableCommand(Fields.CMD_FUNCTIONAL_TEST_MODE));
        testCustomEngine();
        testVW_60_2();
        testV12();
        testMazdaMiata2003();
        test2003DodgeNeon();
        testFordAspire();
        test1995DodgeNeon();
        testMazdaProtege();
// todo?        sendCommand("reboot"); // this would cause firmware in 5 seconds
// todo?        sleep(10); // time to reboot
        testBmwE34();
        testSachs();
        testRoverV8();
        testMitsu();
        testCamaro();
        testCitroenBerlingo();
        testMazda626();
        testFord6();
        testFordFiesta();
    }

    private static Function<String, Object> FAIL = new Function<String, Object>() {
        @Override
        public Object apply(String errorCode) {
            if (errorCode != null)
                throw new IllegalStateException("Failed " + errorCode);
            return null;
        }
    };

    private void testVW_60_2() {
        setEngineType(32);
        changeRpm(900);
        // first let's get to expected RPM
        assertRpmDoesNotJump(20000, 15, 30, FAIL);
    }

    private void testV12() {
        setEngineType(40);
        changeRpm(700);
        // first let's get to expected RPM
        assertRpmDoesNotJump(15000, 15, 30, FAIL);
    }

    public void assertRpmDoesNotJump(int rpm, int settleTime, int testDuration, Function<String, Object> callback) {
        changeRpm(rpm);
        sleepSeconds(settleTime);
        AtomicReference<String> result = new AtomicReference<>();
        SensorCentral.SensorListener listener = new SensorCentral.SensorListener() {
            @Override
            public void onSensorUpdate(double value) {
                double actualRpm = SensorCentral.getInstance().getValue(Sensor.RPM);
                if (!isCloseEnough(rpm, actualRpm))
                    result.set("Got " + actualRpm + " while trying to stay at " + rpm);
            }
        };
        SensorCentral.getInstance().addListener(Sensor.RPM, listener);
        sleepSeconds(testDuration);
        callback.apply(result.get());
        SensorCentral.getInstance().removeListener(Sensor.RPM, listener);
    }

    private void testCustomEngine() {
        setEngineType(0);
        sendCommand("set_toothed_wheel 4 0");
//        sendCommand("enable trigger_only_front");
//        changeRpm(100);
//        changeRpm(1500);
//        sendCommand("disable trigger_only_front");
//        changeRpm(100);
//        changeRpm(1500);
    }

    private void testMazdaMiata2003() {
        setEngineType(47);
        sendCommand("get cranking_dwell"); // just test coverage
//        sendCommand("get nosuchgettersdfsdfsdfsdf"); // just test coverage
    }

    private void testCamaro() {
        setEngineType(35);
    }

    private void testSachs() {
        setEngineType(29);
        String msg = "BMW";
        IoUtil.changeRpm(1200);
        // todo: add more content
    }

    private void testBmwE34() {
        setEngineType(25);
        sendCommand("chart 1");
        String msg = "BMW";
        EngineChart chart;
        IoUtil.changeRpm(200);
        chart = nextChart1();
        double x = 173.988;
        // something is wrong here - it's a 6 cylinder here, why 4 cylinder cycle?
        assertWave(msg, chart, EngineChart.SPARK_1, 0.0199666, x, x + 180, x + 360, x + 540);

        IoUtil.changeRpm(1200);
        chart = nextChart1();

        x = 688.464;
        // something is wrong here - it's a 6 cylinder here, why 4 cylinder cycle?
        assertWave(msg, chart, EngineChart.SPARK_1, 0.0597999999, x, x + 180, x + 360, x + 540);

        x = 101;
        // 6 cylinder
        assertWave(msg, chart, EngineChart.MAP_AVERAGING, 0.139, x, x + 120, x + 240, x + 360, x + 480, x + 600);
    }

    void changeRpm(final int rpm) {
        IoUtil.changeRpm(rpm);
    }

    private void testMitsu() {
        setEngineType(16);
        sendCommand("disable cylinder_cleanup");
        String msg = "Mitsubishi";
        changeRpm(200);

        changeRpm(1200);
        // todo: add more content
    }

    private void testCitroenBerlingo() {
        setEngineType(ET_CITROEN_TU3JP);
        String msg = "Citroen";
        changeRpm(1200);
        // todo: add more content
    }

    private void setEngineType(int type) {
        FileLog.MAIN.logLine("AUTOTEST setEngineType " + type);
//        sendCommand(CMD_PINS);
        currentEngineType = type;
        sendCommand("set " + Fields.CMD_ENGINE_TYPE + " " + type, COMPLEX_COMMAND_RETRY, Timeouts.SET_ENGINE_TIMEOUT);
        // TODO: document the reason for this sleep?!
        sleepSeconds(1);
        sendCommand(getEnableCommand("self_stimulation"));
    }

    private void testMazda626() {
        setEngineType(28);
        String msg = "mazda 626 default cranking";
        IoUtil.changeRpm(200);
        EngineChart chart;
        chart = nextChart1();

        double x = 102;
        assertWave(msg, chart, EngineChart.SPARK_1, 0.1944, x, x + 180, x + 360, x + 540);
    }

    static EngineChart nextChart() {
        return TestingUtils.nextChart();
    }

    private void test2003DodgeNeon() {
        setEngineType(23);
        sendCommand("set wwaeTau 0");
        sendCommand("set wwaeBeta 0");
        sendCommand("set mock_map_voltage 1");
        sendCommand("set mock_vbatt_voltage 1.20");
        sendCommand("disable cylinder_cleanup");
        EngineChart chart;
        String msg = "2003 Neon cranking ";
        IoUtil.changeRpm(200);
        IoUtil.changeRpm(250); // another approach to artificial delay
        IoUtil.changeRpm(200);
        assertEquals("VBatt", 12, SensorCentral.getInstance().getValue(Sensor.VBATT));

        chart = nextChart();
        double x = 100;
        assertWave(true, msg, chart, EngineChart.SPARK_1, 0.194433, 0.01, EngineReport.RATIO, x + 180, x + 540);
        assertWaveNull(msg, chart, EngineChart.SPARK_2);
        assertWave(true, msg, chart, EngineChart.SPARK_3, 0.194433, 0.01, EngineReport.RATIO, x, x + 360);
        assertWaveNull(msg, chart, EngineChart.SPARK_4);

        x = 176.856;
        // todo: why is width precision so low here? is that because of loaded Windows with 1ms precision?
        double widthRatio = 0.25;
        // WAT? this was just 0.009733333333333387?
        assertWave(true, msg, chart, EngineChart.INJECTOR_1, 0.01056666666666691, 0.02, widthRatio, x, x + 180, x + 360, x + 540);
        assertWave(true, msg, chart, EngineChart.INJECTOR_2, 0.01056666666666691, 0.02, widthRatio, x, x + 180, x + 360, x + 540);
        assertWave(true, msg, chart, EngineChart.INJECTOR_3, 0.01056666666666691, 0.02, widthRatio, x, x + 180, x + 360, x + 540);
        assertWave(true, msg, chart, EngineChart.INJECTOR_4, 0.01056666666666691, 0.02, widthRatio, x, x + 180, x + 360, x + 540);

        msg = "2003 Neon running";
        IoUtil.changeRpm(2000);
        IoUtil.changeRpm(2700);
        IoUtil.changeRpm(2000);
        chart = nextChart();
        x = 104.0;
        assertWave(true, msg, chart, EngineChart.SPARK_1, 0.13299999999999998, EngineReport.RATIO, EngineReport.RATIO, x + 180, x + 540);
        assertWaveNull(msg, chart, EngineChart.SPARK_2);
        assertWave(true, msg, chart, EngineChart.SPARK_3, 0.13299999999999998, EngineReport.RATIO, EngineReport.RATIO, x, x + 360);
        assertWaveNull(msg, chart, EngineChart.SPARK_4);

        chart = nextChart();
        x = 74;
        assertWave(true, msg, chart, EngineChart.INJECTOR_1, 0.29233, EngineReport.RATIO, 0.2, x + 360);
        assertWave(true, msg, chart, EngineChart.INJECTOR_2, 0.29233, 0.15, 0.2, x + 180);
        assertWave(true, msg, chart, EngineChart.INJECTOR_3, 0.29233, 0.15, EngineReport.RATIO, x + 540);
        assertWave(true, msg, chart, EngineChart.INJECTOR_4, 0.29233, 0.15, 0.2, x);

        sendCommand(getEnableCommand("trigger_only_front"));
        chart = nextChart();
        assertWave(true, msg, chart, EngineChart.INJECTOR_1, 0.29233, 0.1, 0.2, x + 360);
        assertWave(true, msg, chart, EngineChart.INJECTOR_2, 0.29233, EngineReport.RATIO, 0.2, x + 180);
        assertWave(true, msg, chart, EngineChart.INJECTOR_3, 0.29233, 0.1, 0.2, x + 540);
        assertWave(true, msg, chart, EngineChart.INJECTOR_4, 0.29233, 0.1, 0.2, x);

        sendCommand("set_whole_timing_map 520");
        chart = nextChart1();
        x = 328;
        assertWave(true, msg, chart, EngineChart.SPARK_1, 0.13299999999999998, EngineReport.RATIO, EngineReport.RATIO, x + 180, x + 540);

        sendCommand("set_whole_timing_map 0");
        chart = nextChart1();
        x = 128;
        assertWave(true, msg, chart, EngineChart.SPARK_1, 0.13299999999999998, EngineReport.RATIO, EngineReport.RATIO, x + 180, x + 540);
    }

    private void testMazdaProtege() {
        setEngineType(14);
        EngineChart chart;
        sendCommand("set mock_vbatt_voltage 1.395");
        IoUtil.changeRpm(200);
        IoUtil.changeRpm(260);
        IoUtil.changeRpm(200);
        String msg = "ProtegeLX cranking";
        chart = nextChart1();
        assertEquals("", 12, SensorCentral.getInstance().getValue(Sensor.VBATT), 0.1);
        double x = 107;
        assertWave(msg, chart, EngineChart.SPARK_3, 0.194433, x);
        assertWave(msg, chart, EngineChart.SPARK_1, 0.194433, x + 540);
        x = 0;
        assertWaveFall(msg, chart, EngineChart.INJECTOR_1, 0.008566666666, x, x + 180, x + 360, x + 540);
        assertWaveFall(msg, chart, EngineChart.INJECTOR_2, 0.008566666666, x, x + 180, x + 360, x + 540);

        msg = "ProtegeLX running";
        IoUtil.changeRpm(2000);
        chart = nextChart1();
        x = 112;
        assertWave(msg, chart, EngineChart.SPARK_1, 0.13333333333333333, x, x + 180, x + 360, x + 540);
        x = 0;
        assertWaveFall(msg, chart, EngineChart.INJECTOR_1, 0.21433333333333345, x + 180, x + 540);
        assertWaveFall(msg, chart, EngineChart.INJECTOR_2, 0.21433333333333345, x, x + 360);
    }

    private void test1995DodgeNeon() {
        setEngineType(2);
        EngineChart chart;
        sendComplexCommand("set_whole_fuel_map 3");
        sendComplexCommand("set_individual_coils_ignition");
        /**
         * note that command order matters - RPM change resets wave chart
         */
        IoUtil.changeRpm(2000);
        chart = nextChart1();

        String msg = "1995 Neon";
        double x = -70;
        assertWaveFall(msg, chart, EngineChart.INJECTOR_4, 0.133, x + 540);
        assertWaveFall(msg, chart, EngineChart.INJECTOR_2, 0.133, x + 720);
        assertWaveFall(msg, chart, EngineChart.INJECTOR_1, 0.133, x + 180);
        assertWaveFall(msg, chart, EngineChart.INJECTOR_3, 0.133, x + 360);

        x = 112.92;
        assertWave(msg, chart, EngineChart.SPARK_4, 0.13333, x + 540);
        assertWave(msg, chart, EngineChart.SPARK_2, 0.13333, x);
        assertWave(msg, chart, EngineChart.SPARK_1, 0.13333, x + 180);
        assertWave(msg, chart, EngineChart.SPARK_3, 0.13333, x + 360);

        // switching to Speed Density
        sendCommand("set mock_map_voltage 1");
        sendComplexCommand("set algorithm 3");
        IoUtil.changeRpm(2600);
        IoUtil.changeRpm(2000);
        chart = nextChart1();
        x = -70;
        assertWaveFall(msg, chart, EngineChart.INJECTOR_4, 0.493, x + 540);
    }

    private void testRoverV8() {
        setEngineType(10);
    }

    private void testFordFiesta() {
        setEngineType(4);
        EngineChart chart;
        IoUtil.changeRpm(2000);
        chart = nextChart1();

        String msg = "Fiesta";
        double x = 312;
        assertWave("wasted spark #1 with Fiesta", chart, EngineChart.SPARK_1, 0.1333333, x, x + 360);
        assertWaveNull(chart, EngineChart.SPARK_2);
        assertWave("wasted spark #3 with Fiesta", chart, EngineChart.SPARK_3, 0.1333333, x + 180, x + 540);
        assertWaveNull(msg, chart, EngineChart.SPARK_4);
    }

    private void testFord6() {
        setEngineType(7);
        EngineChart chart;
        IoUtil.changeRpm(2000);
        chart = nextChart1();

        String msg = "ford 6";

        double x = 7;
        assertWave(msg, chart, EngineChart.SPARK_1, 0.01666, x, x + 120, x + 240, x + 360, x + 480, x + 600);

        assertWaveNull(msg, chart, EngineChart.TRIGGER_2);
        sendComplexCommand("set " + "trigger_type" + " 1"); // TT_FORD_ASPIRE
        chart = nextChart1();
        assertTrue(msg, chart.get(EngineChart.TRIGGER_2) != null);
    }

    private void testFordAspire() {
        setEngineType(3);
        sendCommand("disable cylinder_cleanup");
        sendCommand("set mock_map_voltage 1");
        sendCommand("set mock_vbatt_voltage 2.2");
        String msg;
        EngineChart chart;
        // todo: interesting changeRpm(100);
        sendComplexCommand("set cranking_rpm 500");
        IoUtil.changeRpm(200);

        double x;
        chart = nextChart1();
        assertEquals(12, SensorCentral.getInstance().getValue(Sensor.VBATT));
        x = 55;
        assertWave("aspire default cranking ", chart, EngineChart.SPARK_1, 0.1944, x, x + 180, x + 360, x + 540);


        IoUtil.changeRpm(600);
        chart = nextChart1();
        x = 78;
        assertWave(true, "aspire default running ", chart, EngineChart.SPARK_1, 0.04, 0.1, 0.1, x, x + 180, x + 360, x + 540);

        IoUtil.changeRpm(200);

        sendCommand("set cranking_charge_angle 65");
        sendCommand("set cranking_timing_angle -31");

        chart = nextChart();
        x = 55;
        assertWave("aspire cranking", chart, EngineChart.SPARK_1, 0.18, x, x + 180, x + 360, x + 540);

        sendCommand("set cranking_timing_angle -40");
        chart = nextChart();
        x = 64;
        assertWave("aspire", chart, EngineChart.SPARK_1, 0.18, x, x + 180, x + 360, x + 540);
        sendCommand("set cranking_timing_angle 149");

        sendCommand("set cranking_charge_angle 40");
        chart = nextChart();
        x = 80;
        assertWave("aspire", chart, EngineChart.SPARK_1, 40.0 / 360, x, x + 180, x + 360, x + 540);
        sendCommand("set cranking_charge_angle 65");

        IoUtil.changeRpm(600);
        sendComplexCommand("set cranking_rpm 700");
        chart = nextChart();
        x = 55;
        assertWave("cranking@600", chart, EngineChart.SPARK_1, 0.18, x, x + 180, x + 360, x + 540);

        IoUtil.changeRpm(2000);
        sendCommand("set_whole_fuel_map 1.57");

        IoUtil.changeRpm(2600);
        IoUtil.changeRpm(2000);
        chart = nextChart();

        msg = "aspire running";

        assertWaveFall(msg, chart, EngineChart.INJECTOR_1, 0.109, 238.75);
        assertWaveFall(msg, chart, EngineChart.INJECTOR_2, 0.109, 53.04);
        assertWaveFall(msg, chart, EngineChart.INJECTOR_3, 0.109, 417.04);
        assertWaveFall(msg, chart, EngineChart.INJECTOR_4, 0.109, 594.04);

        x = 7;
        assertWave(chart, EngineChart.SPARK_1, 0.133, x, x + 180, x + 360, x + 540);

        sendCommand("set_fuel_map 2200 4 15.66");
        sendCommand("set_fuel_map 2000 4 15.66");
        sendCommand("set_fuel_map 2200 4.2 15.66");
        sendCommand("set_fuel_map 2000 4.2 15.66");
        // mock 2 means 4 on the gauge because of the divider. should we simplify this?
        sendCommand("set " + MOCK_MAF_COMMAND + " 2");
        sendComplexCommand("set global_trigger_offset_angle 175");
        chart = nextChart();

        assertWaveFall(msg + " fuel", chart, EngineChart.INJECTOR_1, 0.763, 238.75);
        assertWaveFall(msg + " fuel", chart, EngineChart.INJECTOR_2, 0.763, 53.04);
        assertWaveFall(msg + " fuel", chart, EngineChart.INJECTOR_3, 0.763, 417.04);
        assertWaveFall(msg + " fuel", chart, EngineChart.INJECTOR_4, 0.763, 594.04);

        x = 33.0;
        assertWave(chart, EngineChart.SPARK_1, 0.133, x, x + 180, x + 360, x + 540);
        assertWaveNull(chart, EngineChart.SPARK_2);

        sendComplexCommand("set global_trigger_offset_angle 130");
        sendComplexCommand("set injection_offset 369");
        chart = nextChart();
        x = 33;
        assertWave(chart, EngineChart.SPARK_1, 0.133, x, x + 180, x + 360, x + 540);

        // let's enable more channels dynamically
        sendComplexCommand("set_individual_coils_ignition");
        chart = nextChart();
        assertWave("Switching Aspire into INDIVIDUAL_COILS mode", chart, EngineChart.SPARK_2, 0.133, x + 540);
        assertWave(chart, EngineChart.SPARK_3, 0.133, x + 180);

        sendCommand("set_whole_timing_map 520");
        chart = nextChart();
        x = 58.92;
        assertWave(chart, EngineChart.SPARK_2, 0.133, x);

        // switching to Speed Density
        sendCommand("set mock_maf_voltage 2");
        sendComplexCommand("set algorithm 3");
        IoUtil.changeRpm(2400);
        IoUtil.changeRpm(2000);
        chart = nextChart();
        assertEquals("MAP", 69.12, SensorCentral.getInstance().getValue(Sensor.MAP));
        //assertEquals(1, SensorCentral.getInstance().getValue(Sensor.));
        x = 8.88;
        assertWave(false, msg + " fuel SD #1", chart, EngineChart.INJECTOR_1, 0.577, 0.1, 0.1, x + 180);
        assertWave(false, msg + " fuel SD #2", chart, EngineChart.INJECTOR_2, 0.577, 0.1, 0.1, x);
        assertWave(false, msg + " fuel SD #3", chart, EngineChart.INJECTOR_3, 0.577, 0.1, 0.1, x + 360);
        assertWave(false, msg + " fuel SD #4", chart, EngineChart.INJECTOR_4, 0.577, 0.1, 0.1, x + 540);

        // above hard limit
        IoUtil.changeRpm(10000);
        chart = nextChart();
        assertWaveNull("hard limit check", chart, EngineChart.INJECTOR_1);
    }

    private void sendCommand(String command) {
        sendCommand(command, CommandQueue.DEFAULT_TIMEOUT, Timeouts.CMD_TIMEOUT);
    }

    private void sendCommand(String command, int retryTimeoutMs, int timeoutMs) {
        assertNull("Fatal not expected", criticalError);
        IoUtil.sendCommand(command, retryTimeoutMs, timeoutMs, commandQueue);
    }

    private static void assertEquals(double expected, double actual) {
        assertEquals("", expected, actual);
    }

    private static void assertEquals(String msg, double expected, double actual) {
        assertEquals(msg, expected, actual, EngineReport.RATIO);
    }

    private static void assertEquals(String msg, double expected, double actual, double ratio) {
        if (!isCloseEnough(expected, actual, ratio))
            throw new IllegalStateException(msg + " Expected " + expected + " but got " + actual);
    }

    /**
     * This method waits for longer then usual.
     */
    private void sendComplexCommand(String command) {
        sendCommand(command, COMPLEX_COMMAND_RETRY, Timeouts.CMD_TIMEOUT);
    }

    private static void assertWaveNull(EngineChart chart, String key) {
        assertWaveNull("", chart, key);
    }

    private static void assertWaveNull(String msg, EngineChart chart, String key) {
        assertNull(msg + "chart for " + key, chart.get(key));
    }

    public static void main(String[] args) throws InterruptedException {
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                System.exit(-1);
            }
        });
        boolean startSimulator = args.length == 1 && args[0].equalsIgnoreCase("start");

        long start = System.currentTimeMillis();
        FileLog.SIMULATOR_CONSOLE.start();
        FileLog.MAIN.start();

        boolean failed = false;
        try {
            LinkManager linkManager = new LinkManager();
            IoUtil.connectToSimulator(linkManager, startSimulator);
            new AutoTest(linkManager, CommandQueue.getInstance()).mainTestBody();
        } catch (Throwable e) {
            e.printStackTrace();
            failed = true;
        } finally {
            SimulatorExecHelper.destroy();
        }
        if (failed)
            System.exit(-1);
        FileLog.MAIN.logLine("*******************************************************************************");
        FileLog.MAIN.logLine("************************************  Looks good! *****************************");
        FileLog.MAIN.logLine("*******************************************************************************");
        long time = (System.currentTimeMillis() - start) / 1000;
        FileLog.MAIN.logLine("Done in " + time + "secs");
        System.exit(0); // this is a safer method eliminating the issue of non-daemon threads
    }
}
