package com.image.copy;

import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.ArrayList;

/**
 * Created by mtime
 * on 2017/10/13.
 */
class MainFrame extends JFrame implements ActionListener, ListSelectionListener {
    private JFileChooser fileChooser;
    private JTextField inSrcDir;
    private JTextField outResDirTx;
    private JTextField outImageName;
    private JButton selectSrcBtn;
    private JButton selectOutBtn;
    private JButton copyBtn;
    private JList<String> listView;
    private File srcDir;
    private File outResDir;
    private File outDrawableXh;
    private File outDrawableXxh;
    private DefaultListModel<String> listAdapter;
    private java.util.List<String> showNames;
    private java.util.List<IPair<String, String>> fileNames;
    private JLabel imageView;
    private String homeDir;
    private int selectedIndex = -1;
    private JTextArea logArea;
    private final JButton refreshBtn;
    private final JCheckBox deleteAfterCopy;
    private FileAlterationListener fileChangeListener = new FileChanged();
    private FileAlterationMonitor alterationMonitor;
    private FileAlterationObserver alterationObserver;

    MainFrame() throws HeadlessException {
        super("图片拷贝工具--by 王坤林");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width = screenSize.width;
        int height = screenSize.height;
        int windowWidth = 980;
        int windowHeight = 600;
        int x = (width - windowWidth) / 2;
        int y = (height - windowHeight) / 2;
        showNames = new ArrayList<>();
        fileNames = new ArrayList<>();

        Container container = new Container();
        container.setBackground(Color.LIGHT_GRAY);
        setContentPane(container);

        FileSystemView fsv = FileSystemView.getFileSystemView();
        File homeDirectory = fsv.getHomeDirectory();
        homeDir = homeDirectory.getAbsolutePath();

        fileChooser = new JFileChooser(homeDir, fsv);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        inSrcDir = new JTextField();
        inSrcDir.setEnabled(true);
        inSrcDir.setEditable(false);
        inSrcDir.setBounds(10, 10, 350, 30);
        inSrcDir.setTransferHandler(new DirectoryTransferHandler(dir -> {
            inSrcDir.setText(dir);
            srcDir = new File(dir);
            listenDir();
            fillInData();
        }));

        selectSrcBtn = new JButton("选择图片目录");
        selectSrcBtn.setBounds(355, 10, 150, 30);
        selectSrcBtn.addActionListener(this);

        outResDirTx = new JTextField();
        outResDirTx.setEnabled(true);
        outResDirTx.setEditable(false);
        outResDirTx.setBounds(10, 45, 350, 30);
        outResDirTx.setTransferHandler(new DirectoryTransferHandler(dir -> {
            File file = new File(dir);
            fillOutData(file);
        }));

        selectOutBtn = new JButton("目标res目录");
        selectOutBtn.setBounds(355, 45, 150, 30);
        selectOutBtn.addActionListener(this);

        listView = new JList<>();
        JScrollPane scrollPane = new JScrollPane(listView);
        scrollPane.setBounds(510, 10, windowWidth - 520, windowHeight - 50);
        listAdapter = new DefaultListModel<>();
        listView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listView.addListSelectionListener(this);
        imageView = new JLabel();
        imageView.setHorizontalAlignment(JLabel.CENTER);
        imageView.setVerticalAlignment(JLabel.CENTER);
        imageView.setBounds(110, 75, 300, 300);
        imageView.setBorder(BorderFactory.createTitledBorder("图片预览"));

        refreshBtn = new JButton("刷新");
        refreshBtn.setBounds(410, 75, 100, 30);
        refreshBtn.addActionListener(this);

        JLabel nameLabel = new JLabel("改名");
        nameLabel.setBounds(10, 380, 50, 30);

        outImageName = new JTextField();
        outImageName.setBounds(65, 380, 400, 30);

        copyBtn = new JButton("拷贝到目标");
        copyBtn.setBounds(70, 420, 100, 50);
        copyBtn.addActionListener(this);

        deleteAfterCopy = new JCheckBox("拷贝完成后删除图片", true);
        deleteAfterCopy.setBounds(170, 430, 150, 30);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);

        JScrollPane scane = new JScrollPane(logArea);
        scane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scane.setBounds(10, 475, 490, 92);

        container.add(scane);
        container.add(outImageName);
        container.add(copyBtn);
        container.add(nameLabel);
        container.add(scrollPane);
        container.add(imageView);
        container.add(inSrcDir);
        container.add(selectSrcBtn);
        container.add(outResDirTx);
        container.add(selectOutBtn);
        container.add(refreshBtn);
        container.add(deleteAfterCopy);
        setSize(windowWidth, windowHeight);
        setLocation(x, y);
        setResizable(false);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (alterationMonitor != null) {
                    try {
                        alterationMonitor.stop();
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    alterationObserver.removeListener(fileChangeListener);
                    alterationMonitor.removeObserver(alterationObserver);
                }
            }
        });
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void fillInData() {
        listAdapter.clear();
        fileNames.clear();
        showNames.clear();
        imageView.setIcon(null);
        outImageName.setText("");
        if (srcDir == null) {
            return;
        }
        File[] files = srcDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) return false;
                String name = pathname.getName();
                int index = name.lastIndexOf(".");
                if (index < 0) return false;
                String suffix = name.substring(index + 1);
                switch (suffix) {
                    case "png":
                    case "jpg":
                    case "jpeg":
                        return match(name.substring(0, index));
                    default:
                        return false;
                }
            }

            private boolean match(String name) {
                return name.endsWith("@2x") || name.endsWith("@3x");
            }
        });

        if (files == null) return;
        for (File file : files) {
            String name = file.getName();
            String substring = name.substring(0, name.lastIndexOf("@"));
            log("found " + file.toString());
            if (listAdapter.contains(substring)) {
                int indexOf = listAdapter.indexOf(substring);
                IPair<String, String> pair = fileNames.get(indexOf);
                pair.second = name;
                continue;
            }
            IPair<String, String> pair = new IPair<>();
            pair.first = name;
            fileNames.add(pair);
            showNames.add(name);
            listAdapter.addElement(substring);
        }
        listView.setModel(listAdapter);
    }

    private void fillOutData(File dir) {
        outDrawableXh = new File(dir, "drawable-xhdpi");
        outDrawableXxh = new File(dir, "drawable-xxhdpi");
        if (outDrawableXxh.exists()
                && outDrawableXxh.isDirectory()
                || outDrawableXh.exists()
                && outDrawableXh.isDirectory()) {
            outResDir = dir;
            outResDirTx.setText(outResDir.getAbsolutePath());
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source.equals(selectSrcBtn)) {
            if (srcDir == null) {
                fileChooser.setCurrentDirectory(new File(homeDir));
            } else {
                fileChooser.setCurrentDirectory(srcDir.getParentFile());
            }
            int state = fileChooser.showOpenDialog(this);
            if (state == 1) {
                return;
            } else {
                srcDir = fileChooser.getSelectedFile();
                inSrcDir.setText(srcDir.getAbsolutePath());
                listenDir();
                fillInData();
            }
        }
        if (source.equals(selectOutBtn)) {
            if (outResDir == null) {
                fileChooser.setCurrentDirectory(new File(homeDir));
            } else {
                fileChooser.setCurrentDirectory(outResDir.getParentFile());
            }
            int state = fileChooser.showOpenDialog(this);
            if (state == 1) {
                return;
            } else {
                File dir = fileChooser.getSelectedFile();
                fillOutData(dir);
            }
        }
        if (source.equals(copyBtn)) {
            if (selectedIndex < 0) return;
            if (outResDir == null) return;
            String text = outImageName.getText();
            if (text == null) return;
            text = text.trim();
            if (text.isEmpty()) return;
            IPair<String, String> pair = fileNames.get(selectedIndex);
            copy(pair.first, text);
            copy(pair.second, text);
            if (deleteAfterCopy.isSelected()) { // 删除图片
                deleteFile(pair);
                listAdapter.remove(selectedIndex);
                selectedIndex = -1;
                fillInData();
            }
        }
        if (source.equals(refreshBtn)) {
            fillInData();
        }
    }

    private void listenDir() {
        if (srcDir == null) {
            return;
        }
        if (alterationMonitor == null) {
            alterationMonitor = new FileAlterationMonitor(1000);
        }
        if (alterationObserver != null) {
            alterationObserver.removeListener(fileChangeListener);
            alterationMonitor.removeObserver(alterationObserver);
        }
        alterationObserver = new FileAlterationObserver(srcDir);
        alterationObserver.addListener(fileChangeListener);

        alterationMonitor.addObserver(alterationObserver);
        try {
            alterationMonitor.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class FileChanged extends FileAlterationListenerAdaptor {
        @Override
        public void onFileCreate(File file) {
            SwingUtilities.invokeLater(MainFrame.this::fillInData);
        }
    }

    private void deleteFile(IPair<String, String> pair) {
        delete(pair.first);
        delete(pair.second);
    }

    private void delete(String name) {
        File file = new File(srcDir, name);
        file.delete();
    }

    private void copy(String name, String rename) {
        if (name == null) return;
        int index = name.lastIndexOf("@") + 1;
        char c = name.charAt(index);
        String suffix = name.substring(name.lastIndexOf("."));
        File dst = null;
        switch (c) {
            case '2':
                dst = outDrawableXh;
                break;
            case '3':
                dst = outDrawableXxh;
                break;
        }

        if (dst == null) return;

        File source = new File(srcDir, name);

        File target = new File(dst, rename + suffix);

        if (target.exists()) {
            target.delete();
        }

        InputStream is;
        OutputStream os;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(target);
            byte[] buffer = new byte[128];
            int length;
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
            }
            is.close();
            os.flush();
            os.close();
            log("copied from " + source.toString() + " to " + target.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
            int index = listView.getSelectedIndex();
            if (index < 0 || index >= listAdapter.size()) return;
            selectedIndex = index;
            String fileName = showNames.get(index);
            File img = new File(srcDir, fileName);
            Icon icon = new ImageIcon(img.getAbsolutePath());
            imageView.setIcon(icon);
            outImageName.setText(listAdapter.get(index));
            log("preview " + fileName);
        }
    }

    private static final class IPair<FIRST, SECOND> {
        FIRST first;
        SECOND second;

        @Override
        public String toString() {
            return "IPair{" +
                    "first=" + first +
                    ", second=" + second +
                    '}';
        }
    }

    private void log(String msg) {
        logArea.append(msg + "\r\n\r\n");
    }

    private interface OnDirectoryTransfer {
        void onTransfer(String dir);
    }

    private static class DirectoryTransferHandler extends TransferHandler {

        private OnDirectoryTransfer transfer;

        private DirectoryTransferHandler(OnDirectoryTransfer transfer) {
            this.transfer = transfer;
        }

        @Override
        public boolean canImport(JComponent comp, DataFlavor[] flavors) {
            for (DataFlavor flavor : flavors) {
                if (DataFlavor.javaFileListFlavor.equals(flavor)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean importData(JComponent comp, Transferable t) {
            try {
                Object o = t.getTransferData(DataFlavor.javaFileListFlavor);

                String filepath = o.toString();
                if (filepath.startsWith("[")) {
                    filepath = filepath.substring(1);
                }
                if (filepath.endsWith("]")) {
                    filepath = filepath.substring(0, filepath.length() - 1);
                }
                if (filepath.contains(",")) {
                    boolean hasDir = false;
                    String[] split = filepath.split(",");
                    for (String per : split) {
                        File file = new File(per.trim());
                        if (file.isDirectory()) {
                            filepath = per;
                            hasDir = true;
                            break;
                        }
                    }
                    if (!hasDir) {
                        return false;
                    }
                } else {
                    File file = new File(filepath);
                    if (file.isFile()) {
                        return false;
                    }
                }
                transfer.onTransfer(filepath);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
    }
}
