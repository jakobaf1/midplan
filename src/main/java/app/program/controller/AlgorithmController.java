package app.program.controller;
import app.program.App;
import app.program.model.FlowAlgorithms;
import app.program.model.Edge;
import app.program.model.Employee;
import app.program.model.FlowGraph;
import app.program.model.Preference;
import app.program.model.Shift;
import app.program.model.TabuAlgorithms;
import app.program.model.Vertex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;


public class AlgorithmController implements Initializable {

    // Buttons
    @FXML
    private Button minCostMaxFlowButton;
    @FXML
    private Button distTabuButton;
    // @FXML
    // private Button expGraphButton;

    @FXML
    private Button toPDFButton;
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
    private Label shiftLabel;
    @FXML
    private Label shiftLabel2;
    @FXML
    private Label shiftLabel3;
    @FXML
    private Label shiftDateLabel;
    

    // ListView
    @FXML
    private ListView<Employee> employeeList;
    @FXML
    private ListView<String> pathList;
    @FXML
    private ListView<String> prefList;
    @FXML
    private ListView<String> invalidShiftPathList;
    @FXML
    private ListView<String> invalidDepPathList;
    @FXML
    private ListView<Employee> employeeList2;
    @FXML
    private ListView<Integer> shiftDayList;

    // TextFields
    @FXML
    private TextField searchPaths;

    private FlowGraph fg;
    private FlowAlgorithms algo;
    private Shift[] shifts = {new Shift(7, 15), new Shift(15, 23), new Shift(23, 7), new Shift(7, 19), new Shift(19, 7)};
    private LocalDate date = LocalDate.now();
    private ArrayList<String> flowPaths = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fg = new FlowGraph(8, shifts, readEmployeeFile());
        fg.generateGraph(date);
        System.out.println("total amount of vertices: " + fg.getS().getTotalVertices());
        System.out.println("total amount of edges: " + fg.getS().getOutGoing().get(0).getTotalEdges());
        
        employeeList.getItems().addAll(fg.getEmps());
        employeeList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Employee>() {
            @Override
            public void changed(ObservableValue<? extends Employee> arg0, Employee arg1, Employee arg2) {
                Employee emp = employeeList.getSelectionModel().getSelectedItem();
                // shows the standard data
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

                // shows the data for preferences
                prefList.getItems().clear();
                for (Preference p : emp.getPref()) {
                    String prefData = "Wanted: ";
                    if (p.getWanted()) {
                        prefData += "Yes, ";
                    } else {
                        prefData += "No, ";
                    }
                    prefData += "Preference level: " + p.getPrefLvl() + ", ";
                    if (p.getDate() != null && p.getShift() != null) {
                        prefData += "Date: " + p.getDate() + ", "; 
                    } else if (p.getDate() != null) {
                        prefData += "Date: " + p.getDate();
                    }
                    if (p.getDay() != -1 && (p.getShift() != null || p.getRepeat() != -1)) {
                        prefData += "Day: ";
                        switch (p.getDay()) {
                            case 1:
                                prefData += "monday, ";
                                break;
                            case 2:
                                 prefData += "tuesday, ";
                                break;
                            case 3:
                                 prefData += "wednesday, ";
                                break;
                            case 4:
                                 prefData += "thursday, ";
                                break;
                            case 5:
                                 prefData += "friday, ";
                                break;
                            case 6:
                                 prefData += "saturday, ";
                                 break;
                            case 7:
                                 prefData += "sunday, ";
                                 break;
                            case 8:
                                 prefData += "monday, ";
                                 break;
                            default:
                                 prefData += "__";
                                 break;
                        }  
                    } else if (p.getDay() != -1) {
                        prefData += "Day: ";
                        switch (p.getDay()) {
                            case 1:
                                prefData += "monday";
                            case 2:
                                 prefData += "tuesday";
                            case 3:
                                 prefData += "wednesday";
                            case 4:
                                 prefData += "thursday";
                            case 5:
                                 prefData += "friday";
                            case 6:
                                 prefData += "saturday";
                            case 7:
                                 prefData += "sunday";
                            case 8:
                                 prefData += "monday";
                            default:
                                 prefData += "__";
                        }  
                    }
                    if (p.getShift() != null && p.getRepeat() != -1) {
                        prefData += "Shift: " + p.getShift() + ", ";
                    } else if (p.getShift() != null) {
                        prefData += "Shift: " + p.getShift();
                    }
                    if (p.getRepeat() != -1) {
                        prefData += "Repeat: ";
                        switch (p.getRepeat()) {
                            case 1:
                                prefData += "Weekly";
                                break;
                            case 2:
                                prefData += "Odd weeks";
                                break;
                            case 3:
                                prefData += "Even weeks";
                                break;
                            case 4:
                                prefData += "Every third weeks";
                                break;
                            case 5:
                                prefData += "Monthly";
                                break;
                            default:
                                break;
                        }
                    }
                    prefList.getItems().add(prefData);
                }
            }
        });

        employeeList2.getItems().addAll(fg.getEmps());

        searchPaths.textProperty().addListener((obs, oldVal, newVal) -> {
            String searchText = newVal.toLowerCase();
            // Filter flow paths based on search text
            ObservableList<String> filteredList = FXCollections.observableArrayList();
            for (String path : flowPaths) {
                if (path.toLowerCase().contains(searchText)) {
                    filteredList.add(path);
                }
            }
            pathList.setItems(filteredList);
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
        // Clear up previous results
        invalidShiftPathList.getItems().clear();
        invalidDepPathList.getItems().clear();
        // fg = new FlowGraph(fg.getDaysInPeriod(), shifts, fg.getEmps());

        long startTime = System.currentTimeMillis();
        int totalHours = 0;
        for (Edge e : fg.getS().getOutGoing()) {
            totalHours += e.getCap();
        }
        System.out.println("Employee hours add up to a total of: " + totalHours);
        algo = new FlowAlgorithms(fg);
        int[] results = algo.minCostFlow(fg.getS().getTotalVertices(), totalHours, fg.getS(), fg.getT());
        System.out.println("Max flow found: " + results[0] + ", weight: " + results[1] + ", prefsDenied: " + results[2] + ", prefsFulfilled: " + results[3]);
        long endTime = System.currentTimeMillis();
        System.out.println("runtime: " + (endTime-startTime)/1000.0 + " s");
        flowLabel.setText(results[0] + "");
        costLabel.setText("" + results[1]);
        runTimeLabel.setText((endTime-startTime)/1000.0 + " s");
        pathList.getItems().clear();
        fg.getPathsWithFlow(fg.getS(), fg.getT(), new boolean[fg.getS().getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0, flowPaths);
        pathList.getItems().addAll(flowPaths);
        
        ArrayList<String> shiftRulesBroken = new ArrayList<>();
        fg.getRuleBreakingShift(fg.getS(), fg.getT(), new boolean[fg.getS().getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0, shiftRulesBroken);
        invalidShiftPathList.getItems().addAll(shiftRulesBroken);

        ArrayList<String> depRulesBroken = new ArrayList<>();
        fg.getRuleBreakingDep(fg.getS(), fg.getT(), new boolean[fg.getS().getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0, depRulesBroken);
        invalidDepPathList.getItems().addAll(depRulesBroken);

        fg.getAssignedShifts(fg.getS(), fg.getT(), new boolean[fg.getS().getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0);
        employeeList2.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Employee>() {
            @Override
            public void changed(ObservableValue<? extends Employee> arg0, Employee arg1, Employee arg2) {
                shiftDayList.getItems().clear();
                Employee emp = employeeList2.getSelectionModel().getSelectedItem();
                List<Integer> uniqueList = new ArrayList<>();
                for (int i = 0; i < emp.getShifts().length; i++ ) {
                    if (emp.getShifts()[i].isEmpty()) continue;
                    uniqueList.add(i);
                }
                Collections.sort(uniqueList);
                shiftDayList.getItems().addAll(uniqueList);
                
            }
        });

        shiftDayList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Integer>() {
            @Override
            public void changed(ObservableValue<? extends Integer> arg0, Integer arg1, Integer arg2) {
                Employee emp = employeeList2.getSelectionModel().getSelectedItem();
                Integer day = shiftDayList.getSelectionModel().getSelectedItem();
                if (day != null) {
                    shiftLabel.setText(emp.getShifts()[day].get(0).toString());
                    LocalDate shiftDate = fg.getStartDate();
                    shiftDate = shiftDate.plusDays(day);
                    shiftDateLabel.setText("Shift on: " + shiftDate.toString());
                    if (emp.getShifts()[day].size() > 1) shiftLabel2.setText(emp.getShifts()[day].get(1).toString());
                    if (emp.getShifts()[day].size() > 2) shiftLabel3.setText(emp.getShifts()[day].get(2).toString());
                    if (emp.getShifts()[day].size() > 3) System.out.println("Somehow there are more than 3 shifts assigned to one day");
                }
            }
        });
        fg.printRuleViolations();
    }

    public void runDistributiveTabu() {
        fg.updateInvalidPaths(fg.getS(), fg.getT(), new boolean[fg.getS().getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0, algo.getAssignedShifts());
        TabuAlgorithms tabuDist = new TabuAlgorithms(fg, fg.getInvalidPaths());
        tabuDist.gatherInformation();
    }

    public void runEdmondsKarpAlgorithm() {
        long startTime = System.currentTimeMillis();
        algo = new FlowAlgorithms(fg);
        int maxFlow = algo.edmondsKarp(fg.getS(), fg.getT());
        System.out.println("Max flow: " + maxFlow);
        long endTime = System.currentTimeMillis();
        System.out.println("runtime: " + (endTime-startTime)/1000.0 + " s");
    }


    // public void testExpGraph() {
    //     algo = new FlowAlgorithms(fg);
    //     Vertex[] s_t = fg.makeExperimentalGraph();
    //     int[] results = algo.minCostFlow(fg.getS().getTotalVertices(), 24, s_t[0], s_t[1]);
    //     // int maxFlow = algo.edmondsKarp(s_t[0], s_t[1]);
    //     // fg.printPathsWithFlow(s_t[0], s_t[1], new boolean[fg.getS().getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0);
    //     // System.out.println("Max flow: " + maxFlow);
    //     System.out.println("Max flow found: " + results[0] + ", weight: " + results[1]);
    //     flowLabel.setText(results[0] + "");
    //     costLabel.setText("" + results[1]);
    // }

    public void convertToPDF() {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.beginText();
                contentStream.newLineAtOffset(100, 750);
                contentStream.showText("Weekly Roster");
                contentStream.endText();

                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.beginText();
                contentStream.newLineAtOffset(100, 720);
                contentStream.showText("Week of Nov 1 - Nov 7, 2024");
                contentStream.endText();

                // Days of the week headers
                String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
                int xOffset = 100;

                for (String day : days) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xOffset, 680);
                    contentStream.showText(day);
                    if (!day.equals("Sunday")) contentStream.showText(" | ");
                    contentStream.endText();
                    xOffset += 80;
                }

                // Example shift entries (placeholders)
                xOffset = 100;
                for (String day : days) {
                    contentStream.beginText();
                    contentStream.newLineAtOffset(xOffset, 640);
                    contentStream.showText("09-05");
                    contentStream.endText();
                    contentStream.beginText();
                    contentStream.newLineAtOffset(0, -15);
                    contentStream.showText("05-09");
                    contentStream.endText();
                    xOffset += 80;
                }

                // Add more shift entries as needed
            }

            document.save("WeeklyRoster.pdf");
            System.out.println("PDF created successfully.");
        } catch (IOException e) {
            System.err.println("Error creating PDF: " + e.getMessage());
        }
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
                            int day = Integer.parseInt(dateStr.substring(0, 2));
                            int month = Integer.parseInt(dateStr.substring(3, 5));
                            int year = 2000 + Integer.parseInt(dateStr.substring(6, 8));
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
