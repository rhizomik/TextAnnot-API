Feature: Retrieve Samples marked with Annotation status
  In order to see Samples marked with different Annotation statuses
  As a linguist
  I want to retrieve Samples marked with an Annotation status

  Background:
    Given There is a project with name "Default" and id 1
    And There is an Annotaion Status with name "default" related to the project "Default"
    And There is a single Sample with text "lalala"
    And There is a single Sample with text "lololo"
    And The sample with text "lalala" is marked with Annotation status "default"

  Scenario: Find Samples by Annotation status
    Given I login as "user" with password "password"
    When I retrieve the Samples associated with the Annotation status "default"
    Then The response code is 200
    And The response contains 1 sample

  Scenario: Find Samples by Annotation status unauthenticated
    When I retrieve the Samples associated with the Annotation status "default"
    Then The response code is 401
