# Feature: Checking whether preferences are handled

#     Scenario: Graph has no edges to nodes representing prefLvl 1 and not wanted
#     Given the shifts
#         | shiftName | startTime | endTime |
#         | Shift 1   | 07        | 15      |
#         | Shift 2   | 07        | 19      |
#         | Shift 3   | 15        | 23      |
#         | Shift 4   | 19        | 07      |
#         | Shift 5   | 23        | 07      |
#     And the employees
#         | employeeName | employeeID | departments | weeklyHrs | expLvl |
#         | E1           | E1         | 0, 1        | 37        | 1      | 
#         | E2           | E2         | 0           | 16        | 2      |
#         | E3           | E3         | 1           | 42        | 2      | 
#         | E4           | E4         | 0, 1        | 20        | 2      | 
#         | E5           | E5         | 0, 1        | 37        | 1      | 
#     And preferences for each
#         | employeeName | wanted | prefLvl | date |   day    | shift | repeat |
#         | E1           | No     | 1       | null | sunday   | null  | 2      |
#         | E1           | No     | 1       | null | saturday | null  | 2      |
#         | E3           | No     | 1       | null | sunday   | null  | 1      |
#         | E4           | No     | 1       | null | sunday   | null  | 1      |
#         | E5           | No     | 1       | null | sunday   | null  | 1      |    
#     When the graph for a scheduling period of 8 weeks is created
#     Then nodes representing not wanted circumstances of the highest level are not connected



# can make a scenario based on whether preferences are held -> could probably be its own .feature file
#  that file could have whether edges which aren't wanted at lvl 1 are in the graph, whether costs are correctly displayed and so on
#  maybe even one with checkConditions? depending on how that goes

# can make a test for whether there is a cost to a day node, when it should be a shift node that isn't wanted