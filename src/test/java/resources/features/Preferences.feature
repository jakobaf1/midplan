Feature: Checking whether preferences are handled

    Scenario: Graph has no edges to nodes representing prefLvl 1 and not wanted
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
    And preferences for each
        | employeeName | wanted | prefLvl | date     |   day    | shift | repeat |
        | E1           | No     | 1       | null     | sunday   | null  | 2      |
        | E1           | No     | 1       | null     | saturday | null  | 2      |
        | E2           | No     | 1       | null     | null     | 7-15  | -1     |
        | E3           | No     | 1       | null     | sunday   | null  | 1      |
        | E4           | No     | 1       | 10-12-24 | null     | null  | 1      |
        | E5           | No     | 1       | null     | sunday   | null  | 1      |
    When the graph for a scheduling period of 8 weeks is created
    Then nodes representing unwanted days at the highest preference level are not connected
    And nodes representing unwanted shifts at the highest preference level are not connected
    And nodes representing unwanted dates at the highest preference level are not connected

    #### Currently think I don't handle the combination of days/shifts and dates/shifts preferences properly
    #### See below test scenarios

    Scenario: Graph has no edges to unwanted shifts on certain days
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
    And preferences for each
        | employeeName | wanted | prefLvl | date     |   day    | shift | repeat |
        | E1           | No     | 1       | null     | sunday   | 7-15  | 2      |
        | E1           | No     | 1       | null     | sunday   | 7-19  | 2      |
        | E2           | No     | 1       | null     | sunday   | 7-15  | -1     |
        | E3           | No     | 1       | null     | sunday   | 23-7  | 1      |
        | E4           | No     | 1       | null     | tuesday  | 7-19  | 1      |
        | E5           | No     | 1       | null     | sunday   | 23-7  | 1      |
        | E5           | No     | 1       | null     | monday   | 7-15  | 1      |    
    When the graph for a scheduling period of 8 weeks is created
    And nodes representing unwanted shifts at the highest preference level are not connected
    And other shifts on the same day are still present

    Scenario: Graph has no edges to unwanted shifts on certain dates
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
    And preferences for each
        | employeeName | wanted | prefLvl | date     |   day    | shift | repeat |
        | E1           | No     | 1       | null     | sunday   | null  | 2      |
        | E1           | No     | 1       | null     | saturday | null  | 2      |
        | E2           | No     | 1       | 24-12-24 | null     | 15-23 | -1     |
        | E2           | No     | 1       | 24-12-24 | null     | 19-7  | -1     |
        | E2           | No     | 1       | 10-12-24 | null     | 15-23 | -1     |
        | E3           | No     | 1       | null     | sunday   | null  | 1      |
        | E4           | No     | 1       | 10-12-24 | null     | null  | 1      |
        | E5           | No     | 1       | null     | sunday   | null  | 1      |
        | E5           | No     | 1       | null     | monday   | 7-15  | 1      |    
    When the graph for a scheduling period of 8 weeks is created
    Then nodes representing unwanted days at the highest preference level are not connected
    And nodes representing unwanted shifts at the highest preference level are not connected
    And nodes representing unwanted dates at the highest preference level are not connected

    # Scenario: Graph portrays preferences correctly through weights
    # Given the shifts
    #     | shiftName | startTime | endTime |
    #     | Shift 1   | 07        | 15      |
    #     | Shift 2   | 07        | 19      |
    #     | Shift 3   | 15        | 23      |
    #     | Shift 4   | 19        | 07      |
    #     | Shift 5   | 23        | 07      |
    # And the employees
    #     | employeeName | employeeID | departments | weeklyHrs | expLvl |
    #     | E1           | E1         | 0, 1        | 37        | 1      | 
    #     | E2           | E2         | 0           | 16        | 2      |
    #     | E3           | E3         | 1           | 42        | 2      | 
    #     | E4           | E4         | 0, 1        | 20        | 2      | 
    #     | E5           | E5         | 0, 1        | 37        | 1      | 
    # And preferences for each
    #     | employeeName | wanted | prefLvl | date     |   day    | shift | repeat |
    #     | E1           | No     | 1       | null     | sunday   | null  | 2      |
    #     | E1           | No     | 1       | null     | saturday | null  | 2      |
    #     | E2           | No     | 1       | null     | null     | 7-15  | -1     |
    #     | E3           | No     | 1       | null     | sunday   | null  | 1      |
    #     | E4           | No     | 1       | 10-12-24 | null     | null  | 1      |
    #     | E5           | No     | 1       | null     | sunday   | null  | 1      |    
    # When the graph for a scheduling period of 8 weeks is created
    # Then edge weights properly reflect the preference level



# can make a scenario based on whether preferences are held -> could probably be its own .feature file
#  that file could have whether edges which aren't wanted at lvl 1 are in the graph, whether costs are correctly displayed and so on
#  maybe even one with checkConditions? depending on how that goes

# can make a test for whether there is a cost to a day node, when it should be a shift node that isn't wanted