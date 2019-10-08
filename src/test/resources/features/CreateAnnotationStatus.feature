Feature: Create Annotation Status
  In order to classify the Annotated Samples
  As an Administrator
  I want to be able to create new Annotation Statuses

  Background:
    Given There is a project with name "Default" and id 1

  Scenario: Create AnnotationStatus
    Given I login as "admin" with password "password"
    When I create a new Annotation status with name "status" for the project "Default"
    Then The response code is 201
    And There is an Annotation status with name "status"

  Scenario: Create AnnotationStatus as linguist
    Given I login as "user" with password "password"
    When I create a new Annotation status with name "status" for the project "Default"
    Then The response code is 403

  Scenario: Create AnnotationStatus unauthenticated
    When I create a new Annotation status with name "status" for the project "Default"
    Then The response code is 401

  Scenario: Delete AnnotationStatus with associated Samples
    Given I login as "admin" with password "password"
    And There is an Annotaion Status with name "default" related to the project "Default"
    And There is a single Sample with text "lalala"
    And The sample with text "lalala" is marked with Annotation status "default"
    When I delete the Annotation status "default"
    Then The response code is 204
    And The sample with text "lalala" does not have status "default"

