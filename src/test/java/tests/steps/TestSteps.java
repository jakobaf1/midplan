package tests.steps;


import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import app.program.model.Edge;
import app.program.model.Employee;
import app.program.model.FlowGraph;
import app.program.model.Preference;
import app.program.model.Shift;
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
            assertTrue(shift.equals(shifts[shiftNodes]));
            shiftNodes++;
        }
        assertTrue(shiftNodes == shifts.length);

    }

    @Then("every employee is connected to those shift nodes for every day")
    public void every_employee_is_connected_to_those_shift_nodes_for_every_day() {
        
        for (Edge empEdge : fg.getS().getOutGoing()) {
            Vertex empNode = empEdge.getTo();
            for (Edge dayEdge : empNode.getOutGoing()) {
                if (dayEdge.getType() == 1) continue;
                int shiftNodes = 0;
                for (Edge shiftEdge : dayEdge.getTo().getOutGoing()) {
                    if (shiftEdge.getType() == 1) continue;
                    Shift shift = shiftEdge.getTo().getShift();
                    assertTrue(shift.equals(shifts[shiftNodes]));
                    shiftNodes++;
                }
                assertTrue(shiftNodes == shifts.length);
            }
        }
    }

}