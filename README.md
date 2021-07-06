# **OpeningHoursEvalutator** #

[![build status](https://github.com/goodudetheboy/OpeningHoursEvaluator/actions/workflows/gradle.yml/badge.svg)](https://github.com/simonpoole/OpeningHoursParser/actions) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=goodudetheboy_OpeningHoursEvaluator&metric=alert_status)](https://sonarcloud.io/dashboard?id=goodudetheboy_OpeningHoursEvaluator) [![sonarcloud bugs](https://sonarcloud.io/api/project_badges/measure?project=goodudetheboy_OpeningHoursEvaluator&metric=bugs)](https://sonarcloud.io/component_measures?id=goodudetheboy_OpeningHoursEvaluator&metric=bugs) [![sonarcould maintainability](https://sonarcloud.io/api/project_badges/measure?project=goodudetheboy_OpeningHoursEvaluator&metric=sqale_rating)](https://sonarcloud.io/component_measures?id=goodudetheboy_OpeningHoursEvaluator&metric=Maintainability) [![sonarcloud security](https://sonarcloud.io/api/project_badges/measure?project=goodudetheboy_OpeningHoursEvaluator&metric=security_rating)](https://sonarcloud.io/component_measures?id=goodudetheboy_OpeningHoursEvaluator&metric=Security) [![sonarcloud reliability](https://sonarcloud.io/api/project_badges/measure?project=goodudetheboy_OpeningHoursEvaluator&metric=reliability_rating)](https://sonarcloud.io/component_measures?id=goodudetheboy_OpeningHoursEvaluator&metric=Reliability)

An evaluator for opening hours tag according to OSM opening hours specification. This evaluator uses an opening hours tag and an instance of LocalDateTime as input and produce a Result, wherein users can extract whether the concerned venue is opened or not, if there is any comments, and other non-grammar warnings.

Currently, this evaluator has supported evaluation of most of evaluation syntax, albeit still under construction (to add geocoding etc.)

## Usage

``` java
try {
    // Initialize evaluator
    String openingHours = "Jul 2-Jun 14 open; Jul 20-Jun 4 unknown";
    boolean isStrict = false; // strict or non-strict parsing option
    OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator(openingHours, isStrict);

    // Receive result of the evaluation
    LocalDateTime time = LocalDateTime.now();
    Result result = evaluator.checkStatus(time);
} catch (OpeningHoursParseException e) {
    // Grammar-related exception
} catch (OpeningHoursEvaluationException e) {
    // Evaluation-relation exception
}
```

# Building

The project uses gradle for building. Standard gradle tasks for the java plugin can be found here https://docs.gradle.org/current/userguide/java_plugin.html. They can be invoked on the command line by running gradlew or gradlew.bat with the name of the task, for example gradlew jar to create the jar archive.

## Testing

There is a REPL instance that you can run in order to test the evaluator in its current state. Run `gradle individualTesting --console=plain` in a Gradle environment  to test this out.
