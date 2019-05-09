Feature: Filter Samples
  In order to do an advanced search of the Samples
  As a linguist
  I want to filter them with different parameters

  Background:
    Given There is a metadata template with name "Test Template"
    And There is a metadata field "field1" related to the template "Test Template"
    And There is a metadata field "field2" related to the template "Test Template"

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
