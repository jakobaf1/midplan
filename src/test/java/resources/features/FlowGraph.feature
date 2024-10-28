Feature: Making the graph

    Scenario: Employees are correctly portrayed in the graph
    Given the shifts
        | startTime | endTime |
        | 07        | 15      |
        | 07        | 19      |
        | 15        | 23      |
        | 19        | 07      |
        | 23        | 07      |
    And the employees
        | employeeName | employeeID | departments | weeklyHrs | expLvl |
        | E1           | E1         | 0, 1        | 37        | 1      | 
        | E2           | E2         | 0           | 16        | 2      |
        | E3           | E3         | 1           | 42        | 2      | 
        | E4           | E4         | 0, 1        | 20        | 2      | 
        | E5           | E5         | 0, 1        | 37        | 1      |    
    When the graph for a scheduling period of 2 weeks is created
    Then all the employees are in the graph
    And every employee has an edge from sink to them with capacity equal to their weeklyHrs times 2

    Scenario: There is a day node for each day for every employee
    Given the shifts
        | shiftName | startTime | endTime |
        | Shift 1   | 07        | 15      |
        | Shift 2   | 07        | 19      |
        | Shift 3   | 15        | 23      |
        | Shift 4   | 19        | 07      |
        | Shift 5   | 23        | 07      |
    And the employees
        | employeeName | employeeID | departments | weeklyHrs | expLvl |
        | E1           | E1         | 0, 1        | 37        | 1      | 
        | E2           | E2         | 0           | 16        | 2      |
        | E3           | E3         | 1           | 42        | 2      | 
        | E4           | E4         | 0, 1        | 20        | 2      | 
        | E5           | E5         | 0, 1        | 37        | 1      |    
    When the graph for a scheduling period of 8 weeks is created
    Then every employee is connected to 56 day nodes

    Scenario: Shifts are correctly linked to each employee when creating the graph
    Given the shifts
        | shiftName | startTime | endTime |
        | Shift 1   | 07        | 15      |
        | Shift 2   | 07        | 19      |
        | Shift 3   | 15        | 23      |
        | Shift 4   | 19        | 07      |
        | Shift 5   | 23        | 07      |
    And the employees
        | employeeName | employeeID | departments | weeklyHrs | expLvl |
        | E1           | E1         | 0, 1        | 37        | 1      | 
        | E2           | E2         | 0           | 16        | 2      |
        | E3           | E3         | 1           | 42        | 2      | 
        | E4           | E4         | 0, 1        | 20        | 2      | 
        | E5           | E5         | 0, 1        | 37        | 1      |    
    When the graph for a scheduling period of 8 weeks is created
    Then all the given shifts are in the graph
    And every employee is connected to those shift nodes for every day

    Scenario: Experience levels are accurately portrayed in the graph
    Given the shifts
        | shiftName | startTime | endTime |
        | Shift 1   | 07        | 15      |
        | Shift 2   | 07        | 19      |
    And the employees
        | employeeName | employeeID | departments | weeklyHrs | expLvl |
        | E1           | E1         | 0, 1        | 37        | 1      | 
        | E2           | E2         | 0           | 16        | 2      |
        | E3           | E3         | 1           | 42        | 2      | 
        | E4           | E4         | 0, 1        | 20        | 2      | 
        | E5           | E5         | 0, 1        | 37        | 1      |    
    When the graph for a scheduling period of 2 weeks is created
    Then every employee is connected to the nodes portraying their experience level

    Scenario: Departments are accurately portrayed in the graph
    Given the shifts
        | shiftName | startTime | endTime |
        | Shift 1   | 07        | 15      |
        | Shift 2   | 07        | 19      |
    And the employees
        | employeeName | employeeID | departments | weeklyHrs | expLvl |
        | E1           | E1         | 0, 1        | 37        | 1      | 
        | E2           | E2         | 0           | 16        | 2      |
        | E3           | E3         | 1           | 42        | 2      | 
        | E4           | E4         | 0, 1        | 20        | 2      | 
        | E5           | E5         | 0, 1        | 37        | 1      |    
    When the graph for a scheduling period of 2 weeks is created
    Then every employee is only connected to department nodes matching their own departments
