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
