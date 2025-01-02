package tests.steps;


import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import app.program.controller.AlgorithmController;
import app.program.model.FlowAlgorithms;
import app.program.model.Edge;
import app.program.model.Employee;
import app.program.model.FlowGraph;
import app.program.model.Preference;
import app.program.model.Shift;
import app.program.model.TabuAlgorithms;
import app.program.model.Vertex;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class TestSteps {

    private Edge edge;
    
    @Given("an edge connected from one vertex to another with capacity {int}")
    public void an_edge_connected_from_one_vertex_to_another_with_capacity(int cap) {
        Vertex source = new Vertex(0, "s");
        Vertex sink = new Vertex(6, "t");

        edge = new Edge(0, source, sink, cap, 0, 0);
        source.addOutGoing(edge);
        sink.addInGoing(edge);

        // Now to create the backwards edge
        Edge counterEdge = new Edge(1, sink, source, 0, edge, -0, 0);
        sink.addOutGoing(counterEdge);
        source.addInGoing(counterEdge);
        edge.setCounterpart(counterEdge);
        assertTrue(edge != null && edge.getCap() == cap);
    }
    @When("{int} flow is added to the edge")
    public void flow_is_added_to_the_edge(Integer flow) {
        edge.addFlow(flow);
        assertTrue(edge.getFlow() != 0);
    }
    @Then("the edge has {int} flow over it")
    public void the_edge_has_flow_over_it(Integer flow) {
        assertTrue(edge.getFlow() == flow);
    }
    @Then("the residual edge has capacity equal to the flow")
    public void the_residual_edge_has_capacity_equal_to_the_flow() {
        assertTrue(edge.getCounterpart().getCap() == edge.getFlow());
    }

    // Scenario 2: residual edges
    @Given("an edge connected from one vertex to another with capacity {int} and flow {int}")
    public void an_edge_connected_from_one_vertex_to_another_with_capacity_and_flow(int cap, int flow) {
        Vertex source = new Vertex(0, "s");
        Vertex sink = new Vertex(6, "t");

        edge = new Edge(0, source, sink, cap, 0, 0);
        source.addOutGoing(edge);
        sink.addInGoing(edge);

        // now to create the backwards edge
        Edge counterEdge = new Edge(1, sink, source, 0, edge, -0, 0);
        sink.addOutGoing(counterEdge);
        source.addInGoing(counterEdge);
        edge.setCounterpart(counterEdge);
        edge.addFlow(flow);
        assertTrue(edge != null && edge.getCap() == cap && edge.getFlow() == flow);
    }

    @When("{int} flow is added to the residual edge")
    public void flow_is_added_to_the_residual_edge(int flow) {
        edge.getCounterpart().addFlow(flow);
    }

    @Then("then the residual edge has a capacity of {int}")
    public void then_the_residual_edge_has_a_capacity_of(int cap) {
        assertTrue(edge.getCounterpart().getCap() == cap);
    }
    
    @Then("the normal edge has a flow of {int}")
    public void the_normal_edge_has_a_flow_of(int flow) {
        assertTrue(edge.getFlow() == flow);
    }


    private Shift[] shifts;
    private Employee[] employees;
    private FlowGraph fg;
    // ##### FlowGraph scenarios ##### //
    // Scenario 1: Employees are correctly portrayed in the graph
    @Given("the shifts")
    public void the_shifts(DataTable table) {
        List<Map<String, String>> shiftsAsMap = table.asMaps(String.class, String.class);
        shifts = new Shift[shiftsAsMap.size()];
        for (int i = 0; i < shiftsAsMap.size(); i++) {
            int startTime = Integer.parseInt(shiftsAsMap.get(i).get("startTime"));
            int endTime = Integer.parseInt(shiftsAsMap.get(i).get("endTime"));

            shifts[i] = new Shift(startTime, endTime);
        }

        for (Shift shift : shifts) {
            assertTrue(shift != null);
        }
    }

    @Given("the employees")
    public void the_employees(DataTable table) {
        List<Map<String, String>> employeesAsMap = table.asMaps(String.class, String.class);

        employees = new Employee[employeesAsMap.size()];
        for (int i = 0; i < employeesAsMap.size(); i++) {
            String name = employeesAsMap.get(i).get("employeeName");
            String id = employeesAsMap.get(i).get("employeeID");
            List<String> deps = Arrays.asList(employeesAsMap.get(i).get("departments").split(", "));
            int weeklyHours = Integer.parseInt(employeesAsMap.get(i).get("weeklyHrs"));
            int expLvl = Integer.parseInt(employeesAsMap.get(i).get("expLvl"));
            
            int[] departments = new int[deps.size()];
            for (int j = 0; j < departments.length; j++) {
                departments[j] = Integer.parseInt(deps.get(j));
            }

            employees[i] = new Employee(name, id, departments, weeklyHours, expLvl, new Preference[0]);
        }
        for (Employee emp : employees) {
            assertTrue(emp != null);
        }
    }

    @When("the graph for a scheduling period of {int} weeks is created")
    public void the_graph_for_a_scheduling_period_of_weeks_is_created(int weeks) {
        fg = new FlowGraph(weeks, shifts, employees);
        fg.generateGraph(LocalDate.now());
        assertTrue(fg != null && fg.getS() != null);
    }

    @Then("print the whole graph")
    public void print_the_whole_graph() {
        for (Edge toEmp : fg.getS().getOutGoing()) {
            // if (toEmp.getType() == 1) continue;
            for (Edge toDay : toEmp.getTo().getOutGoing()) {
                if (toDay.getType() == 1) continue;
                if (!(toDay.getFrm().getEmp().getName().equals("Employee P") || toDay.getFrm().getEmp().getName().equals("Employee V") ||toDay.getFrm().getEmp().getName().equals("Employee Ø"))) continue;
                for (Edge toShift : toDay.getTo().getOutGoing()) {
                    // if (toShift.getType() == 1) continue;
                    for (Edge fromShift : toShift.getTo().getOutGoing()) {
                        if (fromShift.getType() == 0) continue;
                    }
                }
            }

        }
    }

    @Then("all the employees are in the graph")
    public void all_the_employees_are_in_the_graph() {
        Vertex s = fg.getS();
        for (Employee emp : employees) {
            Vertex empNode = s;
            for (Edge edge : s.getOutGoing()) {
                if (edge.getTo().getEmp() == emp) {
                    empNode = edge.getTo();
                    break;
                }
            }
            assertTrue(emp.equals(empNode.getEmp()));
        }
    }

    @Then("every employee has an edge from sink to them with capacity equal to their weeklyHrs times {int}")
    public void every_employee_has_an_edge_from_sink_to_them_with_capacity_equal_to_their_weeklyHrs_times(int weeks) {
        for (Edge edge : fg.getS().getOutGoing()) {
            assertTrue(edge.getCap() == edge.getTo().getEmp().getWeeklyHrs()*weeks);
        }
    }

    // Scenario 2: There is a day node for each day for every employee
    @Then("every employee is connected to {int} day nodes")
    public void every_employee_is_connected_to_day_nodes(int days) {
        for (Edge e : fg.getS().getOutGoing()) {
            Vertex empNode = e.getTo();
            int dayNodes = 0;
            for (Edge edge : empNode.getOutGoing()) {
                if (edge.getType() == 0 && edge.getTo().getPurpose() == 2) dayNodes++;
            }
            assertTrue(dayNodes == days);
        }
    }

    // Scenario 3: Shifts are correctly linked to each employee when creating the graph
    @Then("all the given shifts are in the graph")
    public void all_the_given_shifts_are_in_the_graph() {
        int shiftsInGraph = 0;
        for (Shift s : shifts) {
            if (s.calcHours() == 8) shiftsInGraph++;
        }
        Vertex node = fg.getS();
        while (node.getPurpose() != 2) {
            for (Edge e : node.getOutGoing()) {
                if (e.getType() == 0) {
                    node = e.getTo();
                    break;
                }
            }
        }

        int shiftNodes = 0;
        for (Edge edge : node.getOutGoing()) {
            if (edge.getType() == 1) continue;
            Shift shift = edge.getTo().getShift();
            boolean trueShift = false;
            for (Shift s : shifts) {
                if (s.equals(shift)) {
                    trueShift = true;
                    break;
                }
            }
            assertTrue(trueShift);
            shiftNodes++;
        }
        assertTrue(shiftNodes == shiftsInGraph);

    }

    @Then("every employee is connected to those shift nodes for every day")
    public void every_employee_is_connected_to_those_shift_nodes_for_every_day() {
        
        for (Edge empEdge : fg.getS().getOutGoing()) {
            Vertex empNode = empEdge.getTo();
            int shiftsInGraph = 0;
            for (Shift s : shifts) {
                if (s.calcHours() == 8) shiftsInGraph++;
            }
            for (Edge dayEdge : empNode.getOutGoing()) {
                if (dayEdge.getType() == 1) continue;
                int shiftNodes = 0;
                for (Edge shiftEdge : dayEdge.getTo().getOutGoing()) {
                    if (shiftEdge.getType() == 1) continue;
                    Shift shift = shiftEdge.getTo().getShift();
                    boolean trueShift = false;
                    for (Shift s : shifts) {
                        if (s.equals(shift)) {
                            trueShift = true;
                            break;
                        }
                    }
                    assertTrue(trueShift);
                    shiftNodes++;
                }
                assertTrue(shiftNodes == shiftsInGraph);
            }
        }
    }

    // Scenario 4: Experience levels are accurately portrayed in the graph
    @Then("every employee is connected to the nodes portraying their experience level")
    public void every_employee_is_connected_to_the_nodes_portraying_their_experience_level() {
        for (Edge empEdge : fg.getS().getOutGoing()) {
            Vertex empNode = empEdge.getTo();
            for (Edge dayEdge : empNode.getOutGoing()) {
                if (dayEdge.getType() == 1) continue;
                for (Edge shiftEdge : dayEdge.getTo().getOutGoing()) {
                    if (shiftEdge.getType() == 1) continue;
                    ArrayList<Integer> expLvls = new ArrayList<>();
                    for (Edge timeDepExpEdge : shiftEdge.getTo().getOutGoing()) {
                        if (timeDepExpEdge.getType() == 1) continue;
                        if (!expLvls.contains(timeDepExpEdge.getTo().getExpLvl())) expLvls.add(timeDepExpEdge.getTo().getExpLvl());
                    }
                    assertTrue(expLvls.size() == 1 && expLvls.get(0) == empNode.getEmp().getExpLvl());
                }
            }
        }
    }

    // Scenario 5: Departments are accurately portrayed in the graph
    @Then("every employee is only connected to department nodes matching their own departments")
    public void every_employee_is_only_connected_to_department_nodes_matching_their_own_departments() {
        for (Edge empEdge : fg.getS().getOutGoing()) {
            Vertex empNode = empEdge.getTo();
            for (Edge dayEdge : empNode.getOutGoing()) {
                if (dayEdge.getType() == 1) continue;
                for (Edge shiftEdge : dayEdge.getTo().getOutGoing()) {
                    if (shiftEdge.getType() == 1) continue;
                    ArrayList<Integer> deps = new ArrayList<>();
                    for (Edge timeDepExpEdge : shiftEdge.getTo().getOutGoing()) {
                        if (timeDepExpEdge.getType() == 1) continue;
                        if (!deps.contains(timeDepExpEdge.getTo().getDep())) deps.add(timeDepExpEdge.getTo().getDep());
                        for (Edge depTimeEdge : timeDepExpEdge.getTo().getOutGoing()) {
                            if (depTimeEdge.getType() == 1) continue;
                            if (!deps.contains(timeDepExpEdge.getTo().getDep())) deps.add(timeDepExpEdge.getTo().getDep());
                        }
                    }
                    assertTrue(deps.size() == empNode.getEmp().getDepartments().length);
                    for (int dep : empNode.getEmp().getDepartments()) {
                        assertTrue(deps.contains(dep));
                    }
                }
            }
        }
    }


    ///// For minCostMaxFlow.feature /////
    private FlowAlgorithms algo;
    // Scenario 1: when flow problem is solved, all flows are valid
    @When("solved using minCostMaxFlow algorithm")
    public void solved_using_minCostMaxFlow_algorithm() {
        algo = new FlowAlgorithms(fg);
        int totalEmployeeHours = 0;
        for (Edge e : fg.getS().getOutGoing()) {
            totalEmployeeHours += e.getCap();
        }
        int[] results = algo.minCostFlow(fg.getS().getTotalVertices(), totalEmployeeHours, fg.getS(), fg.getT());
        assertTrue(results[0] != 0);
    }

    @Then("all flows distributed are less than or equal to the edge capacity")
    public void all_flows_distributed_are_less_than_or_equal_to_the_edge_capacity() {
        Vertex s = fg.getS();
        // Doing a bfs search on the graph and testing each edge for the capacity
        Queue<Edge> q = new LinkedList<>();
        boolean[] edgesVisited = new boolean[s.getOutGoing().get(0).getTotalEdges()];
        for (int i = 0; i < edgesVisited.length; i++) {
            edgesVisited[i] = false;
        }

        edgesVisited[s.getOutGoing().get(0).getEdgeIndex()] = true;
        q.add(s.getOutGoing().get(0));

        while (!q.isEmpty()) {
            Edge e = q.remove();
            assertTrue(e.getCap() >= e.getFlow());
            if (!edgesVisited[e.getEdgeIndex()] && e.getFlow() > 0) {
                for (Edge edge : e.getTo().getOutGoing()) {
                    q.add(edge);
                    edgesVisited[edge.getEdgeIndex()] = true;
                }
            }
        }
    }

    @Then("the sum of incoming flow in a node is equal to the sum of flow going out of it")
    public void the_sum_of_incoming_flow_in_a_node_is_equal_to_the_sum_of_flow_going_out_of_it() {
        Vertex s = fg.getS();
        // Doing a bfs search on the graph and testing each edge for the capacity
        Queue<Vertex> q = new LinkedList<>();
        boolean[] nodesVisisted = new boolean[s.getTotalVertices()];
        for (int i = 0; i < nodesVisisted.length; i++) {
            nodesVisisted[i] = false;
        }

        nodesVisisted[s.getVertexIndex()] = true;
        q.add(s);

        while (!q.isEmpty()) {
            Vertex node = q.remove();
            // assertTrue(e.getCap() >= e.getFlow());
            for (Edge e : node.getOutGoing()) {
                if (e.getTo().getPurpose() == 0 || e.getTo().getPurpose() == 6) continue; // doesn't check sink and source
                int outGoingFlow = 0;
                for (Edge edge : e.getTo().getOutGoing()) {
                    if (!nodesVisisted[edge.getTo().getVertexIndex()]) {
                        q.add(edge.getTo());
                        nodesVisisted[edge.getTo().getVertexIndex()] = true;
                    }
                    outGoingFlow += edge.getFlow(); // Since backwards edges never have flow - only capacity increases and decreases
                }
                int inGoingFlow = 0;
                for (Edge edge : e.getTo().getInGoing()) {
                    inGoingFlow += edge.getFlow();
                }
                assertTrue(inGoingFlow == outGoingFlow);
            }
        }
        int sourceOutGoingFlow = 0;
        int sinkInGoingFlow = 0;
        for (Edge edge : s.getOutGoing()) {
            sourceOutGoingFlow += edge.getFlow();
        }
        for (Edge edge : fg.getT().getInGoing()) {
            sinkInGoingFlow += edge.getFlow();
        }
        assertTrue(sourceOutGoingFlow == sinkInGoingFlow);
    }

    // For scenario 2: 11-hour breaks between shifts
    @Given("the employees from the data file")
    public void the_employees_from_the_data_file() {
        employees = AlgorithmController.readEmployeeFile();
        assertTrue(employees != null && employees.length == 46);
    }

    @Then("each employee has at least {int} hours between each shift")
    public void each_employee_has_at_least_hours_between_each_shift(int hours) {
        fg.getAssignedShifts(fg.getS(), fg.getT(), new boolean[fg.getS().getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0);
        for (Employee emp : employees) {
            List<Shift>[] empShifts = emp.getShifts();
            for (int day = 0; day < empShifts.length; day++) {
                if (empShifts[day].isEmpty()) continue;
                int endTime;
                int startTime;
                // assertion for shift on current day compared to day after
                if (day < empShifts.length-1 && !empShifts[day+1].isEmpty()) {
                    endTime = empShifts[day].get(0).getEndTime();
                    startTime = empShifts[day+1].get(0).getStartTime();
                    if (endTime == 7) {
                        assertTrue((startTime-endTime) >= hours);
                    } else {
                        assertTrue((24 - endTime + startTime) >= hours);
                    }
                }
                // assertion for shift on current day compared to day before
                if (day > 0 && !empShifts[day-1].isEmpty()) {
                    endTime = empShifts[day-1].get(0).getEndTime();
                    startTime = empShifts[day].get(0).getStartTime();
                    if (endTime == 7) {
                        assertTrue((startTime-endTime) >= hours);
                    } else {
                        assertTrue((24 - endTime + startTime) >= hours);
                    }
                }
                
            }
        }
    }

    @Then("every twelve hour shift is valid")
    public void every_twelve_hour_shift_is_valid() {
        fg.clearAssignedShifts();
        fg.getAssignedShifts(fg.getS(), fg.getT(), new boolean[fg.getS().getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0);
        for (Employee emp : employees) {
            List<Shift>[] empShifts = emp.getShifts();
            for (int day = 0; day < empShifts.length; day++) {
                if (empShifts[day].isEmpty()) continue;
                boolean validShift = false;
                for (Shift s : shifts) {
                    if (empShifts[day].get(0).equals(s)) {
                        validShift = true;
                        break;
                    }
                }
                assertTrue(validShift);
            }
        }
    }

    @Then("only one shift is assigned per day")
    public void only_one_shift_is_assigned_per_day() {
        fg.clearAssignedShifts();
        fg.getAssignedShifts(fg.getS(), fg.getT(), new boolean[fg.getS().getTotalVertices()], new ArrayList<Vertex>(), new ArrayList<Integer>(), 0, new ArrayList<Integer>(), 0);
        for (Employee emp : employees) {
            List<Shift>[] empShifts = emp.getShifts();
            for (int day = 0; day < empShifts.length; day++) {
                if (empShifts[day].isEmpty()) continue;
                assertTrue(empShifts[day].size() == 1);
            }
        }
    }

    @Then("twelve hour shifts only have one department")
    public void twelve_hour_shifts_only_have_one_department() {
        for (Edge empEdge : fg.getS().getOutGoing()) {
            Vertex empNode = empEdge.getTo();
            for (Edge dayEdge : empNode.getOutGoing()) {
                if (dayEdge.getType() == 1 || dayEdge.getFlow() == 0) continue;
                int dep = -1;
                for (Edge shiftEdge : dayEdge.getTo().getOutGoing()) {
                    if (shiftEdge.getType() == 1 || shiftEdge.getFlow() == 0) continue;
                    for (Edge timeDepEdge : shiftEdge.getTo().getOutGoing()) {
                        if (timeDepEdge.getType() == 1 || timeDepEdge.getFlow() == 0) continue;
                        if (dep == -1) dep = timeDepEdge.getTo().getDep();
                        if (dep != -1) {
                            assertTrue(dep == timeDepEdge.getTo().getDep());
                        }
                    }
                }
            }
        }
    }

    ////// Preferences.feature ///////
    //Scenario 1: Graph has no edges to nodes representing prefLvl 1 and not wanted
    @Given("preferences for each")
    public void preferences_for_each(DataTable table) {
        List<Map<String, String>> prefsAsMap = table.asMaps(String.class, String.class);

        List<String> emps = new ArrayList<>();
        List<List<Preference>> prefs = new ArrayList<>();
        for (int i = 0; i < prefsAsMap.size(); i++) {
            String name = prefsAsMap.get(i).get("employeeName");
            String wanted = prefsAsMap.get(i).get("wanted");
            int prefLvl = Integer.parseInt(prefsAsMap.get(i).get("prefLvl"));
            String dateAsString = prefsAsMap.get(i).get("date");
            String dayAsString = prefsAsMap.get(i).get("day");
            String shiftAsString = prefsAsMap.get(i).get("shift");
            int repeat = Integer.parseInt(prefsAsMap.get(i).get("repeat"));
            
            LocalDate date;
            if (dateAsString.equals("null")) {
                date = null;
            } else {
                List<String> dayMonthYear = Arrays.asList(dateAsString.split("-"));
                date = LocalDate.of(Integer.parseInt(dayMonthYear.get(2)), Integer.parseInt(dayMonthYear.get(1)), Integer.parseInt(dayMonthYear.get(0)));
            }
            int day;
            switch (dayAsString) {
                case "monday":
                    day = 1;
                    break;
                case "tuesday":
                    day = 2;
                    break;
                case "wednesday":
                    day = 3;
                    break;
                case "thursday":
                    day = 4;
                    break;
                case "friday":
                    day = 5;
                    break;
                case "saturday":
                    day = 6;
                    break;
                case "sunday":
                    day = 7;
                    break;
                default:
                    day = -1;
                    break;
            }    

            Shift shift;
            if (shiftAsString.equals("null")) {
                shift = null;
            } else {
                List<String> startEndTimes = Arrays.asList(shiftAsString.split("-"));
                shift = new Shift(Integer.parseInt(startEndTimes.get(0)), Integer.parseInt(startEndTimes.get(1)));
            }

            Preference newPref = new Preference(wanted.toLowerCase().equals("yes"), prefLvl, date, day, shift, repeat, 0);
            if (emps.contains(name)) {
                for (int j = 0; j < emps.size(); j++) {
                    if (emps.get(j).equals(name)) {
                        prefs.get(j).add(newPref);
                        break;
                    }
                }
                
            } else {
                emps.add(name);
                prefs.add(new ArrayList<>());
                prefs.get(prefs.size()-1).add(newPref);
            }
            assertTrue(newPref != null);
        }

        for (int p = 0; p < prefs.size(); p++) {
            Preference[] empPrefs = new Preference[prefs.get(p).size()];
            for (int j = 0; j < prefs.get(p).size(); j++) {
                empPrefs[j] = prefs.get(p).get(j);
            }
            for (int j = 0; j < employees.length; j++) {
                if (employees[j].getName().equals(emps.get(p))) {
                    employees[j].addPref(empPrefs);
                }
            }
        }

    }

    @Then("nodes representing unwanted days at the highest preference level are not connected")
    public void nodes_representing_unwanted_days_at_the_highest_preference_level_are_not_connected() {
        for (Edge e : fg.getS().getOutGoing()) {
            Vertex empNode = e.getTo();
            // make an array of the unwanted days
            boolean[] unwantedDays = findUnwantedDays(empNode.getEmp());
            
            for (Edge edge : empNode.getOutGoing()) {
                if (edge.getType() == 1) continue;
                // assert that the day found in the graph doesn't match the unwanted day
                assertTrue(!unwantedDays[edge.getTo().getDay()]);
            }
            
        }
    }

    public boolean[] findUnwantedDays(Employee emp) {
        if (emp.getPref() == null) return null;
        LocalDate date = fg.getStartDate();
        int days = fg.getDaysInPeriod();
        boolean[] unwantedDays = new boolean[days];
        for (Preference p : emp.getPref()) {
            if (p.getDay() != -1) {
                int firstDay = 0;
                int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                int weekDay = date.getDayOfWeek().getValue();

                // find out what day of the week the schedule starts
                if (p.getDay() == weekDay) {
                    firstDay = 0;
                } else if (p.getDay() < weekDay) {
                    firstDay = p.getDay()+7-weekDay;
                } else {
                    firstDay = p.getDay()-weekDay;
                }

                if (p.getRepeat() != -1) {
                    int startingWeek = 0;
                    switch (p.getRepeat()) {
                        case 1: // weekly
                            for (int i = firstDay; i < days; i += 7) {
                                unwantedDays[i] = true;
                            }
                            break;
                        case 2: // odd weeks
                            date = date.plusDays(firstDay);
                            startingWeek = week%2 == 1 ? 0 : 1;
                            for (int i = firstDay+7*startingWeek; i < days; i += 14) {
                                unwantedDays[i] = true;
                            }
                            date = date.minusDays(firstDay);
                            break;
                        case 3: // even weeks
                            date = date.plusDays(firstDay);
                            startingWeek = week%2 == 0 ? 0 : 1;
                            for (int i = firstDay+7*startingWeek; i < days; i += 14) {
                                unwantedDays[i] = true;
                            }
                            date = date.minusDays(firstDay);
                            break;
                        case 4: // tri-weekly
                            // TODO
                            break;
                        case 5: // monthly
                            for (int i = firstDay; i < days; i += 28) {
                                unwantedDays[i] = true;
                            }
                            break;
                        default:
                            break;
                    }
                }    
            }
        }
        return unwantedDays;
    }

    @Then("nodes representing unwanted shifts at the highest preference level are not connected")
    public void nodes_representing_unwanted_shifts_at_the_highest_preference_level_are_not_connected() {
        for (Edge empEdge : fg.getS().getOutGoing()) {
            Vertex empNode = empEdge.getTo();
            for (Edge dayEdge : empNode.getOutGoing()) {
                if (dayEdge.getType() == 1) continue;
                List<Shift>[] unwantedShifts = findUnwantedShifts(empNode.getEmp());
                for (Edge shiftEdge : dayEdge.getTo().getOutGoing()) {
                    if (shiftEdge.getType() == 1) continue;
                    Shift shift = shiftEdge.getTo().getShift();
                    assertTrue(!unwantedShifts[shiftEdge.getFrm().getDay()].contains(shift));
                }
            }
        }
    }

    public List<Shift>[] findUnwantedShifts(Employee emp) {
        if (emp.getPref() == null) return null;
        LocalDate date = fg.getStartDate();
        int days = fg.getDaysInPeriod();
        List<Shift>[] unwantedShifts = new ArrayList[days];
        for (int i = 0; i < unwantedShifts.length; i++) {
            unwantedShifts[i] = new ArrayList<>();
        }
        for (Preference p : emp.getPref()) {
            if (p.getShift() != null) {
                if (p.getShift().calcHours() != 8) continue;
                if (p.getDay() != -1 && p.getDate() == null) {
                    int firstDay = 0;
                    int weekDay = date.getDayOfWeek().getValue();

                    // find out what day of the week the schedule starts
                    if (p.getDay() == weekDay) {
                        firstDay = 0;
                    } else if (p.getDay() < weekDay) {
                        firstDay = p.getDay()+7-weekDay;
                    } else {
                        firstDay = p.getDay()-weekDay;
                    }
                    for (int i = firstDay; i < days; i += 7) {
                        unwantedShifts[i].add(p.getShift());
                    }
                } else if (p.getDate() == null){
                    for (int i = 0; i < days; i ++) {
                        unwantedShifts[i].add(p.getShift());
                    }
                } else {
                    int dayIndex = 0;
                    for (int i = 0; i < days; i++) {
                        if (p.getDate() == date) {
                            unwantedShifts[dayIndex].add(p.getShift());
                            date = date.minusDays(dayIndex);
                            break;
                        }
                        date = date.plusDays(1);
                        dayIndex++;
                    }
                }
            }
            
        }
        return unwantedShifts;
    }

    

    @Then("nodes representing unwanted dates at the highest preference level are not connected")
    public void nodes_representing_unwanted_dates_at_the_highest_preference_level_are_not_connected() {
        for (Edge e : fg.getS().getOutGoing()) {
            Vertex empNode = e.getTo();
            // make an array of the unwanted days
            boolean[] unwantedDates = findUnwantedDays(empNode.getEmp());
            
            for (Edge edge : empNode.getOutGoing()) {
                if (edge.getType() == 1) continue;
                // assert that the day found in the graph doesn't match the unwanted day
                assertTrue(!unwantedDates[edge.getTo().getDay()]);
            }
            
        }
    }

    // scenario 2: Graph has no edges to unwanted shifts on certain days
    @When("other shifts on the same day are still present")
    public void other_shifts_on_the_same_day_are_still_present() {
        int shiftsInGraph = 0;
        for (Shift s : shifts) {
            if (s.calcHours() == 8) shiftsInGraph++;
        }
        for (Edge empEdge : fg.getS().getOutGoing()) {
            Vertex empNode = empEdge.getTo();
            int days = 0;
            for (Edge dayEdge : empNode.getOutGoing()) {
                if (dayEdge.getType() == 1) continue;
                days++;
                List<Shift>[] unwantedShifts = findUnwantedShifts(empNode.getEmp());
                int shiftsOnDay = 0;
                for (Edge shiftEdge : dayEdge.getTo().getOutGoing()) {
                    if (shiftEdge.getType() == 1) continue;
                    shiftsOnDay++;
                }
                assertTrue(shiftsOnDay == (shiftsInGraph-unwantedShifts[dayEdge.getTo().getDay()].size()));
            }
            assertTrue(days == fg.getDaysInPeriod());
        }
    }

    @Then("edge weights properly reflect the preference level")
    public void edge_weights_properly_reflect_the_preference_level() throws Exception {
        for (Edge empEdge : fg.getS().getOutGoing()) {
            Vertex empNode = empEdge.getTo();
            boolean[] unwantedDays = findUnwantedDays(empNode.getEmp());
            List<Shift>[] unwantedShifts = findUnwantedShifts(empNode.getEmp());
            for (Edge dayEdge : empNode.getOutGoing()) {
                if (dayEdge.getType() == 1) continue;
                if (unwantedDays[dayEdge.getTo().getDay()] && unwantedShifts[dayEdge.getTo().getDay()].isEmpty()) {
                    Preference pref = getPref(empNode.getEmp(), dayEdge.getTo().getDay(), 0);
                    if (pref == null) {
                        assertTrue(dayEdge.getWeight() == fg.getBaseEdgeWeight());
                    } else {
                        assertTrue(fg.findWeight(pref) == dayEdge.getWeight());
                    }
                } else {
                    assertTrue(dayEdge.getWeight() == fg.getBaseEdgeWeight());
                }
                for (Edge shiftEdge : dayEdge.getTo().getOutGoing()) {
                    if (shiftEdge.getType() == 1) continue;
                    if (unwantedShifts[dayEdge.getTo().getDay()].isEmpty()) {
                        assertTrue(shiftEdge.getWeight() == fg.getBaseEdgeWeight()); 
                        continue;
                    }
                    Preference pref = getPref(empNode.getEmp(), dayEdge.getTo().getDay(), 1);
                    if (pref == null) {
                        assertTrue(dayEdge.getWeight() == fg.getBaseEdgeWeight());
                    } else {
                        assertTrue(fg.findWeight(pref) == dayEdge.getWeight());
                    }
                }
            }
        }
    }

    public Preference getPref(Employee emp, int day, int type) throws Exception {
        for (Preference p : emp.getPref()) {
            if (type == 0 && p.getShift() != null) continue;
            if (p.getDate() != null) {
                int prefDay = 0;
                LocalDate date = fg.getStartDate();
                while (date != p.getDate()) {
                    prefDay++;
                    date.plusDays(1);
                    if (prefDay > fg.getDaysInPeriod()) throw new Exception("date not found");
                }
                if (prefDay == day) {
                    return p;
                }
            } else if (p.getDay() != -1) {
                if (p.getRepeat() != -1) {
                    int firstDay = 0;
                    LocalDate date = fg.getStartDate();
                    int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                    int weekDay = date.getDayOfWeek().getValue();

                    // find out what day of the week the schedule starts
                    if (p.getDay() == weekDay) {
                        firstDay = 0;
                    } else if (p.getDay() < weekDay) {
                        firstDay = p.getDay()+7-weekDay;
                    } else {
                        firstDay = p.getDay()-weekDay;
                    }

                    int days = fg.getDaysInPeriod();
                    if (p.getRepeat() != -1) {
                        int startingWeek = 0;
                        switch (p.getRepeat()) {
                            case 1: // weekly
                                for (int i = firstDay; i < days; i += 7) {
                                    if (i == day) return p;
                                }
                                break;
                            case 2: // odd weeks
                                date = date.plusDays(firstDay);
                                startingWeek = week%2 == 1 ? 0 : 1;
                                for (int i = firstDay+7*startingWeek; i < days; i += 14) {
                                    if (i == day) return p;
                                }
                                date = date.minusDays(firstDay);
                                break;
                            case 3: // even weeks
                                date = date.plusDays(firstDay);
                                startingWeek = week%2 == 0 ? 0 : 1;
                                for (int i = firstDay+7*startingWeek; i < days; i += 14) {
                                    if (i == day) return p;
                                }
                                date = date.minusDays(firstDay);
                                break;
                            case 4: // tri-weekly
                                // TODO
                                break;
                            case 5: // monthly
                                for (int i = firstDay; i < days; i += 28) {
                                    if (i == day) return p;
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }

            }
        }
        return null;
    }

    @When("solved using the FasterSuccessiveShortestPaths algorithm")
    public void solved_using_the_FasterSuccessiveShortestPaths_algorithm() {
        algo = new FlowAlgorithms(fg);
        int totalEmployeeHours = 0;
        for (Edge e : fg.getS().getOutGoing()) {
            totalEmployeeHours += e.getCap();
        }
        int[] results = algo.fasterSuccessiveShortestPaths(fg.getS().getTotalVertices(), totalEmployeeHours, fg.getS(), fg.getT());

        assertTrue(results[0] != 0);
    }

    private Shift[][] assignments;
    @When("employees are assigned a shift which is unwanted")
    public void employees_are_assigned_a_shift_which_is_unwanted() {
        assignments = new Shift[employees.length][fg.getDaysInPeriod()];
        for (int emp = 0; emp < employees.length; emp++) {
            for (Preference p : employees[emp].getPref()) {
                int firstDay = fg.firstDay(fg.getStartDate(), p);
                LocalDate date = fg.getStartDate();
                int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                if (p.getDay() != -1 && p.getShift() == null) {
                    if (p.getRepeat() != -1) {
                        int startingWeek = 0;
                        switch (p.getRepeat()) {
                            case 1: // weekly
                                assignments[emp][firstDay] = new Shift(7, 15);
                                break;
                            case 2: // odd weeks
                                date = date.plusDays(firstDay);
                                week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                                startingWeek = week%2 == 1 ? 0 : 1;
                                assignments[emp][firstDay+7*startingWeek] = new Shift(7, 15);
                                date = date.minusDays(firstDay);
                                break;
                            case 3: // even weeks
                                date = date.plusDays(firstDay);
                                week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                                startingWeek = week%2 == 0 ? 0 : 1;
                                assignments[emp][firstDay+7*startingWeek] = new Shift(7, 15);
                                date = date.minusDays(firstDay);
                                break;
                            case 4: // tri-weekly
                                // TODO
                                break;
                            case 5: // monthly
                                assignments[emp][firstDay] = new Shift(7, 15);
                                break;
                            default:
                                break;
                        }
                    } else {
                        assignments[emp][firstDay] = new Shift(7, 15);
                    }
                } else if (p.getDay() != -1 && p.getShift() != null) {
                    if (p.getRepeat() != -1) {
                        int startingWeek = 0;
                        switch (p.getRepeat()) {
                            case 1: // weekly
                                assignments[emp][firstDay+14] = p.getShift();
                                break;
                            case 2: // odd weeks
                                date = date.plusDays(firstDay);
                                week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                                startingWeek = week%2 == 1 ? 0 : 1;
                                assignments[emp][firstDay+7*startingWeek] =  p.getShift();
                                date = date.minusDays(firstDay);
                                break;
                            case 3: // even weeks
                                date = date.plusDays(firstDay);
                                week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                                startingWeek = week%2 == 0 ? 0 : 1;
                                assignments[emp][firstDay+7*startingWeek] =  p.getShift();
                                date = date.minusDays(firstDay);
                                break;
                            case 4: // tri-weekly
                                // TODO
                                break;
                            case 5: // monthly
                                assignments[emp][firstDay+28] =  p.getShift();
                                break;
                            default:
                                break;
                        }
                    } else {
                        assignments[emp][firstDay] =  p.getShift();
                    }
                } else if (p.getShift() != null) {
                    assignments[emp][0] =  p.getShift();
                }
            }
        }
        
        int shiftsAssigned = 0;
        for (int emp = 0; emp < assignments.length; emp++) {
            employees[emp].createShifts(assignments[0].length);
            for (int day = 0; day < assignments[0].length; day++) {
                if (assignments[emp][day] != null) {
                    shiftsAssigned++;
                }
            }
        }
        assertTrue(shiftsAssigned != 0);
    }

    @Then("a hard constraint penalty is applied to the objective value")
    public void a_hard_constraint_penalty_is_applied_to_the_objective_value() {
        TabuAlgorithms tabu = new TabuAlgorithms(fg, new ArrayList<>(), false);
        int prefs = 0;
        for (Employee e : employees) {
            prefs += e.getPref().length;
        }
        System.out.println("expected value: " + (prefs*tabu.getHardConstraintPenalty()) + ", actual value: " + tabu.calcPreferencePenalty(assignments));
        assertTrue(prefs*tabu.getHardConstraintPenalty() == tabu.calcPreferencePenalty(assignments));
    }

}