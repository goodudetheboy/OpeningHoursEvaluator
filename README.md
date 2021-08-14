# **OpeningHoursEvalutator** #

[![build status](https://github.com/goodudetheboy/OpeningHoursEvaluator/actions/workflows/gradle.yml/badge.svg)](https://github.com/goodudetheboy/OpeningHoursEvaluator/actions)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=goodudetheboy_OpeningHoursEvaluator&metric=alert_status)](https://sonarcloud.io/dashboard?id=goodudetheboy_OpeningHoursEvaluator)
[![sonarcloud bugs](https://sonarcloud.io/api/project_badges/measure?project=goodudetheboy_OpeningHoursEvaluator&metric=bugs)](https://sonarcloud.io/component_measures?id=goodudetheboy_OpeningHoursEvaluator&metric=bugs)
[![sonarcould maintainability](https://sonarcloud.io/api/project_badges/measure?project=goodudetheboy_OpeningHoursEvaluator&metric=sqale_rating)](https://sonarcloud.io/component_measures?id=goodudetheboy_OpeningHoursEvaluator&metric=Maintainability)
[![sonarcloud security](https://sonarcloud.io/api/project_badges/measure?project=goodudetheboy_OpeningHoursEvaluator&metric=security_rating)](https://sonarcloud.io/component_measures?id=goodudetheboy_OpeningHoursEvaluator&metric=Security)
[![sonarcloud reliability](https://sonarcloud.io/api/project_badges/measure?project=goodudetheboy_OpeningHoursEvaluator&metric=reliability_rating)](https://sonarcloud.io/component_measures?id=goodudetheboy_OpeningHoursEvaluator&metric=Reliability)

An evaluator for opening hours tag according to OSM opening hours specification. This evaluator uses an opening hours tag and an instance of LocalDateTime as input and produce a Result, wherein users can extract whether the concerned venue is opened or not, if there is any comments, and other non-grammar warnings.

Currently, this evaluator has supported evaluation of most of evaluation syntax, albeit still under construction (to add geocoding etc.)

## Features ##

This opening hours evaluator currently supports all the syntax defined in the [specification](https://wiki.openstreetmap.org/wiki/Key:opening_hours/specification), including:

- All rule modifier: `open`, `closed`/`off`, `unknown`
- All rule separators: normal, additional, and fallback rules
- 24/7 time
- OH with both wide range and small range selector
- Time selector:
  - Time range
  - Open-ended time (e.g. `18:00+`)
  - Extended time (time over 24:00, e.g. `23:00-25:00`, which equates to from 23:00 to 2:00 the next day)
  - Variable time: `dawn`, `dusk`, `sunrise`, `sunset`
- Week selector:
  - Weekday range
  - Supports for public and school holiday
  - Nth weekday and weekday with offset
- Week selector:
  - Week number of year (1-53)
  - Week sequence
  - Interval
- Month selector:
  - Monthday range, including specific date
  - Open-ended month day
  - Supports for `easter`
- Year selector:
  - Year range, for year from 1900
  - Year with open-ended
  - Year with month
  - Interval

Full documentation will be added at a later date.

## Installation ##

This project is published on Maven Central. Add the following snippets of codes to your `build.gradle` file to install it:

```
repositories {
    mavenCentral()
}
```

```
dependencies {
    implementation 'dev.vespucci.gsoc.vh:WorldHolidayDates:<LATEST-VERSION>'
}
```

## Usage ##

``` java
try {

    // Initialize evaluator
    String openingHours = "Jul 2-Jun 14 open; Jul 20-Jun 4 unknown";
    boolean isStrict = false; // strict or non-strict parsing option
    OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator(openingHours, isStrict);

    // Receive result of the evaluation
    LocalDateTime time = LocalDateTime.now();
    Result result = evaluator.evaluate(time);
    Status status = result.getStatus();

} catch (OpeningHoursParseException e) {
    // Grammar-related exception
} catch (OpeningHoursEvaluationException e) {
    // Evaluation-relation exception
}
```

There are also options to get next/last differing events. To use it, do the following:

```java
try {
    
    // Get next differing event (open/close/unknown next)
    Result nextEvent = evaluator.getNextEvent(time);
    LocalDateTime nextEventTime = nextEvent.getNextEventTime();

    // Get last differing event (open/close/unknown last)
    Result lastEvent = evaluator.getLastEvent(time);
    LocalDateTime lastEventTime = lastEvent.getLastEventTime();

} catch (OpeningHoursParseException e) {
    // Grammar-related exception
}
```

The evaluator also reports any evaluation-related warning, such as if there are any rules overriden by another rule. Check for warnings with:

```java
List<String> warnings = result.getWarnings();
```

The evaluator is by default set to Ho Chi Minh City, Vietnam as geolocation. Geolocation is used to calculate variable time such as dawn, dusk, sunset, sunrise time, if specificed in the opening hours tag. If you want to change the location, do as follows:

``` java
try {
    // geolocation
    double lat = 10.8231; // replace desired latitude here
    double lng = 106.6297; // replace desired longitude here

    String openingHours = "sunrise-sunset";
    boolean isStrict = false; // strict or non-strict parsing option
    OpeningHoursEvaluator evaluator = new OpeningHoursEvaluator(openingHours, isStrict, lat, lng);

    //....
} catch (OpeningHoursParseException e) {
    // Grammar-related exception
} catch (OpeningHoursEvaluationException e) {
    // Evaluation-relation exception
}
```

## Building ##

The project uses gradle for building. Standard gradle tasks for the java plugin can be found [here](https://docs.gradle.org/current/userguide/java_plugin.html). They can be invoked on the command line by running `gradlew` or `gradlew.bat` with the name of the task, for example `gradlew jar` to create the jar archive.

## Testing ##

There is a REPL instance that you can run on CLI in order to test the evaluator in its current state. Run `gradle individualTesting --console=plain` in a Gradle environment  to test this out, or `gradle individualTestingStrict --console=plain` to run with evaluator in strict mode.

## Contribution ##

Pull requests are always welcomed! You can try taking a look at the [Issues](https://github.com/goodudetheboy/OpeningHoursEvaluator/issues) section and use that to get a start on what to contribute. Since this project is still a bit immature, you can expect some issues to be there.

## Acknowledgements ##

I want to thank [Simon Poole](https://github.com/simonpoole) and [Rebecca Schmidt](https://github.com/rebeccasc) for mentoring me in this project. They were of very great help during the construction of this project, providing me with extremely useful technical help and support. I also want to especially thank Simon for hosting the DNS that is the namespace for this project.

I want to thank [Robin Schneider](https://github.com/ypid) and the creators of [opening_hours.js](https://github.com/opening-hours/opening_hours.js) for their JavaScript version of the evaluator, which has guided my design for my evaluator along the way, and the helpful UI that its website has to offer.
