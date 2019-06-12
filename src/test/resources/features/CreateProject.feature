Feature: Create new Project
  In order to have Projects
  As an admin
  I want to create a new Project

  Scenario: Create a new Project as admin
    Given I login as "admin" with password "password"
    When I create a new Project with name "mtTest"
    Then The response code is 201
    And The Project name is "mtTest"

  Scenario: Create a new Project as linguist
    Given I login as "user" with password "password"
    When I create a new Project with name "mtTest"
    Then The response code is 403

  Scenario: Create a new Project as admin without name
    Given I login as "admin" with password "password"
    When I create a new Project with name ""
    Then The response code is 400
    And The error message is "must not be blank"

  Scenario: Check that a Project has 1 sample
    Given I login as "admin" with password "password"
    When There is a single Sample with text "Test"
    When I create a new Project "mtTest" with the previous sample
    Then The Project with name "mtTest" have 1 samples