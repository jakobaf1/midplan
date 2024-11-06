Feature: minCostMaxFlow algorithm
    Scenario: when flow problem is solved, all flows are valid
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
        And solved using minCostMaxFlow algorithm
        Then all flows distributed are less than or equal to the edge capacity
        And the sum of incoming flow in a node is equal to the sum of flow going out of it
    
    Scenario: when the algorithm has run, no rules are broken
        Given the shifts
            | shiftName | startTime | endTime |
            | Shift 1   | 07        | 15      |
            | Shift 2   | 07        | 19      |
            | Shift 3   | 15        | 23      |
            | Shift 4   | 19        | 07      |
            | Shift 5   | 23        | 07      |
        And the employees from the data file   
        When the graph for a scheduling period of 8 weeks is created
        And solved using minCostMaxFlow algorithm
        Then each employee has at least 11 hours between each shift
        And every twelve hour shift is valid
        And only one shift is assigned per day
