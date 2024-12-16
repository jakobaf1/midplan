# Feature: FasterSuccessiveShortestPaths algorithm

#     Scenario: when the algorithm has run, all flow assignments are valid
    # Given the shifts
    #     | startTime | endTime |
    #     | 07        | 15      |
    #     | 15        | 23      |
    #     | 23        | 07      |
    #     | 07        | 19      |
    #     | 19        | 07      |
#             And the employees from the data file   
#             When the graph for a scheduling period of 8 weeks is created
#             And solved using the FasterSuccessiveShortestPaths algorithm
#             Then all flows distributed are less than or equal to the edge capacity
#             And the sum of incoming flow in a node is equal to the sum of flow going out of it