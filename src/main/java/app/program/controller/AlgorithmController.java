package app.program.controller;
import app.program.App;
import app.program.model.Algorithms;
import app.program.model.Edge;
import app.program.model.Employee;
import app.program.model.FlowGraph;
import app.program.model.Preference;
import app.program.model.Shift;
import app.program.model.Vertex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

public class AlgorithmController implements Initializable {

    // Buttons
    @FXML
    private Button minCostMaxFlowButton;
    @FXML
    private Button expGraphButton;

    // Labels
    @FXML
    private Label costLabel;
    @FXML
    private Label flowLabel;
    @FXML
    private Label runTimeLabel;
    @FXML
    private Label nameLabel;
    @FXML
    private Label idLabel;
    @FXML
    private Label departmentsLabel;
    @FXML
    private Label hoursLabel;
    @FXML
    private Label experienceLabel;
    @FXML
    private Label preferencesLabel;
    

    // ListView
    @FXML
    private ListView<Employee> employeeList;


    private FlowGraph fg;
    private Algorithms algo;
    private Shift[] shifts = {new Shift(7, 19), new Shift(19,7), new Shift(7, 15), new Shift(15, 23), new Shift(23, 7)};
    private LocalDate date = LocalDate.now();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fg = new FlowGraph(8, shifts, readEmployeeFile());
        fg.generateGraph(date);
        // System.out.println("total amount of vertices: " + fg.getS().getTotalVertices());
        // System.out.println("total amount of edges: " + fg.getS().getOutGoing().get(0).getTotalEdges());
        
        employeeList.getItems().addAll(fg.getEmps());
        employeeList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Employee>() {

            @Override
            public void changed(ObservableValue<? extends Employee> arg0, Employee arg1, Employee arg2) {
                Employee emp = employeeList.getSelectionModel().getSelectedItem();
                nameLabel.setText(emp.getName());
                idLabel.setText(emp.getID());
                String deps = "";
                int[] departments = emp.getDepartments();
                for (int i = 0; i < departments.length; i++) {
                    if (departments[i] == 0) {
                        deps += "labor";
                    } else if (departments[i] == 1) {
                        deps += "maternity";
                    }
                    if (i < departments.length-1) {
                        deps += ", ";
                    }
                }
                departmentsLabel.setText(deps);
                hoursLabel.setText("" + emp.getWeeklyHrs());
                experienceLabel.setText("" + emp.getExpLvl());
                // preferencesLabel.setText(emp.getName());
            }

        });
        
        // print statements. Need to make these appear in the middle of the algorithm part
        // fg.printPathsWithFlow(fg.getS(), fg.getT(), new boolean[fg.getS().getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0);
        // fg.printRuleViolations(algo.getEmployeeShifts());
        // fg.printShiftAssignments(algo.getEmployeeShifts());        
    }

    @FXML
    private void switchToEmployeeData() throws IOException {
        App.setRoot("employeeData");
    }

    public void runMinCostAlgorithm() {
        long startTime = System.currentTimeMillis();
        int totalHours = 0;
        for (Edge e : fg.getS().getOutGoing()) {
            totalHours += e.getCap();
        }
        System.out.println("Employee hours add up to a total of: " + totalHours);
        algo = new Algorithms(fg);
        int[] results = algo.minCostFlow(fg.getS().getTotalVertices(), totalHours, fg.getS(), fg.getT());
        System.out.println("Max flow found: " + results[0] + ", weight: " + results[1]);
        long endTime = System.currentTimeMillis();
        System.out.println("runtime: " + (endTime-startTime)/1000.0 + " s");
        flowLabel.setText(results[0] + "");
        costLabel.setText("" + results[1]);
        runTimeLabel.setText((endTime-startTime)/1000.0 + " s");

    }

    public void runEdmondsKarpAlgorithm() {
        long startTime = System.currentTimeMillis();
        algo = new Algorithms(fg);
        int maxFlow = algo.edmondsKarp(fg.getS(), fg.getT());
        System.out.println("Max flow: " + maxFlow);
        long endTime = System.currentTimeMillis();
        System.out.println("runtime: " + (endTime-startTime)/1000.0 + " s");
    }


    public void testExpGraph() {
        algo = new Algorithms(fg);
        Vertex[] s_t = fg.makeExperimentalGraph();
        int[] results = algo.minCostFlow(fg.getS().getTotalVertices(), 24, s_t[0], s_t[1]);
        // int maxFlow = algo.edmondsKarp(s_t[0], s_t[1]);
        // fg.printPathsWithFlow(s_t[0], s_t[1], new boolean[fg.getS().getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0);
        // System.out.println("Max flow: " + maxFlow);
        System.out.println("Max flow found: " + results[0] + ", weight: " + results[1]);
        flowLabel.setText(results[0] + "");
        costLabel.setText("" + results[1]);
    }


    public static Employee[] readEmployeeFile() {
        ArrayList<Employee> empList = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(App.class.getResourceAsStream("/files/employees.txt")))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("#")) continue;
                if (line.startsWith("Employee:")) {
                    String name = line.substring(10).trim();
                    String id = br.readLine().substring(4).trim();
                    List<Integer> dep = new ArrayList<>();

                    String departmentLine = br.readLine();
                    if (departmentLine.contains("labor")) {
                        dep.add(0);
                    }
                    if (departmentLine.contains("maternity")) {
                        dep.add(1);
                    }
                    
                    int hrs = Integer.parseInt(br.readLine().substring(7).trim());
                    int exp = Integer.parseInt(br.readLine().substring(12).trim());

                    // For reading preferences 
                    List<Preference> preferences = new ArrayList<>();
                    Preference pref = new Preference(false, -1, null, -1, null, -1, -1);
                    
                    String preferenceHeader = br.readLine();
                    if (!preferenceHeader.contains("Preferences:")) continue;

                    while (!(line = br.readLine()).contains("]")) {
                        if (line.contains("is_wanted:")) {
                            pref.setWanted(line.contains("yes"));
                        } else if (line.contains("preference level:")) {
                            pref.setPrefLvl(Integer.parseInt(line.substring(18).trim()));
                        } else if (line.contains("date:")) {
                            String dateStr = line.substring(6).trim();
                            int year = 2000 + Integer.parseInt(dateStr.substring(0, 2));
                            int month = Integer.parseInt(dateStr.substring(3, 5));
                            int day = Integer.parseInt(dateStr.substring(6, 8));
                            pref.setDate(LocalDate.of(year, month, day));
                        } else if (line.contains("day")) {
                            switch (line.substring(5).trim().toLowerCase()) {
                                case "monday": pref.setDay(1); break;
                                case "tuesday": pref.setDay(2); break;
                                case "wednesday": pref.setDay(3); break;
                                case "thursday": pref.setDay(4); break;
                                case "friday": pref.setDay(5); break;
                                case "saturday": pref.setDay(6); break;
                                case "sunday": pref.setDay(7); break;
                            }
                        } else if (line.contains("shifts:")) {
                            String[] shiftParts = line.substring(7).trim().split("-");
                            int start = Integer.parseInt(shiftParts[0]);
                            int end = Integer.parseInt(shiftParts[1]);
                            Shift shift = new Shift(start, end);
                            pref.setShift(shift);
                        } else if (line.contains("repeat")) {
                            String repeatType = line.substring(7).trim();
                            switch (repeatType) {
                                case "daily": pref.setRepeat(0); break;
                                case "weekly": pref.setRepeat(1); break;
                                case "odd": pref.setRepeat(2); break;
                                case "even": pref.setRepeat(3); break;
                                case "tri": pref.setRepeat(4); break;
                                case "monthly": pref.setRepeat(5); break;
                            }
                        } else if (line.contains(",")) {
                            preferences.add(pref);
                            pref = new Preference();
                        }
                    }
                    Preference[] prefs = new Preference[preferences.size()];
                    for (int i = 0; i < prefs.length; i++) {
                        prefs[i] = preferences.get(i);
                    }
                    int[] deps = new int[dep.size()];
                    for (int i = 0; i < deps.length; i++) {
                        deps[i] = dep.get(i);
                    }
                    Employee newEmp = new Employee(name.trim(), id.trim(), deps, hrs, exp, prefs);
                    empList.add(newEmp);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Employee[] empArr = new Employee[empList.size()];
        for (int i = 0; i < empList.size(); i++) {
            empArr[i] = empList.get(i);
            // System.out.println(empArr[i]);
        }
        return empArr;
    }
}