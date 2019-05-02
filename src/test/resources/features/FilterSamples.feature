Feature: Filter Samples
  In order to do an advanced search of the Samples
  As a linguist
  I want to filter them with different parameters

  Scenario: Filter Samples by text
    Given I login as "user" with password "password"
    And There is a sample with text "This is a test sample"
    When I filter the samples having the word "test"
    Then The response code is 200
    And The response contains 1 sample
