Feature: Mark Sample with Annotation Status
  In order to classify the annotated Samples
  As a linguist
  I want to be able to mark them with different Annotation Statuses

  Background:
    Given There is a project with name "Default" and id 1
    And There is an Annotaion Status with name "default" related to the project "Default"
    And There is a single Sample with text "lalala"

  Scenario: Mark Sample with Annotation Status
    Given I login as "user" with password "password"
    When I attach the Sample with text "lalala" to the Annotation Status "default"
    Then The response code is 204
    And The sample with text "lalala" has annotation status "default"

  Scenario: Mark Sample with two Annotation Statuses
    Given I login as "user" with password "password"
    And There is an Annotaion Status with name "default2" related to the project "Default"
    When I attach the Sample with text "lalala" to the Annotation Status "default"
    And I attach the Sample with text "lalala" to the Annotation Status "default2"
    Then The response code is 204
    And The sample with text "lalala" has annotation status "default"
    And The sample with text "lalala" has annotation status "default2"

  Scenario: Mark Sample with Annotation Status unauthenticated
    When I attach the Sample with text "lalala" to the Annotation Status "default"
    Then The response code is 401

  Scenario: Delete Annotation Status from Sample
    Given I login as "user" with password "password"
    And The sample with text "lalala" is marked with Annotation status "default"
    When I remove the association of Sample with text "lalala" with Annotation status "default"
    Then The response code is 204
    And The sample with text "lalala" does not have status "default"

  Scenario: Delete Annotation Status from Sample unauthenticated
    And The sample with text "lalala" is marked with Annotation status "default"
    When I remove the association of Sample with text "lalala" with Annotation status "default"
    Then The response code is 401

    