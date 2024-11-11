Feature: Adding flow to edges

    Scenario: Flow is added to a normal edge
    Given an edge connected from one vertex to another with capacity 12
    When 8 flow is added to the edge
    Then the edge has 8 flow over it
    And the residual edge has capacity equal to the flow

    Scenario: Flow is added to a residual edge
    Given an edge connected from one vertex to another with capacity 12 and flow 8
    When 8 flow is added to the residual edge
    Then then the residual edge has a capacity of 0
    And the normal edge has a flow of 0