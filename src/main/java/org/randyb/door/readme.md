## Door Monitor

Web service that logs events from an [IoT wifi sensor](http://supermechanical.com/twine/) attached to my front door to a Mongo database. 

A corresponding [web page](/../src/main/resources/door/index.html) displays the most recent event timestamps, and gives "push notifications" when new events arrive (using HTTP long polling).

![](http://randyb.org/door.png) 