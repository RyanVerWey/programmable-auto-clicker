package com.autoclicker;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultFormatter;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.BaseTSD.ULONG_PTR;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LONG;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinDef.WORD;
import com.sun.jna.platform.win32.WinUser;

public final class ProgrammableAutoClicker {
    private static final String APP_NAME = "Programmable Auto Clicker";
    private static final String APP_VERSION = "1.0.0";
    private static final Color SURFACE = new Color(245, 244, 240);
    private static final Color PANEL = new Color(232, 227, 219);
    private static final Color TEXT = new Color(37, 35, 31);
    private static final Color MUTED = new Color(105, 100, 91);
    private static final Color ACCENT = new Color(40, 111, 108);
    private static final Color DANGER = new Color(157, 52, 52);

    private final JFrame frame = new JFrame(APP_NAME + " v" + APP_VERSION);
    private final JLabel status = valueLabel("Idle", 18);
    private final JLabel clicks = valueLabel("0", 18);
    private final JLabel elapsed = valueLabel("0.0s", 18);
    private final JButton startButton = primaryButton("Start", ACCENT);
    private final JButton stopButton = primaryButton("Stop", DANGER);
    private final JComboBox<String> actionType = new JComboBox<>(new String[] {"keyboard", "mouse", "mouse + keyboard"});

    private final JRadioButton locationFixed = new JRadioButton("Fixed position");
    private final JRadioButton locationCurrent = new JRadioButton("Current cursor", true);
    private final JRadioButton locationRandomRect = new JRadioButton("Random rectangle");
    private final JSpinner x = intSpinner(500, 0, 100000, 1);
    private final JSpinner y = intSpinner(500, 0, 100000, 1);
    private final JSpinner randomLeft = intSpinner(400, 0, 100000, 1);
    private final JSpinner randomTop = intSpinner(300, 0, 100000, 1);
    private final JSpinner randomRight = intSpinner(700, 0, 100000, 1);
    private final JSpinner randomBottom = intSpinner(500, 0, 100000, 1);
    private final JSpinner positionJitter = intSpinner(0, 0, 10000, 1);

    private final JComboBox<String> inputMode = new JComboBox<>(new String[] {"Windows native (games)", "Java Robot (desktop)"});
    private final JComboBox<String> button = new JComboBox<>(new String[] {"left", "right", "middle"});
    private final JComboBox<String> clickMode = new JComboBox<>(new String[] {"single", "double", "hold", "burst"});
    private final JSpinner holdMs = intSpinner(0, 0, 60000, 5);
    private final JSpinner doubleGapMs = intSpinner(80, 0, 10000, 5);
    private final JSpinner burstCount = intSpinner(3, 2, 1000, 1);
    private final JSpinner burstGapMs = intSpinner(45, 0, 10000, 5);
    private final KeyCaptureField keyboardKey = new KeyCaptureField(KeyEvent.VK_F, this::suspendGlobalHotkey, this::registerGlobalHotkey);
    private final JComboBox<String> keyMode = new JComboBox<>(new String[] {"tap", "hold", "burst"});
    private final JSpinner keyHoldMs = intSpinner(50, 0, 60000, 5);
    private final JSpinner keyBurstCount = intSpinner(3, 2, 1000, 1);
    private final JSpinner keyBurstGapMs = intSpinner(45, 0, 10000, 5);

    private final JRadioButton rateCps = new JRadioButton("Clicks per second", true);
    private final JRadioButton rateInterval = new JRadioButton("Interval");
    private final JSpinner cps = doubleSpinner(8.0, 0.01, 1000.0, 0.25);
    private final JSpinner intervalMs = intSpinner(125, 1, 3_600_000, 1);
    private final JSpinner timingJitterMs = intSpinner(0, 0, 3_600_000, 1);

    private final JSpinner startDelayS = doubleSpinner(0.0, 0.0, 86_400.0, 0.25);
    private final JRadioButton stopManual = new JRadioButton("Manual", true);
    private final JRadioButton stopDuration = new JRadioButton("Duration");
    private final JRadioButton stopClicks = new JRadioButton("Clicks");
    private final JSpinner durationS = doubleSpinner(30.0, 0.01, 86_400.0, 1.0);
    private final JSpinner maxClicks = intSpinner(100, 1, Integer.MAX_VALUE, 1);
    private final JCheckBox randomBreaks = new JCheckBox("Enabled");
    private final JSpinner breakAfterMin = intSpinner(50, 1, Integer.MAX_VALUE, 1);
    private final JSpinner breakAfterMax = intSpinner(150, 1, Integer.MAX_VALUE, 1);
    private final JSpinner breakMinMs = intSpinner(300, 0, 3_600_000, 10);
    private final JSpinner breakMaxMs = intSpinner(1200, 0, 3_600_000, 10);
    private final JCheckBox failSafeCorner = new JCheckBox("Fail-safe top-left corner", true);
    private final JComboBox<HotkeyChoice> startStopHotkey = new JComboBox<>(HotkeyChoice.defaults());
    private final JLabel hotkeyState = mutedLabel("Global hotkey: not registered");

    private final ClickEngine engine;
    private GlobalHotkeyManager hotkeyManager;
    private final Timer uiTimer;

    public ProgrammableAutoClicker() throws AWTException {
        this.actionType.setSelectedItem("keyboard");
        this.engine = new ClickEngine(new Robot(), this::engineUpdate);
        this.uiTimer = new Timer(125, event -> refreshStats());
        this.uiTimer.start();
        installLookAndFeel();
        buildUi();
        bindActions();
    }

    public static void main(String[] args) {
        if (args.length == 1 && "--validate-native-input".equals(args[0])) {
            ClickEngine.validateNativeMouseInput();
            System.out.println("Windows native mouse and F-key input are available.");
            return;
        }
        if (args.length == 1 && "--send-native-f-once".equals(args[0])) {
            ClickEngine.sendNativeKeyTap(KeyEvent.VK_F, 50);
            System.out.println("Sent one native F-key tap.");
            return;
        }
        if (args.length == 1 && "--send-native-f-burst".equals(args[0])) {
            for (int i = 0; i < 10; i++) {
                ClickEngine.sendNativeKeyTap(KeyEvent.VK_F, 50);
                ClickEngine.sleepNativeTest(50);
            }
            System.out.println("Sent ten native F-key taps.");
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                new ProgrammableAutoClicker().show();
            } catch (AWTException ex) {
                JOptionPane.showMessageDialog(
                    null,
                    "Robot control is unavailable on this machine: " + ex.getMessage(),
                    APP_NAME,
                    JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    private void show() {
        frame.setVisible(true);
    }

    private void buildUi() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (hotkeyManager != null) {
                    hotkeyManager.close();
                }
                engine.stop();
            }
        });
        frame.setMinimumSize(new Dimension(960, 650));
        frame.setSize(1040, 700);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(SURFACE);
        frame.setLayout(new BorderLayout(16, 16));

        JPanel side = sidePanel();
        JTabbedPane tabs = new JTabbedPane();
        configureButtonGroups();
        tabs.addTab("Easy Setup", easySetupTab());
        tabs.addTab("Advanced", advancedTab());
        tabs.addTab("Runtime", runtimeTab());

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(SURFACE);
        content.setBorder(BorderFactory.createEmptyBorder(18, 0, 18, 18));
        content.add(tabs, BorderLayout.CENTER);

        frame.add(side, BorderLayout.WEST);
        frame.add(content, BorderLayout.CENTER);
        installShortcuts(frame.getRootPane());
        stopButton.setEnabled(false);
    }

    private void configureButtonGroups() {
        ButtonGroup location = new ButtonGroup();
        location.add(locationFixed);
        location.add(locationCurrent);
        location.add(locationRandomRect);

        ButtonGroup rate = new ButtonGroup();
        rate.add(rateCps);
        rate.add(rateInterval);

        ButtonGroup stop = new ButtonGroup();
        stop.add(stopManual);
        stop.add(stopDuration);
        stop.add(stopClicks);
    }

    private JPanel sidePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(270, 1));
        panel.setBackground(PANEL);
        panel.setBorder(BorderFactory.createEmptyBorder(22, 20, 22, 20));
        GridBagConstraints c = baseConstraints();
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("<html>Programmable<br>Macro Clicker</html>");
        title.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 24));
        title.setForeground(TEXT);
        panel.add(title, c);

        c.gridy++;
        c.insets = new Insets(30, 0, 0, 0);
        panel.add(status, c);
        c.gridy++;
        c.insets = new Insets(2, 0, 20, 0);
        panel.add(mutedLabel("Status"), c);

        JPanel stats = new JPanel(new GridBagLayout());
        stats.setBackground(PANEL);
        GridBagConstraints sc = baseConstraints();
        sc.weightx = 1;
        sc.fill = GridBagConstraints.HORIZONTAL;
        stats.add(clicks, sc);
        sc.gridx = 1;
        stats.add(elapsed, sc);
        sc.gridy = 1;
        sc.gridx = 0;
        stats.add(mutedLabel("Actions"), sc);
        sc.gridx = 1;
        stats.add(mutedLabel("Elapsed"), sc);
        c.gridy++;
        c.insets = new Insets(0, 0, 22, 0);
        panel.add(stats, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 8, 0);
        panel.add(startButton, c);
        c.gridy++;
        panel.add(stopButton, c);

        JButton capture = plainButton("Capture Position");
        capture.addActionListener(event -> capturePosition());
        c.gridy++;
        c.insets = new Insets(0, 0, 18, 0);
        panel.add(capture, c);

        JButton save = plainButton("Save Profile");
        save.addActionListener(event -> saveProfile());
        JButton load = plainButton("Load Profile");
        load.addActionListener(event -> loadProfile());
        c.gridy++;
        c.insets = new Insets(0, 0, 8, 0);
        panel.add(save, c);
        c.gridy++;
        panel.add(load, c);

        c.gridy++;
        c.weighty = 1;
        panel.add(new JPanel(null), c);

        c.gridy++;
        c.weighty = 0;
        JLabel shortcut = mutedLabel(
            "<html>v" + APP_VERSION
                + " | Start/stop hotkey is global.<br>F8 captures position when app is focused.</html>"
        );
        panel.add(shortcut, c);
        return panel;
    }

    private JPanel easySetupTab() {
        JPanel panel = tabPanel();
        addHeading(panel, "Ready to go", 0);
        addFull(panel, fieldGroup("Keyboard macro", field("Button to press", keyboardKey), field("Mode", keyMode), field("Run", actionType), field("Input", inputMode)), 1);
        addFull(panel, fieldGroup("Speed", row(rateCps, rateInterval), field("CPS", cps), field("Interval ms", intervalMs), field("+/- random ms", timingJitterMs)), 2);
        addFull(panel, fieldGroup("Start / stop", field("Hotkey", startStopHotkey), hotkeyState), 3);
        return panel;
    }

    private JPanel advancedTab() {
        JPanel panel = tabPanel();
        addHeading(panel, "Advanced", 0);
        addFull(panel, fieldGroup("Random rectangle", field("Left", randomLeft), field("Top", randomTop), field("Right", randomRight), field("Bottom", randomBottom)), 1);
        addFull(panel, fieldGroup("Mouse details", field("Hold ms", holdMs), field("Double gap ms", doubleGapMs), field("Burst count", burstCount), field("Burst gap ms", burstGapMs)), 2);
        addFull(panel, fieldGroup("Keyboard details", field("Hold ms", keyHoldMs), field("Burst count", keyBurstCount), field("Burst gap ms", keyBurstGapMs)), 3);
        return panel;
    }

    private JPanel locationTab() {
        JPanel panel = tabPanel();
        addHeading(panel, "Target", 0);
        JPanel modes = row(locationFixed, locationCurrent, locationRandomRect);
        ButtonGroup group = new ButtonGroup();
        group.add(locationFixed);
        group.add(locationCurrent);
        group.add(locationRandomRect);
        addFull(panel, modes, 1);
        addFull(panel, fieldGroup("Fixed position", field("X", x), field("Y", y)), 2);
        addFull(panel, fieldGroup("Random rectangle", field("Left", randomLeft), field("Top", randomTop), field("Right", randomRight), field("Bottom", randomBottom)), 3);
        addFull(panel, fieldGroup("Per-click location offset", field("Random px", positionJitter)), 4);
        return panel;
    }

    private JPanel clickTab() {
        JPanel panel = tabPanel();
        addHeading(panel, "Mouse action", 0);
        addFull(panel, fieldGroup("Action type", field("Run", actionType)), 1);
        addFull(panel, fieldGroup("Button and mode", field("Button", button), field("Mode", clickMode)), 2);
        addFull(panel, fieldGroup("Click details", field("Hold ms", holdMs), field("Double gap ms", doubleGapMs), field("Burst count", burstCount), field("Burst gap ms", burstGapMs)), 3);
        return panel;
    }

    private JPanel keyboardTab() {
        JPanel panel = tabPanel();
        addHeading(panel, "Keyboard action", 0);
        addFull(panel, fieldGroup("Key", field("Button to press", keyboardKey), field("Mode", keyMode)), 1);
        addFull(panel, fieldGroup("Key details", field("Hold ms", keyHoldMs), field("Burst count", keyBurstCount), field("Burst gap ms", keyBurstGapMs)), 2);
        return panel;
    }

    private JPanel rateTab() {
        JPanel panel = tabPanel();
        addHeading(panel, "Timing", 0);
        ButtonGroup group = new ButtonGroup();
        group.add(rateCps);
        group.add(rateInterval);
        addFull(panel, row(rateCps, rateInterval), 1);
        addFull(panel, fieldGroup("Rate", field("CPS", cps), field("Interval ms", intervalMs)), 2);
        addFull(panel, fieldGroup("Humanization", field("+/- random ms", timingJitterMs)), 3);
        return panel;
    }

    private JPanel runtimeTab() {
        JPanel panel = tabPanel();
        addHeading(panel, "Run control", 0);
        addFull(panel, fieldGroup("Stop", stopManual, stopDuration, stopClicks), 1);
        addFull(panel, fieldGroup("Limits", field("Delay s", startDelayS), field("Duration s", durationS), field("Max clicks", maxClicks), failSafeCorner), 2);
        addFull(panel, fieldGroup("Random breaks", randomBreaks, field("After min", breakAfterMin), field("After max", breakAfterMax), field("Break min ms", breakMinMs), field("Break max ms", breakMaxMs)), 3);
        return panel;
    }

    private void bindActions() {
        startButton.addActionListener(event -> start());
        stopButton.addActionListener(event -> stop());
        startStopHotkey.addActionListener(event -> registerGlobalHotkey());
        javax.swing.event.ChangeListener rateChange = event -> updateEngineRate();
        cps.addChangeListener(rateChange);
        intervalMs.addChangeListener(rateChange);
        timingJitterMs.addChangeListener(rateChange);
        rateCps.addActionListener(event -> updateEngineRate());
        rateInterval.addActionListener(event -> updateEngineRate());
        updateEngineRate();
        registerGlobalHotkey();
    }

    private void updateEngineRate() {
        engine.updateRate(
            rateCps.isSelected() ? "cps" : "interval",
            doubleValue(cps),
            intValue(intervalMs),
            intValue(timingJitterMs)
        );
    }

    private void registerGlobalHotkey() {
        if (hotkeyManager != null) {
            hotkeyManager.close();
            hotkeyManager = null;
        }
        HotkeyChoice hotkey = (HotkeyChoice) startStopHotkey.getSelectedItem();
        if (hotkey == null) {
            return;
        }
        hotkeyManager = new GlobalHotkeyManager(hotkey, () -> SwingUtilities.invokeLater(this::toggleRunning));
        if (hotkeyManager.start()) {
            hotkeyState.setText("Global hotkey: " + hotkey.label);
        } else {
            hotkeyState.setText("Global hotkey unavailable, app focus still works");
        }
    }

    private void suspendGlobalHotkey() {
        if (hotkeyManager != null) {
            hotkeyManager.close();
            hotkeyManager = null;
        }
        hotkeyState.setText("Global hotkey paused for key capture");
    }

    private void toggleRunning() {
        if (engine.isRunning()) {
            stop();
        } else {
            start();
        }
    }

    private void installShortcuts(JComponent root) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "toggle");
        root.getActionMap().put("toggle", new SimpleAction(this::toggleRunning));
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "stop");
        root.getActionMap().put("stop", new SimpleAction(this::stop));
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "capture");
        root.getActionMap().put("capture", new SimpleAction(this::capturePosition));
    }

    private void start() {
        if (keyboardKey.isCapturing()) {
            return;
        }
        try {
            ClickSettings settings = readSettings();
            if (engine.start(settings)) {
                status.setText("Starting");
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), APP_NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void stop() {
        engine.stop();
        status.setText("Stopping");
    }

    private void capturePosition() {
        Point p = MouseInfo.getPointerInfo().getLocation();
        x.setValue(p.x);
        y.setValue(p.y);
        locationFixed.setSelected(true);
        status.setText("Captured " + p.x + ", " + p.y);
    }

    private ClickSettings readSettings() {
        ClickSettings settings = new ClickSettings();
        settings.actionType = (String) actionType.getSelectedItem();
        settings.locationMode = selectedLocationMode();
        settings.x = intValue(x);
        settings.y = intValue(y);
        settings.randomLeft = intValue(randomLeft);
        settings.randomTop = intValue(randomTop);
        settings.randomRight = intValue(randomRight);
        settings.randomBottom = intValue(randomBottom);
        settings.positionJitterPx = intValue(positionJitter);
        settings.inputMode = inputMode.getSelectedIndex() == 0 ? "windows_native" : "java_robot";
        settings.button = (String) button.getSelectedItem();
        settings.clickMode = (String) clickMode.getSelectedItem();
        settings.holdMs = intValue(holdMs);
        settings.doubleGapMs = intValue(doubleGapMs);
        settings.burstCount = intValue(burstCount);
        settings.burstGapMs = intValue(burstGapMs);
        settings.keyboardKeyCode = keyboardKey.keyCode();
        settings.keyboardKeyName = keyboardKey.keyName();
        settings.keyMode = (String) keyMode.getSelectedItem();
        settings.keyHoldMs = intValue(keyHoldMs);
        settings.keyBurstCount = intValue(keyBurstCount);
        settings.keyBurstGapMs = intValue(keyBurstGapMs);
        settings.rateMode = rateCps.isSelected() ? "cps" : "interval";
        settings.cps = doubleValue(cps);
        settings.intervalMs = intValue(intervalMs);
        settings.timingJitterMs = intValue(timingJitterMs);
        settings.startDelayS = doubleValue(startDelayS);
        settings.stopMode = selectedStopMode();
        settings.durationS = doubleValue(durationS);
        settings.maxClicks = intValue(maxClicks);
        settings.randomBreaks = randomBreaks.isSelected();
        settings.breakAfterMin = intValue(breakAfterMin);
        settings.breakAfterMax = intValue(breakAfterMax);
        settings.breakMinMs = intValue(breakMinMs);
        settings.breakMaxMs = intValue(breakMaxMs);
        settings.failSafeCorner = failSafeCorner.isSelected();
        settings.hotkeyCode = ((HotkeyChoice) startStopHotkey.getSelectedItem()).vkCode;
        settings.validate();
        return settings;
    }

    private String selectedLocationMode() {
        if (locationCurrent.isSelected()) {
            return "current";
        }
        if (locationRandomRect.isSelected()) {
            return "random_rect";
        }
        return "fixed";
    }

    private String selectedStopMode() {
        if (stopDuration.isSelected()) {
            return "duration";
        }
        if (stopClicks.isSelected()) {
            return "clicks";
        }
        return "manual";
    }

    private void saveProfile() {
        try {
            ClickSettings settings = readSettings();
            JFileChooser chooser = profileChooser();
            if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            Path path = chooser.getSelectedFile().toPath();
            if (!path.toString().toLowerCase().endsWith(".profile")) {
                path = Path.of(path + ".profile");
            }
            Files.writeString(path, settings.toProfile(), StandardCharsets.UTF_8);
            status.setText("Profile saved");
        } catch (IOException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), APP_NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadProfile() {
        JFileChooser chooser = profileChooser();
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            ClickSettings settings = ClickSettings.fromProfile(Files.readString(chooser.getSelectedFile().toPath(), StandardCharsets.UTF_8));
            applySettings(settings);
            status.setText("Profile loaded");
        } catch (IOException | IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(frame, ex.getMessage(), APP_NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    private JFileChooser profileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Auto clicker profile", "profile"));
        return chooser;
    }

    private void applySettings(ClickSettings settings) {
        actionType.setSelectedItem(settings.actionType);
        locationFixed.setSelected("fixed".equals(settings.locationMode));
        locationCurrent.setSelected("current".equals(settings.locationMode));
        locationRandomRect.setSelected("random_rect".equals(settings.locationMode));
        x.setValue(settings.x);
        y.setValue(settings.y);
        randomLeft.setValue(settings.randomLeft);
        randomTop.setValue(settings.randomTop);
        randomRight.setValue(settings.randomRight);
        randomBottom.setValue(settings.randomBottom);
        positionJitter.setValue(settings.positionJitterPx);
        inputMode.setSelectedIndex("java_robot".equals(settings.inputMode) ? 1 : 0);
        button.setSelectedItem(settings.button);
        clickMode.setSelectedItem(settings.clickMode);
        holdMs.setValue(settings.holdMs);
        doubleGapMs.setValue(settings.doubleGapMs);
        burstCount.setValue(settings.burstCount);
        burstGapMs.setValue(settings.burstGapMs);
        keyboardKey.setKeyCode(settings.keyboardKeyCode);
        keyMode.setSelectedItem(settings.keyMode);
        keyHoldMs.setValue(settings.keyHoldMs);
        keyBurstCount.setValue(settings.keyBurstCount);
        keyBurstGapMs.setValue(settings.keyBurstGapMs);
        rateCps.setSelected("cps".equals(settings.rateMode));
        rateInterval.setSelected("interval".equals(settings.rateMode));
        cps.setValue(settings.cps);
        intervalMs.setValue(settings.intervalMs);
        timingJitterMs.setValue(settings.timingJitterMs);
        startDelayS.setValue(settings.startDelayS);
        stopManual.setSelected("manual".equals(settings.stopMode));
        stopDuration.setSelected("duration".equals(settings.stopMode));
        stopClicks.setSelected("clicks".equals(settings.stopMode));
        durationS.setValue(settings.durationS);
        maxClicks.setValue(settings.maxClicks);
        randomBreaks.setSelected(settings.randomBreaks);
        breakAfterMin.setValue(settings.breakAfterMin);
        breakAfterMax.setValue(settings.breakAfterMax);
        breakMinMs.setValue(settings.breakMinMs);
        breakMaxMs.setValue(settings.breakMaxMs);
        failSafeCorner.setSelected(settings.failSafeCorner);
        startStopHotkey.setSelectedItem(HotkeyChoice.byCode(settings.hotkeyCode));
        registerGlobalHotkey();
    }

    private void engineUpdate(String message) {
        SwingUtilities.invokeLater(() -> {
            status.setText(message);
            if (!engine.isRunning()) {
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
            }
            refreshStats();
        });
    }

    private void refreshStats() {
        clicks.setText(String.valueOf(engine.clickCount()));
        elapsed.setText(String.format("%.1fs", engine.elapsed().toMillis() / 1000.0));
    }

    private static JPanel tabPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        return panel;
    }

    private static JPanel fieldGroup(String title, JComponent... components) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(201, 192, 180)), title),
            BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));
        GridBagConstraints c = baseConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 4, 6, 18);
        for (int i = 0; i < components.length; i++) {
            c.gridx = i % 2;
            c.gridy = i / 2;
            c.weightx = 1;
            panel.add(components[i], c);
        }
        return panel;
    }

    private static JPanel field(String label, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setBackground(SURFACE);
        JLabel l = new JLabel(label);
        l.setForeground(TEXT);
        panel.add(l, BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private static JPanel row(JComponent... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        panel.setBackground(SURFACE);
        for (JComponent component : components) {
            panel.add(component);
        }
        return panel;
    }

    private static void addHeading(JPanel panel, String text, int row) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 18));
        label.setForeground(TEXT);
        addFull(panel, label, row);
    }

    private static void addFull(JPanel panel, JComponent component, int row) {
        GridBagConstraints c = baseConstraints();
        c.gridy = row;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = row == 0 ? new Insets(0, 0, 14, 0) : new Insets(0, 0, 18, 0);
        panel.add(component, c);
        if (row > 3) {
            c.weighty = 1;
        }
    }

    private static GridBagConstraints baseConstraints() {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        return c;
    }

    private static JButton primaryButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(new Color(250, 248, 244));
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        return button;
    }

    private static JButton plainButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(215, 208, 198));
        button.setForeground(TEXT);
        button.setFocusPainted(false);
        return button;
    }

    private static JLabel mutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setBackground(PANEL);
        return label;
    }

    private static JLabel valueLabel(String text, int size) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI Semibold", Font.PLAIN, size));
        label.setForeground(TEXT);
        label.setHorizontalAlignment(SwingConstants.LEFT);
        return label;
    }

    private static JSpinner intSpinner(int value, int min, int max, int step) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        spinner.setPreferredSize(new Dimension(130, 32));
        commitOnValidEdit(spinner);
        return spinner;
    }

    private static JSpinner doubleSpinner(double value, double min, double max, double step) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, min, max, step));
        spinner.setPreferredSize(new Dimension(130, 32));
        commitOnValidEdit(spinner);
        return spinner;
    }

    private static void commitOnValidEdit(JSpinner spinner) {
        if (spinner.getEditor() instanceof JSpinner.DefaultEditor editor
            && editor.getTextField().getFormatter() instanceof DefaultFormatter formatter) {
            formatter.setCommitsOnValidEdit(true);
        }
    }

    private static int intValue(JSpinner spinner) {
        return ((Number) spinner.getValue()).intValue();
    }

    private static double doubleValue(JSpinner spinner) {
        return ((Number) spinner.getValue()).doubleValue();
    }

    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Swing's cross-platform look and feel is acceptable as a fallback.
        }
        UIManager.put("Panel.background", SURFACE);
        UIManager.put("TabbedPane.background", SURFACE);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Button.disabledText", new Color(230, 225, 218));
        UIManager.put("RadioButton.background", SURFACE);
        UIManager.put("CheckBox.background", SURFACE);
    }

    private static final class SimpleAction extends javax.swing.AbstractAction {
        private final Runnable runnable;

        private SimpleAction(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void actionPerformed(java.awt.event.ActionEvent event) {
            runnable.run();
        }
    }

    private static final class ClickEngine {
        private final Robot robot;
        private final Random random = new Random();
        private final AtomicBoolean stop = new AtomicBoolean(false);
        private final EngineListener listener;
        private final ElapsedClock elapsedClock = new ElapsedClock();
        private Thread worker;
        private long clickCount;
        private volatile String liveRateMode = "cps";
        private volatile double liveCps = 8.0;
        private volatile int liveIntervalMs = 125;
        private volatile int liveTimingJitterMs = 0;

        private ClickEngine(Robot robot, EngineListener listener) {
            this.robot = robot;
            this.listener = listener;
            this.robot.setAutoDelay(0);
        }

        synchronized boolean start(ClickSettings settings) {
            if (isRunning()) {
                return false;
            }
            updateRate(settings.rateMode, settings.cps, settings.intervalMs, settings.timingJitterMs);
            stop.set(false);
            clickCount = 0;
            elapsedClock.reset();
            worker = new Thread(() -> run(settings), "auto-clicker-worker");
            worker.setDaemon(true);
            worker.start();
            return true;
        }

        void updateRate(String rateMode, double cps, int intervalMs, int timingJitterMs) {
            this.liveRateMode = rateMode;
            this.liveCps = Math.max(0.01, cps);
            this.liveIntervalMs = Math.max(1, intervalMs);
            this.liveTimingJitterMs = Math.max(0, timingJitterMs);
        }

        void stop() {
            stop.set(true);
        }

        boolean isRunning() {
            return worker != null && worker.isAlive();
        }

        long clickCount() {
            return clickCount;
        }

        Duration elapsed() {
            return elapsedClock.elapsed();
        }

        private void run(ClickSettings settings) {
            listener.onUpdate("Starting");
            sleepMillis((long) (settings.startDelayS * 1000));
            if (stop.get()) {
                finish("Stopped");
                return;
            }

            elapsedClock.start();
            int nextBreakAt = randomBetween(settings.breakAfterMin, settings.breakAfterMax);
            listener.onUpdate("Running");

            while (!stop.get()) {
                if (shouldStop(settings) || failSafe(settings)) {
                    break;
                }
                long actionStartedAt = System.nanoTime();
                try {
                    performAction(settings);
                } catch (RuntimeException ex) {
                    finish("Input error: " + ex.getMessage());
                    return;
                }
                clickCount++;

                if (settings.randomBreaks && clickCount >= nextBreakAt) {
                    int breakMs = randomBetween(settings.breakMinMs, settings.breakMaxMs);
                    listener.onUpdate("Break " + breakMs + "ms");
                    sleepMillis(breakMs);
                    nextBreakAt = (int) clickCount + randomBetween(settings.breakAfterMin, settings.breakAfterMax);
                    listener.onUpdate("Running");
                }

                long actionElapsedMs = (System.nanoTime() - actionStartedAt + 999_999L) / 1_000_000L;
                sleepMillis(Math.max(0, nextDelayMs() - actionElapsedMs));
            }
            finish(failSafe(settings) ? "Stopped by fail-safe" : "Stopped");
        }

        private void finish(String message) {
            elapsedClock.finish();
            stop.set(true);
            listener.onUpdate(message);
        }

        private boolean shouldStop(ClickSettings settings) {
            if ("duration".equals(settings.stopMode) && elapsed().toMillis() >= (long) (settings.durationS * 1000)) {
                return true;
            }
            return "clicks".equals(settings.stopMode) && clickCount >= settings.maxClicks;
        }

        private boolean failSafe(ClickSettings settings) {
            if (!settings.failSafeCorner) {
                return false;
            }
            Point p = MouseInfo.getPointerInfo().getLocation();
            return p.x <= 5 && p.y <= 5;
        }

        private void performAction(ClickSettings settings) {
            if (settings.actionType.contains("mouse")) {
                click(settings);
            }
            if (settings.actionType.contains("keyboard")) {
                pressKey(settings);
            }
        }

        private void click(ClickSettings settings) {
            Point p = target(settings);
            boolean nativeInput = "windows_native".equals(settings.inputMode);
            if (nativeInput) {
                if (!User32.INSTANCE.SetCursorPos(p.x, p.y)) {
                    throw new IllegalStateException("Windows could not move the cursor");
                }
            } else {
                robot.mouseMove(p.x, p.y);
            }
            int count = switch (settings.clickMode) {
                case "double" -> 2;
                case "burst" -> settings.burstCount;
                default -> 1;
            };
            int gap = "burst".equals(settings.clickMode) ? settings.burstGapMs : settings.doubleGapMs;

            for (int i = 0; i < count && !stop.get(); i++) {
                if (nativeInput) {
                    sendNativeMouseButton(settings.button, true);
                } else {
                    robot.mousePress(buttonMask(settings.button));
                }
                if ("hold".equals(settings.clickMode) || settings.holdMs > 0) {
                    sleepMillis(settings.holdMs);
                }
                if (nativeInput) {
                    sendNativeMouseButton(settings.button, false);
                } else {
                    robot.mouseRelease(buttonMask(settings.button));
                }
                if (i + 1 < count) {
                    sleepMillis(gap);
                }
            }
        }

        private void sendNativeMouseButton(String button, boolean down) {
            int flags = switch (button) {
                case "right" -> down ? 0x0008 : 0x0010;
                case "middle" -> down ? 0x0020 : 0x0040;
                default -> down ? 0x0002 : 0x0004;
            };
            sendNativeMouseEvent(flags);
        }

        private static void validateNativeMouseInput() {
            sendNativeMouseEvent(0x0001);
            if (User32.INSTANCE.MapVirtualKeyEx(KeyEvent.VK_F, 0, null) == 0) {
                throw new IllegalStateException("Windows could not map the F key");
            }
        }

        private static void sendNativeMouseEvent(int flags) {
            WinUser.INPUT[] inputs = (WinUser.INPUT[]) new WinUser.INPUT().toArray(1);
            WinUser.INPUT input = inputs[0];
            input.type = new DWORD(WinUser.INPUT.INPUT_MOUSE);
            input.input.setType(WinUser.MOUSEINPUT.class);
            input.input.mi = new WinUser.MOUSEINPUT();
            input.input.mi.dx = new LONG(0);
            input.input.mi.dy = new LONG(0);
            input.input.mi.mouseData = new DWORD(0);
            input.input.mi.dwFlags = new DWORD(flags);
            input.input.mi.time = new DWORD(0);
            input.input.mi.dwExtraInfo = new ULONG_PTR(0);
            input.write();

            int sent = User32.INSTANCE.SendInput(new DWORD(1), inputs, input.size()).intValue();
            if (sent != 1) {
                throw new IllegalStateException("Windows native input failed (" + Kernel32.INSTANCE.GetLastError() + ")");
            }
        }

        private void pressKey(ClickSettings settings) {
            int count = "burst".equals(settings.keyMode) ? settings.keyBurstCount : 1;
            for (int i = 0; i < count && !stop.get(); i++) {
                if ("windows_native".equals(settings.inputMode)) {
                    sendNativeKey(settings.keyboardKeyCode, false);
                } else {
                    robot.keyPress(settings.keyboardKeyCode);
                }
                if ("hold".equals(settings.keyMode) || settings.keyHoldMs > 0) {
                    sleepMillis(settings.keyHoldMs);
                }
                if ("windows_native".equals(settings.inputMode)) {
                    sendNativeKey(settings.keyboardKeyCode, true);
                } else {
                    robot.keyRelease(settings.keyboardKeyCode);
                }
                if (i + 1 < count) {
                    sleepMillis(settings.keyBurstGapMs);
                }
            }
        }

        private static void sendNativeKey(int javaKeyCode, boolean keyUp) {
            int virtualKey = windowsVirtualKey(javaKeyCode);
            int scanCode = User32.INSTANCE.MapVirtualKeyEx(virtualKey, 0, null);
            if (scanCode == 0) {
                throw new IllegalStateException("Windows could not map " + KeyEvent.getKeyText(javaKeyCode));
            }

            WinUser.INPUT[] inputs = (WinUser.INPUT[]) new WinUser.INPUT().toArray(1);
            WinUser.INPUT input = inputs[0];
            input.type = new DWORD(WinUser.INPUT.INPUT_KEYBOARD);
            input.input.setType(WinUser.KEYBDINPUT.class);
            input.input.ki = new WinUser.KEYBDINPUT();
            input.input.ki.wVk = new WORD(0);
            input.input.ki.wScan = new WORD(scanCode);
            int flags = WinUser.KEYBDINPUT.KEYEVENTF_SCANCODE;
            if (keyUp) {
                flags |= WinUser.KEYBDINPUT.KEYEVENTF_KEYUP;
            }
            input.input.ki.dwFlags = new DWORD(flags);
            input.input.ki.time = new DWORD(0);
            input.input.ki.dwExtraInfo = new ULONG_PTR(0);
            input.write();

            int sent = User32.INSTANCE.SendInput(new DWORD(1), inputs, input.size()).intValue();
            if (sent != 1) {
                throw new IllegalStateException("Windows native key input failed (" + Kernel32.INSTANCE.GetLastError() + ")");
            }
        }

        private static void sendNativeKeyTap(int javaKeyCode, int holdMs) {
            sendNativeKey(javaKeyCode, false);
            sleepNativeTest(holdMs);
            sendNativeKey(javaKeyCode, true);
        }

        private static void sleepNativeTest(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Native input test was interrupted", ex);
            }
        }

        private static int windowsVirtualKey(int javaKeyCode) {
            return switch (javaKeyCode) {
                case KeyEvent.VK_ENTER -> 0x0D;
                case KeyEvent.VK_DELETE -> 0x2E;
                case KeyEvent.VK_INSERT -> 0x2D;
                case KeyEvent.VK_SEMICOLON -> 0xBA;
                case KeyEvent.VK_EQUALS -> 0xBB;
                case KeyEvent.VK_COMMA -> 0xBC;
                case KeyEvent.VK_MINUS -> 0xBD;
                case KeyEvent.VK_PERIOD -> 0xBE;
                case KeyEvent.VK_SLASH -> 0xBF;
                case KeyEvent.VK_BACK_QUOTE -> 0xC0;
                case KeyEvent.VK_OPEN_BRACKET -> 0xDB;
                case KeyEvent.VK_BACK_SLASH -> 0xDC;
                case KeyEvent.VK_CLOSE_BRACKET -> 0xDD;
                case KeyEvent.VK_QUOTE -> 0xDE;
                default -> javaKeyCode;
            };
        }

        private Point target(ClickSettings settings) {
            int targetX;
            int targetY;
            if ("current".equals(settings.locationMode)) {
                Point p = MouseInfo.getPointerInfo().getLocation();
                targetX = p.x;
                targetY = p.y;
            } else if ("random_rect".equals(settings.locationMode)) {
                targetX = randomBetween(Math.min(settings.randomLeft, settings.randomRight), Math.max(settings.randomLeft, settings.randomRight));
                targetY = randomBetween(Math.min(settings.randomTop, settings.randomBottom), Math.max(settings.randomTop, settings.randomBottom));
            } else {
                targetX = settings.x;
                targetY = settings.y;
            }

            if (settings.positionJitterPx > 0) {
                targetX += randomBetween(-settings.positionJitterPx, settings.positionJitterPx);
                targetY += randomBetween(-settings.positionJitterPx, settings.positionJitterPx);
            }
            return new Point(Math.max(0, targetX), Math.max(0, targetY));
        }

        private long nextDelayMs() {
            double base = "interval".equals(liveRateMode) ? liveIntervalMs : 1000.0 / liveCps;
            double jitter = randomBetween(-liveTimingJitterMs, liveTimingJitterMs);
            return Math.max(1, Math.round(base + jitter));
        }

        private int randomBetween(int low, int high) {
            if (low == high) {
                return low;
            }
            return low + random.nextInt(high - low + 1);
        }

        private void sleepMillis(long millis) {
            long deadline = System.nanoTime() + Math.max(0, millis) * 1_000_000L;
            while (!stop.get() && System.nanoTime() < deadline) {
                long remainingMs = Math.max(1, (deadline - System.nanoTime()) / 1_000_000L);
                try {
                    Thread.sleep(Math.min(remainingMs, 50));
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    stop.set(true);
                    return;
                }
            }
        }

        private int buttonMask(String button) {
            return switch (button) {
                case "right" -> InputEvent.BUTTON3_DOWN_MASK;
                case "middle" -> InputEvent.BUTTON2_DOWN_MASK;
                default -> InputEvent.BUTTON1_DOWN_MASK;
            };
        }
    }

    private static final class ElapsedClock {
        private volatile long startedAtNanos;
        private volatile long finishedAtNanos;

        void reset() {
            startedAtNanos = 0;
            finishedAtNanos = 0;
        }

        void start() {
            startedAtNanos = System.nanoTime();
            finishedAtNanos = 0;
        }

        void finish() {
            if (startedAtNanos != 0 && finishedAtNanos == 0) {
                finishedAtNanos = System.nanoTime();
            }
        }

        Duration elapsed() {
            long started = startedAtNanos;
            if (started == 0) {
                return Duration.ZERO;
            }
            long finished = finishedAtNanos;
            long end = finished == 0 ? System.nanoTime() : finished;
            return Duration.ofNanos(Math.max(0, end - started));
        }
    }

    @FunctionalInterface
    private interface EngineListener {
        void onUpdate(String message);
    }

    private static final class GlobalHotkeyManager implements AutoCloseable {
        private static final int HOTKEY_ID = 0xAC1C;

        private final HotkeyChoice hotkey;
        private final Runnable callback;
        private final AtomicBoolean closed = new AtomicBoolean(false);
        private Thread thread;
        private int threadId;

        private GlobalHotkeyManager(HotkeyChoice hotkey, Runnable callback) {
            this.hotkey = hotkey;
            this.callback = callback;
        }

        boolean start() {
            closed.set(false);
            AtomicBoolean registered = new AtomicBoolean(false);
            Object ready = new Object();
            thread = new Thread(() -> {
                threadId = Kernel32.INSTANCE.GetCurrentThreadId();
                registered.set(User32.INSTANCE.RegisterHotKey((HWND) null, HOTKEY_ID, 0, hotkey.vkCode));
                synchronized (ready) {
                    ready.notifyAll();
                }
                if (!registered.get()) {
                    return;
                }

                WinUser.MSG msg = new WinUser.MSG();
                while (!closed.get() && User32.INSTANCE.GetMessage(msg, null, 0, 0) != 0) {
                    if (msg.message == WinUser.WM_HOTKEY && msg.wParam.intValue() == HOTKEY_ID) {
                        callback.run();
                    }
                }
                User32.INSTANCE.UnregisterHotKey(null, HOTKEY_ID);
            }, "global-hotkey-listener");
            thread.setDaemon(true);
            thread.start();

            synchronized (ready) {
                try {
                    ready.wait(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            return registered.get();
        }

        @Override
        public void close() {
            closed.set(true);
            User32.INSTANCE.UnregisterHotKey(null, HOTKEY_ID);
            if (threadId != 0) {
                User32.INSTANCE.PostThreadMessage(threadId, WinUser.WM_QUIT, new WPARAM(0), new LPARAM(0));
            }
        }
    }

    private static final class KeyCaptureField extends JButton {
        private int keyCode;
        private boolean capturing;
        private final Runnable onCaptureStart;
        private final Runnable onCaptureFinish;
        private final KeyEventDispatcher dispatcher = event -> {
            if (!capturing || event.getID() != KeyEvent.KEY_PRESSED) {
                return false;
            }
            setKeyCode(event.getKeyCode());
            finishCapture();
            return true;
        };

        private KeyCaptureField(int keyCode, Runnable onCaptureStart, Runnable onCaptureFinish) {
            this.keyCode = keyCode;
            this.onCaptureStart = onCaptureStart;
            this.onCaptureFinish = onCaptureFinish;
            setFocusPainted(false);
            setHorizontalAlignment(LEFT);
            setToolTipText("Click, then press the keyboard button to automate.");
            refreshText();
            addActionListener(event -> beginCapture());
        }

        int keyCode() {
            return keyCode;
        }

        String keyName() {
            return KeyEvent.getKeyText(keyCode);
        }

        boolean isCapturing() {
            return capturing;
        }

        void setKeyCode(int keyCode) {
            this.keyCode = keyCode;
            refreshText();
        }

        private void beginCapture() {
            if (capturing) {
                return;
            }
            capturing = true;
            onCaptureStart.run();
            setText("Press any key...");
            requestFocusInWindow();
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher);
        }

        private void finishCapture() {
            capturing = false;
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(dispatcher);
            refreshText();
            onCaptureFinish.run();
        }

        private void refreshText() {
            setText(KeyEvent.getKeyText(keyCode));
        }
    }

    private static final class HotkeyChoice {
        final String label;
        final int vkCode;

        private HotkeyChoice(String label, int vkCode) {
            this.label = label;
            this.vkCode = vkCode;
        }

        static HotkeyChoice[] defaults() {
            return new HotkeyChoice[] {
                new HotkeyChoice("F6", KeyEvent.VK_F6),
                new HotkeyChoice("F7", KeyEvent.VK_F7),
                new HotkeyChoice("F8", KeyEvent.VK_F8),
                new HotkeyChoice("F9", KeyEvent.VK_F9),
                new HotkeyChoice("F10", KeyEvent.VK_F10),
                new HotkeyChoice("F11", KeyEvent.VK_F11),
                new HotkeyChoice("F12", KeyEvent.VK_F12)
            };
        }

        static HotkeyChoice byCode(int vkCode) {
            for (HotkeyChoice choice : defaults()) {
                if (choice.vkCode == vkCode) {
                    return choice;
                }
            }
            return new HotkeyChoice(KeyEvent.getKeyText(vkCode), vkCode);
        }

        @Override
        public String toString() {
            return label;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof HotkeyChoice choice && choice.vkCode == vkCode;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(vkCode);
        }
    }

    private static final class ClickSettings {
        String actionType = "keyboard";
        String locationMode = "current";
        int x = 500;
        int y = 500;
        int randomLeft = 400;
        int randomTop = 300;
        int randomRight = 700;
        int randomBottom = 500;
        int positionJitterPx = 0;
        String inputMode = "windows_native";
        String button = "left";
        String clickMode = "single";
        int holdMs = 0;
        int doubleGapMs = 80;
        int burstCount = 3;
        int burstGapMs = 45;
        int keyboardKeyCode = KeyEvent.VK_F;
        String keyboardKeyName = "F";
        String keyMode = "tap";
        int keyHoldMs = 50;
        int keyBurstCount = 3;
        int keyBurstGapMs = 45;
        String rateMode = "cps";
        double cps = 8.0;
        int intervalMs = 125;
        int timingJitterMs = 0;
        double startDelayS = 0.0;
        String stopMode = "manual";
        double durationS = 30.0;
        int maxClicks = 100;
        boolean randomBreaks = false;
        int breakAfterMin = 50;
        int breakAfterMax = 150;
        int breakMinMs = 300;
        int breakMaxMs = 1200;
        boolean failSafeCorner = true;
        int hotkeyCode = KeyEvent.VK_F6;

        void validate() {
            if (cps <= 0) {
                throw new IllegalArgumentException("CPS must be greater than zero.");
            }
            if (intervalMs < 1) {
                throw new IllegalArgumentException("Interval must be at least 1 ms.");
            }
            if (breakMinMs > breakMaxMs) {
                throw new IllegalArgumentException("Break min ms must be less than or equal to Break max ms.");
            }
            if (breakAfterMin > breakAfterMax) {
                throw new IllegalArgumentException("After min must be less than or equal to After max.");
            }
        }

        String toProfile() {
            Map<String, String> values = asMap();
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<String, String> entry : values.entrySet()) {
                builder.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
            }
            return builder.toString();
        }

        Map<String, String> asMap() {
            Map<String, String> values = new LinkedHashMap<>();
            values.put("actionType", actionType);
            values.put("locationMode", locationMode);
            values.put("x", String.valueOf(x));
            values.put("y", String.valueOf(y));
            values.put("randomLeft", String.valueOf(randomLeft));
            values.put("randomTop", String.valueOf(randomTop));
            values.put("randomRight", String.valueOf(randomRight));
            values.put("randomBottom", String.valueOf(randomBottom));
            values.put("positionJitterPx", String.valueOf(positionJitterPx));
            values.put("inputMode", inputMode);
            values.put("button", button);
            values.put("clickMode", clickMode);
            values.put("holdMs", String.valueOf(holdMs));
            values.put("doubleGapMs", String.valueOf(doubleGapMs));
            values.put("burstCount", String.valueOf(burstCount));
            values.put("burstGapMs", String.valueOf(burstGapMs));
            values.put("keyboardKeyCode", String.valueOf(keyboardKeyCode));
            values.put("keyboardKeyName", keyboardKeyName);
            values.put("keyMode", keyMode);
            values.put("keyHoldMs", String.valueOf(keyHoldMs));
            values.put("keyBurstCount", String.valueOf(keyBurstCount));
            values.put("keyBurstGapMs", String.valueOf(keyBurstGapMs));
            values.put("rateMode", rateMode);
            values.put("cps", String.valueOf(cps));
            values.put("intervalMs", String.valueOf(intervalMs));
            values.put("timingJitterMs", String.valueOf(timingJitterMs));
            values.put("startDelayS", String.valueOf(startDelayS));
            values.put("stopMode", stopMode);
            values.put("durationS", String.valueOf(durationS));
            values.put("maxClicks", String.valueOf(maxClicks));
            values.put("randomBreaks", String.valueOf(randomBreaks));
            values.put("breakAfterMin", String.valueOf(breakAfterMin));
            values.put("breakAfterMax", String.valueOf(breakAfterMax));
            values.put("breakMinMs", String.valueOf(breakMinMs));
            values.put("breakMaxMs", String.valueOf(breakMaxMs));
            values.put("failSafeCorner", String.valueOf(failSafeCorner));
            values.put("hotkeyCode", String.valueOf(hotkeyCode));
            return values;
        }

        static ClickSettings fromProfile(String text) {
            ClickSettings settings = new ClickSettings();
            for (String line : text.split("\\R")) {
                if (line.isBlank() || line.trim().startsWith("#")) {
                    continue;
                }
                String[] parts = line.split("=", 2);
                if (parts.length != 2) {
                    continue;
                }
                settings.apply(parts[0].trim(), parts[1].trim());
            }
            settings.validate();
            return settings;
        }

        private void apply(String key, String value) {
            switch (key) {
                case "actionType" -> actionType = value;
                case "locationMode" -> locationMode = value;
                case "x" -> x = Integer.parseInt(value);
                case "y" -> y = Integer.parseInt(value);
                case "randomLeft" -> randomLeft = Integer.parseInt(value);
                case "randomTop" -> randomTop = Integer.parseInt(value);
                case "randomRight" -> randomRight = Integer.parseInt(value);
                case "randomBottom" -> randomBottom = Integer.parseInt(value);
                case "positionJitterPx" -> positionJitterPx = Integer.parseInt(value);
                case "inputMode", "mouseInputMode" -> inputMode = value;
                case "button" -> button = value;
                case "clickMode" -> clickMode = value;
                case "holdMs" -> holdMs = Integer.parseInt(value);
                case "doubleGapMs" -> doubleGapMs = Integer.parseInt(value);
                case "burstCount" -> burstCount = Integer.parseInt(value);
                case "burstGapMs" -> burstGapMs = Integer.parseInt(value);
                case "keyboardKeyCode" -> keyboardKeyCode = Integer.parseInt(value);
                case "keyboardKeyName" -> keyboardKeyName = value;
                case "keyMode" -> keyMode = value;
                case "keyHoldMs" -> keyHoldMs = Integer.parseInt(value);
                case "keyBurstCount" -> keyBurstCount = Integer.parseInt(value);
                case "keyBurstGapMs" -> keyBurstGapMs = Integer.parseInt(value);
                case "rateMode" -> rateMode = value;
                case "cps" -> cps = Double.parseDouble(value);
                case "intervalMs" -> intervalMs = Integer.parseInt(value);
                case "timingJitterMs" -> timingJitterMs = Integer.parseInt(value);
                case "startDelayS" -> startDelayS = Double.parseDouble(value);
                case "stopMode" -> stopMode = value;
                case "durationS" -> durationS = Double.parseDouble(value);
                case "maxClicks" -> maxClicks = Integer.parseInt(value);
                case "randomBreaks" -> randomBreaks = Boolean.parseBoolean(value);
                case "breakAfterMin" -> breakAfterMin = Integer.parseInt(value);
                case "breakAfterMax" -> breakAfterMax = Integer.parseInt(value);
                case "breakMinMs" -> breakMinMs = Integer.parseInt(value);
                case "breakMaxMs" -> breakMaxMs = Integer.parseInt(value);
                case "failSafeCorner" -> failSafeCorner = Boolean.parseBoolean(value);
                case "hotkeyCode" -> hotkeyCode = Integer.parseInt(value);
                default -> {
                    // Forward-compatible profiles can include newer keys.
                }
            }
        }
    }
}
