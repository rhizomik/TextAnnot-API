Feature: Filter Samples
  In order to do an advanced search of the Samples
  As a linguist
  I want to filter them with different parameters

  Background:
    Given there is a created Project with name "Test Project"
    And There is a metadata field "field1" related to the template "Test Project"
    And There is a metadata field "field2" related to the template "Test Project"
    And Exists a Tag with name "Tag1" associated to the Project "Test Project"

  Scenario: Filter Samples by text and fields
    Given I login as "user" with password "password"
    And There is a sample with text "This is a test sample" with metadata
      | field1 | test |
      | field2 | test2 |
    And There is a sample with text "Hello world" with metadata
      | field1 | test |
      | field2 | test2 |
    When I filter the samples having the word "test" and the metadata
     | field1 | test |
     | field2 | test2 |
    Then The response code is 200
    And The response contains 1 sample

  Scenario: Filter Samples by text and bad metadata
    Given I login as "user" with password "password"
    And There is a sample with text "This is a test sample" with metadata
      | field1 | test |
      | field2 | wrong_value |
    When I filter the samples having the word "test" and the metadata
      | field1 | test |
      | field2 | test2 |
    Then The response code is 200
    And The response contains 0 sample

  Scenario: Filter Samples by text and annotations
    Given I login as "user" with password "password"
    And There is a sample with text "This is a test sample" with annotations
      | Tag1 |  0  |  4  |
    And There is a sample with text "Hello world" with metadata
      | field1 | test |
      | field2 | test2 |
    When I filter the samples having the word "test" and annotated by the tag "Tag1"
    Then The response code is 200
    And The response contains 1 sample

