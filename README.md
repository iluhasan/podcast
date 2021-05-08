# podcast

### Introduction
Test app based on Apache Camel. The app creates a route that pulls from the configured RSS feed,
extracts audio podcasts, downloads them and puts to configured folder for listening offline.
The feed is checked every 5 minutes. 

### Run

You can try the app by running the following Maven goal:

	mvn spring-boot:run -e

### Test

You can test the app by running the following Maven goal: 

	mvn test
