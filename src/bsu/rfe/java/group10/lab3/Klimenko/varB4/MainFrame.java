package bsu.rfe.java.group10.lab3.Klimenko.varB4;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.Random;

public class MainFrame extends JFrame {
    private JTextField fromField, toField, stepField;
    private JTable table;
    private DefaultTableModel tableModel;
    private DecimalFormat decimalFormat = new DecimalFormat("#.#####");

    public MainFrame(String[] args) {
        super("Вычисление многочлена по схеме Горнера");

        JPanel inputPanel = new JPanel(new GridLayout(1, 6));
        JLabel fromLabel = new JLabel("X от:");
        fromField = new JTextField("0.0");
        JLabel toLabel = new JLabel("до:");
        toField = new JTextField("1.0");
        JLabel stepLabel = new JLabel("с шагом:");
        stepField = new JTextField("0.1");

        inputPanel.add(fromLabel);
        inputPanel.add(fromField);
        inputPanel.add(toLabel);
        inputPanel.add(toField);
        inputPanel.add(stepLabel);
        inputPanel.add(stepField);

        JButton calculateButton = new JButton("Вычислить");
        calculateButton.addActionListener(e -> calculate());
        JButton clearButton = new JButton("Очистить");
        clearButton.addActionListener(e -> clearTable());
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(calculateButton);
        buttonPanel.add(clearButton);

        String[] columnNames = {"X", "Значение многочлена", "Взаимно простые?"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Файл");
        JMenuItem openFileMenuItem = new JMenuItem("Открыть файл");
        JMenuItem saveTextMenuItem = new JMenuItem("Сохранить в текст");
        JMenuItem saveBinaryMenuItem = new JMenuItem("Сохранить в бинарный");
        openFileMenuItem.addActionListener(e -> openFile());
        saveTextMenuItem.addActionListener(e -> saveToFile(false));
        saveBinaryMenuItem.addActionListener(e -> saveToFile(true));
        fileMenu.add(openFileMenuItem);
        fileMenu.add(saveTextMenuItem);
        fileMenu.add(saveBinaryMenuItem);
        menuBar.add(fileMenu);
        JMenu tableMenu = new JMenu("Таблица");
        JMenuItem searchValueMenuItem = new JMenuItem("Найти значение");
        searchValueMenuItem.addActionListener(e -> searchValueInTable());
        tableMenu.add(searchValueMenuItem);
        menuBar.add(tableMenu);
        JMenu helpMenu = new JMenu("Справка");
        JMenuItem aboutMenuItem = new JMenuItem("О программе");
        aboutMenuItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Автор: Клименко Пётр\nГруппа: 10\nВариант 4",
                    "О программе", JOptionPane.INFORMATION_MESSAGE);
        });
        helpMenu.add(aboutMenuItem);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);

        setLayout(new BorderLayout());
        add(inputPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void openFile() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
                tableModel.setRowCount(0); 
                while (dis.available() > 0) {
                    double x = dis.readDouble();
                    double value = dis.readDouble();
                    boolean coprime = dis.readBoolean();
                    String coprimeText = coprime ? "Да" : "Нет";
                    tableModel.addRow(new Object[]{
                            decimalFormat.format(x),
                            decimalFormat.format(value),
                            coprimeText
                    });
                }
                JOptionPane.showMessageDialog(this, "Данные загружены из бинарного файла!", "Успех", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                    tableModel.setRowCount(0); 
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("\t");
                        if (parts.length == 3) {
                            tableModel.addRow(new Object[]{parts[0], parts[1], parts[2]});
                        }
                    }
                    JOptionPane.showMessageDialog(this, "Данные загружены из текстового файла!", "Успех", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this, "Ошибка чтения файла!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void searchValueInTable() {
        String inputValue = JOptionPane.showInputDialog(this, "Введите значение для поиска:",
                "Поиск значения", JOptionPane.QUESTION_MESSAGE);

        if (inputValue == null || inputValue.trim().isEmpty()) {
            return;
        }
        boolean found = false;
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String cellValue = (String) tableModel.getValueAt(row, 1); 
            if (cellValue.equals(inputValue)) {
                table.getSelectionModel().setSelectionInterval(row, row);
                table.scrollRectToVisible(table.getCellRect(row, 1, true));
                highlightCell(row, 1);
                found = true;
                break;
            }
        }

        if (!found) {
            JOptionPane.showMessageDialog(this, "Значение не найдено!", "Результат поиска",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void highlightCell(int row, int column) {
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() { 
        	 @Override
             public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                            boolean hasFocus, int rowIndex, int columnIndex) {
                 Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIndex, columnIndex);
                 if (rowIndex == row && columnIndex == column) {
                     cell.setBackground(Color.GREEN);
                 } else {
                     cell.setBackground(Color.WHITE);
                 }
                 return cell;
             }
        });
        table.repaint();
    }

    private void calculate() {
        try {
            double from = Double.parseDouble(fromField.getText());
            double to = Double.parseDouble(toField.getText());
            double step = Double.parseDouble(stepField.getText());

            tableModel.setRowCount(0);
            double[] coefficients = generateRandomCoefficients(10);
            for (double x = from; x <= to; x += step) {
                double value = evaluatePolynomial(x, coefficients);
                String formattedX = decimalFormat.format(x);
                String formattedValue = decimalFormat.format(value);
                String coprimeText = areCoprime((int) x, (int) value) ? "Да" : "Нет";
                tableModel.addRow(new Object[]{formattedX, formattedValue, coprimeText});
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Введите корректные числовые значения!", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private double[] generateRandomCoefficients(int size) {
        double[] coeffs = new double[size];
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            coeffs[i] = random.nextDouble() * 10;
        }
        return coeffs;
    }

    private double evaluatePolynomial(double x, double[] coefficients) {
        double result = 0;
        for (double coefficient : coefficients) {
            result = result * x + coefficient;
        }
        return result;
    }

    private boolean areCoprime(int a, int b) {
        return gcd(a, b) == 1;
    }

    private int gcd(int a, int b) {
        while (b != 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return Math.abs(a);
    }

    private void clearTable() {
        tableModel.setRowCount(0); 
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {   });
        table.repaint(); 
    }

    private void saveToFile(boolean binary) {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showSaveDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            if (binary) {
                try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(file))) {
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        double x = Double.parseDouble((String) tableModel.getValueAt(i, 0));
                        double value = Double.parseDouble((String) tableModel.getValueAt(i, 1));
                        boolean coprime = "Да".equals(tableModel.getValueAt(i, 2));
                        dos.writeDouble(x);
                        dos.writeDouble(value);
                        dos.writeBoolean(coprime);
                    }
                    JOptionPane.showMessageDialog(this, "Данные сохранены в бинарный файл!", "Успех", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Ошибка записи в бинарный файл!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                    for (int i = 0; i < tableModel.getRowCount(); i++) {
                        String x = (String) tableModel.getValueAt(i, 0);
                        String value = (String) tableModel.getValueAt(i, 1);
                        String coprime = (String) tableModel.getValueAt(i, 2);
                        writer.printf("%s\t%s\t%s%n", x, value, coprime);
                    }
                    JOptionPane.showMessageDialog(this, "Данные сохранены в текстовый файл!", "Успех", JOptionPane.INFORMATION_MESSAGE);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Ошибка записи в текстовый файл!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public static void main(String[] args) {
        new MainFrame(args);
    }
}