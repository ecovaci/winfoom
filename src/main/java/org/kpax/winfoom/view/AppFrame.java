/*
 * Copyright (c) 2020. Eugen Covaci
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.kpax.winfoom.view;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.kpax.winfoom.config.ProxyConfig;
import org.kpax.winfoom.exception.InvalidProxySettingsException;
import org.kpax.winfoom.proxy.ProxyBlacklist;
import org.kpax.winfoom.proxy.ProxyController;
import org.kpax.winfoom.proxy.ProxyValidator;
import org.kpax.winfoom.util.HttpUtils;
import org.kpax.winfoom.util.SwingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

@Slf4j
@Profile("gui")
@Component
public class AppFrame extends JFrame {
    private static final long serialVersionUID = 4009799697210970761L;

    private static final int ICON_SIZE = 16;

    private static final int TOOLTIP_TIMEOUT = 10;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    private ProxyConfig proxyConfig;

    @Autowired
    private ProxyController proxyController;

    @Autowired
    private ProxyValidator proxyValidator;

    @Autowired
    private ProxyBlacklist proxyBlacklist;

    private JLabel proxyTypeLabel;
    private JComboBox<ProxyConfig.Type> proxyTypeCombo;

    private JSpinner localPortJSpinner;

    private JButton btnStart;
    private JButton btnStop;
    private JButton btnTest;
    private JButton btnCancelBlacklist;
    private JButton btnAutoDetect;

    private JPanel mainContentPanel;
    private JPanel labelsFieldsPanel;
    private JPanel labelPanel;
    private JPanel fieldPanel;
    private JPanel btnPanel;

    private JMenuBar menuBar;
    private JMenu mnFile;
    private JMenu mntmSettings;

    private JCheckBoxMenuItem autoStartMenuItem;
    private JCheckBoxMenuItem autoDetectMenuItem;

    private JMenuItem mntmExit;
    private JMenu mnHelp;
    private JMenuItem mntmAbout;

    /**
     * Create the frame.
     */
    @PostConstruct
    void init() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setMinimumSize(new Dimension(250, 150));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdownApp();
            }
        });

        Image iconImage = SwingUtils.loadImage(AppFrame.class, "icon.png");
        setIconImage(iconImage);
        //
        if (SystemTray.isSupported()) {
            final SystemTray tray = SystemTray.getSystemTray();
            final TrayIcon trayIcon = new TrayIcon(iconImage, "WinFoom");
            trayIcon.setImageAutoSize(true);
            trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    tray.remove(trayIcon);
                    setVisible(true);
                    setState(Frame.NORMAL);
                }
            });
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowIconified(WindowEvent e) {
                    try {
                        tray.add(trayIcon);
                    } catch (AWTException ex) {
                        logger.error("Cannot add icon to tray", ex);
                    }
                    setVisible(false);
                }

                @Override
                public void windowDeiconified(WindowEvent e) {
                    tray.remove(trayIcon);
                    setExtendedState(getExtendedState() & ~Frame.ICONIFIED);
                    setVisible(true);
                    setState(Frame.NORMAL);
                }
            });
        }
        //
        setTitle("Winfoom");
        setJMenuBar(getMainMenuBar());
        setContentPane(getMainContentPanel());
        ToolTipManager.sharedInstance().setDismissDelay(TOOLTIP_TIMEOUT * 1000);

        if (proxyConfig.isAutoDetectNeeded()) {
            autoDetectProxySettings();
        }

        applyProxyType();
    }

    private void applyProxyType() {
        getProxyTypeCombo().setSelectedItem(proxyConfig.getProxyType());
        pack();
    }

    public void focusOnStartButton() {
        getBtnStart().requestFocus();
    }

    public void activate() {
        boolean started = proxyConfig.isAutostart() && startServer();
        EventQueue.invokeLater(() -> {
            if (started) {
                getBtnStop().requestFocus();
                dispatchEvent(new WindowEvent(
                        this, WindowEvent.WINDOW_ICONIFIED));
            } else {
                focusOnStartButton();
                setVisible(true);
            }
        });
    }

    private boolean autoDetectProxySettings() {
        boolean autoDetect = false;
        try {
            autoDetect = proxyConfig.autoDetect();
            if (autoDetect) {
                logger.info("Successfully autodetect proxy settings");
            } else {
                logger.warn("Failed to autodetect proxy settings");
                SwingUtils.showWarningMessage(this, "No proxy found within Internet Explorer settings!");
            }
        } catch (Exception e) {
            logger.error("Error on getting system proxy", e);
            SwingUtils.showErrorMessage(this, "Failed to retrieve Internet Explorer settings! See the log file for details");
        }
        return autoDetect;

    }

    // ---------- Labels

    private JLabel getProxyTypeLabel() {
        if (proxyTypeLabel == null) {
            proxyTypeLabel = new JLabel("Proxy type* ");
        }
        return proxyTypeLabel;
    }

    private JLabel getProxyHostLabel() {
        return new JLabel("Proxy host* ");
    }

    private JLabel getProxyPortLabel() {
        return new JLabel("Proxy port* ");
    }

    private JLabel getLocalPortLabel() {
        return new JLabel("Local proxy port* ");
    }

    private JLabel getUseSystemCredentialsLabel() {
        return new JLabel("Use system credentials");
    }

    private JLabel getPacFileLabel() {
        return new JLabel("PAC file location* ");
    }

    private JLabel getUsernameLabel(boolean mandatory) {
        return new JLabel("Username" + (mandatory ? "* " : " "));
    }

    private JLabel getPasswordLabel(boolean mandatory) {
        return new JLabel("Password" + (mandatory ? "* " : " "));
    }

    private JLabel getAuthProtocolLabel(boolean mandatory) {
        return new JLabel("Authentication protocol" + (mandatory ? "* " : " "));
    }

    private JLabel getBlacklistTimeoutLabel() {
        return new JLabel("Blacklist timeout* ");
    }

    // ------- End Labels

    // -------- Fields

    private JComboBox<ProxyConfig.Type> getProxyTypeCombo() {
        if (proxyTypeCombo == null) {
            proxyTypeCombo = new JComboBox<>(ProxyConfig.Type.values());
            proxyTypeCombo.setMinimumSize(new Dimension(80, 35));
            proxyTypeCombo.addActionListener((e) -> configureContent());
        }
        return proxyTypeCombo;
    }

    private JComboBox<ProxyConfig.HttpAuthProtocol> getHttpAuthProtocolCombo() {
        JComboBox<ProxyConfig.HttpAuthProtocol> comboBox = new JComboBox<>(ProxyConfig.HttpAuthProtocol.values());
        comboBox.setSelectedItem(proxyConfig.getHttpAuthProtocol());
        comboBox.addActionListener((e) -> proxyConfig.setHttpAuthProtocol((ProxyConfig.HttpAuthProtocol) comboBox.getSelectedItem()));
        return comboBox;
    }

    private JComboBox<ProxyConfig.HttpAuthProtocol> getPacAuthProtocolCombo() {
        JComboBox<ProxyConfig.HttpAuthProtocol> comboBox = new JComboBox<>(ProxyConfig.HttpAuthProtocol.values());
        comboBox.insertItemAt(null, 0);
        comboBox.setSelectedItem(proxyConfig.getPacHttpAuthProtocol());
        comboBox.addActionListener((e) -> proxyConfig.setPacHttpAuthProtocol((ProxyConfig.HttpAuthProtocol) comboBox.getSelectedItem()));
        comboBox.setToolTipText(HttpUtils.toHtml("The authentication protocol." +
                "<br>Make sure that all the HTTP upstream proxies share the same protocol." +
                "<br><b>Note:</b> Select this for HTTP upstream proxies only!"));
        return comboBox;
    }

    private void configureContent() {
        clearLabelsAndFields();
        getBtnCancelBlacklist().setVisible(false);
        addProxyType();
        ProxyConfig.Type proxyType = (ProxyConfig.Type) proxyTypeCombo.getSelectedItem();
        proxyConfig.setProxyType(proxyType);
        switch (Objects.requireNonNull(proxyType)) {
            case SOCKS4:
                configureForSocks4();
                break;
            case HTTP:
                configureForHttp();
                break;
            case SOCKS5:
                configureForSocks5();
                break;
            case PAC:
                configureForPac();
                break;
            case DIRECT:
                configureForDirect();
                break;
        }
        this.pack();
    }

    private JTextField getProxyHostJTextField() {
        JTextField proxyHostJTextField = createTextField(proxyConfig.getProxyHost());
        proxyHostJTextField.setToolTipText("The ip or domain name of the remote proxy");
        proxyHostJTextField.getDocument().addDocumentListener((TextChangeListener) (e) -> proxyConfig.setProxyHost(proxyHostJTextField.getText()));
        return proxyHostJTextField;
    }

    private JTextField getPacFileJTextField() {
        JTextField pacFileJTextField = createTextField(proxyConfig.getProxyPacFileLocation());
        pacFileJTextField.getDocument().addDocumentListener((TextChangeListener) (e) -> proxyConfig.setProxyPacFileLocation(pacFileJTextField.getText()));
        pacFileJTextField.setToolTipText(HttpUtils.toHtml("The location of the Proxy Auto-Config file." +
                "<br>It can be a local location (like <i>C:/pac/proxy.pac</i>) or a HTTP(s) address (like " +
                "<i>http://pacserver:80/proxy.pac</i>)"));
        return pacFileJTextField;
    }

    private JSpinner getProxyPortJSpinner() {
        JSpinner proxyPortJSpinner = createJSpinner(proxyConfig.getProxyPort());
        proxyPortJSpinner.setToolTipText("The remote proxy port, between 1 and 65535");
        proxyPortJSpinner.addChangeListener(e -> proxyConfig.setProxyPort((Integer) proxyPortJSpinner.getValue()));
        return proxyPortJSpinner;
    }

    private JSpinner getLocalPortJSpinner() {
        if (localPortJSpinner == null) {
            localPortJSpinner = createJSpinner(proxyConfig.getLocalPort());
            localPortJSpinner.setToolTipText("The port Winfoom will listen on, between 1 and 65535");
            localPortJSpinner.addChangeListener(e -> proxyConfig.setLocalPort((Integer) localPortJSpinner.getValue()));
        }
        return localPortJSpinner;
    }

    private JTextField getSocks5UsernameJTextField() {// FIXME Use getUsernameJTextField
        JTextField usernameJTextField = createTextField(proxyConfig.getProxyUsername());
        usernameJTextField.setToolTipText("The optional username if the SOCKS5 proxy requires authentication.");
        usernameJTextField.getDocument().addDocumentListener((TextChangeListener) (e) -> proxyConfig.setProxyUsername(usernameJTextField.getText()));
        return usernameJTextField;
    }


    private JPasswordField getSocks5PasswordField() {// FIXME Use getPasswordField
        JPasswordField passwordField = new JPasswordField(proxyConfig.getProxyPassword());
        passwordField.setToolTipText("The optional password if the SOCKS5 proxy requires authentication.");
        passwordField.getDocument().addDocumentListener((TextChangeListener) (e) -> proxyConfig.setProxyPassword(new String(passwordField.getPassword())));
        return passwordField;
    }

    private JTextField getHttpUsernameTextField() {
        return getUsernameTextField("The username or DOMAIN\\username required by the upstream proxy");
    }

    private JPasswordField getHttpPasswordField() {
        return getPasswordField("The password required by the upstream proxy");
    }

    private JTextField getPacUsernameTextField() {
        return getUsernameTextField(HttpUtils.toHtml("The username or DOMAIN\\username when required by the upstream proxy." +
                "<br>On Windows, when leaving this field empty the current user's credentials would be used." +
                "<br>On other operating systems leaving this field empty would cancel authentication."));
    }

    private JPasswordField getPacPasswordField() {
        return getPasswordField("The password when required by the upstream proxy");
    }

    private JTextField getUsernameTextField(String toolTipText) {
        JTextField usernameJTextField = createTextField(proxyConfig.getProxyUsername());
        usernameJTextField.setToolTipText(toolTipText);
        usernameJTextField.getDocument().addDocumentListener((TextChangeListener) (e) -> proxyConfig.setProxyUsername(usernameJTextField.getText()));
        return usernameJTextField;
    }


    private JPasswordField getPasswordField(String toolTipText) {
        JPasswordField passwordField = new JPasswordField(proxyConfig.getProxyPassword());
        passwordField.setToolTipText(toolTipText);
        passwordField.getDocument().addDocumentListener((TextChangeListener) (e) -> proxyConfig.setProxyPassword(new String(passwordField.getPassword())));
        return passwordField;
    }

    private JCheckBox getUseSystemCredentialsJCheckBox() {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setSelected(proxyConfig.isUseCurrentCredentials());
        checkBox.setToolTipText(HttpUtils.toHtml("Whether to use the current user credentials or not"));
        checkBox.addActionListener((e -> {
            proxyConfig.setUseCurrentCredentials(checkBox.isSelected());
            configureContent();
        }));

        return checkBox;
    }

    private JSpinner getBlacklistTimeoutJSpinner() {
        JSpinner proxyPortJSpinner = createJSpinner(proxyConfig.getBlacklistTimeout());
        proxyPortJSpinner.addChangeListener(e -> proxyConfig.setBlacklistTimeout((Integer) proxyPortJSpinner.getValue()));
        proxyPortJSpinner.setToolTipText(HttpUtils.toHtml("If a proxy doesn't responds it is blacklisted"
                + "<br> which means it will not be used again until the blacklist timeout (in minutes) happens."
                + "<br>A value of zero or negative would disable the blacklisting mechanism."));
        return proxyPortJSpinner;
    }

    // ---------- End Fields

    // ------- Buttons

    private JButton getBtnStart() {
        if (btnStart == null) {
            btnStart = new JButton("Start");
            btnStart.setMargin(new Insets(2, 6, 2, 6));
            btnStart.setIcon(new TunedImageIcon("arrow-right.png"));
            btnStart.addActionListener(e -> {
                startServer();
                getBtnStop().requestFocus();
            });
            btnStart.setToolTipText("Start the proxy facade");
        }
        return btnStart;
    }

    private JButton getBtnStop() {
        if (btnStop == null) {
            btnStop = new JButton("Stop");
            btnStop.setMargin(new Insets(2, 6, 2, 6));
            btnStop.addActionListener(e -> {
                stopServer();
                focusOnStartButton();
                if (proxyConfig.isAutoConfig()) {
                    getBtnCancelBlacklist().setEnabled(false);
                }
            });
            btnStop.setIcon(new TunedImageIcon("process-stop.png"));
            btnStop.setEnabled(false);
            btnStop.setToolTipText("Stop the proxy facade");
        }
        return btnStop;
    }

    private JButton getBtnCancelBlacklist() {
        if (btnCancelBlacklist == null) {
            btnCancelBlacklist = new JButton("Whitelist");
            btnCancelBlacklist.setMargin(new Insets(2, 6, 2, 6));
            btnCancelBlacklist.addActionListener(e -> {
                int clearCount = proxyBlacklist.clear();
                SwingUtils.showInfoMessage(this, String.format("Found: %d blacklisted proxies!", clearCount));
            });
            btnCancelBlacklist.setIcon(new TunedImageIcon("clear-blacklist.png"));
            btnCancelBlacklist.setVisible(false);
            btnCancelBlacklist.setEnabled(false);
            btnCancelBlacklist.setToolTipText(HttpUtils.toHtml("Remove all proxies from blacklist log." +
                    "<br>Use this when you know that some blacklisted proxies are up" +
                    "<br>and it doesn't make sense to wait for timeout."));
        }
        return btnCancelBlacklist;
    }

    private JButton getBtnTest() {
        if (btnTest == null) {
            btnTest = new JButton("Test");
            btnTest.setMargin(new Insets(2, 6, 2, 6));
            btnTest.addActionListener(event -> SwingUtils.executeRunnable(() -> {
                setBtnEnable(false);
                try {
                    String testURL = JOptionPane.showInputDialog(AppFrame.this, "Test URL*:", proxyConfig.getProxyTestUrl());
                    if (StringUtils.isNotBlank(testURL)) {
                        proxyConfig.setProxyTestUrl(testURL);
                        proxyValidator.testProxy();
                        SwingUtils.showInfoMessage(AppFrame.this, "Success!");
                    } else if (testURL != null) {
                        SwingUtils.showErrorMessage(AppFrame.this, "Invalid test URL!");
                    }
                } catch (InvalidProxySettingsException e) {
                    SwingUtils.showErrorMessage(AppFrame.this, e.getMessage());
                } catch (Exception e) {
                    logger.error("Error on testing proxy", e);
                    SwingUtils.showErrorMessage(AppFrame.this, "Proxy test failed. See the log file for details");
                } finally {
                    setBtnEnable(true);
                }
            }, AppFrame.this));
            btnTest.setIcon(new TunedImageIcon("test.png"));
            btnTest.setToolTipText("Test the proxy settings");
            btnTest.setEnabled(false);
        }
        return btnTest;
    }

    public JButton getBtnAutoDetect() {
        if (btnAutoDetect == null) {
            btnAutoDetect = new JButton("Autodetect");
            btnAutoDetect.setMargin(new Insets(2, 6, 2, 6));
            btnAutoDetect.setIcon(new TunedImageIcon("system-search.png"));
            btnAutoDetect.addActionListener((event) -> SwingUtils.executeRunnable(() -> {
                if (autoDetectProxySettings()) {
                    applyProxyType();
                }
            }, AppFrame.this));
            btnAutoDetect.setToolTipText(HttpUtils.toHtml("Attempt to load the system's current proxy settings"));
        }
        return btnAutoDetect;
    }

    // ------- End Buttons

    // --------- Panels

    private JPanel getMainContentPanel() {
        if (mainContentPanel == null) {
            mainContentPanel = new JPanel(new BorderLayout());
            mainContentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

            mainContentPanel.add(getLabelsFieldsPanel(), BorderLayout.CENTER);
            mainContentPanel.add(getBtnPanel(), BorderLayout.SOUTH);
        }
        return mainContentPanel;
    }

    private JPanel getLabelPanel() {
        if (labelPanel == null) {
            labelPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        }
        return labelPanel;
    }

    private JPanel getFieldPanel() {
        if (fieldPanel == null) {
            fieldPanel = new JPanel(new GridLayout(0, 1, 5, 5));
        }
        return fieldPanel;
    }

    private JPanel getLabelsFieldsPanel() {
        if (labelsFieldsPanel == null) {
            labelsFieldsPanel = new JPanel(new BorderLayout());
            labelsFieldsPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            labelsFieldsPanel.add(getLabelPanel(), BorderLayout.WEST);
            labelsFieldsPanel.add(getFieldPanel(), BorderLayout.CENTER);
            addProxyType();
        }
        return labelsFieldsPanel;
    }

    private void setBtnEnable(boolean enabled) {
        btnTest.setEnabled(enabled);
        btnStop.setEnabled(enabled);
        if (btnCancelBlacklist != null) {
            btnCancelBlacklist.setEnabled(enabled);
        }
    }

    private void addProxyType() {
        getLabelPanel().add(getProxyTypeLabel());
        getFieldPanel().add(wrapToPanel(getProxyTypeCombo()));
    }

    private void clearLabelsAndFields() {
        getLabelPanel().removeAll();
        getFieldPanel().removeAll();
    }

    private void configureForHttp() {
        labelPanel.add(getProxyHostLabel());
        labelPanel.add(getProxyPortLabel());
        labelPanel.add(getLocalPortLabel());
        labelPanel.add(getUseSystemCredentialsLabel());

        fieldPanel.add(getProxyHostJTextField());
        fieldPanel.add(wrapToPanel(getProxyPortJSpinner()));
        fieldPanel.add(wrapToPanel(getLocalPortJSpinner()));
        fieldPanel.add(getUseSystemCredentialsJCheckBox());

        if (!proxyConfig.isUseCurrentCredentials()) {
            labelPanel.add(getAuthProtocolLabel(true));
            labelPanel.add(getUsernameLabel(true));
            labelPanel.add(getPasswordLabel(true));

            fieldPanel.add((wrapToPanel(getHttpAuthProtocolCombo())));
            fieldPanel.add(getHttpUsernameTextField());
            fieldPanel.add(getHttpPasswordField());
        }
    }

    private void configureForSocks4() {
        labelPanel.add(getProxyHostLabel());
        labelPanel.add(getProxyPortLabel());
        labelPanel.add(getLocalPortLabel());

        fieldPanel.add(getProxyHostJTextField());
        fieldPanel.add(wrapToPanel(getProxyPortJSpinner()));
        fieldPanel.add(wrapToPanel(getLocalPortJSpinner()));
    }

    private void configureForPac() {
        labelPanel.add(getPacFileLabel());
        labelPanel.add(getBlacklistTimeoutLabel());
        labelPanel.add(getLocalPortLabel());
        labelPanel.add(getUsernameLabel(false));
        labelPanel.add(getPasswordLabel(false));
        labelPanel.add(getAuthProtocolLabel(false));

        fieldPanel.add(getPacFileJTextField());
        fieldPanel.add(wrapToPanel(getBlacklistTimeoutJSpinner(),
                new JLabel(" (" + ProxyBlacklist.TEMPORAL_UNIT.toString().toLowerCase() + ")")));
        fieldPanel.add(wrapToPanel(getLocalPortJSpinner()));

        fieldPanel.add(getPacUsernameTextField());
        fieldPanel.add(getPacPasswordField());
        fieldPanel.add((wrapToPanel(getPacAuthProtocolCombo())));

        getBtnCancelBlacklist().setEnabled(false);
        getBtnCancelBlacklist().setVisible(true);
    }


    private void configureForDirect() {
        labelPanel.add(getLocalPortLabel());
        fieldPanel.add(wrapToPanel(getLocalPortJSpinner()));
    }

    private void configureForSocks5() {
        labelPanel.add(getProxyHostLabel());
        labelPanel.add(getProxyPortLabel());
        labelPanel.add(getUsernameLabel(false));
        labelPanel.add(getPasswordLabel(false));
        labelPanel.add(getLocalPortLabel());

        fieldPanel.add(getProxyHostJTextField());
        fieldPanel.add(wrapToPanel(getProxyPortJSpinner()));
        fieldPanel.add(getSocks5UsernameJTextField());
        fieldPanel.add(getSocks5PasswordField());
        fieldPanel.add(wrapToPanel(getLocalPortJSpinner()));
    }


    private JPanel wrapToPanel(java.awt.Component... components) {
        FlowLayout flowLayout = new FlowLayout(FlowLayout.LEFT, 0, 0);
        JPanel panel = new JPanel(flowLayout);
        if (components != null) {
            for (java.awt.Component comp : components) {
                panel.setPreferredSize(comp.getPreferredSize());
                panel.add(comp);
            }
        }
        return panel;
    }

    private JPanel getBtnPanel() {
        if (btnPanel == null) {
            btnPanel = new JPanel();
            btnPanel.add(getBtnStart());
            btnPanel.add(getBtnAutoDetect());
            btnPanel.add(getBtnTest());
            btnPanel.add(getBtnCancelBlacklist());
            btnPanel.add(getBtnStop());
        }
        return btnPanel;
    }

    // -------- Menu

    private JMenuBar getMainMenuBar() {
        if (menuBar == null) {
            menuBar = new JMenuBar();
            menuBar.add(getMnFile());
            menuBar.add(getMntmSettings());
            menuBar.add(getMnHelp());
        }
        return menuBar;
    }

    private JMenu getMnFile() {
        if (mnFile == null) {
            mnFile = new JMenu("File");
            mnFile.add(getMntmExit());
        }
        return mnFile;
    }

    private JMenuItem getMntmExit() {
        if (mntmExit == null) {
            mntmExit = new JMenuItem("Exit");
            mntmExit.setIcon(new TunedImageIcon("application-exit.png"));
            mntmExit.addActionListener(e -> shutdownApp());
        }
        return mntmExit;
    }


    private JMenu getMntmSettings() {
        if (mntmSettings == null) {
            mntmSettings = new JMenu("Settings");
            mntmSettings.add(getAutoStartMenuItem());
            mntmSettings.add(getAutoDetectMenuItem());
        }
        return mntmSettings;
    }

    private JCheckBoxMenuItem getAutoStartMenuItem() {
        if (autoStartMenuItem == null) {
            autoStartMenuItem = new JCheckBoxMenuItem("Autostart");
            autoStartMenuItem.setSelected(proxyConfig.isAutostart());
            autoStartMenuItem.setToolTipText(HttpUtils.toHtml("When checked, next time you start the application " +
                    "<br>it will automatically start the proxy and minimize the window to tray."));
            autoStartMenuItem.addActionListener((event -> proxyConfig.setAutostart(autoStartMenuItem.isSelected())));
        }
        return autoStartMenuItem;
    }

    public JCheckBoxMenuItem getAutoDetectMenuItem() {
        if (autoDetectMenuItem == null) {
            autoDetectMenuItem = new JCheckBoxMenuItem("Use system proxy settings");
            autoDetectMenuItem.setSelected(proxyConfig.isAutodetect());
            autoDetectMenuItem.setToolTipText(HttpUtils.toHtml("When checked, the application " +
                    "<br>will automatically detect the proxy settings " +
                    "<br>by interrogating Internet Explorer network settings on each startup." +
                    "<br><b>WARNING: The existent settings will be overwritten!</b>"));
            autoDetectMenuItem.addActionListener((event -> proxyConfig.setAutodetect(autoDetectMenuItem.isSelected())));
        }
        return autoDetectMenuItem;
    }

    private JMenu getMnHelp() {
        if (mnHelp == null) {
            mnHelp = new JMenu("Help");
            mnHelp.add(getMntmAbout());
        }
        return mnHelp;
    }

    private JMenuItem getMntmAbout() {
        if (mntmAbout == null) {
            mntmAbout = new JMenuItem("About");
            mntmAbout.setIcon(new TunedImageIcon("dialog-information.png"));
            mntmAbout.addActionListener(e -> SwingUtils.showInfoMessage(this, "About", "Winfoom - Basic Proxy Facade" +
                    "<br>Version: " + proxyConfig.getAppVersion()
                    + "<br>Project home page: https://github.com/ecovaci/winfoom"
                    + "<br>License: Apache 2.0"));
        }
        return mntmAbout;
    }

    // ------- End Menu

    private JTextField createTextField(String text) {
        JTextField textField = new JTextField(text);
        textField.setPreferredSize(new Dimension(220, 25));
        textField.setMinimumSize(new Dimension(6, 25));
        return textField;
    }

    private JSpinner createJSpinner(Integer value) {
        JSpinner jSpinner = new JSpinner();
        jSpinner.setPreferredSize(new Dimension(60, 25));
        jSpinner.setEditor(new JSpinner.NumberEditor(jSpinner, "#"));
        SwingUtils.commitsOnValidEdit(jSpinner);
        jSpinner.setValue(value);
        return jSpinner;
    }


    private void disableAll() {
        SwingUtils.setEnabled(getContentPane(), false, JLabel.class);
    }

    private boolean isValidInput() {
        if ((proxyConfig.getProxyType().isSocks() || proxyConfig.getProxyType().isHttp())
                && StringUtils.isBlank(proxyConfig.getProxyHost())) {
            SwingUtils.showErrorMessage(this, "Fill in the proxy host");
            return false;
        }

        if ((proxyConfig.getProxyType().isSocks() || proxyConfig.getProxyType().isHttp())) {
            if (proxyConfig.getProxyPort() == null || !HttpUtils.isValidPort(proxyConfig.getProxyPort())) {
                SwingUtils.showErrorMessage(this, "Fill in a valid proxy port, between 1 and 65535");
                return false;
            }
        }

        if (proxyConfig.isAutoConfig() && StringUtils.isBlank(proxyConfig.getProxyPacFileLocation())) {
            SwingUtils.showErrorMessage(this, "Fill in a valid Pac file location");
            return false;
        }

        Integer localPort = (Integer) localPortJSpinner.getValue();
        if (!HttpUtils.isValidPort(localPort)) {
            SwingUtils.showErrorMessage(this, "Fill in a valid local proxy port, between 1 and 65535");
            return false;
        }

        if (proxyConfig.getProxyType().isHttp() && !proxyConfig.isUseCurrentCredentials()) {
            if (proxyConfig.getHttpAuthProtocol() == null) {
                SwingUtils.showErrorMessage(this, "Select the authentication protocol");
                return false;
            }
            if (StringUtils.isBlank(proxyConfig.getProxyHttpUsername())) {
                SwingUtils.showErrorMessage(this, "Fill in the username");
                return false;
            }
            if (StringUtils.isBlank(proxyConfig.getProxyHttpPassword())) {
                SwingUtils.showErrorMessage(this, "Fill in the password");
                return false;
            }
        }

        return true;
    }

    private boolean startServer() {
        logger.debug("Prepare for starting the local server");
        if (proxyConfig.getProxyType().isSocks5()) {
            if (StringUtils.isNotEmpty(proxyConfig.getProxySocks5Username())
                    && StringUtils.isEmpty(proxyConfig.getProxySocks5Password())) {
                int option = JOptionPane.showConfirmDialog(this, "The username is not empty, but you did not provide " +
                                "any password." +
                                "\nDo you still want to proceed?", "Warning", JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (option != JOptionPane.OK_OPTION) {
                    return false;
                }
            }
        }
        logger.debug("Disable all components");
        disableAll();
        logger.debug("Now start the server ");
        if (isValidInput()) {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            try {
                proxyController.start();
                getBtnTest().setEnabled(true);
                getBtnStop().setEnabled(true);
                if (proxyConfig.isAutoConfig()) {
                    getBtnCancelBlacklist().setEnabled(true);
                }
                return true;
            } catch (Exception e) {
                logger.error("Error on starting proxy server", e);
                enableInput();
                SwingUtils.showErrorMessage(AppFrame.this,
                        "Error on starting proxy server.<br>See the application's log for details.");
            } finally {
                setCursor(Cursor.getDefaultCursor());
            }
        } else {
            enableInput();
        }
        return false;
    }

    private void stopServer() {
        if (proxyController.isRunning() && JOptionPane.showConfirmDialog(this,
                "The local proxy facade is started. \nDo you like to stop the proxy facade?",
                "Warning", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.YES_OPTION) {
            proxyController.stop();
            enableInput();
        }
    }

    private void enableInput() {
        SwingUtils.setEnabled(getContentPane(), true, JLabel.class);
        getBtnStop().setEnabled(false);
        getBtnTest().setEnabled(false);
    }


    private void shutdownApp() {
        if (!proxyController.isRunning() || JOptionPane.showConfirmDialog(this,
                "The local proxy facade is started. \nDo you like to stop the proxy facade and leave the application?",
                "Warning", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.YES_OPTION) {
            logger.info("Now shutdown application");
            applicationContext.close();
            dispose();
        }
    }

    private static class TunedImageIcon extends ImageIcon {

        TunedImageIcon(String filename) {
            super(SwingUtils.loadImage(AppFrame.class, filename));
        }

        @Override
        public int getIconHeight() {
            return ICON_SIZE;
        }

        @Override
        public int getIconWidth() {
            return ICON_SIZE;
        }

        @Override
        public void paintIcon(java.awt.Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2d.drawImage(getImage(), x, y, c);
            g2d.dispose();
        }
    }

    private interface TextChangeListener extends DocumentListener {
        @Override
        default void insertUpdate(DocumentEvent e) {
            onTextChange(e);
        }

        @Override
        default void removeUpdate(DocumentEvent e) {
            onTextChange(e);
        }

        @Override
        default void changedUpdate(DocumentEvent e) {
            onTextChange(e);
        }

        void onTextChange(DocumentEvent e);
    }

}