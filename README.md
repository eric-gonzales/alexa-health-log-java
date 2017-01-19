# alexa-health-log
An Alexa Skill for logging health status for Amazon Echo. Implemented in Java 8.

Available here: https://www.amazon.com/dp/B01N9SEUM2

# How it works
* When you say a command to Alexa, it triggers the Alexa skill with invocation name "health log".
* The Alexa skill calls a web service running on AWS Lambda, passing it given parameters.
* Health data is logged and can be accessed using health log commands.